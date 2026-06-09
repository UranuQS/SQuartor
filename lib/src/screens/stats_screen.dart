import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../app_state.dart';
import '../models.dart';
import '../typography.dart';
import '../widgets/book_cover.dart';

class StatsScreen extends StatefulWidget {
  const StatsScreen({super.key, required this.state});

  final AppState state;

  @override
  State<StatsScreen> createState() => _StatsScreenState();
}

class _StatsScreenState extends State<StatsScreen> {
  late DateTime _selectedDate = _dateOnly(DateTime.now());

  void _selectDate(DateTime date) {
    final next = _dateOnly(date);
    if (_sameDay(next, _selectedDate)) {
      return;
    }
    HapticFeedback.selectionClick();
    setState(() {
      _selectedDate = next;
    });
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: widget.state.statsScreenChanges,
      builder: (context, _) {
        final palette = widget.state.palette;
        final today = DateTime.now();
        return ListView(
          padding: const EdgeInsets.fromLTRB(20, 30, 20, 112),
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    '统计',
                    style: TextStyle(
                      color: palette.text,
                      fontSize: 32,
                      fontWeight: AppTextWeight.semibold,
                      letterSpacing: -1.2,
                    ),
                  ),
                ),
                IconButton(
                  onPressed: () {
                    HapticFeedback.selectionClick();
                    Navigator.of(context).push(
                      MaterialPageRoute<void>(
                        builder: (_) => _StatsDetailScreen(state: widget.state),
                      ),
                    );
                  },
                  icon: Icon(Icons.bar_chart_rounded, color: palette.muted),
                  tooltip: '详情',
                ),
              ],
            ),
            const SizedBox(height: 24),
            Text(
              '日历',
              style: TextStyle(
                color: palette.text,
                fontSize: 21,
                fontWeight: AppTextWeight.semibold,
              ),
            ),
            const SizedBox(height: 16),
            _Heatmap(
              state: widget.state,
              palette: palette,
              selectedDate: _selectedDate,
              onSelected: _selectDate,
            ),
            const SizedBox(height: 28),
            _SelectedDayDetails(
              state: widget.state,
              palette: palette,
              selectedDate: _selectedDate,
              today: _dateOnly(today),
            ),
          ],
        );
      },
    );
  }
}

String _dateLabel(DateTime date) {
  String two(int value) => value.toString().padLeft(2, '0');
  return '${date.year}-${two(date.month)}-${two(date.day)}';
}

