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
  final Set<String> _expandedBlockKeys = {};
  final List<String> _manualSelectionHistory = [];
  final List<Set<String>> _selectionUndoStack = [];
  List<String> _visibleBookIds = const [];
  var _reportedSelectionMode = false;

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
                  padding: const EdgeInsets.fromLTRB(20, 30, 20, 8),
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
                            CircleButton(
                              palette: palette,
                              icon: _selectionMode
                                  ? Icons.close_rounded
                                  : Icons.more_vert_rounded,
                              onTap: () {
                                HapticFeedback.selectionClick();
                                if (_selectionMode) {
                                  setState(_clearSelection);
                                } else {
                                  _showShelfMenu(context);
                                }
                              },
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
                        const SizedBox(height: 24),
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
                        ? SliverList.separated(
                            itemCount: bookBlocks.length,
                            separatorBuilder: (_, _) =>
                                const SizedBox(height: 16),
                            itemBuilder: (context, index) {
                              final block = bookBlocks[index];
                              final expanded = _expandedBlockKeys.contains(
                                block.key,
                              );
                              return BookBlockCard(
                                block: block,
                                palette: palette,
                                expanded: expanded,
                                selectionMode: _selectionMode,
                                selectedBookIds: _selectedBookIds,
                                onToggle: () {
                                  HapticFeedback.selectionClick();
                                  setState(() {
                                    if (expanded) {
                                      _expandedBlockKeys.remove(block.key);
                                    } else {
                                      _expandedBlockKeys.add(block.key);
                                    }
                                  });
                                },
                                onOpenBook: (book) {
                                  if (_selectionMode) {
                                    HapticFeedback.selectionClick();
                                    _toggleSelection(book);
                                  } else {
                                    _openBook(book);
                                  }
                                },
                                onLongPressBook: (book) {
                                  if (_selectionMode) {
                                    HapticFeedback.selectionClick();
                                    _toggleSelection(book);
                                  } else {
                                    _showBookMenu(context, book);
                                  }
                                },
                              );
                            },
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
                                  _showBookMenu(context, book);
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
          ? 'manual::${_seriesKeyForTitle(title)}'
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
        title: candidate.title,
        author: candidate.author,
        books: _sortBlockBooks([...candidate.books, ...block.books]),
        manual: candidate.manual,
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
      return candidateKey == blockKey;
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
    var cleaned = title.toLowerCase();
    const aliases = <String, String>{
      '我的妹妹不可能那么可爱': '我的妹妹哪有这么可爱',
      '我的妹妹不可能这么可爱': '我的妹妹哪有这么可爱',
      '我的妹妹不可能那麼可愛': '我的妹妹哪有这么可爱',
      '我的妹妹哪有那麼可愛': '我的妹妹哪有这么可爱',
    };
    for (final entry in aliases.entries) {
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
    var title = book.title.trim();
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

  Future<void> _showMoveToGroupSheet(
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
      return;
    }
    HapticFeedback.selectionClick();
    final palette = widget.state.palette;
    final groups = _availableSeriesGroups(excludingIds: bookIds);
    final result = await showShelfFloatingSheet<({bool clear, String? name})>(
      context: context,
      palette: palette,
      child: ShelfActionList(
        palette: palette,
        title: '移动到分组',
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(14, 0, 14, 8),
            child: Text(
              '选择目标分组后，这 ${targetBooks.length} 本书会移动到该分组显示。',
              style: TextStyle(color: palette.muted, height: 1.45),
            ),
          ),
          for (final group in groups)
            ShelfActionTile(
              palette: palette,
              icon: Icons.folder_rounded,
              title: group,
              subtitle: group == _effectiveSeriesName(targetBooks.first)
                  ? '当前分组'
                  : null,
              onTap: () => Navigator.pop(context, (clear: false, name: group)),
            ),
          ShelfActionTile(
            palette: palette,
            icon: Icons.auto_awesome_rounded,
            title: '恢复自动分组',
            subtitle: '清除手动分组，让应用重新按书名归类',
            onTap: () => Navigator.pop(context, (clear: true, name: null)),
          ),
        ],
      ),
    );
    if (!context.mounted || result == null) {
      return;
    }
    await widget.state.updateBookSeriesOverride(
      bookIds,
      result.clear ? null : result.name,
    );
    if (mounted && books == null) {
      setState(_clearSelection);
    }
  }

  List<String> _availableSeriesGroups({Set<String> excludingIds = const {}}) {
    final names = <String>{};
    for (final book in widget.state.books) {
      if (excludingIds.contains(book.id)) {
        continue;
      }
      final name = _effectiveSeriesName(book).trim();
      if (name.isNotEmpty) {
        names.add(name);
      }
    }
    if (names.isEmpty) {
      names.addAll(_targetFallbackGroups(excludingIds));
    }
    final sorted = names.toList()..sort((a, b) => compareNaturalText(a, b));
    return sorted;
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

  Future<void> _showShelfMenu(BuildContext context) async {
    final palette = widget.state.palette;
    final action = await showShelfFloatingSheet<ShelfMenuAction>(
      context: context,
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
        child: ShelfTextField(
          controller: controller,
          palette: palette,
          label: '书架名',
          hintText: '例如：校园、异世界',
          maxLength: 12,
          autofocus: true,
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
