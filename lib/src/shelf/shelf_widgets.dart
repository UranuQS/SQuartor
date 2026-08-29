import 'dart:math' as math;
import 'dart:ui' as ui;

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
                    ? _ExpandedBookBlockList(
                        books: visibleBooks,
                        palette: palette,
                        selectedBookIds: selectedBookIds,
                        selectionMode: selectionMode,
                        onOpenBook: onOpenBook,
                        onLongPressBook: onLongPressBook,
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

class BookBlockGridCard extends StatelessWidget {
  const BookBlockGridCard({
    super.key,
    required this.block,
    required this.palette,
    required this.selectionMode,
    required this.selectedBookIds,
    required this.onTap,
    required this.onLongPress,
  });

  final ShelfBookBlock block;
  final AppPalette palette;
  final bool selectionMode;
  final Set<String> selectedBookIds;
  final VoidCallback onTap;
  final VoidCallback onLongPress;

  @override
  Widget build(BuildContext context) {
    final books = block.books;
    final selected =
        books.isNotEmpty &&
        books.every((book) => selectedBookIds.contains(book.id));
    return InkWell(
      borderRadius: BorderRadius.circular(26),
      onTap: onTap,
      onLongPress: onLongPress,
      child: Container(
        padding: const EdgeInsets.fromLTRB(14, 14, 14, 13),
        decoration: BoxDecoration(
          color: palette.surface,
          borderRadius: BorderRadius.circular(26),
        ),
        child: Stack(
          children: [
            Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                LayoutBuilder(
                  builder: (context, constraints) {
                    final cardWidth = constraints.maxWidth.isFinite
                        ? constraints.maxWidth
                        : 180.0;
                    final coverMetrics = _gridCoverMetricsFor(
                      cardWidth,
                      books.length,
                    );
                    final coverHeight = coverMetrics.coverWidth * 1.38;
                    return Center(
                      child: _BookCoverStack(
                        books: books,
                        palette: palette,
                        width: coverMetrics.coverWidth,
                        height: coverHeight,
                        spread: coverMetrics.spread,
                        yStep: 3,
                      ),
                    );
                  },
                ),
                const SizedBox(height: 11),
                Text(
                  block.title,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color: palette.text,
                    fontSize: 16.2,
                    height: 1.16,
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
                    fontSize: 12.2,
                    height: 1.15,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  bookWordCountLabel(block.wordCount),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color: palette.muted,
                    fontSize: 12.2,
                    height: 1.15,
                  ),
                ),
                const SizedBox(height: 12),
                _ShelfProgressLine(
                  progress: block.progress.clamp(0, 1),
                  palette: palette,
                ),
              ],
            ),
            Positioned(
              right: 0,
              top: 0,
              child: selectionMode
                  ? Icon(
                      selected
                          ? Icons.check_circle_rounded
                          : Icons.circle_outlined,
                      color: selected ? palette.accentText : palette.muted,
                      size: 24,
                    )
                  : _BlockCountBadge(palette: palette, count: books.length),
            ),
          ],
        ),
      ),
    );
  }
}

class _GridCoverMetrics {
  const _GridCoverMetrics({required this.coverWidth, required this.spread});

  final double coverWidth;
  final double spread;
}

_GridCoverMetrics _gridCoverMetricsFor(double contentWidth, int bookCount) {
  final visibleCount = bookCount <= 0 ? 1 : math.min(bookCount, 3);
  final stackWidth = math.max(contentWidth * .88, 104.0);
  final spread = visibleCount > 1 ? stackWidth * .07 : 0.0;
  final coverWidth = math.max(104.0, stackWidth - spread * (visibleCount - 1));
  return _GridCoverMetrics(coverWidth: coverWidth, spread: spread);
}

class _BlockCountBadge extends StatelessWidget {
  const _BlockCountBadge({required this.palette, required this.count});

