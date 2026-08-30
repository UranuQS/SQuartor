import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:archive/archive.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:gbk_codec/gbk_codec.dart';
import 'package:html/parser.dart' as html_parser;
import 'package:path/path.dart' as path;
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../models.dart';
import 'epub_parser.dart';
import 'epub_stream_reader.dart';
import 'txt_parser.dart';
import 'txt_stream_reader.dart';
import 'book_repository_types.dart';

String _encodeBooksForPrefs(List<BookEntry> books) {
  return jsonEncode(books.map((book) => book.toJson()).toList());
}

class BookRepository {
  static const _booksKey = 'books.v1';
  static const _styleKey = 'style.v1';
  static const _fontsKey = 'fonts.v1';
  static const _readingStatsKey = 'reading_stats.v1';
  static const _shelvesKey = 'shelves.v1';
  static const _cloudSyncSettingsKey = 'cloud_sync_settings.v1';
  static const _androidPickerChannel = MethodChannel('squartor/native_picker');

  Future<SharedPreferences>? _prefsFuture;

  Future<SharedPreferences> _prefs() {
    return _prefsFuture ??= SharedPreferences.getInstance();
  }

  Future<BookRepositorySnapshot> loadSnapshot() async {
    final prefs = await _prefs();
    var books = _decodeBooks(prefs.getString(_booksKey));
    books = await _upgradeImportedEpubs(books);
    books = await _upgradeImportedTxts(books);
    books = await _upgradeBookWordCounts(books);
    try {
      final docDir = await getApplicationDocumentsDirectory();
      // Purge leftover debug kernel_blob and isolate_snapshot_data (77MB+10MB)
      final flutterAssets = Directory(path.join(docDir.path, 'flutter_assets'));
      if (await flutterAssets.exists()) {
        try {
          await flutterAssets.delete(recursive: true);
        } catch (_) {}
      }
      for (final name in const ['books', 'picked_books', 'open_books', 'epub', 'temp_books']) {
        final d = Directory(path.join(docDir.path, name));
        if (await d.exists()) {
          try {
            await d.delete(recursive: true);
          } catch (_) {}
        }
      }
      final root = await _rootDir();
      final booksDir = Directory(path.join(root.path, 'books'));
      if (await booksDir.exists()) {
        for (final bookSub in booksDir.listSync().whereType<Directory>()) {
          await _cleanupRedundantBookFiles(bookSub);
        }
      }
    } catch (_) {}
    return BookRepositorySnapshot(
      books: books,
      fonts: _decodeFonts(prefs.getString(_fontsKey)),
      shelves: _decodeShelves(prefs.getString(_shelvesKey)),
      readingStats: _decodeReadingStats(prefs.getString(_readingStatsKey)),
      style: _decodeStyle(prefs.getString(_styleKey)),
      cloudSyncSettings: _decodeCloudSyncSettings(
        prefs.getString(_cloudSyncSettingsKey),
      ),
    );
  }

  Future<List<BookEntry>> loadBooks() async {
    final prefs = await _prefs();
    var books = _decodeBooks(prefs.getString(_booksKey));
    books = await _upgradeImportedEpubs(books);
    books = await _upgradeImportedTxts(books);
    return _upgradeBookWordCounts(books);
  }

  Future<void> saveBooks(List<BookEntry> books) async {
    final prefs = await _prefs();
    final payload = await compute(_encodeBooksForPrefs, books);
    await prefs.setString(_booksKey, payload);
  }

  Future<ReadingStyle> loadStyle() async {
    final prefs = await _prefs();
    return _decodeStyle(prefs.getString(_styleKey));
  }

  Future<void> saveStyle(ReadingStyle style) async {
    final prefs = await _prefs();
    await prefs.setString(_styleKey, jsonEncode(style.toJson()));
  }

  Future<List<ImportedFont>> loadFonts() async {
    final prefs = await _prefs();
    return _decodeFonts(prefs.getString(_fontsKey));
  }

