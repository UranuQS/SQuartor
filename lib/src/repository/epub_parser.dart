import 'dart:convert';
import 'dart:io';
import 'dart:math' as math;

import 'package:html/dom.dart' as dom;
import 'package:html/parser.dart' as html_parser;
import 'package:path/path.dart' as path;
import 'package:xml/xml.dart';

import '../epub_flow.dart';
import '../models.dart';
import 'epub_types.dart';

String? firstTextByLocalName(XmlNode node, String localName) {
  return node.descendants
      .whereType<XmlElement>()
      .where((element) => element.name.local == localName)
      .map((element) => element.innerText)
      .firstOrNull;
}

class EpubParser {
  static const epubReaderVersion = '8';
  static const epubReaderVersionFile = '.flow-version';

  /// Parse an extracted EPUB directory and return metadata + chapters.
  static Future<EpubMeta> parseEpub(
    Directory extractDir,
    String fallbackTitle, {
    required String Function(List<int> bytes) decodeText,
  }) async {
    final container = File(
      path.join(extractDir.path, 'META-INF', 'container.xml'),
    );
    if (!await container.exists()) {
      return EpubMeta(title: fallbackTitle, author: '未知作者', chapters: const []);
    }
    final containerXml = XmlDocument.parse(await container.readAsString());
    final rootFile = containerXml
        .findAllElements('rootfile')
        .map((element) => element.getAttribute('full-path'))
        .whereType<String>()
        .firstOrNull;
    if (rootFile == null) {
      return EpubMeta(title: fallbackTitle, author: '未知作者', chapters: const []);
    }

    final opfPath = safeJoin(extractDir.path, rootFile);
    final opfFile = File(opfPath);
    final opfXml = XmlDocument.parse(await opfFile.readAsString());
    final opfDir = path.posix.dirname(rootFile);
    final title = firstTextByLocalName(opfXml, 'title')?.trim();
    final author = firstTextByLocalName(opfXml, 'creator')?.trim();

    final manifest = <String, ManifestItem>{};
    for (final item in opfXml.findAllElements('item')) {
      final id = item.getAttribute('id');
      final href = item.getAttribute('href');
      if (id == null || href == null) {
        continue;
      }
      manifest[id] = ManifestItem(
        id: id,
        href: href,
        fullHref: normalizeEpubHref(path.posix.join(opfDir, href)),
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
              isReadableDocument(manifestItem.mediaType);
        })
        .toList();
    final toc = await readEpubToc(extractDir, manifest, decodeText: decodeText);
    final chapters = await buildEpubReaderChapters(
      extractDir: extractDir,
      manifest: manifest,
      spine: spine,
      toc: toc,
      decodeText: decodeText,
    );

    final coverPath = findCoverPath(opfXml, manifest, extractDir.path);
    return EpubMeta(
      title: title?.isEmpty == false ? title! : fallbackTitle,
      author: author?.isEmpty == false ? author! : '未知作者',
      chapters: chapters,
      coverPath: coverPath,
    );
  }

