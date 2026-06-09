import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../app_state.dart';
import '../models.dart';
import '../typography.dart';
import '../widgets/book_cover.dart';
import 'reader_screen.dart';

enum _ShelfSortMode {
  manual('手动排序'),
  name('按名称'),
  recent('最近阅读');

  const _ShelfSortMode(this.label);

  final String label;
}

enum _BookMenuAction { edit, select }

enum _ShelfMenuAction { import, sort, create }

enum _ShelfDisplayMode { list, blocks }

class _ShelfBookBlock {
  const _ShelfBookBlock({
    required this.key,
    required this.title,
    required this.author,
    required this.books,
  });

  final String key;
  final String title;
  final String author;
  final List<BookEntry> books;

  int get chapterCount =>
      books.fold(0, (sum, book) => sum + book.chapters.length);

  int? get wordCount {
    var total = 0;
    var hasValue = false;
    for (final book in books) {
      final count = book.wordCount;
      if (count != null && count > 0) {
        hasValue = true;
        total += count;
      }
    }
    return hasValue ? total : null;
  }

  double get progress {
    if (books.isEmpty) {
      return 0;
    }
    return books.fold<double>(0, (sum, book) => sum + book.progress) /
        books.length;
  }

  DateTime get lastTouchedAt {
    return books
        .map((book) => book.lastReadAt ?? book.importedAt)
        .reduce((a, b) => a.isAfter(b) ? a : b);
  }
}

class _NaturalSortToken {
  const _NaturalSortToken.text(this.text) : number = null;
  const _NaturalSortToken.number(this.number) : text = null;

  final String? text;
  final int? number;
}

int _compareNaturalText(String left, String right) {
  final leftTokens = _naturalSortTokens(left);
  final rightTokens = _naturalSortTokens(right);
  final length = leftTokens.length < rightTokens.length
      ? leftTokens.length
      : rightTokens.length;
  for (var i = 0; i < length; i++) {
    final leftToken = leftTokens[i];
    final rightToken = rightTokens[i];
    final leftNumber = leftToken.number;
    final rightNumber = rightToken.number;
    if (leftNumber != null && rightNumber != null) {
      final compared = leftNumber.compareTo(rightNumber);
      if (compared != 0) {
        return compared;
      }
      continue;
    }
    if (leftNumber != null || rightNumber != null) {
      return leftNumber != null ? -1 : 1;
    }
    final compared = leftToken.text!.compareTo(rightToken.text!);
    if (compared != 0) {
      return compared;
    }
  }
  final byLength = leftTokens.length.compareTo(rightTokens.length);
  if (byLength != 0) {
    return byLength;
  }
  return left.toLowerCase().compareTo(right.toLowerCase());
}

List<_NaturalSortToken> _naturalSortTokens(String value) {
  final tokens = <_NaturalSortToken>[];
  final text = <int>[];
  final runes = value.trim().toLowerCase().runes.toList();

  void flushText() {
    if (text.isEmpty) {
      return;
    }
    tokens.add(_NaturalSortToken.text(String.fromCharCodes(text)));
    text.clear();
  }

  var index = 0;
  while (index < runes.length) {
    final rune = runes[index];
    if (_isAsciiDigit(rune)) {
      flushText();
      final start = index;
      while (index < runes.length && _isAsciiDigit(runes[index])) {
        index++;
      }
      final number = int.tryParse(
        String.fromCharCodes(runes.sublist(start, index)),
      );
      tokens.add(_NaturalSortToken.number(number ?? 0));
      continue;
    }
    if (_isChineseNumberRune(rune)) {
      flushText();
      final start = index;
      while (index < runes.length && _isChineseNumberRune(runes[index])) {
        index++;
      }
      tokens.add(
        _NaturalSortToken.number(
          _parseChineseNumber(runes.sublist(start, index)),
        ),
      );
      continue;
    }
    text.add(rune);
    index++;
  }
  flushText();
  return tokens;
}

bool _isAsciiDigit(int rune) => rune >= 48 && rune <= 57;

bool _isChineseNumberRune(int rune) {
  return _chineseDigitValue(rune) >= 0 || _chineseUnitValue(rune) > 0;
}