  Future<void> saveFonts(List<ImportedFont> fonts) async {
    final prefs = await _prefs();
    await prefs.setString(
      _fontsKey,
      jsonEncode(fonts.map((font) => font.toJson()).toList()),
    );
  }

  Future<Map<String, Map<String, int>>> loadReadingStats() async {
    final prefs = await _prefs();
    return _decodeReadingStats(prefs.getString(_readingStatsKey));
  }

  Future<void> saveReadingStats(Map<String, Map<String, int>> stats) async {
    final prefs = await _prefs();
    await prefs.setString(_readingStatsKey, jsonEncode(stats));
  }

  Future<List<String>> loadShelves() async {
    final prefs = await _prefs();
    return _decodeShelves(prefs.getString(_shelvesKey));
  }

  Future<void> saveShelves(List<String> shelves) async {
    final prefs = await _prefs();
    await prefs.setString(_shelvesKey, jsonEncode(shelves));
  }

  Future<CloudSyncSettings> loadCloudSyncSettings() async {
    final prefs = await _prefs();
    return _decodeCloudSyncSettings(prefs.getString(_cloudSyncSettingsKey));
  }

  Future<void> saveCloudSyncSettings(CloudSyncSettings settings) async {
    final prefs = await _prefs();
    await prefs.setString(_cloudSyncSettingsKey, jsonEncode(settings.toJson()));
  }

  List<BookEntry> _decodeBooks(String? raw) {
    if (raw == null || raw.isEmpty) {
      return const [];
    }
    final decoded = jsonDecode(raw);
    if (decoded is! List) {
      return const [];
    }
    return decoded
        .whereType<Map>()
        .map((item) => BookEntry.fromJson(item.cast<String, Object?>()))
        .where((book) => book.id.isNotEmpty)
        .toList();
  }

  ReadingStyle _decodeStyle(String? raw) {
    if (raw == null || raw.isEmpty) {
      return const ReadingStyle();
    }
    final decoded = jsonDecode(raw);
    if (decoded is Map) {
      return ReadingStyle.fromJson(decoded.cast<String, Object?>());
    }
    return const ReadingStyle();
  }

  CloudSyncSettings _decodeCloudSyncSettings(String? raw) {
    if (raw == null || raw.isEmpty) {
      return const CloudSyncSettings();
    }
    final decoded = jsonDecode(raw);
    if (decoded is Map) {
      return CloudSyncSettings.fromJson(decoded.cast<String, Object?>());
    }
    return const CloudSyncSettings();
  }

  List<ImportedFont> _decodeFonts(String? raw) {
    if (raw == null || raw.isEmpty) {
      return const [];
    }
    final decoded = jsonDecode(raw);
    if (decoded is! List) {
      return const [];
    }
    return decoded
        .whereType<Map>()
        .map((item) => ImportedFont.fromJson(item.cast<String, Object?>()))
        .where((font) => font.path.isNotEmpty)
        .toList();
  }

  Map<String, Map<String, int>> _decodeReadingStats(String? raw) {
    if (raw == null || raw.isEmpty) {
      return const {};
    }
    final decoded = jsonDecode(raw);
    if (decoded is! Map) {
      return const {};
    }
    final firstValue = decoded.values.firstOrNull;
    if (firstValue is Map) {
      final result = <String, Map<String, int>>{};
      for (final entry in decoded.entries) {
        final bookId = entry.key.toString();
        final inner = entry.value;
        if (inner is Map) {
          result[bookId] = inner.map(
            (k, v) => MapEntry(k.toString(), v is num ? v.toInt() : 0),
          );
        }
      }
      return result;
    }
    final legacy = decoded.map(
      (key, value) =>
          MapEntry(key.toString(), value is num ? value.toInt() : 0),
    );
    return {'__legacy__': Map<String, int>.from(legacy)};
  }

  List<String> _decodeShelves(String? raw) {
    if (raw == null || raw.isEmpty) {
      return const [];
    }
    final decoded = jsonDecode(raw);
    if (decoded is! List) {
      return const [];
    }
    return decoded
        .whereType<String>()
        .map((name) => name.trim())
        .where((name) => name.isNotEmpty)
        .toList();
  }

