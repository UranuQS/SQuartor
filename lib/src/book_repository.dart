import 'dart:convert';
import 'dart:io';

import 'package:archive/archive.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:gbk_codec/gbk_codec.dart';
import 'package:html/dom.dart' as dom;
import 'package:html/parser.dart' as html_parser;
import 'package:path/path.dart' as path;
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:xml/xml.dart';

import 'epub_flow.dart';
import 'models.dart';

String _encodeBooksForPrefs(List<BookEntry> books) {
  return jsonEncode(books.map((book) => book.toJson()).toList());
}

class BookRepository {
  static const _booksKey = 'books.v1';
  static const _styleKey = 'style.v1';
  static const _fontsKey = 'fonts.v1';
  static const _readingStatsKey = 'reading_stats.v1';
  static const _shelvesKey = 'shelves.v1';
  static const _androidPickerChannel = MethodChannel('squartor/native_picker');
  static const _epubReaderVersion = '4';
  static const _epubReaderVersionFile = '.flow-version';
  static const _txtReaderVersion = '4';
  static const _txtReaderVersionFile = '.txt-flow-version';
  static const _maxTxtDocumentChars = 180000;

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
    final text = _decodeText(await target.readAsBytes());
    final chapters = await _prepareTxtReader(
      sourceFile: target,
      bookDir: bookDir,
      title: title,
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
      wordCount: _estimateWordCount(text),
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
      final safePath = _safeJoin(extractDir.path, file.name);
      final output = File(safePath);
      await output.parent.create(recursive: true);
      await output.writeAsBytes(file.content as List<int>, flush: false);
    }

