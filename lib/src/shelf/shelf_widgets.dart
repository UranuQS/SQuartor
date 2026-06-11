import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../models.dart';
import '../typography.dart';
import '../widgets/book_cover.dart';
import 'shelf_enums.dart';

class BookBlockCard extends StatelessWidget {
  const BookBlockCard({
    super.key,
    required this.block,
    required this.palette,
    required this.expanded,
    required this.selectionMode,
    required this.selectedBookIds,
    required this.onToggle,
    required this.onOpenBook,
    required this.onLongPressBook,
  });

  final ShelfBookBlock block;
  final AppPalette palette;
  final bool expanded;
  final bool selectionMode;
  final Set<String> selectedBookIds;
  final VoidCallback onToggle;
  final ValueChanged<BookEntry> onOpenBook;
  final ValueChanged<BookEntry> onLongPressBook;

  @override
  Widget build(BuildContext context) {
    final books = block.books;
    final visibleBooks = expanded ? books : const <BookEntry>[];
    return DecoratedBox(
      decoration: BoxDecoration(
        color: palette.surface,
        borderRadius: BorderRadius.circular(26),
      ),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            InkWell(
              borderRadius: BorderRadius.circular(18),
              onTap: onToggle,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(2, 0, 2, 12),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Padding(
                      padding: const EdgeInsets.only(right: 14),
                      child: _BookCoverStack(
                        books: books,
                        palette: palette,
                        width: 76,
                        height: 106,
                      ),
                    ),
                    Expanded(
                      child: SizedBox(
                        height: 106,
                        child: Stack(
                          children: [
                            Positioned(
                              left: 0,
                              right: 0,
                              top: 0,
                              bottom: 20,
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    block.title,
                                    maxLines: 2,
                                    overflow: TextOverflow.ellipsis,
                                    style: TextStyle(
                                      color: palette.text,
                                      fontSize: 19,
                                      height: 1.18,
                                      fontWeight: AppTextWeight.semibold,
                                    ),
                                  ),
                                  const SizedBox(height: 7),
                                  Text(
                                    '${books.length} 本 · ${block.chapterCount} 章',
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: TextStyle(
                                      color: palette.muted,
                                      fontSize: 12.5,
                                      height: 1.15,
                                    ),
                                  ),
                                  const SizedBox(height: 4),
                                  Text(
                                    bookWordCountLabel(block.wordCount),
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: TextStyle(
                                      color: palette.muted,
                                      fontSize: 12.5,
                                      height: 1.15,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            Positioned(
                              left: 0,
                              right: 0,
                              bottom: 0,
                              child: _ShelfProgressLine(
                                progress: block.progress.clamp(0, 1),
                                palette: palette,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(width: 10),
                    _BlockStatePill(
                      palette: palette,
                      expanded: expanded,
                      count: books.length,
                    ),
                  ],
                ),
              ),
            ),
            AnimatedSize(
              duration: const Duration(milliseconds: 260),
              reverseDuration: const Duration(milliseconds: 220),
              curve: Curves.easeOutCubic,
              alignment: Alignment.topCenter,
              child: ClipRect(
                child: expanded
                    ? Column(
                        children: [
                          const SizedBox(height: 1),
                          const SizedBox(height: 12),
                          for (
                            var index = 0;
                            index < visibleBooks.length;
                            index++
                          ) ...[
                            if (index > 0)
                              Padding(
                                padding: const EdgeInsets.symmetric(
                                  vertical: 12,
                                ),
                                child: const SizedBox(height: 1),
                              ),
                            _BookBlockRow(
                              book: visibleBooks[index],
                              palette: palette,
                              selected: selectedBookIds.contains(
                                visibleBooks[index].id,
                              ),
                              selectionMode: selectionMode,
                              onTap: () => onOpenBook(visibleBooks[index]),
                              onLongPress: () =>
                                  onLongPressBook(visibleBooks[index]),
                            ),
                          ],
                        ],
                      )
                    : const SizedBox.shrink(),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _BookBlockRow extends StatelessWidget {
  const _BookBlockRow({
    required this.book,
    required this.palette,
    required this.selected,
    required this.selectionMode,
    required this.onTap,
    required this.onLongPress,
  });

  final BookEntry book;
  final AppPalette palette;
  final bool selected;
  final bool selectionMode;
  final VoidCallback onTap;
  final VoidCallback onLongPress;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(18),
      onTap: onTap,
      onLongPress: onLongPress,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          BookCover(
            book: book,
            palette: palette,
            width: 76,
            height: 108,
            radius: 13,
            hero: true,
          ),
          const SizedBox(width: 13),
          Expanded(
            child: SizedBox(
              height: 108,
              child: Stack(
                children: [
                  Positioned.fill(
                    bottom: 20,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Expanded(
                              child: Text(
                                book.title,
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(
                                  color: palette.text,
                                  fontSize: 16.5,
                                  height: 1.16,
                                  fontWeight: AppTextWeight.semibold,
                                ),
                              ),
                            ),
                            if (selectionMode)
                              Icon(
                                selected
                                    ? Icons.check_circle_rounded
                                    : Icons.circle_outlined,
                                color: selected
                                    ? palette.accentText
                                    : palette.muted,
                                size: 23,
                              ),
                          ],
                        ),
                        const SizedBox(height: 3),
                        Text(
                          book.author,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            color: palette.accentText,
                            fontSize: 12.5,
                            height: 1.1,
                          ),
                        ),
                        const SizedBox(height: 6),
                        SizedBox(
                          height: 28,
                          child: ClipRect(
                            child: Row(
                              children: [
                                Flexible(
                                  flex: 8,
                                  child: _MetaChip(
                                    icon: Icons.article_outlined,
                                    label: book.formatLabel,
                                    palette: palette,
                                  ),
                                ),
                                const SizedBox(width: 6),
                                Flexible(
                                  flex: 10,
                                  child: _MetaChip(
                                    icon: Icons.list_alt_rounded,
                                    label: '${book.chapters.length} 章',
                                    palette: palette,
                                  ),
                                ),
                                const SizedBox(width: 6),
                                Flexible(
                                  flex: 10,
                                  child: _MetaChip(
                                    icon: Icons.format_size_rounded,
                                    label: bookWordCountLabel(book.wordCount),
                                    palette: palette,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  Positioned(
                    left: 0,
                    right: 0,
                    bottom: 0,
                    child: _ShelfProgressLine(
                      progress: book.progress,
                      palette: palette,
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

class _BookCoverStack extends StatelessWidget {
  const _BookCoverStack({
    required this.books,
    required this.palette,
    required this.width,
    required this.height,
  });

  final List<BookEntry> books;
  final AppPalette palette;
  final double width;
  final double height;

  @override
  Widget build(BuildContext context) {
    final visible = books.take(3).toList();
    return SizedBox(
      width: width + (visible.length - 1) * 18,
      height: height,
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          for (var i = visible.length - 1; i >= 0; i--)
            Positioned(
              left: i * 18,
              top: i * 3,
              child: BookCover(
                book: visible[i],
                palette: palette,
                width: width,
                height: height,
                radius: 13,
              ),
            ),
        ],
      ),
    );
  }
}

class _BlockStatePill extends StatelessWidget {
  const _BlockStatePill({
    required this.palette,
    required this.expanded,
    required this.count,
  });

  final AppPalette palette;
  final bool expanded;
  final int count;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
      decoration: BoxDecoration(
        color: expanded
            ? palette.primarySoft.withValues(alpha: palette.isLight ? .28 : .22)
            : palette.card,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            expanded ? '收起' : '$count 本',
            style: TextStyle(
              color: expanded ? palette.accentText : palette.muted,
              fontSize: 12,
              fontWeight: AppTextWeight.medium,
            ),
          ),
          const SizedBox(width: 3),
          Icon(
            expanded
                ? Icons.keyboard_arrow_up_rounded
                : Icons.keyboard_arrow_down_rounded,
            size: 17,
            color: expanded ? palette.accentText : palette.muted,
          ),
        ],
      ),
    );
  }
}

class BookTile extends StatelessWidget {
  const BookTile({
    super.key,
    required this.book,
    required this.palette,
    required this.selected,
    required this.selectionMode,
    required this.onTap,
    required this.onLongPress,
  });

  final BookEntry book;
  final AppPalette palette;
  final bool selected;
  final bool selectionMode;
  final VoidCallback onTap;
  final VoidCallback onLongPress;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(18),
      onTap: onTap,
      onLongPress: onLongPress,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          BookCover(
            book: book,
            palette: palette,
            width: 92,
            height: 130,
            radius: 14,
            hero: true,
          ),
          const SizedBox(width: 14),
          Expanded(
            child: SizedBox(
              height: 130,
              child: Stack(
                clipBehavior: Clip.none,
                children: [
                  Positioned(
                    left: 0,
                    right: 0,
                    top: 0,
                    bottom: 34,
                    child: ClipRect(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Expanded(
                                child: Text(
                                  book.title,
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                  style: TextStyle(
                                    color: palette.text,
                                    fontSize: 17.2,
                                    height: 1.18,
                                    fontWeight: AppTextWeight.semibold,
                                  ),
                                ),
                              ),
                              if (selectionMode)
                                SizedBox(
                                  width: 34,
                                  height: 34,
                                  child: Icon(
                                    selected
                                        ? Icons.check_circle_rounded
                                        : Icons.circle_outlined,
                                    color: selected
                                        ? palette.accentText
                                        : palette.muted,
                                    size: 24,
                                  ),
                                ),
                            ],
                          ),
                          const SizedBox(height: 4),
                          Text(
                            book.author,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              color: palette.accentText,
                              fontSize: 13.5,
                              height: 1.1,
                              fontWeight: AppTextWeight.regular,
                            ),
                          ),
                          const SizedBox(height: 5),
                          Wrap(
                            spacing: 7,
                            runSpacing: 7,
                            children: [
                              _MetaChip(
                                icon: Icons.article_outlined,
                                label: book.formatLabel,
                                palette: palette,
                              ),
                              _MetaChip(
                                icon: Icons.list_alt_rounded,
                                label: '${book.chapters.length} 章',
                                palette: palette,
                              ),
                              _MetaChip(
                                icon: Icons.format_size_rounded,
                                label: bookWordCountLabel(book.wordCount),
                                palette: palette,
                              ),
                            ],
                          ),
                        ],
                      ),
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
                          style: TextStyle(
                            color: palette.muted,
                            fontSize: 13,
                            height: 1.2,
                          ),
                        ),
                        const SizedBox(height: 5),
                        _ShelfProgressLine(
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

class _ShelfProgressLine extends StatelessWidget {
  const _ShelfProgressLine({required this.progress, required this.palette});

  final double progress;
  final AppPalette palette;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            _ShelfProgressGlyph(progress: progress, palette: palette),
            const SizedBox(width: 4),
            Text(
              '${(progress * 100).toStringAsFixed(0)}%',
              style: TextStyle(
                color: palette.muted,
                fontSize: 11.5,
                height: 1,
                fontWeight: AppTextWeight.medium,
              ),
            ),
          ],
        ),
        const SizedBox(width: 8),
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

class _ShelfProgressGlyph extends StatelessWidget {
  const _ShelfProgressGlyph({required this.progress, required this.palette});

  final double progress;
  final AppPalette palette;

  @override
  Widget build(BuildContext context) {
    return SizedBox.square(
      dimension: 13,
      child: CustomPaint(
        painter: _ShelfProgressGlyphPainter(
          progress: progress.clamp(0, 1),
          trackColor: palette.line,
          progressColor: palette.accentText,
        ),
      ),
    );
  }
}

class _ShelfProgressGlyphPainter extends CustomPainter {
  const _ShelfProgressGlyphPainter({
    required this.progress,
    required this.trackColor,
    required this.progressColor,
  });

  final double progress;
  final Color trackColor;
  final Color progressColor;

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = math.min(size.width, size.height) / 2 - 1.2;
    final trackPaint = Paint()
      ..color = trackColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.8;
    final progressPaint = Paint()
      ..color = progressColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2
      ..strokeCap = StrokeCap.round;
    canvas.drawCircle(center, radius, trackPaint);
    if (progress > 0) {
      canvas.drawArc(
        Rect.fromCircle(center: center, radius: radius),
        -math.pi / 2,
        math.pi * 2 * progress,
        false,
        progressPaint,
      );
    }
  }

  @override
  bool shouldRepaint(covariant _ShelfProgressGlyphPainter oldDelegate) {
    return oldDelegate.progress != progress ||
        oldDelegate.trackColor != trackColor ||
        oldDelegate.progressColor != progressColor;
  }
}

class _MetaChip extends StatelessWidget {
  const _MetaChip({
    required this.icon,
    required this.label,
    required this.palette,
  });

  final IconData icon;
  final String label;
  final AppPalette palette;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: palette.card,
        borderRadius: BorderRadius.circular(9),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: palette.muted),
          const SizedBox(width: 4),
          Text(
            label,
            style: TextStyle(
              color: palette.muted,
              fontSize: 12,
              fontWeight: AppTextWeight.regular,
            ),
          ),
        ],
      ),
    );
  }
}

class EmptyShelf extends StatelessWidget {
  const EmptyShelf({
    super.key,
    required this.palette,
    required this.title,
    required this.subtitle,
    required this.actionLabel,
    required this.onImport,
  });

  final AppPalette palette;
  final String title;
  final String subtitle;
  final String actionLabel;
  final VoidCallback onImport;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: palette.card,
          borderRadius: BorderRadius.circular(22),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(
              Icons.auto_stories_outlined,
              color: palette.accentText,
              size: 36,
            ),
            const SizedBox(height: 16),
            Text(
              title,
              style: TextStyle(
                color: palette.text,
                fontSize: 20,
                fontWeight: AppTextWeight.semibold,
              ),
            ),
            const SizedBox(height: 10),
            Text(
              subtitle,
              style: TextStyle(color: palette.muted, height: 1.65),
            ),
            const SizedBox(height: 20),
            FilledButton.icon(
              style: FilledButton.styleFrom(
                backgroundColor: palette.primary,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(
                  horizontal: 18,
                  vertical: 14,
                ),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(18),
                ),
              ),
              onPressed: onImport,
              icon: const Icon(Icons.add_rounded),
              label: Text(actionLabel),
            ),
          ],
        ),
      ),
    );
  }
}

class ShelfTabs extends StatelessWidget {
  const ShelfTabs({
    super.key,
    required this.shelves,
    required this.selectedIndex,
    required this.palette,
    required this.onChanged,
  });

  static const _itemWidth = 122.0;
  static const _indicatorWidth = 88.0;

  final List<String> shelves;
  final int selectedIndex;
  final AppPalette palette;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) {
    final totalWidth = shelves.length * _itemWidth;
    final left =
        selectedIndex * _itemWidth + (_itemWidth - _indicatorWidth) / 2;
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: SizedBox(
        width: totalWidth,
        height: 52,
        child: Stack(
          children: [
            Row(
              children: [
                for (var i = 0; i < shelves.length; i++)
                  GestureDetector(
                    behavior: HitTestBehavior.opaque,
                    onTap: () {
                      HapticFeedback.selectionClick();
                      onChanged(i);
                    },
                    child: SizedBox(
                      width: _itemWidth,
                      height: 44,
                      child: Center(
                        child: Text(
                          shelves[i] == defaultShelfName
                              ? defaultShelfLabel
                              : shelves[i],
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            color: i == selectedIndex
                                ? palette.accentText
                                : palette.muted,
                            fontSize: 16,
                            fontWeight: AppTextWeight.regular,
                          ),
                        ),
                      ),
                    ),
                  ),
              ],
            ),
            AnimatedPositioned(
              duration: const Duration(milliseconds: 330),
              curve: Curves.easeOutBack,
              left: left,
              bottom: 4,
              child: TweenAnimationBuilder<double>(
                key: ValueKey(selectedIndex),
                tween: Tween(begin: 1.45, end: 1),
                duration: const Duration(milliseconds: 330),
                curve: Curves.easeOutCubic,
                builder: (context, scale, child) {
                  return Transform.scale(scaleX: scale, child: child);
                },
                child: Container(
                  width: _indicatorWidth,
                  height: 4,
                  decoration: BoxDecoration(
                    color: palette.accentText,
                    borderRadius: BorderRadius.circular(99),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class CircleButton extends StatelessWidget {
  const CircleButton({
    super.key,
    required this.palette,
    required this.icon,
    required this.onTap,
  });

  final AppPalette palette;
  final IconData icon;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () {
        HapticFeedback.selectionClick();
        onTap();
      },
      borderRadius: BorderRadius.circular(999),
      child: Container(
        width: 46,
        height: 46,
        decoration: BoxDecoration(color: palette.card, shape: BoxShape.circle),
        child: Icon(icon, color: palette.muted, size: 25),
      ),
    );
  }
}
