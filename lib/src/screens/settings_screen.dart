import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../app_state.dart';
import '../models.dart';
import '../typography.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key, required this.state});

  final AppState state;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: state.settingsChanges,
      builder: (context, _) {
        final palette = state.palette;
        return ListView(
          padding: const EdgeInsets.fromLTRB(20, 30, 20, 112),
          children: [
            Row(
              children: [
                Icon(Icons.settings_rounded, color: palette.text, size: 32),
                const SizedBox(width: 14),
                Text('设置', style: _titleStyle(palette)),
              ],
            ),
            const SizedBox(height: 28),
            _SettingsGroup(
              palette: palette,
              title: '阅读',
              entries: [
                _SettingEntry(
                  icon: Icons.format_paint_rounded,
                  title: '主题与纸张',
                  subtitle: '设置应用主题和阅读器纸张',
                  palette: palette,
                  onTap: () => _openThemePage(context),
                ),
                _SettingEntry(
                  icon: Icons.text_fields_rounded,
                  title: '文本格式化',
                  subtitle: '字号、行高、段距和页边距',
                  palette: palette,
                  onTap: () => _openLayoutPage(context),
                ),
                _SettingEntry(
                  icon: Icons.font_download_rounded,
                  title: '字体',
                  subtitle:
                      '应用：${state.style.appFontName ?? '系统'} · 书籍：${state.style.fontName ?? '系统'}',
                  palette: palette,
                  onTap: () => _openFontPage(context),
                ),
              ],
            ),
          ],
        );
      },
    );
  }

  void _openThemePage(BuildContext context) {
    _pushDetail(
      context,
      '主题与纸张',
      (context, palette, style) => [
        _Card(
          palette: palette,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _SectionTitle('浅色 / 深色', palette),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  _ChoicePill(
                    label: '跟随系统',
                    selected: style.brightnessMode == AppBrightnessMode.system,
                    palette: palette,
                    onTap: () => state.updateStyle(
                      style.copyWith(brightnessMode: AppBrightnessMode.system),
                      immediate: true,
                    ),
                  ),
                  _ChoicePill(
                    label: '浅色',
                    selected: style.brightnessMode == AppBrightnessMode.light,
                    palette: palette,
                    onTap: () => state.updateStyle(
                      style.copyWith(brightnessMode: AppBrightnessMode.light),
                      immediate: true,
                    ),
                  ),
                  _ChoicePill(
                    label: '深色',
                    selected: style.brightnessMode == AppBrightnessMode.dark,
                    palette: palette,
                    onTap: () => state.updateStyle(
                      style.copyWith(brightnessMode: AppBrightnessMode.dark),
                      immediate: true,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
        _Card(
          palette: palette,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _SectionTitle('应用主题色', palette),
              const SizedBox(height: 10),
              Text(
                '当前：${style.themeLabel}',
                style: TextStyle(color: palette.muted, height: 1.5),
              ),
              const SizedBox(height: 14),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  for (final item in themeSeeds.values)
                    _ChoicePill(
                      label: item.label,
                      selected:
                          style.customThemeColorValue == null &&
                          style.appTheme == item.id,
                      palette: palette,
                      color: item.color,
                      onTap: () => state.updateStyle(
                        style.copyWith(
                          appTheme: item.id,
                          clearCustomThemeColor: true,
                        ),
                        immediate: true,
                      ),
                    ),
                  _ChoicePill(
                    label: '自定义色号',
                    selected: style.customThemeColorValue != null,
                    palette: palette,
                    color: style.themeSeedColor,
                    onTap: () => _showColorDialog(context, state),
                  ),
                ],
              ),
            ],
          ),
        ),
        _Card(
          palette: palette,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _SectionTitle('独立阅读背景', palette),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  for (final item in readerPalettes.values)
                    _ChoicePill(
                      label: item.label,
                      selected: style.readerBackground == item.id,
                      palette: palette,
                      color: item.background,
                      onTap: () => state.updateStyle(
                        style.copyWith(readerBackground: item.id),
                      ),
                    ),
                ],
              ),
            ],
          ),
        ),
      ],
    );
  }

  void _openLayoutPage(BuildContext context) {
    _pushDetail(
      context,
      '文本格式化',
      (_, palette, style) => [
        _Card(
          palette: palette,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _SectionTitle('阅读排版', palette),
              const SizedBox(height: 8),
              _SliderRow(
                label: '字号',
                value: style.fontSize,
                min: 14,
                max: 72,
                divisions: 58,
                display: style.fontSize.toStringAsFixed(0),
                palette: palette,
                onChanged: (value) =>
                    state.updateStyle(style.copyWith(fontSize: value)),
              ),
              _SliderRow(
                label: '行高',
                value: style.lineHeight,
                min: 1.2,
                max: 2.6,
                divisions: 14,
                display: style.lineHeight.toStringAsFixed(1),
                palette: palette,
                onChanged: (value) =>
                    state.updateStyle(style.copyWith(lineHeight: value)),
              ),
              _SliderRow(
                label: '段距',
                value: style.paragraphSpacing,
                min: 0,
                max: 30,
                divisions: 15,
                display: style.paragraphSpacing.toStringAsFixed(0),
                palette: palette,
                onChanged: (value) =>
                    state.updateStyle(style.copyWith(paragraphSpacing: value)),
              ),
              _SliderRow(
                label: '字距',
                value: style.letterSpacing,
                min: 0,
                max: 2,
                divisions: 20,
                display: style.letterSpacing.toStringAsFixed(1),
                palette: palette,
                onChanged: (value) =>
                    state.updateStyle(style.copyWith(letterSpacing: value)),
              ),
              _SliderRow(
                label: '左右边距',
                value: style.pageMargin,
                min: 12,
                max: 52,
                divisions: 20,
                display: style.pageMargin.toStringAsFixed(0),
                palette: palette,
                onChanged: (value) =>
                    state.updateStyle(style.copyWith(pageMargin: value)),
              ),
              _SliderRow(
                label: '上下边距',
                value: style.verticalMargin,
                min: 24,
                max: 96,
                divisions: 18,
                display: style.verticalMargin.toStringAsFixed(0),
                palette: palette,
                onChanged: (value) =>
                    state.updateStyle(style.copyWith(verticalMargin: value)),
              ),
            ],
          ),
        ),
      ],
    );
  }

  void _openFontPage(BuildContext context) {
    _pushDetail(
      context,
      '字体',
      (_, palette, style) => [
        _Card(
          palette: palette,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(child: _SectionTitle('应用字体', palette)),
                  FilledButton.icon(
                    style: FilledButton.styleFrom(
                      backgroundColor: palette.primary,
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16),
                      ),
                    ),
                    onPressed: state.importAppFont,
                    icon: const Icon(Icons.upload_file_rounded, size: 19),
                    label: const Text('导入'),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                style.appFontName == null
                    ? '应用界面当前使用系统字体'
                    : '应用界面当前字体：${style.appFontName}',
                style: TextStyle(color: palette.muted, height: 1.5),
              ),
              const SizedBox(height: 14),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  _ChoicePill(
                    label: '系统字体',
                    selected: style.appFontPath == null,
                    palette: palette,
                    onTap: () =>
                        state.updateStyle(style.copyWith(clearAppFont: true)),
                  ),
                  for (final font in state.fonts)
                    _ChoicePill(
                      label: font.name,
                      selected: style.appFontPath == font.path,
                      palette: palette,
                      onTap: () => state.updateStyle(
                        style.copyWith(
                          appFontName: font.name,
                          appFontPath: font.path,
                        ),
                      ),
                    ),
                ],
              ),
            ],
          ),
        ),
        _Card(
          palette: palette,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(child: _SectionTitle('书籍字体', palette)),
                  FilledButton.icon(
                    style: FilledButton.styleFrom(
                      backgroundColor: palette.primary,
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16),
                      ),
                    ),
                    onPressed: state.importReaderFont,
                    icon: const Icon(Icons.upload_file_rounded, size: 19),
                    label: const Text('导入'),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                style.fontName == null
                    ? '书籍正文当前使用系统字体'
                    : '书籍正文当前字体：${style.fontName}',
                style: TextStyle(color: palette.muted, height: 1.5),
              ),
              const SizedBox(height: 14),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  _ChoicePill(
                    label: '系统字体',
                    selected: style.fontPath == null,
                    palette: palette,
                    onTap: () =>
                        state.updateStyle(style.copyWith(clearFont: true)),
                  ),
                  for (final font in state.fonts)
                    _ChoicePill(
                      label: font.name,
                      selected: style.fontPath == font.path,
                      palette: palette,
                      onTap: () => state.updateStyle(
                        style.copyWith(
                          fontName: font.name,
                          fontPath: font.path,
                        ),
                      ),
                    ),
                ],
              ),
            ],
          ),
        ),
      ],
    );
  }

  void _pushDetail(BuildContext context, String title, _DetailBuilder builder) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) =>
            _SettingsDetailPage(state: state, title: title, builder: builder),
      ),
    );
  }

  Future<void> _showColorDialog(BuildContext context, AppState state) async {
    final palette = state.palette;
    final controller = TextEditingController(
      text: themeHex(state.style.themeSeedColor),
    );
    int? selected = _parseHex(controller.text);
    await showDialog<void>(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) {
          final color = selected == null ? palette.primary : Color(selected!);
          return AlertDialog(
            backgroundColor: palette.surface,
            title: Text('自定义主题色', style: TextStyle(color: palette.text)),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '输入十六进制色号，例如 #B00046。',
                  style: TextStyle(color: palette.muted),
                ),
                const SizedBox(height: 14),
                TextField(
                  controller: controller,
                  autofocus: true,
                  style: TextStyle(
                    color: palette.text,
                    fontWeight: AppTextWeight.regular,
                  ),
                  decoration: InputDecoration(
                    prefixIcon: Padding(
                      padding: const EdgeInsets.all(12),
                      child: Container(
                        width: 22,
                        height: 22,
                        decoration: BoxDecoration(
                          color: color,
                          shape: BoxShape.circle,
                        ),
                      ),
                    ),
                    errorText: selected == null ? '格式应为 #RRGGBB' : null,
                    filled: true,
                    fillColor: palette.card,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                  ),
                  onChanged: (value) =>
                      setDialogState(() => selected = _parseHex(value)),
                ),
              ],
            ),
            actions: [
              TextButton(
                onPressed: () {
                  state.updateStyle(
                    state.style.copyWith(clearCustomThemeColor: true),
                    immediate: true,
                  );
                  Navigator.pop(context);
                },
                child: const Text('恢复默认'),
              ),
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('取消'),
              ),
              FilledButton(
                onPressed: selected == null
                    ? null
                    : () {
                        state.updateStyle(
                          state.style.copyWith(customThemeColorValue: selected),
                          immediate: true,
                        );
                        Navigator.pop(context);
                      },
                child: const Text('应用'),
              ),
            ],
          );
        },
      ),
    );
    controller.dispose();
  }

  int? _parseHex(String raw) {
    final value = raw.trim().replaceFirst('#', '');
    if (!RegExp(r'^[0-9a-fA-F]{6}$').hasMatch(value)) {
      return null;
    }
    return 0xFF000000 | int.parse(value, radix: 16);
  }
}