class _Heatmap extends StatelessWidget {
  const _Heatmap({
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
    final today = _dateOnly(DateTime.now());
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
            _WeekdayLabels(palette: palette),
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
                                    child: _DayCell(
                                      date: day,
                                      palette: palette,
                                      seconds: day.isAfter(today)
                                          ? null
                                          : state.readingSecondsFor(day),
                                      isToday: _sameDay(day, today),
                                      isSelected: _sameDay(day, selectedDate),
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
              _LegendBox(color: _heatColor(seconds, palette)),
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

class _WeekdayLabels extends StatelessWidget {
  const _WeekdayLabels({required this.palette});

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

class _DayCell extends StatelessWidget {
  const _DayCell({
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
              : _heatColor(seconds!, palette),
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

class _SelectedDayDetails extends StatelessWidget {
  const _SelectedDayDetails({
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
    final isToday = _sameDay(selectedDate, today);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          isToday
              ? '今天 · ${_dateLabel(selectedDate)}'
              : _dateLabel(selectedDate),
          style: TextStyle(
            color: palette.text,
            fontSize: 24,
            fontWeight: AppTextWeight.semibold,
            letterSpacing: .2,
          ),
        ),
        const SizedBox(height: 16),
        _StatCard(
          palette: palette,
          title: isToday ? '今日总时长' : '当天总时长',
          seconds: total,
        ),
        const SizedBox(height: 12),
        if (rows.isEmpty)
          _EmptyDayCard(palette: palette, isToday: isToday)
        else
          _DailyBookBreakdownCard(
            palette: palette,
            rows: rows,
            totalSeconds: total,
          ),
      ],
    );
  }
}

class _EmptyDayCard extends StatelessWidget {
  const _EmptyDayCard({required this.palette, required this.isToday});

  final AppPalette palette;
  final bool isToday;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: _detailDecoration(palette),
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

class _LegendBox extends StatelessWidget {
  const _LegendBox({required this.color});

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

class _DailyBookBreakdownCard extends StatelessWidget {
  const _DailyBookBreakdownCard({
    required this.palette,
    required this.rows,
    required this.totalSeconds,
  });

  final AppPalette palette;
  final List<MapEntry<BookEntry, int>> rows;
  final int totalSeconds;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 8),
      decoration: _detailDecoration(palette),
      child: Column(
        children: [
          Row(
            children: [
              Icon(Icons.menu_book_rounded, color: palette.primary, size: 19),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  '阅读明细',
                  style: TextStyle(
                    color: palette.text,
                    fontSize: 16,
                    fontWeight: AppTextWeight.semibold,
                  ),
                ),
              ),
              Text(
                '按时长排序',
                style: TextStyle(
                  color: palette.muted,
                  fontSize: 12.5,
                  fontWeight: AppTextWeight.regular,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          for (var index = 0; index < rows.length; index++) ...[
            if (index > 0)
              Divider(height: 1, color: palette.line.withValues(alpha: .52)),
            _DailyBookBreakdownRow(
              palette: palette,
              book: rows[index].key,
              seconds: rows[index].value,
              totalSeconds: totalSeconds,
            ),
          ],
        ],
      ),
    );
  }
}

class _DailyBookBreakdownRow extends StatelessWidget {
  const _DailyBookBreakdownRow({
    required this.palette,
    required this.book,
    required this.seconds,
    required this.totalSeconds,
  });

  final AppPalette palette;
  final BookEntry book;
  final int seconds;
  final int totalSeconds;

  @override
  Widget build(BuildContext context) {
    final share = totalSeconds <= 0
        ? 0.0
        : (seconds / totalSeconds).clamp(0.0, 1.0);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 11),
      child: Row(
        children: [
          BookCover(
            book: book,
            palette: palette,
            width: 42,
            height: 56,
            radius: 9,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Text(
                        book.title,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          color: palette.text,
                          fontSize: 15,
                          height: 1.12,
                          fontWeight: AppTextWeight.semibold,
                        ),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Text(
                      _durationLabel(seconds),
                      style: TextStyle(
                        color: palette.primary,
                        fontSize: 14.5,
                        fontWeight: AppTextWeight.semibold,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 5),
                Text(
                  '${book.safeCurrentChapter.title} · ${bookWordCountLabel(book.wordCount)}',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color: palette.muted,
                    fontSize: 12,
                    height: 1.1,
                  ),
                ),
                const SizedBox(height: 8),
                ClipRRect(
                  borderRadius: BorderRadius.circular(999),
                  child: LinearProgressIndicator(
                    value: share,
                    minHeight: 5,
                    backgroundColor: palette.line.withValues(alpha: .55),
                    valueColor: AlwaysStoppedAnimation<Color>(palette.primary),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _StatCard extends StatelessWidget {
  const _StatCard({
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
        border: Border.all(color: palette.line.withValues(alpha: .45)),
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
            seconds == 0 ? '0分钟' : _durationLabel(seconds),
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

String _durationLabel(int seconds) {
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

Color _heatColor(int seconds, AppPalette palette) {
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

DateTime _dateOnly(DateTime date) => DateTime(date.year, date.month, date.day);

bool _sameDay(DateTime a, DateTime b) {
  return a.year == b.year && a.month == b.month && a.day == b.day;
}

enum _StatsRange { week, month, year }

class _StatsDetailScreen extends StatefulWidget {
  const _StatsDetailScreen({required this.state});

  final AppState state;

  @override
  State<_StatsDetailScreen> createState() => _StatsDetailScreenState();
}

class _StatsDetailScreenState extends State<_StatsDetailScreen> {
  var _range = _StatsRange.week;

  void _setRange(_StatsRange range) {
    if (range == _range) {
      return;
    }
    setState(() {
      _range = range;
    });
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: widget.state.statsScreenChanges,
      builder: (context, _) {
        final palette = widget.state.palette;
        final now = DateTime.now();
        return Scaffold(
          backgroundColor: palette.background,
          body: SafeArea(
            bottom: false,
            child: ListView(
              padding: const EdgeInsets.fromLTRB(20, 26, 20, 32),
              children: [
                Row(
                  children: [
                    IconButton(
                      onPressed: () {
                        HapticFeedback.selectionClick();
                        Navigator.pop(context);
                      },
                      icon: Icon(Icons.arrow_back_rounded, color: palette.text),
                    ),
                    const SizedBox(width: 6),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            '详情',
                            style: TextStyle(
                              color: palette.text,
                              fontSize: 30,
                              fontWeight: AppTextWeight.medium,
                              letterSpacing: -1,
                            ),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            _rangeLabel(now, _range),
                            style: TextStyle(
                              color: palette.muted,
                              fontSize: 15,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 22),
                _RangeSwitch(
                  range: _range,
                  palette: palette,
                  onChanged: _setRange,
                ),
                _StatsRangeContent(
                  state: widget.state,
                  palette: palette,
                  now: now,
                  range: _range,
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class _RangeSwitch extends StatelessWidget {
  const _RangeSwitch({
    required this.range,
    required this.palette,
    required this.onChanged,
  });

  final _StatsRange range;
  final AppPalette palette;
  final ValueChanged<_StatsRange> onChanged;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final segment = constraints.maxWidth / 3;
        return Container(
          height: 54,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(999),
            border: Border.all(color: palette.muted.withValues(alpha: .8)),
          ),
          clipBehavior: Clip.antiAlias,
          child: Stack(
            children: [
              AnimatedPositioned(
                duration: const Duration(milliseconds: 260),
                curve: Curves.easeOutCubic,
                left: segment * range.index + 4,
                top: 4,
                bottom: 4,
                width: segment - 8,
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: palette.primary.withValues(alpha: .38),
                    borderRadius: BorderRadius.circular(999),
                  ),
                ),
              ),
              Row(
                children: [
                  _RangeButton(
                    label: '周',
                    value: _StatsRange.week,
                    range: range,
                    palette: palette,
                    onChanged: onChanged,
                  ),
                  _RangeButton(
                    label: '月',
                    value: _StatsRange.month,
                    range: range,
                    palette: palette,
                    onChanged: onChanged,
                  ),
                  _RangeButton(
                    label: '年',
                    value: _StatsRange.year,
                    range: range,
                    palette: palette,
                    onChanged: onChanged,
                  ),
                ],
              ),
            ],
          ),
        );
      },
    );
  }
}

class _RangeButton extends StatelessWidget {
  const _RangeButton({
    required this.label,
    required this.value,
    required this.range,
    required this.palette,
    required this.onChanged,
  });

  final String label;
  final _StatsRange value;
  final _StatsRange range;
  final AppPalette palette;
  final ValueChanged<_StatsRange> onChanged;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: InkWell(
        onTap: () {
          HapticFeedback.selectionClick();
          onChanged(value);
        },
        child: Container(
          alignment: Alignment.center,
          child: Text(
            label,
            style: TextStyle(
              color: palette.text,
              fontSize: 18,
              fontWeight: AppTextWeight.regular,
            ),
          ),
        ),
      ),
    );
  }
}

class _StatsRangeContent extends StatelessWidget {
  const _StatsRangeContent({
    required this.state,
    required this.palette,
    required this.now,
    required this.range,
  });

  final AppState state;
  final AppPalette palette;
  final DateTime now;
  final _StatsRange range;

  @override
  Widget build(BuildContext context) {
    final bars = _buildBars(state, now, range);
    final total = bars.fold<int>(0, (sum, item) => sum + item.seconds);
    final bookTotals = _bookTotals(state, now, range);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SizedBox(height: 30),
        Text(
          '活动',
          style: TextStyle(
            color: palette.text,
            fontSize: 21,
            fontWeight: AppTextWeight.medium,
          ),
        ),
        const SizedBox(height: 14),
        _ActivityCard(books: state.books, palette: palette, range: range),
        const SizedBox(height: 26),
        Text(
          '阅读时长',
          style: TextStyle(
            color: palette.text,
            fontSize: 21,
            fontWeight: AppTextWeight.medium,
          ),
        ),
        const SizedBox(height: 14),
        _ChartCard(palette: palette, bars: bars, totalSeconds: total),
        const SizedBox(height: 26),
        Text(
          '阅读详情',
          style: TextStyle(
            color: palette.text,
            fontSize: 21,
            fontWeight: AppTextWeight.medium,
          ),
        ),
        const SizedBox(height: 14),
        _BookBreakdownCard(
          books: state.books,
          totals: bookTotals,
          palette: palette,
        ),
      ],
    );
  }
}

class _ActivityCard extends StatelessWidget {
  const _ActivityCard({
    required this.books,
    required this.palette,
    required this.range,
  });

  final List<BookEntry> books;
  final AppPalette palette;
  final _StatsRange range;

  @override
  Widget build(BuildContext context) {
    final active = books
        .where((book) => book.progress > 0)
        .take(range == _StatsRange.year ? 6 : 2)
        .toList();
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: _detailDecoration(palette),
      child: active.isEmpty
          ? Text('还没有足够的活动记录。', style: TextStyle(color: palette.muted))
          : Column(
              children: [
                for (var i = 0; i < active.length; i++) ...[
                  Row(
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              i == 0 ? '最近读了' : '继续推进',
                              style: TextStyle(
                                color: palette.text,
                                fontSize: 17,
                                fontWeight: AppTextWeight.medium,
                              ),
                            ),
                            const SizedBox(height: 6),
                            Text(
                              active[i].title,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: TextStyle(
                                color: palette.muted,
                                height: 1.45,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 16),
                      BookCover(
                        book: active[i],
                        palette: palette,
                        width: 62,
                        height: 88,
                        radius: 10,
                      ),
                    ],
                  ),
                  if (i != active.length - 1)
                    Divider(color: palette.line, height: 26),
                ],
              ],
            ),
    );
  }
}

class _ChartCard extends StatelessWidget {
  const _ChartCard({
    required this.palette,
    required this.bars,
    required this.totalSeconds,
  });

  final AppPalette palette;
  final List<_BarData> bars;
  final int totalSeconds;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: _detailDecoration(palette),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '总计',
            style: TextStyle(
              color: palette.muted,
              fontSize: 15,
              fontWeight: AppTextWeight.regular,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            _duration(totalSeconds),
            style: TextStyle(
              color: palette.text,
              fontSize: 26,
              fontWeight: AppTextWeight.regular,
            ),
          ),
          const SizedBox(height: 20),
          SizedBox(
            height: 230,
            child: TweenAnimationBuilder<double>(
              key: ValueKey(bars.map((item) => item.seconds).join(',')),
              tween: Tween(begin: 0, end: 1),
              duration: const Duration(milliseconds: 520),
              curve: Curves.easeOutCubic,
              builder: (context, progress, _) {
                return CustomPaint(
                  painter: _BarChartPainter(
                    bars: bars,
                    palette: palette,
                    progress: progress,
                  ),
                  child: const SizedBox.expand(),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _BookBreakdownCard extends StatelessWidget {
  const _BookBreakdownCard({
    required this.books,
    required this.totals,
    required this.palette,
  });

  final List<BookEntry> books;
  final Map<String, int> totals;
  final AppPalette palette;

  @override
  Widget build(BuildContext context) {
    final rows = books.where((book) => (totals[book.id] ?? 0) > 0).toList()
      ..sort((a, b) => (totals[b.id] ?? 0).compareTo(totals[a.id] ?? 0));
    final total = rows.fold<int>(
      0,
      (sum, book) => sum + (totals[book.id] ?? 0),
    );
    final colors = [
      palette.primarySoft,
      palette.primary,
      palette.primarySoft.withValues(alpha: .74),
      palette.primary.withValues(alpha: .74),
      palette.primarySoft.withValues(alpha: .52),
      palette.primary.withValues(alpha: .52),
      palette.line,
    ];
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: _detailDecoration(palette),
      child: rows.isEmpty
          ? Text('这个时间范围还没有阅读记录。', style: TextStyle(color: palette.muted))
          : Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                SizedBox(
                  height: 94,
                  child: Stack(
                    children: [
                      for (var i = 0; i < rows.take(7).length; i++)
                        Positioned(
                          left: i * 34,
                          child: BookCover(
                            book: rows[i],
                            palette: palette,
                            width: 66,
                            height: 92,
                            radius: 10,
                          ),
                        ),
                    ],
                  ),
                ),
                const SizedBox(height: 18),
                ClipRRect(
                  borderRadius: BorderRadius.circular(999),
                  child: Row(
                    children: [
                      for (var i = 0; i < rows.length; i++)
                        Expanded(
                          flex: math.max(1, totals[rows[i].id] ?? 0),
                          child: ColoredBox(
                            color: colors[i % colors.length],
                            child: const SizedBox(height: 14),
                          ),
                        ),
                    ],
                  ),
                ),
                const SizedBox(height: 16),
                for (var i = 0; i < rows.length; i++)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: Row(
                      children: [
                        Container(
                          width: 10,
                          height: 10,
                          decoration: BoxDecoration(
                            color: colors[i % colors.length],
                            shape: BoxShape.circle,
                          ),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Text(
                            rows[i].title,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(color: palette.text, fontSize: 15),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Text(
                          _clock(totals[rows[i].id] ?? 0),
                          style: TextStyle(
                            color: palette.muted,
                            fontSize: 15,
                            fontWeight: AppTextWeight.regular,
                          ),
                        ),
                      ],
                    ),
                  ),
                if (total > 0)
                  Text(
                    '共 ${_duration(total)}',
                    style: TextStyle(color: palette.subtle, fontSize: 13),
                  ),
              ],
            ),
    );
  }
}

class _BarChartPainter extends CustomPainter {
  const _BarChartPainter({
    required this.bars,
    required this.palette,
    required this.progress,
  });

  final List<_BarData> bars;
  final AppPalette palette;
  final double progress;

  @override
  void paint(Canvas canvas, Size size) {
    final axis = Paint()
      ..color = palette.muted.withValues(alpha: .35)
      ..strokeWidth = 1;
    final grid = Paint()
      ..color = palette.muted.withValues(alpha: .22)
      ..strokeWidth = 1
      ..style = PaintingStyle.stroke;
    final textPainter = TextPainter(textDirection: TextDirection.ltr);
    final chart = Rect.fromLTWH(0, 8, size.width, size.height - 34);
    for (var i = 0; i < 5; i++) {
      final y = chart.top + chart.height * i / 4;
      canvas.drawLine(Offset(chart.left, y), Offset(chart.right, y), grid);
    }
    canvas.drawLine(
      Offset(chart.left, chart.bottom),
      Offset(chart.right, chart.bottom),
      axis,
    );
    final maxSeconds = math.max(
      60,
      bars.fold<int>(0, (maxValue, item) => math.max(maxValue, item.seconds)),
    );
    final gap = 10.0;
    final width = (chart.width - gap * (bars.length - 1)) / bars.length;
    for (var i = 0; i < bars.length; i++) {
      final item = bars[i];
      final height = chart.height * item.seconds / maxSeconds * progress;
      final rect = RRect.fromRectAndRadius(
        Rect.fromLTWH(i * (width + gap), chart.bottom - height, width, height),
        const Radius.circular(8),
      );
      canvas.drawRRect(rect, Paint()..color = palette.primarySoft);
      textPainter.text = TextSpan(
        text: item.label,
        style: TextStyle(color: palette.muted, fontSize: 11),
      );
      textPainter.layout(maxWidth: width + 8);
      textPainter.paint(
        canvas,
        Offset(i * (width + gap) - 2, chart.bottom + 8),
      );
    }
  }

  @override
  bool shouldRepaint(covariant _BarChartPainter oldDelegate) {
    return oldDelegate.bars != bars ||
        oldDelegate.palette != palette ||
        oldDelegate.progress != progress;
  }
}

class _BarData {
  const _BarData(this.label, this.seconds);

  final String label;
  final int seconds;
}

List<_BarData> _buildBars(AppState state, DateTime now, _StatsRange range) {
  if (range == _StatsRange.week) {
    final start = _dateOnly(now).subtract(Duration(days: now.weekday - 1));
    return [
      for (var i = 0; i < 7; i++)
        _BarData(
          _monthDay(start.add(Duration(days: i))),
          state.readingSecondsFor(start.add(Duration(days: i))),
        ),
    ];
  }
  if (range == _StatsRange.month) {
    final first = DateTime(now.year, now.month, 1);
    final weeks = <_BarData>[];
    for (var i = 0; i < 5; i++) {
      var seconds = 0;
      for (var d = 0; d < 7; d++) {
        final day = first.add(Duration(days: i * 7 + d));
        if (day.month == now.month) {
          seconds += state.readingSecondsFor(day);
        }
      }
      weeks.add(_BarData('第${i + 1}周', seconds));
    }
    return weeks;
  }
  return [
    for (var month = 1; month <= 12; month++)
      _BarData('$month月', _monthSeconds(state, now.year, month)),
  ];
}

Map<String, int> _bookTotals(AppState state, DateTime now, _StatsRange range) {
  final result = <String, int>{};
  for (final book in state.books) {
    var total = 0;
    for (final entry
        in state.readingStats[book.id]?.entries ??
            const Iterable<MapEntry<String, int>>.empty()) {
      final date = DateTime.tryParse(entry.key);
      if (date != null && _inRange(date, now, range)) {
        total += entry.value;
      }
    }
    result[book.id] = total;
  }
  return result;
}

bool _inRange(DateTime date, DateTime now, _StatsRange range) {
  if (range == _StatsRange.year) {
    return date.year == now.year;
  }
  if (range == _StatsRange.month) {
    return date.year == now.year && date.month == now.month;
  }
  final start = _dateOnly(now).subtract(Duration(days: now.weekday - 1));
  final end = start.add(const Duration(days: 7));
  return !date.isBefore(start) && date.isBefore(end);
}

int _monthSeconds(AppState state, int year, int month) {
  final first = DateTime(year, month, 1);
  final next = DateTime(year, month + 1, 1);
  var total = 0;
  for (
    var day = first;
    day.isBefore(next);
    day = day.add(const Duration(days: 1))
  ) {
    total += state.readingSecondsFor(day);
  }
  return total;
}

String _rangeLabel(DateTime now, _StatsRange range) {
  if (range == _StatsRange.year) {
    return '${now.year}-01-01 至 ${now.year}-12-31';
  }
  if (range == _StatsRange.month) {
    return '${now.year}-${now.month.toString().padLeft(2, '0')}-01 至 ${now.year}-${now.month.toString().padLeft(2, '0')}-${DateTime(now.year, now.month + 1, 0).day}';
  }
  final start = _dateOnly(now).subtract(Duration(days: now.weekday - 1));
  final end = start.add(const Duration(days: 6));
  return '${_dateLabel(start)} 至 ${_dateLabel(end)}';
}

String _duration(int seconds) {
  final hours = seconds ~/ 3600;
  final minutes = (seconds % 3600) ~/ 60;
  if (hours > 0) {
    return '$hours小时 $minutes分钟';
  }
  return '$minutes分钟';
}

String _clock(int seconds) {
  final hours = seconds ~/ 3600;
  final minutes = (seconds % 3600) ~/ 60;
  final secs = seconds % 60;
  if (hours > 0) {
    return '$hours:${minutes.toString().padLeft(2, '0')}:${secs.toString().padLeft(2, '0')}';
  }
  return '$minutes:${secs.toString().padLeft(2, '0')}';
}

String _monthDay(DateTime date) => '${date.month}/${date.day}';

BoxDecoration _detailDecoration(AppPalette palette) {
  return BoxDecoration(
    color: palette.card,
    borderRadius: BorderRadius.circular(24),
    border: Border.all(color: palette.line.withValues(alpha: .45)),
  );
}
