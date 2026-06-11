import 'dart:ui';

import 'package:flutter/material.dart';

import '../app_state.dart';
import '../typography.dart';
import '../models.dart';

class ImportActivityOverlay extends StatelessWidget {
  const ImportActivityOverlay({
    super.key,
    required this.state,
    required this.bottom,
  });

  final AppState state;
  final double bottom;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: state.messageChanges,
      builder: (context, _) {
        final activity = state.importActivity;
        return Positioned(
          left: 24,
          right: 24,
          bottom: bottom,
          child: AnimatedSwitcher(
            duration: const Duration(milliseconds: 220),
            reverseDuration: const Duration(milliseconds: 180),
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
                  position: Tween<Offset>(
                    begin: const Offset(0, .18),
                    end: Offset.zero,
                  ).animate(curved),
                  child: child,
                ),
              );
            },
            child: activity == null
                ? const SizedBox.shrink(key: ValueKey('import-empty'))
                : ImportActivityBanner(
                    key: ValueKey('${activity.title}-${activity.detail}'),
                    activity: activity,
                    palette: state.palette,
                  ),
          ),
        );
      },
    );
  }
}

class ImportActivityBanner extends StatelessWidget {
  const ImportActivityBanner({
    super.key,
    required this.activity,
    required this.palette,
  });

  final ImportActivity activity;
  final AppPalette palette;

  @override
  Widget build(BuildContext context) {
    final accent = activity.failed ? Colors.redAccent : palette.primarySoft;
    final isLight =
        ThemeData.estimateBrightnessForColor(palette.background) ==
        Brightness.light;
    return ClipRRect(
      borderRadius: BorderRadius.circular(22),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 18, sigmaY: 18),
        child: Material(
          color: palette.surface.withValues(alpha: isLight ? .72 : .78),
          elevation: isLight ? 1 : 2,
          shadowColor: Colors.black.withValues(alpha: isLight ? .12 : .28),
          borderRadius: BorderRadius.circular(22),
          clipBehavior: Clip.antiAlias,
          child: SizedBox(
            height: 72,
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                const SizedBox(width: 18),
                SizedBox(
                  width: 32,
                  height: 32,
                  child: activity.active
                      ? CircularProgressIndicator(
                          strokeWidth: 2.8,
                          valueColor: AlwaysStoppedAnimation(accent),
                          backgroundColor: accent.withValues(alpha: .12),
                        )
                      : Icon(
                          activity.failed
                              ? Icons.error_outline_rounded
                              : Icons.check_circle_outline_rounded,
                          color: accent,
                        ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        activity.title,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          color: palette.text,
                          fontSize: 13,
                          fontWeight: AppTextWeight.semibold,
                        ),
                      ),
                      const SizedBox(height: 3),
                      Text(
                        activity.detail,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          color: palette.muted,
                          fontSize: 12,
                          fontWeight: AppTextWeight.regular,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 18),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