  final AppPalette palette;
  final int count;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 6),
      decoration: BoxDecoration(
        color: palette.card,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        '$count 本',
        style: TextStyle(
          color: palette.muted,
          fontSize: 11.5,
          height: 1,
          fontWeight: AppTextWeight.medium,
        ),
      ),
    );
  }
}

class _ExpandedBookBlockList extends StatelessWidget {
  const _ExpandedBookBlockList({
    required this.books,
    required this.palette,
    required this.selectedBookIds,
    required this.selectionMode,
    required this.onOpenBook,
    required this.onLongPressBook,
  });

  final List<BookEntry> books;
  final AppPalette palette;
  final Set<String> selectedBookIds;
  final bool selectionMode;
  final ValueChanged<BookEntry> onOpenBook;
  final ValueChanged<BookEntry> onLongPressBook;

  static const _rowExtent = 132.0;

  @override
  Widget build(BuildContext context) {
    final height = math.min(books.length * _rowExtent, 430.0).toDouble();
    return Padding(
      padding: const EdgeInsets.only(top: 13),
      child: SizedBox(
        height: height,
        child: ListView.separated(
          padding: EdgeInsets.zero,
          physics: books.length * _rowExtent > height
              ? const ClampingScrollPhysics()
              : const NeverScrollableScrollPhysics(),
          itemCount: books.length,
          separatorBuilder: (_, _) => const SizedBox(height: 24),
          itemBuilder: (context, index) {
            final book = books[index];
            return _BookBlockRow(
              book: book,
              palette: palette,
              selected: selectedBookIds.contains(book.id),
              selectionMode: selectionMode,
              deleteMode: false,
              onDelete: () {},
              onRemoveFromGroup: () {},
              onMoveToGroup: () {},
              onReorderGroup: () {},
              onEditBook: () => onLongPressBook(book),
              onTap: () => onOpenBook(book),
              onLongPress: () => onLongPressBook(book),
            );
          },
        ),
      ),
    );
  }
}

class BookBlockExpandedPanel extends StatefulWidget {
  const BookBlockExpandedPanel({
    super.key,
    required this.block,
    required this.palette,
    required this.selectionMode,
    required this.selectedBookIds,
    required this.onDismiss,
    required this.onDeleteBook,
    required this.onRemoveBookFromGroup,
    required this.onMoveBookToGroup,
    required this.onReorderGroup,
    required this.onEditBook,
    required this.onOpenBook,
    required this.onLongPressBook,
  });

  final ShelfBookBlock block;
  final AppPalette palette;
  final bool selectionMode;
  final Set<String> selectedBookIds;
  final VoidCallback onDismiss;
  final Future<bool> Function(BookEntry book) onDeleteBook;
  final Future<bool> Function(BookEntry book) onRemoveBookFromGroup;
  final Future<bool> Function(BookEntry book) onMoveBookToGroup;
  final Future<List<BookEntry>?> Function(List<BookEntry> books) onReorderGroup;
  final ValueChanged<BookEntry> onEditBook;
  final ValueChanged<BookEntry> onOpenBook;
  final ValueChanged<BookEntry> onLongPressBook;

  @override
  State<BookBlockExpandedPanel> createState() => _BookBlockExpandedPanelState();
}

class _BookBlockExpandedPanelState extends State<BookBlockExpandedPanel> {
  final Set<String> _hiddenBookIds = {};
  late List<BookEntry> _books;
  bool _deleteMode = false;

  @override
  void initState() {
    super.initState();
    _books = [...widget.block.books];
  }

