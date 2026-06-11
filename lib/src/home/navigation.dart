import 'dart:ui';

import 'package:flutter/material.dart';

import '../typography.dart';
import '../models.dart';

class M3Navigation extends StatelessWidget {
  const M3Navigation({
    super.key,
    required this.index,
    required this.palette,
    required this.hidden,
    required this.onChanged,
  });

  final int index;
  final AppPalette palette;
  final bool hidden;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) {
    final isLight = palette.background.computeLuminance() > .5;
    final destinations = const [
      NavDestination(
        outlined: Icons.bookmark_border_rounded,
        filled: Icons.bookmark_rounded,
        label: '\u9605\u8bfb\u4e2d',
      ),
      NavDestination(
        outlined: Icons.library_books_outlined,
        filled: Icons.library_books_rounded,
        label: '\u4e66\u67b6',
      ),
      NavDestination(
        outlined: Icons.grid_view_rounded,
        filled: Icons.grid_view_rounded,
        label: '\u7edf\u8ba1',
      ),
      NavDestination(
        outlined: Icons.settings_outlined,
        filled: Icons.settings_rounded,
        label: '\u8bbe\u7f6e',
      ),
    ];
    final shadowColor = isLight
        ? palette.primary.withValues(alpha: .11)
        : Colors.black.withValues(alpha: .28);
    return IgnorePointer(
      ignoring: hidden,
      child: AnimatedSlide(
        offset: hidden ? const Offset(0, 1.45) : Offset.zero,
        duration: const Duration(milliseconds: 260),
        curve: Curves.easeInOutCubic,
        child: AnimatedOpacity(
          opacity: hidden ? 0 : 1,
          duration: const Duration(milliseconds: 170),
          curve: Curves.easeOutCubic,
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 720),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(999),
                child: BackdropFilter(
                  filter: ImageFilter.blur(sigmaX: 18, sigmaY: 18),
                  child: Material(
                    color: palette.surface.withValues(
                      alpha: isLight ? .70 : .76,
                    ),
                    elevation: isLight ? 1 : 2,
                    shadowColor: shadowColor,
                    borderRadius: BorderRadius.circular(999),
                    clipBehavior: Clip.antiAlias,
                    child: SizedBox(
                      height: 82,
                      child: Padding(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 6,
                          vertical: 8,
                        ),
                        child: LayoutBuilder(
                          builder: (context, constraints) {
                            final segment =
                                constraints.maxWidth / destinations.length;
                            final indicatorColor = isLight
                                ? palette.primarySoft.withValues(alpha: .24)
                                : Color.lerp(
                                    palette.cardAlt,
                                    palette.primarySoft,
                                    .10,
                                  )!;
                            return Stack(
                              children: [
                                AnimatedPositioned(
                                  duration: const Duration(milliseconds: 360),
                                  curve: Curves.easeOutBack,
                                  left: segment * index + 3,
                                  top: 4,
                                  bottom: 4,
                                  width: segment - 6,
                                  child: DecoratedBox(
                                    decoration: BoxDecoration(
                                      color: indicatorColor,
                                      borderRadius: BorderRadius.circular(999),
                                    ),
                                  ),
                                ),
                                Row(
                                  children: [
                                    for (
                                      var i = 0;
                                      i < destinations.length;
                                      i++
                                    )
                                      Expanded(
                                        child: M3NavigationItem(
                                          destination: destinations[i],
                                          selected: i == index,
                                          palette: palette,
                                          isLight: isLight,
                                          onTap: () => onChanged(i),
                                        ),
                                      ),
                                  ],
                                ),
                              ],
                            );
                          },
                        ),
                      ),
                    ),
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

class NavDestination {
  const NavDestination({
    required this.outlined,
    required this.filled,
    required this.label,
  });

  final IconData outlined;
  final IconData filled;
  final String label;
}

class M3NavigationItem extends StatelessWidget {
  const M3NavigationItem({
    super.key,
    required this.destination,
    required this.selected,
    required this.palette,
    required this.isLight,
    required this.onTap,
  });

  final NavDestination destination;
  final bool selected;
  final AppPalette palette;
  final bool isLight;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final selectedColor = isLight ? palette.primary : palette.primarySoft;
    final foreground = selected ? selectedColor : palette.muted;

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 3),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(999),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: onTap,
          splashColor: selectedColor.withValues(alpha: .08),
          highlightColor: selectedColor.withValues(alpha: .06),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 160),
            curve: Curves.easeOutCubic,
            margin: const EdgeInsets.symmetric(vertical: 4),
            child: Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  AnimatedSwitcher(
                    duration: const Duration(milliseconds: 130),
                    switchInCurve: Curves.easeOutCubic,
                    switchOutCurve: Curves.easeInCubic,
                    child: Icon(
                      selected ? destination.filled : destination.outlined,
                      key: ValueKey(
                        '${destination.label}-${selected ? 'on' : 'off'}',
                      ),
                      size: 27,
                      color: foreground,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    destination.label,
                    maxLines: 1,
                    overflow: TextOverflow.fade,
                    softWrap: false,
                    style: TextStyle(
                      color: foreground,
                      fontSize: 12.5,
                      height: 1.05,
                      fontWeight: selected
                          ? AppTextWeight.semibold
                          : AppTextWeight.medium,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
