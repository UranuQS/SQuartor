import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
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
      final files = [
        File(r'C:\Users\UranuQS\Desktop\多看全屏版 凡人修仙传合集 校对全.epub'),
        File(r'C:\Users\UranuQS\Desktop\败北女角太多了！01.epub'),
      ].where((file) => file.existsSync()).toList();
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
