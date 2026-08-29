import 'dart:ui';

import 'package:flutter/material.dart';

import '../models.dart';
import 'reader_glass_palette.dart';

class FloatingPanelSurface extends StatelessWidget {
  const FloatingPanelSurface({
    super.key,
    required this.palette,
    required this.child,
    this.blurSigma = 34,
    this.transparent = false,
  });

  final AppPalette palette;
  final Widget child;
  final double blurSigma;
  final bool transparent;

  @override
  Widget build(BuildContext context) {
    final glass = ReaderGlassPalette.from(palette);
    final surfaceColor = transparent
        ? Colors.transparent
        : glass.panel.withValues(alpha: glass.dark ? .58 : .50);
    final material = Material(
      color: surfaceColor,
      elevation: transparent ? 0 : (glass.dark ? 2 : 1),
      shadowColor: transparent
          ? Colors.transparent
          : Colors.black.withValues(alpha: glass.dark ? .22 : .10),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
      child: child,
    );
    return ClipRRect(
      borderRadius: BorderRadius.circular(28),
      child: blurSigma <= 0
          ? material
          : BackdropFilter(
              filter: ImageFilter.blur(sigmaX: blurSigma, sigmaY: blurSigma),
              child: material,
            ),
    );
  }
}
