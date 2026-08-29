import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:archive/archive.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:gbk_codec/gbk_codec.dart';
import 'package:html/parser.dart' as html_parser;
import 'package:path/path.dart' as path;
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../models.dart';
import 'epub_parser.dart';
import 'txt_parser.dart';
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
    final result = await FilePicker.pickFiles(
      type: FileType.custom,
      allowedExtensions: const ['txt', 'epub'],
      withData: false,
    );
    final picked = result?.files.single.path;
    if (picked == null) {
      return null;
    }
    final extension = path.extension(picked).toLowerCase();
    if (extension == '.epub') {
      return _importEpub(File(picked));
    }
    return _importTxt(File(picked));
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
    final target = File(path.join(bookDir.path, path.basename(source.path)));
    await source.copy(target.path);

    final title = path.basenameWithoutExtension(source.path);
    final chapters = await TxtParser.prepareTxtReader(
      sourceFile: target,
      bookDir: bookDir,
      title: title,
      decodeText: _decodeText,
      estimateWordCount: _estimateWordCount,
    );

    return BookEntry(
      id: id,
      title: title,
      author: '本地 TXT',
      format: BookFormat.txt,
      bookDir: bookDir.path,
      sourcePath: target.path,
      importedAt: DateTime.now(),
      chapters: chapters,
      wordCount: _sumChapterWordCounts(chapters),
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
    final extractDir = Directory(path.join(bookDir.path, 'epub'));
    await extractDir.create(recursive: true);
    final target = File(path.join(bookDir.path, path.basename(source.path)));
    await source.copy(target.path);

    final archive = ZipDecoder().decodeBytes(await source.readAsBytes());
    for (final file in archive.files) {
      if (!file.isFile) {
        continue;
      }
      try {
        final safePath = EpubParser.safeJoin(extractDir.path, file.name);
        final output = File(safePath);
        await output.parent.create(recursive: true);
        await output.writeAsBytes(file.content as List<int>, flush: false);
      } catch (e) {
        try {
          final sanitizedName = file.name.replaceAll(RegExp(r'[:*?"<>|]'), '_');
          final safePath = EpubParser.safeJoin(extractDir.path, sanitizedName);
          final output = File(safePath);
          await output.parent.create(recursive: true);
          await output.writeAsBytes(file.content as List<int>, flush: false);
        } catch (_) {}
      }
    }

    final meta = await EpubParser.parseEpub(
      extractDir,
      path.basenameWithoutExtension(source.path),
      decodeText: _decodeText,
    );
    final chapters = await EpubParser.estimateGeneratedChapterWordCounts(
      meta.chapters,
      estimateWordCount: _estimateWordCount,
    );
    final wordCount = _sumChapterWordCounts(chapters);

    // Prune redundant intermediate text files (.xhtml, .html, .xml, .ncx, .opf) to save storage
    await _cleanupRedundantEpubFiles(extractDir);

    return BookEntry(
      id: id,
      title: meta.title,
      author: meta.author,
      format: BookFormat.epub,
      bookDir: bookDir.path,
      sourcePath: target.path,
      importedAt: DateTime.now(),
      chapters: chapters,
      coverPath: meta.coverPath,
      wordCount: wordCount,
    );
  }

  static Future<void> _cleanupRedundantEpubFiles(Directory extractDir) async {
    if (!await extractDir.exists()) return;
    try {
      final entities = extractDir.listSync(recursive: true);
      for (final entity in entities) {
        if (entity is File) {
          final ext = path.extension(entity.path).toLowerCase();
          if (const {'.xhtml', '.html', '.htm', '.xml', '.ncx', '.opf', '.txt'}.contains(ext)) {
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
    // 1. Clear temporary cache directory
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

    // 2. Clean all redundant epub files in books/
    try {
      final root = await _rootDir();
      final booksDir = Directory(path.join(root.path, 'books'));
      if (await booksDir.exists()) {
        for (final bookSub in booksDir.listSync().whereType<Directory>()) {
          final epubDir = Directory(path.join(bookSub.path, 'epub'));
          await _cleanupRedundantEpubFiles(epubDir);
        }
      }
    } catch (_) {}
  }

  Future<List<BookEntry>> _upgradeImportedEpubs(List<BookEntry> books) async {
    var changed = false;
    final upgraded = <BookEntry>[];
    for (final book in books) {
      final readerDir = path.join(book.bookDir, 'reader');
      var alreadyPrepared = book.format != BookFormat.epub;
      if (book.format == BookFormat.epub) {
        // Opportunistically prune redundant intermediate files from existing books
        unawaited(_cleanupRedundantEpubFiles(Directory(path.join(book.bookDir, 'epub'))));
        final versionFile = File(
          path.join(readerDir, EpubParser.epubReaderVersionFile),
        );
        final hasCurrentVersion =
            await versionFile.exists() &&
            (await versionFile.readAsString()).trim() ==
                EpubParser.epubReaderVersion;
        alreadyPrepared =
            hasCurrentVersion &&
            book.chapters.isNotEmpty &&
            book.chapters.every(
              (chapter) =>
                  path.equals(path.dirname(chapter.filePath), readerDir),
            );
      }
      if (alreadyPrepared) {
        upgraded.add(book);
        continue;
      }
      try {
        final meta = await EpubParser.parseEpub(
          Directory(path.join(book.bookDir, 'epub')),
          book.title,
          decodeText: _decodeText,
        );
        final chapters = await EpubParser.estimateGeneratedChapterWordCounts(
          meta.chapters,
          estimateWordCount: _estimateWordCount,
        );
        final oldHref = book.safeCurrentChapter.href.split('#').first;
        final matchingIndex = chapters.indexWhere(
          (chapter) => chapter.href.split('#').first == oldHref,
        );
        final proportionalIndex = chapters.isEmpty
            ? 0
            : (book.progress * chapters.length).floor().clamp(
                0,
                chapters.length - 1,
              );
        upgraded.add(
          book.copyWith(
            title: meta.title,
            author: meta.author,
            chapters: chapters,
            coverPath: meta.coverPath,
            wordCount: _sumChapterWordCounts(chapters),
            currentChapterIndex: matchingIndex >= 0
                ? matchingIndex
                : proportionalIndex,
            currentPage: book.currentPage,
            pageCount: book.pageCount,
            progress: book.progress,
          ),
        );
        changed = true;
      } catch (_) {
        upgraded.add(book);
      }
    }
    if (changed) {
      await saveBooks(upgraded);
    }
    return upgraded;
  }

  Future<List<BookEntry>> _upgradeImportedTxts(List<BookEntry> books) async {
    var changed = false;
    final upgraded = <BookEntry>[];
    for (final book in books) {
      if (book.format != BookFormat.txt) {
        upgraded.add(book);
        continue;
      }
      final readerDir = path.join(book.bookDir, 'txt-reader');
      final versionFile = File(
        path.join(readerDir, TxtParser.txtReaderVersionFile),
      );
      final hasCurrentVersion =
          await versionFile.exists() &&
          (await versionFile.readAsString()).trim() ==
              TxtParser.txtReaderVersion;
      final alreadyPrepared =
          hasCurrentVersion &&
          book.chapters.isNotEmpty &&
          book.chapters.every(
            (chapter) => path.equals(path.dirname(chapter.filePath), readerDir),
          );
      if (alreadyPrepared) {
        upgraded.add(book);
        continue;
      }
      final sourceFile = File(book.sourcePath);
      if (!await sourceFile.exists()) {
        upgraded.add(book);
        continue;
      }
      try {
        final chapters = await TxtParser.prepareTxtReader(
          sourceFile: sourceFile,
          bookDir: Directory(book.bookDir),
          title: book.title,
          decodeText: _decodeText,
          estimateWordCount: _estimateWordCount,
        );
        final oldTitle = book.safeCurrentChapter.title;
        final matchingIndex = chapters.indexWhere(
          (chapter) => chapter.title == oldTitle,
        );
        final proportionalIndex = chapters.isEmpty
            ? 0
            : (book.progress * chapters.length).floor().clamp(
                0,
                chapters.length - 1,
              );
        upgraded.add(
          book.copyWith(
            chapters: chapters,
            wordCount: _sumChapterWordCounts(chapters),
            currentChapterIndex: matchingIndex >= 0
                ? matchingIndex
                : proportionalIndex,
            currentPage: book.currentPage,
            pageCount: book.pageCount,
            progress: book.progress,
          ),
        );
        changed = true;
      } catch (_) {
        upgraded.add(book);
      }
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
      final fontsDir = Directory(path.join(root.path, 'fonts'))..createSync(recursive: true);
      final fontFile = File(path.join(fontsDir.path, 'NotoSansSC-VF.ttf'));
      if (!await fontFile.exists() || await fontFile.length() < 100000) {
        final data = await rootBundle.load('assets/fonts/NotoSansSC-VF.ttf');
        final bytes = data.buffer.asUint8List(data.offsetInBytes, data.lengthInBytes);
        await fontFile.writeAsBytes(bytes, flush: true);
      }
      return fontFile.uri.toString();
    } catch (_) {
      return null;
    }
  }

  String _safeFileName(String input) {
    return input.replaceAll(RegExp(r'[\\/:*?"<>|]'), '_');
  }

  String _newId() => DateTime.now().microsecondsSinceEpoch.toString();
}
