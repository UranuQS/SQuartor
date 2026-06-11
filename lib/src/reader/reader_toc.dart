import 'package:flutter/material.dart';

import '../models.dart';
import '../typography.dart';
import 'reader_glass_palette.dart';
import 'reader_menu.dart';

// ---------------------------------------------------------------------------
// ReaderTocDrawer
// ---------------------------------------------------------------------------

class ReaderTocDrawer extends StatefulWidget {
  const ReaderTocDrawer({
    super.key,
    required this.book,
    required this.chapterIndex,
    required this.currentPageCount,
    required this.cachedPageCounts,
    required this.palette,
    required this.onChapterSelected,
  });

  final BookEntry book;
  final int chapterIndex;
  final int currentPageCount;
  final Map<int, int> cachedPageCounts;
  final AppPalette palette;
  final ValueChanged<int> onChapterSelected;

  @override
  State<ReaderTocDrawer> createState() => _ReaderTocDrawerState();
}

class _ReaderTocDrawerState extends State<ReaderTocDrawer> {
  static const _tocItemExtent = 40.0;

  late final ScrollController _scrollController;
  late final Set<int> _expanded;

  @override
  void initState() {
    super.initState();
    _expanded = _currentPath();
    final currentVisibleIndex = _visibleChapterIndices().indexOf(
      widget.chapterIndex,
    );
    _scrollController = ScrollController(
      initialScrollOffset: ((currentVisibleIndex - 1) * _tocItemExtent).clamp(
        0,
        double.infinity,
      ),
    );
  }

  bool _hasChildren(int index) {
    return index + 1 < widget.book.chapters.length &&
        widget.book.chapters[index + 1].tocDepth >
            widget.book.chapters[index].tocDepth;
  }

  Set<int> _currentPath() {
    final result = <int>{};
    if (widget.book.chapters.isEmpty) {
      return result;
    }
    var targetDepth = widget.book.chapters[widget.chapterIndex].tocDepth - 1;
    for (var i = widget.chapterIndex - 1; i >= 0 && targetDepth >= 0; i--) {
      if (widget.book.chapters[i].tocDepth == targetDepth) {
        result.add(i);
        targetDepth -= 1;
      }
    }
    if (_hasChildren(widget.chapterIndex)) {
      result.add(widget.chapterIndex);
    }
    return result;
  }

