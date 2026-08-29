import 'dart:async';

import 'package:flutter/material.dart';

import '../models.dart';
import '../typography.dart';

class ReaderFooter extends StatelessWidget {
  const ReaderFooter({
    super.key,
    required this.chapter,
    required this.chapterCount,
    required this.page,
    required this.pageCount,
    required this.progress,
    required this.palette,
  });

  final int chapter;
  final int chapterCount;
  final int page;
  final int pageCount;
  final double progress;
  final ReaderPalette palette;

  @override
  Widget build(BuildContext context) {
    final isDark =
        ThemeData.estimateBrightnessForColor(palette.background) ==
        Brightness.dark;
    final textStyle = TextStyle(
      color: palette.muted.withValues(alpha: .82),
      fontSize: 12,
      fontWeight: AppTextWeight.medium,
    );
    return Row(
      children: [
        ReaderFooterClockChip(
          icon: isDark ? Icons.dark_mode_rounded : Icons.wb_sunny_rounded,
          palette: palette,
        ),
        const Spacer(),
        ReaderFooterChip(
          icon: Icons.bookmark_border_rounded,
          label: '$page/$pageCount',
          palette: palette,
        ),
        const SizedBox(width: 8),
        ReaderFooterChip(
          icon: Icons.menu_book_rounded,
          label: '$chapter/$chapterCount',
          palette: palette,
        ),
        Offstage(
          child: Text(
            '\u7b2c $chapter / $chapterCount \u7ae0  \u00b7  \u672c\u7ae0 $page / $pageCount \u9875  \u00b7  ${(progress * 100).toStringAsFixed(1)}%',
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            textAlign: TextAlign.right,
            style: textStyle,
          ),
        ),
      ],
    );
  }
}

class ReaderFooterClockChip extends StatefulWidget {
  const ReaderFooterClockChip({
    super.key,
    required this.icon,
    required this.palette,
  });

  final IconData icon;
  final ReaderPalette palette;

  @override
  State<ReaderFooterClockChip> createState() => _ReaderFooterClockChipState();
}

class _ReaderFooterClockChipState extends State<ReaderFooterClockChip> {
  late DateTime _now;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _now = DateTime.now();
    _scheduleNextTick();
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  void _scheduleNextTick() {
    _timer?.cancel();
    final nextMinute = DateTime(
      _now.year,
      _now.month,
      _now.day,
      _now.hour,
      _now.minute + 1,
    );
    final delayMs = nextMinute
        .difference(DateTime.now())
        .inMilliseconds
        .clamp(250, const Duration(minutes: 1).inMilliseconds);
    final delay = Duration(milliseconds: delayMs);
    _timer = Timer(delay, () {
      if (!mounted) {
        return;
      }
      setState(() => _now = DateTime.now());
      _scheduleNextTick();
    });
  }

  @override
  Widget build(BuildContext context) {
    final time =
        '${_now.hour.toString().padLeft(2, '0')}:${_now.minute.toString().padLeft(2, '0')}';
    return ReaderFooterChip(
      icon: widget.icon,
      label: time,
      palette: widget.palette,
    );
  }
}

class ReaderFooterChip extends StatelessWidget {
  const ReaderFooterChip({
    super.key,
    required this.icon,
    required this.label,
    required this.palette,
  });

  final IconData icon;
  final String label;
  final ReaderPalette palette;

  @override
  Widget build(BuildContext context) {
    final color = palette.muted.withValues(alpha: .86);
    return DecoratedBox(
      decoration: BoxDecoration(
        color: palette.text.withValues(alpha: .045),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 13, color: color),
            const SizedBox(width: 4),
            Text(
              label,
              maxLines: 1,
              style: TextStyle(
                color: color,
                fontSize: 11,
                height: 1,
                fontWeight: AppTextWeight.medium,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
