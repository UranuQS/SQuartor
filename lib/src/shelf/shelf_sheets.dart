import 'dart:math' as math;
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../models.dart';
import '../typography.dart';
import '../widgets/book_cover.dart';
import 'shelf_enums.dart';

Future<T?> showShelfFloatingSheet<T>({
  required BuildContext context,
  required AppPalette palette,
  required Widget child,
  bool isDismissible = true,
}) {
  return showModalBottomSheet<T>(
    context: context,
    isScrollControlled: true,
    isDismissible: isDismissible,
    showDragHandle: false,
    backgroundColor: Colors.transparent,
    elevation: 0,
    barrierColor: Colors.black.withValues(alpha: .54),
    builder: (context) {
      final insets = MediaQuery.viewInsetsOf(context);
      return SafeArea(
        top: false,
        child: Padding(
          padding: EdgeInsets.fromLTRB(20, 0, 20, 20 + insets.bottom),
          child: _ShelfFloatingSheet(palette: palette, child: child),
        ),
      );
    },
  );
}

Future<T?> showShelfFollowerMenu<T>({
  required BuildContext context,
  required LayerLink anchorLink,
  required AppPalette palette,
  required Widget child,
  double width = 318,
}) {
  return showGeneralDialog<T>(
    context: context,
    barrierDismissible: true,
    barrierLabel: '\u5173\u95ed',
    barrierColor: Colors.black.withValues(alpha: .18),
    transitionDuration: const Duration(milliseconds: 180),
    pageBuilder: (context, _, _) {
      return Stack(
        children: [
          CompositedTransformFollower(
            link: anchorLink,
            showWhenUnlinked: false,
            targetAnchor: Alignment.bottomRight,
            followerAnchor: Alignment.topRight,
            offset: const Offset(0, 12),
            child: _ShelfFloatingSurface(
              palette: palette,
              maxWidth: width,
              child: child,
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
          alignment: Alignment.topCenter,
          scale: Tween<double>(begin: .88, end: 1).animate(curved),
          child: child,
        ),
      );
    },
  );
}

Future<T?> showShelfAnchoredMenu<T>({
  required BuildContext context,
  required BuildContext anchorContext,
  required AppPalette palette,
  required Widget child,
  double width = 318,
}) {
  final overlay = Overlay.of(anchorContext).context.findRenderObject();
  final anchor = anchorContext.findRenderObject();
  if (overlay is! RenderBox || anchor is! RenderBox) {
    return showShelfFloatingSheet<T>(
      context: context,
      palette: palette,
      child: child,
    );
  }

  final anchorRect = MatrixUtils.transformRect(
    anchor.getTransformTo(overlay),
    Offset.zero & anchor.size,
  );

  return showGeneralDialog<T>(
    context: context,
    barrierDismissible: true,
    barrierLabel: '关闭',
    barrierColor: Colors.black.withValues(alpha: .18),
    transitionDuration: const Duration(milliseconds: 180),
    pageBuilder: (context, _, _) {
      return _ShelfAnchoredMenuLayer(
        anchorRect: anchorRect,
        maxWidth: width,
        palette: palette,
        child: child,
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
          alignment: Alignment.center,
          scale: Tween<double>(begin: .88, end: 1).animate(curved),
          child: child,
        ),
      );
    },
  );
}

Future<T?> showShelfFloatingDialog<T>({
  required BuildContext context,
  required AppPalette palette,
  required Widget child,
}) {
  return showGeneralDialog<T>(
    context: context,
    barrierDismissible: false,
    barrierLabel: '关闭',
    barrierColor: Colors.black.withValues(alpha: .54),
    transitionDuration: const Duration(milliseconds: 260),
    pageBuilder: (context, _, _) {
      final insets = MediaQuery.viewInsetsOf(context);
      return SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: EdgeInsets.fromLTRB(22, 22, 22, 22 + insets.bottom),
            child: _ShelfFloatingSurface(
              palette: palette,
              maxWidth: 560,
              child: child,
            ),
          ),
        ),
      );
    },
    transitionBuilder: (context, animation, secondaryAnimation, child) {
      final curved = CurvedAnimation(
        parent: animation,
        curve: Curves.easeOutCubic,
        reverseCurve: Curves.easeInCubic,
      );
      return FadeTransition(
        opacity: curved,
        child: SlideTransition(
          position: Tween(
            begin: const Offset(0, .22),
            end: Offset.zero,
          ).animate(curved),
          child: child,
        ),
      );
    },
  );
}

