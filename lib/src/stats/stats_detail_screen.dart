import 'dart:math' as math;
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../app_state.dart';
import '../models.dart';
import '../typography.dart';
import '../widgets/book_cover.dart';
import 'stats_widgets.dart';

// ── StatsRange enum ──────────────────────────────────────────────────

enum StatsRange { week, month, year }

// ── StatsDetailScreen ────────────────────────────────────────────────

class StatsDetailScreen extends StatefulWidget {
  const StatsDetailScreen({super.key, required this.state});

  final AppState state;

  @override
  State<StatsDetailScreen> createState() => _StatsDetailScreenState();
}

class _StatsDetailScreenState extends State<StatsDetailScreen> {
  var _range = StatsRange.week;

  void _setRange(StatsRange range) {
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
                RangeSwitch(
                  range: _range,
                  palette: palette,
                  onChanged: _setRange,
                ),
                StatsRangeContent(
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

// ── RangeSwitch ──────────────────────────────────────────────────────

class RangeSwitch extends StatelessWidget {
  const RangeSwitch({
    super.key,
    required this.range,
    required this.palette,
    required this.onChanged,
  });

  final StatsRange range;
  final AppPalette palette;
  final ValueChanged<StatsRange> onChanged;

  @override
  Widget build(BuildContext context) {
    final isLight = palette.background.computeLuminance() > .5;
    return LayoutBuilder(
      builder: (context, constraints) {
        final segment = constraints.maxWidth / 3;
        final indicatorColor = isLight
            ? palette.primarySoft.withValues(alpha: .34)
            : Color.lerp(palette.cardAlt, palette.primarySoft, .12)!;
        return ClipRRect(
          borderRadius: BorderRadius.circular(999),
          child: BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 16, sigmaY: 16),
            child: Container(
              height: 54,
              decoration: BoxDecoration(
                color: palette.surface.withValues(alpha: isLight ? .58 : .72),
                borderRadius: BorderRadius.circular(999),
              ),
              clipBehavior: Clip.antiAlias,
              child: Stack(
                children: [
                  AnimatedPositioned(
                    duration: const Duration(milliseconds: 360),
                    curve: Curves.easeOutBack,
                    left: segment * range.index + 4,
                    top: 4,
                    bottom: 4,
                    width: segment - 8,
                    child: DecoratedBox(
                      decoration: BoxDecoration(
                        color: indicatorColor,
                        borderRadius: BorderRadius.circular(999),
                      ),
                    ),
                  ),
                  Row(
                    children: [
                      RangeButton(
                        label: '周',
                        value: StatsRange.week,
                        range: range,
                        palette: palette,
                        onChanged: onChanged,
                      ),
                      RangeButton(
                        label: '月',
                        value: StatsRange.month,
                        range: range,
                        palette: palette,
                        onChanged: onChanged,
                      ),
                      RangeButton(
                        label: '年',
                        value: StatsRange.year,
                        range: range,
                        palette: palette,
                        onChanged: onChanged,
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}

// ── RangeButton ──────────────────────────────────────────────────────

class RangeButton extends StatelessWidget {
  const RangeButton({
    super.key,
    required this.label,
    required this.value,
    required this.range,
    required this.palette,
    required this.onChanged,
  });

  final String label;
  final StatsRange value;
  final StatsRange range;
  final AppPalette palette;
  final ValueChanged<StatsRange> onChanged;

  @override
  Widget build(BuildContext context) {
    final selected = value == range;
    return Expanded(
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(999),
          splashColor: palette.primary.withValues(alpha: .08),
          highlightColor: palette.primary.withValues(alpha: .05),
          onTap: () {
            HapticFeedback.selectionClick();
            onChanged(value);
          },
          child: Center(
            child: AnimatedDefaultTextStyle(
              duration: const Duration(milliseconds: 160),
              curve: Curves.easeOutCubic,
              style: TextStyle(
                color: selected ? palette.text : palette.muted,
                fontSize: 18,
                fontWeight: selected
                    ? AppTextWeight.semibold
                    : AppTextWeight.medium,
              ),
              child: Text(label),
            ),
          ),
        ),
      ),
    );
  }
}

// ── StatsRangeContent ────────────────────────────────────────────────

class StatsRangeContent extends StatelessWidget {
  const StatsRangeContent({
    super.key,
    required this.state,
    required this.palette,
    required this.now,
    required this.range,
  });

  final AppState state;
  final AppPalette palette;
  final DateTime now;
  final StatsRange range;

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
        ActivityCard(books: state.books, palette: palette, range: range),
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
        ChartCard(palette: palette, bars: bars, totalSeconds: total),
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
        BookBreakdownCard(
          books: state.books,
          totals: bookTotals,
          palette: palette,
        ),
      ],
    );
  }
}

// ── ActivityCard ─────────────────────────────────────────────────────

class ActivityCard extends StatelessWidget {
  const ActivityCard({
    super.key,
    required this.books,
    required this.palette,
    required this.range,
  });

  final List<BookEntry> books;
  final AppPalette palette;
  final StatsRange range;

  @override
  Widget build(BuildContext context) {
    final active = books
        .where((book) => book.progress > 0)
        .take(range == StatsRange.year ? 6 : 2)
        .toList();
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: detailDecoration(palette),
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
                  if (i != active.length - 1) const SizedBox(height: 26),
                ],
              ],
            ),
    );
  }
}

// ── ChartCard ────────────────────────────────────────────────────────

class ChartCard extends StatelessWidget {
  const ChartCard({
    super.key,
    required this.palette,
    required this.bars,
    required this.totalSeconds,
  });

  final AppPalette palette;
  final List<BarData> bars;
  final int totalSeconds;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: detailDecoration(palette),
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
            durationFormat(totalSeconds),
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
                  painter: BarChartPainter(
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

// ── BookBreakdownCard ────────────────────────────────────────────────

class BookBreakdownCard extends StatelessWidget {
  const BookBreakdownCard({
    super.key,
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
      decoration: detailDecoration(palette),
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
                          clockFormat(totals[rows[i].id] ?? 0),
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
                    '共 ${durationFormat(total)}',
                    style: TextStyle(color: palette.subtle, fontSize: 13),
                  ),
              ],
            ),
    );
  }
}

// ── BarChartPainter ──────────────────────────────────────────────────

class BarChartPainter extends CustomPainter {
  const BarChartPainter({
    required this.bars,
    required this.palette,
    required this.progress,
  });

  final List<BarData> bars;
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
  bool shouldRepaint(covariant BarChartPainter oldDelegate) {
    return oldDelegate.bars != bars ||
        oldDelegate.palette != palette ||
        oldDelegate.progress != progress;
  }
}

// ── BarData ──────────────────────────────────────────────────────────

class BarData {
  const BarData(this.label, this.seconds);

  final String label;
  final int seconds;
}

// ── Helper functions ─────────────────────────────────────────────────

List<BarData> _buildBars(AppState state, DateTime now, StatsRange range) {
  if (range == StatsRange.week) {
    final start = dateOnly(now).subtract(Duration(days: now.weekday - 1));
    return [
      for (var i = 0; i < 7; i++)
        BarData(
          monthDay(start.add(Duration(days: i))),
          state.readingSecondsFor(start.add(Duration(days: i))),
        ),
    ];
  }
  if (range == StatsRange.month) {
    final first = DateTime(now.year, now.month, 1);
    final weeks = <BarData>[];
    for (var i = 0; i < 5; i++) {
      var seconds = 0;
      for (var d = 0; d < 7; d++) {
        final day = first.add(Duration(days: i * 7 + d));
        if (day.month == now.month) {
          seconds += state.readingSecondsFor(day);
        }
      }
      weeks.add(BarData('第${i + 1}周', seconds));
    }
    return weeks;
  }
  return [
    for (var month = 1; month <= 12; month++)
      BarData('$month月', _monthSeconds(state, now.year, month)),
  ];
}

Map<String, int> _bookTotals(AppState state, DateTime now, StatsRange range) {
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

bool _inRange(DateTime date, DateTime now, StatsRange range) {
  if (range == StatsRange.year) {
    return date.year == now.year;
  }
  if (range == StatsRange.month) {
    return date.year == now.year && date.month == now.month;
  }
  final start = dateOnly(now).subtract(Duration(days: now.weekday - 1));
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

String _rangeLabel(DateTime now, StatsRange range) {
  if (range == StatsRange.year) {
    return '${now.year}-01-01 至 ${now.year}-12-31';
  }
  if (range == StatsRange.month) {
    return '${now.year}-${now.month.toString().padLeft(2, '0')}-01 至 ${now.year}-${now.month.toString().padLeft(2, '0')}-${DateTime(now.year, now.month + 1, 0).day}';
  }
  final start = dateOnly(now).subtract(Duration(days: now.weekday - 1));
  final end = start.add(const Duration(days: 6));
  return '${dateLabel(start)} 至 ${dateLabel(end)}';
}
