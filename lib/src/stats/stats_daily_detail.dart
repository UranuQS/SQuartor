import 'package:flutter/material.dart';

import '../models.dart';
import '../typography.dart';
import '../widgets/book_cover.dart';
import 'stats_widgets.dart';

// ── DailyBookBreakdownCard ───────────────────────────────────────────

class DailyBookBreakdownCard extends StatelessWidget {
  const DailyBookBreakdownCard({
    super.key,
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
      decoration: detailDecoration(palette),
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
            if (index > 0) const SizedBox(height: 1),
            DailyBookBreakdownRow(
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

// ── DailyBookBreakdownRow ────────────────────────────────────────────

class DailyBookBreakdownRow extends StatelessWidget {
  const DailyBookBreakdownRow({
    super.key,
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
                      durationLabel(seconds),
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
