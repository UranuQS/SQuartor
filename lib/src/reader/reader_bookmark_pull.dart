import 'package:flutter/material.dart';

import '../models.dart';
import '../typography.dart';
import 'reader_glass_palette.dart';

class ReaderBookmarkPullOverlay extends StatelessWidget {
  const ReaderBookmarkPullOverlay({
    super.key,
    required this.pullDy,
    required this.active,
    required this.committed,
    required this.hasBookmark,
    required this.palette,
    required this.readerPalette,
    required this.systemPadding,
  });

  static const threshold = 92.0;

  final double pullDy;
  final bool active;
  final bool committed;
  final bool hasBookmark;
  final AppPalette palette;
  final ReaderPalette readerPalette;
  final EdgeInsets systemPadding;

  @override
  Widget build(BuildContext context) {
    final progress = (pullDy / threshold).clamp(0.0, 1.0).toDouble();
    final visible = active || committed;
    final glass = ReaderGlassPalette.from(palette);
    final actionText = hasBookmark
        ? (progress >= 1
              ? '\u677e\u624b\u53d6\u6d88\u4e66\u7b7e'
              : '\u4e0b\u62c9\u53d6\u6d88\u4e66\u7b7e')
        : (progress >= 1
              ? '\u677e\u624b\u6dfb\u52a0\u4e66\u7b7e'
              : '\u4e0b\u62c9\u6dfb\u52a0\u4e66\u7b7e');
    return IgnorePointer(
      child: Stack(
        children: [
          Positioned(
            top: systemPadding.top + 18,
            left: 0,
            right: 0,
            child: AnimatedOpacity(
              opacity: visible ? 1 : 0,
              duration: const Duration(milliseconds: 140),
              curve: Curves.easeOutCubic,
              child: Center(
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: readerPalette.background.withValues(alpha: .72),
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 14,
                      vertical: 8,
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(
                            value: progress,
                            strokeWidth: 2.2,
                            backgroundColor: glass.line.withValues(alpha: .34),
                            valueColor: AlwaysStoppedAnimation(
                              progress >= 1 ? palette.primarySoft : glass.text,
                            ),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Text(
                          actionText,
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
              ),
            ),
          ),
          Positioned(
            top: 0,
            right: systemPadding.right + 34,
            child: AnimatedSlide(
              offset: hasBookmark || committed
                  ? Offset.zero
                  : const Offset(0, -1.08),
              duration: const Duration(milliseconds: 260),
              curve: hasBookmark || committed
                  ? Curves.easeOutBack
                  : Curves.easeInCubic,
              child: _BookmarkHanger(color: palette.primarySoft),
            ),
          ),
        ],
      ),
    );
  }
}

class _BookmarkHanger extends StatelessWidget {
  const _BookmarkHanger({required this.color});

  final Color color;

  @override
  Widget build(BuildContext context) {
    return ClipPath(
      clipper: _BookmarkHangerClipper(),
      child: Container(width: 28, height: 40, color: color),
    );
  }
}

class _BookmarkHangerClipper extends CustomClipper<Path> {
  @override
  Path getClip(Size size) {
    final notchTop = size.height * .68;
    return Path()
      ..moveTo(0, 0)
      ..lineTo(size.width, 0)
      ..lineTo(size.width, size.height)
      ..lineTo(size.width / 2, notchTop)
      ..lineTo(0, size.height)
      ..close();
  }

  @override
  bool shouldReclip(covariant _BookmarkHangerClipper oldClipper) => false;
}