  static Future<List<TocItem>> readEpubToc(
    Directory extractDir,
    Map<String, ManifestItem> manifest, {
    required String Function(List<int> bytes) decodeText,
  }) async {
    final navItem = manifest.values
        .where((item) => item.properties.split(' ').contains('nav'))
        .firstOrNull;
    if (navItem != null) {
      final file = File(safeJoin(extractDir.path, navItem.fullHref));
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
          return tocFromAnchor(anchor, base, depth: (depth - 1).clamp(0, 99));
        }).toList();
      }
    }

    final ncxItem = manifest.values
        .where((item) => item.mediaType.contains('ncx'))
        .firstOrNull;
    if (ncxItem != null) {
      final file = File(safeJoin(extractDir.path, ncxItem.fullHref));
      if (await file.exists()) {
        final xml = XmlDocument.parse(await file.readAsString());
        return xml.findAllElements('navPoint').map((point) {
          final text = firstTextByLocalName(point, 'text') ?? '章节';
          final src =
              point
                  .findAllElements('content')
                  .firstOrNull
                  ?.getAttribute('src') ??
              '';
          final base = path.posix.dirname(ncxItem.fullHref);
          return TocItem(
            title: text.trim(),
            href: resolveEpubHref(base, src),
            depth: point.ancestors.where((node) {
              return node is XmlElement && node.localName == 'navPoint';
            }).length,
          );
        }).toList();
      }
    }
    return const [];
  }

  static TocItem tocFromAnchor(
    dom.Element anchor,
    String baseHref, {
    int depth = 0,
  }) {
    final href = anchor.attributes['href'] ?? '';
    final title = anchor.text.trim().replaceAll(RegExp(r'\s+'), ' ');
    return TocItem(
      title: title.isEmpty ? '章节' : title,
      href: resolveEpubHref(baseHref, href),
      depth: depth,
    );
  }

  static Future<List<ReaderChapter>> buildEpubReaderChapters({
    required Directory extractDir,
    required Map<String, ManifestItem> manifest,
    required List<SpineItem> spine,
    required List<TocItem> toc,
    required String Function(List<int> bytes) decodeText,
  }) async {
    final readableItems = spine
        .map((item) => manifest[item.idref])
        .whereType<ManifestItem>()
        .where(
          (item) => File(safeJoin(extractDir.path, item.fullHref)).existsSync(),
        )
        .toList();
    if (readableItems.isEmpty) {
      return const [];
    }

    final indexByHref = <String, int>{
      for (var i = 0; i < readableItems.length; i++)
        readableItems[i].fullHref: i,
    };
    final boundaries = <ChapterBoundary>[];
    for (final item in toc) {
      final parts = splitEpubHref(item.href);
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
        boundaries.add(
          ChapterBoundary(
            spineIndex: i,
            title: await fallbackDocumentTitle(
              File(safeJoin(extractDir.path, item.fullHref)),
              i + 1,
              decodeText: decodeText,
            ),
            href: item.fullHref,
            depth: 0,
            order: boundaries.length,
          ),
        );
      }
    } else if (boundaries.first.spineIndex > 0) {
      boundaries.insert(
        0,
        ChapterBoundary(
          spineIndex: 0,
          title: await fallbackDocumentTitle(
            File(safeJoin(extractDir.path, readableItems.first.fullHref)),
            1,
            decodeText: decodeText,
          ),
          href: readableItems.first.fullHref,
          depth: 0,
          order: -1,
        ),
      );
    }

    final readerDir = Directory(path.join(extractDir.parent.path, 'reader'));
    await readerDir.create(recursive: true);
    final groups = <ReaderChapterGroup>[];
    for (var i = 0; i < boundaries.length; i++) {
      final boundary = boundaries[i];
      final nextBoundary = i + 1 < boundaries.length ? boundaries[i + 1] : null;
      final end = nextBoundary == null
          ? readableItems.length
          : math.max(boundary.spineIndex + 1, nextBoundary.spineIndex);
      groups.add(
        ReaderChapterGroup(
          title: boundary.title,
          href: boundary.href,
          anchor: boundary.anchor,
          endAnchor: nextBoundary?.spineIndex == boundary.spineIndex
              ? nextBoundary?.anchor
              : null,
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

    final groupBySourceHref = <String, ReaderChapterGroup>{
      for (final group in groups)
        for (final item in group.items) item.fullHref: group,
    };
    for (final group in groups) {
      final sections = <String>[];
      var totalTextLength = 0;
      var totalMediaCount = 0;
      for (final item in group.items) {
        final sourceFile = File(safeJoin(extractDir.path, item.fullHref));
        final flow = normalizeEpubFlow(
          decodeText(await sourceFile.readAsBytes()),
          resolveLink: (href) => readerLinkHref(
            href: href,
            sourceHref: item.fullHref,
            extractDir: extractDir,
            groupBySourceHref: groupBySourceHref,
          ),
          resolveResource: (href) => readerResourceHref(
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
      path.join(readerDir.path, epubReaderVersionFile),
    ).writeAsString(epubReaderVersion, flush: true);

    return groups.map((group) {
      return ReaderChapter(
        title: group.title,
        href: group.href,
        filePath: group.outputFile.path,
        anchor: group.anchor,
        endAnchor: group.endAnchor,
        tocDepth: group.depth,
      );
    }).toList();
  }

  static String readerLinkHref({
    required String href,
    required String sourceHref,
    required Directory extractDir,
    required Map<String, ReaderChapterGroup> groupBySourceHref,
  }) {
    final uri = Uri.tryParse(href);
    if (uri != null && uri.hasScheme) {
      return href;
    }
    if (href.startsWith('#')) {
      final group = groupBySourceHref[sourceHref];
      if (group == null) {
        return href;
      }
      return group.outputFile.uri
          .replace(fragment: decodeLooseFragment(href.substring(1)))
          .toString();
    }
    final resolved = splitEpubHref(
      resolveEpubHref(path.posix.dirname(sourceHref), href),
    );
    final group = groupBySourceHref[resolved.path];
    if (group != null) {
      return group.outputFile.uri
          .replace(fragment: resolved.fragment)
          .toString();
    }
    return File(
      safeJoin(extractDir.path, resolved.path),
    ).uri.replace(fragment: resolved.fragment).toString();
  }

  static String readerResourceHref({
    required String href,
    required String sourceHref,
    required Directory extractDir,
  }) {
    if (href.startsWith('data:')) {
      return href;
    }
    final uri = Uri.tryParse(href);
    if (uri != null && uri.hasScheme) {
      return href;
    }
    final resolved = splitEpubHref(
      resolveEpubHref(path.posix.dirname(sourceHref), href),
    );
    return File(
      safeJoin(extractDir.path, resolved.path),
    ).uri.replace(fragment: resolved.fragment).toString();
  }

  static Future<String> fallbackDocumentTitle(
    File file,
    int index, {
    required String Function(List<int> bytes) decodeText,
  }) async {
    try {
      final document = html_parser.parse(decodeText(await file.readAsBytes()));
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

  static String? findCoverPath(
    XmlDocument opfXml,
    Map<String, ManifestItem> manifest,
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
    return safeJoin(extractPath, coverItem.fullHref);
  }

  static Future<int> estimateGeneratedWordCount(
    List<ReaderChapter> chapters, {
    required int Function(String) estimateWordCount,
  }) async {
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
        count += estimateWordCount(
          document.body?.text ?? document.documentElement?.text ?? '',
        );
      } catch (_) {
        // Ignore one broken generated file instead of hiding the whole book.
      }
    }
    return count;
  }

  // --- EPUB href/path utilities ---

  static String safeJoin(String root, String relativePath) {
    final normalized = normalizeEpubHref(relativePath);
    if (normalized.startsWith('../') || path.posix.isAbsolute(normalized)) {
      throw FileSystemException('Unsafe EPUB path', relativePath);
    }
    return path.joinAll([root, ...normalized.split('/')]);
  }

  static String normalizeEpubHref(String href) {
    final decoded = decodeLooseUriComponent(
      href,
      full: true,
    ).replaceAll('\\', '/');
    return path.posix.normalize(decoded).replaceFirst(RegExp(r'^\./'), '');
  }

  static String resolveEpubHref(String baseHref, String href) {
    final parts = href.split('#');
    final resolvedPath = normalizeEpubHref(
      path.posix.join(baseHref, parts.first),
    );
    if (parts.length == 1 || parts.skip(1).join('#').isEmpty) {
      return resolvedPath;
    }
    return '$resolvedPath#${parts.skip(1).join('#')}';
  }

  static EpubHrefParts splitEpubHref(String href) {
    final parts = href.split('#');
    return EpubHrefParts(
      path: normalizeEpubHref(parts.first),
      fragment: parts.length > 1
          ? decodeLooseFragment(parts.skip(1).join('#'))
          : null,
    );
  }

  static String decodeLooseFragment(String value) {
    return decodeLooseUriComponent(value, full: false);
  }

  static String decodeLooseUriComponent(String value, {required bool full}) {
    try {
      return full ? Uri.decodeFull(value) : Uri.decodeComponent(value);
    } on FormatException {
      final repaired = value.replaceAllMapped(
        RegExp(r'%(?![0-9A-Fa-f]{2})'),
        (_) => '%25',
      );
      try {
        return full ? Uri.decodeFull(repaired) : Uri.decodeComponent(repaired);
      } on FormatException {
        return repaired;
      } on ArgumentError {
        return repaired;
      }
    } on ArgumentError {
      final repaired = value.replaceAllMapped(
        RegExp(r'%(?![0-9A-Fa-f]{2})'),
        (_) => '%25',
      );
      try {
        return full ? Uri.decodeFull(repaired) : Uri.decodeComponent(repaired);
      } on FormatException {
        return repaired;
      } on ArgumentError {
        return repaired;
      }
    }
  }

  static bool isReadableDocument(String mediaType) {
    final normalized = mediaType.toLowerCase();
    return normalized.contains('xhtml') || normalized == 'text/html';
  }
}
