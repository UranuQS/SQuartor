import 'dart:io';
import 'dart:math' as math;

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:path/path.dart' as path;

import '../app_state.dart';
import '../models.dart';
import '../typography.dart';
import '../widgets/book_cover.dart';
import '../screens/reader_screen.dart';
import 'shelf_enums.dart';
import 'shelf_sheets.dart';
import 'shelf_selection.dart';
import 'shelf_widgets.dart';

class ShelfScreen extends StatefulWidget {
  const ShelfScreen({
    super.key,
    required this.state,
    this.onSelectionModeChanged,
  });

  final AppState state;
  final ValueChanged<bool>? onSelectionModeChanged;

  @override
  State<ShelfScreen> createState() => _ShelfScreenState();
}

class _ShelfScreenState extends State<ShelfScreen> {
  String _selectedShelf = defaultShelfName;
  var _sortMode = ShelfSortMode.name;
  final Set<String> _selectedBookIds = {};
  final List<String> _manualSelectionHistory = [];
  final List<Set<String>> _selectionUndoStack = [];
  List<String> _visibleBookIds = const [];
  var _reportedSelectionMode = false;
  final _shelfMenuLayerLink = LayerLink();

  bool get _selectionMode => _selectedBookIds.isNotEmpty;

  @override
  void dispose() {
    if (_reportedSelectionMode) {
      widget.onSelectionModeChanged?.call(false);
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = widget.state;
    return AnimatedBuilder(
      animation: state.shelfChanges,
      builder: (context, _) {
        final palette = state.palette;
        final showingDefault = _selectedShelf == defaultShelfName;
        final rawVisibleBooks = showingDefault
            ? state.books
            : state.books
                  .where((book) => book.shelfName == _selectedShelf)
                  .toList();
        final visibleBooks = _sortBooks(rawVisibleBooks);
        _visibleBookIds = [for (final book in visibleBooks) book.id];
        final blockDisplay = showingDefault;
        final bookBlocks = blockDisplay
            ? _buildBookBlocks(visibleBooks)
            : const <ShelfBookBlock>[];
        final shelves = [defaultShelfName, ...state.shelves];
        final selectedIndex = shelves
            .indexOf(_selectedShelf)
            .clamp(0, shelves.length - 1);
        _reportSelectionModeIfNeeded();
        final bottomInset = MediaQuery.paddingOf(context).bottom;
        final contentBottomPadding = _selectionMode ? 190.0 : 120.0;
        return Stack(
          children: [
            CustomScrollView(
              slivers: [
                SliverPadding(
                  padding: const EdgeInsets.fromLTRB(20, 30, 20, 12),
                  sliver: SliverToBoxAdapter(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    _selectionMode
                                        ? '已选择 ${_selectedBookIds.length}'
                                        : '书架',
                                    style: TextStyle(
                                      color: palette.text,
                                      fontSize: 32,
                                      fontWeight: AppTextWeight.semibold,
                                      letterSpacing: -1.2,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            Builder(
                              builder: (buttonContext) =>
                                  CompositedTransformTarget(
                                    link: _shelfMenuLayerLink,
                                    child: CircleButton(
                                      palette: palette,
                                      icon: _selectionMode
                                          ? Icons.close_rounded
                                          : Icons.more_vert_rounded,
                                      onTap: () {
                                        HapticFeedback.selectionClick();
                                        if (_selectionMode) {
                                          setState(_clearSelection);
                                        } else {
                                          _showShelfMenu(
                                            context,
                                            anchorLink: _shelfMenuLayerLink,
                                          );
                                        }
                                      },
                                    ),
                                  ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 24),
                        ShelfTabs(
                          shelves: shelves,
                          selectedIndex: selectedIndex,
                          palette: palette,
                          onChanged: (index) => setState(() {
                            _selectedShelf = shelves[index];
                            _clearSelection();
                          }),
                        ),
                        const SizedBox(height: 22),
                        Row(
                          children: [
                            Icon(
                              Icons.bookmark_border_rounded,
                              color: palette.text,
                              size: 26,
                            ),
                            const SizedBox(width: 10),
                            Text(
                              _selectedShelf == defaultShelfName
                                  ? '全部 (${state.books.length})'
                                  : '$_selectedShelf (${visibleBooks.length})',
                              style: TextStyle(
                                color: palette.text,
                                fontSize: 20,
                                fontWeight: AppTextWeight.semibold,
                              ),
                            ),
                          ],
                        ),
                      ],
                      ),
                  ),
                ),
                if (state.loading)
                  SliverFillRemaining(
                    child: Center(
                      child: CircularProgressIndicator(
                        color: palette.accentText,
                      ),
                    ),
                  )
                else if (visibleBooks.isEmpty)
                  SliverFillRemaining(
                    hasScrollBody: false,
                    child: Padding(
                      padding: EdgeInsets.fromLTRB(
                        20,
                        10,
                        20,
                        contentBottomPadding,
                      ),
                      child: EmptyShelf(
                        palette: palette,
                        title: showingDefault ? '导入你的第一本书' : '这个书架还没有书',
                        subtitle: showingDefault
                            ? '支持 TXT 和 EPUB。EPUB 会保留原 CSS，并在阅读时注入你的字体、行高和背景设置。'
                            : '自定义书架已经创建，后续可以在书籍菜单里把书加入这里。',
                        actionLabel: showingDefault ? '导入书籍' : '从全部书籍选择',
                        onImport: () => showingDefault
                            ? _showImportSheet(context)
                            : _showAddExistingBooksToShelf(context),
                      ),
                    ),
                  )
                else
                  SliverPadding(
                    padding: EdgeInsets.fromLTRB(
                      20,
                      10,
                      20,
                      contentBottomPadding,
                    ),
                    sliver: blockDisplay
                        ? SliverToBoxAdapter(
                            child: _BookBlockMasonry(
                              blocks: bookBlocks,
                              palette: palette,
                              selectionMode: _selectionMode,
                              selectedBookIds: _selectedBookIds,
                              onTapBlock: _handleBookBlockTap,
                              onLongPressBlock: _handleBookBlockLongPress,
                            ),
                          )
                        : SliverList.separated(
                            itemCount: visibleBooks.length,
                            separatorBuilder: (_, _) =>
                                const SizedBox(height: 22),
                            itemBuilder: (context, index) {
                              final book = visibleBooks[index];
                              return BookTile(
                                book: book,
                                palette: palette,
                                selected: _selectedBookIds.contains(book.id),
                                selectionMode: _selectionMode,
                                onTap: () {
                                  if (_selectionMode) {
                                    HapticFeedback.selectionClick();
                                    _toggleSelection(book);
                                  } else {
                                    _openBook(book);
                                  }
                                },
                                onLongPress: () {
                                  HapticFeedback.mediumImpact();
                                  if (_selectionMode) {
                                    _toggleSelection(book);
                                  } else {
                                    _showBookMenu(context, book);
                                  }
                                },
                              );
                            },
                          ),
                  ),
              ],
            ),
            Positioned(
              left: 14,
              right: 14,
              bottom: bottomInset + 8,
              child: IgnorePointer(
                ignoring: !_selectionMode,
                child: AnimatedSwitcher(
                  duration: const Duration(milliseconds: 240),
                  reverseDuration: const Duration(milliseconds: 210),
                  switchInCurve: Curves.easeOutCubic,
                  switchOutCurve: Curves.easeInCubic,
                  transitionBuilder: (child, animation) {
                    return FadeTransition(
                      opacity: animation,
                      child: SlideTransition(
                        position: Tween(
                          begin: const Offset(0, .7),
                          end: Offset.zero,
                        ).animate(animation),
                        child: child,
                      ),
                    );
                  },
                  child: _selectionMode
                      ? SelectionBar(
                          key: const ValueKey('selection-bar'),
                          palette: palette,
                          rangeEnabled: _canSelectRange,
                          undoEnabled: _selectionUndoStack.isNotEmpty,
                          onSelectRange: _selectEndpointRange,
                          onUndo: _undoSelectionRange,
                          groupMoveEnabled: _selectedBookIds.isNotEmpty,
                          onMoveToGroup: () => _showMoveToGroupSheet(context),
                          onMove: () => _showMoveSheet(context),
                          onDelete: () => _confirmDeleteSelected(context),
                        )
                      : const SizedBox(
                          key: ValueKey('selection-bar-empty'),
                          height: 0,
                        ),
                ),
              ),
            ),
          ],
        );
      },
    );
  }

  Future<void> _showBookBlockPanel(
    BuildContext context,
    ShelfBookBlock block,
  ) async {
    final palette = widget.state.palette;
    await showGeneralDialog<void>(
      context: context,
      barrierDismissible: true,
      barrierLabel: '关闭',
      barrierColor: Colors.black.withValues(alpha: palette.isLight ? .18 : .42),
      transitionDuration: const Duration(milliseconds: 260),
      pageBuilder: (dialogContext, _, _) {
        void closeThen(VoidCallback action) {
          Navigator.of(dialogContext).pop();
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (mounted) {
              action();
            }
          });
        }

        return BookBlockExpandedPanel(
          block: block,
          palette: palette,
          selectionMode: _selectionMode,
          selectedBookIds: _selectedBookIds,
          onDismiss: () => Navigator.of(dialogContext).pop(),
          onDeleteBook: (book) => _confirmDeleteBooks(
            context,
            [book],
            title: '删除书籍',
            message: '确定删除《${book.title}》吗？本地导入记录和文件会一起删除。',
          ),
          onRemoveBookFromGroup: _removeBookFromSeriesGroup,
          onMoveBookToGroup: (book) =>
              _showMoveToGroupSheet(context, books: [book]),
          onReorderGroup: (books) =>
              _showReorderGroupSheet(context, block.title, books),
          onEditBook: (book) {
            closeThen(() {
              _showEditBookDialog(context, book);
            });
          },
          onOpenBook: (book) {
            closeThen(() {
              if (_selectionMode) {
                HapticFeedback.selectionClick();
                _toggleSelection(book);
              } else {
                _openBook(book);
              }
            });
          },
          onLongPressBook: (book) {
            closeThen(() {
              if (_selectionMode) {
                HapticFeedback.selectionClick();
                _toggleSelection(book);
              } else {
                _showBookMenu(context, book);
              }
            });
          },
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
            scale: Tween<double>(begin: .88, end: 1).animate(curved),
            child: child,
          ),
        );
      },
    );
  }

  void _handleBookBlockTap(ShelfBookBlock block) {
    HapticFeedback.selectionClick();
    if (_selectionMode) {
      if (block.books.length == 1) {
        _toggleSelection(block.books.first);
      } else {
        _showBookBlockPanel(context, block);
      }
      return;
    }
    if (block.books.length == 1) {
      _openBook(block.books.first);
      return;
    }
    _showBookBlockPanel(context, block);
  }

  void _handleBookBlockLongPress(ShelfBookBlock block) {
    HapticFeedback.mediumImpact();
    if (_selectionMode) {
      _toggleBlockSelection(block);
      return;
    }
    if (block.books.length == 1) {
      _showBookMenu(context, block.books.first);
      return;
    }
    _showBookBlockMenu(context, block);
  }

  void _toggleBlockSelection(ShelfBookBlock block) {
    final bookIds = block.books.map((book) => book.id).toList();
    if (bookIds.isEmpty) {
      return;
    }
    setState(() {
      final allSelected = bookIds.every(_selectedBookIds.contains);
      if (allSelected) {
        _selectedBookIds.removeAll(bookIds);
        _manualSelectionHistory.removeWhere(bookIds.contains);
      } else {
        _selectedBookIds.addAll(bookIds);
        _recordManualEndpoint(bookIds.first);
        if (bookIds.length > 1) {
          _recordManualEndpoint(bookIds.last);
        }
      }
      if (_selectedBookIds.isEmpty) {
        _manualSelectionHistory.clear();
        _selectionUndoStack.clear();
      }
    });
  }

  Future<void> _showBookBlockMenu(
    BuildContext context,
    ShelfBookBlock block,
  ) async {
    final palette = widget.state.palette;
    final action = await showShelfFloatingSheet<BookBlockMenuAction>(
      context: context,
      palette: palette,
      child: ShelfActionList(
        palette: palette,
        title: block.title,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(14, 0, 14, 8),
            child: Text(
              '${block.books.length} 本 · ${block.chapterCount} 章 · ${bookWordCountLabel(block.wordCount)}',
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(color: palette.muted, height: 1.45),
            ),
          ),
          ShelfActionTile(
            palette: palette,
            icon: Icons.drive_file_rename_outline_rounded,
            title: '修改分组名称',
            subtitle: '给这个分组里的书统一换一个分组名',
            onTap: () => Navigator.pop(context, BookBlockMenuAction.rename),
          ),
          ShelfActionTile(
            palette: palette,
            icon: Icons.delete_outline_rounded,
            title: '删除整个分组',
            subtitle: '删除这个分组里的全部书籍',
            onTap: () => Navigator.pop(context, BookBlockMenuAction.delete),
          ),
          ShelfActionTile(
            palette: palette,
            icon: Icons.merge_type_rounded,
            title: '合并到分组',
            subtitle: '把这个分组并入另一个已有分组',
            onTap: () => Navigator.pop(context, BookBlockMenuAction.merge),
          ),
        ],
      ),
    );
    if (!mounted || !context.mounted || action == null) {
      return;
    }
    switch (action) {
      case BookBlockMenuAction.rename:
        await _showRenameGroupDialog(context, block);
      case BookBlockMenuAction.delete:
        await _confirmDeleteBooks(
          context,
          block.books,
          title: '删除整个分组',
          message:
              '确定删除“${block.title}”里的 ${block.books.length} 本书吗？本地导入记录和文件会一起删除。',
        );
      case BookBlockMenuAction.merge:
        await _showMoveToGroupSheet(context, books: block.books);
    }
  }

  Future<void> _showRenameGroupDialog(
    BuildContext context,
    ShelfBookBlock block,
  ) async {
    final palette = widget.state.palette;
    final controller = TextEditingController(
      text: _canonicalSeriesTitle(block),
    );
    final name = await showShelfFloatingDialog<String>(
      context: context,
      palette: palette,
      child: ShelfDialogPanel(
        palette: palette,
        title: '修改分组名称',
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, controller.text),
            child: const Text('保存'),
          ),
        ],
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '会把这个分组里的 ${block.books.length} 本书移动到新的分组名下。',
              style: TextStyle(
                color: palette.muted,
                fontSize: 13,
                height: 1.45,
              ),
            ),
            const SizedBox(height: 14),
            ShelfTextField(
              controller: controller,
              palette: palette,
              label: '分组名称',
              maxLength: 28,
              autofocus: true,
            ),
          ],
        ),
      ),
    );
    controller.dispose();
    final trimmed = name?.trim();
    if (trimmed == null || trimmed.isEmpty) {
      return;
    }
    await widget.state.updateBookSeriesOverride(
      block.books.map((book) => book.id).toSet(),
      trimmed,
    );
  }

  void _reportSelectionModeIfNeeded() {
    final selectionMode = _selectionMode;
    if (_reportedSelectionMode == selectionMode) {
      return;
    }
    _reportedSelectionMode = selectionMode;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) {
        return;
      }
      widget.onSelectionModeChanged?.call(selectionMode);
    });
  }

  List<BookEntry> _sortBooks(List<BookEntry> books) {
    final sorted = [...books];
    switch (_sortMode) {
      case ShelfSortMode.name:
        sorted.sort((a, b) {
          final byTitle = compareNaturalText(a.title, b.title);
          if (byTitle != 0) {
            return byTitle;
          }
          return compareNaturalText(a.author, b.author);
        });
        return sorted;
      case ShelfSortMode.recent:
        sorted.sort((a, b) {
          final byTime = (b.lastReadAt ?? b.importedAt).compareTo(
            a.lastReadAt ?? a.importedAt,
          );
          if (byTime != 0) {
            return byTime;
          }
          return b.progress.compareTo(a.progress);
        });
        return sorted;
    }
  }

  List<ShelfBookBlock> _buildBookBlocks(List<BookEntry> books) {
    final groups = <String, List<BookEntry>>{};
    final titles = <String, String>{};
    final authors = <String, String>{};
    final manualKeys = <String>{};
    for (final book in books) {
      final override = book.seriesOverride?.trim();
      final hasManualSeries = override != null && override.isNotEmpty;
      final title = hasManualSeries ? override : _blockTitleForBook(book);
      final author = book.author.trim();
      final key = hasManualSeries
          ? 'manual::${_manualSeriesKeyForTitle(title)}'
          : '${author.toLowerCase()}::${_seriesKeyForTitle(title)}';
      groups.putIfAbsent(key, () => <BookEntry>[]).add(book);
      titles.putIfAbsent(key, () => title);
      authors.putIfAbsent(key, () => author);
      if (hasManualSeries) {
        manualKeys.add(key);
      }
    }
    final blocks = _mergeRelatedSeriesBlocks([
      for (final entry in groups.entries)
        ShelfBookBlock(
          key: entry.key,
          title: titles[entry.key] ?? entry.value.first.title,
          author:
              _commonBlockAuthor(entry.value) ??
              authors[entry.key] ??
              entry.value.first.author,
          books: _sortBlockBooks(entry.value),
          manual: manualKeys.contains(entry.key),
        ),
    ]);
    switch (_sortMode) {
      case ShelfSortMode.name:
        blocks.sort((a, b) => compareNaturalText(a.title, b.title));
      case ShelfSortMode.recent:
        blocks.sort((a, b) => b.lastTouchedAt.compareTo(a.lastTouchedAt));
    }
    return blocks;
  }

  List<ShelfBookBlock> _mergeRelatedSeriesBlocks(List<ShelfBookBlock> blocks) {
    if (blocks.length < 2) {
      return blocks;
    }
    final result = <ShelfBookBlock>[];
    for (final block in blocks) {
      final candidateIndex = result.lastIndexWhere((candidate) {
        if (!_shouldMergeSeriesBlocks(candidate, block)) {
          return false;
        }
        return true;
      });
      if (candidateIndex == -1) {
        result.add(block);
        continue;
      }
      final candidate = result[candidateIndex];
      result[candidateIndex] = ShelfBookBlock(
        key: candidate.key,
        title: _preferredSeriesTitle(candidate, block),
        author: candidate.author,
        books: _sortBlockBooks([...candidate.books, ...block.books]),
        manual: candidate.manual || block.manual,
      );
    }
    return result;
  }

  bool _shouldMergeSeriesBlocks(
    ShelfBookBlock candidate,
    ShelfBookBlock block,
  ) {
    final candidateKey = _seriesKeyForTitle(candidate.title);
    final blockKey = _seriesKeyForTitle(block.title);
    if (candidate.manual || block.manual) {
      return false;
    }
    final titleClose =
        candidateKey == blockKey ||
        candidateKey.contains(blockKey) ||
        blockKey.contains(candidateKey) ||
        _seriesTitleSimilarity(candidateKey, blockKey) >= .66;
    final candidateAuthor = _authorKey(candidate.author);
    final blockAuthor = _authorKey(block.author);
    final hasBothAuthors = candidateAuthor.isNotEmpty && blockAuthor.isNotEmpty;
    final authorCompatible =
        hasBothAuthors &&
        (candidateAuthor == blockAuthor ||
            candidateAuthor.contains(blockAuthor) ||
            blockAuthor.contains(candidateAuthor));
    if (!titleClose && !authorCompatible) {
      return false;
    }
    if (!authorCompatible &&
        _seriesTitleSimilarity(candidateKey, blockKey) < .86) {
      return false;
    }
    final candidateVolumes = candidate.books.map(_bookVolumeNumber).toList();
    final blockVolumes = block.books.map(_bookVolumeNumber).toList();
    final knownCandidate = candidateVolumes.whereType<int>().toList();
    final knownBlock = blockVolumes.whereType<int>().toList();
    if (knownCandidate.isEmpty || knownBlock.isEmpty) {
      return titleClose &&
          (candidate.books.length >= 2 ||
              authorCompatible &&
                  (_isSeriesExtra(candidate.books.first.title) ||
                      _isSeriesExtra(block.books.first.title)));
    }
    final candidateSet = knownCandidate.toSet();
    final blockSet = knownBlock.toSet();
    if (candidateSet.intersection(blockSet).isNotEmpty) {
      return false;
    }
    final minCandidate = knownCandidate.reduce(math.min);
    final maxCandidate = knownCandidate.reduce(math.max);
    final minBlock = knownBlock.reduce(math.min);
    final maxBlock = knownBlock.reduce(math.max);
    final volumeClose =
        minBlock <= maxCandidate + 2 && maxBlock >= minCandidate - 2;
    if (titleClose) {
      return volumeClose;
    }
    return authorCompatible &&
        candidate.books.length >= 3 &&
        block.books.length == 1 &&
        (minBlock == maxCandidate + 1 || maxBlock == minCandidate - 1);
  }

  List<BookEntry> _sortBlockBooks(List<BookEntry> books) {
    return [...books]..sort((a, b) {
      final aOrder = a.seriesOrder;
      final bOrder = b.seriesOrder;
      if (aOrder != null && bOrder != null && aOrder != bOrder) {
        return aOrder.compareTo(bOrder);
      }
      if (aOrder != null || bOrder != null) {
        return aOrder != null ? -1 : 1;
      }
      final aVolume = _bookVolumeNumber(a);
      final bVolume = _bookVolumeNumber(b);
      if (aVolume != null && bVolume != null) {
        final byVolume = aVolume.compareTo(bVolume);
        if (byVolume != 0) {
          return byVolume;
        }
      }
      final byTitle = compareNaturalText(a.title, b.title);
      if (byTitle != 0) {
        return byTitle;
      }
      return compareNaturalText(a.author, b.author);
    });
  }

  String? _commonBlockAuthor(List<BookEntry> books) {
    String? author;
    for (final book in books) {
      final trimmed = book.author.trim();
      if (trimmed.isEmpty) {
        continue;
      }
      author ??= trimmed;
      if (_authorKey(author) != _authorKey(trimmed)) {
        return '';
      }
    }
    return author;
  }

  String _seriesKeyForTitle(String title) {
    var cleaned = _stripSeriesSuffix(title).toLowerCase();
    const aliases = <String, String>{
      '我的妹妹不可能那么可爱': '我的妹妹哪有这么可爱',
      '我的妹妹不可能这么可爱': '我的妹妹哪有这么可爱',
      '我的妹妹不可能那麼可愛': '我的妹妹哪有这么可爱',
      '我的妹妹哪有那麼可愛': '我的妹妹哪有这么可爱',
    };
    for (final entry in aliases.entries) {
      cleaned = cleaned.replaceAll(entry.key, entry.value);
    }
    const seriesAliases = <String, String>{
      '败犬女主': '败北女角',
      '敗犬女主': '敗北女角',
      '败犬女角': '败北女角',
      '敗犬女角': '敗北女角',
      '负けヒロインが多すぎる': '败北女角太多了',
      '負けヒロインが多すぎる': '敗北女角太多了',
      'makeine': '败北女角太多了',
    };
    for (final entry in seriesAliases.entries) {
      cleaned = cleaned.replaceAll(entry.key, entry.value);
    }
    cleaned = cleaned
        .replaceFirst(
          RegExp(
            r'[\s\-_:：·.]*?(?:番外|外传|外傳|短篇|特典|if线|if線|\bif\b|bd|dvd|广播剧|廣播劇|携带版|攜帶版).*$',
            caseSensitive: false,
          ),
          '',
        )
        .replaceAll(RegExp(r'[\s\-_·・:：,，.。!！?？\[\]【】()（）]+'), '')
        .replaceAll(
          RegExp(
            r'(台版|臺版|个人翻译|個人翻譯|翻译|翻譯|epub|txt|番外|外传|外傳|短篇|特典|if线|if線|if|bd|dvd|广播剧|廣播劇|携带版|攜帶版)',
            caseSensitive: false,
          ),
          '',
        );
    return pinyinSortKey(cleaned);
  }

  String _manualSeriesKeyForTitle(String title) {
    return pinyinSortKey(
      title.trim().toLowerCase().replaceAll(RegExp(r'[\s\-_·・:：.]+'), ''),
    );
  }

  double _seriesTitleSimilarity(String left, String right) {
    return math.max(_similarity(left, right), _bigramSimilarity(left, right));
  }

  double _bigramSimilarity(String left, String right) {
    if (left.length < 2 || right.length < 2) {
      return _similarity(left, right);
    }
    Set<String> grams(String value) => {
      for (var index = 0; index < value.length - 1; index++)
        value.substring(index, index + 2),
    };
    final leftGrams = grams(left);
    final rightGrams = grams(right);
    final intersection = leftGrams.intersection(rightGrams).length;
    return (2 * intersection) / (leftGrams.length + rightGrams.length);
  }

  bool _isSeriesExtra(String title) {
    return RegExp(
      r'(番外|外传|外傳|短篇|特典|if线|if線|\bif\b|bd|dvd|广播剧|廣播劇|携带版|攜帶版)',
      caseSensitive: false,
    ).hasMatch(title);
  }

  String _authorKey(String author) {
    return pinyinSortKey(
      author.toLowerCase().replaceAll(
        RegExp(r'[\s\-_·・:：,，.。!！?？\[\]【】()（）]+'),
        '',
      ),
    );
  }

  double _similarity(String left, String right) {
    if (left.isEmpty || right.isEmpty) {
      return 0;
    }
    if (left == right) {
      return 1;
    }
    final distance = _levenshtein(left, right);
    return 1 - distance / math.max(left.length, right.length);
  }

  int _levenshtein(String left, String right) {
    final previous = List<int>.generate(right.length + 1, (index) => index);
    final current = List<int>.filled(right.length + 1, 0);
    for (var i = 0; i < left.length; i++) {
      current[0] = i + 1;
      for (var j = 0; j < right.length; j++) {
        final cost = left.codeUnitAt(i) == right.codeUnitAt(j) ? 0 : 1;
        current[j + 1] = math.min(
          math.min(current[j] + 1, previous[j + 1] + 1),
          previous[j] + cost,
        );
      }
      previous.setAll(0, current);
    }
    return previous[right.length];
  }

  String _blockTitleForBook(BookEntry book) {
    var title = _stripSeriesSuffix(book.title);
    title = title.replaceAll(
      RegExp(
        r'[\s\-_:：·.]*第?\s*[\d一二三四五六七八九十百千万零〇两]+\s*(?:卷|册|集)?\s*(?=(?:番外|外传|外傳|短篇|特典|if线|if線|if|bd|dvd|广播剧|廣播劇|携带版|攜帶版).*$)',
        caseSensitive: false,
      ),
      '',
    );
    title = title.replaceAll(
      RegExp(
        r'[\s\-_:：·.]*[(（\[]?\s*(?:vol(?:ume)?|卷|第)\s*[\d一二三四五六七八九十百千万零〇两]+(?:卷|册|集)?\s*[)）\]]?\s*$',
        caseSensitive: false,
      ),
      '',
    );
    title = title.replaceAll(
      RegExp(r'[\s\-_:：·.]+[\d一二三四五六七八九十百千万零〇两]{1,4}\s*$'),
      '',
    );
    title = title.replaceAll(RegExp(r'\s+'), ' ').trim();
    return title.isEmpty ? book.title.trim() : title;
  }

  String _stripSeriesSuffix(String rawTitle) {
    var title = rawTitle.trim();
    title = title.replaceAll(
      RegExp(
        r'[\s\-_:：·.]+[\d一二三四五六七八九十百千万零〇两]{1,4}\s*(?:[(（][^)）]*(?:版|优化|優化|翻译|翻譯|校对|校對|epub|txt|自制|自製)[)）])?\s*$',
        caseSensitive: false,
      ),
      '',
    );
    title = title.replaceAll(
      RegExp(
        r'[\s\-_:：·.]+(?:vol(?:ume)?\.?|卷|第)\s*[\d一二三四五六七八九十百千万零〇两]{1,4}\s*$',
        caseSensitive: false,
      ),
      '',
    );
    return title.trim();
  }

  int? _bookVolumeNumber(BookEntry book) {
    final title = book.title;
    final patterns = [
      RegExp(r'(?:第|卷|vol(?:ume)?\.?\s*)\s*([0-9]+)', caseSensitive: false),
      RegExp(r'([0-9]+)\s*(?:卷|册|集)\s*$', caseSensitive: false),
      RegExp(r'([0-9]+)\s*$', caseSensitive: false),
      RegExp(
        r'([0-9]+)\s*(?=(?:番外|外传|外傳|短篇|特典|if线|if線|if|bd|dvd|广播剧|廣播劇|携带版|攜帶版))',
        caseSensitive: false,
      ),
      RegExp(r'(?:第|卷)\s*([一二三四五六七八九十百千万零〇两]+)'),
      RegExp(r'([一二三四五六七八九十百千万零〇两]+)\s*(?:卷|册|集)\s*$'),
    ];
    for (final pattern in patterns) {
      final match = pattern.firstMatch(title);
      final raw = match?.group(1);
      if (raw == null) {
        continue;
      }
      return int.tryParse(raw) ?? parseChineseNumber(raw.runes.toList());
    }
    return null;
  }

  void _openBook(BookEntry book) {
    HapticFeedback.selectionClick();
    Navigator.of(context).pushNamed(ReaderScreen.routeName, arguments: book);
  }

  void _clearSelection() {
    _selectedBookIds.clear();
    _manualSelectionHistory.clear();
    _selectionUndoStack.clear();
  }

  void _recordManualEndpoint(String bookId) {
    _manualSelectionHistory.remove(bookId);
    _manualSelectionHistory.add(bookId);
    if (_manualSelectionHistory.length > 2) {
      _manualSelectionHistory.removeAt(0);
    }
  }

  (String, String)? get _rangeEndpoints {
    if (_manualSelectionHistory.length < 2) {
      return null;
    }
    final first = _manualSelectionHistory[0];
    final second = _manualSelectionHistory[1];
    if (first == second) {
      return null;
    }
    return (first, second);
  }

  bool get _canSelectRange {
    final endpoints = _rangeEndpoints;
    if (endpoints == null) {
      return false;
    }
    return _visibleBookIds.contains(endpoints.$1) &&
        _visibleBookIds.contains(endpoints.$2);
  }

  void _selectEndpointRange() {
    final endpoints = _rangeEndpoints;
    if (endpoints == null) {
      return;
    }
    final firstIndex = _visibleBookIds.indexOf(endpoints.$1);
    final secondIndex = _visibleBookIds.indexOf(endpoints.$2);
    if (firstIndex < 0 || secondIndex < 0) {
      return;
    }
    HapticFeedback.mediumImpact();
    final from = math.min(firstIndex, secondIndex);
    final to = math.max(firstIndex, secondIndex);
    setState(() {
      _selectionUndoStack.add(Set<String>.of(_selectedBookIds));
      _selectedBookIds.addAll(_visibleBookIds.sublist(from, to + 1));
    });
  }

  void _undoSelectionRange() {
    if (_selectionUndoStack.isEmpty) {
      return;
    }
    HapticFeedback.selectionClick();
    setState(() {
      final previousSelection = _selectionUndoStack.removeLast();
      _selectedBookIds
        ..clear()
        ..addAll(previousSelection);
      if (_selectedBookIds.isEmpty) {
        _manualSelectionHistory.clear();
      }
    });
  }

  Future<void> _showBookMenu(BuildContext context, BookEntry book) async {
    final palette = widget.state.palette;
    final action = await showShelfFloatingSheet<BookMenuAction>(
      context: context,
      palette: palette,
      child: ShelfActionList(
        palette: palette,
        children: [
          ShelfActionTile(
            palette: palette,
            icon: Icons.edit_rounded,
            title: '编辑书籍信息',
            subtitle: '修改标题和作者',
            onTap: () => Navigator.pop(context, BookMenuAction.edit),
          ),
          ShelfActionTile(
            palette: palette,
            icon: Icons.auto_awesome_motion_rounded,
            title: '移动到分组',
            subtitle: '当前：${_effectiveSeriesName(book)}',
            onTap: () => Navigator.pop(context, BookMenuAction.series),
          ),
          ShelfActionTile(
            palette: palette,
            icon: Icons.checklist_rounded,
            title: '选择多个',
            subtitle: '用于批量移动或删除',
            onTap: () => Navigator.pop(context, BookMenuAction.select),
          ),
        ],
      ),
    );
    if (!mounted || !context.mounted || action == null) {
      return;
    }
    switch (action) {
      case BookMenuAction.edit:
        await _showEditBookDialog(context, book);
      case BookMenuAction.series:
        await _showMoveToGroupSheet(context, books: [book]);
      case BookMenuAction.select:
        HapticFeedback.selectionClick();
        setState(() {
          _selectedBookIds.add(book.id);
          _recordManualEndpoint(book.id);
        });
    }
  }

  String _effectiveSeriesName(BookEntry book) {
    final override = book.seriesOverride?.trim();
    if (override != null && override.isNotEmpty) {
      return override;
    }
    return _blockTitleForBook(book);
  }

  Future<bool> _showMoveToGroupSheet(
    BuildContext context, {
    List<BookEntry>? books,
  }) async {
    final targetBooks =
        books ??
        widget.state.books
            .where((book) => _selectedBookIds.contains(book.id))
            .toList();
    final bookIds = targetBooks.map((book) => book.id).toSet();
    if (targetBooks.isEmpty) {
      return false;
    }
    HapticFeedback.selectionClick();
    final palette = widget.state.palette;
    final groups = _availableSeriesGroups(excludingIds: bookIds);
    final currentGroup = _currentSeriesBlockTitle(bookIds);
    final result = await showShelfFloatingSheet<({bool clear, String? name})>(
      context: context,
      palette: palette,
      child: _MoveToGroupSheetWidget(
        palette: palette,
        targetBooksCount: targetBooks.length,
        groups: groups,
        currentGroup: currentGroup,
        bookCountForGroup: (group) => _seriesBlockBookIds(group).length,
      ),
    );
    if (!context.mounted || result == null) {
      return false;
    }
    final targetName = result.clear ? null : _canonicalSeriesName(result.name);
    final targetIds = result.clear
        ? bookIds
        : {...bookIds, ..._seriesBlockBookIds(targetName)};
    await widget.state.updateBookSeriesOverride(targetIds, targetName);
    if (mounted && books == null) {
      setState(_clearSelection);
    }
    return true;
  }

  Future<bool> _removeBookFromSeriesGroup(BookEntry book) async {
    HapticFeedback.selectionClick();
    await widget.state.updateBookSeriesOverride({book.id}, book.title.trim());
    return true;
  }

  Future<List<BookEntry>?> _showReorderGroupSheet(
    BuildContext context,
    String groupTitle,
    List<BookEntry> books,
  ) async {
    if (books.length < 2) {
      return null;
    }
    HapticFeedback.selectionClick();
    final palette = widget.state.palette;
    final ordered = [...books];
    final saved = await showShelfFloatingDialog<bool>(
      context: context,
      palette: palette,
      child: StatefulBuilder(
        builder: (dialogContext, setDialogState) {
          final height = math.min(ordered.length * 82.0, 430.0).toDouble();
          return ShelfDialogPanel(
            palette: palette,
            title: '分组内排序',
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(dialogContext, false),
                child: const Text('取消'),
              ),
              FilledButton(
                onPressed: () => Navigator.pop(dialogContext, true),
                child: const Text('保存'),
              ),
            ],
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  groupTitle,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(color: palette.muted, height: 1.45),
                ),
                const SizedBox(height: 12),
                SizedBox(
                  height: height,
                  child: ReorderableListView.builder(
                    padding: EdgeInsets.zero,
                    physics: const ClampingScrollPhysics(),
                    proxyDecorator: (child, index, animation) {
                      return Material(
                        color: Colors.transparent,
                        child: ScaleTransition(
                          scale: Tween<double>(begin: 1, end: 1.03).animate(
                            CurvedAnimation(
                              parent: animation,
                              curve: Curves.easeOutCubic,
                            ),
                          ),
                          child: child,
                        ),
                      );
                    },
                    itemCount: ordered.length,
                    onReorderItem: (oldIndex, newIndex) {
                      setDialogState(() {
                        final item = ordered.removeAt(oldIndex);
                        ordered.insert(newIndex, item);
                      });
                    },
                    itemBuilder: (context, index) {
                      final book = ordered[index];
                      return _ReorderBookTile(
                        key: ValueKey(book.id),
                        book: book,
                        index: index,
                        palette: palette,
                      );
                    },
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
    if (saved != true) {
      return null;
    }
    await widget.state.updateBookSeriesOrder(
      ordered.map((book) => book.id).toList(),
    );
    return ordered;
  }

  Future<bool> _confirmDeleteBooks(
    BuildContext context,
    List<BookEntry> books, {
    required String title,
    required String message,
  }) async {
    if (books.isEmpty) {
      return false;
    }
    HapticFeedback.selectionClick();
    final palette = widget.state.palette;
    final confirm = await showShelfFloatingDialog<bool>(
      context: context,
      palette: palette,
      child: ShelfDialogPanel(
        palette: palette,
        title: title,
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('删除'),
          ),
        ],
        child: Text(
          message,
          style: TextStyle(color: palette.muted, height: 1.5),
        ),
      ),
    );
    if (confirm != true) {
      return false;
    }
    for (final book in books) {
      await widget.state.removeBook(book);
    }
    return true;
  }

  List<String> _availableSeriesGroups({Set<String> excludingIds = const {}}) {
    final namesByKey = <String, String>{};
    for (final block in _buildBookBlocks(widget.state.books)) {
      if (block.books.every((book) => excludingIds.contains(book.id))) {
        continue;
      }
      final name = _canonicalSeriesTitle(block).trim();
      if (name.isNotEmpty) {
        namesByKey.putIfAbsent(_seriesKeyForTitle(name), () => name);
      }
    }
    if (namesByKey.isEmpty) {
      for (final name in _targetFallbackGroups(excludingIds)) {
        namesByKey.putIfAbsent(_seriesKeyForTitle(name), () => name);
      }
    }
    final sorted = namesByKey.values.toList()
      ..sort((a, b) => compareNaturalText(a, b));
    return sorted;
  }

  String? _currentSeriesBlockTitle(Set<String> bookIds) {
    if (bookIds.isEmpty) {
      return null;
    }
    for (final block in _buildBookBlocks(widget.state.books)) {
      if (block.books.any((book) => bookIds.contains(book.id))) {
        return block.title;
      }
    }
    return null;
  }

  Set<String> _seriesBlockBookIds(String? groupName) {
    final key = _seriesKeyForTitle(groupName?.trim() ?? '');
    if (key.isEmpty) {
      return const {};
    }
    final ids = <String>{};
    for (final block in _buildBookBlocks(widget.state.books)) {
      if (_seriesKeyForTitle(_canonicalSeriesTitle(block)) == key ||
          _seriesKeyForTitle(block.title) == key) {
        ids.addAll(block.books.map((book) => book.id));
      }
    }
    return ids;
  }

  String _canonicalSeriesTitle(ShelfBookBlock block) {
    if (block.manual) {
      return block.title;
    }
    final candidates = <String>[
      block.title,
      for (final book in block.books) _blockTitleForBook(book),
    ].map((title) => title.trim()).where((title) => title.isNotEmpty).toList();
    if (candidates.isEmpty) {
      return block.title;
    }
    candidates.sort((a, b) {
      final byKeyLength = _seriesKeyForTitle(
        a,
      ).length.compareTo(_seriesKeyForTitle(b).length);
      if (byKeyLength != 0) {
        return byKeyLength;
      }
      return a.length.compareTo(b.length);
    });
    return candidates.first;
  }

  String? _canonicalSeriesName(String? rawName) {
    final name = rawName?.trim();
    if (name == null || name.isEmpty) {
      return null;
    }
    for (final block in _buildBookBlocks(widget.state.books)) {
      if (_seriesKeyForTitle(block.title) == _seriesKeyForTitle(name) ||
          _seriesKeyForTitle(_canonicalSeriesTitle(block)) ==
              _seriesKeyForTitle(name)) {
        return _canonicalSeriesTitle(block);
      }
    }
    return _stripSeriesSuffix(name);
  }

  String _preferredSeriesTitle(ShelfBookBlock left, ShelfBookBlock right) {
    return _canonicalSeriesTitle(
      ShelfBookBlock(
        key: left.key,
        title: left.title,
        author: left.author,
        books: [...left.books, ...right.books],
      ),
    );
  }

  Iterable<String> _targetFallbackGroups(Set<String> excludingIds) sync* {
    for (final book in widget.state.books) {
      if (!excludingIds.contains(book.id)) {
        yield _blockTitleForBook(book);
      }
    }
  }

  Future<void> _showEditBookDialog(BuildContext context, BookEntry book) async {
    final palette = widget.state.palette;
    final titleController = TextEditingController(text: book.title);
    final authorController = TextEditingController(text: book.author);
    String? selectedCoverPath;
    final result = await showShelfFloatingDialog<(String, String, String?)>(
      context: context,
      palette: palette,
      child: StatefulBuilder(
        builder: (dialogContext, setDialogState) {
          final previewPath = selectedCoverPath ?? book.coverPath;
          return ShelfDialogPanel(
            palette: palette,
            title: '编辑书籍信息',
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(dialogContext),
                child: const Text('取消'),
              ),
              FilledButton(
                onPressed: () => Navigator.pop(dialogContext, (
                  titleController.text,
                  authorController.text,
                  selectedCoverPath,
                )),
                child: const Text('保存'),
              ),
            ],
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Row(
                  children: [
                    ClipRRect(
                      borderRadius: BorderRadius.circular(12),
                      child: SizedBox(
                        width: 64,
                        height: 90,
                        child: previewPath == null
                            ? BookCover(
                                book: book,
                                palette: palette,
                                width: 64,
                                height: 90,
                                radius: 12,
                              )
                            : Image.file(
                                File(previewPath),
                                fit: BoxFit.cover,
                                errorBuilder: (_, _, _) => BookCover(
                                  book: book,
                                  palette: palette,
                                  width: 64,
                                  height: 90,
                                  radius: 12,
                                ),
                              ),
                      ),
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: () async {
                          final picked = await FilePicker.pickFiles(
                            type: FileType.image,
                            allowMultiple: false,
                            withData: false,
                          );
                          final pickedPath = picked?.files.single.path;
                          if (pickedPath != null && dialogContext.mounted) {
                            setDialogState(
                              () => selectedCoverPath = pickedPath,
                            );
                          }
                        },
                        icon: const Icon(Icons.image_outlined),
                        label: const Text('更换封面'),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 14),
                ShelfTextField(
                  controller: titleController,
                  palette: palette,
                  label: '标题',
                  autofocus: true,
                ),
                const SizedBox(height: 12),
                ShelfTextField(
                  controller: authorController,
                  palette: palette,
                  label: '作者',
                ),
              ],
            ),
          );
        },
      ),
    );
    final title = result?.$1;
    final author = result?.$2;
    final pickedCoverPath = result?.$3;
    titleController.dispose();
    authorController.dispose();
    if (title == null || author == null || title.trim().isEmpty) {
      return;
    }
    String? coverPath;
    if (pickedCoverPath != null) {
      final source = File(pickedCoverPath);
      final extension = path.extension(pickedCoverPath).toLowerCase();
      final targetDir = Directory(book.bookDir);
      await targetDir.create(recursive: true);
      final target = File(path.join(targetDir.path, 'custom_cover$extension'));
      coverPath = (await source.copy(target.path)).path;
    }
    await widget.state.updateBookMetadata(
      book,
      title: title,
      author: author,
      coverPath: coverPath,
    );
  }

  Future<void> _showSortSheet(BuildContext context) async {
    final palette = widget.state.palette;
    final selected = await showShelfFloatingSheet<ShelfSortMode>(
      context: context,
      palette: palette,
      child: ShelfActionList(
        palette: palette,
        children: [
          for (final mode in ShelfSortMode.values)
            ShelfActionTile(
              palette: palette,
              icon: mode == ShelfSortMode.name
                  ? Icons.sort_by_alpha_rounded
                  : Icons.history_rounded,
              title: mode.label,
              trailing: _sortMode == mode
                  ? Icon(Icons.check_rounded, color: palette.accentText)
                  : null,
              onTap: () => Navigator.pop(context, mode),
            ),
        ],
      ),
    );
    if (selected != null && mounted) {
      setState(() => _sortMode = selected);
    }
  }

  Future<void> _showShelfMenu(
    BuildContext context, {
    required LayerLink anchorLink,
  }) async {
    final palette = widget.state.palette;
    final action = await showShelfFollowerMenu<ShelfMenuAction>(
      context: context,
      anchorLink: anchorLink,
      palette: palette,
      child: ShelfActionList(
        palette: palette,
        children: [
          ShelfActionTile(
            palette: palette,
            icon: Icons.upload_file_rounded,
            title: '导入书籍',
            subtitle: '单本、多本或导入整个文件夹',
            onTap: () => Navigator.pop(context, ShelfMenuAction.import),
          ),
          ShelfActionTile(
            palette: palette,
            icon: Icons.sort_rounded,
            title: '排序方式',
            subtitle: _sortMode.label,
            onTap: () => Navigator.pop(context, ShelfMenuAction.sort),
          ),
          ShelfActionTile(
            palette: palette,
            icon: Icons.create_new_folder_rounded,
            title: '创建书架',
            subtitle: '创建一个新的分类入口',
            onTap: () => Navigator.pop(context, ShelfMenuAction.create),
          ),
          if (_selectedShelf != defaultShelfName)
            ShelfActionTile(
              palette: palette,
              icon: Icons.delete_outline_rounded,
              title: '删除书架',
              subtitle: '只删除当前书架，不删除其中的书籍',
              onTap: () => Navigator.pop(context, ShelfMenuAction.delete),
            ),
        ],
      ),
    );
    if (!mounted || !context.mounted || action == null) {
      return;
    }
    switch (action) {
      case ShelfMenuAction.import:
        await _showImportSheet(context);
      case ShelfMenuAction.sort:
        await _showSortSheet(context);
      case ShelfMenuAction.create:
        await _showCreateShelfDialog(context);
      case ShelfMenuAction.delete:
        await _showDeleteShelfDialog(context);
    }
  }

  Future<void> _showImportSheet(BuildContext context) async {
    final palette = widget.state.palette;
    final action = await showShelfFloatingSheet<int>(
      context: context,
      palette: palette,
      child: ShelfActionList(
        palette: palette,
        children: [
          ShelfActionTile(
            palette: palette,
            icon: Icons.insert_drive_file_rounded,
            title: '导入单本',
            subtitle: '选择一个 TXT 或 EPUB',
            onTap: () => Navigator.pop(context, 0),
          ),
          ShelfActionTile(
            palette: palette,
            icon: Icons.file_upload_rounded,
            title: '批量导入',
            subtitle: '一次选择多本 TXT / EPUB',
            onTap: () => Navigator.pop(context, 1),
          ),
          ShelfActionTile(
            palette: palette,
            icon: Icons.folder_open_rounded,
            title: '导入文件夹',
            subtitle: '递归扫描文件夹内的 TXT / EPUB',
            onTap: () => Navigator.pop(context, 2),
          ),
        ],
      ),
    );
    if (action == null) {
      return;
    }
    switch (action) {
      case 0:
        widget.state.importBook();
      case 1:
        widget.state.importBooks();
      case 2:
        widget.state.importBookDirectory();
    }
  }

  Future<void> _showCreateShelfDialog(BuildContext context) async {
    final palette = widget.state.palette;
    final controller = TextEditingController();
    final name = await showShelfFloatingDialog<String>(
      context: context,
      palette: palette,
      child: ShelfDialogPanel(
        palette: palette,
        title: '创建书架',
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, controller.text),
            child: const Text('创建'),
          ),
        ],
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '书架名',
              style: TextStyle(
                color: palette.text,
                fontSize: 16,
                fontWeight: AppTextWeight.medium,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              '用来单独收藏一组你想固定管理的书。',
              style: TextStyle(color: palette.muted, fontSize: 13, height: 1.4),
            ),
            const SizedBox(height: 14),
            TextField(
              controller: controller,
              autofocus: true,
              maxLength: 12,
              style: TextStyle(
                color: palette.text,
                fontSize: 18,
                fontWeight: AppTextWeight.medium,
              ),
              decoration: InputDecoration(
                hintText: '例如：校园、异世界',
                hintStyle: TextStyle(color: palette.subtle),
                counterStyle: TextStyle(color: palette.muted),
                filled: true,
                fillColor: palette.card,
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: 18,
                  vertical: 18,
                ),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(20),
                  borderSide: BorderSide.none,
                ),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(20),
                  borderSide: BorderSide.none,
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(20),
                  borderSide: BorderSide(
                    color: palette.accentText.withValues(alpha: .32),
                    width: 1.4,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
    controller.dispose();
    if (name == null || name.trim().isEmpty) {
      return;
    }
    await widget.state.createShelf(name);
    if (mounted) {
      setState(() => _selectedShelf = name.trim());
    }
  }

  Future<void> _showDeleteShelfDialog(BuildContext context) async {
    final palette = widget.state.palette;
    final shelfName = _selectedShelf;
    final confirm = await showShelfFloatingDialog<bool>(
      context: context,
      palette: palette,
      child: ShelfDialogPanel(
        palette: palette,
        title: '删除书架',
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('删除'),
          ),
        ],
        child: Padding(
          padding: const EdgeInsets.only(bottom: 8.0),
          child: Text(
            '确定要删除书架「$shelfName」吗？\n该书架内的书籍不会被删除，它们将返回“全部”列表。',
            style: TextStyle(color: palette.text, fontSize: 15, height: 1.6),
          ),
        ),
      ),
    );
    if (confirm != true) {
      return;
    }
    await widget.state.deleteShelf(shelfName);
    if (mounted) {
      setState(() => _selectedShelf = defaultShelfName);
    }
  }

  Future<void> _showAddExistingBooksToShelf(BuildContext context) async {
    final palette = widget.state.palette;
    final candidates = widget.state.books
        .where((book) => book.shelfName != _selectedShelf)
        .toList();
    if (candidates.isEmpty) {
      await showShelfFloatingDialog<void>(
        context: context,
        palette: palette,
        child: ShelfDialogPanel(
          palette: palette,
          title: '没有可加入的书',
          actions: [
            FilledButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('知道了'),
            ),
          ],
          child: Text(
            '全部书籍都已经在这个书架里了。',
            style: TextStyle(color: palette.muted, height: 1.5),
          ),
        ),
      );
      return;
    }
    final selectedIds = await showShelfFloatingSheet<Set<String>>(
      context: context,
      palette: palette,
      child: AddExistingBooksSheet(palette: palette, books: candidates),
    );
    if (selectedIds == null || selectedIds.isEmpty) {
      return;
    }
    await widget.state.moveBooksToShelf(selectedIds, _selectedShelf);
  }

  void _toggleSelection(BookEntry book) {
    setState(() {
      if (_selectedBookIds.contains(book.id)) {
        _selectedBookIds.remove(book.id);
        _manualSelectionHistory.remove(book.id);
      } else {
        _selectedBookIds.add(book.id);
        _recordManualEndpoint(book.id);
      }
      if (_selectedBookIds.isEmpty) {
        _manualSelectionHistory.clear();
        _selectionUndoStack.clear();
      }
    });
  }

  Future<void> _showMoveSheet(BuildContext context) async {
    HapticFeedback.selectionClick();
    final palette = widget.state.palette;
    final shelves = [defaultShelfName, ...widget.state.shelves];
    final selected = await showShelfFloatingSheet<String>(
      context: context,
      palette: palette,
      child: ShelfActionList(
        palette: palette,
        title: '移动到书架',
        children: [
          for (final shelf in shelves)
            ShelfActionTile(
              palette: palette,
              icon: shelf == defaultShelfName
                  ? Icons.library_books_rounded
                  : Icons.folder_rounded,
              title: shelf == defaultShelfName ? defaultShelfLabel : shelf,
              onTap: () => Navigator.pop(context, shelf),
            ),
        ],
      ),
    );
    if (selected != null) {
      await widget.state.moveBooksToShelf(
        _selectedBookIds,
        selected == defaultShelfName ? null : selected,
      );
      if (mounted) {
        setState(_clearSelection);
      }
    }
  }

  Future<void> _confirmDeleteSelected(BuildContext context) async {
    HapticFeedback.selectionClick();
    final palette = widget.state.palette;
    final count = _selectedBookIds.length;
    final confirm = await showShelfFloatingDialog<bool>(
      context: context,
      palette: palette,
      child: ShelfDialogPanel(
        palette: palette,
        title: '删除书籍',
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('删除'),
          ),
        ],
        child: Text(
          '确定删除这 $count 本书吗？本地导入记录和统计会从应用移除。',
          style: TextStyle(color: palette.muted, height: 1.5),
        ),
      ),
    );
    if (confirm == true) {
      final booksToRemove = widget.state.books
          .where((b) => _selectedBookIds.contains(b.id))
          .toList();
      for (final book in booksToRemove) {
        await widget.state.removeBook(book);
      }
      if (mounted) {
        setState(_clearSelection);
      }
    }
  }
}

