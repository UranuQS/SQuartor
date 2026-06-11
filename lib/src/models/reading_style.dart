import 'dart:ui';

import 'app_palette.dart';

enum ReadingFlowMode { paged, scroll }

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
    this.readingFlow = ReadingFlowMode.paged,
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
  final ReadingFlowMode readingFlow;
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
    ReadingFlowMode? readingFlow,
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
      readingFlow: readingFlow ?? this.readingFlow,
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
    'readingFlow': readingFlow.name,
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

    double ranged(Object? value, double fallback, double min, double max) {
      return number(value, fallback).clamp(min, max).toDouble();
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
      fontSize: ranged(json['fontSize'], 20, 14, 32),
      lineHeight: ranged(json['lineHeight'], 1.85, 1.2, 2.6),
      paragraphSpacing: ranged(json['paragraphSpacing'], 14, 0, 30),
      letterSpacing: ranged(json['letterSpacing'], 0.4, 0, 2),
      pageMargin: ranged(json['pageMargin'], 28, 0, 52),
      verticalMargin: ranged(json['verticalMargin'], 48, 4, 96),
      readingFlow: enumByName(
        ReadingFlowMode.values,
        json['readingFlow'],
        ReadingFlowMode.paged,
      ),
      reverseTapPageTurn: json['reverseTapPageTurn'] as bool? ?? false,
      firstLineIndent: json['firstLineIndent'] as bool? ?? true,
      fontName: json['fontName'] as String?,
      fontPath: json['fontPath'] as String?,
      appFontName: json['appFontName'] as String?,
      appFontPath: json['appFontPath'] as String?,
    );
  }
}
