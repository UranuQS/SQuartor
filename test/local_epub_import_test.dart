import 'dart:convert';
import 'dart:io';
import 'dart:math' as math;

import 'package:archive/archive.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:path/path.dart' as path;
// ignore: depend_on_referenced_packages
import 'package:path_provider_platform_interface/path_provider_platform_interface.dart';
// ignore: depend_on_referenced_packages
import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:squartor/src/book_repository.dart';
import 'package:squartor/src/repository/epub_stream_reader.dart';

class _FakePathProviderPlatform extends Fake
    with MockPlatformInterfaceMixin
    implements PathProviderPlatform {
  _FakePathProviderPlatform(this.root);

  final String root;

  @override
  Future<String?> getApplicationDocumentsPath() async => root;
}

void main() {
  test(
    'imports local regression EPUB files when available',
    () async {
      final desktop = Directory(r'C:\Users\UranuQS\Desktop');
      final unigramDir = Directory(r'C:\Users\UranuQS\Downloads\38833FF26BA1D.UnigramPreview_g9c9v27vpyspw!App');
      final searchDirs = [desktop, unigramDir];
      final files = <File>[];
      for (final dir in searchDirs) {
        if (dir.existsSync()) {
          files.addAll(
            dir
                .listSync(recursive: true)
                .whereType<File>()
                .where((f) => f.path.toLowerCase().endsWith('.epub')),
          );
        }
      }
      print('Found ${files.length} EPUBs to test:');
      for (final f in files) {
        print('  - ${f.path}');
      }
      if (files.isEmpty) {
        return;
      }

      SharedPreferences.setMockInitialValues({});
      final root = Directory.systemTemp.createTempSync('squartor_import_test_');
      PathProviderPlatform.instance = _FakePathProviderPlatform(root.path);

      try {
        final repository = BookRepository();
        for (final file in files) {
          final book = await repository.importBookFile(file.path);
          print('Imported [${book.title}], chapters: ${book.chapters.length}');
          expect(book.chapters, isNotEmpty);
          final bookDir = Directory(book.bookDir);
          final allFiles = bookDir.listSync(recursive: true).whereType<File>().toList();
          var totalBytes = 0;
          for (final f in allFiles) {
            totalBytes += f.lengthSync();
          }
          print('Book [${book.title}] direct-stream disk size: ${(totalBytes / 1024).toStringAsFixed(1)} KB (Original file: ${(file.lengthSync() / (1024 * 1024)).toStringAsFixed(2)} MB)');
          expect(totalBytes, lessThan(500 * 1024), reason: 'Zero-copy direct stream must write < 500KB (only cover thumbnail)');

          // Test reading chapters in-memory
          for (var i = 0; i < math.min(10, book.chapters.length); i++) {
            final ch = book.chapters[i];
            expect(ch.filePath.startsWith('sq-epub://'), isTrue);
            final rest = ch.filePath.substring('sq-epub://'.length);
            final hash = rest.indexOf('#');
            final epubPath = hash >= 0 ? rest.substring(0, hash) : rest;
            final entry = hash >= 0 ? rest.substring(hash + 1) : '';
            final html = await EpubStreamReader.readEpubChapterHtml(
              epubPath,
              entry,
              decodeText: (b) => utf8.decode(b, allowMalformed: true),
            );
            expect(html, isNotEmpty);
          }
        }
      } finally {
        root.deleteSync(recursive: true);
      }
    },
    timeout: const Timeout(Duration(minutes: 3)),
  );
}
