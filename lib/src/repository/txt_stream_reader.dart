import 'dart:convert';
import 'dart:io';

import 'package:path/path.dart' as path;

import '../models.dart';
import 'txt_parser.dart';
import 'txt_types.dart';

class TxtStreamReader {
  static final Map<String, List<TxtDocument>> _documentCache = {};
  static final Map<String, int> _cacheAccess = {};
  static const _maxCachedTxts = 3;

  static void clearCache() {
    _documentCache.clear();
    _cacheAccess.clear();
  }

  static Future<List<TxtDocument>> getDocuments(
    String txtPath,
    String title, {
    required String Function(List<int> bytes) decodeText,
  }) async {
    if (_documentCache.containsKey(txtPath)) {
      _cacheAccess[txtPath] = DateTime.now().millisecondsSinceEpoch;
      return _documentCache[txtPath]!;
    }
    final file = File(txtPath);
    if (!await file.exists()) {
      throw FileSystemException('TXT source file not found', txtPath);
    }
    final rawBytes = await file.readAsBytes();
    final text = decodeText(rawBytes);
    final documents = TxtParser.buildTxtDocuments(text, title);

    if (_documentCache.length >= _maxCachedTxts) {
      final oldest = _cacheAccess.entries
          .reduce((a, b) => a.value < b.value ? a : b)
          .key;
      _documentCache.remove(oldest);
      _cacheAccess.remove(oldest);
    }
    _documentCache[txtPath] = documents;
    _cacheAccess[txtPath] = DateTime.now().millisecondsSinceEpoch;
    return documents;
  }

  static Future<BookEntry> importTxtDirect(
    File sourceFile, {
    required Directory bookDir,
    required String id,
    required String Function(List<int> bytes) decodeText,
    required int Function(String text) estimateWordCount,
  }) async {
    final title = path.basenameWithoutExtension(sourceFile.path);
    final documents = await getDocuments(sourceFile.path, title, decodeText: decodeText);

    final chapters = <ReaderChapter>[];
    for (var i = 0; i < documents.length; i++) {
      final doc = documents[i];
      final docText = doc.paragraphs.join('\n\n');
      final wordCount = estimateWordCount(docText);
      chapters.add(
        ReaderChapter(
          title: doc.title,
          href: 'txt-$i',
          filePath: 'sq-txt://${sourceFile.path}#$i',
          anchor: 'top',
          wordCount: wordCount,
        ),
      );
    }

    return BookEntry(
      id: id,
      title: title,
      author: '本地 TXT',
      format: BookFormat.txt,
      bookDir: bookDir.path,
      sourcePath: sourceFile.path,
      importedAt: DateTime.now(),
      chapters: chapters,
      wordCount: chapters.fold<int>(0, (sum, c) => sum + (c.wordCount ?? 0)),
    );
  }

  static Future<String> readTxtChapterHtml(
    String txtPath,
    int chapterIndex,
    String title, {
    required String Function(List<int> bytes) decodeText,
  }) async {
    final documents = await getDocuments(txtPath, title, decodeText: decodeText);
    if (chapterIndex < 0 || chapterIndex >= documents.length) {
      throw RangeError.index(chapterIndex, documents, 'chapterIndex');
    }
    return TxtParser.renderTxtDocument(documents[chapterIndex]);
  }
}
