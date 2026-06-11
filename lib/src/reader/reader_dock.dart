import 'dart:math' as math;
import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../typography.dart';
import 'reader_glass_palette.dart';
import 'reader_progress.dart';

// ---------------------------------------------------------------------------
// ReaderDockActionPill
// ---------------------------------------------------------------------------

class ReaderDockActionPill extends StatelessWidget {
  const ReaderDockActionPill({
    super.key,
    required this.icon,
    required this.label,
    required this.glass,
    required this.selected,
    required this.compact,
    required this.labelProgress,
    required this.transparent,
    this.showContent = true,
    required this.onPressed,
  });

  final IconData icon;
  final String label;
  final ReaderGlassPalette glass;
  final bool selected;
  final bool compact;
  final double labelProgress;
  final bool transparent;
  final bool showContent;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    final backgroundColor = transparent ? Colors.transparent : glass.pill;
    return Material(
      color: backgroundColor,
      borderRadius: BorderRadius.circular(28),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        overlayColor: WidgetStatePropertyAll(glass.text.withValues(alpha: .10)),
        onTap: () {
          HapticFeedback.selectionClick();
          onPressed();
        },
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
          child: showContent
              ? ReaderDockActionContent(
                  icon: icon,
                  label: label,
                  glass: glass,
                  labelProgress: labelProgress,
                  iconLeft: 16,
                )
              : const SizedBox.expand(),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// ReaderDockActionContent
// ---------------------------------------------------------------------------

class ReaderDockActionContent extends StatelessWidget {
  const ReaderDockActionContent({
    super.key,
    required this.icon,
    required this.label,
    required this.glass,
    required this.labelProgress,
    required this.iconLeft,
    this.labelWidth = 34,
  });

  final IconData icon;
  final String label;
  final ReaderGlassPalette glass;
  final double labelProgress;
  final double iconLeft;
  final double labelWidth;

  @override
  Widget build(BuildContext context) {
    return RepaintBoundary(
      child: ClipRect(
        child: CustomPaint(
          painter: ReaderDockActionContentPainter(
            icon: icon,
            label: label,
            color: glass.text,
            labelProgress: labelProgress,
            iconLeft: iconLeft,
            labelWidth: labelWidth,
          ),
          size: Size.infinite,
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// ReaderDockActionContentPainter
// ---------------------------------------------------------------------------

class ReaderDockActionContentPainter extends CustomPainter {
  const ReaderDockActionContentPainter({
    required this.icon,
    required this.label,
    required this.color,
    required this.labelProgress,
    required this.iconLeft,
    required this.labelWidth,
  });

  static const _iconSize = 26.0;
  static const _labelGap = 10.0;

  final IconData icon;
  final String label;
  final Color color;
  final double labelProgress;
  final double iconLeft;
  final double labelWidth;

  @override
  void paint(Canvas canvas, Size size) {
    if (size.isEmpty) {
      return;
    }
    final textT = labelProgress.clamp(0.0, 1.0).toDouble();
    final iconPainter = TextPainter(
      text: TextSpan(
        text: String.fromCharCode(icon.codePoint),
        style: TextStyle(
          color: color,
          fontSize: _iconSize,
          fontFamily: icon.fontFamily,
          package: icon.fontPackage,
          height: 1,
        ),
      ),
      textDirection: TextDirection.ltr,
      maxLines: 1,
    )..layout();
    iconPainter.paint(
      canvas,
      Offset(
        iconLeft + (_iconSize - iconPainter.width) / 2,
        (size.height - iconPainter.height) / 2,
      ),
    );
    if (textT <= 0.001) {
      return;
    }
    final labelPainter = TextPainter(
      text: TextSpan(
        text: label,
        style: TextStyle(
          color: color.withValues(alpha: textT),
          fontSize: 16,
          height: 1,
          fontWeight: AppTextWeight.semibold,
        ),
      ),
      textDirection: TextDirection.ltr,
      textAlign: TextAlign.left,
      maxLines: 1,
      ellipsis: '',
    )..layout(maxWidth: labelWidth + 2);
    labelPainter.paint(
      canvas,
      Offset(
        iconLeft + _iconSize + _labelGap,
        (size.height - labelPainter.height) / 2,
      ),
    );
  }

  @override
  bool shouldRepaint(covariant ReaderDockActionContentPainter oldDelegate) {
    return oldDelegate.icon != icon ||
        oldDelegate.label != label ||
        oldDelegate.color != color ||
        oldDelegate.labelProgress != labelProgress ||
        oldDelegate.iconLeft != iconLeft ||
        oldDelegate.labelWidth != labelWidth;
  }
}

// ---------------------------------------------------------------------------
// ReaderProgressBatteryPill
// ---------------------------------------------------------------------------

class ReaderProgressBatteryPill extends StatefulWidget {
  const ReaderProgressBatteryPill({
    super.key,
    required this.progress,
    required this.currentChapter,
    required this.chapterCount,
    required this.glass,
    required this.chapterOpacity,
    required this.rulerVisible,
    required this.onRulerVisibilityChanged,
    required this.onSeek,
    required this.onScrubStart,
    required this.onPressed,
  });

  final double progress;
  final int currentChapter;
  final int chapterCount;
  final ReaderGlassPalette glass;
  final double chapterOpacity;
  final bool rulerVisible;
  final ValueChanged<bool> onRulerVisibilityChanged;
  final ValueChanged<double> onSeek;
  final VoidCallback onScrubStart;
  final VoidCallback onPressed;

  @override
  State<ReaderProgressBatteryPill> createState() =>
      _ReaderProgressBatteryPillState();
}

class _ReaderProgressBatteryPillState extends State<ReaderProgressBatteryPill>
    with SingleTickerProviderStateMixin {
  late double _displayProgress = widget.progress.clamp(0.0, 1.0).toDouble();
  late final AnimationController _settleController;
  Animation<double>? _settleAnimation;
  var _dragging = false;
  double? _interactiveProgress;
  double? _pendingSeekProgress;
  var _pendingSeek = false;
  int? _lastHapticTick;
  DateTime _lastHapticAt = DateTime.fromMillisecondsSinceEpoch(0);
  Timer? _scrubberHideTimer;

  @override
  void initState() {
    super.initState();
    _settleController = AnimationController(vsync: this)
      ..addListener(() {
        final animation = _settleAnimation;
        if (animation == null) {
          return;
        }
        setState(() => _displayProgress = animation.value);
      })
      ..addStatusListener((status) {
        if (status != AnimationStatus.completed) {
          return;
        }
        final target = _pendingSeekProgress;
        final shouldSeek = _pendingSeek;
        _settleAnimation = null;
        _pendingSeekProgress = null;
        _pendingSeek = false;
        _dragging = false;
        _interactiveProgress = null;
        if (shouldSeek && target != null) {
          widget.onSeek(target);
        }
        _scheduleScrubberHide();
      });
  }

  @override
  void didUpdateWidget(covariant ReaderProgressBatteryPill oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (_dragging || _settleController.isAnimating) {
      return;
    }
    final target = widget.progress.clamp(0.0, 1.0).toDouble();
    if ((target - _displayProgress).abs() > .0001) {
      setState(() => _displayProgress = target);
    }
  }

  @override
  void dispose() {
    _scrubberHideTimer?.cancel();
    _settleController.dispose();
    super.dispose();
  }

  double _progressForChapter(int chapter) {
    final count = widget.chapterCount;
    if (count <= 1) {
      return 1.0;
    }
    final safeChapter = chapter.clamp(0, count - 1);
    if (safeChapter >= count - 1) {
      return 1.0;
    }
    return (safeChapter / count).clamp(0.0, 1.0).toDouble();
  }

  double _chapterPositionForProgress(double progress) {
    final count = widget.chapterCount;
    if (count <= 1) {
      return 0;
    }
    final p = progress.clamp(0.0, 1.0).toDouble();
    if (p >= .999) {
      return (count - 1).toDouble();
    }
    return (p * count).clamp(0.0, (count - 1).toDouble()).toDouble();
  }

  double _progressForChapterPosition(double position) {
    final count = widget.chapterCount;
    if (count <= 1) {
      return 1.0;
    }
    final maxChapter = count - 1;
    final safePosition = position.clamp(0.0, maxChapter.toDouble()).toDouble();
    if (safePosition >= maxChapter - .001) {
      return 1.0;
    }
    return (safePosition / count).clamp(0.0, 1.0).toDouble();
  }

  double _rulerTickSpacing() {
    return 112.0;
  }

  void _setInteractiveProgress(double value) {
    final next = value.clamp(0.0, 1.0).toDouble();
    _maybeTickHaptic(next);
    setState(() {
      _interactiveProgress = next;
      _displayProgress = next;
    });
  }

  void _maybeTickHaptic(double progress) {
    final tick = _chapterPositionForProgress(progress).round();
    final now = DateTime.now();
    if (tick == _lastHapticTick ||
        now.difference(_lastHapticAt).inMilliseconds < 36) {
      return;
    }
    _lastHapticTick = tick;
    _lastHapticAt = now;
    HapticFeedback.selectionClick();
  }

  void _showScrubber() {
    _scrubberHideTimer?.cancel();
    if (!widget.rulerVisible) {
      widget.onRulerVisibilityChanged(true);
    }
  }

  void _scheduleScrubberHide() {
    _scrubberHideTimer?.cancel();
    _scrubberHideTimer = Timer(const Duration(milliseconds: 900), () {
      if (!mounted) {
        return;
      }
      widget.onRulerVisibilityChanged(false);
    });
  }

  void _startDrag() {
    widget.onScrubStart();
    _showScrubber();
    _settleController.stop();
    _settleAnimation = null;
    _pendingSeekProgress = null;
    _pendingSeek = false;
    _dragging = true;
    _interactiveProgress = _displayProgress;
    _lastHapticTick = _chapterPositionForProgress(_displayProgress).round();
    HapticFeedback.selectionClick();
  }

  void _updateDrag(double delta, double width) {
    final current = _interactiveProgress ?? _displayProgress;
    final currentPosition = _chapterPositionForProgress(current);
    final nextPosition = (currentPosition - delta / _rulerTickSpacing()).clamp(
      0.0,
      math.max(0, widget.chapterCount - 1).toDouble(),
    );
    _setInteractiveProgress(_progressForChapterPosition(nextPosition));
  }

  void _endDrag(DragEndDetails details, double width) {
    final current = _interactiveProgress ?? _displayProgress;
    final currentPosition = _chapterPositionForProgress(current);
    final velocityChapters =
        details.velocity.pixelsPerSecond.dx / _rulerTickSpacing();
    final projectedPosition = (currentPosition - velocityChapters * .045).clamp(
      0.0,
      math.max(0, widget.chapterCount - 1).toDouble(),
    );
    final targetChapter = projectedPosition
        .round()
        .clamp(0, math.max(0, widget.chapterCount - 1))
        .toInt();
    final currentChapter = widget.currentChapter.clamp(
      0,
      math.max(0, widget.chapterCount - 1),
    );
    final shouldSeek = targetChapter != currentChapter;
    final targetProgress = shouldSeek
        ? _progressForChapter(targetChapter)
        : widget.progress.clamp(0.0, 1.0).toDouble();
    _animateToProgress(
      targetProgress,
      shouldSeek: shouldSeek,
      from: current,
      distance: (targetProgress - current).abs(),
    );
  }

  void _cancelDrag() {
    final current = _interactiveProgress ?? _displayProgress;
    final target = widget.progress.clamp(0.0, 1.0).toDouble();
    _animateToProgress(
      target,
      shouldSeek: false,
      from: current,
      distance: (target - current).abs(),
    );
  }

  void _animateToProgress(
    double target, {
    required bool shouldSeek,
    required double from,
    required double distance,
  }) {
    _pendingSeekProgress = target;
    _pendingSeek = shouldSeek;
    _settleAnimation = Tween<double>(begin: from, end: target).animate(
      CurvedAnimation(parent: _settleController, curve: Curves.easeOutCubic),
    );
    _settleController
      ..duration = Duration(
        milliseconds: (180 + distance * 420).clamp(180, 420).round(),
      )
      ..forward(from: 0);
  }

  @override
  Widget build(BuildContext context) {
    return TweenAnimationBuilder<double>(
      tween: Tween<double>(end: _displayProgress),
      duration: _dragging || _settleController.isAnimating
          ? Duration.zero
          : const Duration(milliseconds: 360),
      curve: Curves.easeOutQuart,
      builder: (context, animatedProgress, _) {
        final shownProgress = animatedProgress.clamp(0.0, 1.0).toDouble();
        return Material(
          color: widget.glass.pill,
          borderRadius: BorderRadius.circular(28),
          clipBehavior: Clip.antiAlias,
          child: LayoutBuilder(
            builder: (context, constraints) {
              final width = math.max(1.0, constraints.maxWidth);
              final chromeActive = widget.chapterOpacity > .5;
              final scrubberVisible =
                  widget.rulerVisible ||
                  _dragging ||
                  _settleController.isAnimating;
              final dragEnabled = chromeActive;
              return GestureDetector(
                behavior: HitTestBehavior.opaque,
                onTap: () {
                  if (!chromeActive) {
                    HapticFeedback.selectionClick();
                    widget.onPressed();
                  }
                },
                onHorizontalDragStart: dragEnabled ? (_) => _startDrag() : null,
                onHorizontalDragUpdate: dragEnabled
                    ? (details) => _updateDrag(details.delta.dx, width)
                    : null,
                onHorizontalDragEnd: dragEnabled
                    ? (details) => _endDrag(details, width)
                    : null,
                onHorizontalDragCancel: dragEnabled ? _cancelDrag : null,
                child: Stack(
                  fit: StackFit.expand,
                  children: [
                    AnimatedOpacity(
                      opacity: scrubberVisible ? 0 : 1,
                      duration: const Duration(milliseconds: 160),
                      curve: Curves.easeOutCubic,
                      child: ReaderProgressOverview(
                        progress: shownProgress,
                        glass: widget.glass,
                      ),
                    ),
                    AnimatedOpacity(
                      opacity: scrubberVisible ? 1 : 0,
                      duration: const Duration(milliseconds: 180),
                      curve: Curves.easeOutCubic,
                      child: CustomPaint(
                        painter: ReaderProgressRulerPainter(
                          progress: shownProgress,
                          majorTickColor: widget.glass.text.withValues(
                            alpha: widget.glass.dark ? .70 : .76,
                          ),
                          labelColor: widget.glass.text,
                          chapterCount: widget.chapterCount,
                          tickSpacing: _rulerTickSpacing(),
                        ),
                        size: Size.infinite,
                      ),
                    ),
                  ],
                ),
              );
            },
          ),
        );
      },
    );
  }
}
