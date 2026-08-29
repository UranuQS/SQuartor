import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../models.dart';
import 'reader_bookmark_pull.dart';
import 'reader_enums.dart';
import 'reader_state_fields.dart';

mixin ReaderBookmarkMixin<T extends ReaderScreenWidget>
    on ReaderStateFields<T> {
  static const _startMiddleMin = .20;
  static const _startMiddleMax = .78;

  var _bookmarkPullCandidate = false;
  Offset? _bookmarkPullStartGlobal;
  static const _maxPagePull = 156.0;

  @override
  bool get currentPageBookmarked {
    return currentPageBookmark != null;
  }

  BookBookmark? get currentPageBookmark {
    for (final item in book.bookmarks) {
      if (item.chapterIndex == chapterIndex && item.page == page) {
        return item;
      }
    }
    return null;
  }

  @override
  double readerBookmarkPullOffset() {
    final returning = bookmarkPullReturn;
    if (returning != null && bookmarkPullReturnAnimation.isAnimating) {
      return returning.value;
    }
    return bookmarkPullDy;
  }

  @override
  double readerContentPullOffset() {
    final bookmarkOffset = readerBookmarkPullOffset();
    if (bookmarkOffset > 0) {
      return bookmarkOffset;
    }
    final direction = scrollEdgeTurnDirection;
    if (!usesVerticalScroll ||
        direction == null ||
        scrollEdgeTurnProgress <= 0) {
      return 0;
    }
    final eased = Curves.easeOutCubic.transform(
      scrollEdgeTurnProgress.clamp(0.0, 1.0).toDouble(),
    );
    final offset = ReaderBookmarkPullOverlay.threshold * eased;
    return direction == ScrollEdgeTurnDirection.previous ? offset : -offset;
  }

  @override
  void setScrollEdgeTurnProgress(
    ScrollEdgeTurnDirection? direction,
    double progress,
  ) {
    if (!mounted) {
      return;
    }
    final clamped = progress.clamp(0.0, 1.0).toDouble();
    if (scrollEdgeTurnDirection == direction &&
        (scrollEdgeTurnProgress - clamped).abs() < .015) {
      return;
    }
    setState(() {
      scrollEdgeTurnDirection = clamped <= 0 ? null : direction;
      scrollEdgeTurnProgress = clamped;
    });
  }

  @override
  void onBookmarkPullStart(DragStartDetails details, Size screenSize) {
    if (isLoading || loadError != null) {
      return;
    }
    if (overlay != ReaderOverlay.hidden && overlay != ReaderOverlay.chrome) {
      return;
    }
    final localY = details.localPosition.dy;
    final top = screenSize.height * _startMiddleMin;
    final bottom = screenSize.height * _startMiddleMax;
    _bookmarkPullCandidate = localY >= top && localY <= bottom;
    _bookmarkPullStartGlobal = details.globalPosition;
    bookmarkPullReturnAnimation.stop();
    bookmarkPullReturn = null;
  }

  @override
  void onBookmarkPullUpdate(DragUpdateDetails details) {
    if (!_bookmarkPullCandidate) {
      return;
    }
    final start = _bookmarkPullStartGlobal;
    if (start == null) {
      return;
    }
    final delta = details.globalPosition - start;
    if (!bookmarkPullActive) {
      if (delta.dy < 14 || delta.dy < delta.dx.abs() * 1.45) {
        return;
      }
      setState(() {
        bookmarkPullActive = true;
        bookmarkPullCommitted = false;
        bookmarkPullDy = 0;
      });
    }
    final dy = math.max(0.0, delta.dy);
    final damped = _maxPagePull * (1 - math.exp(-dy / 132));
    setState(() {
      bookmarkPullDy = damped.clamp(0.0, _maxPagePull);
    });
  }

  @override
  Future<void> onBookmarkPullEnd(DragEndDetails details) async {
    _bookmarkPullCandidate = false;
    _bookmarkPullStartGlobal = null;
    if (!bookmarkPullActive) {
      return;
    }
    final shouldCommit = bookmarkPullDy >= ReaderBookmarkPullOverlay.threshold;
    if (shouldCommit) {
      setState(() => bookmarkPullCommitted = true);
      await toggleCurrentBookmark();
      await Future<void>.delayed(const Duration(milliseconds: 120));
    }
    if (!mounted) {
      return;
    }
    await animateBookmarkPullBack();
  }

  @override
  void onBookmarkPullCancel() {
    _bookmarkPullCandidate = false;
    _bookmarkPullStartGlobal = null;
    if (!bookmarkPullActive) {
      return;
    }
    unawaited(animateBookmarkPullBack());
  }

  Future<void> animateBookmarkPullBack() async {
    final from = readerBookmarkPullOffset();
    bookmarkPullReturnAnimation.stop();
    bookmarkPullReturn = Tween<double>(begin: from, end: 0).animate(
      CurvedAnimation(
        parent: bookmarkPullReturnAnimation,
        curve: Curves.easeOutBack,
      ),
    );
    bookmarkPullReturnAnimation
      ..duration = const Duration(milliseconds: 360)
      ..reset();
    setState(() {
      bookmarkPullActive = false;
      bookmarkPullDy = 0;
    });
    await bookmarkPullReturnAnimation.forward();
    if (!mounted) {
      return;
    }
    setState(() {
      bookmarkPullCommitted = false;
      bookmarkPullReturn = null;
    });
  }

  @override
  Future<void> addCurrentBookmark() async {
    final safePageCount = pageCount < 1 ? 1 : pageCount;
    final safePage = page.clamp(0, safePageCount - 1);
    final progress = safePageCount <= 1 ? 0.0 : safePage / (safePageCount - 1);
    final bookmark = BookBookmark(
      id: '${DateTime.now().microsecondsSinceEpoch}-$chapterIndex-$safePage',
      chapterIndex: chapterIndex,
      page: safePage,
      pageCount: safePageCount,
      progress: progress,
      createdAt: DateTime.now(),
      snippet: currentBookmarkSnippet(),
    );
    await appState.addBookBookmark(book, bookmark);
    await _syncReaderBookFromState();
  }

  Future<void> toggleCurrentBookmark() async {
    final existing = currentPageBookmark;
    if (existing != null) {
      await HapticFeedback.selectionClick();
      await appState.removeBookBookmark(book, existing.id);
      await _syncReaderBookFromState();
      return;
    }
    await HapticFeedback.selectionClick();
    await addCurrentBookmark();
  }

  Future<void> _syncReaderBookFromState() async {
    if (!mounted) {
      return;
    }
    BookEntry? updated;
    for (final item in appState.books) {
      if (item.id == book.id) {
        updated = item;
        break;
      }
    }
    if (updated != null) {
      final updatedBook = updated;
      setState(() => readerBook = updatedBook);
    }
  }

  @override
  String currentBookmarkSnippet() {
    final chapterTitle = currentChapter.title.trim();
    final text = _currentVisibleText().replaceAll(RegExp(r'\s+'), ' ').trim();
    if (text.isEmpty) {
      return chapterTitle.isEmpty
          ? '\u7b2c ${chapterIndex + 1} \u7ae0'
          : chapterTitle;
    }
    return text.characters.take(72).toString();
  }

  String _currentVisibleText() {
    if (usesFlutterTxt) {
      if (usesVerticalScroll) {
        final blocks = txtScrollBlocks
            .where((block) => block.text.trim().isNotEmpty)
            .map((block) => block.text.trim());
        return blocks.take(3).join(' ');
      }
      if (txtPages.isNotEmpty) {
        final safePage = page.clamp(0, txtPages.length - 1);
        return txtPages[safePage].blocks
            .where((block) => block.text.trim().isNotEmpty)
            .map((block) => block.text.trim())
            .take(3)
            .join(' ');
      }
    }
    return currentChapter.title;
  }
}