int _parseChineseNumber(List<int> runes) {
  var hasUnit = false;
  for (final rune in runes) {
    if (_chineseUnitValue(rune) > 0) {
      hasUnit = true;
      break;
    }
  }
  if (!hasUnit) {
    var value = 0;
    for (final rune in runes) {
      value = value * 10 + _chineseDigitValue(rune).clamp(0, 9).toInt();
    }
    return value;
  }

  var total = 0;
  var section = 0;
  var digit = 0;
  for (final rune in runes) {
    final digitValue = _chineseDigitValue(rune);
    if (digitValue >= 0) {
      digit = digitValue;
      continue;
    }
    final unit = _chineseUnitValue(rune);
    if (unit == 10000) {
      section += digit;
      total += (section == 0 ? 1 : section) * unit;
      section = 0;
      digit = 0;
    } else if (unit > 0) {
      section += (digit == 0 ? 1 : digit) * unit;
      digit = 0;
    }
  }
  return total + section + digit;
}

int _chineseDigitValue(int rune) {
  return switch (rune) {
    12295 || 38646 => 0,
    19968 => 1,
    20108 || 20004 => 2,
    19977 => 3,
    22235 => 4,
    20116 => 5,
    20845 => 6,
    19971 => 7,
    20843 => 8,
    20061 => 9,
    _ => -1,
  };
}

int _chineseUnitValue(int rune) {
  return switch (rune) {
    21313 => 10,
    30334 => 100,
    21315 => 1000,
    19975 => 10000,
    _ => 0,
  };
}

const _defaultShelfName = '已收藏';
const _defaultShelfLabel = '全部';