  Future<BookEntry?> pickAndImportBook() async {
    final books = await pickAndImportBooks();
    return books.firstOrNull;
  }

  Future<BookEntry> importBookFile(String filePath) async {
    final file = File(filePath);
    if (!_isImportableBookPath(file.path) || !await file.exists()) {
      throw FileSystemException('Unsupported book file', filePath);
    }
    final extension = path.extension(file.path).toLowerCase();
    return extension == '.epub' ? _importEpub(file) : _importTxt(file);
  }

  Future<PendingOpenBook?> consumePendingOpenBook() async {
    if (!Platform.isAndroid) {
      return null;
    }
    final raw = await _androidPickerChannel.invokeMapMethod<String, Object?>(
      'consumePendingOpenBook',
    );
    if (raw == null) {
      return null;
    }
    final filePath = raw['path'] as String?;
    if (filePath == null || filePath.isEmpty) {
      return null;
    }
    final size = raw['size'];
    return PendingOpenBook(
      path: filePath,
      name: raw['name'] as String? ?? path.basename(filePath),
      size: size is int ? size : int.tryParse('$size'),
    );
  }

  Future<List<BookEntry>> pickAndImportBooks() async {
    if (Platform.isAndroid) {
      final nativePaths = await _pickAndroidBookFiles();
      if (nativePaths != null && nativePaths.isNotEmpty) {
        return _importBookFiles(nativePaths.map((filePath) => File(filePath)));
      }
    }
    final result = await FilePicker.pickFiles(
      type: FileType.custom,
      allowedExtensions: const ['txt', 'epub'],
      allowMultiple: true,
      withData: false,
    );
    final paths = result?.files.map((file) => file.path).whereType<String>();
    if (paths == null) {
      return const [];
    }
    return _importBookFiles(paths.map((filePath) => File(filePath)));
  }

  Future<List<String>?> _pickAndroidBookFiles() async {
    try {
      final result = await _androidPickerChannel.invokeListMethod<String>(
        'pickBookFiles',
      );
      return result;
    } on MissingPluginException {
      return null;
    }
  }

  Future<List<BookEntry>> pickAndImportBookDirectory() async {
    if (Platform.isAndroid) {
      final nativePaths = await _pickAndroidBookDirectory();
      if (nativePaths != null) {
        return _importBookFiles(nativePaths.map((filePath) => File(filePath)));
      }
    }
    String? selected;
    try {
      selected = await FilePicker.getDirectoryPath();
    } catch (_) {
      return pickAndImportBooks();
    }
    if (selected == null || selected.isEmpty) {
      return pickAndImportBooks();
    }
    final dir = Directory(selected);
    if (!await dir.exists()) {
      return pickAndImportBooks();
    }
    final files = <File>[];
    try {
      await for (final entity in dir.list(
        recursive: true,
        followLinks: false,
      )) {
        if (entity is! File || !_isImportableBookPath(entity.path)) {
          continue;
        }
        files.add(entity);
      }
    } on FileSystemException {
      return pickAndImportBooks();
    }
    files.sort((a, b) => a.path.toLowerCase().compareTo(b.path.toLowerCase()));
    if (files.isEmpty) {
      return pickAndImportBooks();
    }
    return _importBookFiles(files);
  }

  Future<List<String>?> _pickAndroidBookDirectory() async {
    try {
      final result = await _androidPickerChannel.invokeListMethod<String>(
        'pickBookDirectory',
      );
      return result;
    } on MissingPluginException {
      return null;
    }
  }

  Future<ImportedFont?> pickAndImportFont() async {
    final result = await FilePicker.pickFiles(
      type: FileType.custom,
      allowedExtensions: const ['ttf', 'otf'],
      withData: false,
    );
    final picked = result?.files.single.path;
    if (picked == null) {
      return null;
    }
    final source = File(picked);
    final fontsDir = Directory(path.join((await _rootDir()).path, 'fonts'));
    await fontsDir.create(recursive: true);
    final name = path.basenameWithoutExtension(picked);
    final target = File(
      path.join(
        fontsDir.path,
        '${_safeFileName(name)}${path.extension(picked).toLowerCase()}',
      ),
    );
    await source.copy(target.path);
    return ImportedFont(name: name, path: target.path);
  }