typedef _DetailBuilder =
    List<Widget> Function(BuildContext, AppPalette, ReadingStyle);

class _SettingsDetailPage extends StatelessWidget {
  const _SettingsDetailPage({
    required this.state,
    required this.title,
    required this.builder,
  });

  final AppState state;
  final String title;
  final _DetailBuilder builder;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: state.settingsChanges,
      builder: (context, _) {
        final palette = state.palette;
        return Scaffold(
          backgroundColor: palette.background,
          body: SafeArea(
            bottom: false,
            child: ListView(
              padding: const EdgeInsets.fromLTRB(18, 26, 18, 28),
              children: [
                Row(
                  children: [
                    IconButton(
                      onPressed: () => Navigator.pop(context),
                      icon: Icon(Icons.arrow_back_rounded, color: palette.text),
                    ),
                    const SizedBox(width: 4),
                    Text(title, style: _titleStyle(palette)),
                  ],
                ),
                const SizedBox(height: 22),
                for (final child in builder(context, palette, state.style)) ...[
                  child,
                  const SizedBox(height: 16),
                ],
              ],
            ),
          ),
        );
      },
    );
  }
}

class _SettingsGroup extends StatelessWidget {
  const _SettingsGroup({
    required this.palette,
    required this.title,
    required this.entries,
  });

