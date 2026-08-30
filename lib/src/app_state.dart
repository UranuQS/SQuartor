import 'dart:async';
import 'dart:io';
import 'dart:ui' show Brightness, Color;

import 'package:flutter/foundation.dart';

import 'book_repository.dart';
import 'cloud_sync/cloud_sync_merger.dart';
import 'fonts/app_font_registrar.dart';
import 'models.dart';
import 'repository/book_identity.dart';

class ImportActivity {
  const ImportActivity({
    required this.title,
    required this.detail,
    required this.active,
    required this.failed,
  });

  final String title;
  final String detail;
  final bool active;
  final bool failed;
}

class AppState extends ChangeNotifier {
  AppState(this._repository) {
    _initialLoad = load();
  }

  final BookRepository _repository;
  late final Future<void> _initialLoad;
  final CloudSyncMerger _cloudSyncMerger = const CloudSyncMerger();
  final AppFontRegistrar _appFontRegistrar = AppFontRegistrar();
  final ChangeNotifier _appThemeChanges = ChangeNotifier();
  final ChangeNotifier _readingStyleChanges = ChangeNotifier();
  final ChangeNotifier _readerChromeChanges = ChangeNotifier();
  final ChangeNotifier _libraryChanges = ChangeNotifier();
  final ChangeNotifier _statisticsChanges = ChangeNotifier();
  final ChangeNotifier _messageChanges = ChangeNotifier();
  final ChangeNotifier _cloudSyncChanges = ChangeNotifier();

  late final Listenable appChanges = _appThemeChanges;
  late final Listenable shelfChanges = Listenable.merge([
    _appThemeChanges,
    _libraryChanges,
  ]);
  late final Listenable readingNowChanges = shelfChanges;
  late final Listenable statsScreenChanges = Listenable.merge([
    _appThemeChanges,
    _libraryChanges,
    _statisticsChanges,
  ]);
  late final Listenable settingsChanges = Listenable.merge([
    _appThemeChanges,
    _readingStyleChanges,
    _cloudSyncChanges,
  ]);
  late final Listenable readerChanges = Listenable.merge([
    _appThemeChanges,
    _readingStyleChanges,
    _readerChromeChanges,
  ]);
  late final Listenable messageChanges = _messageChanges;

  List<BookEntry> _books = const [];
  List<ImportedFont> _fonts = const [];
  List<String> _shelves = const [];
  Map<String, Map<String, int>> _readingStats = const {};
  Map<String, int> _dailyReadingTotals = const {};
  ReadingStyle _style = const ReadingStyle();
  CloudSyncSettings _cloudSyncSettings = const CloudSyncSettings();
  Brightness _platformBrightness = Brightness.dark;
  Color? _dynamicThemeSeedColor;
  bool _loading = true;
  String? _error;
  Timer? _styleSaveTimer;
  Timer? _booksSaveTimer;
  Timer? _statsSaveTimer;
  Timer? _shelvesSaveTimer;
  bool _booksSaveInFlight = false;
  bool _booksSavePending = false;
  ImportActivity? _importActivity;
  Timer? _importActivityClearTimer;
  String? _appFontFamily;
  String? _defaultReaderFontUri;

  List<BookEntry> get books => _books;
  List<ImportedFont> get fonts => _fonts;
  List<String> get shelves => _shelves;
  Map<String, Map<String, int>> get readingStats => _readingStats;
  ReadingStyle get style => _style;
  CloudSyncSettings get cloudSyncSettings => _cloudSyncSettings;
  String? get appFontFamily => _appFontFamily;
  Brightness get effectiveBrightness {
    return switch (_style.brightnessMode) {
      AppBrightnessMode.light => Brightness.light,
      AppBrightnessMode.dark => Brightness.dark,
      AppBrightnessMode.system => _platformBrightness,
    };
  }

  AppPalette get palette => _style.resolvePalette(
    effectiveBrightness,
    dynamicSeedColor: _dynamicThemeSeedColor,
  );
  bool get loading => _loading;
  String? get error => _error;
  String? get defaultReaderFontUri => _defaultReaderFontUri;
  ImportActivity? get importActivity => _importActivity;
  Future<void> get ready => _initialLoad;