  Future<BookEntry> _importTxt(File source) async {
    final id = _newId();
    final bookDir = Directory(path.join((await _rootDir()).path, 'books', id));
    await bookDir.create(recursive: true);

    return TxtStreamReader.importTxtDirect(
      source,
      bookDir: bookDir,
      id: id,
      decodeText: _decodeText,
      estimateWordCount: _estimateWordCount,
    );
  }

  Future<List<BookEntry>> _importBookFiles(Iterable<File> files) async {
    final imported = <BookEntry>[];
    for (final file in files) {
      if (!_isImportableBookPath(file.path) || !await file.exists()) {
        continue;
      }
      try {
        final extension = path.extension(file.path).toLowerCase();
        imported.add(
          extension == '.epub'
              ? await _importEpub(file)
              : await _importTxt(file),
        );
      } catch (_) {
        // Keep batch import useful even if one file is malformed.
      }
    }
    return imported;
  }

  bool _isImportableBookPath(String filePath) {
    final extension = path.extension(filePath).toLowerCase();
    return extension == '.txt' || extension == '.epub';
  }

  Future<BookEntry> _importEpub(File source) async {
    final id = _newId();
    final bookDir = Directory(path.join((await _rootDir()).path, 'books', id));
    await bookDir.create(recursive: true);

    return EpubStreamReader.importEpubDirect(
      source,
      bookDir: bookDir,
      id: id,
      decodeText: _decodeText,
      estimateWordCount: _estimateWordCount,
    );
  }

  static Future<void> _cleanupRedundantBookFiles(Directory bookDir) async {
    if (!await bookDir.exists()) return;
    try {
      // 1. Delete epub/ extract directory completely (images, fonts, text)
      final extractDir = Directory(path.join(bookDir.path, 'epub'));
      if (await extractDir.exists()) {
        try {
          await extractDir.delete(recursive: true);
        } catch (_) {}
      }

      // 2. Delete reader/ html chapters directory completely
      final readerDir = Directory(path.join(bookDir.path, 'reader'));
      if (await readerDir.exists()) {
        try {
          await readerDir.delete(recursive: true);
        } catch (_) {}
      }

      // 3. Delete txt-reader/ directory completely
      final txtReaderDir = Directory(path.join(bookDir.path, 'txt-reader'));
      if (await txtReaderDir.exists()) {
        try {
          await txtReaderDir.delete(recursive: true);
        } catch (_) {}
      }

      // 4. Delete any duplicate raw files in bookDir root, keep ONLY cover.jpg / cover.png
      for (final entity in bookDir.listSync(recursive: false)) {
        if (entity is File) {
          final name = path.basename(entity.path).toLowerCase();
          if (name != 'cover.jpg' && name != 'cover.png' && name != 'cover.webp') {
            try {
              entity.deleteSync();
            } catch (_) {}
          }
        }
      }
    } catch (_) {}
  }

  Future<StorageStats> getStorageStats() async {
    var booksBytes = 0;
    var cacheBytes = 0;
    try {
      final root = await _rootDir();
      final booksDir = Directory(path.join(root.path, 'books'));
      if (await booksDir.exists()) {
        for (final file in booksDir.listSync(recursive: true).whereType<File>()) {
          try {
            booksBytes += file.lengthSync();
          } catch (_) {}
        }
      }
    } catch (_) {}

    try {
      final tempDir = await getTemporaryDirectory();
      if (await tempDir.exists()) {
        for (final file in tempDir.listSync(recursive: true).whereType<File>()) {
          try {
            cacheBytes += file.lengthSync();
          } catch (_) {}
        }
      }
    } catch (_) {}

    return StorageStats(booksBytes: booksBytes, cacheBytes: cacheBytes);
  }

