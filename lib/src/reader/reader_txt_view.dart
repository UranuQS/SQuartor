import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_page_curl/flutter_page_curl.dart';

import '../models.dart';
import '../typography.dart';
import 'custom_page_curl_view.dart';
import 'reader_enums.dart';
import 'reader_epub_fallback.dart';

// ---------------------------------------------------------------------------
// FlutterTxtReaderView (paged mode)
// ---------------------------------------------------------------------------

class FlutterTxtReaderView extends StatefulWidget {
  const FlutterTxtReaderView({
    super.key,
    required this.navigationToken,
    required this.pages,
    required this.currentPage,
    required this.metrics,
    required this.readerPalette,
    required this.style,
    required this.fontFamily,
    required this.linkColor,
    required this.onTapUp,
    required this.onPageChanged,
    required this.onEdgePrevious,
    required this.onEdgeNext,
    required this.onLinkTap,
    required this.onFootnoteTap,
    this.onVerticalDragStart,
    this.onVerticalDragUpdate,
    this.onVerticalDragEnd,
    this.onVerticalDragCancel,
  });

  final int navigationToken;
  final List<FlutterTxtPage> pages;
  final int currentPage;
  final TxtLayoutMetrics metrics;
  final ReaderPalette readerPalette;
  final ReadingStyle style;
  final String? fontFamily;
  final Color linkColor;
  final GestureTapUpCallback onTapUp;
  final void Function(int token, int page) onPageChanged;
  final Future<void> Function() onEdgePrevious;
  final Future<void> Function() onEdgeNext;
  final Future<void> Function(String href) onLinkTap;
  final void Function(String text, Offset? globalPosition) onFootnoteTap;
  final GestureDragStartCallback? onVerticalDragStart;
  final GestureDragUpdateCallback? onVerticalDragUpdate;
  final GestureDragEndCallback? onVerticalDragEnd;
  final GestureDragCancelCallback? onVerticalDragCancel;

  @override
  State<FlutterTxtReaderView> createState() => _FlutterTxtReaderViewState();
}

class _FlutterTxtReaderViewState extends State<FlutterTxtReaderView> {
  static const double _edgeTurnThreshold = 56;

  late final PageController _pageController;
  late final PageCurlController _turnPageController;
  double _edgeOverscroll = 0;
  var _edgeTurnInFlight = false;

  @override
  void initState() {
    super.initState();
    _pageController = PageController(initialPage: _clampedCurrentPage);
    _turnPageController = PageCurlController(initialPage: _clampedCurrentPage);
    _turnPageController.addListener(() {
      widget.onPageChanged(
        widget.navigationToken,
        _turnPageController.currentPage,
      );
    });
  }