  List<int> _visibleChapterIndices() {
    final result = <int>[];
    int? collapsedDepth;
    for (var i = 0; i < widget.book.chapters.length; i++) {
      final depth = widget.book.chapters[i].tocDepth;
      if (collapsedDepth != null) {
        if (depth > collapsedDepth) {
          continue;
        }
        collapsedDepth = null;
      }
      result.add(i);
      if (_hasChildren(i) && !_expanded.contains(i)) {
        collapsedDepth = depth;
      }
    }
    return result;
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final progress = widget.book.chapters.isEmpty
        ? 0.0
        : (widget.chapterIndex + 1) / widget.book.chapters.length;
    final visibleIndices = _visibleChapterIndices();
    final glass = ReaderGlassPalette.from(widget.palette);
    return Material(
      color: Colors.transparent,
      elevation: 0,
      shadowColor: Colors.transparent,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(28),
        child: ColoredBox(
          color: Colors.transparent,
          child: Column(
            children: [
              DecoratedBox(
                decoration: BoxDecoration(
                  color: glass.panel.withValues(alpha: glass.dark ? .42 : .38),
                ),
                child: Column(
                  children: [
                    Padding(
                      padding: const EdgeInsets.fromLTRB(20, 18, 12, 12),
                      child: Row(
                        children: [
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  widget.book.title,
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: TextStyle(
                                    color: glass.text,
                                    fontSize: 24,
                                    fontWeight: AppTextWeight.semibold,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 20),
                      child: ReaderProgressTrack(
                        progress: progress,
                        trackColor: glass.line.withValues(alpha: .45),
                        fillColor: widget.palette.primary,
                        thumbColor: widget.palette.primarySoft,
                        height: 6,
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.fromLTRB(20, 8, 20, 12),
                      child: Align(
                        alignment: Alignment.centerRight,
                        child: Text(
                          '\u7b2c ${widget.chapterIndex + 1} / ${widget.book.chapters.length} \u7ae0',
                          style: TextStyle(color: glass.muted, fontSize: 12),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              Expanded(
                child: Stack(
                  children: [
                    ListView.builder(
                      controller: _scrollController,
                      itemExtent: _tocItemExtent,
                      padding: const EdgeInsets.fromLTRB(16, 8, 42, 18),
                      itemCount: visibleIndices.length,
                      itemBuilder: (context, visibleIndex) {
                        final index = visibleIndices[visibleIndex];
                        final chapter = widget.book.chapters[index];
                        final selected = index == widget.chapterIndex;
                        final hasChildren = _hasChildren(index);
                        if (widget.book.chapters.isNotEmpty) {
                          return ReaderTocEntry(
                            chapter: chapter,
                            selected: selected,
                            hasChildren: hasChildren,
                            expanded: _expanded.contains(index),
                            palette: widget.palette,
                            glass: glass,
                            onToggle: hasChildren
                                ? () {
                                    setState(() {
                                      if (!_expanded.remove(index)) {
                                        _expanded.add(index);
                                      }
                                    });
                                  }
                                : null,
                            onSelected: () => widget.onChapterSelected(index),
                          );
                        }
                        return const SizedBox.shrink();
                      },
                    ),
                    Positioned(
                      right: 4,
                      top: 14,
                      bottom: 22,
                      child: TocFastScroller(
                        controller: _scrollController,
                        visibleIndices: visibleIndices,
                        totalCount: widget.book.chapters.length,
                        itemExtent: _tocItemExtent,
                        palette: widget.palette,
                        glass: glass,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// TocFastScroller
// ---------------------------------------------------------------------------

class TocFastScroller extends StatefulWidget {
  const TocFastScroller({
    super.key,
    required this.controller,
    required this.visibleIndices,
    required this.totalCount,
    required this.itemExtent,
    required this.palette,
    required this.glass,
  });

  final ScrollController controller;
  final List<int> visibleIndices;
  final int totalCount;
  final double itemExtent;
  final AppPalette palette;
  final ReaderGlassPalette glass;

  @override
  State<TocFastScroller> createState() => _TocFastScrollerState();
}

class _TocFastScrollerState extends State<TocFastScroller> {
  static const _thumbHeight = 46.0;

  bool _dragging = false;
  int? _previewVisibleIndex;

  void _seekFromY(double localY, double height) {
    if (!widget.controller.hasClients || widget.visibleIndices.isEmpty) {
      return;
    }
    final usableHeight = (height - _thumbHeight).clamp(1.0, double.infinity);
    final ratio = ((localY - _thumbHeight / 2) / usableHeight).clamp(0.0, 1.0);
    final position = widget.controller.position;
    final target = (ratio * position.maxScrollExtent).clamp(
      position.minScrollExtent,
      position.maxScrollExtent,
    );
    widget.controller.jumpTo(target.toDouble());
    final visibleIndex = (target / widget.itemExtent)
        .round()
        .clamp(0, widget.visibleIndices.length - 1)
        .toInt();
    setState(() => _previewVisibleIndex = visibleIndex);
  }

  @override
  Widget build(BuildContext context) {
    if (widget.visibleIndices.length < 18) {
      return const SizedBox.shrink();
    }
    final glass = widget.glass;
    return LayoutBuilder(
      builder: (context, constraints) {
        final height = constraints.maxHeight;
        return GestureDetector(
          behavior: HitTestBehavior.translucent,
          onPanStart: (details) {
            setState(() => _dragging = true);
            _seekFromY(details.localPosition.dy, height);
          },
          onPanUpdate: (details) =>
              _seekFromY(details.localPosition.dy, height),
          onPanEnd: (_) => setState(() => _dragging = false),
          onPanCancel: () => setState(() => _dragging = false),
          child: SizedBox(
            width: 54,
            height: double.infinity,
            child: Stack(
              clipBehavior: Clip.none,
              children: [
                Positioned(
                  right: 21,
                  top: 0,
                  bottom: 0,
                  child: DecoratedBox(
                    decoration: BoxDecoration(
                      color: glass.line.withValues(alpha: .42),
                      borderRadius: BorderRadius.circular(999),
                    ),
                    child: const SizedBox(width: 4),
                  ),
                ),
                AnimatedBuilder(
                  animation: widget.controller,
                  builder: (context, _) {
                    final top = _thumbTop(height);
                    return Positioned(
                      right: 10,
                      top: top,
                      child: AnimatedContainer(
                        duration: const Duration(milliseconds: 120),
                        width: _dragging ? 28 : 24,
                        height: _thumbHeight,
                        decoration: BoxDecoration(
                          color: _dragging
                              ? widget.palette.primary
                              : glass.pill,
                          borderRadius: BorderRadius.circular(999),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withValues(
                                alpha: glass.dark ? .24 : .10,
                              ),
                              blurRadius: 12,
                              offset: const Offset(0, 4),
                            ),
                          ],
                        ),
                        child: Icon(
                          Icons.unfold_more_rounded,
                          size: 18,
                          color: _dragging ? Colors.white : glass.muted,
                        ),
                      ),
                    );
                  },
                ),
                if (_dragging && _previewVisibleIndex != null)
                  Positioned(
                    right: 46,
                    top: (_thumbTop(height) - 8).clamp(
                      0.0,
                      (height - 38).clamp(0.0, double.infinity),
                    ),
                    child: DecoratedBox(
                      decoration: BoxDecoration(
                        color: glass.panel,
                        borderRadius: BorderRadius.circular(16),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withValues(
                              alpha: glass.dark ? .28 : .12,
                            ),
                            blurRadius: 16,
                            offset: const Offset(0, 6),
                          ),
                        ],
                      ),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 10,
                          vertical: 7,
                        ),
                        child: Text(
                          _label,
                          maxLines: 1,
                          style: TextStyle(
                            color: glass.text,
                            fontSize: 12,
                            fontWeight: AppTextWeight.medium,
                          ),
                        ),
                      ),
                    ),
                  ),
              ],
            ),
          ),
        );
      },
    );
  }

  double _thumbTop(double height) {
    if (!widget.controller.hasClients || height <= _thumbHeight) {
      return 0;
    }
    final position = widget.controller.position;
    final maxScroll = position.maxScrollExtent;
    final ratio = maxScroll <= 0 ? 0.0 : (position.pixels / maxScroll);
    return ((height - _thumbHeight) * ratio).clamp(0.0, height - _thumbHeight);
  }

  String get _label {
    final visibleIndex = _previewVisibleIndex;
    if (visibleIndex == null || widget.visibleIndices.isEmpty) {
      return '';
    }
    final safeIndex = visibleIndex
        .clamp(0, widget.visibleIndices.length - 1)
        .toInt();
    final chapter = widget.visibleIndices[safeIndex];
    return '\u7b2c ${chapter + 1} / ${widget.totalCount} \u7ae0';
  }
}

// ---------------------------------------------------------------------------
// ReaderTocEntry
// ---------------------------------------------------------------------------

class ReaderTocEntry extends StatelessWidget {
  const ReaderTocEntry({
    super.key,
    required this.chapter,
    required this.selected,
    required this.hasChildren,
    required this.expanded,
    required this.palette,
    required this.glass,
    required this.onToggle,
    required this.onSelected,
  });

  final ReaderChapter chapter;
  final bool selected;
  final bool hasChildren;
  final bool expanded;
  final AppPalette palette;
  final ReaderGlassPalette glass;
  final VoidCallback? onToggle;
  final VoidCallback onSelected;

  @override
  Widget build(BuildContext context) {
    final depth = chapter.tocDepth.clamp(0, 6).toInt();
    final textTone = selected
        ? glass.text
        : Color.lerp(glass.text, glass.muted, (depth * .18).clamp(0.0, .76))!;
    final rowColor = selected
        ? palette.primary.withValues(alpha: glass.dark ? .26 : .15)
        : Colors.transparent;
    return Padding(
      padding: EdgeInsets.only(left: depth * 12.0, top: 1, bottom: 1),
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: rowColor,
          borderRadius: BorderRadius.circular(15),
        ),
        child: Row(
          children: [
            Padding(
              padding: const EdgeInsets.only(left: 5),
              child: SizedBox(
                width: 24,
                height: 24,
                child: hasChildren
                    ? IconButton(
                        padding: EdgeInsets.zero,
                        visualDensity: VisualDensity.compact,
                        style: IconButton.styleFrom(
                          backgroundColor: selected
                              ? palette.primarySoft.withValues(
                                  alpha: glass.dark ? .24 : .34,
                                )
                              : glass.pill,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(11),
                          ),
                        ),
                        onPressed: onToggle,
                        icon: Icon(
                          expanded
                              ? Icons.keyboard_arrow_down_rounded
                              : Icons.keyboard_arrow_right_rounded,
                          color: selected ? palette.primarySoft : glass.muted,
                          size: 18,
                        ),
                      )
                    : Center(
                        child: AnimatedContainer(
                          duration: const Duration(milliseconds: 180),
                          width: selected ? 13 : 5,
                          height: selected ? 13 : 5,
                          decoration: BoxDecoration(
                            color: selected
                                ? palette.primarySoft
                                : glass.subtle.withValues(alpha: .72),
                            shape: BoxShape.circle,
                          ),
                          child: selected
                              ? Icon(
                                  Icons.bookmark_rounded,
                                  size: 8,
                                  color: glass.dark
                                      ? const Color(0xFF1B1019)
                                      : Colors.white,
                                )
                              : null,
                        ),
                      ),
              ),
            ),
            Expanded(
              child: InkWell(
                borderRadius: BorderRadius.circular(15),
                onTap: onSelected,
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(6, 2, 8, 2),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text(
                          chapter.title,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            color: textTone,
                            fontSize: depth == 0 ? 12.9 : 12.0,
                            height: 1.0,
                            fontWeight: selected || depth == 0
                                ? AppTextWeight.medium
                                : AppTextWeight.regular,
                          ),
                        ),
                      ),
                    ],
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