class _ShelfAnchoredMenuLayer extends StatelessWidget {
  const _ShelfAnchoredMenuLayer({
    required this.anchorRect,
    required this.maxWidth,
    required this.palette,
    required this.child,
  });

  final Rect anchorRect;
  final double maxWidth;
  final AppPalette palette;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final media = MediaQuery.of(context);
    return CustomSingleChildLayout(
      delegate: _ShelfAnchoredMenuLayoutDelegate(
        anchorRect: anchorRect,
        padding: media.padding,
        maxWidth: maxWidth,
      ),
      child: _ShelfFloatingSurface(
        palette: palette,
        maxWidth: maxWidth,
        child: child,
      ),
    );
  }
}

class _ShelfAnchoredMenuLayoutDelegate extends SingleChildLayoutDelegate {
  const _ShelfAnchoredMenuLayoutDelegate({
    required this.anchorRect,
    required this.padding,
    required this.maxWidth,
  });

  final Rect anchorRect;
  final EdgeInsets padding;
  final double maxWidth;

  EdgeInsets get _gutter {
    final inset = kMinInteractiveDimension / 3;
    return EdgeInsets.fromLTRB(
      padding.left + inset,
      padding.top + inset,
      padding.right + inset,
      padding.bottom + inset,
    );
  }

  @override
  BoxConstraints getConstraintsForChild(BoxConstraints constraints) {
    final gutter = _gutter;
    final width = math.min<double>(
      maxWidth,
      math.max<double>(0, constraints.maxWidth - gutter.horizontal),
    );
    return BoxConstraints(
      minWidth: width,
      maxWidth: width,
      maxHeight: math.max(0, constraints.maxHeight - gutter.vertical),
    );
  }

  @override
  Offset getPositionForChild(Size size, Size childSize) {
    final gutter = _gutter;
    final safeRect = Rect.fromLTRB(
      gutter.left,
      gutter.top,
      size.width - gutter.right,
      size.height - gutter.bottom,
    );
    final preferred = Offset(
      anchorRect.center.dx - childSize.width / 2,
      anchorRect.center.dy - childSize.height / 2,
    );
    final dx = preferred.dx.clamp(
      safeRect.left,
      safeRect.right - childSize.width,
    );
    final dy = preferred.dy.clamp(
      safeRect.top,
      safeRect.bottom - childSize.height,
    );
    return Offset(dx.toDouble(), dy.toDouble());
  }

  @override
  bool shouldRelayout(covariant _ShelfAnchoredMenuLayoutDelegate oldDelegate) {
    return anchorRect != oldDelegate.anchorRect ||
        padding != oldDelegate.padding ||
        maxWidth != oldDelegate.maxWidth;
  }
}

class _ShelfFollowerMenuSurface<T> extends StatefulWidget {
  const _ShelfFollowerMenuSurface({
    required this.palette,
    required this.maxWidth,
    required this.child,
    required this.onSelected,
  });

  final AppPalette palette;
  final double maxWidth;
  final Widget child;
  final ValueChanged<T?> onSelected;

  @override
  State<_ShelfFollowerMenuSurface<T>> createState() =>
      _ShelfFollowerMenuSurfaceState<T>();
}

class _ShelfFollowerMenuSurfaceState<T>
    extends State<_ShelfFollowerMenuSurface<T>>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  late final Animation<double> _scale;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 180),
      reverseDuration: const Duration(milliseconds: 140),
    )..forward();
    _scale = CurvedAnimation(
      parent: _controller,
      curve: Curves.easeOutBack,
      reverseCurve: Curves.easeInCubic,
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      type: MaterialType.transparency,
      child: ScaleTransition(
        alignment: Alignment.topCenter,
        scale: Tween<double>(begin: .88, end: 1).animate(_scale),
        child: _ShelfMenuResultScope<T>(
          onSelected: widget.onSelected,
          child: _ShelfFloatingSurface(
            palette: widget.palette,
            maxWidth: widget.maxWidth,
            child: widget.child,
          ),
        ),
      ),
    );
  }
}