  void setPlatformBrightness(Brightness brightness) {
    if (_platformBrightness == brightness) {
      return;
    }
    _platformBrightness = brightness;
    if (_style.brightnessMode == AppBrightnessMode.system) {
      _appThemeChanges.notifyListeners();
    }
  }

  void setDynamicThemeSeedColor(Color? color) {
    if (_dynamicThemeSeedColor == color) {
      return;
    }
    _dynamicThemeSeedColor = color;
    if (_style.appTheme == AppThemeId.wallpaper &&
        _style.customThemeColorValue == null) {
      _appThemeChanges.notifyListeners();
    }
  }

  Future<void> load() async {
    if (!_loading) {
      _loading = true;
      _libraryChanges.notifyListeners();
    }
    try {
      final snapshot = await _repository.loadSnapshot();
      _books = _deduplicateBooks(snapshot.books);
      _fonts = snapshot.fonts;
      _shelves = snapshot.shelves;
      _readingStats = snapshot.readingStats;
      _rebuildDailyReadingTotals();
      _style = snapshot.style;
      _cloudSyncSettings = snapshot.cloudSyncSettings;
      _appFontFamily = await _appFontRegistrar.register(_style.appFontPath);
      _defaultReaderFontUri = await _repository.ensureDefaultReaderFont();
      _error = null;
    } catch (error) {
      _error = '加载数据失败：$error';
    } finally {
      _loading = false;
      _appThemeChanges.notifyListeners();
      _readingStyleChanges.notifyListeners();
      _cloudSyncChanges.notifyListeners();
      _libraryChanges.notifyListeners();
      _statisticsChanges.notifyListeners();
      _messageChanges.notifyListeners();
    }
    unawaited(_runBackgroundMaintenance());
  }

  Future<void> _runBackgroundMaintenance() async {
    try {
      final current = _books;
      final upgraded = await _repository.runBackgroundMaintenance(current);
      if (!listEquals(upgraded, current)) {
        _books = _deduplicateBooks(upgraded);
        _libraryChanges.notifyListeners();
      }
    } catch (_) {}
  }

  Future<void> importBook() async {
    _beginImportActivity(title: '正在导入书籍', detail: '正在解析文件...');
    try {
      final book = await _repository.pickAndImportBook();
      if (book == null) {
        _clearImportActivity();
        return;
      }
      final imported = _storeImportedBooks([book]);
      _finishImportActivity(imported == 0 ? '书籍已存在' : '导入完成');
    } catch (error) {
      _finishImportActivity('导入失败', failed: true);
      _error = '导入书籍失败：$error';
      _messageChanges.notifyListeners();
    }
  }

  Future<BookEntry?> consumeAndOpenExternalBook() async {
    try {
      await ready;
      final pending = await _repository.consumePendingOpenBook();
      if (pending == null) {
        return null;
      }
      final existing = await _findExistingExternalBook(pending);
      if (existing != null) {
        return existing;
      }
      _beginImportActivity(title: '正在导入书籍', detail: pending.name);
      final book = await _repository.importBookFile(pending.path);
      final duplicate = _findExistingDuplicateBook(book);
      final imported = _storeImportedBooks([book]);
      _finishImportActivity(imported == 0 ? '书籍已存在' : '导入完成');
      return imported == 0 ? duplicate : book;
    } catch (error) {
      _finishImportActivity('导入失败', failed: true);
      _error = '打开外部书籍失败：$error';
      _messageChanges.notifyListeners();
      return null;
    }
  }

  Future<void> importBooks() async {
    _beginImportActivity(title: '正在批量导入', detail: '正在读取所选文件...');
    try {
      var totalImportedCount = 0;
      final books = await _repository.pickAndImportBooks(
        onProgress: (current, total, book, fileName) {
          _beginImportActivity(
            title: '正在导入 ($current/$total)',
            detail: fileName,
          );
          if (book != null) {
            final count = _storeImportedBooks([book]);
            totalImportedCount += count;
          }
        },
      );
      if (books.isEmpty && totalImportedCount == 0) {
        _clearImportActivity();
        return;
      }
      _finishImportActivity(totalImportedCount == 0 ? '所选书籍已存在' : '已导入 $totalImportedCount 本书');
    } catch (error) {
      _finishImportActivity('导入失败', failed: true);
      _error = '批量导入失败：$error';
      _messageChanges.notifyListeners();
    }
  }

