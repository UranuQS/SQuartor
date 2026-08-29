import 'dart:convert';
import 'dart:io';

import 'package:archive/archive.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:path/path.dart' as path;
// ignore: depend_on_referenced_packages
import 'package:path_provider_platform_interface/path_provider_platform_interface.dart';
// ignore: depend_on_referenced_packages
import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:squartor/src/book_repository.dart';

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
          expect(book.chapters, isNotEmpty, reason: file.path);
          for (var i = 0; i < book.chapters.length; i++) {
            final ch = book.chapters[i];
            final f = File(ch.filePath);
            expect(f.existsSync(), isTrue, reason: 'Chapter $i [${ch.title}] file missing: ${ch.filePath}');
            expect(f.lengthSync(), greaterThan(0), reason: 'Chapter $i [${ch.title}] file is empty');
            final content = f.readAsStringSync();
            expect(content.contains('<body class='), isTrue, reason: 'Chapter $i missing body');
            if (content.contains('境界设定')) {
              final idx = content.indexOf('境界设定');
              print('Found 境界设定 in [${ch.title}] of book [${book.title}]:');
              print(content.substring(idx - 100, (idx + 150).clamp(0, content.length)));
            }
          }
        }
      } finally {
        root.deleteSync(recursive: true);
      }
    },
    timeout: const Timeout(Duration(minutes: 3)),
  );
}
