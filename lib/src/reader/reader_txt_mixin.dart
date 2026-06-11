import 'dart:async';
import 'dart:io';
import 'dart:math' as math;
import 'dart:typed_data' as typed_data show ByteData;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart' show FontLoader;
import 'package:html/parser.dart' as html_parser;

import '../models.dart';
import '../typography.dart';
import 'reader_enums.dart';
import 'reader_epub_fallback.dart';
import 'reader_menu.dart';
import 'reader_settings.dart';
import 'reader_state_fields.dart';
import 'reader_txt_view.dart';

mixin ReaderTxtMixin<T extends ReaderScreenWidget> on ReaderStateFields<T> {
  @override
  Future<void> loadAndPaginateTxt(
    TxtLayoutMetrics metrics,
    String signature,
    int token,
    int chapterIndex,
  ) async {
    if (!isCurrentReaderToken(token) ||
        chapterIndex != this.chapterIndex ||
        !usesFlutterTxt ||
        book.chapters.isEmpty) {
      readerLog(
        'skip pagination token=$token current=$readerNavigationToken chapter=$chapterIndex currentChapter=${this.chapterIndex} usesFlutter=$usesFlutterTxt chapters=${book.chapters.length}',
      );
      return;
    }
    if (mounted && !isLoading) {
      setState(() {
        isLoading = true;
        loadError = null;
      });
    }
    try {
      readerLog('start pagination chapter=$chapterIndex');
      final fontFamily = await registerReaderFont(style.fontPath);
      final chapter = currentChapter;
      final exists = await File(chapter.filePath).exists();
      readerLog('chapter file exists=$exists path=${chapter.filePath}');
      final anchor = book.format == BookFormat.epub
          ? (pendingAnchor ?? chapter.anchor)
          : null;
      final endAnchor = book.format == BookFormat.epub
          ? chapterEndAnchor(chapterIndex)
          : null;
      final document = book.format == BookFormat.epub
          ? await readFlutterEpubDocument(
              chapter,
              anchor: anchor,
              endAnchor: endAnchor,
            )
          : await readFlutterTxtDocument(chapter);
      final scrollMode = style.readingFlow == ReadingFlowMode.scroll;
      final pages = scrollMode
          ? const <FlutterTxtPage>[]
          : paginateFlutterTxt(
              document: document,
              metrics: metrics,
              readingStyle: style,
              fontFamily: fontFamily,
            );
      final scrollBlocks = scrollMode
          ? buildFlutterScrollBlocks(
              document: document,
              metrics: metrics,
              readingStyle: style,
            )
          : const <FlutterTxtBlock>[];
      if (!mounted ||
          !isCurrentReaderToken(token) ||
          chapterIndex != this.chapterIndex ||
          signature != txtRequestedSignature) {
        readerLog('drop stale pagination result mounted=$mounted');
        return;
      }
      readerLog(
        'pagination ok mode=${scrollMode ? 'scroll' : 'paged'} pages=${pages.length} blocks=${document.blocks.length} title=${document.title}',
      );
      final exactPage = pendingExactPage;
      final exactPageCount = pendingExactPageCount;
      final restoredProgress =
          pendingPageProgress ??
          (pageCount <= 1 ? 0.0 : page / (pageCount - 1));
      pendingPageProgress = null;
      pendingExactPage = null;
      pendingExactPageCount = null;
      final safePageCount = pages.isEmpty ? 1 : pages.length;
      final targetPage = exactPage != null && exactPageCount == safePageCount
          ? exactPage.clamp(0, safePageCount - 1)
          : (restoredProgress * (safePageCount - 1)).round().clamp(
              0,
              safePageCount - 1,
            );
      setState(() {
        flutterReaderFontFamily = fontFamily;
        txtPages = scrollMode
            ? const []
            : pages.isEmpty
            ? [FlutterTxtPage.empty()]
            : pages;
        txtScrollBlocks = scrollMode ? scrollBlocks : const [];
        txtScrollInitialProgress = scrollMode
            ? restoredProgress.clamp(0.0, 1.0).toDouble()
            : 0;
        txtPaginationSignature = signature;
        txtRequestedSignature = null;
        pendingAnchor = null;
        pendingWebJumpToEnd = false;
        page = scrollMode ? 0 : targetPage;
        pageCount = scrollMode ? 1 : safePageCount;
        isLoading = false;
        loadError = null;
      });
      flushPendingProgressSeek();
      if (!scrollMode) {
        unawaited(
          appState.updateBookProgress(
            book: book,
            chapterIndex: chapterIndex,
            page: targetPage,
            pageCount: safePageCount,
          ),
        );
      }
    } on EpubWebViewFallbackException {
      if (!mounted || signature != txtRequestedSignature) {
        return;
      }
      readerLog(
        'fallback to webview chapter=$chapterIndex file=${currentChapter.filePath}',
      );
      epubWebViewFallbackChapters.add(currentChapter.filePath);
      setState(() {
        txtRequestedSignature = null;
        txtPaginationSignature = null;
        txtPages = const [];
        isLoading = true;
        loadError = null;
      });
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted || usesFlutterTxt) {
          return;
        }
        unawaited(loadCurrentWebViewChapter());
      });
    } catch (error) {
      if (!mounted || signature != txtRequestedSignature) {
        return;
      }
      readerLog('pagination error $error');
      setState(() {
        txtRequestedSignature = null;
        txtPaginationSignature = signature;
        isLoading = false;
        loadError = error.toString();
        overlay = ReaderOverlay.chrome;
      });
      flushPendingProgressSeek();
    }
  }

  @override
  Future<FlutterTxtDocument> readFlutterTxtDocument(
    ReaderChapter chapter,
  ) async {
    final raw = await File(chapter.filePath).readAsString();
    final document = html_parser.parse(raw);
    final title =
        document
            .querySelector('h1')
            ?.text
            .trim()
            .replaceAll(RegExp(r'\s+'), ' ') ??
        chapter.title;
    var paragraphs = document
        .querySelectorAll('p')
        .map((node) => node.text.trim().replaceAll(RegExp(r'\s+'), ' '))
        .where((text) => text.isNotEmpty)
        .toList();
    if (paragraphs.isEmpty) {
      paragraphs =
          document.body?.text
              .split(RegExp(r'\n+'))
              .map((line) => line.trim())
              .where((line) => line.isNotEmpty && line != title)
              .toList() ??
          const [];
    }
    return FlutterTxtDocument(
      title: title,
      blocks: [
        for (final paragraph in paragraphs)
          FlutterDocumentBlock.paragraph(paragraph),
      ],
    );
  }

  @override
  int preferScrollBreak(
    List<String> chars,
    int start,
    int target,
    int hardLimit,
  ) {
    final maxEnd = math.min(start + hardLimit, chars.length);
    final preferredEnd = math.min(start + target, maxEnd);
    final minEnd = math.min(
      math.max(start + (target * .55).round(), start + 1),
      maxEnd,
    );
    final sentencePattern = RegExp(
      r'[\n\u3002\uff01\uff1f!\?\u300d\u300f\uff09)]',
    );
    for (var i = preferredEnd; i >= minEnd; i--) {
      if (sentencePattern.hasMatch(chars[i - 1])) {
        return i - start;
      }
    }
    final softPattern = RegExp(r'[\s\uff0c\u3001,.\uff1b;]');
    for (var i = preferredEnd; i >= minEnd; i--) {
      if (softPattern.hasMatch(chars[i - 1])) {
        return i - start;
      }
    }
    return preferredEnd - start;
  }

  @override
  int fitTxtFragmentCount({
    required List<String> chars,
    required int start,
    required String prefix,
    required double availableHeight,
    required double width,
    required TextStyle style,
  }) {
    var low = 1;
    var high = chars.length - start;
    var best = 0;
    while (low <= high) {
      final mid = (low + high) >> 1;
      final text = prefix + chars.sublist(start, start + mid).join();
      final painter = TextPainter(
        text: TextSpan(text: text, style: style),
        textDirection: TextDirection.ltr,
        textScaler: TextScaler.noScaling,
      )..layout(maxWidth: width);
      if (painter.height <= availableHeight) {
        best = mid;
        low = mid + 1;
      } else {
        high = mid - 1;
      }
    }
    return best;
  }

  @override
  int preferTxtBreak(List<String> chars, int start, int count) {
    if (count <= 8) {
      return count;
    }
    final end = start + count;
    final searchStart = (end - 36).clamp(start + 1, end - 1);
    for (var i = end - 1; i >= searchStart; i--) {
      if (RegExp(
        r'[\s\uff0c\u3002\uff01\uff1f\u3001\uff1b,.!?:;\u300d\u300f\uff09)]',
      ).hasMatch(chars[i])) {
        return i - start + 1;
      }
    }
    return count;
  }

  @override
  void onTxtPageChanged(int token, int page) {
    if (isLoading || !isCurrentReaderToken(token)) {
      return;
    }
    final safePageCount = txtPages.isEmpty ? 1 : txtPages.length;
    final safePage = page.clamp(0, safePageCount - 1);
    if (this.page != safePage || pageCount != safePageCount) {
      setState(() {
        this.page = safePage;
        pageCount = safePageCount;
      });
    }
    unawaited(
      appState.updateBookProgress(
        book: book,
        chapterIndex: chapterIndex,
        page: safePage,
        pageCount: safePageCount,
      ),
    );
  }

  @override
  void onTxtScrollProgressChanged(int token, int page, int pageCount) {
    if (isLoading || !isCurrentReaderToken(token)) {
      return;
    }
    final safePageCount = math.max(1, pageCount);
    final safePage = page.clamp(0, safePageCount - 1);
    if (this.page == safePage && this.pageCount == safePageCount) {
      return;
    }
    if (this.page != safePage || this.pageCount != safePageCount) {
      setState(() {
        this.page = safePage;
        this.pageCount = safePageCount;
      });
    }
    unawaited(
      appState.updateBookProgress(
        book: book,
        chapterIndex: chapterIndex,
        page: safePage,
        pageCount: safePageCount,
      ),
    );
  }

  @override
  double effectiveReaderFontSize(ReadingStyle style) {
    return (style.fontSize * 1.12).clamp(16.0, 38.0).toDouble();
  }

  @override
  void requestTxtPagination(TxtLayoutMetrics metrics) {
    txtLayoutMetrics = metrics;
    if (!usesFlutterTxt) {
      return;
    }
    final signature = buildTxtPaginationSignature(metrics);
    if (signature == txtPaginationSignature ||
        signature == txtRequestedSignature) {
      return;
    }
    txtRequestedSignature = signature;
    readerLog(
      'request pagination chapter=$chapterIndex format=${book.format.name} file=${currentChapter.filePath} size=${metrics.contentWidth.toStringAsFixed(1)}x${metrics.contentHeight.toStringAsFixed(1)}',
    );
    final token = readerNavigationToken;
    final chapterIndexParam = chapterIndex;
    txtPaginationTimer?.cancel();
    txtPaginationTimer = Timer(const Duration(milliseconds: 20), () {
      unawaited(
        loadAndPaginateTxt(metrics, signature, token, chapterIndexParam),
      );
    });
  }

  @override
  String buildTxtPaginationSignature(TxtLayoutMetrics metrics) {
    final chapter = currentChapter;
    final anchor = book.format == BookFormat.epub
        ? (pendingAnchor ?? chapter.anchor ?? '')
        : '';
    final endAnchor = book.format == BookFormat.epub
        ? (chapterEndAnchor(chapterIndex) ?? '')
        : '';
    return [
      book.format.name,
      chapterIndex.toString(),
      chapter.filePath,
      anchor,
      endAnchor,
      metrics.contentWidth.toStringAsFixed(1),
      metrics.contentHeight.toStringAsFixed(1),
      metrics.pageOuterInset.toStringAsFixed(1),
      style.fontSize.toStringAsFixed(2),
      style.lineHeight.toStringAsFixed(3),
      style.paragraphSpacing.toStringAsFixed(2),
      style.letterSpacing.toStringAsFixed(2),
      style.pageMargin.toStringAsFixed(1),
      style.verticalMargin.toStringAsFixed(1),
      style.readingFlow.name,
      style.firstLineIndent ? 'indent' : 'plain',
      style.fontPath ?? '',
    ].join('|');
  }

  @override
  bool hasTxtLayoutSize(BoxConstraints constraints) {
    if (!constraints.hasBoundedWidth || !constraints.hasBoundedHeight) {
      return false;
    }
    return constraints.maxWidth > 1 && constraints.maxHeight > 1;
  }

  @override
  TxtLayoutMetrics resolveTxtLayoutMetrics({
    required BoxConstraints constraints,
    required EdgeInsets systemPadding,
  }) {
    final viewportWidth = constraints.maxWidth;
    final viewportHeight = constraints.maxHeight;
    final maxHorizontalMargin = ((viewportWidth - 260) / 2)
        .clamp(0.0, 52.0)
        .toDouble();
    final maxVerticalMargin = ((viewportHeight - 420) / 2)
        .clamp(4.0, 96.0)
        .toDouble();
    final horizontalMargin = style.pageMargin
        .clamp(0.0, maxHorizontalMargin)
        .toDouble();
    final verticalMargin = style.verticalMargin
        .clamp(4.0, maxVerticalMargin)
        .toDouble();
    final pageOuterInset = horizontalMargin <= 0.5
        ? 0.0
        : (horizontalMargin * .45).clamp(0.0, 26.0).toDouble();
    final effectiveFontSize = effectiveReaderFontSize(style);
    final lineHeightPx = (effectiveFontSize * style.lineHeight).clamp(
      1.0,
      double.infinity,
    );
    final left = (horizontalMargin + systemPadding.left - pageOuterInset)
        .clamp(0.0, double.infinity)
        .toDouble();
    final right = (horizontalMargin + systemPadding.right - pageOuterInset)
        .clamp(0.0, double.infinity)
        .toDouble();
    final top = verticalMargin + systemPadding.top;
    final bottom = verticalMargin + systemPadding.bottom;
    final availableWidth = viewportWidth - pageOuterInset * 2 - left - right;
    final width = availableWidth.clamp(120.0, viewportWidth).toDouble();
    final availableHeight = viewportHeight - top - bottom;
    final rawHeight = availableHeight.clamp(160.0, double.infinity).toDouble();
    final height = ((rawHeight / lineHeightPx).floor() * lineHeightPx).clamp(
      lineHeightPx,
      rawHeight,
    );
    return TxtLayoutMetrics(
      padding: EdgeInsets.fromLTRB(left, top, right, bottom),
      pageOuterInset: pageOuterInset,
      contentWidth: width,
      contentHeight: height,
    );
  }

  @override
  List<FlutterTxtPage> paginateFlutterTxt({
    required FlutterTxtDocument document,
    required TxtLayoutMetrics metrics,
    required ReadingStyle readingStyle,
    required String? fontFamily,
  }) {
    final effectiveFontSize = effectiveReaderFontSize(readingStyle);
    final paragraphStyle = TextStyle(
      fontFamily: fontFamily,
      fontSize: effectiveFontSize,
      height: readingStyle.lineHeight,
      letterSpacing: readingStyle.letterSpacing,
      color: readerPalette.text,
      fontWeight: AppTextWeight.regular,
    );
    final titleStyle = TextStyle(
      fontFamily: fontFamily,
      fontSize: effectiveFontSize * 1.45,
      height: 1.35,
      letterSpacing: readingStyle.letterSpacing,
      color: readerPalette.text,
      fontWeight: AppTextWeight.semibold,
    );
    final linkStyle = TextStyle(
      fontFamily: fontFamily,
      fontSize: effectiveFontSize,
      height: readingStyle.lineHeight,
      letterSpacing: readingStyle.letterSpacing,
      color: appPalette.primarySoft,
      fontWeight: AppTextWeight.medium,
      decoration: TextDecoration.underline,
      decorationColor: appPalette.primarySoft,
    );
    final pages = <FlutterTxtPage>[];
    var blocks = <FlutterTxtBlock>[];
    var usedHeight = 0.0;
    final pageHeight = metrics.contentHeight;
    final pageWidth = metrics.contentWidth;
    final paragraphSpacing = readingStyle.paragraphSpacing;
    final minLineHeight = effectiveFontSize * readingStyle.lineHeight;

    double measure(String text, TextStyle textStyle) {
      final painter = TextPainter(
        text: TextSpan(text: text, style: textStyle),
        textDirection: TextDirection.ltr,
        textScaler: TextScaler.noScaling,
      )..layout(maxWidth: pageWidth);
      return painter.height;
    }

    void finishPage() {
      if (blocks.isEmpty) {
        return;
      }
      pages.add(FlutterTxtPage(blocks: blocks));
      blocks = <FlutterTxtBlock>[];
      usedHeight = 0;
    }

    void ensureSpace(double neededHeight) {
      if (blocks.isNotEmpty && usedHeight + neededHeight > pageHeight) {
        finishPage();
      }
    }

    void addBlock(FlutterTxtBlock block, double textHeight) {
      blocks.add(block);
      usedHeight += textHeight + block.bottomSpacing;
    }

    void addImageBlock(String source) {
      final maxImageHeight = pageHeight;
      final minImageHeight = minLineHeight * 5 > maxImageHeight
          ? maxImageHeight
          : minLineHeight * 5;
      final imageHeight = (pageWidth * 1.38)
          .clamp(minImageHeight, maxImageHeight)
          .toDouble();
      ensureSpace(imageHeight);
      final spacing = usedHeight + imageHeight + paragraphSpacing <= pageHeight
          ? paragraphSpacing
          : 0.0;
      addBlock(
        FlutterTxtBlock(
          text: '',
          kind: FlutterTxtBlockKind.image,
          bottomSpacing: spacing,
          imageSource: source,
          imageHeight: imageHeight,
        ),
        imageHeight,
      );
      if (imageHeight >= pageHeight - minLineHeight) {
        finishPage();
      }
    }

    final normalizedTitle = document.title.trim();
    if (normalizedTitle.isNotEmpty) {
      final titleHeight = measure(normalizedTitle, titleStyle);
      final titleSpacing = paragraphSpacing * 1.25;
      addBlock(
        FlutterTxtBlock(
          text: normalizedTitle,
          kind: FlutterTxtBlockKind.title,
          bottomSpacing: titleSpacing,
        ),
        titleHeight,
      );
    }

    for (final sourceBlock in document.blocks) {
      if (sourceBlock.kind == FlutterDocumentBlockKind.image) {
        final imageSource = sourceBlock.imageSource;
        if (imageSource != null && imageSource.isNotEmpty) {
          addImageBlock(imageSource);
        }
        continue;
      }
      final isLink = sourceBlock.kind == FlutterDocumentBlockKind.link;
      final textStyle = isLink ? linkStyle : paragraphStyle;
      final blockKind = isLink
          ? FlutterTxtBlockKind.link
          : FlutterTxtBlockKind.paragraph;
      final paragraph = sourceBlock.text;
      final chars = paragraph.runes.map(String.fromCharCode).toList();
      if (chars.isEmpty) {
        continue;
      }
      var offset = 0;
      var firstFragment = true;
      while (offset < chars.length) {
        ensureSpace(minLineHeight);
        final indentFirstLine =
            !isLink && firstFragment && readingStyle.firstLineIndent;
        final prefix = indentFirstLine ? '\u3000\u3000' : '';
        final available = (pageHeight - usedHeight).clamp(
          minLineHeight,
          pageHeight,
        );
        final remaining = chars.sublist(offset).join();
        final fullText = '$prefix$remaining';
        final fullHeight = measure(fullText, textStyle);
        final isLastParagraphPart = fullHeight <= available;
        if (isLastParagraphPart) {
          final spacing =
              usedHeight + fullHeight + paragraphSpacing <= pageHeight
              ? paragraphSpacing
              : 0.0;
          addBlock(
            FlutterTxtBlock(
              text: remaining,
              kind: blockKind,
              bottomSpacing: spacing,
              firstLineIndent: indentFirstLine,
              href: sourceBlock.href,
              segments: sliceSegmentsByTextRange(
                sourceBlock.segments,
                offset,
                chars.length,
              ),
            ),
            fullHeight,
          );
          offset = chars.length;
          continue;
        }

        var fitCount = fitTxtFragmentCount(
          chars: chars,
          start: offset,
          prefix: prefix,
          availableHeight: available,
          width: pageWidth,
          style: textStyle,
        );
        if (fitCount <= 0) {
          fitCount = 1;
        }
        if (offset + fitCount < chars.length) {
          fitCount = preferTxtBreak(chars, offset, fitCount);
        }
        final fragment = chars.sublist(offset, offset + fitCount).join();
        final measuredFragment = '$prefix$fragment';
        addBlock(
          FlutterTxtBlock(
            text: fragment,
            kind: blockKind,
            bottomSpacing: 0,
            firstLineIndent: indentFirstLine,
            href: sourceBlock.href,
            segments: sliceSegmentsByTextRange(
              sourceBlock.segments,
              offset,
              offset + fitCount,
            ),
          ),
          measure(measuredFragment, textStyle),
        );
        offset += fitCount;
        firstFragment = false;
        finishPage();
      }
    }
    finishPage();
    return pages;
  }

  @override
  List<FlutterTxtBlock> buildFlutterScrollBlocks({
    required FlutterTxtDocument document,
    required TxtLayoutMetrics metrics,
    required ReadingStyle readingStyle,
  }) {
    final blocks = <FlutterTxtBlock>[];
    final paragraphSpacing = readingStyle.paragraphSpacing;
    final effectiveFontSize = effectiveReaderFontSize(readingStyle);
    final minLineHeight = effectiveFontSize * readingStyle.lineHeight;

    final normalizedTitle = document.title.trim();
    if (normalizedTitle.isNotEmpty) {
      blocks.add(
        FlutterTxtBlock(
          text: normalizedTitle,
          kind: FlutterTxtBlockKind.title,
          bottomSpacing: paragraphSpacing * 1.25,
        ),
      );
    }

    for (final sourceBlock in document.blocks) {
      if (sourceBlock.kind == FlutterDocumentBlockKind.image) {
        final imageSource = sourceBlock.imageSource;
        if (imageSource == null || imageSource.isEmpty) {
          continue;
        }
        final maxImageHeight = metrics.contentHeight * .92;
        final minImageHeight = math.min(minLineHeight * 5, maxImageHeight);
        final imageHeight = (metrics.contentWidth * 1.38)
            .clamp(minImageHeight, maxImageHeight)
            .toDouble();
        blocks.add(
          FlutterTxtBlock(
            text: '',
            kind: FlutterTxtBlockKind.image,
            bottomSpacing: paragraphSpacing,
            imageSource: imageSource,
            imageHeight: imageHeight,
          ),
        );
        continue;
      }
      final text = sourceBlock.text.trim();
      if (text.isEmpty) {
        continue;
      }
      final isLink = sourceBlock.kind == FlutterDocumentBlockKind.link;
      final blockKind = isLink
          ? FlutterTxtBlockKind.link
          : FlutterTxtBlockKind.paragraph;
      final shouldSplit = sourceBlock.segments == null && text.length > 1200;
      if (!shouldSplit) {
        blocks.add(
          FlutterTxtBlock(
            text: text,
            kind: blockKind,
            bottomSpacing: paragraphSpacing,
            firstLineIndent: !isLink && readingStyle.firstLineIndent,
            href: sourceBlock.href,
            segments: sourceBlock.segments,
          ),
        );
        continue;
      }
      final fragments = splitScrollText(text);
      for (var i = 0; i < fragments.length; i++) {
        blocks.add(
          FlutterTxtBlock(
            text: fragments[i],
            kind: blockKind,
            bottomSpacing: i == fragments.length - 1 ? paragraphSpacing : 0,
            firstLineIndent: i == 0 && !isLink && readingStyle.firstLineIndent,
            href: sourceBlock.href,
          ),
        );
      }
    }
    return blocks.isEmpty
        ? [
            FlutterTxtBlock(
              text: '',
              kind: FlutterTxtBlockKind.paragraph,
              bottomSpacing: 0,
            ),
          ]
        : blocks;
  }

  @override
  List<String> splitScrollText(String text) {
    final chars = text.runes.map(String.fromCharCode).toList();
    if (chars.length <= 1200) {
      return [text];
    }
    const target = 900;
    const hardLimit = 1400;
    final fragments = <String>[];
    var offset = 0;
    while (offset < chars.length) {
      final remaining = chars.length - offset;
      if (remaining <= hardLimit) {
        fragments.add(chars.sublist(offset).join().trim());
        break;
      }
      final count = preferScrollBreak(chars, offset, target, hardLimit);
      final end = (offset + count).clamp(offset + 1, chars.length);
      fragments.add(chars.sublist(offset, end).join().trim());
      offset = end;
    }
    return fragments.where((fragment) => fragment.isNotEmpty).toList();
  }

  @override
  List<InlineTextSegment>? sliceSegmentsByTextRange(
    List<InlineTextSegment>? segments,
    int start,
    int end,
  ) {
    if (segments == null || segments.isEmpty || start >= end) {
      return null;
    }
    final result = <InlineTextSegment>[];
    var cursor = 0;
    for (final segment in segments) {
      final chars = segment.text.runes.map(String.fromCharCode).toList();
      final segmentStart = cursor;
      final segmentEnd = cursor + chars.length;
      cursor = segmentEnd;
      if (segmentEnd <= start || segmentStart >= end) {
        continue;
      }
      final localStart = (start - segmentStart).clamp(0, chars.length);
      final localEnd = (end - segmentStart).clamp(0, chars.length);
      if (localStart >= localEnd) {
        continue;
      }
      result.add(
        segment.copyWith(text: chars.sublist(localStart, localEnd).join()),
      );
    }
    return result.isEmpty ? null : result;
  }

  @override
  Future<String?> registerReaderFont(String? fontPath) async {
    if (fontPath == null || fontPath.isEmpty) {
      return null;
    }
    try {
      final file = File(fontPath);
      if (!await file.exists()) {
        return null;
      }
      final family = readerFontFamilyForPath(fontPath);
      if (registeredReaderFontFamilies.add(family)) {
        final loader = FontLoader(family);
        loader.addFont(
          file.readAsBytes().then(
            (bytes) => typed_data.ByteData.view(
              bytes.buffer,
              bytes.offsetInBytes,
              bytes.lengthInBytes,
            ),
          ),
        );
        await loader.load();
      }
      return family;
    } catch (error) {
      debugPrint('SQuartor reader font load failed: $error');
      return null;
    }
  }

  @override
  String readerFontFamilyForPath(String fontPath) {
    var hash = 0x811C9DC5;
    for (final codeUnit in fontPath.codeUnits) {
      hash ^= codeUnit;
      hash = (hash * 0x01000193) & 0xFFFFFFFF;
    }
    return 'SQuartorReaderFont_${hash.toRadixString(16)}';
  }

  @override
  Widget buildFlutterTxtContent(
    ReaderChapter chapter,
    EdgeInsets systemPadding,
  ) {
    if (chapter.filePath.isEmpty) {
      return MissingChapter(readerPalette: readerPalette);
    }
    return LayoutBuilder(
      builder: (context, constraints) {
        if (!hasTxtLayoutSize(constraints)) {
          if (!txtWaitingForLayout) {
            txtWaitingForLayout = true;
            readerLog(
              'wait layout size=${constraints.maxWidth.toStringAsFixed(1)}x${constraints.maxHeight.toStringAsFixed(1)}',
            );
          }
          if (!txtLayoutRetryScheduled) {
            txtLayoutRetryScheduled = true;
            WidgetsBinding.instance.addPostFrameCallback((_) {
              txtLayoutRetryScheduled = false;
              if (mounted && txtWaitingForLayout) {
                setState(() {});
              }
            });
          }
          return ReaderStatusOverlay(
            readerPalette: readerPalette,
            message: '\u6b63\u5728\u52a0\u8f7d...',
          );
        }
        txtWaitingForLayout = false;
        txtLayoutRetryScheduled = false;
        final metrics = resolveTxtLayoutMetrics(
          constraints: constraints,
          systemPadding: systemPadding,
        );
        requestTxtPagination(metrics);
        if (usesVerticalScroll && txtPaginationSignature != null) {
          return FlutterTxtScrollReaderView(
            key: ValueKey('scroll-$txtPaginationSignature'),
            navigationToken: readerNavigationToken,
            blocks: txtScrollBlocks,
            controller: txtScrollController,
            initialProgress: txtScrollInitialProgress,
            metrics: metrics,
            readerPalette: readerPalette,
            appPalette: appPalette,
            style: style,
            fontFamily: flutterReaderFontFamily,
            linkColor: appPalette.primarySoft,
            onTapUp: onReaderTap,
            onProgressChanged: onTxtScrollProgressChanged,
            onEdgePrevious: previousTxtPage,
            onEdgeNext: nextTxtPage,
            onLinkTap: openInternalLink,
            onFootnoteTap: showFootnote,
          );
        }
        if (txtPages.isEmpty) {
          return ReaderStatusOverlay(
            readerPalette: readerPalette,
            message: '\u6b63\u5728\u52a0\u8f7d...',
          );
        }
        return FlutterTxtReaderView(
          key: ValueKey('paged-$txtPaginationSignature'),
          navigationToken: readerNavigationToken,
          pages: txtPages,
          currentPage: page,
          metrics: metrics,
          readerPalette: readerPalette,
          style: style,
          fontFamily: flutterReaderFontFamily,
          linkColor: appPalette.primarySoft,
          onTapUp: onReaderTap,
          onPageChanged: onTxtPageChanged,
          onEdgePrevious: previousTxtPage,
          onEdgeNext: nextTxtPage,
          onLinkTap: openInternalLink,
          onFootnoteTap: showFootnote,
        );
      },
    );
  }
}
