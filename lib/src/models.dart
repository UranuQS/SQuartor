import 'dart:ui';

enum BookFormat { txt, epub }

String bookWordCountLabel(int? wordCount) {
  final count = wordCount ?? 0;
  if (count <= 0) {
    return '字数未知';
  }
  if (count >= 1000000) {
    final value = count / 10000;
    return '${value.toStringAsFixed(value >= 100 ? 0 : 1)}万字';
  }
  if (count >= 10000) {
    return '${(count / 10000).toStringAsFixed(1)}万字';
  }
  return '$count 字';
}

enum AppThemeId { wine, blue, green, brown, purple, black }

enum ReaderBackgroundId { theme, warm, black, paper, green }

enum AppBrightnessMode { system, light, dark }

class AppPalette {
  const AppPalette({
    required this.id,
    required this.label,
    required this.background,
    required this.surface,
    required this.card,
    required this.cardAlt,
    required this.line,
    required this.primary,
    required this.primarySoft,
    required this.text,
    required this.muted,
    required this.subtle,
    required this.blueMuted,
  });

  final AppThemeId id;
  final String label;
  final Color background;
  final Color surface;
  final Color card;
  final Color cardAlt;
  final Color line;
  final Color primary;
  final Color primarySoft;
  final Color text;
  final Color muted;
  final Color subtle;
  final Color blueMuted;

  bool get isLight => background.computeLuminance() > .5;
  Color get accentText => isLight ? primary : primarySoft;
}

class ThemeSeed {
  const ThemeSeed({required this.id, required this.label, required this.color});

  final AppThemeId id;
  final String label;
  final Color color;
}

const themeSeeds = <AppThemeId, ThemeSeed>{
  AppThemeId.wine: ThemeSeed(
    id: AppThemeId.wine,
    label: '酒红',
    color: Color(0xFFB00046),
  ),
  AppThemeId.blue: ThemeSeed(
    id: AppThemeId.blue,
    label: '深蓝',
    color: Color(0xFF2F6FA8),
  ),
  AppThemeId.green: ThemeSeed(
    id: AppThemeId.green,
    label: '墨绿',
    color: Color(0xFF2E7D5F),
  ),
  AppThemeId.brown: ThemeSeed(
    id: AppThemeId.brown,
    label: '暖棕',
    color: Color(0xFF9B522F),
  ),
  AppThemeId.purple: ThemeSeed(
    id: AppThemeId.purple,
    label: '紫黑',
    color: Color(0xFF7D4CC2),
  ),
  AppThemeId.black: ThemeSeed(
    id: AppThemeId.black,
    label: '纯黑',
    color: Color(0xFF5B6472),
  ),
};

class ReaderPalette {
  const ReaderPalette({
    required this.id,
    required this.label,
    required this.background,
    required this.text,
    required this.muted,
  });

  final ReaderBackgroundId id;
  final String label;
  final Color background;
  final Color text;
  final Color muted;
}

