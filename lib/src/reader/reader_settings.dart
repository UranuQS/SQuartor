import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../app_state.dart';
import '../models.dart';
import '../typography.dart';
import 'reader_glass_palette.dart';

// ---------------------------------------------------------------------------
// ReaderSettingsSheet
// ---------------------------------------------------------------------------

class ReaderSettingsSheet extends StatefulWidget {
  const ReaderSettingsSheet({
    super.key,
    required this.state,
    required this.onChanged,
  });

  final AppState state;
  final VoidCallback onChanged;

  @override
  State<ReaderSettingsSheet> createState() => _ReaderSettingsSheetState();
}

class _ReaderSettingsSheetState extends State<ReaderSettingsSheet> {
  late ReadingStyle _draft = widget.state.style;
  final _acceptInput = true;
  Timer? _deferredCommitTimer;

  @override
  void dispose() {
    _deferredCommitTimer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: widget.state.appChanges,
      builder: (context, _) {
        final palette = widget.state.palette;
        final glass = ReaderGlassPalette.from(palette);
        final style = _draft;
        return IgnorePointer(
          ignoring: !_acceptInput,
          child: Column(
            children: [
              ReaderSettingsHeader(glass: glass),
              Expanded(
                child: ListView(
                  padding: EdgeInsets.fromLTRB(
                    20,
                    18,
                    20,
                    22 + MediaQuery.viewInsetsOf(context).bottom,
                  ),
                  children: [
                    ReaderSectionLabel(
                      label: '\u5e38\u7528\u5fae\u8c03',
                      glass: glass,
                    ),
                    const SizedBox(height: 10),
                    ReaderSettingsCard(
                      glass: glass,
                      children: [
                        ReaderFlowSelector(
                          palette: palette,
                          glass: glass,
                          value: style.readingFlow,
                          onChanged: (value) {
                            if (value == style.readingFlow) {
                              return;
                            }
                            _preview(
                              style.copyWith(readingFlow: value),
                              localOnly: true,
                            );
                            _commit(delay: const Duration(milliseconds: 260));
                          },
                        ),
                        const ReaderCardDivider(),
                        SheetSlider(
                          label: '\u5b57\u53f7',
                          valueLabel: style.fontSize.toStringAsFixed(0),
                          value: style.fontSize.clamp(14, 32).toDouble(),
                          min: 14,
                          max: 32,
                          divisions: 18,
                          palette: palette,
                          onChanged: (value) =>
                              _preview(style.copyWith(fontSize: value)),
                          onChangeEnd: (_) => _commit(),
                        ),
                        const ReaderCardDivider(),
                        SheetSlider(
                          label: '\u884c\u9ad8',
                          valueLabel: style.lineHeight.toStringAsFixed(2),
                          value: style.lineHeight,
                          min: 1.2,
                          max: 2.6,
                          divisions: 14,
                          palette: palette,
                          onChanged: (value) =>
                              _preview(style.copyWith(lineHeight: value)),
                          onChangeEnd: (_) => _commit(),
                        ),
                        const ReaderCardDivider(),
                        SheetSlider(
                          label: '\u5de6\u53f3\u8fb9\u8ddd',
                          valueLabel: style.pageMargin.toStringAsFixed(0),
                          value: style.pageMargin,
                          min: 0,
                          max: 52,
                          divisions: 26,
                          palette: palette,
                          onChanged: (value) =>
                              _preview(style.copyWith(pageMargin: value)),
                          onChangeEnd: (_) => _commit(),
                        ),
                        const ReaderCardDivider(),
                        SheetSlider(
                          label: '\u4e0a\u4e0b\u8fb9\u8ddd',
                          valueLabel: style.verticalMargin.toStringAsFixed(0),
                          value: style.verticalMargin,
                          min: 4,
                          max: 96,
                          divisions: 24,
                          palette: palette,
                          onChanged: (value) =>
                              _preview(style.copyWith(verticalMargin: value)),
                          onChangeEnd: (_) => _commit(),
                        ),
                      ],
                    ),
                    const SizedBox(height: 18),
                    ReaderSectionLabel(
                      label: '\u9605\u8bfb\u80cc\u666f',
                      glass: glass,
                    ),
                    const SizedBox(height: 10),
                    ReaderSettingsCard(
                      glass: glass,
                      children: [
                        ReaderBackgroundSelector(
                          palette: palette,
                          glass: glass,
                          value: style.readerBackground,
                          onChanged: (value) {
                            if (value == style.readerBackground) {
                              return;
                            }
                            _preview(
                              style.copyWith(readerBackground: value),
                              localOnly: true,
                            );
                            _commit(delay: const Duration(milliseconds: 230));
                          },
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    ReaderSettingsCard(
                      glass: glass,
                      padding: EdgeInsets.zero,
                      children: [
                        Theme(
                          data: Theme.of(
                            context,
                          ).copyWith(dividerColor: Colors.transparent),
                          child: ExpansionTile(
                            tilePadding: const EdgeInsets.fromLTRB(
                              16,
                              4,
                              12,
                              4,
                            ),
                            childrenPadding: const EdgeInsets.fromLTRB(
                              16,
                              0,
                              16,
                              16,
                            ),
                            iconColor: glass.text,
                            collapsedIconColor: glass.muted,
                            title: Text(
                              '\u66f4\u591a\u7ec6\u8c03',
                              style: TextStyle(
                                color: glass.text,
                                fontWeight: AppTextWeight.medium,
                              ),
                            ),
                            subtitle: Text(
                              '\u6bb5\u8ddd\u3001\u5b57\u8ddd\u3001\u70b9\u51fb\u65b9\u5411\u3001\u7f29\u8fdb\u4e0e\u4e66\u7c4d\u5b57\u4f53',
                              style: TextStyle(
                                color: glass.muted,
                                fontSize: 12,
                              ),
                            ),
                            children: [
                              SheetSlider(
                                label: '\u6bb5\u8ddd',
                                valueLabel: style.paragraphSpacing
                                    .toStringAsFixed(0),
                                value: style.paragraphSpacing,
                                min: 0,
                                max: 30,
                                divisions: 15,
                                palette: palette,
                                onChanged: (value) => _preview(
                                  style.copyWith(paragraphSpacing: value),
                                ),
                                onChangeEnd: (_) => _commit(),
                              ),
                              const ReaderCardDivider(),
                              SheetSlider(
                                label: '\u5b57\u8ddd',
                                valueLabel: style.letterSpacing.toStringAsFixed(
                                  1,
                                ),
                                value: style.letterSpacing,
                                min: 0,
                                max: 2,
                                divisions: 20,
                                palette: palette,
                                onChanged: (value) => _preview(
                                  style.copyWith(letterSpacing: value),
                                ),
                                onChangeEnd: (_) => _commit(),
                              ),
                              const ReaderCardDivider(),
                              ReaderInlineSwitch(
                                palette: palette,
                                glass: glass,
                                title: '\u53cd\u8f6c\u70b9\u51fb\u7ffb\u9875',
                                subtitle:
                                    '\u5de6\u53f3\u70b9\u51fb\u533a\u57df\u4e92\u6362',
                                value: style.reverseTapPageTurn,
                                onChanged: (value) {
                                  _preview(
                                    style.copyWith(reverseTapPageTurn: value),
                                  );
                                  _commit();
                                },
                              ),
                              const ReaderCardDivider(),
                              ReaderInlineSwitch(
                                palette: palette,
                                glass: glass,
                                title: '\u9996\u884c\u7f29\u8fdb',
                                subtitle:
                                    '\u6b63\u6587\u6bb5\u843d\u5f00\u5934\u7f29\u8fdb\u4e24\u4e2a\u6c49\u5b57',
                                value: style.firstLineIndent,
                                onChanged: (value) {
                                  _preview(
                                    style.copyWith(firstLineIndent: value),
                                  );
                                  _commit();
                                },
                              ),
                              const SizedBox(height: 12),
                              FilledButton.icon(
                                style: FilledButton.styleFrom(
                                  backgroundColor: palette.primary,
                                  foregroundColor: Colors.white,
                                  minimumSize: const Size.fromHeight(46),
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(16),
                                  ),
                                ),
                                onPressed: () {
                                  widget.state.importReaderFont();
                                  widget.onChanged();
                                },
                                icon: const Icon(Icons.upload_file_rounded),
                                label: Text(
                                  style.fontName == null
                                      ? '\u5bfc\u5165\u4e66\u7c4d\u5b57\u4f53 .ttf / .otf'
                                      : '\u4e66\u7c4d\u5b57\u4f53\uff1a${style.fontName}',
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  void _preview(ReadingStyle style, {bool localOnly = false}) {
    setState(() => _draft = style);
    if (localOnly) {
      return;
    }
    _deferredCommitTimer?.cancel();
    unawaited(widget.state.updateStyle(style));
    widget.onChanged();
  }

  void _commit({Duration delay = Duration.zero}) {
    _deferredCommitTimer?.cancel();
    if (delay > Duration.zero) {
      _deferredCommitTimer = Timer(delay, _commitNow);
      return;
    }
    _commitNow();
  }

  void _commitNow() {
    if (!mounted) {
      return;
    }
    unawaited(widget.state.updateStyle(_draft, immediate: true));
    widget.onChanged();
  }
}

// ---------------------------------------------------------------------------
// ReaderSettingsHeader
// ---------------------------------------------------------------------------

class ReaderSettingsHeader extends StatelessWidget {
  const ReaderSettingsHeader({super.key, required this.glass});

  final ReaderGlassPalette glass;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(color: glass.panel),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 18, 12, 14),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '\u8c03\u6574\u9605\u8bfb\u7248\u5f0f\u4e0e\u80cc\u666f',
                    style: TextStyle(
                      color: glass.text,
                      fontSize: 24,
                      fontWeight: AppTextWeight.semibold,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// ReaderSectionLabel
// ---------------------------------------------------------------------------

class ReaderSectionLabel extends StatelessWidget {
  const ReaderSectionLabel({
    super.key,
    required this.label,
    required this.glass,
  });

  final String label;
  final ReaderGlassPalette glass;

  @override
  Widget build(BuildContext context) {
    return Text(
      label,
      style: TextStyle(
        color: glass.muted,
        fontSize: 13,
        fontWeight: AppTextWeight.medium,
        letterSpacing: .4,
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// ReaderSettingsCard
// ---------------------------------------------------------------------------

class ReaderSettingsCard extends StatelessWidget {
  const ReaderSettingsCard({
    super.key,
    required this.glass,
    required this.children,
    this.padding = const EdgeInsets.all(12),
  });

  final ReaderGlassPalette glass;
  final List<Widget> children;
  final EdgeInsetsGeometry padding;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: glass.pill,
        borderRadius: BorderRadius.circular(22),
      ),
      child: Padding(
        padding: padding,
        child: Column(mainAxisSize: MainAxisSize.min, children: children),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// ReaderCardDivider
// ---------------------------------------------------------------------------

class ReaderCardDivider extends StatelessWidget {
  const ReaderCardDivider({super.key});

  @override
  Widget build(BuildContext context) {
    return const SizedBox(height: 12);
  }
}

// ---------------------------------------------------------------------------
// ReaderInlineSwitch
// ---------------------------------------------------------------------------

class ReaderInlineSwitch extends StatelessWidget {
  const ReaderInlineSwitch({
    super.key,
    required this.palette,
    required this.glass,
    required this.title,
    required this.subtitle,
    required this.value,
    required this.onChanged,
  });

  final AppPalette palette;
  final ReaderGlassPalette glass;
  final String title;
  final String subtitle;
  final bool value;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(16),
      onTap: () => onChanged(!value),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 6),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: TextStyle(
                      color: glass.text,
                      fontWeight: AppTextWeight.medium,
                    ),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    subtitle,
                    style: TextStyle(color: glass.muted, fontSize: 12),
                  ),
                ],
              ),
            ),
            Switch(
              value: value,
              activeTrackColor: palette.primary,
              activeThumbColor: palette.primarySoft,
              inactiveTrackColor: glass.line.withValues(alpha: .55),
              inactiveThumbColor: glass.muted,
              onChanged: onChanged,
            ),
          ],
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// ReaderFlowSelector
// ---------------------------------------------------------------------------

class ReaderFlowSelector extends StatelessWidget {
  const ReaderFlowSelector({
    super.key,
    required this.palette,
    required this.glass,
    required this.value,
    required this.onChanged,
  });

  final AppPalette palette;
  final ReaderGlassPalette glass;
  final ReadingFlowMode value;
  final ValueChanged<ReadingFlowMode> onChanged;

  @override
  Widget build(BuildContext context) {
    final isScroll = value == ReadingFlowMode.scroll;
    final selectedPillColor = glass.dark
        ? Color.lerp(glass.pill, palette.primarySoft, .10)!
        : Color.lerp(glass.pill, palette.primarySoft, .24)!;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '\u9605\u8bfb\u65b9\u5f0f',
          style: TextStyle(
            color: glass.muted,
            fontWeight: AppTextWeight.regular,
          ),
        ),
        const SizedBox(height: 10),
        SizedBox(
          height: 46,
          child: LayoutBuilder(
            builder: (context, constraints) {
              final segment = constraints.maxWidth / 2;
              return DecoratedBox(
                decoration: BoxDecoration(
                  color: glass.pill.withValues(alpha: glass.dark ? .78 : .64),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: Stack(
                  children: [
                    AnimatedPositioned(
                      duration: const Duration(milliseconds: 320),
                      curve: Curves.easeOutBack,
                      left: isScroll ? segment + 4 : 4,
                      top: 4,
                      bottom: 4,
                      width: segment - 8,
                      child: DecoratedBox(
                        decoration: BoxDecoration(
                          color: selectedPillColor,
                          borderRadius: BorderRadius.circular(999),
                        ),
                      ),
                    ),
                    Row(
                      children: [
                        ReaderFlowSegment(
                          label: '\u5de6\u53f3\u7ffb\u9875',
                          selected: !isScroll,
                          glass: glass,
                          onTap: () => onChanged(ReadingFlowMode.paged),
                        ),
                        ReaderFlowSegment(
                          label: '\u4e0a\u4e0b\u6eda\u52a8',
                          selected: isScroll,
                          glass: glass,
                          onTap: () => onChanged(ReadingFlowMode.scroll),
                        ),
                      ],
                    ),
                  ],
                ),
              );
            },
          ),
        ),
      ],
    );
  }
}

// ---------------------------------------------------------------------------
// ReaderBackgroundSelector
// ---------------------------------------------------------------------------

class ReaderBackgroundSelector extends StatelessWidget {
  const ReaderBackgroundSelector({
    super.key,
    required this.palette,
    required this.glass,
    required this.value,
    required this.onChanged,
  });

  final AppPalette palette;
  final ReaderGlassPalette glass;
  final ReaderBackgroundId value;
  final ValueChanged<ReaderBackgroundId> onChanged;

  @override
  Widget build(BuildContext context) {
    final items = readerPalettes.values
        .where((item) => item.id != ReaderBackgroundId.green)
        .toList(growable: false);
    final rawSelectedIndex = items.indexWhere((item) => item.id == value);
    final selectedIndex = rawSelectedIndex < 0 ? 0 : rawSelectedIndex;
    final selectedPillColor = glass.dark
        ? Color.lerp(glass.pill, palette.primarySoft, .10)!
        : Color.lerp(glass.pill, palette.primarySoft, .24)!;
    return SizedBox(
      height: 48,
      child: LayoutBuilder(
        builder: (context, constraints) {
          final segment = constraints.maxWidth / items.length;
          return DecoratedBox(
            decoration: BoxDecoration(
              color: glass.pill.withValues(alpha: glass.dark ? .78 : .64),
              borderRadius: BorderRadius.circular(999),
            ),
            child: Stack(
              children: [
                AnimatedPositioned(
                  duration: const Duration(milliseconds: 320),
                  curve: Curves.easeOutBack,
                  left: selectedIndex * segment + 4,
                  top: 4,
                  bottom: 4,
                  width: segment - 8,
                  child: DecoratedBox(
                    decoration: BoxDecoration(
                      color: selectedPillColor,
                      borderRadius: BorderRadius.circular(999),
                    ),
                  ),
                ),
                Row(
                  children: [
                    for (final item in items)
                      ReaderBackgroundSegment(
                        label: item.label,
                        selected: item.id == items[selectedIndex].id,
                        glass: glass,
                        onTap: () => onChanged(item.id),
                      ),
                  ],
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// ReaderBackgroundSegment
// ---------------------------------------------------------------------------

class ReaderBackgroundSegment extends StatelessWidget {
  const ReaderBackgroundSegment({
    super.key,
    required this.label,
    required this.selected,
    required this.glass,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final ReaderGlassPalette glass;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: InkWell(
        borderRadius: BorderRadius.circular(999),
        onTap: onTap,
        child: Center(
          child: AnimatedDefaultTextStyle(
            duration: const Duration(milliseconds: 160),
            curve: Curves.easeOut,
            style: TextStyle(
              color: selected ? glass.text : glass.muted,
              fontWeight: selected
                  ? AppTextWeight.semibold
                  : AppTextWeight.regular,
              fontSize: 14,
            ),
            child: Text(label, maxLines: 1, overflow: TextOverflow.ellipsis),
          ),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// ReaderFlowSegment
// ---------------------------------------------------------------------------

class ReaderFlowSegment extends StatelessWidget {
  const ReaderFlowSegment({
    super.key,
    required this.label,
    required this.selected,
    required this.glass,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final ReaderGlassPalette glass;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: InkWell(
        borderRadius: BorderRadius.circular(999),
        onTap: onTap,
        child: Center(
          child: AnimatedDefaultTextStyle(
            duration: const Duration(milliseconds: 180),
            curve: Curves.easeOutCubic,
            style: TextStyle(
              color: selected ? glass.text : glass.muted,
              fontSize: 14,
              fontWeight: selected
                  ? AppTextWeight.semibold
                  : AppTextWeight.medium,
            ),
            child: Text(label),
          ),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// SheetSlider
// ---------------------------------------------------------------------------

class SheetSlider extends StatelessWidget {
  const SheetSlider({
    super.key,
    required this.label,
    required this.valueLabel,
    required this.value,
    required this.min,
    required this.max,
    required this.divisions,
    required this.palette,
    required this.onChanged,
    required this.onChangeEnd,
  });

  final String label;
  final String valueLabel;
  final double value;
  final double min;
  final double max;
  final int divisions;
  final AppPalette palette;
  final ValueChanged<double> onChanged;
  final ValueChanged<double> onChangeEnd;

  @override
  Widget build(BuildContext context) {
    final glass = ReaderGlassPalette.from(palette);
    final step = (max - min) / divisions;

    void nudge(int direction) {
      final raw = value + step * direction;
      final snapped = ((raw - min) / step).round() * step + min;
      final next = snapped.clamp(min, max).toDouble();
      if ((next - value).abs() < 0.001) {
        return;
      }
      HapticFeedback.selectionClick();
      onChanged(next);
      onChangeEnd(next);
    }

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Column(
        children: [
          Row(
            children: [
              Text(
                label,
                style: TextStyle(
                  color: glass.muted,
                  fontWeight: AppTextWeight.regular,
                ),
              ),
              const Spacer(),
              Text(
                valueLabel,
                textAlign: TextAlign.right,
                style: TextStyle(
                  color: glass.text,
                  fontSize: 16,
                  fontWeight: AppTextWeight.medium,
                ),
              ),
            ],
          ),
          const SizedBox(height: 1),
          Row(
            children: [
              SheetStepButton(
                icon: Icons.remove_rounded,
                palette: palette,
                glass: glass,
                onTap: () => nudge(-1),
              ),
              Expanded(
                child: SliderTheme(
                  data: SliderTheme.of(context).copyWith(
                    trackHeight: 5,
                    activeTrackColor: palette.primary,
                    inactiveTrackColor: glass.line.withValues(alpha: .55),
                    thumbColor: palette.primarySoft,
                    overlayColor: palette.primary.withValues(alpha: .14),
                    thumbShape: const RoundSliderThumbShape(
                      enabledThumbRadius: 8,
                    ),
                    overlayShape: const RoundSliderOverlayShape(
                      overlayRadius: 15,
                    ),
                    tickMarkShape: SliderTickMarkShape.noTickMark,
                  ),
                  child: Slider(
                    value: value,
                    min: min,
                    max: max,
                    divisions: divisions,
                    onChanged: onChanged,
                    onChangeEnd: onChangeEnd,
                  ),
                ),
              ),
              SheetStepButton(
                icon: Icons.add_rounded,
                palette: palette,
                glass: glass,
                onTap: () => nudge(1),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// SheetStepButton
// ---------------------------------------------------------------------------

class SheetStepButton extends StatelessWidget {
  const SheetStepButton({
    super.key,
    required this.icon,
    required this.palette,
    required this.glass,
    required this.onTap,
  });

  final IconData icon;
  final AppPalette palette;
  final ReaderGlassPalette glass;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 36,
      height: 36,
      child: IconButton.filledTonal(
        style: IconButton.styleFrom(
          backgroundColor: palette.surface,
          foregroundColor: glass.muted,
          padding: EdgeInsets.zero,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
        ),
        onPressed: onTap,
        icon: Icon(icon, size: 19),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// MissingChapter
// ---------------------------------------------------------------------------

class MissingChapter extends StatelessWidget {
  const MissingChapter({super.key, required this.readerPalette});

  final ReaderPalette readerPalette;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Text(
        '\u6ca1\u6709\u53ef\u8bfb\u53d6\u7684\u7ae0\u8282',
        style: TextStyle(
          color: readerPalette.text,
          fontSize: 18,
          fontWeight: AppTextWeight.regular,
        ),
      ),
    );
  }
}