Future<T?> _showShelfFloatingSheet<T>({
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

Future<T?> _showShelfFloatingDialog<T>({
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

class ShelfScreen extends StatefulWidget {
  const ShelfScreen({super.key, required this.state});

  final AppState state;

  @override
  State<ShelfScreen> createState() => _ShelfScreenState();
}

class _ShelfScreenState extends State<ShelfScreen> {
  String _selectedShelf = _defaultShelfName;
  var _sortMode = _ShelfSortMode.manual;
  var _displayMode = _ShelfDisplayMode.list;
  final Set<String> _selectedBookIds = {};
  final Set<String> _expandedBlockKeys = {};
  final List<String> _manualSelectionHistory = [];
  final List<Set<String>> _selectionUndoStack = [];
  List<String> _visibleBookIds = const [];

  bool get _selectionMode => _selectedBookIds.isNotEmpty;

  @override
  Widget build(BuildContext context) {
    final state = widget.state;
    return AnimatedBuilder(
      animation: state.shelfChanges,
      builder: (context, _) {
        final palette = state.palette;
        final showingDefault = _selectedShelf == _defaultShelfName;
        final rawVisibleBooks = showingDefault
            ? state.books
            : state.books
                  .where((book) => book.shelfName == _selectedShelf)
                  .toList();
        final visibleBooks = _sortBooks(rawVisibleBooks);
        _visibleBookIds = [for (final book in visibleBooks) book.id];
        final blockDisplay =
            _displayMode == _ShelfDisplayMode.blocks && !_selectionMode;
        final bookBlocks = blockDisplay
            ? _buildBookBlocks(visibleBooks)
            : const <_ShelfBookBlock>[];
        final reorderable =
            !showingDefault &&
            !_selectionMode &&
            !blockDisplay &&
            _sortMode == _ShelfSortMode.manual &&
            visibleBooks.length > 1;
        final shelves = [_defaultShelfName, ...state.shelves];
        final selectedIndex = shelves
            .indexOf(_selectedShelf)
            .clamp(0, shelves.length - 1);
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
                            _CircleButton(
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
                        _ShelfTabs(
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
                              _selectedShelf == _defaultShelfName
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
                        if (!_selectionMode) ...[
                          const SizedBox(height: 16),
                          _ShelfDisplaySwitch(
                            palette: palette,
                            mode: _displayMode,
                            onChanged: (mode) {
                              HapticFeedback.selectionClick();
                              setState(() => _displayMode = mode);
                            },
                          ),
                        ],
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
                      padding: const EdgeInsets.fromLTRB(20, 10, 20, 112),
                      child: _EmptyShelf(
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
                    padding: const EdgeInsets.fromLTRB(20, 10, 20, 112),
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
                              return _BookBlockCard(
                                block: block,
                                palette: palette,
                                expanded: expanded,
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
                                onOpenBook: _openBook,
                                onLongPressBook: (book) =>
                                    _showBookMenu(context, book),
                              );
                            },
                          )
                        : reorderable
                        ? SliverToBoxAdapter(
                            child: ReorderableListView.builder(
                              shrinkWrap: true,
                              physics: const NeverScrollableScrollPhysics(),
                              buildDefaultDragHandles: false,
                              proxyDecorator: (child, index, animation) {
                                return AnimatedBuilder(
                                  animation: animation,
                                  child: child,
                                  builder: (context, child) {
                                    final t = Curves.easeOutCubic.transform(
                                      animation.value,
                                    );
                                    return Transform.scale(
                                      scale: 1 + t * .015,
                                      child: Opacity(
                                        opacity: .96,
                                        child: Material(
                                          type: MaterialType.transparency,
                                          child: child,
                                        ),
                                      ),
                                    );
                                  },
                                );
                              },
                              itemCount: visibleBooks.length,
                              onReorderItem: (oldIndex, newIndex) {
                                final reordered = [...visibleBooks];
                                final moved = reordered.removeAt(oldIndex);
                                reordered.insert(newIndex, moved);
                                widget.state.reorderBooksInShelf(
                                  shelfName: _selectedShelf,
                                  orderedIds: [
                                    for (final book in reordered) book.id,
                                  ],
                                );
                              },
                              itemBuilder: (context, index) {
                                final book = visibleBooks[index];
                                return Padding(
                                  key: ValueKey('shelf-${book.id}'),
                                  padding: EdgeInsets.only(
                                    bottom: index == visibleBooks.length - 1
                                        ? 0
                                        : 22,
                                  ),
                                  child: _BookTile(
                                    book: book,
                                    palette: palette,
                                    selected: false,
                                    selectionMode: false,
                                    trailing:
                                        ReorderableDelayedDragStartListener(
                                          index: index,
                                          child: Icon(
                                            Icons.drag_handle_rounded,
                                            color: palette.subtle,
                                          ),
                                        ),
                                    onTap: () => _openBook(book),
                                    onLongPress: () =>
                                        _showBookMenu(context, book),
                                  ),
                                );
                              },
                            ),
                          )
                        : SliverList.separated(
                            itemCount: visibleBooks.length,
                            separatorBuilder: (_, _) =>
                                const SizedBox(height: 22),
                            itemBuilder: (context, index) {
                              final book = visibleBooks[index];
                              return _BookTile(
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
              left: 20,
              right: 20,
              bottom: 18,
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
                      ? _SelectionBar(
                          key: const ValueKey('selection-bar'),
                          palette: palette,
                          rangeEnabled: _canSelectRange,
                          undoEnabled: _selectionUndoStack.isNotEmpty,
                          onSelectRange: _selectEndpointRange,
                          onUndo: _undoSelectionRange,
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

  List<BookEntry> _sortBooks(List<BookEntry> books) {
    final sorted = [...books];
    switch (_sortMode) {
      case _ShelfSortMode.manual:
        return sorted;
      case _ShelfSortMode.name:
        sorted.sort((a, b) {
          final byTitle = _compareNaturalText(a.title, b.title);
          if (byTitle != 0) {
            return byTitle;
          }
          return _compareNaturalText(a.author, b.author);
        });
        return sorted;
      case _ShelfSortMode.recent:
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

  List<_ShelfBookBlock> _buildBookBlocks(List<BookEntry> books) {
    final groups = <String, List<BookEntry>>{};
    final titles = <String, String>{};
    final authors = <String, String>{};
    for (final book in books) {
      final title = _blockTitleForBook(book);
      final author = book.author.trim();
      final key = '${author.toLowerCase()}::$title';
      groups.putIfAbsent(key, () => <BookEntry>[]).add(book);
      titles.putIfAbsent(key, () => title);
      authors.putIfAbsent(key, () => author);
    }
    final blocks = _mergeAdjacentVolumeBlocks([
      for (final entry in groups.entries)
        _ShelfBookBlock(
          key: entry.key,
          title: titles[entry.key] ?? entry.value.first.title,
          author: authors[entry.key] ?? entry.value.first.author,
          books: entry.value,
        ),
    ]);
    switch (_sortMode) {
      case _ShelfSortMode.name:
        blocks.sort((a, b) => _compareNaturalText(a.title, b.title));
      case _ShelfSortMode.recent:
        blocks.sort((a, b) => b.lastTouchedAt.compareTo(a.lastTouchedAt));
      case _ShelfSortMode.manual:
        break;
    }
    return blocks;
  }

  List<_ShelfBookBlock> _mergeAdjacentVolumeBlocks(
    List<_ShelfBookBlock> blocks,
  ) {
    if (blocks.length < 2) {
      return blocks;
    }
    final result = <_ShelfBookBlock>[];
    for (final block in blocks) {
      final firstVolume = _bookVolumeNumber(block.books.first);
      final candidateIndex = result.lastIndexWhere((candidate) {
        if (candidate.author.trim().toLowerCase() !=
            block.author.trim().toLowerCase()) {
          return false;
        }
        if (candidate.books.length < 2 || block.books.length != 1) {
          return false;
        }
        final maxVolume = candidate.books
            .map(_bookVolumeNumber)
            .whereType<int>()
            .fold<int?>(null, (max, value) {
              if (max == null || value > max) {
                return value;
              }
              return max;
            });
        return firstVolume != null &&
            maxVolume != null &&
            firstVolume == maxVolume + 1;
      });
      if (candidateIndex == -1) {
        result.add(block);
        continue;
      }
      final candidate = result[candidateIndex];
      result[candidateIndex] = _ShelfBookBlock(
        key: candidate.key,
        title: candidate.title,
        author: candidate.author,
        books: [...candidate.books, ...block.books],
      );
    }
    return result;
  }

  String _blockTitleForBook(BookEntry book) {
    var title = book.title.trim();
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
      RegExp(r'(?:第|卷)\s*([一二三四五六七八九十百千万零〇两]+)'),
      RegExp(r'([一二三四五六七八九十百千万零〇两]+)\s*(?:卷|册|集)\s*$'),
    ];
    for (final pattern in patterns) {
      final match = pattern.firstMatch(title);
      final raw = match?.group(1);
      if (raw == null) {
        continue;
      }
      return int.tryParse(raw) ?? _parseChineseNumber(raw.runes.toList());
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
    final action = await _showShelfFloatingSheet<_BookMenuAction>(
      context: context,
      palette: palette,
      child: _ShelfActionList(
        palette: palette,
        children: [
          _ShelfActionTile(
            palette: palette,
            icon: Icons.edit_rounded,
            title: '编辑书籍信息',
            subtitle: '修改标题和作者',
            onTap: () => Navigator.pop(context, _BookMenuAction.edit),
          ),
          _ShelfActionTile(
            palette: palette,
            icon: Icons.checklist_rounded,
            title: '选择多个',
            subtitle: '用于批量移动或删除',
            onTap: () => Navigator.pop(context, _BookMenuAction.select),
          ),
        ],
      ),
    );
    if (!mounted || !context.mounted || action == null) {
      return;
    }
    switch (action) {
      case _BookMenuAction.edit:
        await _showEditBookDialog(context, book);
      case _BookMenuAction.select:
        HapticFeedback.selectionClick();
        setState(() {
          _selectedBookIds.add(book.id);
          _recordManualEndpoint(book.id);
        });
    }
  }

  Future<void> _showEditBookDialog(BuildContext context, BookEntry book) async {
    final palette = widget.state.palette;
    final titleController = TextEditingController(text: book.title);
    final authorController = TextEditingController(text: book.author);
    final result = await _showShelfFloatingDialog<(String, String)>(
      context: context,
      palette: palette,
      child: _ShelfDialogPanel(
        palette: palette,
        title: '编辑书籍信息',
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, (
              titleController.text,
              authorController.text,
            )),
            child: const Text('保存'),
          ),
        ],
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            _ShelfTextField(
              controller: titleController,
              palette: palette,
              label: '标题',
              autofocus: true,
            ),
            const SizedBox(height: 12),
            _ShelfTextField(
              controller: authorController,
              palette: palette,
              label: '作者',
            ),
          ],
        ),
      ),
    );
    final title = result?.$1;
    final author = result?.$2;
    titleController.dispose();
    authorController.dispose();
    if (title == null || author == null || title.trim().isEmpty) {
      return;
    }
    await widget.state.updateBookMetadata(book, title: title, author: author);
  }

  Future<void> _showSortSheet(BuildContext context) async {
    final palette = widget.state.palette;
    final selected = await _showShelfFloatingSheet<_ShelfSortMode>(
      context: context,
      palette: palette,
      child: _ShelfActionList(
        palette: palette,
        children: [
          for (final mode in _ShelfSortMode.values)
            _ShelfActionTile(
              palette: palette,
              icon: mode == _ShelfSortMode.manual
                  ? Icons.drag_handle_rounded
                  : mode == _ShelfSortMode.name
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
    final action = await _showShelfFloatingSheet<_ShelfMenuAction>(
      context: context,
      palette: palette,
      child: _ShelfActionList(
        palette: palette,
        children: [
          _ShelfActionTile(
            palette: palette,
            icon: Icons.upload_file_rounded,
            title: '导入书籍',
            subtitle: '单本、多本或导入整个文件夹',
            onTap: () => Navigator.pop(context, _ShelfMenuAction.import),
          ),
          _ShelfActionTile(
            palette: palette,
            icon: Icons.sort_rounded,
            title: '排序方式',
            subtitle: _sortMode.label,
            onTap: () => Navigator.pop(context, _ShelfMenuAction.sort),
          ),
          _ShelfActionTile(
            palette: palette,
            icon: Icons.create_new_folder_rounded,
            title: '创建书架',
            subtitle: '创建一个新的分类入口',
            onTap: () => Navigator.pop(context, _ShelfMenuAction.create),
          ),
        ],
      ),
    );
    if (!mounted || !context.mounted || action == null) {
      return;
    }
    switch (action) {
      case _ShelfMenuAction.import:
        await _showImportSheet(context);
      case _ShelfMenuAction.sort:
        await _showSortSheet(context);
      case _ShelfMenuAction.create:
        await _showCreateShelfDialog(context);
    }
  }

  Future<void> _showImportSheet(BuildContext context) async {
    final palette = widget.state.palette;
    final action = await _showShelfFloatingSheet<int>(
      context: context,
      palette: palette,
      child: _ShelfActionList(
        palette: palette,
        children: [
          _ShelfActionTile(
            palette: palette,
            icon: Icons.insert_drive_file_rounded,
            title: '导入单本',
            subtitle: '选择一个 TXT 或 EPUB',
            onTap: () => Navigator.pop(context, 0),
          ),
          _ShelfActionTile(
            palette: palette,
            icon: Icons.file_upload_rounded,
            title: '批量导入',
            subtitle: '一次选择多本 TXT / EPUB',
            onTap: () => Navigator.pop(context, 1),
          ),
          _ShelfActionTile(
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
    final name = await _showShelfFloatingDialog<String>(
      context: context,
      palette: palette,
      child: _ShelfDialogPanel(
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
        child: _ShelfTextField(
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
      await _showShelfFloatingDialog<void>(
        context: context,
        palette: palette,
        child: _ShelfDialogPanel(
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
    final selectedIds = await _showShelfFloatingSheet<Set<String>>(
      context: context,
      palette: palette,
      child: _AddExistingBooksSheet(palette: palette, books: candidates),
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
    final shelves = [_defaultShelfName, ...widget.state.shelves];
    final selected = await _showShelfFloatingSheet<String>(
      context: context,
      palette: palette,
      child: _ShelfActionList(
        palette: palette,
        title: '移动到书架',
        children: [
          for (final shelf in shelves)
            _ShelfActionTile(
              palette: palette,
              icon: shelf == _defaultShelfName
                  ? Icons.library_books_rounded
                  : Icons.folder_rounded,
              title: shelf == _defaultShelfName ? _defaultShelfLabel : shelf,
              onTap: () => Navigator.pop(context, shelf),
            ),
        ],
      ),
    );
    if (selected != null) {
      await widget.state.moveBooksToShelf(
        _selectedBookIds,
        selected == _defaultShelfName ? null : selected,
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
    final confirm = await _showShelfFloatingDialog<bool>(
      context: context,
      palette: palette,
      child: _ShelfDialogPanel(
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
      child: Material(
        color: palette.surface,
        elevation: palette.isLight ? 1 : 2,
        shadowColor: Colors.black.withValues(
          alpha: palette.isLight ? .12 : .28,
        ),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(28),
          side: BorderSide(color: palette.line),
        ),
        clipBehavior: Clip.antiAlias,
        child: child,
      ),
    );
  }
}

class _ShelfActionList extends StatelessWidget {
  const _ShelfActionList({
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

class _ShelfActionTile extends StatelessWidget {
  const _ShelfActionTile({
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

class _ShelfDialogPanel extends StatelessWidget {
  const _ShelfDialogPanel({
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
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              for (var i = 0; i < actions.length; i++) ...[
                actions[i],
                if (i != actions.length - 1) const SizedBox(width: 10),
              ],
            ],
          ),
        ],
      ),
    );
  }
}

class _ShelfTextField extends StatelessWidget {
  const _ShelfTextField({
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
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(16)),
      ),
    );
  }
}

class _AddExistingBooksSheet extends StatefulWidget {
  const _AddExistingBooksSheet({required this.palette, required this.books});

  final AppPalette palette;
  final List<BookEntry> books;

  @override
  State<_AddExistingBooksSheet> createState() => _AddExistingBooksSheetState();
}

class _AddExistingBooksSheetState extends State<_AddExistingBooksSheet> {
  final Set<String> _selectedIds = {};

  @override
  Widget build(BuildContext context) {
    final palette = widget.palette;
    final books = [...widget.books]
      ..sort((a, b) {
        final byTitle = _compareNaturalText(a.title, b.title);
        if (byTitle != 0) {
          return byTitle;
        }
        return _compareNaturalText(a.author, b.author);
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
              separatorBuilder: (_, _) =>
                  Divider(height: 1, color: palette.line),
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

class _SelectionBar extends StatelessWidget {
  const _SelectionBar({
    super.key,
    required this.palette,
    required this.rangeEnabled,
    required this.undoEnabled,
    required this.onSelectRange,
    required this.onUndo,
    required this.onMove,
    required this.onDelete,
  });

  final AppPalette palette;
  final bool rangeEnabled;
  final bool undoEnabled;
  final VoidCallback onSelectRange;
  final VoidCallback onUndo;
  final VoidCallback onMove;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    Widget compactAction({
      required IconData icon,
      required String label,
      required Color color,
      required VoidCallback? onPressed,
    }) {
      final enabled = onPressed != null;
      final effectiveColor = enabled ? color : palette.muted;
      return TextButton.icon(
        style: TextButton.styleFrom(
          foregroundColor: effectiveColor,
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
          tapTargetSize: MaterialTapTargetSize.shrinkWrap,
        ),
        onPressed: onPressed,
        icon: Icon(icon, size: 19),
        label: Text(
          label,
          maxLines: 1,
          style: TextStyle(
            color: effectiveColor,
            fontWeight: AppTextWeight.medium,
          ),
        ),
      );
    }

    Widget divider() => Container(width: 1, height: 24, color: palette.line);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: palette.surface,
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: palette.line),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: .3),
            blurRadius: 16,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: [
            compactAction(
              icon: Icons.select_all_rounded,
              label: '选中区间',
              color: palette.text,
              onPressed: rangeEnabled ? onSelectRange : null,
            ),
            divider(),
            compactAction(
              icon: Icons.undo_rounded,
              label: '撤回',
              color: palette.text,
              onPressed: undoEnabled ? onUndo : null,
            ),
            divider(),
            TextButton.icon(
              onPressed: onMove,
              icon: Icon(Icons.drive_file_move_rounded, color: palette.text),
              label: Text('移动到书架', style: TextStyle(color: palette.text)),
            ),
            Container(width: 1, height: 24, color: palette.line),
            TextButton.icon(
              onPressed: onDelete,
              icon: Icon(Icons.delete_rounded, color: palette.accentText),
              label: Text('删除', style: TextStyle(color: palette.accentText)),
            ),
          ],
        ),
      ),
    );
  }
}

class _ShelfDisplaySwitch extends StatelessWidget {
  const _ShelfDisplaySwitch({
    required this.palette,
    required this.mode,
    required this.onChanged,
  });

  final AppPalette palette;
  final _ShelfDisplayMode mode;
  final ValueChanged<_ShelfDisplayMode> onChanged;

  @override
  Widget build(BuildContext context) {
    Widget item({
      required _ShelfDisplayMode value,
      required IconData icon,
      required String label,
    }) {
      final selected = mode == value;
      return Expanded(
        child: InkWell(
          borderRadius: BorderRadius.circular(999),
          onTap: () => onChanged(value),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 180),
            curve: Curves.easeOutCubic,
            padding: const EdgeInsets.symmetric(vertical: 10),
            decoration: BoxDecoration(
              color: selected
                  ? palette.primarySoft.withValues(
                      alpha: palette.isLight ? .3 : .24,
                    )
                  : Colors.transparent,
              borderRadius: BorderRadius.circular(999),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  icon,
                  size: 18,
                  color: selected ? palette.accentText : palette.muted,
                ),
                const SizedBox(width: 6),
                Text(
                  label,
                  style: TextStyle(
                    color: selected ? palette.accentText : palette.muted,
                    fontSize: 13,
                    fontWeight: selected
                        ? AppTextWeight.semibold
                        : AppTextWeight.medium,
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }

    return Container(
      padding: const EdgeInsets.all(5),
      decoration: BoxDecoration(
        color: palette.surface,
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: palette.line),
      ),
      child: Row(
        children: [
          item(
            value: _ShelfDisplayMode.list,
            icon: Icons.view_agenda_outlined,
            label: '列表',
          ),
          item(
            value: _ShelfDisplayMode.blocks,
            icon: Icons.dashboard_customize_outlined,
            label: '分块',
          ),
        ],
      ),
    );
  }
}

class _BookBlockCard extends StatelessWidget {
  const _BookBlockCard({
    required this.block,
    required this.palette,
    required this.expanded,
    required this.onToggle,
    required this.onOpenBook,
    required this.onLongPressBook,
  });

  final _ShelfBookBlock block;
  final AppPalette palette;
  final bool expanded;
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
        border: Border.all(color: palette.line),
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
                          const SizedBox(height: 10),
                          _ShelfProgressLine(
                            progress: block.progress.clamp(0, 1),
                            palette: palette,
                          ),
                        ],
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
            if (expanded) Divider(height: 1, color: palette.line),
            if (expanded) const SizedBox(height: 12),
            if (expanded)
              AnimatedSize(
                duration: const Duration(milliseconds: 220),
                curve: Curves.easeOutCubic,
                alignment: Alignment.topCenter,
                child: Column(
                  children: [
                    for (
                      var index = 0;
                      index < visibleBooks.length;
                      index++
                    ) ...[
                      if (index > 0)
                        Padding(
                          padding: const EdgeInsets.symmetric(vertical: 12),
                          child: Divider(height: 1, color: palette.line),
                        ),
                      _BookBlockRow(
                        book: visibleBooks[index],
                        palette: palette,
                        onTap: () => onOpenBook(visibleBooks[index]),
                        onLongPress: () => onLongPressBook(visibleBooks[index]),
                      ),
                    ],
                  ],
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
    required this.onTap,
    required this.onLongPress,
  });

  final BookEntry book;
  final AppPalette palette;
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
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
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
                  Wrap(
                    spacing: 6,
                    runSpacing: 6,
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
                  const Spacer(),
                  _ShelfProgressLine(progress: book.progress, palette: palette),
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

class _BookTile extends StatelessWidget {
  const _BookTile({
    required this.book,
    required this.palette,
    required this.selected,
    required this.selectionMode,
    required this.onTap,
    required this.onLongPress,
    this.trailing,
  });

  final BookEntry book;
  final AppPalette palette;
  final bool selected;
  final bool selectionMode;
  final VoidCallback onTap;
  final VoidCallback onLongPress;
  final Widget? trailing;

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
                              if (!selectionMode && trailing != null)
                                SizedBox(
                                  width: 34,
                                  height: 34,
                                  child: trailing,
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
      crossAxisAlignment: CrossAxisAlignment.end,
      children: [
        Transform.translate(
          offset: const Offset(0, 2),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.timelapse_rounded, size: 13, color: palette.muted),
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

class _EmptyShelf extends StatelessWidget {
  const _EmptyShelf({
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
          border: Border.all(color: palette.line),
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

class _ShelfTabs extends StatelessWidget {
  const _ShelfTabs({
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
                          shelves[i] == _defaultShelfName
                              ? _defaultShelfLabel
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

class _CircleButton extends StatelessWidget {
  const _CircleButton({
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
        decoration: BoxDecoration(
          color: palette.card,
          shape: BoxShape.circle,
          border: Border.all(color: palette.line),
        ),
        child: Icon(icon, color: palette.muted, size: 25),
      ),
    );
  }
}
