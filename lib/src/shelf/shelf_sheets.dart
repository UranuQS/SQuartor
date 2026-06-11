import 'dart:ui';

import 'package:flutter/material.dart';

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
      padding: const EdgeInsets.fromLTRB(14, 0, 14, 18),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (title != null) ...[
            Padding(
              padding: const EdgeInsets.fromLTRB(14, 4, 14, 10),
              child: Text(
                title!,
                style: TextStyle(
                  color: palette.muted,
                  fontWeight: AppTextWeight.semibold,
                ),
              ),
            ),
          ],
          ...children,
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
    return ListTile(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
      leading: Icon(icon, color: palette.accentText),
      title: Text(
        title,
        style: TextStyle(color: palette.text, fontWeight: AppTextWeight.medium),
      ),
      subtitle: subtitle == null
          ? null
          : Text(subtitle!, style: TextStyle(color: palette.muted)),
      trailing: trailing,
      onTap: onTap,
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
            child: ListView.separated(
              shrinkWrap: true,
              physics: const ClampingScrollPhysics(),
              itemCount: books.length,
              separatorBuilder: (_, _) => const SizedBox(height: 1),
              itemBuilder: (context, index) {
                final book = books[index];
                final selected = _selectedIds.contains(book.id);
                return InkWell(
                  borderRadius: BorderRadius.circular(16),
                  onTap: () {
                    setState(() {
                      if (selected) {
                        _selectedIds.remove(book.id);
                      } else {
                        _selectedIds.add(book.id);
                      }
                    });
                  },
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
                        Icon(
                          selected
                              ? Icons.check_circle_rounded
                              : Icons.circle_outlined,
                          color: selected ? palette.accentText : palette.subtle,
                        ),
                      ],
                    ),
                  ),
                );
              },
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
}
