import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../app_state.dart';
import '../typography.dart';
import 'stats_detail_screen.dart';
import 'stats_heatmap.dart';
import 'stats_widgets.dart';

// ── StatsScreen ──────────────────────────────────────────────────────

class StatsScreen extends StatefulWidget {
  const StatsScreen({super.key, required this.state});

  final AppState state;

  @override
  State<StatsScreen> createState() => _StatsScreenState();
}

class _StatsScreenState extends State<StatsScreen> {
  late DateTime _selectedDate = dateOnly(DateTime.now());

  void _selectDate(DateTime date) {
    final next = dateOnly(date);
    if (sameDay(next, _selectedDate)) {
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
                        builder: (_) => StatsDetailScreen(state: widget.state),
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
            StatsHeatmap(
              state: widget.state,
              palette: palette,
              selectedDate: _selectedDate,
              onSelected: _selectDate,
            ),
            const SizedBox(height: 28),
            SelectedDayDetails(
              state: widget.state,
              palette: palette,
              selectedDate: _selectedDate,
              today: dateOnly(today),
            ),
          ],
        );
      },
    );
  }
}
