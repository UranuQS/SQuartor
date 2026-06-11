import 'package:flutter/material.dart';

class LazyPageStack extends StatefulWidget {
  const LazyPageStack({super.key, required this.index, required this.pages});

  final int index;
  final List<Widget?> pages;

  @override
  State<LazyPageStack> createState() => _LazyPageStackState();
}

class _LazyPageStackState extends State<LazyPageStack>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  int? _fromIndex;
  var _direction = 1;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 240),
      value: 1,
    );
    _controller.addStatusListener(_handleAnimationStatus);
  }

  void _handleAnimationStatus(AnimationStatus status) {
    if (status == AnimationStatus.completed && _fromIndex != null && mounted) {
      setState(() => _fromIndex = null);
    }
  }

  @override
  void didUpdateWidget(LazyPageStack oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.index != widget.index) {
      _fromIndex = oldWidget.index;
      _direction = widget.index > oldWidget.index ? 1 : -1;
      _controller.forward(from: 0);
    }
  }

  @override
  void dispose() {
    _controller.removeStatusListener(_handleAnimationStatus);
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        for (var i = 0; i < widget.pages.length; i++)
          if (widget.pages[i] != null) _buildPage(i, widget.pages[i]!),
      ],
    );
  }

  Widget _buildPage(int pageIndex, Widget page) {
    final animating = _controller.value < 1;
    final isIncoming = pageIndex == widget.index;
    final isOutgoing = animating && pageIndex == _fromIndex;
    final visible = isIncoming || isOutgoing;
    return Positioned.fill(
      child: Offstage(
        offstage: !visible,
        child: IgnorePointer(
          ignoring: !isIncoming,
          child: TickerMode(
            enabled: isIncoming,
            child: RepaintBoundary(
              child: AnimatedBuilder(
                animation: _controller,
                child: page,
                builder: (context, child) {
                  final t = Curves.easeOutCubic.transform(_controller.value);
                  var opacity = 1.0;
                  var offset = 0.0;
                  if (isOutgoing) {
                    opacity = 1 - t;
                    offset = -0.025 * _direction * t;
                  } else if (isIncoming && animating) {
                    opacity = t;
                    offset = 0.045 * _direction * (1 - t);
                  }
                  return FractionalTranslation(
                    translation: Offset(offset, 0),
                    child: Opacity(opacity: opacity, child: child),
                  );
                },
              ),
            ),
          ),
        ),
      ),
    );
  }
}
