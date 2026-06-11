import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../models.dart';
import '../typography.dart';
import 'reader_enums.dart';
import 'reader_epub_fallback.dart';
import 'reader_scroll_edge.dart';

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

  @override
  State<FlutterTxtReaderView> createState() => _FlutterTxtReaderViewState();
}

class _FlutterTxtReaderViewState extends State<FlutterTxtReaderView> {
  static const double _edgeTurnThreshold = 56;

  late final PageController _pageController;
  double _edgeOverscroll = 0;
  var _edgeTurnInFlight = false;

  @override
  void initState() {
    super.initState();
    _pageController = PageController(initialPage: _clampedCurrentPage);
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
      fontWeight: AppTextWeight.regular,
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
      child: ColoredBox(
        color: widget.readerPalette.background,
        child: NotificationListener<ScrollNotification>(
          onNotification: _handlePageScrollNotification,
          child: PageView.builder(
            controller: _pageController,
            physics: const PageScrollPhysics(
              parent: AlwaysScrollableScrollPhysics(),
            ),
            itemCount: safePages.length,
            onPageChanged: (page) =>
                widget.onPageChanged(widget.navigationToken, page),
            itemBuilder: (context, index) {
              return RepaintBoundary(
                child: _txtPageView(
                  blocks: safePages[index].blocks,
                  paragraphStyle: paragraphStyle,
                  titleStyle: titleStyle,
                  linkStyle: linkStyle,
                ),
              );
            },
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
      if (!mounted || !_pageController.hasClients) {
        return;
      }
      final target = _clampedCurrentPage;
      final current = (_pageController.page ?? _pageController.initialPage)
          .round();
      if (current == target) {
        return;
      }
      if (animated && (current - target).abs() == 1) {
        unawaited(
          _pageController.animateToPage(
            target,
            duration: const Duration(milliseconds: 180),
            curve: Curves.easeOutCubic,
          ),
        );
      } else {
        _pageController.jumpToPage(target);
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
    required this.onEdgePrevious,
    required this.onEdgeNext,
    required this.onLinkTap,
    required this.onFootnoteTap,
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
  final Future<void> Function() onEdgePrevious;
  final Future<void> Function() onEdgeNext;
  final Future<void> Function(String href) onLinkTap;
  final void Function(String text, Offset? globalPosition) onFootnoteTap;

  @override
  State<FlutterTxtScrollReaderView> createState() =>
      _FlutterTxtScrollReaderViewState();
}

class _FlutterTxtScrollReaderViewState
    extends State<FlutterTxtScrollReaderView> {
  static const double _edgeTurnThreshold = 132;

  double _edgeOverscroll = 0;
  ScrollEdgeTurnDirection? _edgeTurnDirection;
  var _edgeTurnInFlight = false;
  var _initialScrollApplied = false;
  var _lastReportedPage = -1;
  var _lastReportedPageCount = -1;
  final ValueNotifier<ScrollEdgeTurnState> _edgeTurnNotifier =
      ValueNotifier<ScrollEdgeTurnState>(const ScrollEdgeTurnState.hidden());

  @override
  void initState() {
    super.initState();
    _scheduleInitialScroll();
  }

  @override
  void dispose() {
    _edgeTurnNotifier.dispose();
    super.dispose();
  }

  @override
  void didUpdateWidget(covariant FlutterTxtScrollReaderView oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.key != widget.key) {
      _initialScrollApplied = false;
      _lastReportedPage = -1;
      _lastReportedPageCount = -1;
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
      fontWeight: AppTextWeight.regular,
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
      child: ColoredBox(
        color: widget.readerPalette.background,
        child: Stack(
          children: [
            NotificationListener<ScrollNotification>(
              onNotification: _handleScrollNotification,
              child: ListView.builder(
                controller: widget.controller,
                physics: const ClampingScrollPhysics(
                  parent: AlwaysScrollableScrollPhysics(),
                ),
                padding: EdgeInsets.only(
                  left:
                      widget.metrics.pageOuterInset +
                      widget.metrics.padding.left,
                  right:
                      widget.metrics.pageOuterInset +
                      widget.metrics.padding.right,
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
                          onLinkTap: widget.onLinkTap,
                          onFootnoteTap: widget.onFootnoteTap,
                        ),
                      ),
                    ),
                  );
                },
              ),
            ),
            ValueListenableBuilder<ScrollEdgeTurnState>(
              valueListenable: _edgeTurnNotifier,
              builder: (context, state, _) {
                if (state.progress <= 0 || state.direction == null) {
                  return const SizedBox.shrink();
                }
                return ScrollEdgeTurnHintPositioned(
                  direction: state.direction!,
                  progress: state.progress,
                  readerPalette: widget.readerPalette,
                  palette: widget.appPalette,
                  systemPadding: MediaQuery.viewPaddingOf(context),
                );
              },
            ),
          ],
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
    _setEdgeTurnProgress(
      overscroll > 0
          ? ScrollEdgeTurnDirection.next
          : ScrollEdgeTurnDirection.previous,
      magnitude / _edgeTurnThreshold,
    );
  }

  void _setEdgeTurnProgress(
    ScrollEdgeTurnDirection? direction,
    double progress,
  ) {
    final clamped = progress.clamp(0.0, 1.0).toDouble();
    if (_edgeTurnDirection == direction &&
        (_edgeTurnNotifier.value.progress - clamped).abs() < .015) {
      return;
    }
    _edgeTurnDirection = direction;
    _edgeTurnNotifier.value = ScrollEdgeTurnState(
      direction: direction,
      progress: clamped,
    );
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
  });

  final FlutterTxtBlock block;
  final TextStyle paragraphStyle;
  final TextStyle titleStyle;
  final TextStyle linkStyle;
  final double firstLineIndentWidth;
  final bool justifyText;
  final Future<void> Function(String href) onLinkTap;
  final void Function(String text, Offset? globalPosition) onFootnoteTap;

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
                  justifyText && block.kind == FlutterTxtBlockKind.paragraph
                  ? TextAlign.justify
                  : TextAlign.start,
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
  ) {
    final segments = block.segments;
    if (segments == null || segments.isEmpty) {
      return [TextSpan(text: block.text)];
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
              color: linkStyle.color ?? paragraphStyle.color ?? Colors.red,
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
                style: linkStyle,
              ),
            ),
          )
        else
          TextSpan(text: segment.text),
    ];
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
  const FlutterDocumentBlock.paragraph(this.text)
    : kind = FlutterDocumentBlockKind.paragraph,
      imageSource = null,
      href = null,
      segments = null;

  const FlutterDocumentBlock.image(this.imageSource)
    : kind = FlutterDocumentBlockKind.image,
      text = '',
      href = null,
      segments = null;

  const FlutterDocumentBlock.link(this.text, this.href)
    : kind = FlutterDocumentBlockKind.link,
      imageSource = null,
      segments = null;

  FlutterDocumentBlock.rich(List<InlineTextSegment> segments)
    : kind = FlutterDocumentBlockKind.paragraph,
      text = segmentsText(segments),
      imageSource = null,
      href = null,
      segments = mergeInlineSegments(segments);

  final FlutterDocumentBlockKind kind;
  final String text;
  final String? imageSource;
  final String? href;
  final List<InlineTextSegment>? segments;
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
  });

  final String text;
  final FlutterTxtBlockKind kind;
  final double bottomSpacing;
  final bool firstLineIndent;
  final String? imageSource;
  final double imageHeight;
  final String? href;
  final List<InlineTextSegment>? segments;
}

