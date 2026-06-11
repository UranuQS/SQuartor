import 'package:flutter/material.dart';

import '../models.dart';

class ReaderPanelScrim extends StatelessWidget {
  const ReaderPanelScrim({
    super.key,
    required this.visible,
    required this.palette,
    required this.onDismiss,
  });

  final bool visible;
  final AppPalette palette;
  final VoidCallback onDismiss;

  @override
  Widget build(BuildContext context) {
    return Positioned.fill(
      child: IgnorePointer(
        ignoring: !visible,
        child: AnimatedOpacity(
          duration: const Duration(milliseconds: 240),
          curve: Curves.easeOut,
          opacity: visible ? 1 : 0,
          child: GestureDetector(
            behavior: HitTestBehavior.opaque,
            onTap: onDismiss,
            child: const ColoredBox(color: Colors.transparent),
          ),
        ),
      ),
    );
  }
}