  @override
  void didUpdateWidget(covariant BookBlockExpandedPanel oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.block.key != widget.block.key ||
        oldWidget.block.books.length != widget.block.books.length) {
      _books = [...widget.block.books];
    }
  }

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.sizeOf(context);
    final block = widget.block;
    final palette = widget.palette;
    final visibleBooks = _books
        .where((book) => !_hiddenBookIds.contains(book.id))
        .toList();
    final panelHeight = math.min(size.height * .72, 620.0).toDouble();
    const horizontalPadding = 18.0;
    const headerHeight = 118.0;
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(22, 42, 22, 96),
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 540),
            child: Material(
              color: Colors.transparent,
              child: Container(
                height: panelHeight,
                decoration: BoxDecoration(
                  color: palette.surface.withValues(
                    alpha: palette.isLight ? .96 : .94,
                  ),
                  borderRadius: BorderRadius.circular(32),
                  border: Border.all(
                    color: palette.line.withValues(alpha: .28),
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withValues(
                        alpha: palette.isLight ? .14 : .34,
                      ),
                      blurRadius: 44,
                      offset: const Offset(0, 22),
                    ),
                  ],
                ),
                clipBehavior: Clip.antiAlias,
                child: Stack(
                  children: [
                    Positioned.fill(
                      child: ListView.separated(
                        padding: const EdgeInsets.fromLTRB(
                          horizontalPadding,
                          headerHeight + 18,
                          horizontalPadding,
                          16,
                        ),
                        physics: const ClampingScrollPhysics(),
                        itemCount: visibleBooks.length,
                        separatorBuilder: (_, _) => const SizedBox(height: 24),
                        itemBuilder: (context, index) {
                          final book = visibleBooks[index];
                          return _BookBlockRow(
                            book: book,
                            palette: palette,
                            selected: widget.selectedBookIds.contains(book.id),
                            selectionMode: widget.selectionMode,
                            deleteMode: _deleteMode,
                            onDelete: () => _deleteBook(book),
                            onRemoveFromGroup: () => _removeBookFromGroup(book),
                            onMoveToGroup: () => _moveBookToGroup(book),
                            onReorderGroup: () => _reorderGroup(visibleBooks),
                            onEditBook: () => widget.onEditBook(book),
                            onTap: () => widget.onOpenBook(book),
                            onLongPress: () {
                              if (widget.selectionMode) {
                                widget.onLongPressBook(book);
                              } else {
                                HapticFeedback.mediumImpact();
                                setState(() => _deleteMode = true);
                              }
                            },
                          );
                        },
                      ),
                    ),
                    Positioned(
                      left: 0,
                      right: 0,
                      top: 0,
                      height: headerHeight,
                      child: ClipRect(
                        child: BackdropFilter(
                          filter: ui.ImageFilter.blur(sigmaX: 16, sigmaY: 16),
                          child: Container(
                            padding: const EdgeInsets.fromLTRB(
                              horizontalPadding,
                              18,
                              horizontalPadding,
                              14,
                            ),
                            decoration: BoxDecoration(
                              color: palette.surface.withValues(
                                alpha: palette.isLight ? .74 : .66,
                              ),
                              gradient: LinearGradient(
                                begin: Alignment.topCenter,
                                end: Alignment.bottomCenter,
                                colors: [
                                  palette.surface.withValues(
                                    alpha: palette.isLight ? .90 : .82,
                                  ),
                                  palette.surface.withValues(
                                    alpha: palette.isLight ? .68 : .58,
                                  ),
                                ],
                              ),
                            ),
                            child: _BookBlockPanelHeader(
                              block: block,
                              palette: palette,
                              deleteMode: _deleteMode,
                              canReorder: visibleBooks.length > 1,
                              onReorder: () => _reorderGroup(visibleBooks),
                              onDismiss: _deleteMode
                                  ? () => setState(() => _deleteMode = false)
                                  : widget.onDismiss,
                            ),
                          ),
                        ),
                      ),
                    ),
                    Positioned(
                      left: 0,
                      right: 0,
                      top: headerHeight - 1,
                      child: Container(
                        height: 1,
                        color: palette.line.withValues(alpha: .18),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Future<void> _deleteBook(BookEntry book) async {
    final deleted = await widget.onDeleteBook(book);
    if (deleted && mounted) {
      setState(() => _hiddenBookIds.add(book.id));
    }
  }

  Future<void> _removeBookFromGroup(BookEntry book) async {
    final removed = await widget.onRemoveBookFromGroup(book);
    if (removed && mounted) {
      setState(() => _hiddenBookIds.add(book.id));
    }
  }

  Future<void> _moveBookToGroup(BookEntry book) async {
    final moved = await widget.onMoveBookToGroup(book);
    if (moved && mounted) {
      setState(() => _hiddenBookIds.add(book.id));
    }
  }

  Future<void> _reorderGroup(List<BookEntry> books) async {
    final reordered = await widget.onReorderGroup(books);
    if (reordered != null && mounted) {
      setState(() => _books = [...reordered]);
    }
  }
}

class _BookBlockPanelHeader extends StatelessWidget {
  const _BookBlockPanelHeader({
    required this.block,
    required this.palette,
    required this.deleteMode,
    required this.canReorder,
    required this.onReorder,
    required this.onDismiss,
  });

  final ShelfBookBlock block;
  final AppPalette palette;
  final bool deleteMode;
  final bool canReorder;
  final VoidCallback onReorder;
  final VoidCallback onDismiss;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        _BookCoverStack(
          books: block.books,
          palette: palette,
          width: 58,
          height: 80,
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                block.title,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  color: palette.text,
                  fontSize: 22,
                  height: 1.13,
                  fontWeight: AppTextWeight.semibold,
                  letterSpacing: -.35,
                ),
              ),
              const SizedBox(height: 7),
              Text(
                '${block.books.length} 本 · ${block.chapterCount} 章 · ${bookWordCountLabel(block.wordCount)}',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  color: palette.muted,
                  fontSize: 12.5,
                  height: 1.15,
                ),
              ),
              const SizedBox(height: 8),
              _ShelfProgressLine(
                progress: block.progress.clamp(0, 1),
                palette: palette,
              ),
            ],
          ),
        ),
        const SizedBox(width: 12),
        SizedBox(
          width: 38,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              AnimatedScale(
                duration: const Duration(milliseconds: 180),
                curve: Curves.easeOutBack,
                scale: canReorder && !deleteMode ? 1 : .82,
                child: AnimatedOpacity(
                  duration: const Duration(milliseconds: 150),
                  opacity: canReorder && !deleteMode ? 1 : 0,
                  child: IgnorePointer(
                    ignoring: !canReorder || deleteMode,
                    child: InkWell(
                      borderRadius: BorderRadius.circular(999),
                      onTap: () {
                        HapticFeedback.selectionClick();
                        onReorder();
                      },
                      child: Container(
                        width: 38,
                        height: 38,
                        decoration: BoxDecoration(
                          color: palette.card.withValues(alpha: .88),
                          shape: BoxShape.circle,
                        ),
                        child: Icon(
                          Icons.swap_vert_rounded,
                          color: palette.muted,
                          size: 22,
                        ),
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 6),
              InkWell(
                borderRadius: BorderRadius.circular(999),
                onTap: onDismiss,
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 180),
                  curve: Curves.easeOutCubic,
                  width: 38,
                  height: 38,
                  decoration: BoxDecoration(
                    color: deleteMode
                        ? palette.primarySoft.withValues(alpha: .26)
                        : palette.card,
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    Icons.close_rounded,
                    color: deleteMode ? palette.accentText : palette.muted,
                    size: 23,
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _BookDeleteBadge extends StatelessWidget {
  const _BookDeleteBadge({required this.palette, required this.onTap});

  final AppPalette palette;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(999),
      onTap: () {
        HapticFeedback.selectionClick();
        onTap();
      },
      child: Container(
        width: 24,
        height: 24,
        decoration: BoxDecoration(
          color: palette.surface.withValues(alpha: .96),
          shape: BoxShape.circle,
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: .22),
              blurRadius: 12,
              offset: const Offset(0, 5),
            ),
          ],
        ),
        child: Icon(Icons.close_rounded, size: 17, color: palette.accentText),
      ),
    );
  }
}

class _BookRowMoreButton extends StatelessWidget {
  const _BookRowMoreButton({
    required this.palette,
    required this.onRemoveFromGroup,
    required this.onMoveToGroup,
    required this.onReorderGroup,
    required this.onEditBook,
  });

  final AppPalette palette;
  final VoidCallback onRemoveFromGroup;
  final VoidCallback onMoveToGroup;
  final VoidCallback onReorderGroup;
  final VoidCallback onEditBook;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(999),
      onTap: () {
        HapticFeedback.selectionClick();
        _showMenu(context);
      },
      child: SizedBox(
        width: 34,
        height: 34,
        child: Icon(Icons.more_vert_rounded, color: palette.muted, size: 23),
      ),
    );
  }

  void _showMenu(BuildContext buttonContext) {
    final overlay = Overlay.of(buttonContext).context.findRenderObject();
    final button = buttonContext.findRenderObject();
    if (overlay is! RenderBox || button is! RenderBox) {
      return;
    }
    final media = MediaQuery.of(buttonContext);
    final topLeft = button.localToGlobal(Offset.zero, ancestor: overlay);
    const menuWidth = 206.0;
    const horizontalMargin = 14.0;
    final left = (topLeft.dx + button.size.width - menuWidth).clamp(
      horizontalMargin,
      media.size.width - menuWidth - horizontalMargin,
    );
    final top = (topLeft.dy + button.size.height + 6).clamp(
      media.padding.top + 8,
      media.size.height - 132 - media.padding.bottom,
    );

    showGeneralDialog<BookBlockBookMenuAction>(
      context: buttonContext,
      barrierDismissible: true,
      barrierLabel: '关闭',
      barrierColor: Colors.transparent,
      transitionDuration: const Duration(milliseconds: 180),
      pageBuilder: (context, _, _) {
        return Stack(
          children: [
            Positioned(
              left: left,
              top: top,
              width: menuWidth,
              child: _BookRowInlineMenu(
                palette: palette,
                onSelected: (action) => Navigator.pop(context, action),
              ),
            ),
          ],
        );
      },
      transitionBuilder: (context, animation, secondaryAnimation, child) {
        final curved = CurvedAnimation(
          parent: animation,
          curve: Curves.easeOutBack,
          reverseCurve: Curves.easeInCubic,
        );
        return FadeTransition(
          opacity: animation,
          child: ScaleTransition(
            alignment: Alignment.topRight,
            scale: Tween<double>(begin: .88, end: 1).animate(curved),
            child: child,
          ),
        );
      },
    ).then((action) {
      if (action == null) {
        return;
      }
      HapticFeedback.selectionClick();
      switch (action) {
        case BookBlockBookMenuAction.removeFromGroup:
          onRemoveFromGroup();
        case BookBlockBookMenuAction.moveToGroup:
          onMoveToGroup();
        case BookBlockBookMenuAction.edit:
          onEditBook();
      }
    });
  }
}