class InlineTextSegment {
  const InlineTextSegment({required this.text, this.href, this.footnote});

  final String text;
  final String? href;
  final String? footnote;

  InlineTextSegment copyWith({String? text}) {
    return InlineTextSegment(
      text: text ?? this.text,
      href: href,
      footnote: footnote,
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
        previous.footnote == segment.footnote) {
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

class FlutterReaderImage extends StatelessWidget {
  const FlutterReaderImage({super.key, required this.source});

  final String source;

  @override
  Widget build(BuildContext context) {
    final uri = Uri.tryParse(source);
    final image = _imageForSource(uri);
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onLongPress: () {
        Navigator.of(context).push<void>(
          PageRouteBuilder<void>(
            opaque: true,
            transitionDuration: Duration.zero,
            reverseTransitionDuration: Duration.zero,
            pageBuilder: (context, animation, secondaryAnimation) =>
                FullscreenImageViewer(source: source),
          ),
        );
      },
      child: Center(child: image),
    );
  }

  Widget _imageForSource(Uri? uri) {
    if (source.isEmpty) {
      return const Icon(Icons.broken_image_rounded);
    }
    if (uri?.scheme == 'file') {
      return Image.file(File(uri!.toFilePath()), fit: BoxFit.contain);
    }
    if (uri?.scheme == 'data') {
      final comma = source.indexOf(',');
      if (comma > 0 && source.substring(0, comma).contains(';base64')) {
        try {
          return Image.memory(
            base64Decode(source.substring(comma + 1)),
            fit: BoxFit.contain,
          );
        } catch (_) {
          // Fall through to a small placeholder below.
        }
      }
      return const Icon(Icons.broken_image_rounded);
    }
    return Image.network(source, fit: BoxFit.contain);
  }
}