  Future<void> clearCache() async {
    EpubStreamReader.clearCache();
    TxtStreamReader.clearCache();

    // 1. Clear InAppWebView disk and memory caches
    try {
      if (!kIsWeb && (Platform.isAndroid || Platform.isIOS)) {
        await InAppWebViewController.clearAllCache(includeDiskFiles: true);
        await WebStorageManager.instance().deleteAllData();
      }
    } catch (_) {}

    // 2. Clear temporary cache directory
    try {
      final tempDir = await getTemporaryDirectory();
      if (await tempDir.exists()) {
        for (final entity in tempDir.listSync()) {
          try {
            entity.deleteSync(recursive: true);
          } catch (_) {}
        }
      }
    } catch (_) {}

    // 3. Clean all legacy unzipped folders (epub/, reader/, txt-reader/) for all books
    try {
      final root = await _rootDir();
      final booksDir = Directory(path.join(root.path, 'books'));
      if (await booksDir.exists()) {
        for (final bookSub in booksDir.listSync().whereType<Directory>()) {
          await _cleanupRedundantBookFiles(bookSub);
        }
      }
    } catch (_) {}

    // 4. Clean legacy folders directly under application documents directory
    try {
      final docDir = await getApplicationDocumentsDirectory();
      final flutterAssets = Directory(path.join(docDir.path, 'flutter_assets'));
      if (await flutterAssets.exists()) {
        try {
          await flutterAssets.delete(recursive: true);
        } catch (_) {}
      }
      for (final name in const ['books', 'picked_books', 'open_books', 'epub', 'temp_books']) {
        final d = Directory(path.join(docDir.path, name));
        if (await d.exists()) {
          try {
            await d.delete(recursive: true);
          } catch (_) {}
        }
      }
    } catch (_) {}
  }

  Future<String?> _findSourceFileForBook(BookEntry book) async {
    final fileName = book.sourcePath != null && book.sourcePath!.isNotEmpty
        ? path.basename(book.sourcePath!)
        : '${book.title}.${book.format == BookFormat.epub ? 'epub' : 'txt'}';
    final candidateDirs = [
      '/sdcard/Download',
      '/storage/emulated/0/Download',
      '/storage/emulated/0/Books',
      '/storage/emulated/0/Documents',
      '/sdcard',
      book.bookDir,
    ];
    for (final dir in candidateDirs) {
      final candidate = File(path.join(dir, fileName));
      if (await candidate.exists()) {
        return candidate.path;
      }
    }
    for (final dirPath in candidateDirs) {
      final dir = Directory(dirPath);
      if (!await dir.exists()) continue;
      try {
        for (final entity in dir.listSync(recursive: false)) {
          if (entity is File) {
            final name = path.basename(entity.path);
            if (name.contains(book.title) &&
                (name.endsWith('.epub') || name.endsWith('.txt'))) {
              return entity.path;
            }
          }
        }
      } catch (_) {}
    }
    return null;
  }

