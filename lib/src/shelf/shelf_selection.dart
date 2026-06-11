import 'dart:ui';

import 'package:flutter/material.dart';

import '../models.dart';
import '../typography.dart';

class SelectionBar extends StatelessWidget {
  const SelectionBar({
    super.key,
    required this.palette,
    required this.rangeEnabled,
    required this.undoEnabled,
    required this.groupMoveEnabled,
    required this.onSelectRange,
    required this.onUndo,
    required this.onMoveToGroup,
    required this.onMove,
    required this.onDelete,
  });

  final AppPalette palette;
  final bool rangeEnabled;
  final bool undoEnabled;
  final bool groupMoveEnabled;
  final VoidCallback onSelectRange;
  final VoidCallback onUndo;
  final VoidCallback onMoveToGroup;
  final VoidCallback onMove;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    Widget chipAction({
      required IconData icon,
      required String label,
      required Color color,
      required VoidCallback? onPressed,
    }) {
      final enabled = onPressed != null;
      final effectiveColor = enabled ? color : palette.muted;
      return Material(
        color: enabled
            ? palette.background.withValues(alpha: .72)
            : palette.line.withValues(alpha: .28),
        borderRadius: BorderRadius.circular(999),
        child: InkWell(
          borderRadius: BorderRadius.circular(999),
          onTap: onPressed,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 9),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(icon, size: 18, color: effectiveColor),
                const SizedBox(width: 6),
                Text(
                  label,
                  maxLines: 1,
                  style: TextStyle(
                    color: effectiveColor,
                    fontSize: 13.5,
                    height: 1,
                    fontWeight: AppTextWeight.medium,
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }

    Widget primaryAction({
      required IconData icon,
      required String label,
      required Color foreground,
      required Color background,
      required VoidCallback onPressed,
    }) {
      return FilledButton.icon(
        style: FilledButton.styleFrom(
          elevation: 0,
          foregroundColor: foreground,
          backgroundColor: background,
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(18),
          ),
          textStyle: const TextStyle(
            fontSize: 15,
            fontWeight: AppTextWeight.semibold,
          ),
        ),
        onPressed: onPressed,
        icon: Icon(icon, size: 20),
        label: Text(label, maxLines: 1, overflow: TextOverflow.ellipsis),
      );
    }

    return ClipRRect(
      borderRadius: BorderRadius.circular(30),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 18, sigmaY: 18),
        child: Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: palette.surface.withValues(
              alpha: palette.isLight ? .74 : .82,
            ),
            borderRadius: BorderRadius.circular(30),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: .18),
                blurRadius: 24,
                offset: const Offset(0, 10),
              ),
            ],
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  chipAction(
                    icon: Icons.select_all_rounded,
                    label: '选中区间',
                    color: palette.text,
                    onPressed: rangeEnabled ? onSelectRange : null,
                  ),
                  const SizedBox(width: 8),
                  chipAction(
                    icon: Icons.undo_rounded,
                    label: '撤回',
                    color: palette.text,
                    onPressed: undoEnabled ? onUndo : null,
                  ),
                  const SizedBox(width: 8),
                  chipAction(
                    icon: Icons.auto_awesome_motion_rounded,
                    label: '移动到分组',
                    color: palette.text,
                    onPressed: groupMoveEnabled ? onMoveToGroup : null,
                  ),
                ],
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    flex: 3,
                    child: primaryAction(
                      icon: Icons.drive_file_move_rounded,
                      label: '移动到书架',
                      foreground:
                          ThemeData.estimateBrightnessForColor(
                                palette.accentText,
                              ) ==
                              Brightness.dark
                          ? Colors.white
                          : Colors.black,
                      background: palette.accentText,
                      onPressed: onMove,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    flex: 2,
                    child: primaryAction(
                      icon: Icons.delete_rounded,
                      label: '删除',
                      foreground: palette.accentText,
                      background: palette.accentText.withValues(alpha: .13),
                      onPressed: onDelete,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
