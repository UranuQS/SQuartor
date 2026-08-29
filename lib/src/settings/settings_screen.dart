import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

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
                  iconColor: const Color(0xFFE89B71),
                  title: '主题与纸张',
                  subtitle: '设置应用主题和阅读器纸张',
                  palette: palette,
                  onTap: () => _openThemePage(context),
                ),
                SettingEntry(
                  icon: Icons.font_download_rounded,
                  iconColor: const Color(0xFF7388C1),
                  title: '字体',
                  subtitle:
                      '应用：${state.style.appFontName ?? '系统'} · 书籍：${state.style.fontName ?? '系统'}',
                  palette: palette,
                  onTap: () => _openFontPage(context),
                ),
                SettingEntry(
                  icon: Icons.format_size_rounded,
                  iconColor: const Color(0xFF8E7CC3),
                  title: '文本格式化',
                  subtitle: '调整字号、行高、段距、字距和页边距',
                  palette: palette,
                  onTap: () => _openLayoutPage(context),
                ),
                SettingEntry(
                  icon: Icons.cloud_sync_rounded,
                  iconColor: const Color(0xFF5CA4A9),
                  title: '云同步',
                  subtitle: state.cloudSyncSettings.configured
                      ? 'WebDAV 已配置，可同步阅读进度和应用设置'
                      : '用 WebDAV 同步阅读进度、书签和应用设置',
                  palette: palette,
                  onTap: () => _openCloudSyncPage(context),
                ),
              ],
            ),
            const SizedBox(height: 26),
            SettingsGroup(
              palette: palette,
              title: '关于',
              entries: [
                SettingEntry(
                  icon: Icons.info_outline_rounded,
                  title: 'SQuartor',
                  subtitle: 'com.squartor.reader · 1.0.0+1 release',
                  palette: palette,
                  onTap: () => _showAppInfo(context),
                ),
                SettingEntry(
                  icon: Icons.archive_outlined,
                  title: 'GitHub 仓库',
                  subtitle: '给本项目点颗 star，帮助更多人发现',
                  palette: palette,
                  onTap: () => _openGithub(context),
                ),
              ],
            ),
          ],
        );
      },
    );
  }

  void _showAppInfo(BuildContext context) {
    showAboutDialog(
      context: context,
      applicationName: 'SQuartor',
      applicationVersion: '1.0.0+1 release',
      applicationIcon: const Icon(Icons.auto_stories_rounded, size: 42),
      applicationLegalese: '本地优先的轻小说阅读器。',
    );
  }

  void _showComingSoon(BuildContext context, String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), behavior: SnackBarBehavior.floating),
    );
  }

  void _openGithub(BuildContext context) {
    final uri = Uri.parse('https://github.com/UranuQS/SQuartor');
    launchUrl(uri, mode: LaunchMode.externalApplication).then((opened) {
      if (!opened && context.mounted) {
        _showComingSoon(context, '没有找到可打开 GitHub 链接的应用。');
      }
    });
  }

  void _openCloudSyncPage(BuildContext context) {
    Navigator.of(context).push(
      CupertinoPageRoute<void>(
        builder: (context) => _CloudSyncPage(state: state),
      ),
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
      CupertinoPageRoute<void>(
        builder: (context) =>
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

class _CloudSyncPage extends StatefulWidget {
  const _CloudSyncPage({required this.state});

  final AppState state;

  @override
  State<_CloudSyncPage> createState() => _CloudSyncPageState();
}

class _CloudSyncPageState extends State<_CloudSyncPage> {
  late final TextEditingController _endpointController;
  late final TextEditingController _usernameController;
  late final TextEditingController _passwordController;
  var _busy = false;

  @override
  void initState() {
    super.initState();
    final settings = widget.state.cloudSyncSettings;
    _endpointController = TextEditingController(text: settings.endpoint);
    _usernameController = TextEditingController(text: settings.username);
    _passwordController = TextEditingController(text: settings.password);
  }

  @override
  void dispose() {
    _endpointController.dispose();
    _usernameController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: widget.state.settingsChanges,
      builder: (context, _) {
        final palette = widget.state.palette;
        final settings = widget.state.cloudSyncSettings;
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
                      onPressed: _busy ? null : () => Navigator.pop(context),
                      icon: Icon(Icons.arrow_back_rounded, color: palette.text),
                    ),
                    const SizedBox(width: 4),
                    Text('云同步', style: settingsTitleStyle(palette)),
                  ],
                ),
                const SizedBox(height: 22),
                SettingsCard(
                  palette: palette,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      SectionTitle('WebDAV', palette),
                      const SizedBox(height: 10),
                      Text(
                        '同步阅读进度、书签、阅读统计、书架归属和应用设置，不上传书籍正文。',
                        style: TextStyle(color: palette.muted, height: 1.5),
                      ),
                      const SizedBox(height: 16),
                      _CloudTextField(
                        controller: _endpointController,
                        palette: palette,
                        label: 'WebDAV 地址',
                        hint: 'https://dav.jianguoyun.com/dav/',
                        keyboardType: TextInputType.url,
                      ),
                      const SizedBox(height: 6),
                      Text(
                        '会自动创建/使用 SQuartor/squartor-sync-v1.json。',
                        style: TextStyle(
                          color: palette.subtle,
                          height: 1.35,
                          fontSize: 12,
                        ),
                      ),
                      const SizedBox(height: 12),
                      _CloudTextField(
                        controller: _usernameController,
                        palette: palette,
                        label: '账号',
                      ),
                      const SizedBox(height: 12),
                      _CloudTextField(
                        controller: _passwordController,
                        palette: palette,
                        label: '密码',
                        obscureText: true,
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 16),
                SettingsCard(
                  palette: palette,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      SectionTitle('操作', palette),
                      const SizedBox(height: 12),
                      FilledButton.icon(
                        onPressed: _busy ? null : _save,
                        icon: const Icon(Icons.save_rounded),
                        label: const Text('保存配置'),
                      ),
                      const SizedBox(height: 10),
                      OutlinedButton.icon(
                        onPressed: _busy
                            ? null
                            : () => _run(
                                action: widget.state.uploadCloudSync,
                                success: '已上传本机同步数据',
                              ),
                        icon: const Icon(Icons.cloud_upload_rounded),
                        label: const Text('上传本机数据'),
                      ),
                      const SizedBox(height: 10),
                      OutlinedButton.icon(
                        onPressed: _busy
                            ? null
                            : () => _run(
                                action: widget.state.downloadCloudSync,
                                success: '已下载并合并远端数据',
                              ),
                        icon: const Icon(Icons.cloud_download_rounded),
                        label: const Text('下载远端数据'),
                      ),
                      if (_busy) ...[
                        const SizedBox(height: 16),
                        LinearProgressIndicator(color: palette.primary),
                      ],
                    ],
                  ),
                ),
                const SizedBox(height: 16),
                Text(
                  [
                    if (settings.lastUploadAt != null)
                      '上次上传：${_timeLabel(settings.lastUploadAt!)}',
                    if (settings.lastDownloadAt != null)
                      '上次下载：${_timeLabel(settings.lastDownloadAt!)}',
                    if (settings.lastUploadAt == null &&
                        settings.lastDownloadAt == null)
                      '还没有同步记录',
                  ].join('\n'),
                  style: TextStyle(color: palette.muted, height: 1.45),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Future<void> _save() async {
    try {
      await widget.state.saveCloudSyncSettings(_settingsFromForm());
      if (mounted) {
        _message('配置已保存');
      }
    } catch (error) {
      if (mounted) {
        _message(error.toString());
      }
    }
  }

  Future<void> _run({
    required Future<void> Function() action,
    required String success,
  }) async {
    setState(() => _busy = true);
    try {
      await widget.state.saveCloudSyncSettings(_settingsFromForm());
      await action();
      if (mounted) {
        _message(success);
      }
    } catch (error) {
      if (mounted) {
        _message(error.toString());
      }
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }
  }

  CloudSyncSettings _settingsFromForm() {
    final endpoint = _endpointController.text.trim();
    final username = _usernameController.text.trim();
    final password = _passwordController.text;
    final current = widget.state.cloudSyncSettings;
    return current.copyWith(
      enabled:
          endpoint.isNotEmpty && username.isNotEmpty && password.isNotEmpty,
      endpoint: endpoint,
      username: username,
      password: password,
    );
  }

  void _message(String message) {
    final messenger = ScaffoldMessenger.of(context);
    messenger.hideCurrentSnackBar();
    messenger.showSnackBar(
      SnackBar(content: Text(message), behavior: SnackBarBehavior.floating),
    );
  }

  String _timeLabel(DateTime time) {
    String two(int value) => value.toString().padLeft(2, '0');
    return '${time.year}-${two(time.month)}-${two(time.day)} '
        '${two(time.hour)}:${two(time.minute)}';
  }
}

class _CloudTextField extends StatelessWidget {
  const _CloudTextField({
    required this.controller,
    required this.palette,
    required this.label,
    this.hint,
    this.obscureText = false,
    this.keyboardType,
  });

  final TextEditingController controller;
  final AppPalette palette;
  final String label;
  final String? hint;
  final bool obscureText;
  final TextInputType? keyboardType;

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      obscureText: obscureText,
      keyboardType: keyboardType,
      style: TextStyle(color: palette.text),
      decoration: InputDecoration(
        labelText: label,
        hintText: hint,
        labelStyle: TextStyle(color: palette.muted),
        hintStyle: TextStyle(color: palette.subtle),
        filled: true,
        fillColor: palette.cardAlt.withValues(
          alpha: palette.isLight ? .55 : .5,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(color: palette.line.withValues(alpha: .35)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(color: palette.line.withValues(alpha: .26)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(color: palette.primarySoft, width: 1.2),
        ),
      ),
    );
  }
}