  final AppPalette palette;
  final String title;
  final List<Widget> entries;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(left: 10, bottom: 12),
          child: Text(
            title,
            style: TextStyle(
              color: palette.muted,
              fontSize: 16,
              fontWeight: AppTextWeight.medium,
            ),
          ),
        ),
        ClipRRect(
          borderRadius: BorderRadius.circular(22),
          child: DecoratedBox(
            decoration: BoxDecoration(
              color: palette.card,
              border: Border.all(color: palette.line.withValues(alpha: .55)),
              borderRadius: BorderRadius.circular(22),
            ),
            child: Column(children: entries),
          ),
        ),
      ],
    );
  }
}

class _SettingEntry extends StatelessWidget {
  const _SettingEntry({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.palette,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final AppPalette palette;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () {
        HapticFeedback.selectionClick();
        onTap();
      },
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 13),
        child: Row(
          children: [
            Container(
              width: 42,
              height: 42,
              decoration: BoxDecoration(
                color: palette.cardAlt,
                borderRadius: BorderRadius.circular(14),
              ),
              child: Icon(icon, color: palette.muted, size: 24),
            ),
            const SizedBox(width: 13),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: TextStyle(
                      color: palette.text,
                      fontSize: 18,
                      fontWeight: AppTextWeight.semibold,
                    ),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    subtitle,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      color: palette.muted,
                      fontSize: 14,
                      height: 1.35,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            Icon(Icons.chevron_right_rounded, color: palette.subtle, size: 24),
          ],
        ),
      ),
    );
  }
}