  @override
  void didUpdateWidget(covariant FlutterTxtReaderView oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.currentPage != widget.currentPage) {
      _syncControllerToCurrentPage(animated: true);
    } else if (oldWidget.pages != widget.pages) {
      _syncControllerToCurrentPage(animated: false);
    }
  }

  @override
  void dispose() {
    _pageController.dispose();
    _turnPageController.dispose();
    super.dispose();
  }

  int get _pageCount => widget.pages.isEmpty ? 1 : widget.pages.length;

  int get _clampedCurrentPage => widget.currentPage.clamp(0, _pageCount - 1);

  @override
  Widget build(BuildContext context) {
    final safePages = widget.pages.isEmpty
        ? [FlutterTxtPage.empty()]
        : widget.pages;
    final effectiveFontSize = (widget.style.fontSize * 1.12)
        .clamp(16.0, 38.0)
        .toDouble();
    final paragraphStyle = TextStyle(
      fontFamily: widget.fontFamily,
      color: widget.readerPalette.text,
      fontSize: effectiveFontSize,
      height: widget.style.lineHeight,
      letterSpacing: widget.style.letterSpacing,
      leadingDistribution: TextLeadingDistribution.even,
      fontWeight: widget.style.fontWeight,
    );
    final titleStyle = TextStyle(
      fontFamily: widget.fontFamily,
      color: widget.readerPalette.text,
      fontSize: effectiveFontSize * 1.45,
      height: 1.35,
      letterSpacing: widget.style.letterSpacing,
      leadingDistribution: TextLeadingDistribution.even,
      fontWeight: AppTextWeight.semibold,
    );
    final linkStyle = TextStyle(
      fontFamily: widget.fontFamily,
      color: widget.linkColor,
      fontSize: effectiveFontSize,
      height: widget.style.lineHeight,
      letterSpacing: widget.style.letterSpacing,
      leadingDistribution: TextLeadingDistribution.even,
      fontWeight: AppTextWeight.medium,
      decoration: TextDecoration.underline,
      decorationColor: widget.linkColor,
    );
    return GestureDetector(
      behavior: HitTestBehavior.translucent,
      onTapUp: widget.onTapUp,
      onVerticalDragStart: widget.onVerticalDragStart,
      onVerticalDragUpdate: widget.onVerticalDragUpdate,
      onVerticalDragEnd: widget.onVerticalDragEnd,
      onVerticalDragCancel: widget.onVerticalDragCancel,
      child: ColoredBox(
        color: widget.readerPalette.background,
        child: NotificationListener<ScrollNotification>(
          onNotification: _handlePageScrollNotification,
          child: Stack(
            children: [
              Positioned.fill(
                child: widget.style.pageTurnAnimation
                    ? CustomPageCurlView(
                        controller: _turnPageController,
                        itemCount: safePages.length,
                        backOpacity: 0.1, // Subtle darkening for mirrored text
                        onCenterTap: widget.onTapUp,
                        itemBuilder: (context, index) {
                          return ColoredBox(
                            color: widget.readerPalette.background,
                            child: _txtPageView(
                              blocks: safePages[index].blocks,
                              paragraphStyle: paragraphStyle,
                              titleStyle: titleStyle,
                              linkStyle: linkStyle,
                            ),
                          );
                        },
                      )
                    : PageView.builder(
                        controller: _pageController,
                        physics: const PageScrollPhysics(
                          parent: AlwaysScrollableScrollPhysics(),
                        ),
                        itemCount: safePages.length,
                        onPageChanged: (page) =>
                            widget.onPageChanged(widget.navigationToken, page),
                        itemBuilder: (context, index) {
                          return RepaintBoundary(
                            child: ColoredBox(
                              color: widget.readerPalette.background,
                              child: _txtPageView(
                                blocks: safePages[index].blocks,
                                paragraphStyle: paragraphStyle,
                                titleStyle: titleStyle,
                                linkStyle: linkStyle,
                              ),
                            ),
                          );
                        },
                      ),
              ),
              if (widget.style.pageTurnAnimation)
                Positioned.fill(
                  child: GestureDetector(
                    behavior: HitTestBehavior.translucent,
                    onTapUp: widget.onTapUp,
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  bool _handlePageScrollNotification(ScrollNotification notification) {
    if (notification.metrics.axis != Axis.horizontal) {
      return false;
    }
    if (notification is ScrollStartNotification) {
      _edgeOverscroll = 0;
    } else if (notification is OverscrollNotification) {
      _edgeOverscroll += notification.overscroll;
    } else if (notification is ScrollEndNotification) {
      final overscroll = _edgeOverscroll;
      _edgeOverscroll = 0;
      if (_edgeTurnInFlight || overscroll.abs() < _edgeTurnThreshold) {
        return false;
      }
      _edgeTurnInFlight = true;
      final turn = overscroll > 0
          ? widget.onEdgeNext()
          : widget.onEdgePrevious();
      turn.whenComplete(() {
        if (mounted) {
          _edgeTurnInFlight = false;
        }
      });
    }
    return false;
  }

  void _syncControllerToCurrentPage({required bool animated}) {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) {
        return;
      }
      final target = _clampedCurrentPage;
      final current = widget.style.pageTurnAnimation
          ? _turnPageController.currentPage
          : (_pageController.page ?? _pageController.initialPage).round();
      if (current == target) return;
      if (widget.style.pageTurnAnimation) {
        if (animated && (target - current).abs() == 1) {
          if (current < target) {
            _turnPageController.nextPage();
          } else {
            _turnPageController.previousPage();
          }
        } else {
          _turnPageController.jumpToPage(target);
        }
      } else {
        if (animated && (current - target).abs() == 1) {
          _pageController.animateToPage(
            target,
            duration: const Duration(milliseconds: 180),
            curve: Curves.easeOutCubic,
          );
        } else {
          _pageController.jumpToPage(target);
        }
      }
    });
  }

  Widget _txtPageView({
    required List<FlutterTxtBlock> blocks,
    required TextStyle paragraphStyle,
    required TextStyle titleStyle,
    required TextStyle linkStyle,
  }) {
    return FlutterTxtPageView(
      blocks: blocks,
      metrics: widget.metrics,
      height: widget.metrics.contentHeight,
      clipContent: true,
      paragraphStyle: paragraphStyle,
      titleStyle: titleStyle,
      linkStyle: linkStyle,
      justifyText: true,
      dimJapaneseText: true,
      onLinkTap: widget.onLinkTap,
      onFootnoteTap: widget.onFootnoteTap,
    );
  }
}

// ---------------------------------------------------------------------------
// FlutterTxtScrollReaderView (scroll mode)
// ---------------------------------------------------------------------------

class FlutterTxtScrollReaderView extends StatefulWidget {
  const FlutterTxtScrollReaderView({
    super.key,
    required this.navigationToken,
    required this.blocks,
    required this.controller,
    required this.initialProgress,
    required this.metrics,
    required this.readerPalette,
    required this.appPalette,
    required this.style,
    required this.fontFamily,
    required this.linkColor,
    required this.onTapUp,
    required this.onProgressChanged,
    required this.onEdgeTurnProgress,
    required this.onEdgePrevious,
    required this.onEdgeNext,
    required this.onLinkTap,
    required this.onFootnoteTap,
    this.onVerticalDragStart,
    this.onVerticalDragUpdate,
    this.onVerticalDragEnd,
    this.onVerticalDragCancel,
  });

  final int navigationToken;
  final List<FlutterTxtBlock> blocks;
  final ScrollController controller;
  final double initialProgress;
  final TxtLayoutMetrics metrics;
  final ReaderPalette readerPalette;
  final AppPalette appPalette;
  final ReadingStyle style;
  final String? fontFamily;
  final Color linkColor;
  final GestureTapUpCallback onTapUp;
  final void Function(int token, int page, int pageCount) onProgressChanged;
  final void Function(ScrollEdgeTurnDirection? direction, double progress)
  onEdgeTurnProgress;
  final Future<void> Function() onEdgePrevious;
  final Future<void> Function() onEdgeNext;
  final Future<void> Function(String href) onLinkTap;
  final void Function(String text, Offset? globalPosition) onFootnoteTap;
  final GestureDragStartCallback? onVerticalDragStart;
  final GestureDragUpdateCallback? onVerticalDragUpdate;
  final GestureDragEndCallback? onVerticalDragEnd;
  final GestureDragCancelCallback? onVerticalDragCancel;

  @override
  State<FlutterTxtScrollReaderView> createState() =>
      _FlutterTxtScrollReaderViewState();
}

class _FlutterTxtScrollReaderViewState
    extends State<FlutterTxtScrollReaderView> {
  static const double _edgeTurnThreshold = 176;
  static const double _edgeTurnProgressExponent = 1.35;

  double _edgeOverscroll = 0;
  ScrollEdgeTurnDirection? _edgeTurnDirection;
  double _edgeTurnProgress = 0;
  var _edgeTurnInFlight = false;
  var _initialScrollApplied = false;
  var _lastReportedPage = -1;
  var _lastReportedPageCount = -1;

  @override
  void initState() {
    super.initState();
    _scheduleInitialScroll();
  }

  @override
  void didUpdateWidget(covariant FlutterTxtScrollReaderView oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.key != widget.key) {
      _initialScrollApplied = false;
      _lastReportedPage = -1;
      _lastReportedPageCount = -1;
      _setEdgeTurnProgress(null, 0);
      _scheduleInitialScroll();
    }
  }

  @override
  Widget build(BuildContext context) {
    final effectiveFontSize = (widget.style.fontSize * 1.12)
        .clamp(16.0, 38.0)
        .toDouble();
    final paragraphStyle = TextStyle(
      fontFamily: widget.fontFamily,
      color: widget.readerPalette.text,
      fontSize: effectiveFontSize,
      height: widget.style.lineHeight,
      letterSpacing: widget.style.letterSpacing,
      leadingDistribution: TextLeadingDistribution.even,
      fontWeight: widget.style.fontWeight,
    );
    final titleStyle = TextStyle(
      fontFamily: widget.fontFamily,
      color: widget.readerPalette.text,
      fontSize: effectiveFontSize * 1.45,
      height: 1.35,
      letterSpacing: widget.style.letterSpacing,
      leadingDistribution: TextLeadingDistribution.even,
      fontWeight: AppTextWeight.semibold,
    );
    final linkStyle = TextStyle(
      fontFamily: widget.fontFamily,
      color: widget.linkColor,
      fontSize: effectiveFontSize,
      height: widget.style.lineHeight,
      letterSpacing: widget.style.letterSpacing,
      leadingDistribution: TextLeadingDistribution.even,
      fontWeight: AppTextWeight.medium,
      decoration: TextDecoration.underline,
      decorationColor: widget.linkColor,
    );
    return GestureDetector(
      behavior: HitTestBehavior.translucent,
      onTapUp: widget.onTapUp,
      onVerticalDragStart: widget.onVerticalDragStart,
      onVerticalDragUpdate: widget.onVerticalDragUpdate,
      onVerticalDragEnd: widget.onVerticalDragEnd,
      onVerticalDragCancel: widget.onVerticalDragCancel,
      child: ColoredBox(
        color: widget.readerPalette.background,
        child: NotificationListener<ScrollNotification>(
          onNotification: _handleScrollNotification,
          child: ListView.builder(
            controller: widget.controller,
            physics: const ClampingScrollPhysics(
              parent: AlwaysScrollableScrollPhysics(),
            ),
            padding: EdgeInsets.only(
              left: widget.metrics.pageOuterInset + widget.metrics.padding.left,
              right:
                  widget.metrics.pageOuterInset + widget.metrics.padding.right,
              top: widget.metrics.padding.top,
              bottom: widget.metrics.padding.bottom,
            ),
            itemCount: widget.blocks.length,
            itemBuilder: (context, index) {
              final block = widget.blocks[index];
              return RepaintBoundary(
                child: Center(
                  child: SizedBox(
                    width: widget.metrics.contentWidth,
                    child: FlutterTxtBlockView(
                      block: block,
                      paragraphStyle: paragraphStyle,
                      titleStyle: titleStyle,
                      linkStyle: linkStyle,
                      firstLineIndentWidth: effectiveFontSize * 2,
                      justifyText: false,
                      dimJapaneseText: true,
                      onLinkTap: widget.onLinkTap,
                      onFootnoteTap: widget.onFootnoteTap,
                    ),
                  ),
                ),
              );
            },
          ),
        ),
      ),
    );
  }

  bool _handleScrollNotification(ScrollNotification notification) {
    if (notification.metrics.axis != Axis.vertical) {
      return false;
    }
    if (notification is ScrollStartNotification) {
      _edgeOverscroll = 0;
      _setEdgeTurnProgress(null, 0);
    } else if (notification is OverscrollNotification) {
      _edgeOverscroll += notification.overscroll;
      _updateEdgeTurnHint(_edgeOverscroll);
    } else if (notification is ScrollUpdateNotification) {
      final delta = notification.scrollDelta ?? 0;
      final metrics = notification.metrics;
      final atBottom = metrics.pixels >= metrics.maxScrollExtent - 2;
      final atTop = metrics.pixels <= metrics.minScrollExtent + 2;
      if (atBottom && delta > 0) {
        _edgeOverscroll += delta;
      } else if (atTop && delta < 0) {
        _edgeOverscroll += delta;
      } else if (_edgeOverscroll != 0) {
        _edgeOverscroll = (_edgeOverscroll + delta)
            .clamp(-_edgeTurnThreshold, _edgeTurnThreshold)
            .toDouble();
        if (_edgeOverscroll.abs() < 2) {
          _edgeOverscroll = 0;
        }
      }
      _updateEdgeTurnHint(_edgeOverscroll);
    }
    if (notification is ScrollEndNotification) {
      _emitProgress(_estimateScrollPage(notification.metrics));
      final overscroll = _edgeOverscroll;
      _edgeOverscroll = 0;
      _setEdgeTurnProgress(null, 0);
      if (!_edgeTurnInFlight && overscroll.abs() >= _edgeTurnThreshold) {
        _edgeTurnInFlight = true;
        final turn = overscroll > 0
            ? widget.onEdgeNext()
            : widget.onEdgePrevious();
        turn.whenComplete(() {
          if (mounted) {
            _edgeTurnInFlight = false;
          }
        });
      }
    }
    return false;
  }

  void _updateEdgeTurnHint(double overscroll) {
    final magnitude = overscroll.abs();
    if (magnitude <= 2) {
      _setEdgeTurnProgress(null, 0);
      return;
    }
    final rawProgress = (magnitude / _edgeTurnThreshold)
        .clamp(0.0, 1.0)
        .toDouble();
    final dampedProgress = math
        .pow(rawProgress, _edgeTurnProgressExponent)
        .toDouble();
    _setEdgeTurnProgress(
      overscroll > 0
          ? ScrollEdgeTurnDirection.next
          : ScrollEdgeTurnDirection.previous,
      dampedProgress,
    );
  }

  void _setEdgeTurnProgress(
    ScrollEdgeTurnDirection? direction,
    double progress,
  ) {
    final clamped = progress.clamp(0.0, 1.0).toDouble();
    if (_edgeTurnDirection == direction &&
        (_edgeTurnProgress - clamped).abs() < .015) {
      return;
    }
    _edgeTurnDirection = direction;
    _edgeTurnProgress = clamped;
    widget.onEdgeTurnProgress(direction, clamped);
  }

  void _scheduleInitialScroll() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted || _initialScrollApplied) {
        return;
      }
      if (!widget.controller.hasClients) {
        _scheduleInitialScroll();
        return;
      }
      _initialScrollApplied = true;
      final position = widget.controller.position;
      final target =
          position.maxScrollExtent * widget.initialProgress.clamp(0.0, 1.0);
      widget.controller.jumpTo(
        target.clamp(position.minScrollExtent, position.maxScrollExtent),
      );
      _emitProgress(_estimateScrollPage(position));
    });
  }

  ScrollPageEstimate _estimateScrollPage(ScrollMetrics metrics) {
    final viewport = metrics.viewportDimension <= 0
        ? 1.0
        : metrics.viewportDimension;
    final pageCount = math.max(
      1,
      (metrics.maxScrollExtent / viewport).ceil() + 1,
    );
    final atBottom = metrics.pixels >= metrics.maxScrollExtent - 1;
    final page = atBottom
        ? pageCount - 1
        : (metrics.pixels / viewport).round().clamp(0, pageCount - 1);
    return ScrollPageEstimate(page: page, pageCount: pageCount);
  }

  void _emitProgress(ScrollPageEstimate estimate) {
    if (_lastReportedPage == estimate.page &&
        _lastReportedPageCount == estimate.pageCount) {
      return;
    }
    _lastReportedPage = estimate.page;
    _lastReportedPageCount = estimate.pageCount;
    widget.onProgressChanged(
      widget.navigationToken,
      estimate.page,
      estimate.pageCount,
    );
  }
}