class _BookBlockMasonry extends StatelessWidget {
  const _BookBlockMasonry({
    required this.blocks,
    required this.palette,
    required this.selectionMode,
    required this.selectedBookIds,
    required this.onTapBlock,
    required this.onLongPressBlock,
  });

  final List<ShelfBookBlock> blocks;
  final AppPalette palette;
  final bool selectionMode;
  final Set<String> selectedBookIds;
  final ValueChanged<ShelfBookBlock> onTapBlock;
  final ValueChanged<ShelfBookBlock> onLongPressBlock;

  static const _gap = 14.0;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final columnWidth = math.max((constraints.maxWidth - _gap) / 2, 120.0);
        final columns = [<ShelfBookBlock>[], <ShelfBookBlock>[]];
        final heights = [0.0, 0.0];

        for (final block in blocks) {
          final target = heights[0] <= heights[1] ? 0 : 1;
          columns[target].add(block);
          heights[target] += _estimateCardHeight(block, columnWidth) + _gap;
        }

        Widget buildColumn(List<ShelfBookBlock> columnBlocks) {
          return Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              for (var index = 0; index < columnBlocks.length; index++) ...[
                BookBlockGridCard(
                  block: columnBlocks[index],
                  palette: palette,
                  selectionMode: selectionMode,
                  selectedBookIds: selectedBookIds,
                  onTap: () => onTapBlock(columnBlocks[index]),
                  onLongPress: () => onLongPressBlock(columnBlocks[index]),
                ),
                if (index != columnBlocks.length - 1)
                  const SizedBox(height: _gap),
              ],
            ],
          );
        }

        return Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(child: buildColumn(columns[0])),
            const SizedBox(width: _gap),
            Expanded(child: buildColumn(columns[1])),
          ],
        );
      },
    );
  }

  double _estimateCardHeight(ShelfBookBlock block, double columnWidth) {
    final contentWidth = math.max(columnWidth - 28, 80.0);
    final visibleCount = block.books.isEmpty
        ? 1
        : math.min(block.books.length, 3);
    final stackWidth = math.max(contentWidth * .88, 104.0);
    final spread = visibleCount > 1 ? stackWidth * .07 : 0.0;
    final coverWidth = math.max(
      104.0,
      stackWidth - spread * (visibleCount - 1),
    );
    final coverHeight = coverWidth * 1.38;
    final textWidth = math.max(columnWidth - 28, 80.0);
    final charsPerLine = math.max((textWidth / 15.5).floor(), 5);
    final titleLines = (block.title.runes.length / charsPerLine).ceil().clamp(
      1,
      2,
    );
    return 14 + coverHeight + 11 + titleLines * 19 + 7 + 14 + 3 + 14 + 12 + 16;
  }
}