class _BookRowInlineMenu extends StatelessWidget {
  const _BookRowInlineMenu({required this.palette, required this.onSelected});

  final AppPalette palette;
  final ValueChanged<BookBlockBookMenuAction> onSelected;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 8),
        decoration: BoxDecoration(
          color: palette.surface.withValues(alpha: palette.isLight ? .96 : .94),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: palette.line.withValues(alpha: .24)),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(
                alpha: palette.isLight ? .14 : .34,
              ),
              blurRadius: 28,
              offset: const Offset(0, 14),
            ),
          ],
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            _BookRowInlineMenuItem(
              palette: palette,
              icon: Icons.remove_circle_outline_rounded,
              title: '从分组移出',
              onTap: () => onSelected(BookBlockBookMenuAction.removeFromGroup),
            ),
            _BookRowInlineMenuItem(
              palette: palette,
              icon: Icons.drive_file_move_rounded,
              title: '移动到其它分组',
              onTap: () => onSelected(BookBlockBookMenuAction.moveToGroup),
            ),
            _BookRowInlineMenuItem(
              palette: palette,
              icon: Icons.edit_rounded,
              title: '编辑书籍',
              onTap: () => onSelected(BookBlockBookMenuAction.edit),
            ),
          ],
        ),
      ),
    );
  }
}

