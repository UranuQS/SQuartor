import 'dart:ui';
import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../models.dart';
import '../typography.dart';
import 'reader_enums.dart';
import 'reader_glass_palette.dart';
import 'reader_dock.dart';

// ---------------------------------------------------------------------------
// FrostedReaderCard
// ---------------------------------------------------------------------------

class FrostedReaderCard extends StatelessWidget {
  const FrostedReaderCard({
    super.key,
    required this.readerPalette,
    required this.appPalette,
    required this.borderRadius,
    required this.child,
  });

  final ReaderPalette readerPalette;
  final AppPalette appPalette;
  final double borderRadius;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final glass = ReaderGlassPalette.from(appPalette);
    return ClipRRect(
      borderRadius: BorderRadius.circular(borderRadius),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 22, sigmaY: 22),
        child: Material(
          color: glass.panel,
          elevation: glass.dark ? 2 : 1,
          shadowColor: Colors.black.withValues(alpha: glass.dark ? .28 : .12),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(borderRadius),
          ),
          child: child,
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// ReaderProgressTrack
// ---------------------------------------------------------------------------

class ReaderProgressTrack extends StatelessWidget {
  const ReaderProgressTrack({
    super.key,
    required this.progress,
    required this.trackColor,
    required this.fillColor,
    required this.thumbColor,
    this.height = 6,
  });

  final double progress;
  final Color trackColor;
  final Color fillColor;
  final Color thumbColor;
  final double height;

