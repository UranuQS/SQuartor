import 'package:flutter/material.dart';

import '../models.dart';
import '../typography.dart';

// ── Shared utility functions ────────────────────────────────────────

String dateLabel(DateTime date) {
  String two(int value) => value.toString().padLeft(2, '0');
  return '${date.year}-${two(date.month)}-${two(date.day)}';
}

String durationLabel(int seconds) {
  final hours = seconds ~/ 3600;
  final minutes = (seconds % 3600) ~/ 60;
  if (hours > 0) {
    return '$hours 小时 $minutes 分钟';
  }
  if (minutes > 0) {
    return '$minutes 分钟';
  }
  return '$seconds 秒';
}

Color heatColor(int seconds, AppPalette palette) {
  if (seconds <= 0) {
    return palette.background.computeLuminance() > .5
        ? const Color(0xFFE3E6EC)
        : palette.line.withValues(alpha: .45);
  }
  if (seconds < 15 * 60) {
    return palette.primarySoft.withValues(alpha: .38);
  }
  if (seconds < 30 * 60) {
    return palette.primarySoft.withValues(alpha: .58);
  }
  if (seconds < 60 * 60) {
    return palette.primarySoft.withValues(alpha: .78);
  }
  return palette.primary;
}

DateTime dateOnly(DateTime date) => DateTime(date.year, date.month, date.day);

bool sameDay(DateTime a, DateTime b) {
  return a.year == b.year && a.month == b.month && a.day == b.day;
}

BoxDecoration detailDecoration(AppPalette palette) {
  return BoxDecoration(
    color: palette.card,
    borderRadius: BorderRadius.circular(24),
  );
}

String durationFormat(int seconds) {
  final hours = seconds ~/ 3600;
  final minutes = (seconds % 3600) ~/ 60;
  if (hours > 0) {
    return '$hours小时 $minutes分钟';
  }
  return '$minutes分钟';
}

String clockFormat(int seconds) {
  final hours = seconds ~/ 3600;
  final minutes = (seconds % 3600) ~/ 60;
  final secs = seconds % 60;
  if (hours > 0) {
    return '$hours:${minutes.toString().padLeft(2, '0')}:${secs.toString().padLeft(2, '0')}';
  }
  return '$minutes:${secs.toString().padLeft(2, '0')}';
}

String monthDay(DateTime date) => '${date.month}/${date.day}';

// ── StatCard ─────────────────────────────────────────────────────────

class StatCard extends StatelessWidget {
  const StatCard({
    super.key,
    required this.palette,
    required this.seconds,
    this.title = '阅读时长',
  });

  final AppPalette palette;
  final int seconds;
  final String title;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 18),
      decoration: BoxDecoration(
        color: palette.card,
        borderRadius: BorderRadius.circular(22),
      ),
      child: Row(
        children: [
          Icon(Icons.access_time_rounded, color: palette.primarySoft, size: 26),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              title,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                color: palette.text,
                fontSize: 16,
                fontWeight: AppTextWeight.regular,
              ),
            ),
          ),
          const SizedBox(width: 12),
          Text(
            seconds == 0 ? '0分钟' : durationLabel(seconds),
            style: TextStyle(
              color: seconds == 0 ? palette.subtle : palette.primary,
              fontSize: 16,
              fontWeight: AppTextWeight.medium,
            ),
          ),
        ],
      ),
    );
  }
}
