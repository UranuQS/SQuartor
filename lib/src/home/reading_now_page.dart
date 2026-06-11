import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../app_state.dart';
import '../models.dart';
import '../screens/reader_screen.dart';
import '../typography.dart';
import '../widgets/book_cover.dart';
import 'error_overlay.dart';

class ReadingNowPage extends StatelessWidget {
  const ReadingNowPage({super.key, required this.state});

  final AppState state;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: state.readingNowChanges,
      builder: (context, _) {
        final palette = state.palette;
        final recent = [...state.books]..sort(_compareRecentlyRead);
        final readingBooks = recent.where((item) => item.progress > 0).toList();
        final book = readingBooks.firstOrNull;
        final now = DateTime.now();
        return ListView(
          physics: const ClampingScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(20, 30, 20, 84),
          children: [
            _ReadingNowHeader(
              palette: palette,
              monthSeconds: _monthReadingSeconds(state, now),
              weekSeconds: _weekReadingSeconds(state, now),
            ),
            const SizedBox(height: 26),
            Text(
              '继续阅读',
              style: TextStyle(
                color: palette.muted,
                fontSize: 18,
                fontWeight: AppTextWeight.semibold,
              ),
            ),
            const SizedBox(height: 14),
            if (book == null)
              EmptyCard(
                palette: palette,
                title: '还没有阅读进度',
                subtitle: '从书架导入 TXT 或 EPUB 后，打开一本书就会自动记录阅读位置。',
              )
            else
              _ContinueCard(book: book, palette: palette),
            const SizedBox(height: 26),
            Text(
              '最近阅读 (${readingBooks.length})',
              style: TextStyle(
                color: palette.muted,
                fontSize: 18,
                fontWeight: AppTextWeight.semibold,
              ),
            ),
            const SizedBox(height: 14),
            if (readingBooks.isEmpty)
              EmptyCard(
                palette: palette,
                title: '最近阅读会出现在这里',
                subtitle: '打开一本书并翻页后，SQuartor 会按最近阅读时间排序，而不是只按进度排序。',
              )
            else
              for (final item in readingBooks)
                Padding(
                  padding: const EdgeInsets.only(bottom: 22),
                  child: _RecentReadingTile(book: item, palette: palette),
                ),
          ],
        );
      },
    );
  }
}

int _compareRecentlyRead(BookEntry a, BookEntry b) {
  final aTime = a.lastReadAt ?? a.importedAt;
  final bTime = b.lastReadAt ?? b.importedAt;
  final byTime = bTime.compareTo(aTime);
  if (byTime != 0) {
    return byTime;
  }
  return b.progress.compareTo(a.progress);
}

int _monthReadingSeconds(AppState state, DateTime now) {
  var total = 0;
  for (
    var day = DateTime(now.year, now.month, 1);
    !day.isAfter(now);
    day = day.add(const Duration(days: 1))
  ) {
    total += state.readingSecondsFor(day);
  }
  return total;
}

int _weekReadingSeconds(AppState state, DateTime now) {
  var total = 0;
  final today = DateTime(now.year, now.month, now.day);
  final start = today.subtract(Duration(days: today.weekday - 1));
  for (var i = 0; i <= today.difference(start).inDays; i++) {
    total += state.readingSecondsFor(start.add(Duration(days: i)));
  }
  return total;
}

String _readingDurationLabel(int seconds) {
  if (seconds <= 0) {
    return '0 分钟';
  }
  final minutes = seconds < 60 ? 1 : (seconds / 60).round();
  if (minutes < 60) {
    return '$minutes 分钟';
  }
  final hours = minutes ~/ 60;
  final rest = minutes % 60;
  if (rest == 0) {
    return '$hours 小时';
  }
  return '$hours 小时 $rest 分钟';
}

TextStyle _titleStyle(AppPalette palette) {
  return TextStyle(
    color: palette.text,
    fontSize: 32,
    fontWeight: AppTextWeight.semibold,
    letterSpacing: -1.2,
  );
}

class _ReadingNowHeader extends StatelessWidget {
  const _ReadingNowHeader({
    required this.palette,
    required this.monthSeconds,
    required this.weekSeconds,
  });