  Future<void> importBookDirectory() async {
    _beginImportActivity(title: '正在导入文件夹', detail: '正在扫描书籍文件...');
    try {
      var totalImportedCount = 0;
      final books = await _repository.pickAndImportBookDirectory(
        onProgress: (current, total, book, fileName) {
          _beginImportActivity(
            title: '正在导入 ($current/$total)',
            detail: fileName,
          );
          if (book != null) {
            final count = _storeImportedBooks([book]);
            totalImportedCount += count;
          }
        },
      );
      if (books.isEmpty && totalImportedCount == 0) {
        _clearImportActivity();
        return;
      }
      _finishImportActivity(totalImportedCount == 0 ? '文件夹内书籍已存在' : '已导入 $totalImportedCount 本书');
    } catch (error) {
      _finishImportActivity('导入失败', failed: true);
      _error = '导入文件夹失败：$error';
      _messageChanges.notifyListeners();
    }
  }

  Future<void> importFont() async {
    await importReaderFont();
  }

  Future<void> importReaderFont() async {
    try {
      final font = await _repository.pickAndImportFont();
      if (font == null) {
        return;
      }
      await _storeImportedFont(font);
      await updateStyle(
        _style.copyWith(fontName: font.name, fontPath: font.path),
        immediate: true,
      );
      _error = null;
      _messageChanges.notifyListeners();
    } catch (error) {
      _error = '导入字体失败：$error';
      _messageChanges.notifyListeners();
    }
  }

  Future<void> importAppFont() async {
    try {
      final font = await _repository.pickAndImportFont();
      if (font == null) {
        return;
      }
      await _storeImportedFont(font);
      await updateStyle(
        _style.copyWith(appFontName: font.name, appFontPath: font.path),
        immediate: true,
      );
      _error = null;
      _messageChanges.notifyListeners();
    } catch (error) {
      _error = '导入应用字体失败：$error';
      _messageChanges.notifyListeners();
    }
  }

  Future<void> updateStyle(ReadingStyle style, {bool immediate = false}) async {
    final styleUnchanged =
        _style.brightnessMode == style.brightnessMode &&
        _style.appTheme == style.appTheme &&
        _style.customThemeColorValue == style.customThemeColorValue &&
        _style.readerBackground == style.readerBackground &&
        _style.fontSize == style.fontSize &&
        _style.lineHeight == style.lineHeight &&
        _style.paragraphSpacing == style.paragraphSpacing &&
        _style.letterSpacing == style.letterSpacing &&
        _style.pageMargin == style.pageMargin &&
        _style.verticalMargin == style.verticalMargin &&
        _style.readingFlow == style.readingFlow &&
        _style.reverseTapPageTurn == style.reverseTapPageTurn &&
        _style.firstLineIndent == style.firstLineIndent &&
        _style.dimJapaneseText == style.dimJapaneseText &&
        _style.pageTurnAnimation == style.pageTurnAnimation &&
        _style.fontWeightValue == style.fontWeightValue &&
        _style.fontName == style.fontName &&
        _style.fontPath == style.fontPath &&
        _style.appFontName == style.appFontName &&
        _style.appFontPath == style.appFontPath;
    if (styleUnchanged) {
      _queueStyleSave(immediate: immediate);
      return;
    }
    final appFontChanged = _style.appFontPath != style.appFontPath;
    final appThemeChanged =
        _style.brightnessMode != style.brightnessMode ||
        _style.appTheme != style.appTheme ||
        _style.customThemeColorValue != style.customThemeColorValue ||
        appFontChanged;
    final readerChromeChanged =
        _style.readerBackground != style.readerBackground ||
        _style.readingFlow != style.readingFlow ||
        _style.reverseTapPageTurn != style.reverseTapPageTurn ||
        _style.pageTurnAnimation != style.pageTurnAnimation;
    final paginationChanged =
        _style.fontSize != style.fontSize ||
        _style.fontWeightValue != style.fontWeightValue ||
        _style.lineHeight != style.lineHeight ||
        _style.paragraphSpacing != style.paragraphSpacing ||
        _style.letterSpacing != style.letterSpacing ||
        _style.pageMargin != style.pageMargin ||
        _style.verticalMargin != style.verticalMargin ||
        _style.firstLineIndent != style.firstLineIndent ||
        _style.fontPath != style.fontPath;
    if (appFontChanged) {
      _appFontFamily = await _appFontRegistrar.register(style.appFontPath);
    }
    _style = style;
    if (paginationChanged) {
      _books = [
        for (final book in _books)
          book.copyWith(
            chapters: [
              for (final chapter in book.chapters)
                chapter.copyWith(clearCachedPageCount: true),
            ],
          ),
      ];
      _queueBooksSave();
    }
    if (appThemeChanged) {
      _appThemeChanges.notifyListeners();
    }
    if (readerChromeChanged) {
      _readerChromeChanges.notifyListeners();
    }
    _readingStyleChanges.notifyListeners();
    _queueStyleSave(immediate: immediate);
  }

