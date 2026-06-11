import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../typography.dart';
import 'reader_glass_palette.dart';

// ---------------------------------------------------------------------------
// ReaderProgressOverview
// ---------------------------------------------------------------------------

class ReaderProgressOverview extends StatelessWidget {
  const ReaderProgressOverview({
    super.key,
    required this.progress,
    required this.glass,
  });

  final double progress;
  final ReaderGlassPalette glass;

  @override
  Widget build(BuildContext context) {
    final p = progress.clamp(0.0, 1.0).toDouble();
    final percentText = (p * 100).round().toString();
    return RepaintBoundary(
      child: LayoutBuilder(
        builder: (context, constraints) {
          final width = math.max(1.0, constraints.maxWidth);
          final availableInnerWidth = math.max(1.0, width - 12);
          final innerWidth = math.min(availableInnerWidth, 150.0);
          final compact = innerWidth < 124;
          final textLeft = compact ? 12.0 : 18.0;
          final numberFontSize = compact ? 20.0 : 22.0;
          final numberSpacing = compact ? -.9 : -1.1;
          final percentTop = compact ? 5.2 : 5.6;
          final numberStyle = TextStyle(
            color: glass.text.withValues(alpha: glass.dark ? .94 : .96),
            fontSize: numberFontSize,
            height: .98,
            fontWeight: AppTextWeight.semibold,
            letterSpacing: numberSpacing,
          );
          final percentStyle = TextStyle(
            color: glass.text.withValues(alpha: glass.dark ? .66 : .70),
            fontSize: 9.5,
            height: 1,
            fontWeight: AppTextWeight.medium,
            letterSpacing: -.7,
          );
          double measureText(String text, TextStyle style) {
            final painter = TextPainter(
              text: TextSpan(text: text, style: style),
              maxLines: 1,
              textDirection: TextDirection.ltr,
            )..layout();
            return painter.width;
          }

          final percentRight =
              textLeft +
              measureText(percentText, numberStyle) +
              2 +
              measureText('%', percentStyle);
          final trackGap = compact ? 5.0 : 10.0;
          final trackLeft = percentRight + trackGap;
          final maxTrackWidth = math.max(0.0, innerWidth - trackLeft - 10);
          final trackWidth = math
              .min(compact ? 34.0 : 42.0, maxTrackWidth)
              .clamp(0.0, 46.0)
              .toDouble();
          final showTrack = trackWidth >= 16;
          return Center(
            child: DecoratedBox(
              decoration: BoxDecoration(
                color: glass.dark
                    ? Colors.black.withValues(alpha: .13)
                    : Colors.white.withValues(alpha: .16),
                borderRadius: BorderRadius.circular(999),
              ),
              child: SizedBox(
                width: innerWidth,
                height: 44,
                child: Stack(
                  clipBehavior: Clip.none,
                  children: [
                    Align(
                      alignment: Alignment.centerLeft,
                      child: Transform.translate(
                        offset: Offset(textLeft, -.5),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(percentText, maxLines: 1, style: numberStyle),
                            Padding(
                              padding: EdgeInsets.only(
                                top: percentTop,
                                left: 2,
                              ),
                              child: Text(
                                '%',
                                maxLines: 1,
                                style: percentStyle,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                    if (showTrack)
                      Positioned(
                        left: trackLeft,
                        top: 19.5,
                        width: trackWidth,
                        height: 5,
                        child: DecoratedBox(
                          decoration: BoxDecoration(
                            color: glass.line.withValues(
                              alpha: glass.dark ? .48 : .44,
                            ),
                            borderRadius: BorderRadius.circular(999),
                          ),
                          child: TweenAnimationBuilder<double>(
                            tween: Tween<double>(
                              begin: 0,
                              end: p.clamp(.06, 1),
                            ),
                            duration: const Duration(milliseconds: 260),
                            curve: Curves.easeOutCubic,
                            builder: (context, value, _) {
                              return FractionallySizedBox(
                                widthFactor: value,
                                alignment: Alignment.centerLeft,
                                child: DecoratedBox(
                                  decoration: BoxDecoration(
                                    color: glass.text.withValues(
                                      alpha: glass.dark ? .54 : .58,
                                    ),
                                    borderRadius: BorderRadius.circular(999),
                                  ),
                                ),
                              );
                            },
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// ReaderProgressRulerPainter
// ---------------------------------------------------------------------------

class ReaderProgressRulerPainter extends CustomPainter {
  const ReaderProgressRulerPainter({
    required this.progress,
    required this.majorTickColor,
    required this.labelColor,
    required this.chapterCount,
    required this.tickSpacing,
  });

  final double progress;
  final Color majorTickColor;
  final Color labelColor;
  final int chapterCount;
  final double tickSpacing;

  @override
  void paint(Canvas canvas, Size size) {
    if (size.isEmpty) {
      return;
    }
    final count = chapterCount;
    final p = progress.clamp(0.0, 1.0).toDouble();
    final maxChapter = math.max(0, count - 1);
    final value = count <= 1
        ? 0.0
        : p >= .999
        ? maxChapter.toDouble()
        : (p * count).clamp(0.0, maxChapter.toDouble()).toDouble();
    final centerX = size.width / 2;
    final centerY = size.height / 2;
    final pillWidth = math.max(1.0, size.width - 14);
    final pillHeight = math.max(1.0, size.height - 12);
    final firstChapter = (value - centerX / tickSpacing).floor() - 1;
    final lastChapter = (value + centerX / tickSpacing).ceil() + 1;
    final visibleChapters = <({int chapter, double x, double distance})>[];
    for (var chapter = firstChapter; chapter <= lastChapter; chapter++) {
      if (chapter < 0 || chapter > maxChapter) {
        continue;
      }
      final x = centerX + (chapter - value) * tickSpacing;
      final distance = ((x - centerX).abs() / tickSpacing).toDouble();
      if (distance > 1.65) {
        continue;
      }
      visibleChapters.add((chapter: chapter, x: x, distance: distance));
    }
    visibleChapters.sort((a, b) => b.distance.compareTo(a.distance));
    for (final item in visibleChapters) {
      final focus = (1 - item.distance).clamp(0.0, 1.0).toDouble();
      final edgeFade = (1 - ((item.x - centerX).abs() / (size.width / 2)))
          .clamp(.08, 1.0)
          .toDouble();
      final opacity = (.28 + .72 * focus) * edgeFade;
      final rect = Rect.fromCenter(
        center: Offset(item.x, centerY),
        width: pillWidth,
        height: pillHeight,
      );
      final paint = Paint()
        ..color = majorTickColor.withValues(alpha: .12 * opacity)
        ..isAntiAlias = true;
      canvas.drawRRect(
        RRect.fromRectAndRadius(rect, const Radius.circular(999)),
        paint,
      );
      _paintChapterSelector(
        canvas,
        text: '\u7b2c${item.chapter + 1}\u7ae0',
        centerX: item.x,
        centerY: centerY,
        opacity: opacity,
      );
    }
  }

  void _paintChapterSelector(
    Canvas canvas, {
    required String text,
    required double centerX,
    required double centerY,
    required double opacity,
  }) {
    final painter = TextPainter(
      text: TextSpan(
        children: [
          TextSpan(
            text: '\u2039',
            style: TextStyle(
              color: majorTickColor.withValues(alpha: .92 * opacity),
              fontSize: 26,
              height: 1,
              fontWeight: AppTextWeight.semibold,
            ),
          ),
          TextSpan(
            text: '  $text  ',
            style: TextStyle(
              color: labelColor.withValues(alpha: opacity),
              fontSize: 17,
              height: 1,
              fontWeight: AppTextWeight.semibold,
              letterSpacing: -.45,
            ),
          ),
          TextSpan(
            text: '\u203a',
            style: TextStyle(
              color: majorTickColor.withValues(alpha: .92 * opacity),
              fontSize: 26,
              height: 1,
              fontWeight: AppTextWeight.semibold,
            ),
          ),
        ],
      ),
      textDirection: TextDirection.ltr,
      textAlign: TextAlign.center,
      maxLines: 1,
    )..layout();
    painter.paint(
      canvas,
      Offset(centerX - painter.width / 2, centerY - painter.height / 2),
    );
  }

  @override
  bool shouldRepaint(covariant ReaderProgressRulerPainter oldDelegate) {
    return oldDelegate.progress != progress ||
        oldDelegate.majorTickColor != majorTickColor ||
        oldDelegate.labelColor != labelColor ||
        oldDelegate.chapterCount != chapterCount ||
        oldDelegate.tickSpacing != tickSpacing;
  }
}