  final AppPalette palette;
  final int monthSeconds;
  final int weekSeconds;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('阅读中', style: _titleStyle(palette)),
        const SizedBox(height: 20),
        Row(
          children: [
            Expanded(
              child: _ReadingStatCard(
                label: '本月阅读',
                value: _readingDurationLabel(monthSeconds),
                palette: palette,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _ReadingStatCard(
                label: '本周阅读',
                value: _readingDurationLabel(weekSeconds),
                palette: palette,
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class _ReadingStatCard extends StatelessWidget {
  const _ReadingStatCard({
    required this.label,
    required this.value,
    required this.palette,
  });

  final String label;
  final String value;
  final AppPalette palette;

  @override
  Widget build(BuildContext context) {
    final isLight = palette.background.computeLuminance() > .5;
    return Container(
      constraints: const BoxConstraints(minHeight: 82),
      padding: const EdgeInsets.fromLTRB(18, 14, 16, 14),
      decoration: BoxDecoration(
        color: palette.card,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color: palette.muted,
                    fontSize: 12.5,
                    height: 1,
                    fontWeight: AppTextWeight.medium,
                  ),
                ),
              ),
              Icon(
                Icons.schedule_rounded,
                size: 16,
                color: isLight ? palette.primary : palette.accentText,
              ),
            ],
          ),
          const SizedBox(height: 10),
          FittedBox(
            fit: BoxFit.scaleDown,
            alignment: Alignment.centerLeft,
            child: Text(
              value,
              maxLines: 1,
              style: TextStyle(
                color: palette.text,
                fontSize: 24,
                height: 1,
                fontWeight: AppTextWeight.semibold,
                letterSpacing: -.4,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _ContinueCard extends StatelessWidget {
  const _ContinueCard({required this.book, required this.palette});

  final BookEntry book;
  final AppPalette palette;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: cardDecoration(palette),
      child: LayoutBuilder(
        builder: (context, constraints) {
          final compact = constraints.maxWidth < 360;
          final coverWidth = compact ? 90.0 : 100.0;
          final coverHeight = compact ? 126.0 : 140.0;
          final cover = BookCover(
            book: book,
            palette: palette,
            width: coverWidth,
            height: coverHeight,
            radius: 16,
            hero: true,
          );
          return Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              cover,
              SizedBox(width: compact ? 12 : 16),
              Expanded(
                child: SizedBox(
                  height: coverHeight,
                  child: _ContinueInfo(book: book, palette: palette),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}

class _ContinueInfo extends StatelessWidget {
  const _ContinueInfo({required this.book, required this.palette});

  final BookEntry book;
  final AppPalette palette;

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        Positioned(
          left: 0,
          right: 0,
          top: 0,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                book.title,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  color: palette.text,
                  fontSize: 19.5,
                  height: 1.2,
                  fontWeight: AppTextWeight.semibold,
                  letterSpacing: -.4,
                ),
              ),
              const SizedBox(height: 5),
              Text(
                book.safeCurrentChapter.title,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  color: palette.accentText,
                  fontSize: 13.5,
                  height: 1.35,
                  fontWeight: AppTextWeight.medium,
                ),
              ),
            ],
          ),
        ),
        Positioned(
          left: 0,
          right: 0,
          bottom: 0,
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              _InfoPill(
                icon: Icons.timelapse_rounded,
                leading: _CircularProgressGlyph(
                  progress: book.progress,
                  palette: palette,
                ),
                label: '${(book.progress * 100).toStringAsFixed(0)}%',
                palette: palette,
                height: 38,
              ),
              const SizedBox(width: 10),
              Expanded(
                child: _ContinueButton(book: book, palette: palette),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _ContinueButton extends StatelessWidget {
  const _ContinueButton({required this.book, required this.palette});

  final BookEntry book;
  final AppPalette palette;

  @override
  Widget build(BuildContext context) {
    return FilledButton.icon(
      style: FilledButton.styleFrom(
        backgroundColor: palette.primary,
        foregroundColor: Colors.white,
        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
        minimumSize: const Size(0, 38),
        maximumSize: const Size(double.infinity, 38),
        padding: const EdgeInsets.symmetric(horizontal: 14),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(13)),
      ),
      onPressed: () {
        HapticFeedback.selectionClick();
        Navigator.of(
          context,
        ).pushNamed(ReaderScreen.routeName, arguments: book);
      },
      icon: const Icon(Icons.menu_book_rounded, size: 18),
      label: const Text(
        '继续阅读',
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: TextStyle(fontWeight: AppTextWeight.semibold),
      ),
    );
  }
}

class _ReadingProgressBar extends StatelessWidget {
  const _ReadingProgressBar({required this.progress, required this.palette});

  final double progress;
  final AppPalette palette;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.end,
      children: [
        Transform.translate(
          offset: const Offset(0, 2),
          child: Text(
            '${(progress * 100).toStringAsFixed(0)}%',
            style: TextStyle(
              color: palette.subtle,
              fontSize: 11.5,
              height: 1,
              fontWeight: AppTextWeight.medium,
            ),
          ),
        ),
        const SizedBox(width: 7),
        Expanded(
          child: SizedBox(
            height: 3,
            child: ClipRRect(
              borderRadius: BorderRadius.circular(99),
              child: LinearProgressIndicator(
                minHeight: 3,
                value: progress,
                backgroundColor: palette.line.withValues(alpha: .5),
                valueColor: AlwaysStoppedAnimation(palette.accentText),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _RecentReadingTile extends StatelessWidget {
  const _RecentReadingTile({required this.book, required this.palette});

  final BookEntry book;
  final AppPalette palette;

  @override
  Widget build(BuildContext context) {
    const coverWidth = 88.0;
    const coverHeight = 124.0;
    return InkWell(
      borderRadius: BorderRadius.circular(16),
      onTap: () {
        HapticFeedback.selectionClick();
        Navigator.of(
          context,
        ).pushNamed(ReaderScreen.routeName, arguments: book);
      },
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          BookCover(
            book: book,
            palette: palette,
            width: coverWidth,
            height: coverHeight,
            radius: 13,
          ),
          const SizedBox(width: 16),
          Expanded(
            child: SizedBox(
              height: coverHeight,
              child: Stack(
                clipBehavior: Clip.none,
                children: [
                  Positioned(
                    left: 0,
                    right: 0,
                    top: 0,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          book.title,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            color: palette.text,
                            fontSize: 18,
                            fontWeight: AppTextWeight.semibold,
                          ),
                        ),
                        const SizedBox(height: 10),
                        Text(
                          book.author,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            color: palette.accentText,
                            fontSize: 14,
                            fontWeight: AppTextWeight.regular,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Positioned(
                    left: 0,
                    right: 0,
                    bottom: 0,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(
                          book.safeCurrentChapter.title,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(color: palette.muted, fontSize: 14),
                        ),
                        const SizedBox(height: 9),
                        _ReadingProgressBar(
                          progress: book.progress,
                          palette: palette,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _InfoPill extends StatelessWidget {
  const _InfoPill({
    required this.icon,
    required this.label,
    required this.palette,
    this.leading,
    this.height,
  });

  final IconData icon;
  final String label;
  final AppPalette palette;
  final Widget? leading;
  final double? height;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: height,
      padding: const EdgeInsets.symmetric(horizontal: 12),
      decoration: BoxDecoration(
        color: palette.cardAlt,
        borderRadius: BorderRadius.circular(13),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          leading ?? Icon(icon, size: 16, color: palette.muted),
          const SizedBox(width: 6),
          Text(
            label,
            style: TextStyle(
              color: palette.muted,
              fontSize: 13,
              fontWeight: AppTextWeight.regular,
            ),
          ),
        ],
      ),
    );
  }
}

class _CircularProgressGlyph extends StatelessWidget {
  const _CircularProgressGlyph({required this.progress, required this.palette});

  final double progress;
  final AppPalette palette;

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      size: const Size.square(17),
      painter: _CircularProgressGlyphPainter(
        progress: progress.clamp(0, 1),
        trackColor: palette.line.withValues(alpha: .65),
        progressColor: palette.accentText,
        centerColor: palette.muted,
      ),
    );
  }
}

class _CircularProgressGlyphPainter extends CustomPainter {
  const _CircularProgressGlyphPainter({
    required this.progress,
    required this.trackColor,
    required this.progressColor,
    required this.centerColor,
  });

  final double progress;
  final Color trackColor;
  final Color progressColor;
  final Color centerColor;

  @override
  void paint(Canvas canvas, Size size) {
    final strokeWidth = math.max(1.8, size.shortestSide * .13);
    final rect = Offset.zero & size;
    final circleRect = rect.deflate(strokeWidth / 2);
    final trackPaint = Paint()
      ..color = trackColor
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round
      ..strokeWidth = strokeWidth;
    final progressPaint = Paint()
      ..color = progressColor
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round
      ..strokeWidth = strokeWidth;
    canvas.drawArc(circleRect, 0, math.pi * 2, false, trackPaint);
    if (progress > 0) {
      canvas.drawArc(
        circleRect,
        -math.pi / 2,
        math.pi * 2 * progress,
        false,
        progressPaint,
      );
    }
    canvas.drawCircle(
      rect.center,
      size.shortestSide * .16,
      Paint()..color = centerColor,
    );
  }

  @override
  bool shouldRepaint(covariant _CircularProgressGlyphPainter oldDelegate) {
    return oldDelegate.progress != progress ||
        oldDelegate.trackColor != trackColor ||
        oldDelegate.progressColor != progressColor ||
        oldDelegate.centerColor != centerColor;
  }
}