class _ShelfMenuResultScope<T> extends StatelessWidget {
  const _ShelfMenuResultScope({required this.onSelected, required this.child});

  final ValueChanged<T?> onSelected;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Navigator(
      onGenerateRoute: (_) => PageRouteBuilder<T>(
        opaque: false,
        pageBuilder: (routeContext, animation, secondaryAnimation) {
          return _ShelfMenuResultBridge<T>(
            onSelected: onSelected,
            child: child,
          );
        },
      ),
    );
  }
}

class _ShelfMenuResultBridge<T> extends StatelessWidget {
  const _ShelfMenuResultBridge({required this.onSelected, required this.child});

  final ValueChanged<T?> onSelected;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return PopScope<T>(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) => onSelected(result),
      child: child,
    );
  }
}

class _ShelfFloatingSheet extends StatelessWidget {
  const _ShelfFloatingSheet({required this.palette, required this.child});

  final AppPalette palette;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return _ShelfFloatingSurface(
      palette: palette,
      maxWidth: 560,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 42,
            height: 5,
            margin: const EdgeInsets.only(top: 12, bottom: 12),
            decoration: BoxDecoration(
              color: palette.muted.withValues(alpha: .72),
              borderRadius: BorderRadius.circular(999),
            ),
          ),
          child,
        ],
      ),
    );
  }
}

class _ShelfFloatingSurface extends StatelessWidget {
  const _ShelfFloatingSurface({
    required this.palette,
    required this.child,
    required this.maxWidth,
  });

  final AppPalette palette;
  final Widget child;
  final double maxWidth;

  @override
  Widget build(BuildContext context) {
    return ConstrainedBox(
      constraints: BoxConstraints(maxWidth: maxWidth),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(28),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 20, sigmaY: 20),
          child: Material(
            color: palette.surface.withValues(
              alpha: palette.isLight ? .76 : .82,
            ),
            elevation: palette.isLight ? 1 : 2,
            shadowColor: Colors.black.withValues(
              alpha: palette.isLight ? .12 : .28,
            ),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(28),
            ),
            clipBehavior: Clip.antiAlias,
            child: child,
          ),
        ),
      ),
    );
  }
}

class ShelfActionList extends StatelessWidget {
  const ShelfActionList({
    super.key,
    required this.palette,
    required this.children,
    this.title,
  });

  final AppPalette palette;
  final String? title;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 22),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (title != null) ...[
            Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: Text(
                title!,
                style: TextStyle(
                  color: palette.muted,
                  fontWeight: AppTextWeight.semibold,
                ),
              ),
            ),
          ],
          for (var i = 0; i < children.length; i++) ...[
            if (i > 0) const SizedBox(height: 18),
            children[i],
          ],
        ],
      ),
    );
  }
}

class ShelfActionTile extends StatelessWidget {
  const ShelfActionTile({
    super.key,
    required this.palette,
    required this.icon,
    required this.title,
    required this.onTap,
    this.subtitle,
    this.trailing,
  });

  final AppPalette palette;
  final IconData icon;
  final String title;
  final String? subtitle;
  final Widget? trailing;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      borderRadius: BorderRadius.circular(18),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: SizedBox(
          height: 64,
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              SizedBox(
                width: 48,
                child: Center(
                  child: Icon(icon, color: palette.accentText, size: 28),
                ),
              ),
              const SizedBox(width: 18),
              Expanded(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        color: palette.text,
                        fontSize: 20,
                        height: 1.12,
                        fontWeight: AppTextWeight.medium,
                      ),
                    ),
                    if (subtitle != null) ...[
                      const SizedBox(height: 6),
                      Text(
                        subtitle!,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          color: palette.muted,
                          fontSize: 16,
                          height: 1.12,
                          fontWeight: AppTextWeight.regular,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              if (trailing != null) ...[
                const SizedBox(width: 12),
                SizedBox(width: 40, child: trailing!),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class ShelfDialogPanel extends StatelessWidget {
  const ShelfDialogPanel({
    super.key,
    required this.palette,
    required this.title,
    required this.child,
    required this.actions,
  });

  final AppPalette palette;
  final String title;
  final Widget child;
  final List<Widget> actions;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 24, 24, 18),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: TextStyle(
              color: palette.text,
              fontSize: 24,
              fontWeight: AppTextWeight.semibold,
            ),
          ),
          const SizedBox(height: 18),
          child,
          const SizedBox(height: 18),
          Wrap(
            alignment: WrapAlignment.end,
            spacing: 10,
            runSpacing: 8,
            children: actions,
          ),
        ],
      ),
    );
  }
}