  Future<void> _storeImportedFont(ImportedFont font) async {
    final existing = _fonts.where((item) => item.path != font.path).toList();
    _fonts = [...existing, font];
    await _repository.saveFonts(_fonts);
  }

  Future<StorageStats> getStorageStats() => _repository.getStorageStats();

  Future<void> clearCache() async {
    await _repository.clearCache();
    notifyListeners();
  }

  int _storeImportedBooks(List<BookEntry> books) {
    if (books.isEmpty) {
      return 0;
    }
    final existingKeys = _books.map(BookIdentity.duplicateKey).toSet();
    final incomingKeys = <String>{};
    final uniqueBooks = <BookEntry>[];
    for (final book in books) {
      final key = BookIdentity.duplicateKey(book);
      if (existingKeys.contains(key) || !incomingKeys.add(key)) {
        unawaited(_deleteImportedBookFiles(book));
        continue;
      }
      uniqueBooks.add(book);
    }
    if (uniqueBooks.isEmpty) {
      _error = null;
      _messageChanges.notifyListeners();
      return 0;
    }
    _books = [...uniqueBooks, ..._books];
    _queueBooksSave(immediate: true);
    _error = null;
    _libraryChanges.notifyListeners();
    _messageChanges.notifyListeners();
    return uniqueBooks.length;
  }

  List<BookEntry> _deduplicateBooks(List<BookEntry> books) {
    final seen = <String>{};
    final result = <BookEntry>[];
    for (final book in books) {
      if (seen.add(BookIdentity.duplicateKey(book))) {
        result.add(book);
      }
    }
    if (result.length != books.length) {
      _queueBooksSave(immediate: true);
    }
    return result;
  }

  BookEntry? _findExistingDuplicateBook(BookEntry importedBook) {
    final importedKey = BookIdentity.duplicateKey(importedBook);
    for (final book in _books) {
      if (BookIdentity.duplicateKey(book) == importedKey) {
        return book;
      }
    }
    return null;
  }

  Future<void> _deleteImportedBookFiles(BookEntry book) async {
    try {
      final dir = Directory(book.bookDir);
      if (await dir.exists()) {
        await dir.delete(recursive: true);
      }
    } catch (_) {
      // Duplicate cleanup is best-effort; importing should not fail because of it.
    }
  }

  void _beginImportActivity({required String title, required String detail}) {
    _importActivityClearTimer?.cancel();
    _importActivity = ImportActivity(
      title: title,
      detail: detail,
      active: true,
      failed: false,
    );
    _messageChanges.notifyListeners();
  }

  void _finishImportActivity(String detail, {bool failed = false}) {
    _importActivityClearTimer?.cancel();
    _importActivity = ImportActivity(
      title: failed ? '导入失败' : '导入完成',
      detail: detail,
      active: false,
      failed: failed,
    );
    _messageChanges.notifyListeners();
    _importActivityClearTimer = Timer(const Duration(milliseconds: 1800), () {
      _clearImportActivity();
    });
  }

  void _clearImportActivity() {
    _importActivityClearTimer?.cancel();
    _importActivityClearTimer = null;
    if (_importActivity == null) {
      return;
    }
    _importActivity = null;
    _messageChanges.notifyListeners();
  }