    final meta = await _parseEpub(
      extractDir,
      path.basenameWithoutExtension(source.path),
    );
    final wordCount = await _estimateGeneratedWordCount(meta.chapters);
    return BookEntry(
      id: id,
      title: meta.title,
      author: meta.author,
      format: BookFormat.epub,
      bookDir: bookDir.path,
      sourcePath: target.path,
      importedAt: DateTime.now(),
      chapters: meta.chapters,
      coverPath: meta.coverPath,
      wordCount: wordCount,
    );
  }

  Future<_EpubMeta> _parseEpub(
    Directory extractDir,
    String fallbackTitle,
  ) async {
    final container = File(
      path.join(extractDir.path, 'META-INF', 'container.xml'),
    );
    if (!await container.exists()) {
      return _EpubMeta(
        title: fallbackTitle,
        author: '未知作者',
        chapters: const [],
      );
    }
    final containerXml = XmlDocument.parse(await container.readAsString());
    final rootFile = containerXml
        .findAllElements('rootfile')
        .map((element) => element.getAttribute('full-path'))
        .whereType<String>()
        .firstOrNull;
    if (rootFile == null) {
      return _EpubMeta(
        title: fallbackTitle,
        author: '未知作者',
        chapters: const [],
      );
    }

    final opfPath = _safeJoin(extractDir.path, rootFile);
    final opfFile = File(opfPath);
    final opfXml = XmlDocument.parse(await opfFile.readAsString());
    final opfDir = path.posix.dirname(rootFile);
    final title = _firstTextByLocalName(opfXml, 'title')?.trim();
    final author = _firstTextByLocalName(opfXml, 'creator')?.trim();

    final manifest = <String, _ManifestItem>{};
    for (final item in opfXml.findAllElements('item')) {
      final id = item.getAttribute('id');
      final href = item.getAttribute('href');
      if (id == null || href == null) {
        continue;
      }
      manifest[id] = _ManifestItem(
        id: id,
        href: href,
        fullHref: _normalizeEpubHref(path.posix.join(opfDir, href)),
        mediaType: item.getAttribute('media-type') ?? '',
        properties: item.getAttribute('properties') ?? '',
      );
    }

    final spine = opfXml
        .findAllElements('itemref')
        .map((item) {
          return _SpineItem(
            idref: item.getAttribute('idref') ?? '',
            linear: item.getAttribute('linear')?.toLowerCase() != 'no',
            properties: item.getAttribute('properties') ?? '',
          );
        })
        .where((item) {
          final manifestItem = manifest[item.idref];
          return item.linear &&
              manifestItem != null &&
              _isReadableDocument(manifestItem.mediaType);
        })
        .toList();
    final toc = await _readEpubToc(extractDir, manifest);
    final chapters = await _buildEpubReaderChapters(
      extractDir: extractDir,
      manifest: manifest,
      spine: spine,
      toc: toc,
    );

    final coverPath = _findCoverPath(opfXml, manifest, extractDir.path);
    return _EpubMeta(
      title: title?.isEmpty == false ? title! : fallbackTitle,
      author: author?.isEmpty == false ? author! : '未知作者',
      chapters: chapters,
      coverPath: coverPath,
    );
  }

  Future<List<_TocItem>> _readEpubToc(
    Directory extractDir,
    Map<String, _ManifestItem> manifest,
  ) async {
    final navItem = manifest.values
        .where((item) => item.properties.split(' ').contains('nav'))
        .firstOrNull;
    if (navItem != null) {
      final file = File(_safeJoin(extractDir.path, navItem.fullHref));
      if (await file.exists()) {
        final document = html_parser.parse(await file.readAsString());
        final base = path.posix.dirname(navItem.fullHref);
        final nav =
            document.querySelector('nav[epub\\:type="toc"]') ??
            document.querySelector('nav[type="toc"]') ??
            document.querySelector('nav');
        final links = (nav ?? document).querySelectorAll('a[href]');
        return links.map((anchor) {
          var depth = 0;
          dom.Element? parent = anchor.parent;
          while (parent != null && parent != nav) {
            if (parent.localName == 'ol' || parent.localName == 'ul') {
              depth += 1;
            }
            parent = parent.parent;
          }
          return _tocFromAnchor(anchor, base, depth: (depth - 1).clamp(0, 99));
        }).toList();
      }
    }

    final ncxItem = manifest.values
        .where((item) => item.mediaType.contains('ncx'))
        .firstOrNull;
    if (ncxItem != null) {
      final file = File(_safeJoin(extractDir.path, ncxItem.fullHref));
      if (await file.exists()) {
        final xml = XmlDocument.parse(await file.readAsString());
        return xml.findAllElements('navPoint').map((point) {
          final text = _firstTextByLocalName(point, 'text') ?? '章节';
          final src =
              point
                  .findAllElements('content')
                  .firstOrNull
                  ?.getAttribute('src') ??
              '';
          final base = path.posix.dirname(ncxItem.fullHref);
          return _TocItem(
            title: text.trim(),
            href: _resolveEpubHref(base, src),
            depth: point.ancestors.where((node) {
              return node is XmlElement && node.localName == 'navPoint';
            }).length,
          );
        }).toList();
      }
    }
    return const [];
  }

  _TocItem _tocFromAnchor(
    dom.Element anchor,
    String baseHref, {
    int depth = 0,
  }) {
    final href = anchor.attributes['href'] ?? '';
    final title = anchor.text.trim().replaceAll(RegExp(r'\s+'), ' ');
    return _TocItem(
      title: title.isEmpty ? '章节' : title,
      href: _resolveEpubHref(baseHref, href),
      depth: depth,
    );
  }

  Future<List<ReaderChapter>> _buildEpubReaderChapters({
    required Directory extractDir,
    required Map<String, _ManifestItem> manifest,
    required List<_SpineItem> spine,
    required List<_TocItem> toc,
  }) async {
    final readableItems = spine
        .map((item) => manifest[item.idref])
        .whereType<_ManifestItem>()
        .where(
          (item) =>
              File(_safeJoin(extractDir.path, item.fullHref)).existsSync(),
        )
        .toList();
    if (readableItems.isEmpty) {
      return const [];
    }

    final indexByHref = <String, int>{
      for (var i = 0; i < readableItems.length; i++)
        readableItems[i].fullHref: i,
    };
    final boundaries = <_ChapterBoundary>[];
    for (final item in toc) {
      final parts = _splitEpubHref(item.href);
      final index = indexByHref[parts.path];
      if (index == null ||
          boundaries.any((entry) => entry.spineIndex == index)) {
        continue;
      }
      boundaries.add(
        _ChapterBoundary(
          spineIndex: index,
          title: item.title,
          href: item.href,
          anchor: parts.fragment,
          depth: item.depth,
        ),
      );
    }
    boundaries.sort((a, b) => a.spineIndex.compareTo(b.spineIndex));

    if (boundaries.isEmpty) {
      for (var i = 0; i < readableItems.length; i++) {
        final item = readableItems[i];
        boundaries.add(
          _ChapterBoundary(
            spineIndex: i,
            title: await _fallbackDocumentTitle(
              File(_safeJoin(extractDir.path, item.fullHref)),
              i + 1,
            ),
            href: item.fullHref,
            depth: 0,
          ),
        );
      }
    } else if (boundaries.first.spineIndex > 0) {
      boundaries.insert(
        0,
        _ChapterBoundary(
          spineIndex: 0,
          title: await _fallbackDocumentTitle(
            File(_safeJoin(extractDir.path, readableItems.first.fullHref)),
            1,
          ),
          href: readableItems.first.fullHref,
          depth: 0,
        ),
      );
    }

    final readerDir = Directory(path.join(extractDir.parent.path, 'reader'));
    await readerDir.create(recursive: true);
    final groups = <_ReaderChapterGroup>[];
    for (var i = 0; i < boundaries.length; i++) {
      final boundary = boundaries[i];
      final end = i + 1 < boundaries.length
          ? boundaries[i + 1].spineIndex
          : readableItems.length;
      groups.add(
        _ReaderChapterGroup(
          title: boundary.title,
          href: boundary.href,
          anchor: boundary.anchor,
          depth: boundary.depth,
          items: readableItems.sublist(boundary.spineIndex, end),
          outputFile: File(
            path.join(
              readerDir.path,
              'chapter-${i.toString().padLeft(4, '0')}.html',
            ),
          ),
        ),
      );
    }

    final groupBySourceHref = <String, _ReaderChapterGroup>{
      for (final group in groups)
        for (final item in group.items) item.fullHref: group,
    };
    for (final group in groups) {
      final sections = <String>[];
      var totalTextLength = 0;
      var totalMediaCount = 0;
      for (final item in group.items) {
        final sourceFile = File(_safeJoin(extractDir.path, item.fullHref));
        final flow = normalizeEpubFlow(
          _decodeText(await sourceFile.readAsBytes()),
          resolveLink: (href) => _readerLinkHref(
            href: href,
            sourceHref: item.fullHref,
            extractDir: extractDir,
            groupBySourceHref: groupBySourceHref,
          ),
          resolveResource: (href) => _readerResourceHref(
            href: href,
            sourceHref: item.fullHref,
            extractDir: extractDir,
          ),
        );
        if (flow.isEmpty) {
          continue;
        }
        totalTextLength += flow.textLength;
        totalMediaCount += flow.mediaCount;
        sections.add(flow.renderFlow(item.fullHref));
      }
      final imageOnly = totalTextLength == 0 && totalMediaCount == 1;
      final html = StringBuffer()
        ..writeln('<!doctype html><html><head><meta charset="utf-8">')
        ..writeln(
          '<meta name="viewport" content="width=device-width, initial-scale=1">',
        )
        ..writeln('<title>${htmlEscape.convert(group.title)}</title></head>')
        ..writeln(
          '<body class="${imageOnly ? 'sq-document-image-only' : 'sq-document-flow'}">',
        )
        ..writeAll(sections)
        ..writeln('</body></html>');
      await group.outputFile.writeAsString(html.toString(), encoding: utf8);
    }
    await File(
      path.join(readerDir.path, _epubReaderVersionFile),
    ).writeAsString(_epubReaderVersion, flush: true);

    return groups.map((group) {
      return ReaderChapter(
        title: group.title,
        href: group.href,
        filePath: group.outputFile.path,
        anchor: group.anchor,
        tocDepth: group.depth,
      );
    }).toList();
  }

  String _readerLinkHref({
    required String href,
    required String sourceHref,
    required Directory extractDir,
    required Map<String, _ReaderChapterGroup> groupBySourceHref,
  }) {
    final uri = Uri.tryParse(href);
    if (uri == null || uri.hasScheme) {
      return href;
    }
    if (href.startsWith('#')) {
      final group = groupBySourceHref[sourceHref];
      if (group == null) {
        return href;
      }
      return group.outputFile.uri
          .replace(fragment: Uri.decodeComponent(href.substring(1)))
          .toString();
    }
    final resolved = _splitEpubHref(
      _resolveEpubHref(path.posix.dirname(sourceHref), href),
    );
    final group = groupBySourceHref[resolved.path];
    if (group != null) {
      return group.outputFile.uri
          .replace(fragment: resolved.fragment)
          .toString();
    }
    return File(
      _safeJoin(extractDir.path, resolved.path),
    ).uri.replace(fragment: resolved.fragment).toString();
  }

  String _readerResourceHref({
    required String href,
    required String sourceHref,
    required Directory extractDir,
  }) {
    final uri = Uri.tryParse(href);
    if (uri == null || uri.hasScheme || href.startsWith('data:')) {
      return href;
    }
    final resolved = _splitEpubHref(
      _resolveEpubHref(path.posix.dirname(sourceHref), href),
    );
    return File(
      _safeJoin(extractDir.path, resolved.path),
    ).uri.replace(fragment: resolved.fragment).toString();
  }

  Future<String> _fallbackDocumentTitle(File file, int index) async {
    try {
      final document = html_parser.parse(_decodeText(await file.readAsBytes()));
      final heading = document.querySelector('h1, h2, h3, h4, h5, h6')?.text;
      final title = heading?.trim().replaceAll(RegExp(r'\s+'), ' ');
      if (title != null && title.isNotEmpty) {
        return title;
      }
    } catch (_) {
      // A malformed document remains readable through a generic title.
    }
    return '第 $index 章';
  }

  Future<List<BookEntry>> _upgradeImportedEpubs(List<BookEntry> books) async {
    var changed = false;
    final upgraded = <BookEntry>[];
    for (final book in books) {
      final readerDir = path.join(book.bookDir, 'reader');
      var alreadyPrepared = book.format != BookFormat.epub;
      if (book.format == BookFormat.epub) {
        final versionFile = File(path.join(readerDir, _epubReaderVersionFile));
        final hasCurrentVersion =
            await versionFile.exists() &&
            (await versionFile.readAsString()).trim() == _epubReaderVersion;
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
        final meta = await _parseEpub(
          Directory(path.join(book.bookDir, 'epub')),
          book.title,
        );
        final oldHref = book.safeCurrentChapter.href.split('#').first;
        final matchingIndex = meta.chapters.indexWhere(
          (chapter) => chapter.href.split('#').first == oldHref,
        );
        final proportionalIndex = meta.chapters.isEmpty
            ? 0
            : (book.progress * meta.chapters.length).floor().clamp(
                0,
                meta.chapters.length - 1,
              );
        upgraded.add(
          book.copyWith(
            title: meta.title,
            author: meta.author,
            chapters: meta.chapters,
            coverPath: meta.coverPath,
            wordCount: await _estimateGeneratedWordCount(meta.chapters),
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
      final versionFile = File(path.join(readerDir, _txtReaderVersionFile));
      final hasCurrentVersion =
          await versionFile.exists() &&
          (await versionFile.readAsString()).trim() == _txtReaderVersion;
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
        final chapters = await _prepareTxtReader(
          sourceFile: sourceFile,
          bookDir: Directory(book.bookDir),
          title: book.title,
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
            wordCount: _estimateWordCount(
              _decodeText(await sourceFile.readAsBytes()),
            ),
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
      if (book.wordCount != null && book.wordCount! > 0) {
        upgraded.add(book);
        continue;
      }
      try {
        final wordCount = book.format == BookFormat.txt
            ? await _estimateTxtBookWordCount(book)
            : await _estimateGeneratedWordCount(book.chapters);
        if (wordCount > 0) {
          upgraded.add(book.copyWith(wordCount: wordCount));
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

  Future<int> _estimateTxtBookWordCount(BookEntry book) async {
    final sourceFile = File(book.sourcePath);
    if (!await sourceFile.exists()) {
      return 0;
    }
    return _estimateWordCount(_decodeText(await sourceFile.readAsBytes()));
  }

  Future<int> _estimateGeneratedWordCount(List<ReaderChapter> chapters) async {
    var count = 0;
    final seen = <String>{};
    for (final chapter in chapters) {
      if (chapter.filePath.isEmpty || !seen.add(chapter.filePath)) {
        continue;
      }
      final file = File(chapter.filePath);
      if (!await file.exists()) {
        continue;
      }
      try {
        final document = html_parser.parse(await file.readAsString());
        count += _estimateWordCount(
          document.body?.text ?? document.documentElement?.text ?? '',
        );
      } catch (_) {
        // Ignore one broken generated file instead of hiding the whole book.
      }
    }
    return count;
  }

  int _estimateWordCount(String text) {
    return RegExp(
      r'[\u3400-\u9FFF\uF900-\uFAFF]|[A-Za-z0-9]+',
    ).allMatches(text).length;
  }

  String? _findCoverPath(
    XmlDocument opfXml,
    Map<String, _ManifestItem> manifest,
    String extractPath,
  ) {
    final coverId = opfXml
        .findAllElements('meta')
        .where((meta) => meta.getAttribute('name') == 'cover')
        .map((meta) => meta.getAttribute('content'))
        .whereType<String>()
        .firstOrNull;
    final coverItem = coverId != null
        ? manifest[coverId]
        : manifest.values
              .where(
                (item) => item.properties.split(' ').contains('cover-image'),
              )
              .firstOrNull;
    if (coverItem == null) {
      return null;
    }
    return _safeJoin(extractPath, coverItem.fullHref);
  }

  Future<List<ReaderChapter>> _prepareTxtReader({
    required File sourceFile,
    required Directory bookDir,
    required String title,
  }) async {
    final readerDir = Directory(path.join(bookDir.path, 'txt-reader'));
    if (await readerDir.exists()) {
      await readerDir.delete(recursive: true);
    }
    await readerDir.create(recursive: true);
    final documents = _buildTxtDocuments(
      _decodeText(await sourceFile.readAsBytes()),
      title,
    );
    final chapters = <ReaderChapter>[];
    for (final document in documents) {
      final output = File(path.join(readerDir.path, document.fileName));
      await output.writeAsString(
        _renderTxtDocument(document),
        encoding: utf8,
        flush: false,
      );
      chapters.add(
        ReaderChapter(
          title: document.title,
          href: '${document.fileName}#top',
          filePath: output.path,
          anchor: 'top',
        ),
      );
    }
    await File(
      path.join(readerDir.path, _txtReaderVersionFile),
    ).writeAsString(_txtReaderVersion, flush: true);
    return chapters;
  }

  List<_TxtDocument> _buildTxtDocuments(String text, String title) {
    final documents = <_TxtDocument>[];
    final paragraphs = <String>[];
    final normalized = text
        .replaceAll('\uFEFF', '')
        .replaceAll('\r\n', '\n')
        .replaceAll('\r', '\n');
    var currentTitle = '正文';
    var currentPart = 1;
    var documentChars = 0;
    var hasExplicitTitle = false;
    String? pendingParagraph;

    void commitDocument() {
      if (paragraphs.isEmpty && documents.isNotEmpty) {
        return;
      }
      final index = documents.length;
      final baseTitle = currentTitle.trim().isEmpty ? title : currentTitle;
      final documentTitle = currentPart <= 1
          ? baseTitle
          : '$baseTitle（$currentPart）';
      documents.add(
        _TxtDocument(
          title: documentTitle,
          fileName: 'txt-${index.toString().padLeft(4, '0')}.html',
          paragraphs: List<String>.of(paragraphs),
        ),
      );
      paragraphs.clear();
      documentChars = 0;
      currentPart += 1;
    }

    void addParagraph(String content) {
      for (final part in _splitLongTxtParagraph(content)) {
        final paragraph = part.trim();
        if (paragraph.isEmpty) {
          continue;
        }
        if (paragraphs.isNotEmpty &&
            documentChars + paragraph.length > _maxTxtDocumentChars) {
          commitDocument();
        }
        paragraphs.add(paragraph);
        documentChars += paragraph.length;
      }
    }

    void flushParagraph() {
      final content = pendingParagraph?.trim();
      if (content != null && content.isNotEmpty) {
        addParagraph(content);
      }
      pendingParagraph = null;
    }

    void startChapter(String chapterTitle) {
      flushParagraph();
      if (paragraphs.isNotEmpty || (hasExplicitTitle && documents.isEmpty)) {
        commitDocument();
      }
      currentTitle = _normalizeTxtTitle(chapterTitle);
      currentPart = 1;
      hasExplicitTitle = true;
    }

    for (final line in const LineSplitter().convert(normalized)) {
      final raw = line.replaceAll('\t', '  ').trimRight();
      final trimmed = raw.trim();
      if (_isTxtChapterTitle(trimmed)) {
        startChapter(trimmed);
        continue;
      }
      if (trimmed.isEmpty) {
        flushParagraph();
        continue;
      }
      final startsIndented = RegExp(r'^[\s　]{2,}').hasMatch(raw);
      if (pendingParagraph == null) {
        pendingParagraph = trimmed;
      } else if (_shouldJoinTxtLine(
        previous: pendingParagraph!,
        next: trimmed,
        nextStartsIndented: startsIndented,
      )) {
        pendingParagraph = '$pendingParagraph$trimmed';
      } else {
        flushParagraph();
        pendingParagraph = trimmed;
      }
    }
    flushParagraph();
    commitDocument();
    if (documents.isEmpty) {
      documents.add(
        _TxtDocument(
          title: title,
          fileName: 'txt-0000.html',
          paragraphs: const [],
        ),
      );
    }
    return documents;
  }

  String _normalizeTxtTitle(String title) {
    return title.replaceAll(RegExp(r'\s+'), ' ').trim();
  }

  bool _isTxtChapterTitle(String trimmed) {
    if (trimmed.isEmpty || trimmed.length > 64) {
      return false;
    }
    final title = _normalizeTxtTitle(trimmed);
    if (title.length < 2) {
      return false;
    }
    if (RegExp(
      r'^(?:https?://|www\.|qq|QQ群|群号|书友群|作者|PS[:：])',
      caseSensitive: false,
    ).hasMatch(title)) {
      return false;
    }
    if (RegExp(r'^[0-9０-９]{1,4}[:：][0-9０-９]{1,2}$').hasMatch(title)) {
      return false;
    }

    const cnNumber = '零〇一二三四五六七八九十百千万两壹贰叁肆伍陆柒捌玖拾佰仟';
    final chapterNumber = '[$cnNumber\\d０-９]+';
    final chapterUnits = '[章章节卷回部幕集话話]';
    final shortTail = r'(?:[\s:：、．.\-—].{0,48}|.{0,24})$';
    final patterns = [
      RegExp('^第\\s*$chapterNumber\\s*$chapterUnits$shortTail'),
      RegExp('^$chapterNumber\\s*$chapterUnits$shortTail'),
      RegExp(r'^[0-9０-９]{3,5}\s*[.．、:：\-]?\s*\S.{0,56}$'),
      RegExp(
        r'^(?:chapter|chap|ch\.?|episode|ep\.?|volume|vol\.?)\s+[0-9ivxlcdm]+(?:[\s:：.\-].{0,56})?$',
        caseSensitive: false,
      ),
      RegExp(r'^(?:序章|楔子|终章|尾声|后记|番外|幕间|插章)(?:[\s:：、．.\-].{0,56}|.{0,20})$'),
    ];
    return patterns.any((pattern) => pattern.hasMatch(title));
  }

  bool _shouldJoinTxtLine({
    required String previous,
    required String next,
    required bool nextStartsIndented,
  }) {
    if (nextStartsIndented) {
      return false;
    }
    if (_endsTxtParagraph(previous)) {
      return false;
    }
    if (previous.length > 48 || next.length > 48) {
      return false;
    }
    if (RegExp(r'^[“"「『（(《]').hasMatch(next)) {
      return false;
    }
    return true;
  }

  bool _endsTxtParagraph(String text) {
    return RegExp(r'[。！？!?；;：:…」』”’）)\]》]$').hasMatch(text.trim());
  }

  List<String> _splitLongTxtParagraph(String paragraph) {
    if (paragraph.length <= 420) {
      return [paragraph];
    }
    final parts = <String>[];
    final buffer = StringBuffer();
    for (final rune in paragraph.runes) {
      final char = String.fromCharCode(rune);
      buffer.write(char);
      final canBreak = RegExp(r'[。！？!?；;」』”’）)]').hasMatch(char);
      if ((buffer.length >= 160 && canBreak) || buffer.length >= 280) {
        parts.add(buffer.toString().trim());
        buffer.clear();
      }
    }
    final rest = buffer.toString().trim();
    if (rest.isNotEmpty) {
      parts.add(rest);
    }
    return parts;
  }

  String _renderTxtDocument(_TxtDocument document) {
    final buffer = StringBuffer()
      ..writeln('<!doctype html><html><head><meta charset="utf-8">')
      ..writeln(
        '<meta name="viewport" content="width=device-width, initial-scale=1">',
      )
      ..writeln('<title>${htmlEscape.convert(document.title)}</title></head>')
      ..writeln('<body class="sq-txt-document">')
      ..writeln('<h1 id="top">${htmlEscape.convert(document.title)}</h1>');
    for (final paragraph in document.paragraphs) {
      buffer.writeln('<p>${htmlEscape.convert(paragraph)}</p>');
    }
    buffer.writeln('</body></html>');
    return buffer.toString();
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

  String _safeJoin(String root, String relativePath) {
    final normalized = _normalizeEpubHref(relativePath);
    if (normalized.startsWith('../') || path.posix.isAbsolute(normalized)) {
      throw FileSystemException('Unsafe EPUB path', relativePath);
    }
    return path.joinAll([root, ...normalized.split('/')]);
  }

  String _normalizeEpubHref(String href) {
    final decoded = Uri.decodeFull(href).replaceAll('\\', '/');
    return path.posix.normalize(decoded).replaceFirst(RegExp(r'^\./'), '');
  }

  String _resolveEpubHref(String baseHref, String href) {
    final parts = href.split('#');
    final resolvedPath = _normalizeEpubHref(
      path.posix.join(baseHref, parts.first),
    );
    if (parts.length == 1 || parts.skip(1).join('#').isEmpty) {
      return resolvedPath;
    }
    return '$resolvedPath#${parts.skip(1).join('#')}';
  }

  _EpubHrefParts _splitEpubHref(String href) {
    final parts = href.split('#');
    return _EpubHrefParts(
      path: _normalizeEpubHref(parts.first),
      fragment: parts.length > 1
          ? Uri.decodeComponent(parts.skip(1).join('#'))
          : null,
    );
  }

  bool _isReadableDocument(String mediaType) {
    final normalized = mediaType.toLowerCase();
    return normalized.contains('xhtml') || normalized == 'text/html';
  }

  String _safeFileName(String input) {
    return input.replaceAll(RegExp(r'[\\/:*?"<>|]'), '_');
  }

  String _newId() => DateTime.now().microsecondsSinceEpoch.toString();
}

String? _firstTextByLocalName(XmlNode node, String localName) {
  return node.descendants
      .whereType<XmlElement>()
      .where((element) => element.name.local == localName)
      .map((element) => element.innerText)
      .firstOrNull;
}

class BookRepositorySnapshot {
  const BookRepositorySnapshot({
    required this.books,
    required this.fonts,
    required this.shelves,
    required this.readingStats,
    required this.style,
  });

  final List<BookEntry> books;
  final List<ImportedFont> fonts;
  final List<String> shelves;
  final Map<String, Map<String, int>> readingStats;
  final ReadingStyle style;
}

class _ManifestItem {
  const _ManifestItem({
    required this.id,
    required this.href,
    required this.fullHref,
    required this.mediaType,
    required this.properties,
  });

  final String id;
  final String href;
  final String fullHref;
  final String mediaType;
  final String properties;
}

class _SpineItem {
  const _SpineItem({
    required this.idref,
    required this.linear,
    required this.properties,
  });

  final String idref;
  final bool linear;
  final String properties;
}

class _TocItem {
  const _TocItem({required this.title, required this.href, this.depth = 0});

  final String title;
  final String href;
  final int depth;
}

class _ChapterBoundary {
  const _ChapterBoundary({
    required this.spineIndex,
    required this.title,
    required this.href,
    required this.depth,
    this.anchor,
  });

  final int spineIndex;
  final String title;
  final String href;
  final int depth;
  final String? anchor;
}

class _ReaderChapterGroup {
  const _ReaderChapterGroup({
    required this.title,
    required this.href,
    required this.items,
    required this.outputFile,
    required this.depth,
    this.anchor,
  });

  final String title;
  final String href;
  final String? anchor;
  final List<_ManifestItem> items;
  final File outputFile;
  final int depth;
}

class _EpubHrefParts {
  const _EpubHrefParts({required this.path, this.fragment});

  final String path;
  final String? fragment;
}

class _EpubMeta {
  const _EpubMeta({
    required this.title,
    required this.author,
    required this.chapters,
    this.coverPath,
  });

  final String title;
  final String author;
  final List<ReaderChapter> chapters;
  final String? coverPath;
}

class _TxtDocument {
  const _TxtDocument({
    required this.title,
    required this.fileName,
    required this.paragraphs,
  });

  final String title;
  final String fileName;
  final List<String> paragraphs;
}