class ShelfTextField extends StatelessWidget {
  const ShelfTextField({
    super.key,
    required this.controller,
    required this.palette,
    required this.label,
    this.hintText,
    this.maxLength,
    this.autofocus = false,
  });

  final TextEditingController controller;
  final AppPalette palette;
  final String label;
  final String? hintText;
  final int? maxLength;
  final bool autofocus;

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      autofocus: autofocus,
      maxLength: maxLength,
      style: TextStyle(color: palette.text, fontWeight: AppTextWeight.regular),
      decoration: InputDecoration(
        labelText: label,
        hintText: hintText,
        labelStyle: TextStyle(color: palette.muted),
        hintStyle: TextStyle(color: palette.subtle),
        filled: true,
        fillColor: palette.card,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide.none,
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide.none,
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide.none,
        ),
      ),
    );
  }
}

class AddExistingBooksSheet extends StatefulWidget {
  const AddExistingBooksSheet({
    super.key,
    required this.palette,
    required this.books,
  });

  final AppPalette palette;
  final List<BookEntry> books;

  @override
  State<AddExistingBooksSheet> createState() => _AddExistingBooksSheetState();
}

class _AddExistingBooksSheetState extends State<AddExistingBooksSheet> {
  final Set<String> _selectedIds = {};
  final _listKey = GlobalKey();
  final _scrollController = ScrollController();
  bool? _dragSelectionTarget;
  Offset? _selectionPointerStart;
  int? _pendingDragIndex;
  int? _lastDragIndex;

