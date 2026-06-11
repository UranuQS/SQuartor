import 'package:flutter/material.dart';

import '../models.dart';
import '../typography.dart';
import 'reader_enums.dart';

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
    final targetInset = isPrevious
        ? systemPadding.top + 58
        : systemPadding.bottom + 58;
    final slideDistance = 86.0 * (1 - eased);
    return Positioned(
      left: 0,
      right: 0,
      top: isPrevious ? targetInset : null,
      bottom: isPrevious ? null : targetInset,
      child: IgnorePointer(
        child: Opacity(
          opacity: eased,
          child: Transform.translate(
            offset: Offset(0, isPrevious ? -slideDistance : slideDistance),
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
        ? (ready ? '\u4e0a\u4e00\u7ae0' : '\u7ee7\u7eed\u62c9\u52a8')
        : (ready ? '\u4e0b\u4e00\u7ae0' : '\u7ee7\u7eed\u62c9\u52a8');
    return AnimatedOpacity(
      duration: const Duration(milliseconds: 80),
      opacity: clamped <= 0 ? 0 : 1,
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: readerPalette.background.withValues(alpha: .9),
          borderRadius: BorderRadius.circular(28),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: .12),
              blurRadius: 18,
              offset: const Offset(0, 7),
            ),
          ],
        ),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(14, 10, 16, 10),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              SizedBox(
                width: 30,
                height: 30,
                child: CircularProgressIndicator(
                  value: clamped,
                  strokeWidth: 3.4,
                  strokeCap: StrokeCap.round,
                  backgroundColor: readerPalette.muted.withValues(alpha: .18),
                  valueColor: AlwaysStoppedAnimation<Color>(palette.primary),
                ),
              ),
              const SizedBox(width: 10),
              Text(
                label,
                style: TextStyle(
                  color: readerPalette.text,
                  fontSize: 13,
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