const appPalettes = <AppThemeId, AppPalette>{
  AppThemeId.wine: AppPalette(
    id: AppThemeId.wine,
    label: '酒红',
    background: Color(0xFF120B10),
    surface: Color(0xFF1E1118),
    card: Color(0xFF2B1821),
    cardAlt: Color(0xFF351B27),
    line: Color(0xFF553141),
    primary: Color(0xFFB00046),
    primarySoft: Color(0xFFFF9FBD),
    text: Color(0xFFF8EDF2),
    muted: Color(0xFFC9B8C0),
    subtle: Color(0xFF8E7C85),
    blueMuted: Color(0xFFA9D5FF),
  ),
  AppThemeId.blue: AppPalette(
    id: AppThemeId.blue,
    label: '深蓝',
    background: Color(0xFF0A1017),
    surface: Color(0xFF111B25),
    card: Color(0xFF172838),
    cardAlt: Color(0xFF1D3347),
    line: Color(0xFF32495F),
    primary: Color(0xFF2F6FA8),
    primarySoft: Color(0xFFA9D5FF),
    text: Color(0xFFEAF3FF),
    muted: Color(0xFFB8C7D6),
    subtle: Color(0xFF7890A4),
    blueMuted: Color(0xFFA9D5FF),
  ),
  AppThemeId.green: AppPalette(
    id: AppThemeId.green,
    label: '墨绿',
    background: Color(0xFF08120F),
    surface: Color(0xFF101D18),
    card: Color(0xFF172A22),
    cardAlt: Color(0xFF1F372D),
    line: Color(0xFF315748),
    primary: Color(0xFF2E7D5F),
    primarySoft: Color(0xFFA7E6CE),
    text: Color(0xFFEDF8F3),
    muted: Color(0xFFB9CFC5),
    subtle: Color(0xFF799387),
    blueMuted: Color(0xFFB3D8FF),
  ),
  AppThemeId.brown: AppPalette(
    id: AppThemeId.brown,
    label: '暖棕',
    background: Color(0xFF150D09),
    surface: Color(0xFF22150F),
    card: Color(0xFF321F16),
    cardAlt: Color(0xFF43291C),
    line: Color(0xFF5C4032),
    primary: Color(0xFF9B522F),
    primarySoft: Color(0xFFFFC49F),
    text: Color(0xFFFFF0E5),
    muted: Color(0xFFD5BFB0),
    subtle: Color(0xFF9B8270),
    blueMuted: Color(0xFFC2DDFF),
  ),
  AppThemeId.purple: AppPalette(
    id: AppThemeId.purple,
    label: '紫黑',
    background: Color(0xFF100D18),
    surface: Color(0xFF191426),
    card: Color(0xFF261C38),
    cardAlt: Color(0xFF31244A),
    line: Color(0xFF4D3D68),
    primary: Color(0xFF7D4CC2),
    primarySoft: Color(0xFFD1B2FF),
    text: Color(0xFFF4EEFF),
    muted: Color(0xFFC7BADB),
    subtle: Color(0xFF8C7BA5),
    blueMuted: Color(0xFFB9DCFF),
  ),
  AppThemeId.black: AppPalette(
    id: AppThemeId.black,
    label: '纯黑',
    background: Color(0xFF090A0D),
    surface: Color(0xFF111318),
    card: Color(0xFF1A1D23),
    cardAlt: Color(0xFF232832),
    line: Color(0xFF383D46),
    primary: Color(0xFF5B6472),
    primarySoft: Color(0xFFD5DDE9),
    text: Color(0xFFF2F5FA),
    muted: Color(0xFFC4CBD6),
    subtle: Color(0xFF858D99),
    blueMuted: Color(0xFFA9D5FF),
  ),
};

const readerPalettes = <ReaderBackgroundId, ReaderPalette>{
  ReaderBackgroundId.theme: ReaderPalette(
    id: ReaderBackgroundId.theme,
    label: '跟随主题',
    background: Color(0xFF17100D),
    text: Color(0xFFEADFD8),
    muted: Color(0xFFB7A8A0),
  ),
  ReaderBackgroundId.warm: ReaderPalette(
    id: ReaderBackgroundId.warm,
    label: '暖棕',
    background: Color(0xFF17100D),
    text: Color(0xFFEADFD8),
    muted: Color(0xFFB7A8A0),
  ),
  ReaderBackgroundId.black: ReaderPalette(
    id: ReaderBackgroundId.black,
    label: '黑色',
    background: Color(0xFF050506),
    text: Color(0xFFDEDEDE),
    muted: Color(0xFF9A9A9A),
  ),
  ReaderBackgroundId.paper: ReaderPalette(
    id: ReaderBackgroundId.paper,
    label: '米白',
    background: Color(0xFFF2E5CF),
    text: Color(0xFF30241B),
    muted: Color(0xFF806B58),
  ),
  ReaderBackgroundId.green: ReaderPalette(
    id: ReaderBackgroundId.green,
    label: '护眼',
    background: Color(0xFFDFEAD9),
    text: Color(0xFF223026),
    muted: Color(0xFF667564),
  ),
};

class ReadingStyle {
  const ReadingStyle({
    this.brightnessMode = AppBrightnessMode.system,
    this.appTheme = AppThemeId.wine,
    this.customThemeColorValue,
    this.readerBackground = ReaderBackgroundId.warm,
    this.fontSize = 20,
    this.lineHeight = 1.85,
    this.paragraphSpacing = 14,
    this.letterSpacing = 0.4,
    this.pageMargin = 28,
    this.verticalMargin = 48,
    this.reverseTapPageTurn = false,
    this.firstLineIndent = true,
    this.fontName,
    this.fontPath,
    this.appFontName,
    this.appFontPath,
  });