// ---------------------------------------------------------------------------
// FlutterTxtPageView
// ---------------------------------------------------------------------------

class FlutterTxtPageView extends StatelessWidget {
  const FlutterTxtPageView({
    super.key,
    required this.blocks,
    required this.metrics,
    this.height,
    this.clipContent = true,
    required this.paragraphStyle,
    required this.titleStyle,
    required this.linkStyle,
    this.justifyText = true,
    required this.dimJapaneseText,
    required this.onLinkTap,
    required this.onFootnoteTap,
  });

  final List<FlutterTxtBlock> blocks;
  final TxtLayoutMetrics metrics;
  final double? height;
  final bool clipContent;
  final TextStyle paragraphStyle;
  final TextStyle titleStyle;
  final TextStyle linkStyle;
  final bool justifyText;
  final bool dimJapaneseText;
  final Future<void> Function(String href) onLinkTap;
  final void Function(String text, Offset? globalPosition) onFootnoteTap;

  @override
  Widget build(BuildContext context) {
    final firstLineIndentWidth = (paragraphStyle.fontSize ?? 18) * 2;
    return Padding(
      padding: EdgeInsets.symmetric(horizontal: metrics.pageOuterInset),
      child: SizedBox(
        width: double.infinity,
        child: Padding(
          padding: metrics.padding,
          child: SizedBox(
            width: metrics.contentWidth,
            height: height,
            child: _maybeClip(
              Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  for (final block in blocks)
                    FlutterTxtBlockView(
                      block: block,
                      paragraphStyle: paragraphStyle,
                      titleStyle: titleStyle,
                      linkStyle: linkStyle,
                      firstLineIndentWidth: firstLineIndentWidth,
                      justifyText: justifyText,
                      dimJapaneseText: dimJapaneseText,
                      onLinkTap: onLinkTap,
                      onFootnoteTap: onFootnoteTap,
                    ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _maybeClip(Widget child) {
    return clipContent ? ClipRect(child: child) : child;
  }
}

// ---------------------------------------------------------------------------
// FlutterTxtBlockView
// ---------------------------------------------------------------------------

class FlutterTxtBlockView extends StatelessWidget {
  const FlutterTxtBlockView({
    super.key,
    required this.block,
    required this.paragraphStyle,
    required this.titleStyle,
    required this.linkStyle,
    required this.firstLineIndentWidth,
    required this.justifyText,
    required this.onLinkTap,
    required this.onFootnoteTap,
    required this.dimJapaneseText,
  });

  final FlutterTxtBlock block;
  final TextStyle paragraphStyle;
  final TextStyle titleStyle;
  final TextStyle linkStyle;
  final double firstLineIndentWidth;
  final bool justifyText;
  final Future<void> Function(String href) onLinkTap;
  final void Function(String text, Offset? globalPosition) onFootnoteTap;
  final bool dimJapaneseText;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(bottom: block.bottomSpacing),
      child: block.kind == FlutterTxtBlockKind.image
          ? SizedBox(
              height: block.imageHeight,
              child: FlutterReaderImage(source: block.imageSource ?? ''),
            )
          : block.kind == FlutterTxtBlockKind.link
          ? InkWell(
              onTap: block.href == null
                  ? null
                  : () => unawaited(onLinkTap(block.href!)),
              child: Text(
                block.text,
                textScaler: TextScaler.noScaling,
                style: linkStyle,
                textAlign: TextAlign.start,
                strutStyle: _strutStyleFor(linkStyle),
                textHeightBehavior: _stableTextHeightBehavior,
                textWidthBasis: TextWidthBasis.parent,
              ),
            )
          : RichText(
              textScaler: TextScaler.noScaling,
              textAlign:
                  block.textAlign ??
                  (justifyText && block.kind == FlutterTxtBlockKind.paragraph
                      ? TextAlign.justify
                      : TextAlign.start),
              strutStyle: _strutStyleFor(
                block.kind == FlutterTxtBlockKind.title
                    ? titleStyle
                    : paragraphStyle,
              ),
              textHeightBehavior: _stableTextHeightBehavior,
              textWidthBasis: TextWidthBasis.parent,
              text: _buildBlockTextSpan(
                block,
                paragraphStyle,
                titleStyle,
                linkStyle,
                firstLineIndentWidth,
                onLinkTap,
                onFootnoteTap,
                dimJapaneseText,
              ),
            ),
    );
  }

  static const _stableTextHeightBehavior = TextHeightBehavior(
    applyHeightToFirstAscent: true,
    applyHeightToLastDescent: true,
  );

  static StrutStyle _strutStyleFor(TextStyle style) {
    return StrutStyle(
      fontFamily: style.fontFamily,
      fontSize: style.fontSize,
      height: style.height,
      leadingDistribution: style.leadingDistribution,
      fontWeight: style.fontWeight,
      forceStrutHeight: true,
    );
  }

  TextSpan _buildBlockTextSpan(
    FlutterTxtBlock block,
    TextStyle paragraphStyle,
    TextStyle titleStyle,
    TextStyle linkStyle,
    double firstLineIndentWidth,
    Future<void> Function(String href) onLinkTap,
    void Function(String text, Offset? globalPosition) onFootnoteTap,
    bool dimJapaneseText,
  ) {
    if (block.kind == FlutterTxtBlockKind.title) {
      return TextSpan(text: block.text, style: titleStyle);
    }
    final spans = _segmentSpans(
      block,
      paragraphStyle,
      linkStyle,
      onLinkTap,
      onFootnoteTap,
      dimJapaneseText,
    );
    if (!block.firstLineIndent) {
      return TextSpan(style: paragraphStyle, children: spans);
    }
    return TextSpan(
      style: paragraphStyle,
      children: [
        WidgetSpan(
          alignment: PlaceholderAlignment.baseline,
          baseline: TextBaseline.alphabetic,
          child: SizedBox(width: firstLineIndentWidth, height: 0),
        ),
        ...spans,
      ],
    );
  }

  List<InlineSpan> _segmentSpans(
    FlutterTxtBlock block,
    TextStyle paragraphStyle,
    TextStyle linkStyle,
    Future<void> Function(String href) onLinkTap,
    void Function(String text, Offset? globalPosition) onFootnoteTap,
    bool dimJapaneseText,
  ) {
    final segments = block.segments;
    final dimBlock = dimJapaneseText && _shouldDimJapaneseParagraph(block.text);
    if (segments == null || segments.isEmpty) {
      return [_japaneseAwareSpan(block.text, paragraphStyle, dimBlock)];
    }
    return [
      for (final segment in segments)
        if (segment.footnote case final footnote?)
          WidgetSpan(
            alignment: PlaceholderAlignment.middle,
            child: FootnoteInlineChip(
              label: segment.text.trim().isEmpty
                  ? '\u6ce8'
                  : segment.text.trim(),
              color: segment.color ?? linkStyle.color ?? paragraphStyle.color ?? Colors.red,
              onTap: (position) => onFootnoteTap(footnote, position),
            ),
          )
        else if (segment.href case final href?)
          WidgetSpan(
            alignment: PlaceholderAlignment.baseline,
            baseline: TextBaseline.alphabetic,
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () => unawaited(onLinkTap(href)),
              child: Text(
                segment.text,
                textScaler: TextScaler.noScaling,
                style: segment.color != null ? linkStyle.copyWith(color: segment.color) : linkStyle,
              ),
            ),
          )
        else
          _styledSegmentSpan(segment, paragraphStyle, dimBlock),
    ];
  }

  TextSpan _styledSegmentSpan(
    InlineTextSegment segment,
    TextStyle baseStyle,
    bool dim,
  ) {
    var style = baseStyle;
    if (segment.color != null) {
      style = style.copyWith(color: segment.color);
    }
    if (segment.isBold) {
      style = style.copyWith(
        fontWeight: FontWeight.bold,
        fontVariations: const [FontVariation('wght', 700)],
      );
    }
    if (segment.isItalic) {
      style = style.copyWith(fontStyle: FontStyle.italic);
    }
    if (dim && segment.color == null) {
      final dimStyle = style.copyWith(
        color: (style.color ?? Colors.black).withValues(alpha: .30),
      );
      return TextSpan(text: segment.text, style: dimStyle);
    }
    return TextSpan(text: segment.text, style: style);
  }

  TextSpan _japaneseAwareSpan(String text, TextStyle baseStyle, bool dim) {
    if (!dim || text.isEmpty) {
      return TextSpan(text: text);
    }
    final dimStyle = baseStyle.copyWith(
      color: (baseStyle.color ?? Colors.black).withValues(alpha: .30),
    );
    return TextSpan(text: text, style: dimStyle);
  }

  bool _shouldDimJapaneseParagraph(String text) {
    var kana = 0;
    var cjk = 0;
    var visible = 0;
    var japanesePunctuation = 0;
    for (final rune in text.runes) {
      final char = String.fromCharCode(rune);
      if (char.trim().isEmpty) {
        continue;
      }
      visible++;
      if (_isKana(rune)) {
        kana++;
      } else if (_isCjkIdeograph(rune)) {
        cjk++;
      } else if (_isJapanesePunctuation(rune)) {
        japanesePunctuation++;
      }
    }
    if (visible == 0) {
      return false;
    }
    final kanaRatio = kana / visible;
    final cjkRatio = cjk / visible;
    if (kana < 6 || kanaRatio < .24) {
      return false;
    }
    final grammarScore =
        _japaneseGrammarScore(text) + (japanesePunctuation >= 2 ? 1 : 0);
    if (grammarScore >= 2 && kanaRatio >= .28) {
      return true;
    }
    return grammarScore >= 1 &&
        kana >= 12 &&
        kanaRatio >= .42 &&
        cjkRatio < .22;
  }

  bool _isKana(int rune) {
    return (rune >= 0x3040 && rune <= 0x309F) ||
        (rune >= 0x30A0 && rune <= 0x30FF) ||
        (rune >= 0x31F0 && rune <= 0x31FF) ||
        (rune >= 0xFF66 && rune <= 0xFF9D);
  }

  bool _isCjkIdeograph(int rune) {
    return (rune >= 0x3400 && rune <= 0x4DBF) ||
        (rune >= 0x4E00 && rune <= 0x9FFF) ||
        (rune >= 0xF900 && rune <= 0xFAFF);
  }

  bool _isJapanesePunctuation(int rune) {
    return rune == 0x3001 ||
        rune == 0x3002 ||
        rune == 0x30FB ||
        rune == 0x300C ||
        rune == 0x300D ||
        rune == 0x300E ||
        rune == 0x300F;
  }

  int _japaneseGrammarScore(String text) {
    var score = 0;
    final patterns = <RegExp>[
      RegExp(
        r'[\u3041-\u309F\u30A0-\u30FF\u3400-\u9FFF]'
        r'(\u3067\u3059|\u307E\u3059|\u3067\u3057\u305F|\u307E\u305B\u3093|\u3060|\u3060\u3063\u305F|\u3058\u3083\u306A\u3044|\u3067\u3057\u3087\u3046|\u304F\u3060\u3055\u3044)',
      ),
      RegExp(
        r'[\u3041-\u309F\u30A0-\u30FF\u3400-\u9FFF]'
        r'(\u3057\u305F|\u3057\u3066|\u3059\u308B|\u3055\u308C|\u308C\u308B|\u3089\u308C\u308B|\u306A\u3044|\u306A\u304B\u3063\u305F|\u304B\u3063\u305F|\u305F\u3044|\u305D\u3046|\u3088\u3046)',
      ),
      RegExp(
        r'[\u3041-\u309F\u30A0-\u30FF\u3400-\u9FFF]'
        r'(\u304B\u3089|\u307E\u3067|\u3088\u308A|\u306E\u3067|\u3051\u3069|\u306A\u3089|\u306E\u306B|\u3066\u3082|\u3067\u306F|\u306B\u306F)',
      ),
      RegExp(
        r'(\u3053\u308C|\u305D\u308C|\u3042\u308C|\u3053\u306E|\u305D\u306E|\u3042\u306E|\u3053\u3053|\u305D\u3053|\u305D\u3057\u3066|\u3067\u3082|\u3060\u304B\u3089|\u3057\u304B\u3057)',
      ),
    ];
    for (final pattern in patterns) {
      if (pattern.hasMatch(text)) {
        score++;
      }
    }
    return score;
  }
}

// ---------------------------------------------------------------------------
// FootnoteInlineChip
// ---------------------------------------------------------------------------

class FootnoteInlineChip extends StatelessWidget {
  const FootnoteInlineChip({
    super.key,
    required this.label,
    required this.color,
    required this.onTap,
  });

  final String label;
  final Color color;
  final ValueChanged<Offset?> onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTapUp: (details) => onTap(details.globalPosition),
      child: Container(
        margin: const EdgeInsets.symmetric(horizontal: 2),
        padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
        decoration: BoxDecoration(
          color: color.withValues(alpha: .14),
          borderRadius: BorderRadius.circular(999),
        ),
        child: Text(
          label,
          textScaler: TextScaler.noScaling,
          style: TextStyle(
            color: color,
            fontSize: 11,
            height: 1.05,
            fontWeight: AppTextWeight.semibold,
            decoration: TextDecoration.none,
          ),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// FootnotePopupData / FootnotePopupOverlay / FootnotePopupBody
// ---------------------------------------------------------------------------

class FootnotePopupData {
  const FootnotePopupData({
    required this.text,
    required this.anchor,
    required this.serial,
  });

  final String text;
  final Offset? anchor;
  final int serial;
}

class FootnotePopupOverlay extends StatefulWidget {
  const FootnotePopupOverlay({
    super.key,
    required this.data,
    required this.palette,
    required this.readerPalette,
    required this.onDismiss,
  });

  final FootnotePopupData? data;
  final AppPalette palette;
  final ReaderPalette readerPalette;
  final VoidCallback onDismiss;

  @override
  State<FootnotePopupOverlay> createState() => _FootnotePopupOverlayState();
}

class _FootnotePopupOverlayState extends State<FootnotePopupOverlay>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  late final Animation<double> _scale;
  late final Animation<double> _opacity;
  FootnotePopupData? _visibleData;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 180),
      reverseDuration: const Duration(milliseconds: 140),
    );
    final curved = CurvedAnimation(
      parent: _controller,
      curve: Curves.easeOutBack,
      reverseCurve: Curves.easeInCubic,
    );
    _scale = Tween<double>(begin: .82, end: 1).animate(curved);
    _opacity = CurvedAnimation(
      parent: _controller,
      curve: Curves.easeOutCubic,
      reverseCurve: Curves.easeInCubic,
    );
    _visibleData = widget.data;
    if (_visibleData != null) {
      _controller.value = 1;
    }
  }

  @override
  void didUpdateWidget(covariant FootnotePopupOverlay oldWidget) {
    super.didUpdateWidget(oldWidget);
    final data = widget.data;
    if (data != null) {
      setState(() => _visibleData = data);
      _controller.forward(from: 0);
      return;
    }
    if (oldWidget.data != null) {
      _controller.reverse().whenComplete(() {
        if (mounted && widget.data == null) {
          setState(() => _visibleData = null);
        }
      });
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final data = _visibleData;
    return Positioned.fill(
      child: data == null
          ? const SizedBox.shrink()
          : Stack(
              children: [
                Positioned.fill(
                  child: GestureDetector(
                    behavior: HitTestBehavior.translucent,
                    onTap: widget.onDismiss,
                    child: const SizedBox.expand(),
                  ),
                ),
                FootnotePopupBody(
                  key: ValueKey(data.serial),
                  data: data,
                  palette: widget.palette,
                  readerPalette: widget.readerPalette,
                  scale: _scale,
                  opacity: _opacity,
                ),
              ],
            ),
    );
  }
}

class FootnotePopupBody extends StatelessWidget {
  const FootnotePopupBody({
    super.key,
    required this.data,
    required this.palette,
    required this.readerPalette,
    required this.scale,
    required this.opacity,
  });

  final FootnotePopupData data;
  final AppPalette palette;
  final ReaderPalette readerPalette;
  final Animation<double> scale;
  final Animation<double> opacity;

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.sizeOf(context);
    final padding = MediaQuery.viewPaddingOf(context);
    const horizontalMargin = 18.0;
    const anchorGap = 4.0;
    const minCardWidth = 220.0;
    const maxCardWidth = 360.0;
    final usableWidth = (size.width - horizontalMargin * 2)
        .clamp(minCardWidth, double.infinity)
        .toDouble();
    final cardWidth = (size.width >= 600 ? maxCardWidth : size.width * .62)
        .clamp(minCardWidth, math.min(maxCardWidth, usableWidth))
        .toDouble();
    final anchor = data.anchor ?? Offset(size.width / 2, size.height * .58);
    final bodyStyle = TextStyle(
      color: palette.text,
      fontSize: 14,
      height: 1.45,
      fontWeight: AppTextWeight.regular,
    );
    final bodyPainter = TextPainter(
      text: TextSpan(text: data.text, style: bodyStyle),
      textDirection: TextDirection.ltr,
      textScaler: TextScaler.noScaling,
    )..layout(maxWidth: cardWidth - 32);
    final estimatedHeight = 14 + 26 + 8 + bodyPainter.height + 15;
    final minTop = padding.top + 14;
    final maxTop = size.height - padding.bottom - estimatedHeight - 14;
    final fitsBelow =
        anchor.dy + anchorGap + estimatedHeight <=
        size.height - padding.bottom - 14;
    final fitsAbove = anchor.dy - anchorGap - estimatedHeight >= minTop;
    final placeBelow =
        fitsBelow && (!fitsAbove || anchor.dy < size.height * .62);
    final scaleAlignment = placeBelow
        ? Alignment.topCenter
        : Alignment.bottomCenter;
    final desiredTop = placeBelow
        ? anchor.dy + anchorGap
        : anchor.dy - estimatedHeight - anchorGap;
    final top = desiredTop
        .clamp(minTop, maxTop.clamp(minTop, double.infinity))
        .toDouble();
    final maxLeft = (size.width - cardWidth - horizontalMargin).clamp(
      horizontalMargin,
      double.infinity,
    );
    final left = (anchor.dx - cardWidth / 2)
        .clamp(horizontalMargin, maxLeft)
        .toDouble();
    final isDark =
        ThemeData.estimateBrightnessForColor(readerPalette.background) ==
        Brightness.dark;
    final background = isDark
        ? Color.lerp(palette.surface, Colors.black, .12)!
        : Color.lerp(palette.surface, Colors.white, .45)!;
    return Stack(
      children: [
        Positioned(
          left: left,
          top: top,
          width: cardWidth,
          child: AnimatedBuilder(
            animation: Listenable.merge([scale, opacity]),
            builder: (context, child) {
              return Opacity(
                opacity: opacity.value,
                child: Transform.scale(
                  scale: scale.value,
                  alignment: scaleAlignment,
                  child: child,
                ),
              );
            },
            child: Material(
              color: Colors.transparent,
              child: Container(
                padding: const EdgeInsets.fromLTRB(16, 14, 16, 15),
                decoration: BoxDecoration(
                  color: background,
                  borderRadius: BorderRadius.circular(20),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withValues(alpha: isDark ? .32 : .16),
                      blurRadius: 24,
                      offset: const Offset(0, 10),
                    ),
                  ],
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          width: 26,
                          height: 26,
                          alignment: Alignment.center,
                          decoration: BoxDecoration(
                            color: palette.primarySoft.withValues(alpha: .14),
                            borderRadius: BorderRadius.circular(999),
                          ),
                          child: Text(
                            '\u6ce8',
                            textScaler: TextScaler.noScaling,
                            style: TextStyle(
                              color: palette.primarySoft,
                              fontSize: 13,
                              fontWeight: AppTextWeight.semibold,
                            ),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            '\u7f16\u6ce8',
                            textScaler: TextScaler.noScaling,
                            style: TextStyle(
                              color: palette.text,
                              fontSize: 13,
                              fontWeight: AppTextWeight.semibold,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Text(
                      data.text,
                      textScaler: TextScaler.noScaling,
                      style: bodyStyle,
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

// ---------------------------------------------------------------------------
// TxtLayoutMetrics
// ---------------------------------------------------------------------------

class TxtLayoutMetrics {
  const TxtLayoutMetrics({
    required this.padding,
    required this.pageOuterInset,
    required this.contentWidth,
    required this.contentHeight,
  });

  final EdgeInsets padding;
  final double pageOuterInset;
  final double contentWidth;
  final double contentHeight;
}

// ---------------------------------------------------------------------------
// FlutterTxtDocument / FlutterDocumentBlock / EpubLinkBlock / FlutterTxtPage
// ---------------------------------------------------------------------------

class FlutterTxtDocument {
  const FlutterTxtDocument({required this.title, required this.blocks});

  final String title;
  final List<FlutterDocumentBlock> blocks;
}

enum FlutterDocumentBlockKind { paragraph, image, link }

class FlutterDocumentBlock {
  const FlutterDocumentBlock.paragraph(
    this.text, {
    this.textAlign,
    this.forceNoIndent = false,
  }) : kind = FlutterDocumentBlockKind.paragraph,
       imageSource = null,
       href = null,
       segments = null;

  const FlutterDocumentBlock.image(this.imageSource)
    : kind = FlutterDocumentBlockKind.image,
      text = '',
      href = null,
      segments = null,
      textAlign = null,
      forceNoIndent = false;

  const FlutterDocumentBlock.link(
    this.text,
    this.href, {
    this.textAlign,
    this.forceNoIndent = false,
  }) : kind = FlutterDocumentBlockKind.link,
       imageSource = null,
       segments = null;

  FlutterDocumentBlock.rich(
    List<InlineTextSegment> segments, {
    this.textAlign,
    this.forceNoIndent = false,
  }) : kind = FlutterDocumentBlockKind.paragraph,
       text = segmentsText(segments),
       imageSource = null,
       href = null,
       segments = mergeInlineSegments(segments);

  final FlutterDocumentBlockKind kind;
  final String text;
  final String? imageSource;
  final String? href;
  final List<InlineTextSegment>? segments;
  final TextAlign? textAlign;
  final bool forceNoIndent;
}

class EpubLinkBlock {
  const EpubLinkBlock({required this.text, required this.href});

  final String text;
  final String href;
}

class FlutterTxtPage {
  const FlutterTxtPage({required this.blocks});

  factory FlutterTxtPage.empty() {
    return const FlutterTxtPage(blocks: []);
  }

  final List<FlutterTxtBlock> blocks;
}

enum FlutterTxtBlockKind { title, paragraph, image, link }

class FlutterTxtBlock {
  const FlutterTxtBlock({
    required this.text,
    required this.kind,
    required this.bottomSpacing,
    this.firstLineIndent = false,
    this.imageSource,
    this.imageHeight = 0,
    this.href,
    this.segments,
    this.textAlign,
  });

  final String text;
  final FlutterTxtBlockKind kind;
  final double bottomSpacing;
  final bool firstLineIndent;
  final String? imageSource;
  final double imageHeight;
  final String? href;
  final List<InlineTextSegment>? segments;
  final TextAlign? textAlign;
}

class InlineTextSegment {
  const InlineTextSegment({
    required this.text,
    this.href,
    this.footnote,
    this.color,
    this.isBold = false,
    this.isItalic = false,
  });

  final String text;
  final String? href;
  final String? footnote;
  final Color? color;
  final bool isBold;
  final bool isItalic;

  InlineTextSegment copyWith({
    String? text,
    String? href,
    String? footnote,
    Color? color,
    bool? isBold,
    bool? isItalic,
  }) {
    return InlineTextSegment(
      text: text ?? this.text,
      href: href ?? this.href,
      footnote: footnote ?? this.footnote,
      color: color ?? this.color,
      isBold: isBold ?? this.isBold,
      isItalic: isItalic ?? this.isItalic,
    );
  }
}

String segmentsText(List<InlineTextSegment> segments) {
  return segments.map((segment) => segment.text).join();
}

List<InlineTextSegment> mergeInlineSegments(List<InlineTextSegment> segments) {
  final result = <InlineTextSegment>[];
  for (final segment in segments) {
    if (segment.text.isEmpty) {
      continue;
    }
    final previous = result.isEmpty ? null : result.last;
    if (previous != null &&
        previous.href == segment.href &&
        previous.footnote == segment.footnote &&
        previous.color == segment.color &&
        previous.isBold == segment.isBold &&
        previous.isItalic == segment.isItalic) {
      result[result.length - 1] = previous.copyWith(
        text: previous.text + segment.text,
      );
    } else {
      result.add(segment);
    }
  }
  return result;
}

// ---------------------------------------------------------------------------
// FlutterReaderImage
// ---------------------------------------------------------------------------

class FlutterReaderImage extends StatefulWidget {
  const FlutterReaderImage({super.key, required this.source});

  final String source;

  @override
  State<FlutterReaderImage> createState() => _FlutterReaderImageState();
}

class _FlutterReaderImageState extends State<FlutterReaderImage> {
  ImageProvider? _provider;
  String? _providerSource;
  var _broken = false;

  @override
  void initState() {
    super.initState();
    _resolveProvider();
  }

  @override
  void didUpdateWidget(covariant FlutterReaderImage oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.source != widget.source) {
      _provider = null;
      _providerSource = null;
      _broken = false;
      _resolveProvider();
    }
  }

  void _resolveProvider() {
    final source = widget.source;
    if (source.isEmpty) {
      _broken = true;
      return;
    }
    if (_providerSource == source) {
      return;
    }
    final uri = Uri.tryParse(source);
    try {
      if (uri?.scheme == 'file') {
        _provider = FileImage(File(uri!.toFilePath()));
      } else if (uri?.scheme == 'data') {
        final comma = source.indexOf(',');
        if (comma <= 0 || !source.substring(0, comma).contains(';base64')) {
          _broken = true;
          return;
        }
        _provider = MemoryImage(base64Decode(source.substring(comma + 1)));
      } else {
        _provider = NetworkImage(source);
      }
      _providerSource = source;
      _broken = false;
    } catch (_) {
      _provider = null;
      _providerSource = null;
      _broken = true;
    }
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onLongPress: () {
        Navigator.of(context).push<void>(readerImageViewerRoute(widget.source));
      },
      child: LayoutBuilder(
        builder: (context, constraints) {
          final provider = _provider;
          if (_broken || provider == null) {
            return const Center(child: Icon(Icons.broken_image_rounded));
          }
          final devicePixelRatio = MediaQuery.devicePixelRatioOf(context);
          final cacheWidth = constraints.hasBoundedWidth
              ? (constraints.maxWidth * devicePixelRatio).round()
              : null;
          return Center(
            child: Image(
              image: ResizeImage.resizeIfNeeded(cacheWidth, null, provider),
              width: constraints.hasBoundedWidth ? constraints.maxWidth : null,
              height: constraints.hasBoundedHeight
                  ? constraints.maxHeight
                  : null,
              fit: BoxFit.contain,
              gaplessPlayback: true,
              filterQuality: FilterQuality.medium,
              errorBuilder: (_, _, _) => const Icon(Icons.broken_image_rounded),
            ),
          );
        },
      ),
    );
  }
}