class _ReorderBookTile extends StatelessWidget {
  const _ReorderBookTile({
    super.key,
    required this.book,
    required this.index,
    required this.palette,
  });

  final BookEntry book;
  final int index;
  final AppPalette palette;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.fromLTRB(10, 8, 10, 8),
      decoration: BoxDecoration(
        color: palette.card,
        borderRadius: BorderRadius.circular(18),
      ),
      child: Row(
        children: [
          Text(
            '${index + 1}',
            textAlign: TextAlign.center,
            style: TextStyle(
              color: palette.muted,
              fontSize: 13,
              fontWeight: AppTextWeight.medium,
            ),
          ),
          const SizedBox(width: 10),
          BookCover(
            book: book,
            palette: palette,
            width: 38,
            height: 54,
            radius: 9,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  book.title,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color: palette.text,
                    fontSize: 15.2,
                    fontWeight: AppTextWeight.semibold,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  book.author,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(color: palette.muted, fontSize: 12.2),
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          Icon(Icons.drag_handle_rounded, color: palette.muted, size: 22),
        ],
      ),
    );
  }
}

class _MoveToGroupSheetWidget extends StatefulWidget {
  const _MoveToGroupSheetWidget({
    required this.palette,
    required this.targetBooksCount,
    required this.groups,
    required this.currentGroup,
    required this.bookCountForGroup,
  });

