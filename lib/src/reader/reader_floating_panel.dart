import 'dart:ui';

import 'package:flutter/material.dart';

import '../models.dart';
import 'reader_glass_palette.dart';

class FloatingPanelSurface extends StatelessWidget {
  const FloatingPanelSurface({
    super.key,
    required this.palette,
    required this.child,
  });

  final AppPalette palette;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final glass = ReaderGlassPalette.from(palette);
    return ClipRRect(
      borderRadius: BorderRadius.circular(28),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 22, sigmaY: 22),
        child: Material(
          color: glass.panel,
          elevation: glass.dark ? 2 : 1,
          shadowColor: Colors.black.withValues(alpha: glass.dark ? .28 : .12),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(28),
          ),
          child: child,
        ),
      ),
    );
  }
}
