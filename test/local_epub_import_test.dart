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
      final files = (desktop.existsSync()
              ? desktop
                  .listSync()
                  .whereType<File>()
                  .where((f) => f.path.toLowerCase().endsWith('.epub'))
                  .toList()
              : <File>[]);
      print('Found ${files.length} desktop EPUBs to test:');
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
          expect(book.chapters, isNotEmpty, reason: file.path);
          for (var i = 0; i < book.chapters.length; i++) {
            final ch = book.chapters[i];
            final f = File(ch.filePath);
            expect(f.existsSync(), isTrue, reason: 'Chapter $i [${ch.title}] file missing: ${ch.filePath}');
            expect(f.lengthSync(), greaterThan(0), reason: 'Chapter $i [${ch.title}] file is empty');
            final content = f.readAsStringSync();
            expect(content.contains('<body class='), isTrue, reason: 'Chapter $i missing body');
            expect(
              content.contains('sq-flow-image') ||
                  content.contains('sq-title-block') ||
                  content.contains('<p') ||
                  content.contains('<h'),
              isTrue,
              reason: 'Chapter $i [${ch.title}] has no content elements',
            );
          }

          if (file.path.contains('败北女角')) {
            final generatedHtml = Directory(book.bookDir)
                .listSync(recursive: true)
                .whereType<File>()
                .where((file) => file.path.endsWith('.html'))
                .map((file) => file.readAsStringSync())
                .join('\n');
            expect(generatedHtml, isNot(contains('note.png')));
            expect(generatedHtml, contains('sq-footnote-ref'));
            expect(generatedHtml, contains('data-footnote='));
          }
        }
      } finally {
        root.deleteSync(recursive: true);
      }
    },
    timeout: const Timeout(Duration(minutes: 3)),
  );
}