  final AppPalette palette;
  final int targetBooksCount;
  final List<String> groups;
  final String? currentGroup;
  final int Function(String groupName) bookCountForGroup;

  @override
  State<_MoveToGroupSheetWidget> createState() => _MoveToGroupSheetWidgetState();
}

class _MoveToGroupSheetWidgetState extends State<_MoveToGroupSheetWidget> {
  final TextEditingController _searchController = TextEditingController();
  String _query = '';

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final palette = widget.palette;
    final trimmedQuery = _query.trim();
    final filteredGroups = widget.groups.where((g) {
      if (trimmedQuery.isEmpty) return true;
      return g.toLowerCase().contains(trimmedQuery.toLowerCase());
    }).toList();

    final queryExactMatch = widget.groups.any(
      (g) => g.trim().toLowerCase() == trimmedQuery.toLowerCase(),
    );
    final showCreateOption = trimmedQuery.isNotEmpty && !queryExactMatch;

    return ConstrainedBox(
      constraints: BoxConstraints(
        maxHeight: MediaQuery.sizeOf(context).height * 0.75,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Header
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 18, 16, 8),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '移动到分组',
                        style: TextStyle(
                          color: palette.text,
                          fontSize: 18,
                          fontWeight: AppTextWeight.semibold,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        '选择目标分组后，这 ${widget.targetBooksCount} 本书会移动到该分组显示。',
                        style: TextStyle(
                          color: palette.muted,
                          fontSize: 12.5,
                          height: 1.35,
                        ),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  icon: Icon(Icons.close_rounded, color: palette.muted, size: 20),
                  onPressed: () => Navigator.pop(context),
                  visualDensity: VisualDensity.compact,
                ),
              ],
            ),
          ),

          // Search / Create Input Bar
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
            child: Container(
              height: 40,
              decoration: BoxDecoration(
                color: palette.cardAlt,
                borderRadius: BorderRadius.circular(12),
              ),
              child: TextField(
                controller: _searchController,
                style: TextStyle(color: palette.text, fontSize: 13.5),
                decoration: InputDecoration(
                  hintText: '搜索或输入新分组名称...',
                  hintStyle: TextStyle(color: palette.muted, fontSize: 13.5),
                  prefixIcon: Icon(Icons.search_rounded, size: 18, color: palette.muted),
                  suffixIcon: _query.isNotEmpty
                      ? IconButton(
                          icon: Icon(Icons.clear_rounded, size: 16, color: palette.muted),
                          onPressed: () {
                            _searchController.clear();
                            setState(() => _query = '');
                          },
                        )
                      : null,
                  border: InputBorder.none,
                  contentPadding: const EdgeInsets.symmetric(vertical: 10),
                ),
                onChanged: (val) => setState(() => _query = val),
              ),
            ),
          ),

          // Scrollable List of Groups
          Expanded(
            child: ListView(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
              physics: const BouncingScrollPhysics(),
              children: [
                if (showCreateOption)
                  _buildGroupTile(
                    palette: palette,
                    icon: Icons.add_circle_outline_rounded,
                    title: '创建并移动到「$trimmedQuery」',
                    isNew: true,
                    onTap: () => Navigator.pop(
                      context,
                      (clear: false, name: trimmedQuery),
                    ),
                  ),
                for (final group in filteredGroups)
                  _buildGroupTile(
                    palette: palette,
                    icon: Icons.folder_rounded,
                    title: group,
                    count: widget.bookCountForGroup(group),
                    isCurrent: group == widget.currentGroup,
                    onTap: () => Navigator.pop(
                      context,
                      (clear: false, name: group),
                    ),
                  ),
                if (filteredGroups.isEmpty && !showCreateOption)
                  Padding(
                    padding: const EdgeInsets.all(24),
                    child: Center(
                      child: Text(
                        '未找到匹配的分组',
                        style: TextStyle(color: palette.muted, fontSize: 13),
                      ),
                    ),
                  ),
              ],
            ),
          ),

          const Divider(height: 1),

          // Bottom Action: Reset Auto Grouping
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
            child: InkWell(
              borderRadius: BorderRadius.circular(12),
              onTap: () => Navigator.pop(context, (clear: true, name: null)),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                child: Row(
                  children: [
                    Icon(Icons.auto_awesome_rounded, size: 18, color: palette.muted),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            '恢复自动分组',
                            style: TextStyle(
                              color: palette.text,
                              fontSize: 13.5,
                              fontWeight: AppTextWeight.medium,
                            ),
                          ),
                          Text(
                            '清除手动分组，让应用按书名自动归类',
                            style: TextStyle(color: palette.muted, fontSize: 11),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildGroupTile({
    required AppPalette palette,
    required IconData icon,
    required String title,
    int? count,
    bool isCurrent = false,
    bool isNew = false,
    required VoidCallback onTap,
  }) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Material(
        color: isCurrent
            ? palette.primary.withValues(alpha: 0.12)
            : Colors.transparent,
        borderRadius: BorderRadius.circular(12),
        child: InkWell(
          borderRadius: BorderRadius.circular(12),
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
            child: Row(
              children: [
                Icon(
                  icon,
                  size: 20,
                  color: isCurrent
                      ? palette.primary
                      : (isNew ? palette.primary : palette.muted),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      color: isCurrent ? palette.primary : palette.text,
                      fontSize: 14,
                      fontWeight: isCurrent ? AppTextWeight.semibold : AppTextWeight.medium,
                    ),
                  ),
                ),
                if (count != null && count > 0)
                  Container(
                    margin: const EdgeInsets.only(left: 8),
                    padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
                    decoration: BoxDecoration(
                      color: palette.cardAlt,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      '$count 本',
                      style: TextStyle(
                        color: palette.muted,
                        fontSize: 11,
                        fontWeight: AppTextWeight.medium,
                      ),
                    ),
                  ),
                if (isCurrent)
                  Padding(
                    padding: const EdgeInsets.only(left: 8),
                    child: Icon(Icons.check_rounded, size: 18, color: palette.primary),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