  @override
  Widget build(BuildContext context) {
    final safeProgress = progress.clamp(0.0, 1.0);
    const thumbSize = 18.0;
    return LayoutBuilder(
      builder: (context, constraints) {
        final trackWidth = constraints.maxWidth;
        final thumbLeft = (trackWidth - thumbSize) * safeProgress;
        return SizedBox(
          height: thumbSize,
          child: Stack(
            alignment: Alignment.centerLeft,
            clipBehavior: Clip.none,
            children: [
              Container(
                height: height,
                decoration: BoxDecoration(
                  color: trackColor,
                  borderRadius: BorderRadius.circular(height),
                ),
              ),
              Container(
                width: trackWidth * safeProgress,
                height: height,
                decoration: BoxDecoration(
                  color: fillColor,
                  borderRadius: BorderRadius.circular(height),
                ),
              ),
              Positioned(
                left: thumbLeft,
                child: Container(
                  width: thumbSize,
                  height: thumbSize,
                  decoration: BoxDecoration(
                    color: thumbColor,
                    shape: BoxShape.circle,
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

// ---------------------------------------------------------------------------
// TopMenu
// ---------------------------------------------------------------------------

class TopMenu extends StatelessWidget {
  const TopMenu({
    super.key,
    required this.title,
    required this.palette,
    required this.appPalette,
    required this.onBack,
  });

  final String title;
  final ReaderPalette palette;
  final AppPalette appPalette;
  final VoidCallback onBack;

  @override
  Widget build(BuildContext context) {
    final glass = ReaderGlassPalette.from(appPalette);
    return FrostedReaderCard(
      readerPalette: palette,
      appPalette: appPalette,
      borderRadius: 32,
      child: SizedBox(
        height: 64,
        child: Row(
          children: [
            IconButton(
              onPressed: onBack,
              icon: Icon(Icons.arrow_back_rounded, color: glass.text),
            ),
            Expanded(
              child: Text(
                title,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: glass.text,
                  fontWeight: AppTextWeight.medium,
                ),
              ),
            ),
            const SizedBox(width: 48),
          ],
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// ReaderStatusOverlay
// ---------------------------------------------------------------------------

class ReaderStatusOverlay extends StatelessWidget {
  const ReaderStatusOverlay({
    super.key,
    required this.readerPalette,
    required this.message,
  });

  final ReaderPalette readerPalette;
  final String message;

  @override
  Widget build(BuildContext context) {
    final isError = message.startsWith('\u52a0\u8f7d\u5931\u8d25');
    return ColoredBox(
      color: readerPalette.background,
      child: Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 28),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (!isError)
                SizedBox(
                  width: 26,
                  height: 26,
                  child: CircularProgressIndicator(
                    strokeWidth: 2.6,
                    color: readerPalette.text,
                  ),
                )
              else
                Icon(
                  Icons.error_outline_rounded,
                  color: readerPalette.text,
                  size: 34,
                ),
              const SizedBox(height: 14),
              Text(
                message,
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: readerPalette.muted,
                  fontSize: 15,
                  fontWeight: AppTextWeight.regular,
                  height: 1.5,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// BottomMenu (fallback layout for wider form factors)
// ---------------------------------------------------------------------------

// ignore: unused_element
class BottomMenu extends StatelessWidget {
  const BottomMenu({
    super.key,
    required this.page,
    required this.pageCount,
    required this.progress,
    required this.readerPalette,
    required this.appPalette,
    required this.onToc,
    required this.onPreviousChapter,
    required this.onNextChapter,
    required this.onSettings,
    required this.onProgressPressed,
  });

  final int page;
  final int pageCount;
  final double progress;
  final ReaderPalette readerPalette;
  final AppPalette appPalette;
  final VoidCallback onToc;
  final VoidCallback onPreviousChapter;
  final VoidCallback onNextChapter;
  final VoidCallback onSettings;
  final VoidCallback onProgressPressed;

  @override
  Widget build(BuildContext context) {
    final iconButtonStyle = IconButton.styleFrom(
      foregroundColor: readerPalette.text,
      backgroundColor: readerPalette.muted.withValues(alpha: .12),
    );
    return FrostedReaderCard(
      readerPalette: readerPalette,
      appPalette: appPalette,
      borderRadius: 36,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(12, 12, 12, 10),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              children: [
                IconButton(
                  tooltip: '\u76ee\u5f55',
                  style: iconButtonStyle,
                  onPressed: onToc,
                  icon: const Icon(Icons.menu_book_rounded),
                ),
                const SizedBox(width: 6),
                TextButton.icon(
                  onPressed: onPreviousChapter,
                  icon: const Icon(Icons.skip_previous_rounded, size: 19),
                  label: const Text('\u4e0a\u4e00\u7ae0'),
                  style: TextButton.styleFrom(
                    foregroundColor: readerPalette.text,
                  ),
                ),
                Expanded(
                  child: Column(
                    children: [
                      Text(
                        '${(progress * 100).toStringAsFixed(1)}%',
                        style: TextStyle(
                          color: readerPalette.text,
                          fontWeight: AppTextWeight.medium,
                        ),
                      ),
                      Text(
                        '${page + 1} / $pageCount',
                        style: TextStyle(
                          color: readerPalette.muted,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                ),
                TextButton.icon(
                  onPressed: onNextChapter,
                  icon: const Icon(Icons.skip_next_rounded, size: 19),
                  label: const Text('\u4e0b\u4e00\u7ae0'),
                  iconAlignment: IconAlignment.end,
                  style: TextButton.styleFrom(
                    foregroundColor: readerPalette.text,
                  ),
                ),
                const SizedBox(width: 6),
                IconButton(
                  tooltip: '\u9605\u8bfb\u8bbe\u7f6e',
                  style: iconButtonStyle,
                  onPressed: onSettings,
                  icon: const Icon(Icons.tune_rounded),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// AdaptiveBottomMenu
// ---------------------------------------------------------------------------

class AdaptiveBottomMenu extends StatefulWidget {
  const AdaptiveBottomMenu({
    super.key,
    required this.page,
    required this.pageCount,
    required this.progress,
    required this.currentChapter,
    required this.chapterCount,
    required this.overlay,
    required this.tocProgress,
    required this.settingsProgress,
    required this.sideOverlayDismissing,
    required this.readerPalette,
    required this.appPalette,
    required this.onToc,
    required this.onPreviousChapter,
    required this.onNextChapter,
    required this.onProgressSeek,
    required this.onProgressScrubStart,
    required this.onSettings,
    required this.onProgressPressed,
  });

  final int page;
  final int pageCount;
  final double progress;
  final int currentChapter;
  final int chapterCount;
  final ReaderOverlay overlay;
  final Animation<double> tocProgress;
  final Animation<double> settingsProgress;
  final bool sideOverlayDismissing;
  final ReaderPalette readerPalette;
  final AppPalette appPalette;
  final VoidCallback onToc;
  final VoidCallback onPreviousChapter;
  final VoidCallback onNextChapter;
  final ValueChanged<double> onProgressSeek;
  final VoidCallback onProgressScrubStart;
  final VoidCallback onSettings;
  final VoidCallback onProgressPressed;

  @override
  State<AdaptiveBottomMenu> createState() => _AdaptiveBottomMenuState();
}

class _AdaptiveBottomMenuState extends State<AdaptiveBottomMenu> {
  var _progressExpanded = false;

  @override
  void didUpdateWidget(covariant AdaptiveBottomMenu oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.overlay != ReaderOverlay.chrome ||
        widget.sideOverlayDismissing) {
      _progressExpanded = false;
    }
  }

  @override
  Widget build(BuildContext context) {
    final glass = ReaderGlassPalette.from(widget.appPalette);
    final tocRaw = widget.tocProgress.value.clamp(0.0, 1.0).toDouble();
    final settingsRaw = widget.settingsProgress.value
        .clamp(0.0, 1.0)
        .toDouble();
    final sideAmount = (tocRaw + settingsRaw).clamp(0.0, 1.0).toDouble();
    final sideEase = Curves.easeOutCubic.transform(sideAmount);
    const buttonHeight = 58.0;
    final sideActive = sideEase > 0.001;
    final chapterOpacity =
        widget.overlay == ReaderOverlay.chrome &&
            !widget.sideOverlayDismissing &&
            tocRaw <= .001 &&
            settingsRaw <= .001
        ? 1.0
        : 0.0;
    return FrostedReaderCard(
      readerPalette: widget.readerPalette,
      appPalette: widget.appPalette,
      borderRadius: 40,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            SizedBox(
              height: buttonHeight,
              child: LayoutBuilder(
                builder: (context, constraints) {
                  final maxWidth = constraints.maxWidth;
                  final innerInset = maxWidth < 360 ? 3.0 : 4.0;
                  final trackWidth = math.max(0.0, maxWidth - innerInset * 2);
                  final gap = trackWidth < 340 ? 8.0 : 10.0;
                  final progressMinWidth = trackWidth < 340 ? 92.0 : 104.0;
                  final compactSideWidth = buttonHeight;
                  final collapsedSideWidth = (trackWidth * .225)
                      .clamp(
                        trackWidth < 340 ? 80.0 : 88.0,
                        trackWidth < 340 ? 112.0 : 126.0,
                      )
                      .toDouble();
                  final expandedTargetWidth =
                      (trackWidth * (trackWidth < 360 ? .40 : .43))
                          .clamp(150.0, 238.0)
                          .toDouble();
                  final sideTotal = tocRaw + settingsRaw;
                  final switchingSidePanels = tocRaw > 0.0 && settingsRaw > 0.0;
                  final settingsShare = sideTotal <= .001
                      ? 0.0
                      : (settingsRaw / sideTotal).clamp(0.0, 1.0).toDouble();
                  final easedSettingsShare =
                      widget.overlay == ReaderOverlay.settings
                      ? Curves.easeOutCubic.transform(settingsShare)
                      : 1 - Curves.easeOutCubic.transform(1 - settingsShare);
                  final tocShare = 1.0 - easedSettingsShare;
                  final maxExpandedSideWidth =
                      trackWidth -
                      compactSideWidth -
                      gap * 2 -
                      progressMinWidth;
                  final expandedSideWidth = math.max(
                    collapsedSideWidth,
                    math.min(expandedTargetWidth, maxExpandedSideWidth),
                  );
                  final chromeProgressLeft =
                      innerInset + collapsedSideWidth + gap;
                  final chromeProgressWidth =
                      maxWidth -
                      innerInset -
                      collapsedSideWidth -
                      gap -
                      chromeProgressLeft;
                  final tocLeft = innerInset;
                  final chromeSettingsLeft =
                      maxWidth - innerInset - collapsedSideWidth;
                  late final double tocWidth;
                  late final double settingsWidth;
                  late final double settingsLeft;
                  late final double progressLeft;
                  late final double progressWidth;
                  if (switchingSidePanels) {
                    final tocSideTarget =
                        tocShare * expandedSideWidth +
                        easedSettingsShare * compactSideWidth;
                    final settingsSideTarget =
                        easedSettingsShare * expandedSideWidth +
                        tocShare * compactSideWidth;
                    tocWidth = lerpDouble(
                      collapsedSideWidth,
                      tocSideTarget,
                      sideEase,
                    )!;
                    settingsWidth = lerpDouble(
                      collapsedSideWidth,
                      settingsSideTarget,
                      sideEase,
                    )!;
                    settingsLeft = maxWidth - innerInset - settingsWidth;
                    final sideProgressLeft = tocLeft + tocWidth + gap;
                    final sideProgressWidth = math.max(
                      progressMinWidth,
                      settingsLeft - gap - sideProgressLeft,
                    );
                    progressLeft = lerpDouble(
                      chromeProgressLeft,
                      sideProgressLeft,
                      sideEase,
                    )!;
                    progressWidth = lerpDouble(
                      chromeProgressWidth,
                      sideProgressWidth,
                      sideEase,
                    )!;
                  } else {
                    final tocActive = tocRaw > 0.0;
                    final settingsActive = settingsRaw > 0.0;
                    final tocTargetWidth = tocActive
                        ? expandedSideWidth
                        : compactSideWidth;
                    final settingsTargetWidth = settingsActive
                        ? expandedSideWidth
                        : compactSideWidth;
                    final targetSettingsLeft =
                        maxWidth - innerInset - settingsTargetWidth;
                    final targetProgressLeft = tocLeft + tocTargetWidth + gap;
                    final targetProgressWidth = math.max(
                      progressMinWidth,
                      targetSettingsLeft - gap - targetProgressLeft,
                    );
                    tocWidth = lerpDouble(
                      collapsedSideWidth,
                      tocTargetWidth,
                      sideEase,
                    )!;
                    settingsWidth = lerpDouble(
                      collapsedSideWidth,
                      settingsTargetWidth,
                      sideEase,
                    )!;
                    settingsLeft = lerpDouble(
                      chromeSettingsLeft,
                      targetSettingsLeft,
                      sideEase,
                    )!;
                    progressLeft = lerpDouble(
                      chromeProgressLeft,
                      targetProgressLeft,
                      sideEase,
                    )!;
                    progressWidth = lerpDouble(
                      chromeProgressWidth,
                      targetProgressWidth,
                      sideEase,
                    )!;
                  }
                  const sideOpacity = 1.0;
                  const actionIconSize = 26.0;
                  const actionLabelGap = 10.0;
                  const actionLabelWidth = 34.0;
                  const actionGroupWidth =
                      actionIconSize + actionLabelGap + actionLabelWidth;
                  final tocExpandedT = (sideEase * tocShare)
                      .clamp(0.0, 1.0)
                      .toDouble();
                  final settingsExpandedT = (sideEase * easedSettingsShare)
                      .clamp(0.0, 1.0)
                      .toDouble();
                  double actionIconLeft({
                    required double currentWidth,
                    required double expandedT,
                    required bool activeAlone,
                  }) {
                    if (switchingSidePanels) {
                      final groupWidth = lerpDouble(
                        actionIconSize,
                        actionGroupWidth,
                        expandedT,
                      )!;
                      return math.max(0.0, (currentWidth - groupWidth) / 2);
                    }
                    if (!activeAlone) {
                      return math.max(0.0, (currentWidth - actionIconSize) / 2);
                    }
                    final collapsedIconLeft =
                        (collapsedSideWidth - actionIconSize) / 2;
                    final expandedIconLeft =
                        (expandedSideWidth - actionGroupWidth) / 2;
                    return math.max(
                      0.0,
                      lerpDouble(
                        collapsedIconLeft,
                        expandedIconLeft,
                        expandedT,
                      )!,
                    );
                  }

                  final tocIconLeft = actionIconLeft(
                    currentWidth: tocWidth,
                    expandedT: tocExpandedT,
                    activeAlone: tocRaw > 0.0 && settingsRaw <= 0.0,
                  );
                  final settingsIconLeft = actionIconLeft(
                    currentWidth: settingsWidth,
                    expandedT: settingsExpandedT,
                    activeAlone: settingsRaw > 0.0 && tocRaw <= 0.0,
                  );
                  return Stack(
                    children: [
                      Positioned(
                        left: tocLeft,
                        top: 0,
                        width: tocWidth,
                        height: buttonHeight,
                        child: IgnorePointer(
                          ignoring: false,
                          child: Opacity(
                            opacity: sideOpacity,
                            child: ReaderDockActionPill(
                              icon: Icons.menu_book_rounded,
                              label: '\u76ee\u5f55',
                              glass: glass,
                              selected: widget.overlay == ReaderOverlay.toc,
                              labelProgress: sideEase,
                              compact: sideActive && tocShare < settingsShare,
                              transparent: false,
                              showContent: false,
                              onPressed: widget.onToc,
                            ),
                          ),
                        ),
                      ),
                      Positioned(
                        left: progressLeft,
                        top: 0,
                        width: progressWidth,
                        height: buttonHeight,
                        child: ReaderProgressBatteryPill(
                          progress: widget.progress,
                          currentChapter: widget.currentChapter,
                          chapterCount: widget.chapterCount,
                          glass: glass,
                          chapterOpacity: chapterOpacity,
                          rulerVisible: _progressExpanded,
                          onRulerVisibilityChanged: (visible) {
                            setState(() => _progressExpanded = visible);
                          },
                          onSeek: widget.onProgressSeek,
                          onScrubStart: widget.onProgressScrubStart,
                          onPressed: widget.onProgressPressed,
                        ),
                      ),
                      Positioned(
                        left: settingsLeft,
                        top: 0,
                        width: settingsWidth,
                        height: buttonHeight,
                        child: IgnorePointer(
                          ignoring: false,
                          child: Opacity(
                            opacity: sideOpacity,
                            child: ReaderDockActionPill(
                              icon: Icons.tune_rounded,
                              label: '\u8bbe\u7f6e',
                              glass: glass,
                              selected:
                                  widget.overlay == ReaderOverlay.settings,
                              labelProgress: sideEase,
                              compact: sideActive && settingsShare < tocShare,
                              transparent: false,
                              showContent: false,
                              onPressed: widget.onSettings,
                            ),
                          ),
                        ),
                      ),
                      Positioned(
                        left: tocLeft,
                        top: 0,
                        width: tocWidth,
                        height: buttonHeight,
                        child: IgnorePointer(
                          child: Opacity(
                            opacity: sideOpacity,
                            child: ReaderDockActionContent(
                              icon: Icons.menu_book_rounded,
                              label: '\u76ee\u5f55',
                              glass: glass,
                              labelProgress: tocExpandedT,
                              iconLeft: tocIconLeft,
                              labelWidth: actionLabelWidth,
                            ),
                          ),
                        ),
                      ),
                      Positioned(
                        left: settingsLeft,
                        top: 0,
                        width: settingsWidth,
                        height: buttonHeight,
                        child: IgnorePointer(
                          child: Opacity(
                            opacity: sideOpacity,
                            child: ReaderDockActionContent(
                              icon: Icons.tune_rounded,
                              label: '\u8bbe\u7f6e',
                              glass: glass,
                              labelProgress: settingsExpandedT,
                              iconLeft: settingsIconLeft,
                              labelWidth: actionLabelWidth,
                            ),
                          ),
                        ),
                      ),
                    ],
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}