  final AppBrightnessMode brightnessMode;
  final AppThemeId appTheme;
  final int? customThemeColorValue;
  final ReaderBackgroundId readerBackground;
  final double fontSize;
  final double lineHeight;
  final double paragraphSpacing;
  final double letterSpacing;
  final double pageMargin;
  final double verticalMargin;
  final bool reverseTapPageTurn;
  final bool firstLineIndent;
  final String? fontName;
  final String? fontPath;
  final String? appFontName;
  final String? appFontPath;

  AppPalette get palette => resolvePalette(Brightness.dark);
  ReaderPalette get readerPalette => readerPalettes[readerBackground]!;

  Color get themeSeedColor {
    final custom = customThemeColorValue;
    if (custom != null) {
      return Color(custom);
    }
    return themeSeeds[appTheme]!.color;
  }

  String get themeLabel => customThemeColorValue == null
      ? themeSeeds[appTheme]!.label
      : '自定义 ${themeHex(themeSeedColor)}';

  AppPalette resolvePalette(Brightness brightness) {
    if (brightness == Brightness.dark && customThemeColorValue == null) {
      return appPalettes[appTheme]!;
    }
    return buildPaletteFromSeed(
      seed: themeSeedColor,
      brightness: brightness,
      label: themeLabel,
      id: appTheme,
    );
  }

  ReadingStyle copyWith({
    AppBrightnessMode? brightnessMode,
    AppThemeId? appTheme,
    int? customThemeColorValue,
    ReaderBackgroundId? readerBackground,
    double? fontSize,
    double? lineHeight,
    double? paragraphSpacing,
    double? letterSpacing,
    double? pageMargin,
    double? verticalMargin,
    bool? reverseTapPageTurn,
    bool? firstLineIndent,
    String? fontName,
    String? fontPath,
    String? appFontName,
    String? appFontPath,
    bool clearFont = false,
    bool clearAppFont = false,
    bool clearCustomThemeColor = false,
  }) {
    return ReadingStyle(
      brightnessMode: brightnessMode ?? this.brightnessMode,
      appTheme: appTheme ?? this.appTheme,
      customThemeColorValue: clearCustomThemeColor
          ? null
          : customThemeColorValue ?? this.customThemeColorValue,
      readerBackground: readerBackground ?? this.readerBackground,
      fontSize: fontSize ?? this.fontSize,
      lineHeight: lineHeight ?? this.lineHeight,
      paragraphSpacing: paragraphSpacing ?? this.paragraphSpacing,
      letterSpacing: letterSpacing ?? this.letterSpacing,
      pageMargin: pageMargin ?? this.pageMargin,
      verticalMargin: verticalMargin ?? this.verticalMargin,
      reverseTapPageTurn: reverseTapPageTurn ?? this.reverseTapPageTurn,
      firstLineIndent: firstLineIndent ?? this.firstLineIndent,
      fontName: clearFont ? null : fontName ?? this.fontName,
      fontPath: clearFont ? null : fontPath ?? this.fontPath,
      appFontName: clearAppFont ? null : appFontName ?? this.appFontName,
      appFontPath: clearAppFont ? null : appFontPath ?? this.appFontPath,
    );
  }

  Map<String, Object?> toJson() => {
    'brightnessMode': brightnessMode.name,
    'appTheme': appTheme.name,
    'customThemeColorValue': customThemeColorValue,
    'readerBackground': readerBackground.name,
    'fontSize': fontSize,
    'lineHeight': lineHeight,
    'paragraphSpacing': paragraphSpacing,
    'letterSpacing': letterSpacing,
    'pageMargin': pageMargin,
    'verticalMargin': verticalMargin,
    'reverseTapPageTurn': reverseTapPageTurn,
    'firstLineIndent': firstLineIndent,
    'fontName': fontName,
    'fontPath': fontPath,
    'appFontName': appFontName,
    'appFontPath': appFontPath,
  };

