import 'dart:convert';
import 'dart:io';
import 'dart:math' as math;
import 'dart:typed_data';

import 'package:archive/archive.dart';
import 'package:html/dom.dart' as dom;
import 'package:html/parser.dart' as html_parser;
import 'package:path/path.dart' as path;
import 'package:xml/xml.dart';

import '../epub_flow.dart';
import '../models.dart';
import 'epub_parser.dart';
import 'epub_types.dart';

class EpubStreamReader {
  static final Map<String, Archive> _archiveCache = {};
  static final Map<String, int> _archiveLastAccess = {};
  static const _maxCachedArchives = 3;

  static void clearCache() {
    _archiveCache.clear();
    _archiveLastAccess.clear();
  }

  static Future<Archive> getArchive(String epubPath) async {
    if (_archiveCache.containsKey(epubPath)) {
      _archiveLastAccess[epubPath] = DateTime.now().millisecondsSinceEpoch;
      return _archiveCache[epubPath]!;
    }
    final file = File(epubPath);
    if (!await file.exists()) {
      throw FileSystemException('EPUB source file not found', epubPath);
    }
    final bytes = await file.readAsBytes();
    final archive = ZipDecoder().decodeBytes(bytes);
    if (_archiveCache.length >= _maxCachedArchives) {
      final oldest = _archiveLastAccess.entries
          .reduce((a, b) => a.value < b.value ? a : b)
          .key;
      _archiveCache.remove(oldest);
      _archiveLastAccess.remove(oldest);
    }
    _archiveCache[epubPath] = archive;
    _archiveLastAccess[epubPath] = DateTime.now().millisecondsSinceEpoch;
    return archive;
  }

  static ArchiveFile? findArchiveFile(Archive archive, String relativeHref) {
    final normalized = EpubParser.normalizeEpubHref(relativeHref).toLowerCase();
    for (final file in archive.files) {
      if (!file.isFile) continue;
      final fileNorm = EpubParser.normalizeEpubHref(file.name).toLowerCase();
      if (fileNorm == normalized) {
        return file;
      }
    }
    final base = path.basename(relativeHref).toLowerCase();
    for (final file in archive.files) {
      if (!file.isFile) continue;
      if (path.basename(file.name).toLowerCase() == base) {
        return file;
      }
    }
    return null;
  }

  static Future<Uint8List?> readEpubImageBytes(
    String epubPath,
    String entryHref,
  ) async {
    try {
      final archive = await getArchive(epubPath);
      final file = findArchiveFile(archive, entryHref);
      if (file != null) {
        final content = file.content;
        if (content is Uint8List) {
          return content;
        }
        return Uint8List.fromList(content as List<int>);
      }
    } catch (_) {}
    return null;
  }

