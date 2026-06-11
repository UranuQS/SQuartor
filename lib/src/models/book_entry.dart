import 'book_format.dart';

class ReaderChapter {
  const ReaderChapter({
    required this.title,
    required this.href,
    required this.filePath,
    this.anchor,
    this.endAnchor,
    this.tocDepth = 0,
    this.cachedPageCount,
  });

  final String title;
  final String href;
  final String filePath;
  final String? anchor;
  final String? endAnchor;
  final int tocDepth;
  final int? cachedPageCount;

  ReaderChapter copyWith({
    int? cachedPageCount,
    bool clearCachedPageCount = false,
  }) {
    return ReaderChapter(
      title: title,
      href: href,
      filePath: filePath,
      anchor: anchor,
      endAnchor: endAnchor,
      tocDepth: tocDepth,
      cachedPageCount: clearCachedPageCount
          ? null
          : cachedPageCount ?? this.cachedPageCount,
    );
  }

  Map<String, Object?> toJson() => {
    'title': title,
    'href': href,
    'filePath': filePath,
    'anchor': anchor,
    'endAnchor': endAnchor,
    'tocDepth': tocDepth,
    'cachedPageCount': cachedPageCount,
  };

  factory ReaderChapter.fromJson(Map<String, Object?> json) {
    return ReaderChapter(
      title: json['title'] as String? ?? '章节',
      href: json['href'] as String? ?? '',
      filePath: json['filePath'] as String? ?? '',
      anchor: json['anchor'] as String?,
      endAnchor: json['endAnchor'] as String?,
      tocDepth: json['tocDepth'] as int? ?? 0,
      cachedPageCount: json['cachedPageCount'] as int?,
    );
  }
}

class BookEntry {
  const BookEntry({
    required this.id,
    required this.title,
    required this.author,
    required this.format,
    required this.bookDir,
    required this.sourcePath,
    required this.importedAt,
    required this.chapters,
    this.coverPath,
    this.currentChapterIndex = 0,
    this.currentPage = 0,
    this.pageCount = 1,
    this.progress = 0,
    this.lastReadAt,
    this.shelfName,
    this.wordCount,
    this.seriesOverride,
  });

  final String id;
  final String title;
  final String author;
  final BookFormat format;
  final String bookDir;
  final String sourcePath;
  final DateTime importedAt;
  final List<ReaderChapter> chapters;
  final String? coverPath;
  final int currentChapterIndex;
  final int currentPage;
  final int pageCount;
  final double progress;
  final DateTime? lastReadAt;
  final String? shelfName;
  final int? wordCount;
  final String? seriesOverride;

  String get formatLabel => format == BookFormat.epub ? 'EPUB' : 'TXT';

  ReaderChapter get safeCurrentChapter {
    if (chapters.isEmpty) {
      return const ReaderChapter(title: '正文', href: '', filePath: '');
    }
    final index = currentChapterIndex.clamp(0, chapters.length - 1);
    return chapters[index];
  }

  BookEntry copyWith({
    String? title,
    String? author,
    List<ReaderChapter>? chapters,
    String? coverPath,
    int? currentChapterIndex,
    int? currentPage,
    int? pageCount,
    double? progress,
    DateTime? lastReadAt,
    String? shelfName,
    int? wordCount,
    String? seriesOverride,
    bool clearShelf = false,
    bool clearSeriesOverride = false,
  }) {
    return BookEntry(
      id: id,
      title: title ?? this.title,
      author: author ?? this.author,
      format: format,
      bookDir: bookDir,
      sourcePath: sourcePath,
      importedAt: importedAt,
      chapters: chapters ?? this.chapters,
      coverPath: coverPath ?? this.coverPath,
      currentChapterIndex: currentChapterIndex ?? this.currentChapterIndex,
      currentPage: currentPage ?? this.currentPage,
      pageCount: pageCount ?? this.pageCount,
      progress: progress ?? this.progress,
      lastReadAt: lastReadAt ?? this.lastReadAt,
      shelfName: clearShelf ? null : shelfName ?? this.shelfName,
      wordCount: wordCount ?? this.wordCount,
      seriesOverride: clearSeriesOverride
          ? null
          : seriesOverride ?? this.seriesOverride,
    );
  }

  Map<String, Object?> toJson() => {
    'id': id,
    'title': title,
    'author': author,
    'format': format.name,
    'bookDir': bookDir,
    'sourcePath': sourcePath,
    'importedAt': importedAt.toIso8601String(),
    'chapters': chapters.map((chapter) => chapter.toJson()).toList(),
    'coverPath': coverPath,
    'currentChapterIndex': currentChapterIndex,
    'currentPage': currentPage,
    'pageCount': pageCount,
    'progress': progress,
    'lastReadAt': lastReadAt?.toIso8601String(),
    'shelfName': shelfName,
    'wordCount': wordCount,
    'seriesOverride': seriesOverride,
  };

  factory BookEntry.fromJson(Map<String, Object?> json) {
    final chapterJson = json['chapters'];
    return BookEntry(
      id: json['id'] as String? ?? '',
      title: json['title'] as String? ?? '未命名书籍',
      author: json['author'] as String? ?? '未知作者',
      format:
          BookFormat.values
              .where((format) => format.name == json['format'])
              .firstOrNull ??
          BookFormat.txt,
      bookDir: json['bookDir'] as String? ?? '',
      sourcePath: json['sourcePath'] as String? ?? '',
      importedAt:
          DateTime.tryParse(json['importedAt'] as String? ?? '') ??
          DateTime.now(),
      chapters: chapterJson is List
          ? chapterJson
                .whereType<Map>()
                .map(
                  (chapter) =>
                      ReaderChapter.fromJson(chapter.cast<String, Object?>()),
                )
                .toList()
          : const [],
      coverPath: json['coverPath'] as String?,
      currentChapterIndex: json['currentChapterIndex'] as int? ?? 0,
      currentPage: json['currentPage'] as int? ?? 0,
      pageCount: json['pageCount'] as int? ?? 1,
      progress: (json['progress'] as num?)?.toDouble() ?? 0,
      lastReadAt: DateTime.tryParse(json['lastReadAt'] as String? ?? ''),
      shelfName: json['shelfName'] as String?,
      wordCount: json['wordCount'] as int?,
      seriesOverride: json['seriesOverride'] as String?,
    );
  }
}