  Future<BookEntry?> _findExistingExternalBook(PendingOpenBook pending) async {
    final pendingName = BookIdentity.normalizedFileName(pending.name);
    if (pendingName.isEmpty) {
      return null;
    }
    for (final book in _books) {
      if (BookIdentity.normalizedFileName(book.sourcePath) != pendingName) {
        continue;
      }
      if (pending.size == null) {
        return book;
      }
      try {
        final source = File(book.sourcePath);
        if (await source.exists() && await source.length() == pending.size) {
          return book;
        }
      } catch (_) {
        // If metadata cannot be read, keep looking instead of risking mismatch.
      }
    }
    return null;
  }

  double _readingProgress({
    required int chapterCount,
    required int chapterIndex,
    required int page,
    required int pageCount,
  }) {
    if (chapterCount <= 0) {
      return 0;
    }
    final safeChapter = chapterIndex.clamp(0, chapterCount - 1);
    final safePageCount = pageCount < 1 ? 1 : pageCount;
    final safePage = page.clamp(0, safePageCount - 1);
    final isLastChapter = safeChapter == chapterCount - 1;
    if (safePageCount <= 1) {
      return isLastChapter ? 1.0 : safeChapter / chapterCount;
    }
    final pageProgress = safePage / (safePageCount - 1);
    return ((safeChapter + pageProgress) / chapterCount).clamp(0.0, 1.0);
  }

  Future<void> updateBookProgress({
    required BookEntry book,
    required int chapterIndex,
    required int page,
    required int pageCount,
    double? displayProgress,
    bool force = false,
  }) async {
    final index = _books.indexWhere((item) => item.id == book.id);
    if (index == -1) {
      return;
    }
    final stored = _books[index];
    final safePageCount = pageCount < 1 ? 1 : pageCount;
    final safeChapterIndex = stored.chapters.isEmpty
        ? 0
        : chapterIndex.clamp(0, stored.chapters.length - 1);
    final safePage = page.clamp(0, safePageCount - 1);
    final progress =
        (displayProgress ??
                _readingProgress(
                  chapterCount: stored.chapters.length,
                  chapterIndex: safeChapterIndex,
                  page: safePage,
                  pageCount: safePageCount,
                ))
            .clamp(0.0, 1.0)
            .toDouble();
    final cachedPageCountChanged =
        stored.chapters.isNotEmpty &&
        stored.chapters[safeChapterIndex].cachedPageCount != safePageCount;
    final progressChanged = (stored.progress - progress).abs() >= .001;
    final samePosition =
        stored.currentChapterIndex == safeChapterIndex &&
        stored.currentPage == safePage &&
        stored.pageCount == safePageCount &&
        !progressChanged &&
        !cachedPageCountChanged;
    final now = DateTime.now();
    final lastReadFresh =
        stored.lastReadAt != null &&
        now.difference(stored.lastReadAt!).inSeconds < 20;
    if (!force && samePosition && lastReadFresh) {
      return;
    }
    final updated = stored.copyWith(
      chapters: cachedPageCountChanged
          ? [
              for (var i = 0; i < stored.chapters.length; i++)
                if (i == safeChapterIndex)
                  stored.chapters[i].copyWith(cachedPageCount: safePageCount)
                else
                  stored.chapters[i],
            ]
          : stored.chapters,
      currentChapterIndex: safeChapterIndex,
      currentPage: safePage,
      pageCount: safePageCount,
      progress: progress,
      lastReadAt: now,
    );
    _books = [
      for (var i = 0; i < _books.length; i++) i == index ? updated : _books[i],
    ];
    _queueBooksSave(immediate: force);
  }

  Future<void> cacheChapterPageCount({
    required BookEntry book,
    required int chapterIndex,
    required int pageCount,
  }) async {
    final index = _books.indexWhere((item) => item.id == book.id);
    if (index == -1 ||
        chapterIndex < 0 ||
        chapterIndex >= _books[index].chapters.length) {
      return;
    }
    final safePageCount = pageCount < 1 ? 1 : pageCount;
    if (_books[index].chapters[chapterIndex].cachedPageCount == safePageCount) {
      return;
    }
    _books = [
      for (var i = 0; i < _books.length; i++)
        if (i == index)
          _books[i].copyWith(
            chapters: [
              for (
                var chapter = 0;
                chapter < _books[i].chapters.length;
                chapter++
              )
                if (chapter == chapterIndex)
                  _books[i].chapters[chapter].copyWith(
                    cachedPageCount: safePageCount,
                  )
                else
                  _books[i].chapters[chapter],
            ],
          )
        else
          _books[i],
    ];
    _queueBooksSave();
  }

