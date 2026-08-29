import 'dart:ui';

import 'package:flutter/material.dart';

import '../app_state.dart';
import '../models.dart';
import '../typography.dart';

class ImportActivityOverlay extends StatelessWidget {
  const ImportActivityOverlay({
    super.key,
    required this.state,
    required this.bottom,
    required this.hidden,
  });

  final AppState state;
  final double bottom;
  final bool hidden;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: state.messageChanges,
      builder: (context, _) {
        final activity = state.importActivity;
        return Positioned(
          left: 0,
          right: 0,
          bottom: bottom,
          child: IgnorePointer(
            child: AnimatedSwitcher(
              duration: const Duration(milliseconds: 180),
              reverseDuration: const Duration(milliseconds: 140),
              switchInCurve: Curves.easeOutCubic,
              switchOutCurve: Curves.easeInCubic,
              transitionBuilder: (child, animation) {
                final curved = CurvedAnimation(
                  parent: animation,
                  curve: Curves.easeOutCubic,
                  reverseCurve: Curves.easeInCubic,
                );
                return FadeTransition(
                  opacity: curved,
                  child: SlideTransition(
                    position: Tween(
                      begin: const Offset(0, .24),
                      end: Offset.zero,
                    ).animate(curved),
                    child: child,
                  ),
                );
              },
              child: activity == null
                  ? const SizedBox.shrink(key: ValueKey('import-empty'))
                  : ImportActivityCapsule(
                      key: const ValueKey('import-capsule'),
                      activity: activity,
                      palette: state.palette,
                    ),
            ),
          ),
        );
      },
    );
  }
}

class ImportActivityCapsule extends StatefulWidget {
  const ImportActivityCapsule({
    super.key,
    required this.activity,
    required this.palette,
  });

  final ImportActivity activity;
  final AppPalette palette;

  @override
  State<ImportActivityCapsule> createState() => _ImportActivityCapsuleState();
}

class _ImportActivityCapsuleState extends State<ImportActivityCapsule>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1200),
    );
    if (widget.activity.active) {
      _controller.repeat();
    } else {
      _controller.value = 1;
    }
  }

  @override
  void didUpdateWidget(covariant ImportActivityCapsule oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.activity.active && !_controller.isAnimating) {
      _controller.repeat();
    } else if (!widget.activity.active) {
      _controller
        ..stop()
        ..value = 1;
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final accent = widget.activity.failed
        ? Colors.redAccent
        : widget.palette.primarySoft;
    return Center(
      child: ClipRRect(
        borderRadius: BorderRadius.circular(999),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 24, sigmaY: 24),
          child: Material(
            color: widget.palette.surface.withValues(
              alpha: widget.palette.isLight ? .60 : .54,
            ),
            elevation: widget.palette.isLight ? 4 : 3,
            shadowColor: Colors.black.withValues(
              alpha: widget.palette.isLight ? .14 : .30,
            ),
            child: SizedBox(
              width: 168,
              height: 42,
              child: AnimatedBuilder(
                animation: _controller,
                builder: (context, _) => CustomPaint(
                  painter: _ImportCapsulePainter(
                    progress: widget.activity.active ? _controller.value : 1,
                    indeterminate: widget.activity.active,
                    trackColor: widget.palette.line.withValues(alpha: .38),
                    progressColor: accent,
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                        widget.activity.failed
                            ? Icons.error_outline_rounded
                            : Icons.upload_file_rounded,
                        color: accent,
                        size: 19,
                      ),
                      const SizedBox(width: 8),
                      Text(
                        widget.activity.failed ? '导入失败' : '正在导入',
                        style: TextStyle(
                          color: widget.palette.text,
                          fontSize: 14,
                          height: 1.1,
                          fontWeight: AppTextWeight.medium,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _ImportCapsulePainter extends CustomPainter {
  const _ImportCapsulePainter({
    required this.progress,
    required this.indeterminate,
    required this.trackColor,
    required this.progressColor,
  });

  final double progress;
  final bool indeterminate;
  final Color trackColor;
  final Color progressColor;

  @override
  void paint(Canvas canvas, Size size) {
    final rect = Offset.zero & size;
    final rrect = RRect.fromRectAndRadius(
      rect.deflate(1.5),
      Radius.circular(size.height / 2),
    );
    final track = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.4
      ..color = trackColor;
    canvas.drawRRect(rrect, track);

    final progressPaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.8
      ..strokeCap = StrokeCap.round
      ..color = progressColor;
    final path = Path()..addRRect(rrect);
    final metric = path.computeMetrics().first;
    if (indeterminate) {
      final start = metric.length * progress;
      final end = start + metric.length * .32;
      final wrappedEnd = end % metric.length;
      if (end <= metric.length) {
        canvas.drawPath(metric.extractPath(start, end), progressPaint);
      } else {
        canvas.drawPath(
          metric.extractPath(start, metric.length),
          progressPaint,
        );
        canvas.drawPath(metric.extractPath(0, wrappedEnd), progressPaint);
      }
    } else {
      canvas.drawPath(
        metric.extractPath(0, metric.length * progress.clamp(0, 1)),
        progressPaint,
      );
    }
  }

  @override
  bool shouldRepaint(covariant _ImportCapsulePainter oldDelegate) {
    return progress != oldDelegate.progress ||
        indeterminate != oldDelegate.indeterminate ||
        trackColor != oldDelegate.trackColor ||
        progressColor != oldDelegate.progressColor;
  }
}