  static Future<BookEntry> importEpubDirect(
    File sourceFile, {
    required Directory bookDir,
    required String id,
    required String Function(List<int> bytes) decodeText,
    required int Function(String text) estimateWordCount,
  }) async {
    final archive = await getArchive(sourceFile.path);

    final containerFile = findArchiveFile(archive, 'META-INF/container.xml');
    if (containerFile == null) {
      throw const FormatException('Invalid EPUB: missing container.xml');
    }
    final containerXml = XmlDocument.parse(decodeText(containerFile.content as List<int>));
    final rootFilePath = containerXml
        .findAllElements('rootfile')
        .map((element) => element.getAttribute('full-path'))
        .whereType<String>()
        .firstOrNull;
    if (rootFilePath == null) {
      throw const FormatException('Invalid EPUB: missing rootfile full-path');
    }

    final opfFile = findArchiveFile(archive, rootFilePath);
    if (opfFile == null) {
      throw FormatException('Invalid EPUB: OPF file not found: $rootFilePath');
    }
    final opfXml = XmlDocument.parse(decodeText(opfFile.content as List<int>));
    final opfDir = path.posix.dirname(rootFilePath);

    final title = firstTextByLocalName(opfXml, 'title')?.trim();
    final author = firstTextByLocalName(opfXml, 'creator')?.trim();
    final fallbackTitle = path.basenameWithoutExtension(sourceFile.path);

    final manifest = <String, ManifestItem>{};
    for (final item in opfXml.findAllElements('item')) {
      final itemId = item.getAttribute('id');
      final href = item.getAttribute('href');
      if (itemId == null || href == null) continue;
      manifest[itemId] = ManifestItem(
        id: itemId,
        href: href,
        fullHref: EpubParser.normalizeEpubHref(path.posix.join(opfDir, href)),
        mediaType: item.getAttribute('media-type') ?? '',
        properties: item.getAttribute('properties') ?? '',
      );
    }

    final spine = opfXml
        .findAllElements('itemref')
        .map((item) {
          return SpineItem(
            idref: item.getAttribute('idref') ?? '',
            linear: item.getAttribute('linear')?.toLowerCase() != 'no',
            properties: item.getAttribute('properties') ?? '',
          );
        })
        .where((item) {
          final manifestItem = manifest[item.idref];
          return item.linear &&
              manifestItem != null &&
              EpubParser.isReadableDocument(manifestItem.mediaType);
        })
        .toList();

    final toc = _readTocFromArchive(archive, manifest, decodeText: decodeText);

    String? coverPath;
    final coverEntryHref = _findCoverEntryHref(opfXml, manifest);
    if (coverEntryHref != null) {
      final coverFile = findArchiveFile(archive, coverEntryHref);
      if (coverFile != null) {
        await bookDir.create(recursive: true);
        final coverOutput = File(path.join(bookDir.path, 'cover.jpg'));
        await coverOutput.writeAsBytes(coverFile.content as List<int>, flush: false);
        coverPath = coverOutput.path;
      }
    }

    final readableItems = spine
        .map((item) => manifest[item.idref])
        .whereType<ManifestItem>()
        .where((item) => findArchiveFile(archive, item.fullHref) != null)
        .toList();

    final indexByHref = <String, int>{
      for (var i = 0; i < readableItems.length; i++) readableItems[i].fullHref: i,
    };

    final boundaries = <ChapterBoundary>[];
    for (final item in toc) {
      final parts = EpubParser.splitEpubHref(item.href);
      final index = indexByHref[parts.path];
      if (index == null ||
          boundaries.any(
            (entry) =>
                entry.spineIndex == index && entry.anchor == parts.fragment,
          )) {
        continue;
      }
      boundaries.add(
        ChapterBoundary(
          spineIndex: index,
          title: item.title,
          href: item.href,
          anchor: parts.fragment,
          depth: item.depth,
          order: boundaries.length,
        ),
      );
    }
    boundaries.sort((a, b) {
      final spineCompare = a.spineIndex.compareTo(b.spineIndex);
      return spineCompare != 0 ? spineCompare : a.order.compareTo(b.order);
    });

    if (boundaries.isEmpty) {
      for (var i = 0; i < readableItems.length; i++) {
        final item = readableItems[i];
        final file = findArchiveFile(archive, item.fullHref);
        final raw = file != null ? decodeText(file.content as List<int>) : '';
        final doc = html_parser.parse(raw);
        final heading = doc.querySelector('h1, h2, h3, h4, h5, h6')?.text.trim();
        final docTitle = doc.querySelector('title')?.text.trim();
        final chapterTitle = (heading?.isNotEmpty == true
            ? heading!
            : docTitle?.isNotEmpty == true
            ? docTitle!
            : '第 ${i + 1} 节');

        boundaries.add(
          ChapterBoundary(
            spineIndex: i,
            title: chapterTitle,
            href: item.fullHref,
            depth: 0,
            order: boundaries.length,
          ),
        );
      }
    }

    final chapters = <ReaderChapter>[];
    for (var i = 0; i < boundaries.length; i++) {
      final boundary = boundaries[i];
      final nextBoundary = i + 1 < boundaries.length ? boundaries[i + 1] : null;
      final endAnchor = nextBoundary?.spineIndex == boundary.spineIndex
          ? nextBoundary?.anchor
          : null;
      final readableItem = readableItems[boundary.spineIndex];

      chapters.add(
        ReaderChapter(
          title: boundary.title,
          href: boundary.href,
          filePath: 'sq-epub://${sourceFile.path}#${readableItem.fullHref}',
          anchor: boundary.anchor,
          endAnchor: endAnchor,
          tocDepth: boundary.depth,
        ),
      );
    }

    return BookEntry(
      id: id,
      title: title?.isNotEmpty == true ? title! : fallbackTitle,
      author: author?.isNotEmpty == true ? author! : '未知作者',
      format: BookFormat.epub,
      bookDir: bookDir.path,
      sourcePath: sourceFile.path,
      importedAt: DateTime.now(),
      chapters: chapters,
      coverPath: coverPath,
    );
  }