  Future<void> addBookBookmark(BookEntry book, BookBookmark bookmark) async {
    final index = _books.indexWhere((item) => item.id == book.id);
    if (index == -1) {
      return;
    }
    final stored = _books[index];
    final samePosition = stored.bookmarks.any(
      (item) =>
          item.chapterIndex == bookmark.chapterIndex &&
          item.page == bookmark.page,
    );
    final nextBookmarks = [
      bookmark,
      for (final item in stored.bookmarks)
        if (!samePosition ||
            item.chapterIndex != bookmark.chapterIndex ||
            item.page != bookmark.page)
          item,
    ];
    _books = [
      for (var i = 0; i < _books.length; i++)
        i == index ? stored.copyWith(bookmarks: nextBookmarks) : _books[i],
    ];
    _libraryChanges.notifyListeners();
    _queueBooksSave(immediate: true);
  }

  Future<void> removeBookBookmark(BookEntry book, String bookmarkId) async {
    if (bookmarkId.isEmpty) {
      return;
    }
    final index = _books.indexWhere((item) => item.id == book.id);
    if (index == -1) {
      return;
    }
    final stored = _books[index];
    final nextBookmarks = stored.bookmarks
        .where((bookmark) => bookmark.id != bookmarkId)
        .toList(growable: false);
    if (nextBookmarks.length == stored.bookmarks.length) {
      return;
    }
    _books = [
      for (var i = 0; i < _books.length; i++)
        i == index ? stored.copyWith(bookmarks: nextBookmarks) : _books[i],
    ];
    _libraryChanges.notifyListeners();
    _queueBooksSave(immediate: true);
  }

  Future<void> addReadingSeconds(int seconds, String bookId) async {
    if (seconds <= 0) {
      return;
    }
    final key = _dateKey(DateTime.now());
    final bookStats = Map<String, int>.from(_readingStats[bookId] ?? {});
    bookStats[key] = (bookStats[key] ?? 0) + seconds;
    _readingStats = {..._readingStats, bookId: bookStats};
    _dailyReadingTotals = {
      ..._dailyReadingTotals,
      key: (_dailyReadingTotals[key] ?? 0) + seconds,
    };
    _statisticsChanges.notifyListeners();
    _queueStatsSave();
  }

  int readingSecondsFor(DateTime date, {String? bookId}) {
    final key = _dateKey(date);
    if (bookId != null) {
      return _readingStats[bookId]?[key] ?? 0;
    }
    return _dailyReadingTotals[key] ?? 0;
  }

  Future<void> removeBook(BookEntry book) async {
    _books = _books.where((item) => item.id != book.id).toList();
    _readingStats = Map<String, Map<String, int>>.from(_readingStats)
      ..remove(book.id);
    _rebuildDailyReadingTotals();
    _libraryChanges.notifyListeners();
    _statisticsChanges.notifyListeners();
    _queueBooksSave(immediate: true);
    _queueStatsSave(immediate: true);
    if (book.bookDir.isNotEmpty) {
      final dir = Directory(book.bookDir);
      if (await dir.exists()) {
        await dir.delete(recursive: true);
      }
    }
  }

  Future<void> updateBookMetadata(
    BookEntry book, {
    required String title,
    required String author,
    String? coverPath,
  }) async {
    final index = _books.indexWhere((item) => item.id == book.id);
    if (index == -1) {
      return;
    }
    final trimmedTitle = title.trim();
    final trimmedAuthor = author.trim();
    if (trimmedTitle.isEmpty) {
      return;
    }
    _books = [
      for (var i = 0; i < _books.length; i++)
        if (i == index)
          _books[i].copyWith(
            title: trimmedTitle,
            author: trimmedAuthor.isEmpty ? '未知作者' : trimmedAuthor,
            coverPath: coverPath,
          )
        else
          _books[i],
    ];
    _libraryChanges.notifyListeners();
    _queueBooksSave(immediate: true);
  }

