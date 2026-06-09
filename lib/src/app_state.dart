import 'dart:async';
import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'book_repository.dart';
import 'models.dart';

class AppState extends ChangeNotifier {
  AppState(this._repository) {
    unawaited(load());
  }

  final BookRepository _repository;
  final ChangeNotifier _appThemeChanges = ChangeNotifier();
  final ChangeNotifier _readingStyleChanges = ChangeNotifier();
  final ChangeNotifier _readerChromeChanges = ChangeNotifier();
  final ChangeNotifier _libraryChanges = ChangeNotifier();
  final ChangeNotifier _statisticsChanges = ChangeNotifier();
  final ChangeNotifier _messageChanges = ChangeNotifier();

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
  Brightness _platformBrightness = Brightness.dark;
  bool _loading = true;
  String? _error;
  Timer? _styleSaveTimer;
  Timer? _booksSaveTimer;
  Timer? _statsSaveTimer;
  Timer? _shelvesSaveTimer;
  bool _booksSaveInFlight = false;
  bool _booksSavePending = false;
  String? _appFontFamily;
  final Set<String> _registeredAppFontFamilies = {};

  List<BookEntry> get books => _books;
  List<ImportedFont> get fonts => _fonts;
  List<String> get shelves => _shelves;
  Map<String, Map<String, int>> get readingStats => _readingStats;
  ReadingStyle get style => _style;
  String? get appFontFamily => _appFontFamily;
  Brightness get effectiveBrightness {
    return switch (_style.brightnessMode) {
      AppBrightnessMode.light => Brightness.light,
      AppBrightnessMode.dark => Brightness.dark,
      AppBrightnessMode.system => _platformBrightness,
    };
  }

  AppPalette get palette => _style.resolvePalette(effectiveBrightness);
  bool get loading => _loading;
  String? get error => _error;

  void setPlatformBrightness(Brightness brightness) {
    if (_platformBrightness == brightness) {
      return;
    }
    _platformBrightness = brightness;
    if (_style.brightnessMode == AppBrightnessMode.system) {
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
      _books = snapshot.books;
      _fonts = snapshot.fonts;
      _shelves = snapshot.shelves;
      _readingStats = snapshot.readingStats;
      _rebuildDailyReadingTotals();
      _style = snapshot.style;
      _appFontFamily = await _registerAppFont(_style.appFontPath);
      _error = null;
    } catch (error) {
      _error = '加载数据失败：$error';
    } finally {
      _loading = false;
      _appThemeChanges.notifyListeners();
      _readingStyleChanges.notifyListeners();
      _libraryChanges.notifyListeners();
      _statisticsChanges.notifyListeners();
      _messageChanges.notifyListeners();
    }
  }

  Future<void> importBook() async {
    try {
      final book = await _repository.pickAndImportBook();
      if (book == null) {
        return;
      }
      _storeImportedBooks([book]);
    } catch (error) {
      _error = '导入失败：$error';
      _messageChanges.notifyListeners();
    }
  }

  Future<void> importBooks() async {
    try {
      final books = await _repository.pickAndImportBooks();
      _storeImportedBooks(books);
    } catch (error) {
      _error = '批量导入失败：$error';
      _messageChanges.notifyListeners();
    }
  }

  Future<void> importBookDirectory() async {
    try {
      final books = await _repository.pickAndImportBookDirectory();
      _storeImportedBooks(books);
    } catch (error) {
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
        _style.reverseTapPageTurn == style.reverseTapPageTurn &&
        _style.firstLineIndent == style.firstLineIndent &&
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
        _style.readerBackground != style.readerBackground;
    final paginationChanged =
        _style.fontSize != style.fontSize ||
        _style.lineHeight != style.lineHeight ||
        _style.paragraphSpacing != style.paragraphSpacing ||
        _style.letterSpacing != style.letterSpacing ||
        _style.pageMargin != style.pageMargin ||
        _style.verticalMargin != style.verticalMargin ||
        _style.firstLineIndent != style.firstLineIndent ||
        _style.fontPath != style.fontPath;
    if (appFontChanged) {
      _appFontFamily = await _registerAppFont(style.appFontPath);
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

  void _storeImportedBooks(List<BookEntry> books) {
    if (books.isEmpty) {
      return;
    }
    _books = [...books, ..._books];
    _queueBooksSave(immediate: true);
    _error = null;
    _libraryChanges.notifyListeners();
    _messageChanges.notifyListeners();
  }

  Future<String?> _registerAppFont(String? fontPath) async {
    if (fontPath == null || fontPath.isEmpty) {
      return null;
    }
    try {
      final file = File(fontPath);
      if (!await file.exists()) {
        return null;
      }
      final family = _appFontFamilyForPath(fontPath);
      if (_registeredAppFontFamilies.add(family)) {
        final loader = FontLoader(family);
        loader.addFont(
          file.readAsBytes().then(
            (bytes) => ByteData.view(
              bytes.buffer,
              bytes.offsetInBytes,
              bytes.lengthInBytes,
            ),
          ),
        );
        await loader.load();
      }
      return family;
    } catch (error) {
      debugPrint('SQuartor app font load failed: $error');
      return null;
    }
  }

  String _appFontFamilyForPath(String fontPath) {
    var hash = 0x811C9DC5;
    for (final codeUnit in fontPath.codeUnits) {
      hash ^= codeUnit;
      hash = (hash * 0x01000193) & 0xFFFFFFFF;
    }
    return 'SQuartorAppFont_${hash.toRadixString(16)}';
  }

  Future<void> updateBookProgress({
    required BookEntry book,
    required int chapterIndex,
    required int page,
    required int pageCount,
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
    final chapterPart = stored.chapters.isEmpty
        ? 0.0
        : safeChapterIndex / stored.chapters.length;
    final pagePart = stored.chapters.isEmpty
        ? 0.0
        : (safePage / safePageCount) / stored.chapters.length;
    final cachedPageCountChanged =
        stored.chapters.isNotEmpty &&
        stored.chapters[safeChapterIndex].cachedPageCount != safePageCount;
    final samePosition =
        stored.currentChapterIndex == safeChapterIndex &&
        stored.currentPage == safePage &&
        stored.pageCount == safePageCount &&
        !cachedPageCountChanged;
    final now = DateTime.now();
    final lastReadFresh =
        stored.lastReadAt != null &&
        now.difference(stored.lastReadAt!).inSeconds < 20;
    if (samePosition && lastReadFresh) {
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
      progress: (chapterPart + pagePart).clamp(0.0, 0.999),
      lastReadAt: now,
    );
    _books = [
      for (var i = 0; i < _books.length; i++) i == index ? updated : _books[i],
    ];
    _queueBooksSave();
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
          )
        else
          _books[i],
    ];
    _libraryChanges.notifyListeners();
    _queueBooksSave(immediate: true);
  }

  Future<void> reorderBooksInShelf({
    required String shelfName,
    required List<String> orderedIds,
  }) async {
    if (shelfName.trim().isEmpty || orderedIds.isEmpty) {
      return;
    }
    final reordered = [
      for (final id in orderedIds)
        _books.where((book) => book.id == id).firstOrNull,
    ].whereType<BookEntry>().toList();
    if (reordered.length != orderedIds.length) {
      return;
    }
    var cursor = 0;
    _books = [
      for (final book in _books)
        if (book.shelfName == shelfName) reordered[cursor++] else book,
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

  void clearError() {
    _error = null;
    _messageChanges.notifyListeners();
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
    _appThemeChanges.dispose();
    _readingStyleChanges.dispose();
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
