package com.squartor.reader.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.squartor.reader.model.BookBookmark
import com.squartor.reader.model.BookEntry
import com.squartor.reader.model.CloudSyncSettings
import com.squartor.reader.model.ImportedFont
import com.squartor.reader.model.ReadingStyle
import com.squartor.reader.model.StorageStats
import com.squartor.reader.model.formatIso8601
import com.squartor.reader.model.parseIso8601
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.ObjectInputStream
import java.net.HttpURLConnection
import java.net.ProtocolException
import java.net.URI
import java.net.URL
import kotlin.math.max

data class CloudSyncMergeResult(
    val shelves: List<String>,
    val shelvesChanged: Boolean,
    val books: List<BookEntry>,
    val booksChanged: Boolean,
    val readingStats: Map<String, Map<String, Int>>,
    val readingStatsChanged: Boolean,
)

class BookRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val rootDir: File
        get() {
            val flutterAppDir = File(context.dataDir, "app_flutter")
            val flutterSquartorDir = File(flutterAppDir, "squartor")
            if (flutterSquartorDir.exists() || flutterAppDir.exists()) {
                flutterSquartorDir.mkdirs()
                return flutterSquartorDir
            }
            return File(context.filesDir, "squartor").apply { mkdirs() }
        }

    val booksRootDir: File
        get() = File(rootDir, "books").apply { mkdirs() }

    val fontsRootDir: File
        get() = File(rootDir, "fonts").apply { mkdirs() }

    fun loadBooks(): List<BookEntry> {
        val arr = decodeJsonArrayFromPref(KEY_BOOKS, FALLBACK_KEY_BOOKS) ?: return emptyList()
        val rawBooks = mutableListOf<BookEntry>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: when (val str = arr.optString(i)) {
                "" -> null
                else -> try {
                    JSONObject(str)
                } catch (_: Throwable) {
                    null
                }
            }
            if (item != null) {
                val book = BookEntry.fromJson(item)
                if (book.id.isNotEmpty()) {
                    rawBooks.add(book)
                }
            }
        }
        return repairBookPathsIfNeeded(rawBooks)
    }

    fun saveBooks(books: List<BookEntry>) {
        val arr = JSONArray()
        for (book in books) {
            arr.put(book.toJson())
        }
        prefs.edit().putString(KEY_BOOKS, arr.toString()).apply()
    }

    fun loadStyle(): ReadingStyle {
        val obj = decodeJsonObjectFromPref(KEY_STYLE, FALLBACK_KEY_STYLE) ?: return ReadingStyle()
        return ReadingStyle.fromJson(obj)
    }

    fun saveStyle(style: ReadingStyle) {
        prefs.edit().putString(KEY_STYLE, style.toJson().toString()).apply()
    }

    fun loadFonts(): List<ImportedFont> {
        val arr = decodeJsonArrayFromPref(KEY_FONTS, FALLBACK_KEY_FONTS) ?: return emptyList()
        val fonts = mutableListOf<ImportedFont>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: when (val str = arr.optString(i)) {
                "" -> null
                else -> try {
                    JSONObject(str)
                } catch (_: Throwable) {
                    null
                }
            }
            if (item != null) {
                val font = ImportedFont.fromJson(item)
                if (font.path.isNotEmpty()) {
                    fonts.add(font)
                }
            }
        }
        return fonts
    }

    fun saveFonts(fonts: List<ImportedFont>) {
        val arr = JSONArray()
        for (font in fonts) {
            arr.put(font.toJson())
        }
        prefs.edit().putString(KEY_FONTS, arr.toString()).apply()
    }

    fun loadReadingStats(): Map<String, Map<String, Int>> {
        val obj = decodeJsonObjectFromPref(KEY_READING_STATS, FALLBACK_KEY_READING_STATS)
            ?: return emptyMap()
        return parseReadingStatsObject(obj)
    }

    fun saveReadingStats(stats: Map<String, Map<String, Int>>) {
        val outer = JSONObject()
        for ((bookId, dayMap) in stats) {
            val inner = JSONObject()
            for ((dateKey, seconds) in dayMap) {
                inner.put(dateKey, seconds)
            }
            outer.put(bookId, inner)
        }
        prefs.edit().putString(KEY_READING_STATS, outer.toString()).apply()
    }

    fun loadShelves(): List<String> {
        val arr = decodeJsonArrayFromPref(KEY_SHELVES, FALLBACK_KEY_SHELVES) ?: return emptyList()
        val shelves = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val name = arr.optString(i, "").trim()
            if (name.isNotEmpty() && !shelves.contains(name)) {
                shelves.add(name)
            }
        }
        return shelves
    }

    fun saveShelves(shelves: List<String>) {
        val arr = JSONArray()
        for (shelf in shelves) {
            val trimmed = shelf.trim()
            if (trimmed.isNotEmpty()) {
                arr.put(trimmed)
            }
        }
        prefs.edit().putString(KEY_SHELVES, arr.toString()).apply()
    }

    fun loadCloudSyncSettings(): CloudSyncSettings {
        val obj = decodeJsonObjectFromPref(KEY_CLOUD_SYNC, FALLBACK_KEY_CLOUD_SYNC)
            ?: return CloudSyncSettings()
        return CloudSyncSettings.fromJson(obj)
    }

    fun saveCloudSyncSettings(settings: CloudSyncSettings) {
        prefs.edit().putString(KEY_CLOUD_SYNC, settings.toJson().toString()).apply()
    }

    fun importBookFile(file: File): BookEntry? {
        if (!file.exists() || !file.isFile || file.length() <= 0L) return null
        val lowerName = file.name.lowercase()
        val isEpub = lowerName.endsWith(".epub") || (!lowerName.endsWith(".txt") && hasZipMagic(file))
        val isTxt = lowerName.endsWith(".txt")
        if (!isEpub && !isTxt) return null

        val id = newBookId()
        val bookDir = File(booksRootDir, id).apply { mkdirs() }
        return try {
            val entry = if (isEpub) {
                EpubStreamEngine.importEpub(file, bookDir, id)
            } else {
                TxtStreamEngine.importTxt(file, bookDir, id)
            }
            cleanupRedundantBookFiles(bookDir)
            entry
        } catch (_: Throwable) {
            try {
                bookDir.deleteRecursively()
            } catch (_: Throwable) {
            }
            null
        }
    }

    fun importBookFile(filePath: String): BookEntry? = importBookFile(File(filePath))

    fun saveCustomCover(bookDir: String, coverFile: File): String? {
        if (!coverFile.exists() || !coverFile.isFile || coverFile.length() <= 0L) return null
        return try {
            val dir = if (bookDir.isNotBlank()) {
                File(bookDir).apply { mkdirs() }
            } else {
                File(booksRootDir, "custom_covers").apply { mkdirs() }
            }
            val ext = coverFile.extension.lowercase().takeIf {
                it == "jpg" || it == "jpeg" || it == "png" || it == "webp"
            } ?: "jpg"
            val target = File(dir, "custom_cover_${System.currentTimeMillis()}.$ext")
            coverFile.copyTo(target, overwrite = true)
            dir.listFiles()?.forEach { f ->
                if (f.isFile &&
                    f.name.startsWith("custom_cover") &&
                    f.absolutePath != target.absolutePath
                ) {
                    try {
                        f.delete()
                    } catch (_: Throwable) {
                    }
                }
            }
            if (target.exists() && target.length() > 0L) target.absolutePath else null
        } catch (_: Throwable) {
            null
        }
    }

    fun saveCustomCover(book: BookEntry, coverFile: File): String? =
        saveCustomCover(book.bookDir, coverFile)

    fun importFontFile(file: File): ImportedFont? {
        if (!file.exists() || !file.isFile || file.length() <= 0L) return null
        val ext = file.extension.lowercase()
        if (ext != "ttf" && ext != "otf") return null
        return try {
            val dir = fontsRootDir
            val rawName = file.nameWithoutExtension.trim().ifEmpty { "自定义字体" }
            val safeName = sanitizeFileName(rawName)
            val target = File(dir, "$safeName.$ext")
            if (file.canonicalPath != target.canonicalPath) {
                file.copyTo(target, overwrite = true)
            }
            ImportedFont(name = rawName, path = target.absolutePath)
        } catch (_: Throwable) {
            null
        }
    }

    fun deleteFontFile(font: ImportedFont) {
        if (font.path.isBlank()) return
        try {
            val file = File(font.path)
            if (file.exists() && file.isFile) {
                file.delete()
            }
        } catch (_: Throwable) {
        }
    }

    fun deleteBookFiles(book: BookEntry) {
        if (book.bookDir.isBlank()) return
        try {
            val dir = File(book.bookDir)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        } catch (_: Throwable) {
        }
    }

    fun calculateStorageStats(): StorageStats {
        var booksBytes = 0L
        var cacheBytes = 0L

        val candidateBookRoots = linkedSetOf<String>()
        val flutterBooks = File(context.dataDir, "app_flutter/squartor/books")
        val nativeBooks = File(context.filesDir, "squartor/books")
        if (flutterBooks.exists()) candidateBookRoots.add(flutterBooks.absolutePath)
        if (nativeBooks.exists()) candidateBookRoots.add(nativeBooks.absolutePath)

        for (rootPath in candidateBookRoots) {
            val dir = File(rootPath)
            dir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val rel = file.relativeToOrNull(dir)?.path?.replace('\\', '/') ?: ""
                    val isLegacyCache = rel.contains("/epub/") ||
                        rel.contains("/reader/") ||
                        rel.contains("/txt-reader/")
                    if (isLegacyCache) {
                        cacheBytes += file.length()
                    } else {
                        booksBytes += file.length()
                    }
                }
            }
        }

        val cacheDirs = listOfNotNull(
            context.cacheDir,
            context.externalCacheDir,
            File(context.dataDir, "app_flutter/flutter_assets").takeIf { it.exists() },
            File(context.dataDir, "app_flutter/picked_books").takeIf { it.exists() },
            File(context.dataDir, "app_flutter/open_books").takeIf { it.exists() },
            File(context.dataDir, "app_flutter/temp_books").takeIf { it.exists() },
        )
        for (dir in cacheDirs) {
            try {
                dir.walkTopDown().forEach { f ->
                    if (f.isFile) {
                        cacheBytes += f.length()
                    }
                }
            } catch (_: Throwable) {
            }
        }

        return StorageStats(booksBytes = booksBytes, cacheBytes = cacheBytes)
    }

    fun getStorageStats(): StorageStats = calculateStorageStats()

    fun clearCache() {
        EpubStreamEngine.clearCache()
        TxtStreamEngine.clearCache()

        listOfNotNull(context.cacheDir, context.externalCacheDir).forEach { cacheRoot ->
            try {
                cacheRoot.listFiles()?.forEach { child ->
                    try {
                        child.deleteRecursively()
                    } catch (_: Throwable) {
                    }
                }
            } catch (_: Throwable) {
            }
        }

        val bookDirsToClean = listOf(
            File(context.dataDir, "app_flutter/squartor/books"),
            File(context.filesDir, "squartor/books"),
        )
        for (booksDir in bookDirsToClean) {
            if (booksDir.exists() && booksDir.isDirectory) {
                booksDir.listFiles()?.filter { it.isDirectory }?.forEach { subDir ->
                    cleanupRedundantBookFiles(subDir)
                }
            }
        }

        val flutterDocDir = File(context.dataDir, "app_flutter")
        if (flutterDocDir.exists()) {
            for (legacyName in listOf("flutter_assets", "books", "picked_books", "open_books", "epub", "temp_books")) {
                val legacyDir = File(flutterDocDir, legacyName)
                if (legacyDir.exists()) {
                    try {
                        legacyDir.deleteRecursively()
                    } catch (_: Throwable) {
                    }
                }
            }
        }
    }

    fun uploadCloudSync(
        settings: CloudSyncSettings,
        books: List<BookEntry>,
        shelves: List<String>,
        readingStats: Map<String, Map<String, Int>>,
    ) {
        val payload = buildCloudSyncPayload(books, shelves, readingStats)
        uploadCloudSync(settings, payload)
    }

    fun uploadCloudSync(settings: CloudSyncSettings = loadCloudSyncSettings()) {
        uploadCloudSync(
            settings = settings,
            books = loadBooks(),
            shelves = loadShelves(),
            readingStats = loadReadingStats(),
        )
    }

    fun uploadCloudSync(
        settings: CloudSyncSettings,
        payload: JSONObject,
    ) {
        val folderUrl = resolveWebDavFolderUrl(settings)
        val fileUrl = URL(folderUrl, SYNC_FILE_NAME)
        val bytes = payload.toString().toByteArray(Charsets.UTF_8)

        ensureRemoteWebDavFolder(folderUrl, settings)

        val conn = (fileUrl.openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 25_000
            requestMethod = "PUT"
            doOutput = true
            setRequestProperty("Authorization", basicAuthHeader(settings))
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setFixedLengthStreamingMode(bytes.size)
        }
        try {
            conn.outputStream.use { out ->
                out.write(bytes)
                out.flush()
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                throw buildHttpSyncException(conn, code, "上传失败")
            }
        } finally {
            conn.disconnect()
        }
    }

    fun downloadCloudSync(settings: CloudSyncSettings = loadCloudSyncSettings()): JSONObject {
        val folderUrl = resolveWebDavFolderUrl(settings)
        val fileUrl = URL(folderUrl, SYNC_FILE_NAME)
        val conn = (fileUrl.openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 25_000
            requestMethod = "GET"
            setRequestProperty("Authorization", basicAuthHeader(settings))
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                throw IllegalStateException("远端还没有同步数据，请先上传本机数据")
            }
            if (code !in 200..299) {
                throw buildHttpSyncException(conn, code, "下载失败")
            }
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return try {
                JSONObject(body)
            } catch (_: Throwable) {
                throw IllegalStateException("远端同步文件格式不正确")
            }
        } finally {
            conn.disconnect()
        }
    }

    fun buildCloudSyncPayload(
        books: List<BookEntry>,
        shelves: List<String>,
        readingStats: Map<String, Map<String, Int>>,
        nowMillis: Long = System.currentTimeMillis(),
    ): JSONObject {
        val payload = JSONObject()
        payload.put("schema", 1)
        payload.put("updatedAt", formatIso8601(nowMillis))

        val shelvesArr = JSONArray()
        shelves.forEach { shelvesArr.put(it) }
        payload.put("shelves", shelvesArr)

        val statsObj = JSONObject()
        for ((bookId, dayMap) in readingStats) {
            val inner = JSONObject()
            for ((dateKey, seconds) in dayMap) {
                inner.put(dateKey, seconds)
            }
            statsObj.put(bookId, inner)
        }
        payload.put("readingStats", statsObj)

        val booksArr = JSONArray()
        for (book in books) {
            val item = JSONObject()
            item.put("identity", syncStrictKey(book))
            item.put("identityBase", syncBaseKey(book))

            val bookStatsObj = JSONObject()
            readingStats[book.id]?.forEach { (dateKey, seconds) ->
                bookStatsObj.put(dateKey, seconds)
            }
            item.put("readingStats", bookStatsObj)
            item.put("title", book.title)
            item.put("author", book.author)
            item.put("format", book.format.name)
            item.put("chapterCount", book.chapters.size)
            item.put("wordCount", book.wordCount ?: JSONObject.NULL)
            item.put("currentChapterIndex", book.currentChapterIndex)
            item.put("currentPage", book.currentPage)
            item.put("pageCount", book.pageCount)
            item.put("progress", book.progress)
            item.put("lastReadAt", book.lastReadAtMillis?.let { formatIso8601(it) } ?: JSONObject.NULL)
            item.put("shelfName", book.shelfName ?: JSONObject.NULL)

            val bmArr = JSONArray()
            book.bookmarks.forEach { bmArr.put(it.toJson()) }
            item.put("bookmarks", bmArr)
            booksArr.put(item)
        }
        payload.put("books", booksArr)
        return payload
    }

    fun mergeCloudSyncPayload(
        payload: JSONObject,
        localBooks: List<BookEntry>,
        localShelves: List<String>,
        localReadingStats: Map<String, Map<String, Int>>,
    ): CloudSyncMergeResult {
        var shelves = localShelves
        var shelvesChanged = false
        val shelvesArr = payload.optJSONArray("shelves")
        if (shelvesArr != null) {
            val remoteShelves = mutableListOf<String>()
            for (i in 0 until shelvesArr.length()) {
                val s = shelvesArr.optString(i, "").trim()
                if (s.isNotEmpty() && !remoteShelves.contains(s)) {
                    remoteShelves.add(s)
                }
            }
            if (remoteShelves != localShelves) {
                shelves = remoteShelves
                shelvesChanged = true
            }
        }

        var books = localBooks
        var booksChanged = false
        val mergedStats = LinkedHashMap<String, Map<String, Int>>(localReadingStats)
        var readingStatsChanged = false

        val remoteBooksArr = payload.optJSONArray("books")
        if (remoteBooksArr != null) {
            val remoteByKey = indexRemoteBooks(remoteBooksArr)
            val updatedBooks = mutableListOf<BookEntry>()
            for (book in localBooks) {
                val remote = findRemoteForBook(book, remoteByKey)
                if (remote != null) {
                    val (mergedBook, changed) = mergeSingleBook(book, remote)
                    if (changed) booksChanged = true
                    updatedBooks.add(mergedBook)

                    val remoteBookStatsObj = remote.optJSONObject("readingStats")
                    if (remoteBookStatsObj != null) {
                        val remoteBookStats = parseSingleBookStats(remoteBookStatsObj)
                        val existingBookStats = mergedStats[book.id] ?: emptyMap()
                        val combined = mergeDayStats(existingBookStats, remoteBookStats)
                        if (combined != existingBookStats) {
                            mergedStats[book.id] = combined
                            readingStatsChanged = true
                        }
                    }
                } else {
                    updatedBooks.add(book)
                }
            }
            books = updatedBooks
        }

        if (!readingStatsChanged) {
            val topLevelStatsObj = payload.optJSONObject("readingStats")
            if (topLevelStatsObj != null) {
                val remoteTopStats = parseReadingStatsObject(topLevelStatsObj)
                for ((bookId, remoteDayMap) in remoteTopStats) {
                    val existing = mergedStats[bookId] ?: emptyMap()
                    val combined = mergeDayStats(existing, remoteDayMap)
                    if (combined != existing) {
                        mergedStats[bookId] = combined
                        readingStatsChanged = true
                    }
                }
            }
        }

        return CloudSyncMergeResult(
            shelves = shelves,
            shelvesChanged = shelvesChanged,
            books = books,
            booksChanged = booksChanged,
            readingStats = mergedStats,
            readingStatsChanged = readingStatsChanged,
        )
    }

    private fun mergeSingleBook(book: BookEntry, remote: JSONObject): Pair<BookEntry, Boolean> {
        val remoteLastReadMillis = if (remote.isNull("lastReadAt")) {
            null
        } else {
            parseIso8601(remote.optString("lastReadAt"))
        }
        val hasRemoteProgress = remote.has("progress") && !remote.isNull("progress")
        val remoteProgress = if (hasRemoteProgress) {
            remote.optDouble("progress", book.progress).let {
                if (it.isNaN()) book.progress else it.coerceIn(0.0, 1.0)
            }
        } else {
            null
        }

        val remoteIsAhead = remoteProgress != null && remoteProgress > book.progress + 0.001
        val remoteIsNewer = remoteLastReadMillis != null &&
            (book.lastReadAtMillis == null || remoteLastReadMillis > book.lastReadAtMillis)
        val shouldUseRemotePosition = remoteIsAhead || remoteIsNewer

        val remoteBookmarksArr = remote.optJSONArray("bookmarks")
        val bookmarksById = LinkedHashMap<String, BookBookmark>()
        for (bm in book.bookmarks) {
            if (bm.id.isNotEmpty()) bookmarksById[bm.id] = bm
        }
        if (remoteBookmarksArr != null) {
            for (i in 0 until remoteBookmarksArr.length()) {
                val bmObj = remoteBookmarksArr.optJSONObject(i) ?: continue
                val bm = BookBookmark.fromJson(bmObj)
                if (bm.id.isNotEmpty()) {
                    bookmarksById[bm.id] = bm
                }
            }
        }
        val mergedBookmarks = bookmarksById.values.sortedByDescending { it.createdAtMillis }
        val bookmarkChanged = mergedBookmarks != book.bookmarks

        val remoteShelf = remote.optString("shelfName").takeIf {
            it.isNotEmpty() && !remote.isNull("shelfName")
        }
        val shouldUpdateShelf = remote.has("shelfName") && remoteShelf != book.shelfName

        if (!shouldUseRemotePosition && !bookmarkChanged && !shouldUpdateShelf) {
            return book to false
        }

        val maxChapterIdx = (book.chapters.size - 1).coerceAtLeast(0)
        val safeRemoteChapter = if (remote.has("currentChapterIndex") && !remote.isNull("currentChapterIndex")) {
            remote.optInt("currentChapterIndex", book.currentChapterIndex).coerceIn(0, maxChapterIdx)
        } else {
            book.currentChapterIndex
        }

        val updated = book.copy(
            currentChapterIndex = if (shouldUseRemotePosition) safeRemoteChapter else book.currentChapterIndex,
            currentPage = if (shouldUseRemotePosition) {
                remote.optInt("currentPage", book.currentPage).coerceAtLeast(0)
            } else {
                book.currentPage
            },
            pageCount = if (shouldUseRemotePosition) {
                remote.optInt("pageCount", book.pageCount).coerceAtLeast(1)
            } else {
                book.pageCount
            },
            progress = if (shouldUseRemotePosition) {
                remoteProgress ?: book.progress
            } else {
                book.progress
            },
            lastReadAtMillis = if (shouldUseRemotePosition && remoteLastReadMillis != null) {
                remoteLastReadMillis
            } else {
                book.lastReadAtMillis
            },
            shelfName = if (shouldUpdateShelf) remoteShelf else book.shelfName,
            bookmarks = mergedBookmarks,
        )
        return updated to true
    }

    private fun indexRemoteBooks(remoteBooksArr: JSONArray): Map<String, JSONObject> {
        val remoteByKey = HashMap<String, JSONObject>()
        val ambiguousKeys = HashSet<String>()

        fun addKey(key: String, remote: JSONObject) {
            if (key.isEmpty() || ambiguousKeys.contains(key)) return
            val existing = remoteByKey[key]
            if (existing == null || existing === remote) {
                remoteByKey[key] = remote
                return
            }
            remoteByKey.remove(key)
            ambiguousKeys.add(key)
        }

        for (i in 0 until remoteBooksArr.length()) {
            val remote = remoteBooksArr.optJSONObject(i) ?: continue
            for (key in matchKeysForRemote(remote)) {
                addKey(key, remote)
            }
        }
        return remoteByKey
    }

    private fun findRemoteForBook(
        book: BookEntry,
        remoteByIdentity: Map<String, JSONObject>,
    ): JSONObject? {
        val candidates = listOf(
            "strict:${syncStrictKey(book)}",
            "base:${syncBaseKey(book)}",
            "title:${syncTitleKey(book)}",
        )
        for (key in candidates) {
            remoteByIdentity[key]?.let { return it }
        }
        return null
    }

    private fun matchKeysForRemote(remote: JSONObject): Set<String> {
        val keys = LinkedHashSet<String>()
        val strictIdentity = remote.optString("identity", "").trim()
        if (strictIdentity.isNotEmpty() && !remote.isNull("identity")) {
            keys.add("strict:$strictIdentity")
        }
        val baseIdentity = remote.optString("identityBase", "").trim()
        if (baseIdentity.isNotEmpty() && !remote.isNull("identityBase")) {
            keys.add("base:$baseIdentity")
        }
        val format = remote.optString("format", "").trim().lowercase()
        val title = normalizeIdentityText(remote.optString("title", ""))
        val author = normalizeIdentityText(remote.optString("author", ""))
        if (format.isEmpty() || title.isEmpty()) return keys

        if (remote.has("chapterCount") && !remote.isNull("chapterCount")) {
            val chapterCount = remote.optInt("chapterCount", 0)
            keys.add("base:$format|$title|$author|$chapterCount")
            if (remote.has("wordCount") && !remote.isNull("wordCount")) {
                val wordCount = remote.optInt("wordCount", -1)
                keys.add("strict:$format|$title|$author|$chapterCount|$wordCount")
            }
        }
        keys.add("title:$format|$title|$author")
        return keys
    }

    private fun mergeDayStats(
        local: Map<String, Int>,
        remote: Map<String, Int>,
    ): Map<String, Int> {
        val merged = LinkedHashMap<String, Int>(local)
        for ((day, seconds) in remote) {
            merged[day] = max(merged[day] ?: 0, seconds)
        }
        return merged
    }

    private fun resolveWebDavFolderUrl(settings: CloudSyncSettings): URL {
        if (!settings.configured) {
            throw IllegalStateException("请先填写 WebDAV 地址、账号和密码")
        }
        val rawEndpoint = settings.endpoint.trim()
        val normalized = if (rawEndpoint.endsWith("/")) rawEndpoint else "$rawEndpoint/"
        val uri = try {
            URI(normalized)
        } catch (_: Throwable) {
            throw IllegalStateException("WebDAV 地址格式不正确")
        }
        val scheme = uri.scheme?.lowercase() ?: ""
        if (scheme != "http" && scheme != "https") {
            throw IllegalStateException("WebDAV 地址需要以 http 或 https 开头")
        }
        if (uri.host.isNullOrBlank()) {
            throw IllegalStateException("WebDAV 地址缺少服务器主机名")
        }
        val pathSegments = (uri.path ?: "").split('/').filter { it.isNotEmpty() }
        val folderUri = if (pathSegments.lastOrNull() == SYNC_FOLDER_NAME) {
            uri
        } else {
            uri.resolve("$SYNC_FOLDER_NAME/")
        }
        return folderUri.toURL()
    }

    private fun ensureRemoteWebDavFolder(folderUrl: URL, settings: CloudSyncSettings) {
        var conn: HttpURLConnection? = null
        try {
            conn = (folderUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 20_000
                setRequestProperty("Authorization", basicAuthHeader(settings))
                setFixedLengthStreamingMode(0)
            }
            if (!setWebDavRequestMethod(conn, "MKCOL")) {
                return
            }
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_CREATED ||
                code == HttpURLConnection.HTTP_OK ||
                code == HttpURLConnection.HTTP_NO_CONTENT ||
                code == HttpURLConnection.HTTP_BAD_METHOD ||
                code == 405
            ) {
                return
            }
            if (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) {
                throw buildHttpSyncException(conn, code, "创建同步文件夹失败")
            }
        } catch (e: IllegalStateException) {
            throw e
        } catch (_: Throwable) {
            // Best-effort MKCOL; some servers or Android HttpURLConnection implementations disallow MKCOL
        } finally {
            conn?.disconnect()
        }
    }

    private fun setWebDavRequestMethod(conn: HttpURLConnection, method: String): Boolean {
        return try {
            conn.requestMethod = method
            true
        } catch (_: ProtocolException) {
            try {
                var target: Any = conn
                try {
                    val delegateField = conn.javaClass.getDeclaredField("delegate")
                    delegateField.isAccessible = true
                    delegateField.get(conn)?.let { target = it }
                } catch (_: Throwable) {
                }
                var cls: Class<*>? = target.javaClass
                while (cls != null) {
                    try {
                        val methodField = cls.getDeclaredField("method")
                        methodField.isAccessible = true
                        methodField.set(target, method)
                        return true
                    } catch (_: NoSuchFieldException) {
                        cls = cls.superclass
                    }
                }
                false
            } catch (_: Throwable) {
                false
            }
        }
    }

    private fun basicAuthHeader(settings: CloudSyncSettings): String {
        val raw = "${settings.username}:${settings.password}".toByteArray(Charsets.UTF_8)
        return "Basic ${Base64.encodeToString(raw, Base64.NO_WRAP)}"
    }

    private fun buildHttpSyncException(
        conn: HttpURLConnection,
        code: Int,
        action: String,
    ): IllegalStateException {
        val detail = try {
            (conn.errorStream ?: conn.inputStream)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        } catch (_: Throwable) {
            ""
        }
        val friendly = when (code) {
            HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN ->
                "账号或应用密码不正确，或没有写入权限"
            HttpURLConnection.HTTP_NOT_FOUND ->
                "远端路径不存在，请确认 WebDAV 地址是否正确"
            HttpURLConnection.HTTP_CONFLICT ->
                "父文件夹不存在，请检查 WebDAV 地址"
            HttpURLConnection.HTTP_BAD_METHOD ->
                "当前 WebDAV 地址不允许写入"
            else -> compactErrorDetail(detail)
        }
        val suffix = if (friendly.isEmpty()) "" else "：$friendly"
        return IllegalStateException("$action（HTTP $code）$suffix")
    }

    private fun compactErrorDetail(detail: String): String {
        val text = detail
            .replace(Regex("""<[^>]*>"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (text.isEmpty()) return ""
        return if (text.length <= 90) text else "${text.substring(0, 90)}..."
    }

    private fun repairBookPathsIfNeeded(books: List<BookEntry>): List<BookEntry> {
        var changed = false
        val result = books.map { book ->
            var current = book
            if (current.bookDir.isNotBlank()) {
                val localFileName = if (current.format == com.squartor.reader.model.BookFormat.epub) {
                    "book.epub"
                } else {
                    "book.txt"
                }
                val localCopy = File(current.bookDir, localFileName)
                val sourceMissing = current.sourcePath.isBlank() || !File(current.sourcePath).exists()
                if (sourceMissing && localCopy.exists()) {
                    current = current.copy(sourcePath = localCopy.absolutePath)
                    changed = true
                }
                if (current.format == com.squartor.reader.model.BookFormat.epub) {
                    val coverMissing = current.coverPath.isNullOrBlank() || !File(current.coverPath).exists()
                    if (coverMissing && current.sourcePath.isNotBlank() && File(current.sourcePath).exists()) {
                        val extracted = EpubStreamEngine.extractCoverIfMissing(
                            current.sourcePath,
                            File(current.bookDir)
                        )
                        if (extracted != null) {
                            current = current.copy(coverPath = extracted)
                            changed = true
                        }
                    }
                }
            }
            current
        }
        if (changed) {
            saveBooks(result)
        }
        return result
    }

    private fun cleanupRedundantBookFiles(bookDir: File) {
        if (!bookDir.exists() || !bookDir.isDirectory) return
        try {
            for (subName in listOf("epub", "reader", "txt-reader")) {
                val subDir = File(bookDir, subName)
                if (subDir.exists()) {
                    try {
                        subDir.deleteRecursively()
                    } catch (_: Throwable) {
                    }
                }
            }
            bookDir.listFiles()?.forEach { entity ->
                if (entity.isFile) {
                    val lower = entity.name.lowercase()
                    val keep = lower == "cover.jpg" ||
                        lower == "cover.png" ||
                        lower == "cover.webp" ||
                        lower.startsWith("custom_cover") ||
                        lower == "book.epub" ||
                        lower == "book.txt"
                    if (!keep) {
                        try {
                            entity.delete()
                        } catch (_: Throwable) {
                        }
                    }
                }
            }
        } catch (_: Throwable) {
        }
    }

    private fun parseReadingStatsObject(obj: JSONObject): Map<String, Map<String, Int>> {
        val keys = obj.keys()
        if (!keys.hasNext()) return emptyMap()
        val firstKey = keys.next()
        val firstVal = obj.opt(firstKey)
        if (firstVal is JSONObject) {
            val result = LinkedHashMap<String, Map<String, Int>>()
            val allKeys = obj.keys()
            while (allKeys.hasNext()) {
                val bookId = allKeys.next()
                val innerObj = obj.optJSONObject(bookId) ?: continue
                result[bookId] = parseSingleBookStats(innerObj)
            }
            return result
        }
        val legacyMap = parseSingleBookStats(obj)
        return if (legacyMap.isEmpty()) emptyMap() else mapOf("__legacy__" to legacyMap)
    }

    private fun parseSingleBookStats(obj: JSONObject): Map<String, Int> {
        val map = LinkedHashMap<String, Int>()
        val iter = obj.keys()
        while (iter.hasNext()) {
            val k = iter.next()
            val v = obj.optInt(k, 0)
            if (v > 0) {
                map[k] = v
            }
        }
        return map
    }

    private fun readPrefRaw(primaryKey: String, fallbackKey: String): Any? {
        val all = prefs.all
        return all[primaryKey] ?: all[fallbackKey]
    }

    private fun decodeJsonArrayFromPref(primaryKey: String, fallbackKey: String): JSONArray? {
        val raw = readPrefRaw(primaryKey, fallbackKey) ?: return null
        when (raw) {
            is String -> {
                val trimmed = raw.trim()
                if (trimmed.isEmpty()) return null
                if (trimmed.startsWith(FLUTTER_LIST_PREFIX)) {
                    val rest = trimmed.removePrefix(FLUTTER_LIST_PREFIX).trim()
                    val jsonCandidate = if (rest.startsWith("!")) rest.substring(1).trim() else rest
                    if (jsonCandidate.startsWith("[")) {
                        return try {
                            JSONArray(jsonCandidate)
                        } catch (_: Throwable) {
                            null
                        }
                    }
                    return decodeJavaSerializedList(rest)
                }
                if (trimmed.startsWith("[")) {
                    return try {
                        JSONArray(trimmed)
                    } catch (_: Throwable) {
                        null
                    }
                }
                return null
            }
            is Collection<*> -> {
                val arr = JSONArray()
                raw.forEach { item ->
                    if (item != null) arr.put(item)
                }
                return arr
            }
            else -> return null
        }
    }

    private fun decodeJsonObjectFromPref(primaryKey: String, fallbackKey: String): JSONObject? {
        val raw = readPrefRaw(primaryKey, fallbackKey) as? String ?: return null
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || !trimmed.startsWith("{")) return null
        return try {
            JSONObject(trimmed)
        } catch (_: Throwable) {
            null
        }
    }

    private fun decodeJavaSerializedList(base64Payload: String): JSONArray? {
        return try {
            val bytes = Base64.decode(base64Payload, Base64.DEFAULT)
            val list = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as? List<*> }
                ?: return null
            val arr = JSONArray()
            for (item in list) {
                if (item != null) {
                    val s = item.toString().trim()
                    if (s.startsWith("{") && s.endsWith("}")) {
                        try {
                            arr.put(JSONObject(s))
                        } catch (_: Throwable) {
                            arr.put(s)
                        }
                    } else {
                        arr.put(s)
                    }
                }
            }
            arr
        } catch (_: Throwable) {
            null
        }
    }

    private fun hasZipMagic(file: File): Boolean {
        return try {
            file.inputStream().use { input ->
                val header = ByteArray(4)
                val read = input.read(header)
                read == 4 &&
                    header[0] == 0x50.toByte() &&
                    header[1] == 0x4B.toByte() &&
                    header[2] == 0x03.toByte() &&
                    header[3] == 0x04.toByte()
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun sanitizeFileName(input: String): String {
        return input.replace(Regex("""[\\/:*?"<>|]"""), "_")
    }

    private fun newBookId(): String {
        return (System.currentTimeMillis() * 1000L + (System.nanoTime() % 1000L).coerceAtLeast(0L)).toString()
    }

    companion object {
        const val PREFS_NAME = "FlutterSharedPreferences"
        const val FLUTTER_LIST_PREFIX = "VGhpcyBpcyB0aGUgcHJlZml4IGZvciBhIGxpc3Qu"

        const val KEY_BOOKS = "flutter.books.v1"
        const val KEY_STYLE = "flutter.style.v1"
        const val KEY_FONTS = "flutter.fonts.v1"
        const val KEY_READING_STATS = "flutter.reading_stats.v1"
        const val KEY_SHELVES = "flutter.shelves.v1"
        const val KEY_CLOUD_SYNC = "flutter.cloud_sync_settings.v1"

        private const val FALLBACK_KEY_BOOKS = "books.v1"
        private const val FALLBACK_KEY_STYLE = "style.v1"
        private const val FALLBACK_KEY_FONTS = "fonts.v1"
        private const val FALLBACK_KEY_READING_STATS = "reading_stats.v1"
        private const val FALLBACK_KEY_SHELVES = "shelves.v1"
        private const val FALLBACK_KEY_CLOUD_SYNC = "cloud_sync_settings.v1"

        const val SYNC_FOLDER_NAME = "SQuartor"
        const val SYNC_FILE_NAME = "squartor-sync-v1.json"

        fun normalizeIdentityText(value: String): String {
            return value.lowercase().replace(Regex("""\s+"""), "").trim()
        }

        fun duplicateKey(book: BookEntry): String {
            return listOf(
                book.format.name,
                normalizeIdentityText(book.title),
                normalizeIdentityText(book.author),
                book.chapters.size.toString(),
                (book.wordCount ?: -1).toString(),
            ).joinToString("|")
        }

        fun syncStrictKey(book: BookEntry): String {
            return listOf(
                book.format.name,
                normalizeIdentityText(book.title),
                normalizeIdentityText(book.author),
                book.chapters.size.toString(),
                (book.wordCount ?: -1).toString(),
            ).joinToString("|")
        }

        fun syncBaseKey(book: BookEntry): String {
            return listOf(
                book.format.name,
                normalizeIdentityText(book.title),
                normalizeIdentityText(book.author),
                book.chapters.size.toString(),
            ).joinToString("|")
        }

        fun syncTitleKey(book: BookEntry): String {
            return listOf(
                book.format.name,
                normalizeIdentityText(book.title),
                normalizeIdentityText(book.author),
            ).joinToString("|")
        }
    }
}