  Future<void> updateBookSeriesOverride(
    Set<String> bookIds,
    String? seriesName,
  ) async {
    if (bookIds.isEmpty) {
      return;
    }
    final trimmed = seriesName?.trim();
    final shouldClear = trimmed == null || trimmed.isEmpty;
    _books = [
      for (final book in _books)
        if (bookIds.contains(book.id))
          book.copyWith(
            seriesOverride: shouldClear ? null : trimmed,
            clearSeriesOverride: shouldClear,
          )
        else
          book,
    ];
    _libraryChanges.notifyListeners();
    _queueBooksSave(immediate: true);
  }

  Future<void> updateBookSeriesOrder(List<String> orderedBookIds) async {
    if (orderedBookIds.isEmpty) {
      return;
    }
    final orderById = <String, int>{
      for (var index = 0; index < orderedBookIds.length; index++)
        orderedBookIds[index]: index,
    };
    _books = [
      for (final book in _books)
        if (orderById.containsKey(book.id))
          book.copyWith(seriesOrder: orderById[book.id])
        else
          book,
    ];
    _libraryChanges.notifyListeners();
    _queueBooksSave(immediate: true);
  }

  Future<void> moveBooksToShelf(Set<String> bookIds, String? shelfName) async {
    if (bookIds.isEmpty) {
      return;
    }
    _books = [
      for (final book in _books)
        if (bookIds.contains(book.id))
          book.copyWith(shelfName: shelfName, clearShelf: shelfName == null)
        else
          book,
    ];
    _libraryChanges.notifyListeners();
    _queueBooksSave(immediate: true);
  }

  Future<void> createShelf(String name) async {
    final trimmed = name.trim();
    if (trimmed.isEmpty) {
      return;
    }
    if (_shelves.contains(trimmed)) {
      return;
    }
    _shelves = [..._shelves, trimmed];
    _libraryChanges.notifyListeners();
    _queueShelvesSave(immediate: true);
  }

  Future<void> deleteShelf(String name) async {
    final trimmed = name.trim();
    if (trimmed.isEmpty || !_shelves.contains(trimmed)) {
      return;
    }
    _shelves = _shelves.where((s) => s != trimmed).toList();
    _books = [
      for (final book in _books)
        if (book.shelfName == trimmed)
          book.copyWith(shelfName: null, clearShelf: true)
        else
          book,
    ];
    _libraryChanges.notifyListeners();
    _queueBooksSave(immediate: true);
    _queueShelvesSave(immediate: true);
  }

  Future<void> saveCloudSyncSettings(CloudSyncSettings settings) async {
    _cloudSyncSettings = settings;
    await _repository.saveCloudSyncSettings(settings);
    _cloudSyncChanges.notifyListeners();
  }

  Future<void> uploadCloudSync() async {
    final settings = _cloudSyncSettings;
    if (!settings.configured) {
      throw const CloudSyncException('请先填写 WebDAV 地址、账号和密码');
    }
    await _flushLocalState();
    await const CloudSyncService().upload(
      settings: settings,
      payload: _cloudSyncPayload(),
    );
    await saveCloudSyncSettings(
      settings.copyWith(enabled: true, lastUploadAt: DateTime.now()),
    );
  }

  Future<void> downloadCloudSync() async {
    final settings = _cloudSyncSettings;
    if (!settings.configured) {
      throw const CloudSyncException('请先填写 WebDAV 地址、账号和密码');
    }
    final payload = await const CloudSyncService().download(settings);
    await _applyCloudSyncPayload(payload);
    await saveCloudSyncSettings(
      _cloudSyncSettings.copyWith(
        enabled: true,
        lastDownloadAt: DateTime.now(),
      ),
    );
  }

  void clearError() {
    _error = null;
    _messageChanges.notifyListeners();
  }

  @visibleForTesting
  Future<void> applyCloudSyncPayloadForTest(Map<String, Object?> payload) {
    return _applyCloudSyncPayload(payload);
  }

  void refreshLibraryViews() {
    _libraryChanges.notifyListeners();
  }

  @override
  void dispose() {
    _styleSaveTimer?.cancel();
    _booksSaveTimer?.cancel();
    _statsSaveTimer?.cancel();
    _shelvesSaveTimer?.cancel();
    _importActivityClearTimer?.cancel();
    _appThemeChanges.dispose();
    _readingStyleChanges.dispose();
    _cloudSyncChanges.dispose();
    _libraryChanges.dispose();
    _statisticsChanges.dispose();
    _messageChanges.dispose();
    super.dispose();
  }

