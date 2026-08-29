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
          final bookDir = Directory(book.bookDir);
          final allFiles = bookDir.listSync(recursive: true).whereType<File>().toList();
          var totalBytes = 0;
          final bySubdir = <String, int>{};
          for (final f in allFiles) {
            final len = f.lengthSync();
            totalBytes += len;
            final rel = path.relative(f.path, from: bookDir.path);
            final top = rel.split(Platform.pathSeparator).first;
            bySubdir[top] = (bySubdir[top] ?? 0) + len;
          }
          print('Book [${book.title}] disk size: ${(totalBytes / (1024 * 1024)).toStringAsFixed(2)} MB (Original raw file was ${(file.lengthSync() / (1024 * 1024)).toStringAsFixed(2)} MB)');
          for (final entry in bySubdir.entries) {
            print('  - ${entry.key}: ${(entry.value / (1024 * 1024)).toStringAsFixed(2)} MB');
          }
          final epubDir = Directory(path.join(bookDir.path, 'epub'));
          if (epubDir.existsSync()) {
            final extSizes = <String, int>{};
            for (final f in epubDir.listSync(recursive: true).whereType<File>()) {
              final ext = path.extension(f.path).toLowerCase();
              extSizes[ext] = (extSizes[ext] ?? 0) + f.lengthSync();
            }
            print('  Breakdown of epub/ directory:');
            for (final entry in extSizes.entries) {
              print('    * ${entry.key}: ${(entry.value / (1024 * 1024)).toStringAsFixed(2)} MB');
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
