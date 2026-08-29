import 'package:flutter/material.dart';

import '../models.dart';
import '../typography.dart';
import 'reader_enums.dart';
import 'reader_glass_palette.dart';

class ScrollEdgeTurnHintPositioned extends StatelessWidget {
  const ScrollEdgeTurnHintPositioned({
    super.key,
    required this.direction,
    required this.progress,
    required this.readerPalette,
    required this.palette,
    required this.systemPadding,
  });

  final ScrollEdgeTurnDirection direction;
  final double progress;
  final ReaderPalette readerPalette;
  final AppPalette palette;
  final EdgeInsets systemPadding;

  @override
  Widget build(BuildContext context) {
    final isPrevious = direction == ScrollEdgeTurnDirection.previous;
    final clamped = progress.clamp(0.0, 1.0).toDouble();
    final eased = Curves.easeOutCubic.transform(clamped);
    return Positioned(
      left: 0,
      right: 0,
      top: isPrevious ? systemPadding.top + 18 : null,
      bottom: isPrevious ? null : systemPadding.bottom + 18,
      child: IgnorePointer(
        child: Opacity(
          opacity: eased,
          child: Transform.translate(
            offset: Offset(0, (isPrevious ? -18 : 18) * (1 - eased)),
            child: Center(
              child: ScrollEdgeTurnStretchHint(
                direction: direction,
                progress: progress,
                readerPalette: readerPalette,
                palette: palette,
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class ScrollEdgeTurnStretchHint extends StatelessWidget {
  const ScrollEdgeTurnStretchHint({
    super.key,
    required this.direction,
    required this.progress,
    required this.readerPalette,
    required this.palette,
  });

  final ScrollEdgeTurnDirection direction;
  final double progress;
  final ReaderPalette readerPalette;
  final AppPalette palette;

  @override
  Widget build(BuildContext context) {
    final clamped = progress.clamp(0.0, 1.0).toDouble();
    final ready = clamped >= 1;
    final label = direction == ScrollEdgeTurnDirection.previous
        ? (ready
              ? '\u677e\u624b\u4e0a\u4e00\u7ae0'
              : '\u7ee7\u7eed\u4e0b\u62c9')
        : (ready
              ? '\u677e\u624b\u4e0b\u4e00\u7ae0'
              : '\u7ee7\u7eed\u4e0a\u62c9');
    final glass = ReaderGlassPalette.from(palette);
    return AnimatedOpacity(
      duration: const Duration(milliseconds: 80),
      opacity: clamped <= 0 ? 0 : 1,
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: readerPalette.background.withValues(alpha: .72),
          borderRadius: BorderRadius.circular(999),
        ),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              SizedBox(
                width: 18,
                height: 18,
                child: CircularProgressIndicator(
                  value: clamped,
                  strokeWidth: 2.2,
                  backgroundColor: glass.line.withValues(alpha: .34),
                  valueColor: AlwaysStoppedAnimation(
                    ready ? palette.primarySoft : glass.text,
                  ),
                ),
              ),
              const SizedBox(width: 8),
              Text(
                label,
                style: TextStyle(
                  color: glass.text,
                  fontSize: 12,
                  fontWeight: AppTextWeight.medium,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