  Future<List<BookEntry>> _upgradeImportedEpubs(List<BookEntry> books) async {
    var changed = false;
    final upgraded = <BookEntry>[];
    for (var book in books) {
      if (book.format != BookFormat.epub) {
        upgraded.add(book);
        continue;
      }

      var sourcePath = book.sourcePath;
      if (sourcePath == null || !await File(sourcePath).exists()) {
        final found = await _findSourceFileForBook(book);
        if (found != null) {
          sourcePath = found;
          book = book.copyWith(sourcePath: found);
          changed = true;
        }
      }

      // Check and restore cover if missing or broken
      final coverFile = book.coverPath != null ? File(book.coverPath!) : null;
      final coverExists = coverFile != null && await coverFile.exists();
      if (!coverExists && sourcePath != null && await File(sourcePath).exists()) {
        final restoredCover = await EpubStreamReader.extractCoverIfMissing(
          sourcePath,
          Directory(book.bookDir),
          decodeText: _decodeText,
        );
        if (restoredCover != null) {
          book = book.copyWith(coverPath: restoredCover);
          changed = true;
        }
      }

      // Opportunistically prune redundant intermediate files from disk
      await _cleanupRedundantBookFiles(Directory(book.bookDir));

      final isAlreadyStream = book.chapters.isNotEmpty &&
          book.chapters.every((chapter) => chapter.filePath.startsWith('sq-epub://'));
      if (isAlreadyStream) {
        upgraded.add(book);
        continue;
      }

      if (sourcePath != null && await File(sourcePath).exists()) {
        try {
          final streamBook = await EpubStreamReader.importEpubDirect(
            File(sourcePath),
            bookDir: Directory(book.bookDir),
            id: book.id,
            decodeText: _decodeText,
            estimateWordCount: _estimateWordCount,
          );
          final oldHref = book.safeCurrentChapter.href.split('#').first;
          final matchingIndex = streamBook.chapters.indexWhere(
            (chapter) => chapter.href.split('#').first == oldHref,
          );
          final proportionalIndex = streamBook.chapters.isEmpty
              ? 0
              : (book.progress * streamBook.chapters.length).floor().clamp(
                  0,
                  streamBook.chapters.length - 1,
                );
          upgraded.add(
            book.copyWith(
              chapters: streamBook.chapters,
              coverPath: streamBook.coverPath ?? book.coverPath,
              wordCount: streamBook.wordCount ?? book.wordCount,
              currentChapterIndex: matchingIndex >= 0
                  ? matchingIndex
                  : proportionalIndex,
              currentPage: book.currentPage,
              pageCount: book.pageCount,
              progress: book.progress,
            ),
          );
          changed = true;
          continue;
        } catch (_) {}
      }
      upgraded.add(book);
    }
    if (changed) {
      await saveBooks(upgraded);
    }
    return upgraded;
  }

  Future<List<BookEntry>> _upgradeImportedTxts(List<BookEntry> books) async {
    var changed = false;
    final upgraded = <BookEntry>[];
    for (var book in books) {
      if (book.format != BookFormat.txt) {
        upgraded.add(book);
        continue;
      }

      var sourcePath = book.sourcePath;
      if (sourcePath == null || !await File(sourcePath).exists()) {
        final found = await _findSourceFileForBook(book);
        if (found != null) {
          sourcePath = found;
          book = book.copyWith(sourcePath: found);
          changed = true;
        }
      }

      // Opportunistically prune duplicate txt files from disk
      await _cleanupRedundantBookFiles(Directory(book.bookDir));

      final isAlreadyStream = book.chapters.isNotEmpty &&
          book.chapters.every((chapter) => chapter.filePath.startsWith('sq-txt://'));
      if (isAlreadyStream) {
        upgraded.add(book);
        continue;
      }

      if (sourcePath != null && await File(sourcePath).exists()) {
        try {
          final streamBook = await TxtStreamReader.importTxtDirect(
            File(sourcePath),
            bookDir: Directory(book.bookDir),
            id: book.id,
            decodeText: _decodeText,
            estimateWordCount: _estimateWordCount,
          );
          final oldTitle = book.safeCurrentChapter.title;
          final matchingIndex = streamBook.chapters.indexWhere(
            (chapter) => chapter.title == oldTitle,
          );
          final proportionalIndex = streamBook.chapters.isEmpty
              ? 0
              : (book.progress * streamBook.chapters.length).floor().clamp(
                  0,
                  streamBook.chapters.length - 1,
                );
          upgraded.add(
            book.copyWith(
              chapters: streamBook.chapters,
              wordCount: streamBook.wordCount ?? book.wordCount,
              currentChapterIndex: matchingIndex >= 0
                  ? matchingIndex
                  : proportionalIndex,
              currentPage: book.currentPage,
              pageCount: book.pageCount,
              progress: book.progress,
            ),
          );
          changed = true;
          continue;
        } catch (_) {}
      }
      upgraded.add(book);
    }
    if (changed) {
      await saveBooks(upgraded);
    }
    return upgraded;
  }

