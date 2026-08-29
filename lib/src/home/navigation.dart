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
    required this.bottomPadding,
    required this.onChanged,
  });

  final int index;
  final AppPalette palette;
  final bool hidden;
  final double bottomPadding;
  final ValueChanged<int> onChanged;

  static const barHeight = 82.0;

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
        ? palette.primary.withValues(alpha: .16)
        : Colors.black.withValues(alpha: .30);
    final navColor = isLight
        ? Color.lerp(palette.surface, Colors.white, .42)!.withValues(alpha: .58)
        : Color.lerp(
            palette.surface,
            Colors.black,
            .02,
          )!.withValues(alpha: .52);
    final topSheen = isLight
        ? Colors.white.withValues(alpha: .48)
        : Colors.white.withValues(alpha: .16);
    return IgnorePointer(
      ignoring: hidden,
      child: AnimatedSlide(
        offset: hidden ? const Offset(0, 1.45) : Offset.zero,
        duration: const Duration(milliseconds: 260),
        curve: Curves.easeInOutCubic,
        child: ClipRRect(
          borderRadius: BorderRadius.circular(999),
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 30, sigmaY: 30),
            child: Material(
              color: navColor,
              elevation: isLight ? 4 : 3,
              shadowColor: shadowColor,
              shape: const StadiumBorder(),
              clipBehavior: Clip.antiAlias,
              child: SizedBox(
                height: barHeight + bottomPadding,
                child: Stack(
                  children: [
                    Positioned(
                      left: 28,
                      right: 28,
                      top: 0,
                      height: 1.5,
                      child: DecoratedBox(
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            colors: [
                              Colors.transparent,
                              topSheen,
                              Colors.transparent,
                            ],
                            stops: const [0, .5, 1],
                          ),
                        ),
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 6,
                        vertical: 8,
                      ),
                      child: LayoutBuilder(
                        builder: (context, constraints) {
                          final segment =
                              constraints.maxWidth / destinations.length;
                          final indicatorColor = isLight
                              ? palette.primarySoft.withValues(alpha: .40)
                              : Color.lerp(
                                  palette.cardAlt,
                                  palette.primarySoft,
                                  .28,
                                )!.withValues(alpha: .58);
                          return Stack(
                            children: [
                              AnimatedPositioned(
                                duration: const Duration(milliseconds: 300),
                                curve: Curves.easeOutCubic,
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
                                  for (var i = 0; i < destinations.length; i++)
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
                  ],
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
    final selectedColor = isLight
        ? Color.lerp(palette.primary, palette.text, .18)!
        : palette.primarySoft;
    final inactiveColor = isLight
        ? Color.lerp(palette.muted, palette.text, .34)!
        : Color.lerp(palette.muted, palette.text, .18)!;
    final foreground = selected ? selectedColor : inactiveColor;

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
