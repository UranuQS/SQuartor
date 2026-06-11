import 'dart:io';

import '../models.dart';

class ManifestItem {
  const ManifestItem({
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

class SpineItem {
  const SpineItem({
    required this.idref,
    required this.linear,
    required this.properties,
  });

  final String idref;
  final bool linear;
  final String properties;
}

class TocItem {
  const TocItem({required this.title, required this.href, this.depth = 0});

  final String title;
  final String href;
  final int depth;
}

class ChapterBoundary {
  const ChapterBoundary({
    required this.spineIndex,
    required this.title,
    required this.href,
    required this.depth,
    required this.order,
    this.anchor,
  });

  final int spineIndex;
  final String title;
  final String href;
  final int depth;
  final int order;
  final String? anchor;
}

class ReaderChapterGroup {
  const ReaderChapterGroup({
    required this.title,
    required this.href,
    required this.items,
    required this.outputFile,
    required this.depth,
    this.anchor,
    this.endAnchor,
  });

  final String title;
  final String href;
  final String? anchor;
  final String? endAnchor;
  final List<ManifestItem> items;
  final File outputFile;
  final int depth;
}

class EpubHrefParts {
  const EpubHrefParts({required this.path, this.fragment});

  final String path;
  final String? fragment;
}

class EpubMeta {
  const EpubMeta({
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
