import 'package:flutter/material.dart';

import '../app_state.dart';
import '../models.dart';
import 'settings_widgets.dart';

typedef DetailBuilder =
    List<Widget> Function(BuildContext, AppPalette, ReadingStyle);

class SettingsDetailPage extends StatelessWidget {
  const SettingsDetailPage({
    super.key,
    required this.state,
    required this.title,
    required this.builder,
  });

  final AppState state;
  final String title;
  final DetailBuilder builder;

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
                    Text(title, style: settingsTitleStyle(palette)),
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
