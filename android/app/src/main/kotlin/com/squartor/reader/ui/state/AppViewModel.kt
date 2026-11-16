package com.squartor.reader.ui.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.squartor.reader.data.BookRepository
import com.squartor.reader.model.AppBrightnessMode
import com.squartor.reader.model.BookBookmark
import com.squartor.reader.model.BookEntry
import com.squartor.reader.model.CloudSyncSettings
import com.squartor.reader.model.DEFAULT_SHELF_NAME
import com.squartor.reader.model.ImportedFont
import com.squartor.reader.model.ReadingStyle
import com.squartor.reader.model.StorageStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.exp

data class ImportActivity(
    val title: String,
    val detail: String = "",
    val active: Boolean = true,
    val failed: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val smoothProgress: Float? = null,
) {
    val progress: Float
        get() = (smoothProgress ?: if (total > 0) (current.toFloat() / total.toFloat()) else 0f)
            .coerceIn(0f, 1f)
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    val repository: BookRepository = BookRepository(application.applicationContext)

    private val _books = MutableStateFlow<List<BookEntry>>(emptyList())
    val books: StateFlow<List<BookEntry>> = _books.asStateFlow()

    private val _shelves = MutableStateFlow<List<String>>(emptyList())
    val shelves: StateFlow<List<String>> = _shelves.asStateFlow()

    private val _style = MutableStateFlow(ReadingStyle())
    val style: StateFlow<ReadingStyle> = _style.asStateFlow()

    private val _fonts = MutableStateFlow<List<ImportedFont>>(emptyList())
    val fonts: StateFlow<List<ImportedFont>> = _fonts.asStateFlow()

    private val _readingStats = MutableStateFlow<Map<String, Map<String, Int>>>(emptyMap())
    val readingStats: StateFlow<Map<String, Map<String, Int>>> = _readingStats.asStateFlow()

    private val _dailyReadingTotals = MutableStateFlow<Map<String, Int>>(emptyMap())
    val dailyReadingTotals: StateFlow<Map<String, Int>> = _dailyReadingTotals.asStateFlow()

    private val _cloudSyncSettings = MutableStateFlow(CloudSyncSettings())
    val cloudSyncSettings: StateFlow<CloudSyncSettings> = _cloudSyncSettings.asStateFlow()

    private val _storageStats = MutableStateFlow(StorageStats(booksBytes = 0L, cacheBytes = 0L))
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _importActivity = MutableStateFlow<ImportActivity?>(null)
    val importActivity: StateFlow<ImportActivity?> = _importActivity.asStateFlow()

    private val _importInProgress = MutableStateFlow(false)
    val importInProgress: StateFlow<Boolean> = _importInProgress.asStateFlow()
    val isImporting: StateFlow<Boolean> = importInProgress

    private val _importProgress = MutableStateFlow(0f)
    val importProgress: StateFlow<Float> = _importProgress.asStateFlow()

    private val _importJustCompleted = MutableStateFlow(false)
    val importJustCompleted: StateFlow<Boolean> = _importJustCompleted.asStateFlow()

    private val _activeBookId = MutableStateFlow<String?>(null)
    val activeBookId: StateFlow<String?> = _activeBookId.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncFeedbackMessage = MutableStateFlow<String?>(null)
    val syncFeedbackMessage: StateFlow<String?> = _syncFeedbackMessage.asStateFlow()

    private data class PendingBookProgress(
        val chapterIndex: Int,
        val page: Int,
        val pageCount: Int,
        val progress: Double,
        val lastReadAtMillis: Long,
        val chapterPageCounts: Map<Int, Int> = emptyMap(),
    )

    private var importJob: Job? = null
    private var importClearJob: Job? = null
    private val importSessionSeq = AtomicInteger(0)
    private var styleSaveJob: Job? = null
    private var pendingStyleToSave: ReadingStyle? = null
    private var progressCommitJob: Job? = null
    private var booksSaveJob: Job? = null
    private var pendingBooksToSave: List<BookEntry>? = null
    private var lastClosedBookId: String? = null
    private var lastBookCloseNanoMs: Long = 0L
    private val patchLock = Any()
    private val pendingBookPatches = LinkedHashMap<String, PendingBookProgress>()
    private val persistScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        loadInitialState()
    }

    fun loadInitialState() {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true
            try {
                val loadedStyle = repository.loadStyle()
                val loadedFonts = repository.loadFonts()
                val loadedShelves = repository.loadShelves()
                val loadedStats = repository.loadReadingStats()
                val loadedCloud = repository.loadCloudSyncSettings()
                val loadedBooks = deduplicateBooks(repository.loadBooks())

                _style.value = loadedStyle
                _fonts.value = loadedFonts
                _shelves.value = loadedShelves
                _readingStats.value = loadedStats
                _dailyReadingTotals.value = computeDailyTotals(loadedStats)
                _cloudSyncSettings.value = loadedCloud
                _books.value = loadedBooks
                _storageStats.value = repository.calculateStorageStats()
                _errorMessage.value = null
            } catch (t: Throwable) {
                _errorMessage.value = "加载数据失败：${t.message ?: "未知错误"}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun toggleDayNight(isSystemDark: Boolean = false) {
        val current = _style.value
        val nextMode = current.nextCycleBrightnessMode(isSystemDark)
        updateStyle(current.copy(brightnessMode = nextMode))
    }

    fun updateStyle(newStyle: ReadingStyle) {
        val previous = _style.value
        if (previous == newStyle) return

        val paginationChanged =
            previous.fontSize != newStyle.fontSize ||
                previous.fontWeightValue != newStyle.fontWeightValue ||
                previous.lineHeight != newStyle.lineHeight ||
                previous.paragraphSpacing != newStyle.paragraphSpacing ||
                previous.letterSpacing != newStyle.letterSpacing ||
                previous.pageMargin != newStyle.pageMargin ||
                previous.verticalMargin != newStyle.verticalMargin ||
                previous.firstLineIndent != newStyle.firstLineIndent ||
                previous.fontPath != newStyle.fontPath

        _style.value = newStyle
        pendingStyleToSave = newStyle
        styleSaveJob?.cancel()
        styleSaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(150L)
            val snapshot = pendingStyleToSave ?: return@launch
            pendingStyleToSave = null
            repository.saveStyle(snapshot)
        }

        if (paginationChanged && _books.value.isNotEmpty()) {
            val baseBooks = consumePendingPatchesInto(_books.value)
            val resetBooks = baseBooks.map { book ->
                book.copy(
                    chapters = book.chapters.map { ch -> ch.copy(cachedPageCount = null) }
                )
            }
            _books.value = resetBooks
            persistBooksDebounced(resetBooks)
        }
    }

    fun recordReadingDuration(
        bookId: String,
        seconds: Int,
        dateKey: String = todayKey(),
    ) {
        if (seconds <= 0 || bookId.isBlank()) return
        val currentStats = _readingStats.value
        val bookMap = LinkedHashMap<String, Int>(currentStats[bookId] ?: emptyMap())
        bookMap[dateKey] = (bookMap[dateKey] ?: 0) + seconds

        val nextStats = LinkedHashMap<String, Map<String, Int>>(currentStats)
        nextStats[bookId] = bookMap
        _readingStats.value = nextStats
        _dailyReadingTotals.value = computeDailyTotals(nextStats)

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveReadingStats(nextStats)
        }
    }

    fun addReadingSeconds(seconds: Int, bookId: String) {
        recordReadingDuration(bookId = bookId, seconds = seconds)
    }

    fun readingSecondsFor(date: LocalDate): Int {
        val key = formatDateKey(date)
        return _dailyReadingTotals.value[key] ?: 0
    }

    fun readingSecondsFor(date: LocalDate, bookId: String?): Int {
        if (bookId == null) return readingSecondsFor(date)
        val key = formatDateKey(date)
        return _readingStats.value[bookId]?.get(key) ?: 0
    }

    fun readingSecondsForBook(bookId: String): Int {
        return _readingStats.value[bookId]?.values?.sum() ?: 0
    }

    fun openBook(book: BookEntry) {
        openBook(book.id)
    }

    fun openBook(bookId: String) {
        if (bookId.isBlank()) return
        _activeBookId.value = bookId
        val nowMillis = System.currentTimeMillis()
        val staged = synchronized(patchLock) {
            val existingPatch = pendingBookPatches[bookId]
            val target = _books.value.firstOrNull { it.id == bookId } ?: return@synchronized false
            pendingBookPatches[bookId] = PendingBookProgress(
                chapterIndex = existingPatch?.chapterIndex ?: target.currentChapterIndex,
                page = existingPatch?.page ?: target.currentPage,
                pageCount = existingPatch?.pageCount ?: target.pageCount,
                progress = existingPatch?.progress ?: target.progress,
                lastReadAtMillis = nowMillis,
                chapterPageCounts = existingPatch?.chapterPageCounts ?: emptyMap(),
            )
            true
        }
        if (staged) {
            progressCommitJob?.cancel()
            // Delay _books mutation + hero re-sort past the ~450ms Hero open animation so Frame 0-30 never hitches.
            progressCommitJob = viewModelScope.launch {
                delay(650L)
                commitPendingBookPatches(bookId = null, immediateDiskWrite = false)
            }
        }
    }

    fun closeActiveBook(bookId: String? = null) {
        val targetId = bookId ?: _activeBookId.value
        lastClosedBookId = targetId
        lastBookCloseNanoMs = System.nanoTime() / 1_000_000L
        if (bookId == null || _activeBookId.value == bookId) {
            _activeBookId.value = null
        }
        val hasPending = synchronized(patchLock) {
            if (targetId != null) {
                pendingBookPatches.containsKey(targetId)
            } else {
                pendingBookPatches.isNotEmpty()
            }
        }
        if (hasPending) {
            progressCommitJob?.cancel()
            // Coalesce closeActiveBook + Reader dispose updateBookProgress past the ~220ms
            // Hero / SeriesModal close settle so Back #1 Frame 0 never mutates _books or causes
            // a second late +900ms re-sort/layout spike.
            progressCommitJob = viewModelScope.launch {
                delay(420L)
                commitPendingBookPatches(bookId = null, immediateDiskWrite = false, debounceDelayMs = 1200L)
            }
        }
    }

    fun updateBookProgress(
        bookId: String,
        chapterIndex: Int,
        page: Int,
        pageCount: Int,
        progress: Double = -1.0,
    ) {
        val currentBooks = _books.value
        val stored = currentBooks.firstOrNull { it.id == bookId } ?: return
        val existingPatch = synchronized(patchLock) { pendingBookPatches[bookId] }
        val latest = if (existingPatch != null) applyProgressPatch(stored, existingPatch) else stored

        val safePageCount = pageCount.coerceAtLeast(1)
        val safeChapterIndex = if (latest.chapters.isEmpty()) {
            0
        } else {
            chapterIndex.coerceIn(0, latest.chapters.size - 1)
        }
        val safePage = page.coerceIn(0, safePageCount - 1)

        val resolvedProgress = if (progress >= 0.0 && !progress.isNaN()) {
            progress.coerceIn(0.0, 1.0)
        } else {
            computeReadingProgress(
                chapterCount = latest.chapters.size,
                chapterIndex = safeChapterIndex,
                page = safePage,
                pageCount = safePageCount,
            )
        }

        val cachedPageCountChanged = latest.chapters.isNotEmpty() &&
            latest.chapters[safeChapterIndex].cachedPageCount != safePageCount
        val progressChanged = abs(latest.progress - resolvedProgress) >= 0.001 ||
            (resolvedProgress >= 1.0 && latest.progress < 1.0)
        val nowMillis = System.currentTimeMillis()
        val maxKnownLastRead = currentBooks.maxOfOrNull { it.lastReadAtMillis ?: 0L } ?: 0L
        val isCurrentlyTopRecent = latest.lastReadAtMillis != null && latest.lastReadAtMillis >= maxKnownLastRead
        val samePosition =
            latest.currentChapterIndex == safeChapterIndex &&
                latest.currentPage == safePage &&
                latest.pageCount == safePageCount &&
                !progressChanged &&
                !cachedPageCountChanged
        val lastReadFresh = latest.lastReadAtMillis != null &&
            (nowMillis - latest.lastReadAtMillis) < 15_000L
        if (samePosition && lastReadFresh && isCurrentlyTopRecent) return

        val isActiveOpen = _activeBookId.value == bookId
        val withinCloseCoalesceWindow = !isActiveOpen &&
            (lastClosedBookId == null || lastClosedBookId == bookId) &&
            ((System.nanoTime() / 1_000_000L) - lastBookCloseNanoMs) in 0L..650L
        val isMajorTransition =
            !isActiveOpen && (
                latest.currentChapterIndex != safeChapterIndex ||
                    stored.lastReadAtMillis == null ||
                    !isCurrentlyTopRecent
                )
        synchronized(patchLock) {
            val prev = pendingBookPatches[bookId]
            val prevCounts = prev?.chapterPageCounts ?: emptyMap()
            val nextCounts = if (cachedPageCountChanged) {
                prevCounts + (safeChapterIndex to safePageCount)
            } else {
                prevCounts
            }
            val effectiveLastReadAt = maxOf(nowMillis, prev?.lastReadAtMillis ?: 0L)
            pendingBookPatches[bookId] = PendingBookProgress(
                chapterIndex = safeChapterIndex,
                page = safePage,
                pageCount = safePageCount,
                progress = resolvedProgress,
                lastReadAtMillis = effectiveLastReadAt,
                chapterPageCounts = nextCounts,
            )
        }

        if (isMajorTransition && !withinCloseCoalesceWindow) {
            progressCommitJob?.cancel()
            commitPendingBookPatches(bookId = null, immediateDiskWrite = false)
        } else {
            progressCommitJob?.cancel()
            progressCommitJob = viewModelScope.launch {
                delay(
                    when {
                        withinCloseCoalesceWindow -> 240L
                        isActiveOpen -> 650L
                        else -> 320L
                    },
                )
                commitPendingBookPatches(bookId = null, immediateDiskWrite = false, debounceDelayMs = 1200L)
            }
        }
    }

    fun updateBookProgress(
        book: BookEntry,
        chapterIndex: Int,
        page: Int,
        pageCount: Int,
        progress: Double = -1.0,
    ) {
        updateBookProgress(
            bookId = book.id,
            chapterIndex = chapterIndex,
            page = page,
            pageCount = pageCount,
            progress = progress,
        )
    }

    fun cacheChapterPageCount(
        bookId: String,
        chapterIndex: Int,
        pageCount: Int,
    ) {
        val currentBooks = _books.value
        val stored = currentBooks.firstOrNull { it.id == bookId } ?: return
        if (chapterIndex !in stored.chapters.indices) return
        val safePageCount = pageCount.coerceAtLeast(1)

        val changed = synchronized(patchLock) {
            val prev = pendingBookPatches[bookId]
            val latest = if (prev != null) applyProgressPatch(stored, prev) else stored
            if (latest.chapters[chapterIndex].cachedPageCount == safePageCount) {
                false
            } else {
                val baseCounts = prev?.chapterPageCounts ?: emptyMap()
                pendingBookPatches[bookId] = PendingBookProgress(
                    chapterIndex = prev?.chapterIndex ?: stored.currentChapterIndex,
                    page = prev?.page ?: stored.currentPage,
                    pageCount = prev?.pageCount ?: stored.pageCount,
                    progress = prev?.progress ?: stored.progress,
                    lastReadAtMillis = prev?.lastReadAtMillis ?: stored.lastReadAtMillis ?: System.currentTimeMillis(),
                    chapterPageCounts = baseCounts + (chapterIndex to safePageCount),
                )
                true
            }
        }
        if (!changed) return

        progressCommitJob?.cancel()
        progressCommitJob = viewModelScope.launch {
            delay(900L)
            commitPendingBookPatches(bookId = null, immediateDiskWrite = false)
        }
    }

    fun flushPendingBookProgress(
        bookId: String? = null,
        debounceDelayMs: Long = 450L,
    ) {
        if (bookId == null) {
            progressCommitJob?.cancel()
        }
        // Keep in-memory _books immediately consistent for sorting/badges while coalescing heavy
        // JSON + SharedPreferences disk writes off rapid enter/exit critical paths (flushed eagerly on onPause).
        commitPendingBookPatches(
            bookId = bookId,
            immediateDiskWrite = false,
            debounceDelayMs = debounceDelayMs,
        )
    }

    fun flushPendingPersistence() {
        progressCommitJob?.cancel()
        commitPendingBookPatches(bookId = null, immediateDiskWrite = true)

        val styleSnapshot = pendingStyleToSave
        if (styleSnapshot != null) {
            pendingStyleToSave = null
            styleSaveJob?.cancel()
            persistScope.launch {
                repository.saveStyle(styleSnapshot)
            }
        }

        val booksSnapshot = pendingBooksToSave
        if (booksSnapshot != null) {
            pendingBooksToSave = null
            booksSaveJob?.cancel()
            persistScope.launch {
                repository.saveBooks(booksSnapshot)
            }
        }
    }

    override fun onCleared() {
        flushPendingPersistence()
        super.onCleared()
    }

    fun addBookmark(bookId: String, bookmark: BookBookmark) {
        val currentBooks = consumePendingPatchesInto(_books.value)
        val index = currentBooks.indexOfFirst { it.id == bookId }
        if (index < 0) return

        val stored = currentBooks[index]
        val filtered = stored.bookmarks.filterNot {
            it.id == bookmark.id ||
                (it.chapterIndex == bookmark.chapterIndex && it.page == bookmark.page)
        }
        val nextBookmarks = listOf(bookmark) + filtered
        val updatedBook = stored.copy(bookmarks = nextBookmarks)
        val nextBooks = currentBooks.toMutableList().apply {
            this[index] = updatedBook
        }
        _books.value = nextBooks
        persistBooksAsync(nextBooks)
    }

    fun removeBookmark(bookId: String, bookmarkId: String) {
        if (bookmarkId.isBlank()) return
        val currentBooks = consumePendingPatchesInto(_books.value)
        val index = currentBooks.indexOfFirst { it.id == bookId }
        if (index < 0) return

        val stored = currentBooks[index]
        val nextBookmarks = stored.bookmarks.filterNot { it.id == bookmarkId }
        if (nextBookmarks.size == stored.bookmarks.size) return

        val updatedBook = stored.copy(bookmarks = nextBookmarks)
        val nextBooks = currentBooks.toMutableList().apply {
            this[index] = updatedBook
        }
        _books.value = nextBooks
        persistBooksAsync(nextBooks)
    }

    fun importBook(path: String) {
        importFiles(listOf(path))
    }

    fun importBooks(paths: List<String>) {
        importFiles(paths)
    }

    fun importFolder(directory: File) {
        if (!directory.exists() || !directory.isDirectory) return
        val collected = directory.walkTopDown().maxDepth(6)
            .filter { file ->
                val lower = file.name.lowercase()
                file.isFile && file.length() > 0L && (lower.endsWith(".epub") || lower.endsWith(".txt"))
            }
            .map { it.absolutePath }
            .toList()
        importFiles(collected)
    }

    fun importFiles(paths: List<String>) {
        val validFiles = paths
            .map { File(it) }
            .filter { file ->
                val lower = file.name.lowercase()
                file.exists() && file.isFile && (lower.endsWith(".epub") || lower.endsWith(".txt"))
            }
        if (validFiles.isEmpty()) {
            if (paths.isNotEmpty()) {
                _errorMessage.value = "未找到可导入的 EPUB 或 TXT 书籍文件"
            }
            return
        }

        val previousImportJob = importJob
        importClearJob?.cancel()
        val sessionToken = importSessionSeq.incrementAndGet()
        val total = validFiles.size
        _importJustCompleted.value = false
        _importInProgress.value = true
        _importProgress.value = 0.05f
        _importActivity.value = ImportActivity(
            title = "正在导入 (0/$total)",
            detail = validFiles.first().name,
            active = true,
            failed = false,
            current = 0,
            total = total,
            smoothProgress = 0.05f,
        )

        importJob = viewModelScope.launch(Dispatchers.IO) {
            previousImportJob?.join()
            if (importSessionSeq.get() != sessionToken) return@launch
            var importedCount = 0
            try {
                for ((idx, file) in validFiles.withIndex()) {
                    val step = idx + 1
                    val sliceStart = 0.05f + (idx.toFloat() / total.toFloat()) * 0.90f
                    val sliceEnd = 0.05f + (step.toFloat() / total.toFloat()) * 0.90f
                    val sliceSpan = (sliceEnd - sliceStart).coerceAtLeast(0.01f)
                    val parseStartProgress = (sliceStart + sliceSpan * 0.10f).coerceIn(0.05f, 0.95f)
                    val parseCeiling = (sliceStart + sliceSpan * 0.86f).coerceIn(parseStartProgress, 0.96f)

                    if (parseStartProgress > _importProgress.value) {
                        _importProgress.value = parseStartProgress
                    }
                    _importActivity.value = ImportActivity(
                        title = "正在导入 ($step/$total)",
                        detail = file.name,
                        active = true,
                        failed = false,
                        current = idx,
                        total = total,
                        smoothProgress = _importProgress.value,
                    )

                    val expectedDurationMs = (320.0 + (file.length().coerceAtLeast(0L) / 3500.0)).coerceIn(320.0, 3200.0)
                    val startNs = System.nanoTime()
                    val tickerJob = launch(Dispatchers.Default) {
                        while (isActive && importSessionSeq.get() == sessionToken) {
                            delay(45L)
                            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000.0
                            val curve = (1.0 - exp(-elapsedMs / expectedDurationMs)).toFloat().coerceIn(0f, 0.98f)
                            val nextProgress = (parseStartProgress + (parseCeiling - parseStartProgress) * curve)
                                .coerceIn(0.05f, 0.96f)
                            if (nextProgress > _importProgress.value) {
                                _importProgress.value = nextProgress
                                _importActivity.value = _importActivity.value?.copy(smoothProgress = nextProgress)
                            }
                        }
                    }

                    val importedBook = try {
                        repository.importBookFile(file)
                    } finally {
                        tickerJob.cancel()
                    }

                    val storeProgress = (sliceStart + sliceSpan * 0.92f).coerceIn(0.05f, 0.97f)
                    if (storeProgress > _importProgress.value) {
                        _importProgress.value = storeProgress
                    }
                    if (importedBook != null) {
                        val added = storeImportedBook(importedBook)
                        if (added) {
                            importedCount += 1
                        }
                    }

                    val fileDoneProgress = sliceEnd.coerceIn(0.05f, 0.98f)
                    if (fileDoneProgress > _importProgress.value) {
                        _importProgress.value = fileDoneProgress
                    }
                    _importActivity.value = ImportActivity(
                        title = "正在导入 ($step/$total)",
                        detail = file.name,
                        active = true,
                        failed = false,
                        current = step,
                        total = total,
                        smoothProgress = _importProgress.value,
                    )
                }
                _storageStats.value = repository.calculateStorageStats()
                val detailMsg = if (importedCount == 0) {
                    "所选书籍已存在"
                } else {
                    "已导入 $importedCount 本书"
                }
                finishImportBanner(detailMsg, failed = false, total = total, sessionToken = sessionToken)
            } catch (t: Throwable) {
                finishImportBanner("导入失败", failed = true, total = total, sessionToken = sessionToken)
                _errorMessage.value = "导入书籍失败：${t.message ?: "未知错误"}"
            }
        }
    }

    fun deleteBooks(ids: Set<String>) {
        if (ids.isEmpty()) return
        lateinit var remainingBooks: List<BookEntry>
        lateinit var toDelete: List<BookEntry>
        synchronized(patchLock) {
            for (id in ids) {
                pendingBookPatches.remove(id)
            }
            val currentBooks = consumePendingPatchesLocked(_books.value)
            toDelete = currentBooks.filter { it.id in ids }
            if (toDelete.isEmpty()) return
            remainingBooks = currentBooks.filterNot { it.id in ids }
            _books.value = remainingBooks
        }

        val nextStats = LinkedHashMap<String, Map<String, Int>>(_readingStats.value)
        var statsChanged = false
        for (id in ids) {
            if (nextStats.remove(id) != null) {
                statsChanged = true
            }
        }
        if (statsChanged) {
            _readingStats.value = nextStats
            _dailyReadingTotals.value = computeDailyTotals(nextStats)
        }

        booksSaveJob?.cancel()
        pendingBooksToSave = null
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveBooks(remainingBooks)
            if (statsChanged) {
                repository.saveReadingStats(nextStats)
            }
            for (book in toDelete) {
                repository.deleteBookFiles(book)
            }
            _storageStats.value = repository.calculateStorageStats()
        }
    }

    fun addShelf(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed == DEFAULT_SHELF_NAME) return
        val current = _shelves.value
        if (current.contains(trimmed)) return

        val next = current + trimmed
        _shelves.value = next
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveShelves(next)
        }
    }

    fun deleteShelf(name: String) {
        val trimmed = name.trim()
        val currentShelves = _shelves.value
        if (trimmed.isEmpty() || !currentShelves.contains(trimmed)) return

        val nextShelves = currentShelves.filterNot { it == trimmed }
        _shelves.value = nextShelves

        val nextBooks = synchronized(patchLock) {
            val currentBooks = consumePendingPatchesLocked(_books.value)
            currentBooks.map { book ->
                if (book.shelfName == trimmed) book.copy(shelfName = null) else book
            }.also { _books.value = it }
        }

        booksSaveJob?.cancel()
        pendingBooksToSave = null
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveShelves(nextShelves)
            repository.saveBooks(nextBooks)
        }
    }

    fun moveBooksToShelf(ids: Set<String>, shelfName: String?) {
        if (ids.isEmpty()) return
        val normalizedShelf = shelfName?.trim()?.takeIf {
            it.isNotEmpty() && it != DEFAULT_SHELF_NAME
        }
        val nextBooks = synchronized(patchLock) {
            consumePendingPatchesLocked(_books.value).map { book ->
                if (book.id in ids) book.copy(shelfName = normalizedShelf) else book
            }.also { _books.value = it }
        }
        persistBooksAsync(nextBooks)
    }

    fun groupBooksAsSeries(ids: Set<String>, seriesName: String) {
        if (ids.isEmpty()) return
        val trimmed = seriesName.trim().takeIf { it.isNotEmpty() }
        val nextBooks = synchronized(patchLock) {
            consumePendingPatchesLocked(_books.value).map { book ->
                if (book.id in ids) {
                    book.copy(seriesOverride = trimmed)
                } else {
                    book
                }
            }.also { _books.value = it }
        }
        persistBooksAsync(nextBooks)
    }

    fun removeBookFromSeries(bookId: String) {
        if (bookId.isBlank()) return
        val nextBooks = synchronized(patchLock) {
            consumePendingPatchesLocked(_books.value).map { book ->
                if (book.id == bookId) {
                    book.copy(seriesOverride = null, seriesOrder = null)
                } else {
                    book
                }
            }.also { _books.value = it }
        }
        persistBooksAsync(nextBooks)
    }

    fun reorderSeriesBooks(orderedIds: List<String>) {
        if (orderedIds.isEmpty()) return
        val orderMap = orderedIds.withIndex().associate { (index, id) -> id to index }
        val nextBooks = synchronized(patchLock) {
            consumePendingPatchesLocked(_books.value).map { book ->
                val newOrder = orderMap[book.id]
                if (newOrder != null) book.copy(seriesOrder = newOrder) else book
            }.also { _books.value = it }
        }
        persistBooksAsync(nextBooks)
    }

    fun updateBookMeta(
        bookId: String,
        title: String,
        author: String,
        coverFile: File? = null,
    ) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return
        val trimmedAuthor = author.trim().ifEmpty { "未知作者" }

        viewModelScope.launch(Dispatchers.IO) {
            val currentBooks = _books.value
            val index = currentBooks.indexOfFirst { it.id == bookId }
            if (index < 0) return@launch
            val stored = currentBooks[index]

            val newCoverPath = if (coverFile != null && coverFile.exists()) {
                repository.saveCustomCover(stored.bookDir, coverFile) ?: stored.coverPath
            } else {
                stored.coverPath
            }

            val nextBooksToSave = synchronized(patchLock) {
                val baseBooks = consumePendingPatchesLocked(_books.value).toMutableList()
                val targetIdx = baseBooks.indexOfFirst { it.id == bookId }
                if (targetIdx >= 0) {
                    baseBooks[targetIdx] = baseBooks[targetIdx].copy(
                        title = trimmedTitle,
                        author = trimmedAuthor,
                        coverPath = newCoverPath,
                    )
                    _books.value = baseBooks
                    baseBooks
                } else {
                    null
                }
            }
            if (nextBooksToSave != null) {
                booksSaveJob?.cancel()
                pendingBooksToSave = null
                repository.saveBooks(nextBooksToSave)
            }
        }
    }

    fun importFontFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val imported = repository.importFontFile(file)
                if (imported == null) {
                    _errorMessage.value = "不支持的字体文件格式（仅支持 TTF/OTF）"
                    return@launch
                }
                val existing = _fonts.value.filterNot {
                    it.path == imported.path || it.name.equals(imported.name, ignoreCase = true)
                }
                val nextFonts = existing + imported
                _fonts.value = nextFonts
                repository.saveFonts(nextFonts)

                val nextStyle = _style.value.copy(
                    fontName = imported.name,
                    fontPath = imported.path,
                )
                pendingStyleToSave = null
                styleSaveJob?.cancel()
                _style.value = nextStyle
                repository.saveStyle(nextStyle)
                _errorMessage.value = null
            } catch (t: Throwable) {
                _errorMessage.value = "导入字体失败：${t.message ?: "未知错误"}"
            }
        }
    }

    fun deleteFont(font: ImportedFont) {
        val nextFonts = _fonts.value.filterNot { it.path == font.path && it.name == font.name }
        _fonts.value = nextFonts

        var currentStyle = _style.value
        var styleChanged = false
        if (currentStyle.fontPath == font.path || currentStyle.fontName == font.name) {
            currentStyle = currentStyle.copy(fontName = null, fontPath = null)
            styleChanged = true
        }
        if (currentStyle.appFontPath == font.path || currentStyle.appFontName == font.name) {
            currentStyle = currentStyle.copy(appFontName = null, appFontPath = null)
            styleChanged = true
        }
        if (styleChanged) {
            pendingStyleToSave = null
            styleSaveJob?.cancel()
            _style.value = currentStyle
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveFonts(nextFonts)
            if (styleChanged) {
                repository.saveStyle(currentStyle)
            }
            repository.deleteFontFile(font)
        }
    }

    fun updateCloudSyncSettings(settings: CloudSyncSettings) {
        _cloudSyncSettings.value = settings
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveCloudSyncSettings(settings)
        }
    }

    fun syncToCloud() {
        if (_isSyncing.value) return
        val settings = _cloudSyncSettings.value
        if (!settings.configured) {
            _errorMessage.value = "请先填写 WebDAV 地址、账号和密码"
            return
        }
        _isSyncing.value = true
        _syncFeedbackMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val styleSnapshot = _style.value
                pendingStyleToSave = null
                styleSaveJob?.cancel()

                val booksSnapshot = synchronized(patchLock) {
                    consumePendingPatchesLocked(_books.value).also { _books.value = it }
                }
                booksSaveJob?.cancel()
                pendingBooksToSave = null

                repository.saveStyle(styleSnapshot)
                repository.saveBooks(booksSnapshot)
                repository.saveReadingStats(_readingStats.value)
                repository.saveShelves(_shelves.value)

                repository.uploadCloudSync(
                    settings = settings,
                    books = booksSnapshot,
                    shelves = _shelves.value,
                    readingStats = _readingStats.value,
                )
                val updatedSettings = settings.copy(
                    enabled = true,
                    lastUploadAtMillis = System.currentTimeMillis(),
                )
                _cloudSyncSettings.value = updatedSettings
                repository.saveCloudSyncSettings(updatedSettings)
                _errorMessage.value = null
                _syncFeedbackMessage.value = "已成功上传同步数据至云端"
            } catch (t: Throwable) {
                _errorMessage.value = t.message ?: "上传同步数据失败"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun syncFromCloud() {
        if (_isSyncing.value) return
        val settings = _cloudSyncSettings.value
        if (!settings.configured) {
            _errorMessage.value = "请先填写 WebDAV 地址、账号和密码"
            return
        }
        _isSyncing.value = true
        _syncFeedbackMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val payload = repository.downloadCloudSync(settings)
                val localBooks = consumePendingPatchesInto(_books.value)
                val mergeResult = repository.mergeCloudSyncPayload(
                    payload = payload,
                    localBooks = localBooks,
                    localShelves = _shelves.value,
                    localReadingStats = _readingStats.value,
                )
                if (mergeResult.shelvesChanged) {
                    _shelves.value = mergeResult.shelves
                    repository.saveShelves(mergeResult.shelves)
                }
                if (mergeResult.booksChanged) {
                    booksSaveJob?.cancel()
                    pendingBooksToSave = null
                    _books.value = mergeResult.books
                    repository.saveBooks(mergeResult.books)
                }
                if (mergeResult.readingStatsChanged) {
                    _readingStats.value = mergeResult.readingStats
                    _dailyReadingTotals.value = computeDailyTotals(mergeResult.readingStats)
                    repository.saveReadingStats(mergeResult.readingStats)
                }
                val updatedSettings = _cloudSyncSettings.value.copy(
                    enabled = true,
                    lastDownloadAtMillis = System.currentTimeMillis(),
                )
                _cloudSyncSettings.value = updatedSettings
                repository.saveCloudSyncSettings(updatedSettings)
                _errorMessage.value = null
                _syncFeedbackMessage.value = "已从云端同步并合并最新阅读数据"
            } catch (t: Throwable) {
                _errorMessage.value = t.message ?: "下载同步数据失败"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.clearCache()
                _storageStats.value = repository.calculateStorageStats()
                _errorMessage.value = null
                _syncFeedbackMessage.value = "缓存已清理"
            } catch (t: Throwable) {
                _errorMessage.value = "清理缓存失败：${t.message ?: "未知错误"}"
            }
        }
    }

    fun clearSyncFeedback() {
        _syncFeedbackMessage.value = null
    }

    fun refreshStorageStats() {
        viewModelScope.launch(Dispatchers.IO) {
            _storageStats.value = repository.calculateStorageStats()
        }
    }

    suspend fun calculateStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val stats = repository.calculateStorageStats()
        _storageStats.value = stats
        stats
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun todayKey(): String = formatDateKey(LocalDate.now())

    private fun storeImportedBook(book: BookEntry): Boolean {
        val nextBooks = synchronized(patchLock) {
            val currentBooks = consumePendingPatchesLocked(_books.value)
            val newKey = BookRepository.duplicateKey(book)
            val isDuplicate = currentBooks.any { BookRepository.duplicateKey(it) == newKey }
            if (isDuplicate) {
                null
            } else {
                (listOf(book) + currentBooks).also { _books.value = it }
            }
        }
        if (nextBooks == null) {
            repository.deleteBookFiles(book)
            return false
        }
        booksSaveJob?.cancel()
        pendingBooksToSave = null
        repository.saveBooks(nextBooks)
        return true
    }

    private fun deduplicateBooks(books: List<BookEntry>): List<BookEntry> {
        val seen = HashSet<String>()
        val result = mutableListOf<BookEntry>()
        for (book in books) {
            if (seen.add(BookRepository.duplicateKey(book))) {
                result.add(book)
            }
        }
        if (result.size != books.size) {
            repository.saveBooks(result)
        }
        return result
    }

    private fun finishImportBanner(
        detail: String,
        failed: Boolean,
        total: Int,
        sessionToken: Int,
    ) {
        if (importSessionSeq.get() != sessionToken) return
        importClearJob?.cancel()
        if (failed) {
            _importInProgress.value = false
            _importJustCompleted.value = false
            _importProgress.value = 0f
            _importActivity.value = ImportActivity(
                title = "导入失败",
                detail = detail,
                active = false,
                failed = true,
                current = total,
                total = total,
                smoothProgress = 0f,
            )
            importClearJob = viewModelScope.launch {
                delay(1800L)
                if (importSessionSeq.get() == sessionToken) {
                    _importActivity.value = null
                }
            }
        } else {
            _importProgress.value = 1.0f
            _importActivity.value = ImportActivity(
                title = "导入完成",
                detail = detail,
                active = false,
                failed = false,
                current = total,
                total = total,
                smoothProgress = 1.0f,
            )
            importClearJob = viewModelScope.launch {
                delay(120L)
                if (importSessionSeq.get() != sessionToken) return@launch
                _importInProgress.value = false
                _importJustCompleted.value = true
                delay(1500L)
                if (importSessionSeq.get() == sessionToken) {
                    _importJustCompleted.value = false
                    _importProgress.value = 0f
                    _importActivity.value = null
                }
            }
        }
    }

    private fun applyProgressPatch(book: BookEntry, patch: PendingBookProgress): BookEntry {
        val updatedChapters = if (patch.chapterPageCounts.isEmpty() || book.chapters.isEmpty()) {
            book.chapters
        } else {
            var anyChanged = false
            val mapped = book.chapters.mapIndexed { idx, ch ->
                val targetCount = patch.chapterPageCounts[idx]
                if (targetCount != null && ch.cachedPageCount != targetCount) {
                    anyChanged = true
                    ch.copy(cachedPageCount = targetCount)
                } else {
                    ch
                }
            }
            if (anyChanged) mapped else book.chapters
        }
        return book.copy(
            chapters = updatedChapters,
            currentChapterIndex = patch.chapterIndex,
            currentPage = patch.page,
            pageCount = patch.pageCount,
            progress = patch.progress,
            lastReadAtMillis = patch.lastReadAtMillis,
        )
    }

    private fun consumePendingPatchesLocked(baseBooks: List<BookEntry>, targetBookId: String? = null): List<BookEntry> {
        if (pendingBookPatches.isEmpty()) return baseBooks
        var changed = false
        val result = baseBooks.map { book ->
            val shouldApply = targetBookId == null || targetBookId == book.id
            val patch = if (shouldApply) pendingBookPatches.remove(book.id) else null
            if (patch != null) {
                changed = true
                applyProgressPatch(book, patch)
            } else {
                book
            }
        }
        if (targetBookId == null) {
            pendingBookPatches.clear()
        }
        return if (changed) result else baseBooks
    }

    private fun consumePendingPatchesInto(baseBooks: List<BookEntry>): List<BookEntry> =
        synchronized(patchLock) { consumePendingPatchesLocked(baseBooks) }

    private fun commitPendingBookPatches(
        bookId: String?,
        immediateDiskWrite: Boolean,
        debounceDelayMs: Long = 450L,
    ) {
        val nextBooks = synchronized(patchLock) {
            val currentBooks = _books.value
            val patched = consumePendingPatchesLocked(currentBooks, targetBookId = bookId)
            if (patched === currentBooks) return
            _books.value = patched
            patched
        }
        if (immediateDiskWrite) {
            persistBooksAsync(nextBooks)
        } else {
            persistBooksDebounced(nextBooks, delayMs = debounceDelayMs)
        }
    }

    private fun persistBooksDebounced(
        booksSnapshot: List<BookEntry>,
        delayMs: Long = 450L,
    ) {
        pendingBooksToSave = booksSnapshot
        booksSaveJob?.cancel()
        booksSaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(delayMs)
            val snapshot = pendingBooksToSave ?: return@launch
            pendingBooksToSave = null
            repository.saveBooks(snapshot)
        }
    }

    private fun persistBooksAsync(booksSnapshot: List<BookEntry>) {
        pendingBooksToSave = null
        booksSaveJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveBooks(booksSnapshot)
        }
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        fun sortRecentBooks(books: List<BookEntry>): List<BookEntry> {
            return books.sortedWith { a, b ->
                val aTime = a.lastReadAtMillis ?: a.importedAtMillis
                val bTime = b.lastReadAtMillis ?: b.importedAtMillis
                val cmp = bTime.compareTo(aTime)
                if (cmp != 0) cmp else b.progress.compareTo(a.progress)
            }
        }

        fun selectHeroBook(books: List<BookEntry>): BookEntry? {
            if (books.isEmpty()) return null
            val sorted = sortRecentBooks(books)
            return sorted.firstOrNull { it.lastReadAtMillis != null }
                ?: sorted.firstOrNull { it.progress > 0.0 }
                ?: sorted.firstOrNull()
        }

        fun formatDateKey(date: LocalDate): String = date.format(DATE_FORMATTER)

        fun computeDailyTotals(stats: Map<String, Map<String, Int>>): Map<String, Int> {
            val totals = LinkedHashMap<String, Int>()
            for (bookStats in stats.values) {
                for ((dateKey, seconds) in bookStats) {
                    if (seconds > 0) {
                        totals[dateKey] = (totals[dateKey] ?: 0) + seconds
                    }
                }
            }
            return totals
        }

        fun computeReadingProgress(
            chapterCount: Int,
            chapterIndex: Int,
            page: Int,
            pageCount: Int,
        ): Double {
            if (chapterCount <= 0) return 0.0
            val safeChapter = chapterIndex.coerceIn(0, chapterCount - 1)
            val safePageCount = pageCount.coerceAtLeast(1)
            val safePage = page.coerceIn(0, safePageCount - 1)
            val isLastChapter = safeChapter == chapterCount - 1
            if (safePageCount <= 1) {
                return if (isLastChapter) 1.0 else safeChapter.toDouble() / chapterCount.toDouble()
            }
            val pageProgress = safePage.toDouble() / (safePageCount - 1).toDouble()
            return ((safeChapter.toDouble() + pageProgress) / chapterCount.toDouble()).coerceIn(0.0, 1.0)
        }
    }
}