  Future<List<BookEntry>> _upgradeBookWordCounts(List<BookEntry> books) async {
    var changed = false;
    final upgraded = <BookEntry>[];
    for (final book in books) {
      if (book.wordCount != null &&
          book.wordCount! > 0 &&
          book.chapters.every((chapter) => chapter.wordCount != null)) {
        upgraded.add(book);
        continue;
      }
      try {
        final chapters = book.format == BookFormat.txt
            ? await _estimateTxtChapterWordCounts(book)
            : await EpubParser.estimateGeneratedChapterWordCounts(
                book.chapters,
                estimateWordCount: _estimateWordCount,
              );
        final wordCount = _sumChapterWordCounts(chapters);
        if (wordCount > 0) {
          upgraded.add(book.copyWith(chapters: chapters, wordCount: wordCount));
          changed = true;
        } else {
          upgraded.add(book);
        }
      } catch (_) {
        upgraded.add(book);
      }
    }
    if (changed) {
      await saveBooks(upgraded);
    }
    return upgraded;
  }

  Future<List<ReaderChapter>> _estimateTxtChapterWordCounts(
    BookEntry book,
  ) async {
    final chapters = <ReaderChapter>[];
    for (final chapter in book.chapters) {
      final file = File(chapter.filePath);
      if (!await file.exists()) {
        chapters.add(chapter);
        continue;
      }
      try {
        final document = html_parser.parse(await file.readAsString());
        final text =
            document.body?.text ?? document.documentElement?.text ?? '';
        chapters.add(chapter.copyWith(wordCount: _estimateWordCount(text)));
      } catch (_) {
        chapters.add(chapter);
      }
    }
    if (chapters.any((chapter) => (chapter.wordCount ?? 0) > 0)) {
      return chapters;
    }
    final sourceFile = File(book.sourcePath);
    if (!await sourceFile.exists()) {
      return book.chapters;
    }
    final total = _estimateWordCount(
      _decodeText(await sourceFile.readAsBytes()),
    );
    if (book.chapters.isEmpty || total <= 0) {
      return book.chapters;
    }
    final average = (total / book.chapters.length).round().clamp(1, total);
    return book.chapters
        .map((chapter) => chapter.copyWith(wordCount: average))
        .toList();
  }

  int _sumChapterWordCounts(List<ReaderChapter> chapters) {
    return chapters.fold<int>(
      0,
      (sum, chapter) =>
          sum + ((chapter.wordCount ?? 0) > 0 ? chapter.wordCount! : 0),
    );
  }

  int _estimateWordCount(String text) {
    return RegExp(
      r'[\u3400-\u9FFF\uF900-\uFAFF]|[A-Za-z0-9]+',
    ).allMatches(text).length;
  }

  String _decodeText(List<int> bytes) {
    if (bytes.length >= 3 &&
        bytes[0] == 0xEF &&
        bytes[1] == 0xBB &&
        bytes[2] == 0xBF) {
      return utf8.decode(bytes.sublist(3), allowMalformed: true);
    }
    final utf8Text = utf8.decode(bytes, allowMalformed: true);
    final replacementCount = '\uFFFD'.allMatches(utf8Text).length;
    if (replacementCount > 4) {
      return gbk_bytes.decode(bytes);
    }
    return utf8Text;
  }

  Future<Directory> _rootDir() async {
    final dir = await getApplicationDocumentsDirectory();
    return Directory(path.join(dir.path, 'squartor'))
      ..createSync(recursive: true);
  }

  Future<String?> ensureDefaultReaderFont() async {
    try {
      final root = await _rootDir();
      final fontFile = File(path.join(root.path, 'fonts', 'NotoSansSC-VF.ttf'));
      if (await fontFile.exists()) {
        try {
          await fontFile.delete();
        } catch (_) {}
      }
    } catch (_) {}
    return null;
  }

  String _safeFileName(String input) {
    return input.replaceAll(RegExp(r'[\\/:*?"<>|]'), '_');
  }

  String _newId() => DateTime.now().microsecondsSinceEpoch.toString();
}