  void _queueStyleSave({bool immediate = false}) {
    _styleSaveTimer?.cancel();
    if (immediate) {
      _repository.saveStyle(_style);
      return;
    }
    _styleSaveTimer = Timer(
      const Duration(milliseconds: 650),
      () => _repository.saveStyle(_style),
    );
  }

  void _queueBooksSave({bool immediate = false}) {
    _booksSaveTimer?.cancel();
    if (immediate) {
      _saveBooksSnapshot();
      return;
    }
    _booksSaveTimer = Timer(
      const Duration(milliseconds: 900),
      _saveBooksSnapshot,
    );
  }

  void _saveBooksSnapshot() {
    if (_booksSaveInFlight) {
      _booksSavePending = true;
      return;
    }
    _booksSaveInFlight = true;
    final snapshot = _books;
    unawaited(
      _repository
          .saveBooks(snapshot)
          .catchError((Object error, StackTrace stackTrace) {
            debugPrint('SQuartor books save failed: $error');
          })
          .whenComplete(() {
            _booksSaveInFlight = false;
            if (_booksSavePending) {
              _booksSavePending = false;
              _saveBooksSnapshot();
            }
          }),
    );
  }

  void _queueStatsSave({bool immediate = false}) {
    _statsSaveTimer?.cancel();
    if (immediate) {
      _repository.saveReadingStats(_readingStats);
      return;
    }
    _statsSaveTimer = Timer(
      const Duration(seconds: 2),
      () => _repository.saveReadingStats(_readingStats),
    );
  }

  void _queueShelvesSave({bool immediate = false}) {
    _shelvesSaveTimer?.cancel();
    if (immediate) {
      _repository.saveShelves(_shelves);
      return;
    }
    _shelvesSaveTimer = Timer(
      const Duration(milliseconds: 650),
      () => _repository.saveShelves(_shelves),
    );
  }

  Future<void> _flushLocalState() async {
    _styleSaveTimer?.cancel();
    _booksSaveTimer?.cancel();
    _statsSaveTimer?.cancel();
    _shelvesSaveTimer?.cancel();
    await _repository.saveStyle(_style);
    await _repository.saveBooks(_books);
    await _repository.saveReadingStats(_readingStats);
    await _repository.saveShelves(_shelves);
  }

  Map<String, Object?> _cloudSyncPayload() {
    return _cloudSyncMerger.buildPayload(
      books: _books,
      shelves: _shelves,
      readingStats: _readingStats,
      now: DateTime.now(),
    );
  }

  Future<void> _applyCloudSyncPayload(Map<String, Object?> payload) async {
    final result = _cloudSyncMerger.mergePayload(
      payload: payload,
      localBooks: _books,
      localShelves: _shelves,
      localReadingStats: _readingStats,
    );
    if (result.shelvesChanged) {
      _shelves = result.shelves;
      await _repository.saveShelves(_shelves);
      _libraryChanges.notifyListeners();
    }
    if (result.booksChanged) {
      _books = result.books;
      await _repository.saveBooks(_books);
      _libraryChanges.notifyListeners();
    } else {
      // mergePayload may rebuild the list to merge stats per book even when
      // no individual book metadata changed; keep the latest reference so
      // identity-only updates aren't lost.
      _books = result.books;
    }
    if (result.readingStatsChanged) {
      _readingStats = result.readingStats;
      _rebuildDailyReadingTotals();
      await _repository.saveReadingStats(_readingStats);
      _statisticsChanges.notifyListeners();
    }
  }

  String _dateKey(DateTime date) {
    final local = DateTime(date.year, date.month, date.day);
    String two(int value) => value.toString().padLeft(2, '0');
    return '${local.year}-${two(local.month)}-${two(local.day)}';
  }

  void _rebuildDailyReadingTotals() {
    final totals = <String, int>{};
    for (final bookStats in _readingStats.values) {
      for (final entry in bookStats.entries) {
        totals[entry.key] = (totals[entry.key] ?? 0) + entry.value;
      }
    }
    _dailyReadingTotals = totals;
  }
}
