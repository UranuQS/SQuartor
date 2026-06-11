import 'package:flutter/material.dart';

import '../app_state.dart';
import '../models.dart';
import '../typography.dart';
import 'settings_detail.dart';
import 'settings_widgets.dart';

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
                Text('设置', style: settingsTitleStyle(palette)),
              ],
            ),
            const SizedBox(height: 28),
            SettingsGroup(
              palette: palette,
              title: '阅读',
              entries: [
                SettingEntry(
                  icon: Icons.format_paint_rounded,
                  title: '主题与纸张',
                  subtitle: '设置应用主题和阅读器纸张',
                  palette: palette,
                  onTap: () => _openThemePage(context),
                ),
                SettingEntry(
                  icon: Icons.text_fields_rounded,
                  title: '文本格式化',
                  subtitle: '字号、行高、段距和页边距',
                  palette: palette,
                  onTap: () => _openLayoutPage(context),
                ),
                SettingEntry(
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
        SettingsCard(
          palette: palette,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SectionTitle('浅色 / 深色', palette),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  ChoicePill(
                    label: '跟随系统',
                    selected: style.brightnessMode == AppBrightnessMode.system,
                    palette: palette,
                    onTap: () => state.updateStyle(
                      style.copyWith(brightnessMode: AppBrightnessMode.system),
                      immediate: true,
                    ),
                  ),
                  ChoicePill(
                    label: '浅色',
                    selected: style.brightnessMode == AppBrightnessMode.light,
                    palette: palette,
                    onTap: () => state.updateStyle(
                      style.copyWith(brightnessMode: AppBrightnessMode.light),
                      immediate: true,
                    ),
                  ),
                  ChoicePill(
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
        SettingsCard(
          palette: palette,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SectionTitle('应用主题色', palette),
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
                    ChoicePill(
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
                  ChoicePill(
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
        SettingsCard(
          palette: palette,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SectionTitle('独立阅读背景', palette),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  for (final item in readerPalettes.values.where(
                    (item) => item.id != ReaderBackgroundId.green,
                  ))
                    ChoicePill(
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
        SettingsCard(
          palette: palette,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SectionTitle('阅读排版', palette),
              const SizedBox(height: 8),
              SliderRow(
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
              SliderRow(
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
              SliderRow(
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
              SliderRow(
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
              SliderRow(
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
              SliderRow(
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
        SettingsCard(
          palette: palette,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(child: SectionTitle('应用字体', palette)),
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
                  ChoicePill(
                    label: '系统字体',
                    selected: style.appFontPath == null,
                    palette: palette,
                    onTap: () =>
                        state.updateStyle(style.copyWith(clearAppFont: true)),
                  ),
                  for (final font in state.fonts)
                    ChoicePill(
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
        SettingsCard(
          palette: palette,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(child: SectionTitle('书籍字体', palette)),
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
                  ChoicePill(
                    label: '系统字体',
                    selected: style.fontPath == null,
                    palette: palette,
                    onTap: () =>
                        state.updateStyle(style.copyWith(clearFont: true)),
                  ),
                  for (final font in state.fonts)
                    ChoicePill(
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

  void _pushDetail(BuildContext context, String title, DetailBuilder builder) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) =>
            SettingsDetailPage(state: state, title: title, builder: builder),
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
