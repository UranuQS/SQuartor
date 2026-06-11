import 'package:flutter/material.dart';

import '../app_state.dart';
import '../models.dart';
import '../typography.dart';
import 'stats_daily_detail.dart';
import 'stats_widgets.dart';

// ── Heatmap ──────────────────────────────────────────────────────────

class StatsHeatmap extends StatelessWidget {
  const StatsHeatmap({
    super.key,
    required this.state,
    required this.palette,
    required this.selectedDate,
    required this.onSelected,
  });

  final AppState state;
  final AppPalette palette;
  final DateTime selectedDate;
  final ValueChanged<DateTime> onSelected;

  @override
  Widget build(BuildContext context) {
    final today = dateOnly(DateTime.now());
    final first = today.subtract(const Duration(days: 104));
    final start = first.subtract(Duration(days: first.weekday - 1));
    final weeks = <List<DateTime>>[];
    var cursor = start;
    while (!cursor.isAfter(today)) {
      weeks.add([for (var i = 0; i < 7; i++) cursor.add(Duration(days: i))]);
      cursor = cursor.add(const Duration(days: 7));
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            WeekdayLabels(palette: palette),
            const SizedBox(width: 8),
            Expanded(
              child: SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                reverse: true,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        for (
                          var weekIndex = 0;
                          weekIndex < weeks.length;
                          weekIndex++
                        )
                          SizedBox(
                            width: 26,
                            child: Text(
                              _monthLabel(weeks, weekIndex),
                              style: TextStyle(
                                color: palette.text,
                                fontWeight: AppTextWeight.medium,
                                fontSize: 12,
                              ),
                            ),
                          ),
                      ],
                    ),
                    const SizedBox(height: 7),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        for (final week in weeks)
                          Padding(
                            padding: const EdgeInsets.only(right: 5),
                            child: Column(
                              children: [
                                for (final day in week)
                                  Padding(
                                    padding: const EdgeInsets.only(bottom: 5),
                                    child: DayCell(
                                      date: day,
                                      palette: palette,
                                      seconds: day.isAfter(today)
                                          ? null
                                          : state.readingSecondsFor(day),
                                      isToday: sameDay(day, today),
                                      isSelected: sameDay(day, selectedDate),
                                      onSelected: onSelected,
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
            ),
          ],
        ),
        const SizedBox(height: 16),
        Row(
          mainAxisAlignment: MainAxisAlignment.end,
          children: [
            Text(
              '少',
              style: TextStyle(
                color: palette.muted,
                fontWeight: AppTextWeight.regular,
              ),
            ),
            const SizedBox(width: 10),
            for (final seconds in const [0, 600, 1800, 3600, 7200]) ...[
              LegendBox(color: heatColor(seconds, palette)),
              const SizedBox(width: 8),
            ],
            Text(
              '多 (120+)',
              style: TextStyle(
                color: palette.muted,
                fontWeight: AppTextWeight.regular,
              ),
            ),
          ],
        ),
      ],
    );
  }

  String _monthLabel(List<List<DateTime>> weeks, int weekIndex) {
    final week = weeks[weekIndex];
    final firstOfMonth = week.where((day) => day.day == 1).firstOrNull;
    if (firstOfMonth == null) {
      return '';
    }
    return '${firstOfMonth.month}月';
  }
}

// ── WeekdayLabels ────────────────────────────────────────────────────

class WeekdayLabels extends StatelessWidget {
  const WeekdayLabels({super.key, required this.palette});

  final AppPalette palette;

  @override
  Widget build(BuildContext context) {
    const labels = ['周一', '', '周三', '', '周五', '', '周日'];
    return Padding(
      padding: const EdgeInsets.only(top: 25),
      child: SizedBox(
        width: 36,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            for (final label in labels)
              SizedBox(
                height: 22,
                child: Text(
                  label,
                  style: TextStyle(
                    color: palette.text,
                    fontSize: 12,
                    fontWeight: AppTextWeight.regular,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

// ── DayCell ──────────────────────────────────────────────────────────

class DayCell extends StatelessWidget {
  const DayCell({
    super.key,
    required this.date,
    required this.palette,
    required this.seconds,
    required this.isToday,
    required this.isSelected,
    required this.onSelected,
  });

  final DateTime date;
  final AppPalette palette;
  final int? seconds;
  final bool isToday;
  final bool isSelected;
  final ValueChanged<DateTime> onSelected;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: seconds == null ? null : () => onSelected(date),
      child: Container(
        width: 17,
        height: 17,
        decoration: BoxDecoration(
          color: seconds == null
              ? Colors.transparent
              : heatColor(seconds!, palette),
          borderRadius: BorderRadius.circular(4),
          border: isSelected
              ? Border.all(color: palette.primary, width: 2)
              : isToday
              ? Border.all(color: palette.text, width: 1.4)
              : null,
        ),
      ),
    );
  }
}

// ── SelectedDayDetails ───────────────────────────────────────────────

class SelectedDayDetails extends StatelessWidget {
  const SelectedDayDetails({
    super.key,
    required this.state,
    required this.palette,
    required this.selectedDate,
    required this.today,
  });

  final AppState state;
  final AppPalette palette;
  final DateTime selectedDate;
  final DateTime today;

  @override
  Widget build(BuildContext context) {
    final total = state.readingSecondsFor(selectedDate);
    final rows = [
      for (final book in state.books)
        if (state.readingSecondsFor(selectedDate, bookId: book.id) > 0)
          MapEntry(
            book,
            state.readingSecondsFor(selectedDate, bookId: book.id),
          ),
    ]..sort((left, right) => right.value.compareTo(left.value));
    final isToday = sameDay(selectedDate, today);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          isToday ? '今天 · ${dateLabel(selectedDate)}' : dateLabel(selectedDate),
          style: TextStyle(
            color: palette.text,
            fontSize: 24,
            fontWeight: AppTextWeight.semibold,
            letterSpacing: .2,
          ),
        ),
        const SizedBox(height: 16),
        StatCard(
          palette: palette,
          title: isToday ? '今日总时长' : '当天总时长',
          seconds: total,
        ),
        const SizedBox(height: 12),
        if (rows.isEmpty)
          EmptyDayCard(palette: palette, isToday: isToday)
        else
          DailyBookBreakdownCard(
            palette: palette,
            rows: rows,
            totalSeconds: total,
          ),
      ],
    );
  }
}

// ── EmptyDayCard ─────────────────────────────────────────────────────

class EmptyDayCard extends StatelessWidget {
  const EmptyDayCard({super.key, required this.palette, required this.isToday});

  final AppPalette palette;
  final bool isToday;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: detailDecoration(palette),
      child: Text(
        isToday ? '今天还没有阅读记录。' : '这一天没有阅读记录。',
        style: TextStyle(
          color: palette.muted,
          fontSize: 15,
          fontWeight: AppTextWeight.medium,
        ),
      ),
    );
  }
}

// ── LegendBox ────────────────────────────────────────────────────────

class LegendBox extends StatelessWidget {
  const LegendBox({super.key, required this.color});

  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 17,
      height: 17,
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(4),
      ),
    );
  }
}