TextStyle _titleStyle(AppPalette palette) {
  return TextStyle(
    color: palette.text,
    fontSize: 32,
    fontWeight: AppTextWeight.semibold,
    letterSpacing: -1.2,
  );
}

class _Card extends StatelessWidget {
  const _Card({required this.palette, required this.child});

  final AppPalette palette;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: palette.card,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: palette.line.withValues(alpha: .55)),
      ),
      child: child,
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle(this.text, this.palette);

  final String text;
  final AppPalette palette;

  @override
  Widget build(BuildContext context) {
    return Text(
      text,
      style: TextStyle(
        color: palette.text,
        fontSize: 18,
        fontWeight: AppTextWeight.semibold,
      ),
    );
  }
}

class _ChoicePill extends StatelessWidget {
  const _ChoicePill({
    required this.label,
    required this.selected,
    required this.palette,
    required this.onTap,
    this.color,
  });

  final String label;
  final bool selected;
  final AppPalette palette;
  final VoidCallback onTap;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(999),
      onTap: () {
        HapticFeedback.selectionClick();
        onTap();
      },
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 9),
        decoration: BoxDecoration(
          color: selected ? palette.primary : palette.cardAlt,
          borderRadius: BorderRadius.circular(999),
          border: Border.all(
            color: selected ? palette.primarySoft : palette.line,
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (color != null) ...[
              Container(
                width: 13,
                height: 13,
                decoration: BoxDecoration(
                  color: color,
                  shape: BoxShape.circle,
                  border: Border.all(
                    color: Colors.white.withValues(alpha: .25),
                  ),
                ),
              ),
              const SizedBox(width: 8),
            ],
            Text(
              label,
              style: TextStyle(
                color: selected ? Colors.white : palette.muted,
                fontSize: 13,
                fontWeight: AppTextWeight.regular,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SliderRow extends StatelessWidget {
  const _SliderRow({
    required this.label,
    required this.value,
    required this.min,
    required this.max,
    required this.divisions,
    required this.display,
    required this.palette,
    required this.onChanged,
  });

  final String label;
  final double value;
  final double min;
  final double max;
  final int divisions;
  final String display;
  final AppPalette palette;
  final ValueChanged<double> onChanged;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 12),
      child: Row(
        children: [
          SizedBox(
            width: 58,
            child: Text(
              label,
              style: TextStyle(
                color: palette.muted,
                fontSize: 13,
                fontWeight: AppTextWeight.regular,
              ),
            ),
          ),
          Expanded(
            child: SliderTheme(
              data: SliderTheme.of(context).copyWith(
                trackHeight: 6,
                activeTrackColor: palette.primary,
                inactiveTrackColor: palette.line.withValues(alpha: .55),
                thumbColor: palette.primarySoft,
                overlayColor: palette.primary.withValues(alpha: .14),
                thumbShape: const RoundSliderThumbShape(enabledThumbRadius: 9),
                overlayShape: const RoundSliderOverlayShape(overlayRadius: 16),
                tickMarkShape: SliderTickMarkShape.noTickMark,
              ),
              child: Slider(
                value: value,
                min: min,
                max: max,
                divisions: divisions,
                onChanged: onChanged,
              ),
            ),
          ),
          SizedBox(
            width: 36,
            child: Text(
              display,
              textAlign: TextAlign.right,
              style: TextStyle(
                color: palette.text,
                fontSize: 13,
                fontWeight: AppTextWeight.medium,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