  factory ReadingStyle.fromJson(Map<String, Object?> json) {
    T enumByName<T extends Enum>(List<T> values, Object? name, T fallback) {
      return values.where((value) => value.name == name).firstOrNull ??
          fallback;
    }

    double number(Object? value, double fallback) {
      if (value is num) {
        return value.toDouble();
      }
      return fallback;
    }

    return ReadingStyle(
      brightnessMode: enumByName(
        AppBrightnessMode.values,
        json['brightnessMode'],
        AppBrightnessMode.system,
      ),
      appTheme: enumByName(
        AppThemeId.values,
        json['appTheme'],
        AppThemeId.wine,
      ),
      customThemeColorValue: json['customThemeColorValue'] as int?,
      readerBackground: enumByName(
        ReaderBackgroundId.values,
        json['readerBackground'],
        ReaderBackgroundId.warm,
      ),
      fontSize: number(json['fontSize'], 20),
      lineHeight: number(json['lineHeight'], 1.85),
      paragraphSpacing: number(json['paragraphSpacing'], 14),
      letterSpacing: number(json['letterSpacing'], 0.4),
      pageMargin: number(json['pageMargin'], 28),
      verticalMargin: number(json['verticalMargin'], 48),
      reverseTapPageTurn: json['reverseTapPageTurn'] as bool? ?? false,
      firstLineIndent: json['firstLineIndent'] as bool? ?? true,
      fontName: json['fontName'] as String?,
      fontPath: json['fontPath'] as String?,
      appFontName: json['appFontName'] as String?,
      appFontPath: json['appFontPath'] as String?,
    );
  }
}

AppPalette buildPaletteFromSeed({
  required Color seed,
  required Brightness brightness,
  required String label,
  required AppThemeId id,
}) {
  if (brightness == Brightness.light) {
    return AppPalette(
      id: id,
      label: label,
      background: _mix(const Color(0xFFFFFBFC), seed, .055),
      surface: _mix(const Color(0xFFFFF4F7), seed, .06),
      card: _mix(const Color(0xFFFFFFFF), seed, .04),
      cardAlt: _mix(const Color(0xFFF6EDF2), seed, .105),
      line: _mix(const Color(0xFFEBDDE5), seed, .18),
      primary: seed,
      primarySoft: _mix(seed, const Color(0xFFFFFFFF), .44),
      text: const Color(0xFF141820),
      muted: const Color(0xFF66707C),
      subtle: const Color(0xFF8F98A5),
      blueMuted: const Color(0xFF2A78B8),
    );
  }
  return AppPalette(
    id: id,
    label: label,
    background: _mix(const Color(0xFF08070A), seed, .08),
    surface: _mix(const Color(0xFF111016), seed, .12),
    card: _mix(const Color(0xFF1B1218), seed, .18),
    cardAlt: _mix(const Color(0xFF251723), seed, .22),
    line: _mix(const Color(0xFF45313B), seed, .36),
    primary: seed,
    primarySoft: _mix(seed, const Color(0xFFFFFFFF), .54),
    text: const Color(0xFFF8F1F5),
    muted: const Color(0xFFC9BBC3),
    subtle: const Color(0xFF948690),
    blueMuted: const Color(0xFFA9D5FF),
  );
}

String themeHex(Color color) {
  String two(double value) {
    final channel = (value * 255).round().clamp(0, 255);
    return channel.toRadixString(16).padLeft(2, '0').toUpperCase();
  }

  return '#${two(color.r)}${two(color.g)}${two(color.b)}';
}

Color _mix(Color a, Color b, double amount) {
  final inverse = 1 - amount;
  return Color.fromARGB(
    255,
    ((a.r * 255 * inverse) + (b.r * 255 * amount)).round().clamp(0, 255),
    ((a.g * 255 * inverse) + (b.g * 255 * amount)).round().clamp(0, 255),
    ((a.b * 255 * inverse) + (b.b * 255 * amount)).round().clamp(0, 255),
  );
}

class ReaderChapter {
  const ReaderChapter({
    required this.title,
    required this.href,
    required this.filePath,
    this.anchor,
    this.tocDepth = 0,
    this.cachedPageCount,
  });

  final String title;
  final String href;
  final String filePath;
  final String? anchor;
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
    'tocDepth': tocDepth,
    'cachedPageCount': cachedPageCount,
  };

  factory ReaderChapter.fromJson(Map<String, Object?> json) {
    return ReaderChapter(
      title: json['title'] as String? ?? '章节',
      href: json['href'] as String? ?? '',
      filePath: json['filePath'] as String? ?? '',
      anchor: json['anchor'] as String?,
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
    bool clearShelf = false,
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
    );
  }
}

class ImportedFont {
  const ImportedFont({required this.name, required this.path});

  final String name;
  final String path;

  Map<String, Object?> toJson() => {'name': name, 'path': path};

  factory ImportedFont.fromJson(Map<String, Object?> json) {
    return ImportedFont(
      name: json['name'] as String? ?? '自定义字体',
      path: json['path'] as String? ?? '',
    );
  }
}