class _BookRowInlineMenuItem extends StatelessWidget {
  const _BookRowInlineMenuItem({
    required this.palette,
    required this.icon,
    required this.title,
    required this.onTap,
  });

  final AppPalette palette;
  final IconData icon;
  final String title;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 10),
        child: Row(
          children: [
            Icon(icon, size: 20, color: palette.accentText),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                title,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  color: palette.text,
                  fontSize: 14.2,
                  fontWeight: AppTextWeight.medium,
                ),
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
    required this.deleteMode,
    required this.onDelete,
    required this.onRemoveFromGroup,
    required this.onMoveToGroup,
    required this.onReorderGroup,
    required this.onEditBook,
    required this.onTap,
    required this.onLongPress,
  });

  final BookEntry book;
  final AppPalette palette;
  final bool selected;
  final bool selectionMode;
  final bool deleteMode;
  final VoidCallback onDelete;
  final VoidCallback onRemoveFromGroup;
  final VoidCallback onMoveToGroup;
  final VoidCallback onReorderGroup;
  final VoidCallback onEditBook;
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
          SizedBox(
            width: 76,
            height: 108,
            child: Stack(
              clipBehavior: Clip.none,
              children: [
                BookCover(
                  book: book,
                  palette: palette,
                  width: 76,
                  height: 108,
                  radius: 13,
                  hero: true,
                ),
                Positioned(
                  left: -6,
                  top: -6,
                  child: AnimatedScale(
                    scale: deleteMode ? 1 : .55,
                    duration: const Duration(milliseconds: 180),
                    curve: Curves.easeOutBack,
                    child: AnimatedOpacity(
                      opacity: deleteMode ? 1 : 0,
                      duration: const Duration(milliseconds: 130),
                      child: IgnorePointer(
                        ignoring: !deleteMode,
                        child: _BookDeleteBadge(
                          palette: palette,
                          onTap: onDelete,
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
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
                              )
                            else
                              AnimatedOpacity(
                                opacity: deleteMode ? 0 : 1,
                                duration: const Duration(milliseconds: 150),
                                child: IgnorePointer(
                                  ignoring: deleteMode,
                                  child: _BookRowMoreButton(
                                    palette: palette,
                                    onRemoveFromGroup: onRemoveFromGroup,
                                    onMoveToGroup: onMoveToGroup,
                                    onReorderGroup: onReorderGroup,
                                    onEditBook: onEditBook,
                                  ),
                                ),
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
                        _BookMetaLine(
                          book: book,
                          palette: palette,
                          fontSize: 12.5,
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
    this.spread = 18,
    this.yStep = 3,
  });

  final List<BookEntry> books;
  final AppPalette palette;
  final double width;
  final double height;
  final double spread;
  final double yStep;

  @override
  Widget build(BuildContext context) {
    final visible = books.take(3).toList();
    return SizedBox(
      width: width + (visible.length - 1) * spread,
      height: height,
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          for (var i = visible.length - 1; i >= 0; i--)
            Positioned(
              left: i * spread,
              top: i * yStep,
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
                          _BookMetaLine(
                            book: book,
                            palette: palette,
                            fontSize: 13,
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

class _BookMetaLine extends StatelessWidget {
  const _BookMetaLine({
    required this.book,
    required this.palette,
    required this.fontSize,
  });

  final BookEntry book;
  final AppPalette palette;
  final double fontSize;

  @override
  Widget build(BuildContext context) {
    final label =
        '${book.formatLabel}  ·  ${book.chapters.length}章  ·  ${bookWordCountLabel(book.wordCount)}';
    return Text(
      label,
      maxLines: 1,
      overflow: TextOverflow.ellipsis,
      style: TextStyle(
        color: palette.muted.withValues(alpha: palette.isLight ? .82 : .88),
        fontSize: fontSize,
        height: 1.15,
        letterSpacing: .1,
        fontFeatures: const [
          ui.FontFeature.tabularFigures(),
          ui.FontFeature.proportionalFigures(),
        ],
        fontWeight: AppTextWeight.regular,
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

class ShelfTabs extends StatefulWidget {
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
  State<ShelfTabs> createState() => _ShelfTabsState();
}

class _ShelfTabsState extends State<ShelfTabs> {
  late final ScrollController _controller;

  @override
  void initState() {
    super.initState();
    _controller = ScrollController();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _scrollSelectedIntoView(animated: false);
    });
  }

  @override
  void didUpdateWidget(covariant ShelfTabs oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.selectedIndex != widget.selectedIndex ||
        oldWidget.shelves.length != widget.shelves.length) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _scrollSelectedIntoView(animated: true);
      });
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _scrollSelectedIntoView({required bool animated}) {
    if (!mounted || !_controller.hasClients || widget.shelves.isEmpty) {
      return;
    }
    final selectedIndex = widget.selectedIndex
        .clamp(0, widget.shelves.length - 1)
        .toInt();
    final maxScroll = _controller.position.maxScrollExtent;
    final viewport = _controller.position.viewportDimension;
    final itemCenter =
        selectedIndex * ShelfTabs._itemWidth + ShelfTabs._itemWidth / 2;
    final target = selectedIndex == 0
        ? 0.0
        : selectedIndex == widget.shelves.length - 1
        ? maxScroll
        : (itemCenter - viewport / 2).clamp(0.0, maxScroll).toDouble();
    if ((_controller.offset - target).abs() < .5) {
      return;
    }
    if (!animated) {
      _controller.jumpTo(target);
      return;
    }
    _controller.animateTo(
      target,
      duration: const Duration(milliseconds: 320),
      curve: Curves.easeOutCubic,
    );
  }

  @override
  Widget build(BuildContext context) {
    final totalWidth = widget.shelves.length * ShelfTabs._itemWidth;
    final left =
        widget.selectedIndex * ShelfTabs._itemWidth +
        (ShelfTabs._itemWidth - ShelfTabs._indicatorWidth) / 2;
    return SingleChildScrollView(
      controller: _controller,
      scrollDirection: Axis.horizontal,
      child: SizedBox(
        width: totalWidth,
        height: 52,
        child: Stack(
          children: [
            Positioned(
              left: 0,
              right: 0,
              bottom: 4,
              child: Container(
                height: 1,
                color: widget.palette.line.withValues(
                  alpha: widget.palette.isLight ? .34 : .28,
                ),
              ),
            ),
            Row(
              children: [
                for (var i = 0; i < widget.shelves.length; i++)
                  GestureDetector(
                    behavior: HitTestBehavior.opaque,
                    onTap: () {
                      HapticFeedback.selectionClick();
                      widget.onChanged(i);
                    },
                    child: SizedBox(
                      width: ShelfTabs._itemWidth,
                      height: 44,
                      child: Center(
                        child: Text(
                          widget.shelves[i] == defaultShelfName
                              ? defaultShelfLabel
                              : widget.shelves[i],
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            color: i == widget.selectedIndex
                                ? widget.palette.accentText
                                : widget.palette.muted,
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
                key: ValueKey(widget.selectedIndex),
                tween: Tween(begin: 1.45, end: 1),
                duration: const Duration(milliseconds: 330),
                curve: Curves.easeOutCubic,
                builder: (context, scale, child) {
                  return Transform.scale(scaleX: scale, child: child);
                },
                child: Container(
                  width: ShelfTabs._indicatorWidth,
                  height: 4,
                  decoration: BoxDecoration(
                    color: widget.palette.accentText,
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
