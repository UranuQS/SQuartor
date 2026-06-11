import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:path/path.dart' as path;

import 'reader_state_fields.dart';

mixin ReaderNavigationMixin<T extends ReaderScreenWidget>
    on ReaderStateFields<T> {
  @override
  void seekToOverallProgress(double progress) {
    final chapterCount = book.chapters.length;
    if (chapterCount <= 0) {
      return;
    }
    final clamped = progress.clamp(0.0, 1.0).toDouble();
    final targetIndex = clamped >= .999
        ? chapterCount - 1
        : (clamped * chapterCount).floor().clamp(0, chapterCount - 1);
    if (targetIndex == chapterIndex) {
      return;
    }
    final serial = ++progressSeekSerial;
    progressSeekTimer?.cancel();
    progressSeekTimer = Timer(const Duration(milliseconds: 260), () {
      if (!mounted || serial != progressSeekSerial) {
        return;
      }
      requestProgressSeekToChapter(targetIndex);
    });
  }

  @override
  void requestProgressSeekToChapter(int targetIndex) {
    if (targetIndex < 0 || targetIndex > lastChapterIndex) {
      return;
    }
    if (isLoading) {
      pendingProgressSeekChapter = targetIndex;
      return;
    }
    if (targetIndex == chapterIndex) {
      pendingProgressSeekChapter = null;
      return;
    }
    pendingProgressSeekChapter = null;
    unawaited(goToChapter(targetIndex));
  }

  @override
  void flushPendingProgressSeek() {
    final target = pendingProgressSeekChapter;
    if (target == null || isLoading) {
      return;
    }
    pendingProgressSeekChapter = null;
    if (target != chapterIndex) {
      unawaited(goToChapter(target));
    }
  }

  @override
  void cancelPendingProgressSeek() {
    progressSeekTimer?.cancel();
    progressSeekSerial++;
    pendingProgressSeekChapter = null;
  }

  @override
  Future<void> goToChapter(
    int index, {
    bool atEnd = false,
    double? progress,
    String? anchor,
  }) async {
    if (index < 0 || index > lastChapterIndex || book.chapters.isEmpty) {
      return;
    }
    progressSeekTimer?.cancel();
    progressSeekSerial++;
    pendingProgressSeekChapter = null;
    final targetChapter = book.chapters[index];
    final targetAnchor = anchor ?? targetChapter.anchor;
    final targetProgress = progress ?? (atEnd ? 1.0 : 0.0);
    final currentPath = path.normalize(currentChapter.filePath);
    final targetPath = path.normalize(targetChapter.filePath);
    final token = readerNavigationToken + 1;
    clearReaderSnapshot(notify: false);
    setState(() {
      readerNavigationToken = token;
      chapterIndex = index;
      page = 0;
      pageCount = 1;
      pendingPageProgress = targetProgress;
      pendingExactPage = null;
      pendingExactPageCount = null;
      pendingAnchor = targetAnchor;
      pendingWebJumpToEnd = atEnd && anchor == null;
      pendingWebLoadPath = currentPath == targetPath ? null : targetPath;
      isLoading = true;
      loadError = null;
      if (usesFlutterTxt) {
        txtPages = const [];
        txtScrollBlocks = const [];
        txtPaginationSignature = null;
        txtRequestedSignature = null;
      }
    });
    if (usesFlutterTxt) {
      final metrics = txtLayoutMetrics;
      if (metrics != null) {
        requestTxtPagination(metrics);
      }
      return;
    }
    if (currentPath == targetPath) {
      Object? jumpResult;
      await controller?.evaluateJavascript(
        source:
            'if (window.SQuartor) window.SQuartor.token = ${jsonEncode(token)};',
      );
      if (atEnd && anchor == null) {
        final targetEndAnchor = chapterEndAnchor(index);
        jumpResult = await controller?.evaluateJavascript(
          source:
              'window.SQuartor && window.SQuartor.jumpToChapterEnd(${jsonEncode(targetEndAnchor)});',
        );
        if (readerJumpStatus(jumpResult) == 'missing') {
          jumpResult = await controller?.evaluateJavascript(
            source:
                'window.SQuartor && window.SQuartor.jumpToProgress($targetProgress);',
          );
        }
      } else if (targetAnchor != null && targetAnchor.isNotEmpty) {
        jumpResult = await controller?.evaluateJavascript(
          source:
              'window.SQuartor && window.SQuartor.jumpToAnchor(${jsonEncode(targetAnchor)});',
        );
        if (readerJumpStatus(jumpResult) == 'missing') {
          jumpResult = await controller?.evaluateJavascript(
            source:
                'window.SQuartor && window.SQuartor.jumpToProgress($targetProgress);',
          );
        }
      } else {
        jumpResult = await controller?.evaluateJavascript(
          source:
              'window.SQuartor && window.SQuartor.jumpToProgress($targetProgress);',
        );
      }
      if (!mounted) {
        return;
      }
      if (!isCurrentReaderToken(token)) {
        return;
      }
      applyReaderProgressPayload(jumpResult, expectedToken: token);
      setState(() {
        pendingPageProgress = null;
        pendingAnchor = null;
        pendingWebJumpToEnd = false;
        pendingWebLoadPath = null;
        isLoading = false;
        loadError = null;
      });
      flushPendingProgressSeek();
      return;
    }
    await controller?.loadUrl(
      urlRequest: URLRequest(url: WebUri.uri(File(targetChapter.filePath).uri)),
    );
  }

  @override
  Future<void> nextPage() async {
    if (usesFlutterTxt) {
      await nextTxtPage();
      return;
    }
    final result = await controller?.evaluateJavascript(
      source: 'window.SQuartor && window.SQuartor.nextPage();',
    );
    if (result == 'end') {
      await goToChapter(chapterIndex + 1);
    }
  }

  @override
  Future<void> previousPage() async {
    if (usesFlutterTxt) {
      await previousTxtPage();
      return;
    }
    final result = await controller?.evaluateJavascript(
      source: 'window.SQuartor && window.SQuartor.previousPage();',
    );
    if (result == 'start') {
      await goToChapter(chapterIndex - 1, atEnd: true);
    }
  }

  @override
  Future<void> nextTxtPage() async {
    if (usesVerticalScroll) {
      await scrollTxtByViewport(1);
      return;
    }
    final safePageCount = txtPages.isEmpty ? 1 : txtPages.length;
    if (page >= safePageCount - 1) {
      await goToChapter(chapterIndex + 1);
      return;
    }
    onTxtPageChanged(readerNavigationToken, page + 1);
  }

  @override
  Future<void> previousTxtPage() async {
    if (usesVerticalScroll) {
      await scrollTxtByViewport(-1);
      return;
    }
    if (page <= 0) {
      await goToChapter(chapterIndex - 1, atEnd: true);
      return;
    }
    onTxtPageChanged(readerNavigationToken, page - 1);
  }

  @override
  Future<void> scrollTxtByViewport(int direction) async {
    if (!txtScrollController.hasClients) {
      await goToChapter(
        direction > 0 ? chapterIndex + 1 : chapterIndex - 1,
        atEnd: direction < 0,
      );
      return;
    }
    final position = txtScrollController.position;
    final delta = position.viewportDimension * .88 * direction;
    final target = position.pixels + delta;
    if (direction > 0 && target >= position.maxScrollExtent - 2) {
      await goToChapter(chapterIndex + 1);
      return;
    }
    if (direction < 0 && target <= 2) {
      await goToChapter(chapterIndex - 1, atEnd: true);
      return;
    }
    await txtScrollController.animateTo(
      target.clamp(position.minScrollExtent, position.maxScrollExtent),
      duration: const Duration(milliseconds: 220),
      curve: Curves.easeOutCubic,
    );
  }
}