  static List<TocItem> _readTocFromArchive(
    Archive archive,
    Map<String, ManifestItem> manifest, {
    required String Function(List<int> bytes) decodeText,
  }) {
    final navItem = manifest.values
        .where((item) => item.properties.split(' ').contains('nav'))
        .firstOrNull;
    if (navItem != null) {
      final file = findArchiveFile(archive, navItem.fullHref);
      if (file != null) {
        final document = html_parser.parse(decodeText(file.content as List<int>));
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
          return EpubParser.tocFromAnchor(anchor, base, depth: (depth - 1).clamp(0, 99));
        }).toList();
      }
    }

    final ncxItem = manifest.values
        .where((item) => item.mediaType.contains('ncx'))
        .firstOrNull;
    if (ncxItem != null) {
      final file = findArchiveFile(archive, ncxItem.fullHref);
      if (file != null) {
        final xml = XmlDocument.parse(decodeText(file.content as List<int>));
        final base = path.posix.dirname(ncxItem.fullHref);
        return xml.findAllElements('navPoint').map((point) {
          final text = firstTextByLocalName(point, 'text') ?? '章节';
          final src =
              point
                  .findAllElements('content')
                  .firstOrNull
                  ?.getAttribute('src') ??
              '';
          return TocItem(
            title: text.trim(),
            href: EpubParser.resolveEpubHref(base, src),
            depth: point.ancestors.where((node) {
              return node is XmlElement && node.localName == 'navPoint';
            }).length,
          );
        }).toList();
      }
    }

    return const [];
  }

  static String? _findCoverEntryHref(
    XmlDocument opfXml,
    Map<String, ManifestItem> manifest,
  ) {
    for (final meta in opfXml.findAllElements('meta')) {
      if (meta.getAttribute('name')?.toLowerCase() == 'cover') {
        final content = meta.getAttribute('content');
        if (content != null && manifest.containsKey(content)) {
          return manifest[content]!.fullHref;
        }
      }
    }
    for (final item in manifest.values) {
      if (item.properties.split(' ').contains('cover-image')) {
        return item.fullHref;
      }
    }
    for (final item in manifest.values) {
      final lower = item.href.toLowerCase();
      if (lower.contains('cover') &&
          (lower.endsWith('.jpg') ||
              lower.endsWith('.jpeg') ||
              lower.endsWith('.png') ||
              lower.endsWith('.webp'))) {
        return item.fullHref;
      }
    }
    return null;
  }

  static Future<String> readEpubChapterHtml(
    String epubPath,
    String chapterHref, {
    required String Function(List<int> bytes) decodeText,
  }) async {
    final archive = await getArchive(epubPath);
    final file = findArchiveFile(archive, chapterHref);
    if (file == null) {
      throw FileSystemException('EPUB chapter not found in archive', '$epubPath#$chapterHref');
    }
    final rawSource = decodeText(file.content as List<int>);

    final doc = html_parser.parse(rawSource);
    final cssSources = <String>[];
    for (final link in doc.querySelectorAll('link[rel*="stylesheet"], link[type*="css"]')) {
      final href = link.attributes['href'];
      if (href != null && href.isNotEmpty) {
        final resolvedCssPath = EpubParser.resolveEpubHref(
          path.posix.dirname(chapterHref),
          href,
        );
        final cssFile = findArchiveFile(archive, resolvedCssPath);
        if (cssFile != null) {
          try {
            cssSources.add(decodeText(cssFile.content as List<int>));
          } catch (_) {}
        }
      }
    }

    final flow = normalizeEpubFlow(
      rawSource,
      resolveLink: (href) => href,
      resolveResource: (href) {
        final resolved = EpubParser.resolveEpubHref(
          path.posix.dirname(chapterHref),
          href,
        );
        final imgFile = findArchiveFile(archive, resolved);
        if (imgFile != null) {
          final bytes = imgFile.content as List<int>;
          final ext = path.extension(resolved).toLowerCase();
          final mime = ext == '.png'
              ? 'image/png'
              : (ext == '.webp'
                  ? 'image/webp'
                  : (ext == '.gif'
                      ? 'image/gif'
                      : (ext == '.svg'
                          ? 'image/svg+xml'
                          : 'image/jpeg')));
          return 'data:$mime;base64,${base64Encode(bytes)}';
        }
        return href;
      },
      extraCssSources: cssSources,
    );

    final html = StringBuffer()
      ..writeln('<!doctype html><html><head><meta charset="utf-8">')
      ..writeln('<meta name="viewport" content="width=device-width, initial-scale=1">')
      ..writeln('<body class="${flow.isImageOnly ? 'sq-document-image-only' : 'sq-document-flow'}">')
      ..write(flow.renderFlow(chapterHref))
      ..writeln('</body></html>');

    return html.toString();
  }
}
