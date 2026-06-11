import 'package:flutter/material.dart';

import '../models.dart';

class ReaderGlassPalette {
  const ReaderGlassPalette({
    required this.dark,
    required this.panel,
    required this.pill,
    required this.text,
    required this.muted,
    required this.subtle,
    required this.line,
    required this.scrim,
  });

  final bool dark;
  final Color panel;
  final Color pill;
  final Color text;
  final Color muted;
  final Color subtle;
  final Color line;
  final Color scrim;

  factory ReaderGlassPalette.from(AppPalette palette) {
    final dark =
        ThemeData.estimateBrightnessForColor(palette.background) ==
        Brightness.dark;
    if (dark) {
      return ReaderGlassPalette(
        dark: true,
        panel: palette.surface.withValues(alpha: .88),
        pill: palette.cardAlt.withValues(alpha: .68),
        text: palette.text,
        muted: palette.muted,
        subtle: palette.subtle,
        line: palette.line,
        scrim: Colors.black.withValues(alpha: .54),
      );
    }
    return ReaderGlassPalette(
      dark: false,
      panel: palette.surface.withValues(alpha: .86),
      pill: palette.cardAlt.withValues(alpha: .66),
      text: palette.text,
      muted: palette.muted,
      subtle: palette.subtle,
      line: palette.line,
      scrim: Colors.black.withValues(alpha: .30),
    );
  }
}