  static const double _bookRowExtent = 83;
  static const double _selectionZoneWidth = 96;
  static const double _dragSlop = 10;

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final palette = widget.palette;
    final books = [...widget.books]
      ..sort((a, b) {
        final byTitle = compareNaturalText(a.title, b.title);
        if (byTitle != 0) {
          return byTitle;
        }
        return compareNaturalText(a.author, b.author);
      });
    final height = MediaQuery.sizeOf(context).height;
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(8, 2, 8, 12),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '从全部书籍选择',
                        style: TextStyle(
                          color: palette.text,
                          fontSize: 22,
                          fontWeight: AppTextWeight.semibold,
                        ),
                      ),
                      const SizedBox(height: 5),
                      Text(
                        '${books.length} 本可加入',
                        style: TextStyle(color: palette.muted, fontSize: 13),
                      ),
                    ],
                  ),
                ),
                TextButton(
                  onPressed: () {
                    setState(() {
                      if (_selectedIds.length == books.length) {
                        _selectedIds.clear();
                      } else {
                        _selectedIds
                          ..clear()
                          ..addAll(books.map((book) => book.id));
                      }
                    });
                  },
                  child: Text(
                    _selectedIds.length == books.length ? '清空' : '全选',
                  ),
                ),
              ],
            ),
          ),
          ConstrainedBox(
            constraints: BoxConstraints(maxHeight: height * .56),
            child: Listener(
              key: _listKey,
              onPointerDown: (event) => _prepareDragSelection(event, books),
              onPointerMove: (event) => _updateDragSelection(event, books),
              onPointerUp: (_) => _endDragSelection(),
              onPointerCancel: (_) => _endDragSelection(),
              child: ListView.builder(
                controller: _scrollController,
                shrinkWrap: true,
                physics: const ClampingScrollPhysics(),
                itemCount: books.length,
                itemBuilder: (context, index) {
                  final book = books[index];
                  final selected = _selectedIds.contains(book.id);
                  return SizedBox(
                    height: _bookRowExtent,
                    child: InkWell(
                      borderRadius: BorderRadius.circular(16),
                      onTap: () => _toggleBook(book.id),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 10,
                        ),
                        child: Row(
                          children: [
                            BookCover(
                              book: book,
                              palette: palette,
                              width: 44,
                              height: 62,
                              radius: 9,
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    book.title,
                                    maxLines: 2,
                                    overflow: TextOverflow.ellipsis,
                                    style: TextStyle(
                                      color: palette.text,
                                      fontSize: 15,
                                      height: 1.25,
                                      fontWeight: AppTextWeight.medium,
                                    ),
                                  ),
                                  const SizedBox(height: 6),
                                  Text(
                                    '${book.author} · ${bookWordCountLabel(book.wordCount)}',
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: TextStyle(
                                      color: palette.muted,
                                      fontSize: 12,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            const SizedBox(width: 10),
                            SizedBox(
                              width: 44,
                              height: double.infinity,
                              child: Center(
                                child: Icon(
                                  selected
                                      ? Icons.check_circle_rounded
                                      : Icons.circle_outlined,
                                  color: selected
                                      ? palette.accentText
                                      : palette.subtle,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  );
                },
              ),
            ),
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text('取消'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: FilledButton(
                  onPressed: _selectedIds.isEmpty
                      ? null
                      : () => Navigator.pop(
                          context,
                          Set<String>.of(_selectedIds),
                        ),
                  child: Text('加入 ${_selectedIds.length} 本'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  void _toggleBook(String bookId) {
    setState(() {
      if (_selectedIds.contains(bookId)) {
        _selectedIds.remove(bookId);
      } else {
        _selectedIds.add(bookId);
      }
    });
    HapticFeedback.selectionClick();
  }

  void _prepareDragSelection(PointerDownEvent event, List<BookEntry> books) {
    final box = _listKey.currentContext?.findRenderObject() as RenderBox?;
    if (box == null || books.isEmpty) {
      return;
    }
    final local = box.globalToLocal(event.position);
    if (local.dx < box.size.width - _selectionZoneWidth) {
      return;
    }
    final index = _bookIndexAt(local.dy, books.length);
    if (index == null) {
      return;
    }
    _selectionPointerStart = local;
    _pendingDragIndex = index;
    _lastDragIndex = null;
  }

  void _updateDragSelection(PointerMoveEvent event, List<BookEntry> books) {
    final start = _selectionPointerStart;
    final pendingIndex = _pendingDragIndex;
    if (start == null || pendingIndex == null || books.isEmpty) {
      return;
    }
    final box = _listKey.currentContext?.findRenderObject() as RenderBox?;
    if (box == null) {
      return;
    }
    final local = box.globalToLocal(event.position);
    if (_dragSelectionTarget == null) {
      final delta = local - start;
      if (delta.distance < _dragSlop) {
        return;
      }
      if (delta.dy.abs() > delta.dx.abs() * 1.35) {
        _endDragSelection();
        return;
      }
      _dragSelectionTarget = !_selectedIds.contains(books[pendingIndex].id);
      _applyDragSelection(pendingIndex, books);
    }
    final index = _bookIndexAt(local.dy, books.length);
    if (index != null) {
      _applyDragSelection(index, books);
    }
  }

  void _endDragSelection() {
    _dragSelectionTarget = null;
    _selectionPointerStart = null;
    _pendingDragIndex = null;
    _lastDragIndex = null;
  }

  int? _bookIndexAt(double localDy, int bookCount) {
    final y = localDy + _scrollController.offset;
    final index = (y / _bookRowExtent).floor();
    if (index < 0 || index >= bookCount) {
      return null;
    }
    return index;
  }

  void _applyDragSelection(int index, List<BookEntry> books) {
    if (_lastDragIndex == index) {
      return;
    }
    final target = _dragSelectionTarget;
    if (target == null) {
      return;
    }
    _lastDragIndex = index;
    final bookId = books[index].id;
    setState(() {
      if (target) {
        _selectedIds.add(bookId);
      } else {
        _selectedIds.remove(bookId);
      }
    });
    HapticFeedback.selectionClick();
  }
}
