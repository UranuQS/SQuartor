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
    return RepaintBoundary(
      child: CustomPaint(
        painter: _ReaderProgressOverviewPainter(progress: p, glass: glass),
        child: const SizedBox.expand(),
      ),
    );
  }
}

class _ReaderProgressOverviewPainter extends CustomPainter {
  const _ReaderProgressOverviewPainter({
    required this.progress,
    required this.glass,
  });

  final double progress;
  final ReaderGlassPalette glass;

  @override
  void paint(Canvas canvas, Size size) {
    if (size.isEmpty) {
      return;
    }
    final p = progress.clamp(0.0, 1.0).toDouble();
    final innerWidth = math.min(math.max(1.0, size.width - 12), 140.0);
    const innerHeight = 44.0;
    final left = (size.width - innerWidth) / 2;
    final top = (size.height - innerHeight) / 2;
    final rect = Rect.fromLTWH(left, top, innerWidth, innerHeight);
    final backgroundPaint = Paint()
      ..color = glass.dark
          ? Colors.black.withValues(alpha: .13)
          : Colors.white.withValues(alpha: .16)
      ..isAntiAlias = true;
    canvas.drawRRect(
      RRect.fromRectAndRadius(rect, const Radius.circular(999)),
      backgroundPaint,
    );

    final widthT = ((innerWidth - 104) / 28).clamp(0.0, 1.0).toDouble();
    final textLeft = _lerp(12, 18, widthT);
    final numberFontSize = _lerp(20, 22, widthT);
    final percentTop = _lerp(5.2, 5.6, widthT);
    final trackGap = _lerp(5, 10, widthT);
    final targetTrackWidth = _lerp(30, 42, widthT);
    final numberStyle = TextStyle(
      color: glass.text.withValues(alpha: glass.dark ? .94 : .96),
      fontSize: numberFontSize,
      height: .98,
      fontWeight: AppTextWeight.semibold,
      letterSpacing: 0,
    );
    final percentStyle = TextStyle(
      color: glass.text.withValues(alpha: glass.dark ? .66 : .70),
      fontSize: 9.5,
      height: 1,
      fontWeight: AppTextWeight.medium,
      letterSpacing: 0,
    );
    final percentText = (p * 100).round().toString();
    final numberPainter = _textPainter(percentText, numberStyle)..layout();
    final signPainter = _textPainter('%', percentStyle)..layout();
    final textX = left + textLeft;
    final textY = top + (innerHeight - numberPainter.height) / 2 - .5;
    numberPainter.paint(canvas, Offset(textX, textY));
    signPainter.paint(
      canvas,
      Offset(textX + numberPainter.width + 2, textY + percentTop),
    );

    final trackLeft =
        textLeft + numberPainter.width + 2 + signPainter.width + trackGap;
    final maxTrackWidth = math.max(0.0, innerWidth - trackLeft - 10);
    final trackWidth = math
        .min(targetTrackWidth, maxTrackWidth)
        .clamp(0.0, 46.0)
        .toDouble();
    if (trackWidth <= 0.5) {
      return;
    }
    final trackOpacity = ((trackWidth - 8) / 8).clamp(0.0, 1.0).toDouble();
    final trackRect = Rect.fromLTWH(
      left + trackLeft,
      top + 19.5,
      trackWidth,
      5,
    );
    final trackRadius = RRect.fromRectAndRadius(
      trackRect,
      const Radius.circular(999),
    );
    final trackPaint = Paint()
      ..color = glass.line.withValues(
        alpha: (glass.dark ? .48 : .44) * trackOpacity,
      )
      ..isAntiAlias = true;
    final fillPaint = Paint()
      ..color = glass.text.withValues(
        alpha: (glass.dark ? .54 : .58) * trackOpacity,
      )
      ..isAntiAlias = true;
    canvas.drawRRect(trackRadius, trackPaint);
    final fillRect = Rect.fromLTWH(
      trackRect.left,
      trackRect.top,
      trackRect.width * p.clamp(.06, 1),
      trackRect.height,
    );
    canvas.drawRRect(
      RRect.fromRectAndRadius(fillRect, const Radius.circular(999)),
      fillPaint,
    );
  }

  TextPainter _textPainter(String text, TextStyle style) {
    return TextPainter(
      text: TextSpan(text: text, style: style),
      maxLines: 1,
      textDirection: TextDirection.ltr,
    );
  }

  double _lerp(double begin, double end, double t) {
    return begin + (end - begin) * t;
  }

  @override
  bool shouldRepaint(covariant _ReaderProgressOverviewPainter oldDelegate) {
    return oldDelegate.progress != progress ||
        oldDelegate.glass.text != glass.text ||
        oldDelegate.glass.line != glass.line ||
        oldDelegate.glass.dark != glass.dark;
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
