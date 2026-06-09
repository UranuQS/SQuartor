import 'dart:convert';
import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:flutter/services.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:html/dom.dart' as dom;
import 'package:html/parser.dart' as html_parser;
import 'package:path/path.dart' as path;
import 'package:url_launcher/url_launcher.dart';

import '../app_state.dart';
import '../models.dart';
import '../typography.dart';

enum _ReaderOverlay { hidden, chrome, toc, settings }

class _ReaderGlassPalette {
  const _ReaderGlassPalette({
    required this.dark,
    required this.panel,
    required this.pill,
    required this.text,
    required this.muted,
    required this.subtle,
    required this.line,
    required this.scrim,
  });

  final bool dark;
  final Color panel;
  final Color pill;
  final Color text;
  final Color muted;
  final Color subtle;
  final Color line;
  final Color scrim;

  factory _ReaderGlassPalette.from(AppPalette palette) {
    final dark =
        ThemeData.estimateBrightnessForColor(palette.background) ==
        Brightness.dark;
    if (dark) {
      return _ReaderGlassPalette(
        dark: true,
        panel: palette.surface,
        pill: palette.cardAlt,
        text: palette.text,
        muted: palette.muted,
        subtle: palette.subtle,
        line: palette.line,
        scrim: Colors.black.withValues(alpha: .54),
      );
    }
    return _ReaderGlassPalette(
      dark: false,
      panel: palette.surface,
      pill: palette.cardAlt,
      text: palette.text,
      muted: palette.muted,
      subtle: palette.subtle,
      line: palette.line,
      scrim: Colors.black.withValues(alpha: .30),
    );
  }
}

class ReaderScreen extends StatefulWidget {
  const ReaderScreen({super.key, required this.state, required this.book});

  static const routeName = '/reader';

  final AppState state;
  final BookEntry book;

  @override
  State<ReaderScreen> createState() => _ReaderScreenState();
}

class _ReaderScreenState extends State<ReaderScreen>
    with TickerProviderStateMixin {
  InAppWebViewController? _controller;
  late int _chapterIndex;
  var _page = 0;
  var _pageCount = 1;
  var _overlay = _ReaderOverlay.chrome;
  var _isLoading = true;
  String? _loadError;
  String? _pendingAnchor;
  Timer? _readingTimer;
  Timer? _styleInjectTimer;
  Timer? _txtPaginationTimer;
  Timer? _clockTimer;
  var _overlayTransitionSerial = 0;
  late final AnimationController _chromeAnimation;
  late final AnimationController _tocAnimation;
  late final AnimationController _settingsAnimation;
  late final AnimationController _footerAnimation;
  MemoryImage? _readerSnapshotImage;
  Uint8List? _readerSnapshotBytes;
  var _now = DateTime.now();
  final _readingStopwatch = Stopwatch();
  double _dragDx = 0;
  double _dragDy = 0;
  var _pageDragActive = false;
  var _dragMoveScheduled = false;
  var _dragSession = 0;
  var _linkTapHandled = false;
  double? _pendingPageProgress;
  int? _pendingExactPage;
  int? _pendingExactPageCount;
  late PageController _txtPageController;
  List<_FlutterTxtPage> _txtPages = const [];
  _TxtLayoutMetrics? _txtLayoutMetrics;
  String? _txtPaginationSignature;
  String? _txtRequestedSignature;
  String? _flutterReaderFontFamily;
  final Set<String> _epubWebViewFallbackChapters = {};
  final Set<String> _registeredReaderFontFamilies = {};

  BookEntry get book => widget.book;
  bool get _usesFlutterTxt {
    if (book.format == BookFormat.txt) {
      return true;
    }
    if (book.format != BookFormat.epub || _chapter.filePath.isEmpty) {
      return false;
    }
    return !_epubWebViewFallbackChapters.contains(_chapter.filePath);
  }

  ReadingStyle get style => widget.state.style;
  AppPalette get appPalette => widget.state.palette;
  ReaderPalette get readerPalette {
    if (style.readerBackground == ReaderBackgroundId.theme) {
      return ReaderPalette(
        id: ReaderBackgroundId.theme,
        label: '跟随主题',
        background: appPalette.background,
        text: appPalette.text,
        muted: appPalette.muted,
      );
    }
    final base = style.readerPalette;
    final isDark =
        ThemeData.estimateBrightnessForColor(base.background) ==
        Brightness.dark;
    return ReaderPalette(
      id: base.id,
      label: base.label,
      background: Color.lerp(
        base.background,
        appPalette.primary,
        isDark ? .10 : .07,
      )!,
      text: Color.lerp(base.text, appPalette.text, .08)!,
      muted: Color.lerp(base.muted, appPalette.primary, .16)!,
    );
  }

  int get _lastChapterIndex =>
      book.chapters.isEmpty ? 0 : book.chapters.length - 1;

  ReaderChapter get _chapter {
    if (book.chapters.isEmpty) {
      return const ReaderChapter(title: '正文', href: '', filePath: '');
    }
    return book.chapters[_chapterIndex.clamp(0, _lastChapterIndex)];
  }

  @override
  void initState() {
    super.initState();
    _chromeAnimation = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 260),
      value: 1,
    );
    _tocAnimation = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 260),
    );
    _settingsAnimation = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 260),
    );
    _footerAnimation = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 240),
    );
    _chapterIndex = widget.book.currentChapterIndex.clamp(0, _lastChapterIndex);
    final savedPageCount = widget.book.pageCount < 1
        ? 1
        : widget.book.pageCount;
    _page = widget.book.currentPage.clamp(0, savedPageCount - 1);
    _pageCount = savedPageCount;
    _txtPageController = PageController(initialPage: _page);
    _pendingExactPage = _page;
    _pendingExactPageCount = savedPageCount;
    _pendingPageProgress = savedPageCount <= 1
        ? 0
        : _page / (savedPageCount - 1);
    widget.state.settingsChanges.addListener(_onReaderStyleChanged);
    _readingStopwatch.start();
    _readingTimer = Timer.periodic(
      const Duration(seconds: 30),
      (_) => _flushReadingTime(),
    );
    _clockTimer = Timer.periodic(const Duration(minutes: 1), (_) {
      if (mounted) {
        setState(() => _now = DateTime.now());
      }
    });
  }

  @override
  void dispose() {
    _flushReadingTime();
    widget.state.settingsChanges.removeListener(_onReaderStyleChanged);
    _readingTimer?.cancel();
    _styleInjectTimer?.cancel();
    _txtPaginationTimer?.cancel();
    _clockTimer?.cancel();
    _txtPageController.dispose();
    _readerSnapshotImage?.evict();
    _controller = null;
    _chromeAnimation.dispose();
    _tocAnimation.dispose();
    _settingsAnimation.dispose();
    _footerAnimation.dispose();
    widget.state.refreshLibraryViews();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final chapter = _chapter;
    return AnimatedBuilder(
      animation: widget.state.readerChanges,
      builder: (context, _) {
        final systemPadding = MediaQuery.viewPaddingOf(context);
        return PopScope(
          canPop: !_isSideOverlay(_overlay),
          onPopInvokedWithResult: (didPop, _) {
            if (!didPop && _isSideOverlay(_overlay)) {
              _hideOverlay();
            }
          },
          child: Scaffold(
            backgroundColor: readerPalette.background,
            body: Stack(
              children: [
                Positioned.fill(
                  child: _usesFlutterTxt
                      ? _buildFlutterTxtContent(chapter, systemPadding)
                      : chapter.filePath.isEmpty
                      ? _MissingChapter(readerPalette: readerPalette)
                      : Opacity(
                          opacity: _isLoading || _readerSnapshotImage != null
                              ? 0
                              : 1,
                          child: InAppWebView(
                            key: ValueKey('reader-${book.id}'),
                            initialUrlRequest: URLRequest(
                              url: WebUri.uri(File(chapter.filePath).uri),
                            ),
                            initialSettings: InAppWebViewSettings(
                              javaScriptEnabled: true,
                              transparentBackground: false,
                              useHybridComposition: true,
                              useShouldOverrideUrlLoading: true,
                              allowFileAccess: true,
                              allowFileAccessFromFileURLs: true,
                              allowUniversalAccessFromFileURLs: true,
                              disableVerticalScroll: true,
                              disableHorizontalScroll: true,
                              supportZoom: false,
                            ),
                            onWebViewCreated: (controller) {
                              _controller = controller;
                              controller.addJavaScriptHandler(
                                handlerName: 'squartorEvent',
                                callback: _handleReaderEvent,
                              );
                            },
                            onLoadStop: (controller, url) async {
                              _syncChapterFromUrl(url);
                              if (url?.fragment.isNotEmpty == true) {
                                _pendingAnchor = Uri.decodeComponent(
                                  url!.fragment,
                                );
                              }
                              try {
                                await _injectReaderStyle();
                              } catch (error) {
                                debugPrint(
                                  'SQuartor inject style failed: $error',
                                );
                              }
                              if (mounted) {
                                setState(() {
                                  _isLoading = false;
                                  _loadError = null;
                                });
                              }
                            },
                            onReceivedError: (controller, request, error) {
                              if (request.isForMainFrame == true && mounted) {
                                setState(() {
                                  _isLoading = false;
                                  _loadError = error.description;
                                  _overlay = _ReaderOverlay.chrome;
                                });
                              }
                            },
                            onConsoleMessage: (controller, message) {
                              debugPrint(
                                'SQuartor WebView: ${message.message}',
                              );
                            },
                            shouldOverrideUrlLoading:
                                (controller, action) async {
                                  final url = action.request.url;
                                  if (url == null) {
                                    return NavigationActionPolicy.ALLOW;
                                  }
                                  if (_isExternalUriString(url.toString())) {
                                    unawaited(
                                      _openExternalLink(
                                        Uri.parse(url.toString()),
                                      ),
                                    );
                                    if (context.mounted &&
                                        !_isExternalUriString(url.toString())) {
                                      ScaffoldMessenger.of(
                                        context,
                                      ).showSnackBar(
                                        const SnackBar(
                                          content: Text('外部链接第一版先不打开'),
                                        ),
                                      );
                                    }
                                    return NavigationActionPolicy.CANCEL;
                                  }
                                  return NavigationActionPolicy.ALLOW;
                                },
                          ),
                        ),
                ),
                if (_readerSnapshotImage case final image?)
                  Positioned.fill(
                    child: Image(
                      image: image,
                      fit: BoxFit.cover,
                      filterQuality: FilterQuality.low,
                    ),
                  ),
                if (!_usesFlutterTxt)
                  Positioned.fill(
                    child: GestureDetector(
                      behavior: HitTestBehavior.translucent,
                      onTapUp: _onReaderTap,
                      onLongPressStart: _onReaderLongPress,
                      onHorizontalDragStart: _onReaderDragStart,
                      onHorizontalDragUpdate: _onReaderDragUpdate,
                      onHorizontalDragEnd: _onReaderDragEnd,
                      onHorizontalDragCancel: _onReaderDragCancel,
                    ),
                  ),
                if (_isLoading)
                  Positioned(
                    left: 0,
                    right: 0,
                    top: 0,
                    child: LinearProgressIndicator(
                      minHeight: 2,
                      backgroundColor: Colors.transparent,
                      valueColor: AlwaysStoppedAnimation(
                        appPalette.primarySoft,
                      ),
                    ),
                  ),
                if (_loadError != null)
                  Positioned.fill(
                    child: _ReaderStatusOverlay(
                      readerPalette: readerPalette,
                      message: '加载失败：$_loadError',
                    ),
                  ),
                Positioned(
                  left: 20,
                  right: 20,
                  top: systemPadding.top + 16,
                  child: IgnorePointer(
                    ignoring: _overlay != _ReaderOverlay.chrome,
                    child: AnimatedBuilder(
                      animation: _chromeAnimation,
                      builder: (context, child) {
                        final t = _overlaySlideProgress(_chromeAnimation);
                        return FractionalTranslation(
                          translation: Offset(0, -2.35 * (1 - t)),
                          child: child,
                        );
                      },
                      child: _TopMenu(
                        title: chapter.title,
                        palette: readerPalette,
                        appPalette: appPalette,
                        onBack: () => Navigator.of(context).pop(),
                      ),
                    ),
                  ),
                ),
                Positioned(
                  left: 16,
                  right: 16,
                  bottom: systemPadding.bottom + 8,
                  child: IgnorePointer(
                    child: AnimatedBuilder(
                      animation: _footerAnimation,
                      builder: (context, child) {
                        return Opacity(
                          opacity: Curves.easeOut.transform(
                            _footerAnimation.value,
                          ),
                          child: child,
                        );
                      },
                      child: _ReaderFooter(
                        now: _now,
                        chapter: _chapterIndex + 1,
                        chapterCount: book.chapters.length,
                        page: _page + 1,
                        pageCount: _pageCount,
                        progress: _overallProgress,
                        palette: readerPalette,
                      ),
                    ),
                  ),
                ),
                Positioned(
                  left: 16,
                  right: 16,
                  bottom: systemPadding.bottom + 18,
                  child: IgnorePointer(
                    ignoring: _overlay != _ReaderOverlay.chrome,
                    child: AnimatedBuilder(
                      animation: _chromeAnimation,
                      builder: (context, child) {
                        final t = _overlaySlideProgress(_chromeAnimation);
                        return FractionalTranslation(
                          translation: Offset(0, 2.1 * (1 - t)),
                          child: child,
                        );
                      },
                      child: _AdaptiveBottomMenu(
                        page: _page,
                        pageCount: _pageCount,
                        progress: _overallProgress,
                        readerPalette: readerPalette,
                        appPalette: appPalette,
                        onToc: _showToc,
                        onPreviousChapter: () =>
                            _goToChapter(_chapterIndex - 1, atEnd: true),
                        onNextChapter: () => _goToChapter(_chapterIndex + 1),
                        onSettings: _showSettings,
                      ),
                    ),
                  ),
                ),
                _ReaderPanelScrim(
                  visible:
                      _overlay == _ReaderOverlay.toc ||
                      _overlay == _ReaderOverlay.settings,
                  palette: appPalette,
                  onDismiss: _hideOverlay,
                ),
                Positioned.fill(
                  child: IgnorePointer(
                    ignoring: _overlay != _ReaderOverlay.toc,
                    child: LayoutBuilder(
                      builder: (context, constraints) {
                        final panelWidth =
                            (constraints.maxWidth -
                                    systemPadding.left -
                                    systemPadding.right -
                                    36)
                                .clamp(300.0, 560.0)
                                .toDouble();
                        final availableHeight =
                            (constraints.maxHeight -
                                    systemPadding.top -
                                    systemPadding.bottom)
                                .clamp(360.0, double.infinity)
                                .toDouble();
                        final panelHeight = (availableHeight * .76)
                            .clamp(420.0, 760.0)
                            .toDouble();
                        return AnimatedBuilder(
                          animation: _tocAnimation,
                          builder: (context, child) {
                            final raw = _tocAnimation.value;
                            if (raw <= 0.001 &&
                                _overlay != _ReaderOverlay.toc) {
                              return const SizedBox.shrink();
                            }
                            final t = _overlaySlideProgress(_tocAnimation);
                            final travel =
                                (constraints.maxWidth + panelWidth) / 2 + 32;
                            return Transform.translate(
                              offset: Offset(-travel * (1 - t), 0),
                              child: child,
                            );
                          },
                          child: Align(
                            alignment: Alignment.bottomCenter,
                            child: Padding(
                              padding: EdgeInsets.fromLTRB(
                                18 + systemPadding.left,
                                18 + systemPadding.top,
                                18 + systemPadding.right,
                                14 + systemPadding.bottom,
                              ),
                              child: SizedBox(
                                width: panelWidth,
                                height: panelHeight,
                                child: RepaintBoundary(
                                  child: _ReaderTocDrawer(
                                    key: ValueKey('toc-$_chapterIndex'),
                                    book: book,
                                    chapterIndex: _chapterIndex,
                                    currentPageCount: _pageCount,
                                    cachedPageCounts: const <int, int>{},
                                    palette: appPalette,
                                    onClose: _hideOverlay,
                                    onChapterSelected: (index) {
                                      _hideOverlay();
                                      _goToChapter(index);
                                    },
                                  ),
                                ),
                              ),
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                ),
                Positioned.fill(
                  child: IgnorePointer(
                    ignoring: _overlay != _ReaderOverlay.settings,
                    child: LayoutBuilder(
                      builder: (context, constraints) {
                        final panelWidth =
                            (constraints.maxWidth -
                                    systemPadding.left -
                                    systemPadding.right -
                                    36)
                                .clamp(300.0, 560.0)
                                .toDouble();
                        final availableHeight =
                            (constraints.maxHeight -
                                    systemPadding.top -
                                    systemPadding.bottom)
                                .clamp(360.0, double.infinity)
                                .toDouble();
                        final panelHeight = (availableHeight * .76)
                            .clamp(420.0, 760.0)
                            .toDouble();
                        return AnimatedBuilder(
                          animation: _settingsAnimation,
                          builder: (context, child) {
                            final raw = _settingsAnimation.value;
                            if (raw <= 0.001 &&
                                _overlay != _ReaderOverlay.settings) {
                              return const SizedBox.shrink();
                            }
                            final t = _overlaySlideProgress(_settingsAnimation);
                            final travel =
                                (constraints.maxWidth + panelWidth) / 2 + 32;
                            return Transform.translate(
                              offset: Offset(travel * (1 - t), 0),
                              child: child,
                            );
                          },
                          child: Align(
                            alignment: Alignment.bottomCenter,
                            child: Padding(
                              padding: EdgeInsets.fromLTRB(
                                18 + systemPadding.left,
                                24 + systemPadding.top,
                                18 + systemPadding.right,
                                24 + systemPadding.bottom,
                              ),
                              child: SizedBox(
                                width: panelWidth,
                                height: panelHeight,
                                child: RepaintBoundary(
                                  child: _FloatingPanelSurface(
                                    palette: appPalette,
                                    child: _ReaderSettingsSheet(
                                      state: widget.state,
                                      onClose: _hideOverlay,
                                      onChanged: _scheduleStyleInjection,
                                    ),
                                  ),
                                ),
                              ),
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildFlutterTxtContent(
    ReaderChapter chapter,
    EdgeInsets systemPadding,
  ) {
    if (chapter.filePath.isEmpty) {
      return _MissingChapter(readerPalette: readerPalette);
    }
    return LayoutBuilder(
      builder: (context, constraints) {
        final metrics = _resolveTxtLayoutMetrics(
          constraints: constraints,
          systemPadding: systemPadding,
        );
        _requestTxtPagination(metrics);
        if (_txtPages.isEmpty) {
          return ColoredBox(color: readerPalette.background);
        }
        return _FlutterTxtReaderView(
          pages: _txtPages,
          controller: _txtPageController,
          metrics: metrics,
          readerPalette: readerPalette,
          style: style,
          fontFamily: _flutterReaderFontFamily,
          linkColor: appPalette.primarySoft,
          onTapUp: _onReaderTap,
          onPageChanged: _onTxtPageChanged,
          onEdgePrevious: _previousTxtPage,
          onEdgeNext: _nextTxtPage,
          onLinkTap: _openInternalLink,
        );
      },
    );
  }

  double get _overallProgress {
    if (book.chapters.isEmpty) {
      return 0;
    }
    final chapterPart = _chapterIndex / book.chapters.length;
    final safePageCount = _pageCount < 1 ? 1 : _pageCount;
    final pagePart = (_page / safePageCount) / book.chapters.length;
    return (chapterPart + pagePart).clamp(0.0, 1.0);
  }

  _TxtLayoutMetrics _resolveTxtLayoutMetrics({
    required BoxConstraints constraints,
    required EdgeInsets systemPadding,
  }) {
    final horizontalMargin = style.pageMargin.clamp(0.0, double.infinity);
    final pageOuterInset = horizontalMargin <= 0.5
        ? 0.0
        : (horizontalMargin * .45).clamp(0.0, 26.0).toDouble();
    final effectiveFontSize = _effectiveReaderFontSize(style);
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
    final top = style.verticalMargin + systemPadding.top;
    final bottom = style.verticalMargin + systemPadding.bottom;
    final width = (constraints.maxWidth - pageOuterInset * 2 - left - right)
        .clamp(160.0, double.infinity)
        .toDouble();
    final rawHeight = (constraints.maxHeight - top - bottom)
        .clamp(lineHeightPx * 4, double.infinity)
        .toDouble();
    final height = (rawHeight / lineHeightPx).floor() * lineHeightPx;
    return _TxtLayoutMetrics(
      padding: EdgeInsets.fromLTRB(left, top, right, bottom),
      pageOuterInset: pageOuterInset,
      contentWidth: width,
      contentHeight: height,
    );
  }

  double _effectiveReaderFontSize(ReadingStyle style) {
    return (style.fontSize * 1.12).clamp(16.0, 38.0).toDouble();
  }

  void _requestTxtPagination(_TxtLayoutMetrics metrics) {
    _txtLayoutMetrics = metrics;
    if (!_usesFlutterTxt) {
      return;
    }
    final signature = _buildTxtPaginationSignature(metrics);
    if (signature == _txtPaginationSignature ||
        signature == _txtRequestedSignature) {
      return;
    }
    _txtRequestedSignature = signature;
    _txtPaginationTimer?.cancel();
    _txtPaginationTimer = Timer(const Duration(milliseconds: 20), () {
      unawaited(_loadAndPaginateTxt(metrics, signature));
    });
  }

  String _buildTxtPaginationSignature(_TxtLayoutMetrics metrics) {
    final chapter = _chapter;
    final anchor = book.format == BookFormat.epub
        ? (_pendingAnchor ?? chapter.anchor ?? '')
        : '';
    return [
      book.format.name,
      chapter.filePath,
      anchor,
      metrics.contentWidth.toStringAsFixed(1),
      metrics.contentHeight.toStringAsFixed(1),
      metrics.pageOuterInset.toStringAsFixed(1),
      style.fontSize.toStringAsFixed(2),
      style.lineHeight.toStringAsFixed(3),
      style.paragraphSpacing.toStringAsFixed(2),
      style.letterSpacing.toStringAsFixed(2),
      style.pageMargin.toStringAsFixed(1),
      style.verticalMargin.toStringAsFixed(1),
      style.firstLineIndent ? 'indent' : 'plain',
      style.fontPath ?? '',
    ].join('|');
  }

  Future<void> _loadAndPaginateTxt(
    _TxtLayoutMetrics metrics,
    String signature,
  ) async {
    if (!_usesFlutterTxt || book.chapters.isEmpty) {
      return;
    }
    if (mounted && !_isLoading) {
      setState(() {
        _isLoading = true;
        _loadError = null;
      });
    }
    try {
      final fontFamily = await _registerReaderFont(style.fontPath);
      final chapter = _chapter;
      final anchor = book.format == BookFormat.epub
          ? (_pendingAnchor ?? chapter.anchor)
          : null;
      final document = book.format == BookFormat.epub
          ? await _readFlutterEpubDocument(chapter, anchor: anchor)
          : await _readFlutterTxtDocument(chapter);
      final pages = _paginateFlutterTxt(
        document: document,
        metrics: metrics,
        style: style,
        fontFamily: fontFamily,
      );
      if (!mounted || signature != _txtRequestedSignature) {
        return;
      }
      final safePageCount = pages.isEmpty ? 1 : pages.length;
      final exactPage = _pendingExactPage;
      final exactPageCount = _pendingExactPageCount;
      final restoredProgress =
          _pendingPageProgress ??
          (_pageCount <= 1 ? 0.0 : _page / (_pageCount - 1));
      _pendingPageProgress = null;
      _pendingExactPage = null;
      _pendingExactPageCount = null;
      final targetPage = exactPage != null && exactPageCount == safePageCount
          ? exactPage.clamp(0, safePageCount - 1)
          : (restoredProgress * (safePageCount - 1)).round().clamp(
              0,
              safePageCount - 1,
            );
      final oldController = _txtPageController;
      _txtPageController = PageController(initialPage: targetPage);
      WidgetsBinding.instance.addPostFrameCallback((_) {
        oldController.dispose();
      });
      setState(() {
        _flutterReaderFontFamily = fontFamily;
        _txtPages = pages.isEmpty ? [_FlutterTxtPage.empty()] : pages;
        _txtPaginationSignature = signature;
        _txtRequestedSignature = null;
        _pendingAnchor = null;
        _page = targetPage;
        _pageCount = safePageCount;
        _isLoading = false;
        _loadError = null;
      });
      unawaited(
        widget.state.updateBookProgress(
          book: book,
          chapterIndex: _chapterIndex,
          page: targetPage,
          pageCount: safePageCount,
        ),
      );
    } on _EpubWebViewFallbackException {
      if (!mounted || signature != _txtRequestedSignature) {
        return;
      }
      _epubWebViewFallbackChapters.add(_chapter.filePath);
      setState(() {
        _txtRequestedSignature = null;
        _txtPaginationSignature = null;
        _txtPages = const [];
        _isLoading = true;
        _loadError = null;
      });
    } catch (error) {
      if (!mounted || signature != _txtRequestedSignature) {
        return;
      }
      setState(() {
        _txtRequestedSignature = null;
        _isLoading = false;
        _loadError = error.toString();
        _overlay = _ReaderOverlay.chrome;
      });
    }
  }

  Future<_FlutterTxtDocument> _readFlutterEpubDocument(
    ReaderChapter chapter, {
    String? anchor,
  }) async {
    final raw = await File(chapter.filePath).readAsString();
    final document = html_parser.parse(raw);
    final body = document.body;
    if (body == null ||
        (!body.classes.contains('sq-document-flow') &&
            !body.classes.contains('sq-document-image-only'))) {
      throw const _EpubWebViewFallbackException();
    }
    final elements = _epubContentElements(body);
    final readableElements = elements
        .where(
          (element) => _cleanReaderText(_extractEpubText(element)).isNotEmpty,
        )
        .toList();
    if (elements.isEmpty) {
      throw const _EpubWebViewFallbackException();
    }
    if (_shouldFallbackForComplexEpubContent(body, readableElements)) {
      throw const _EpubWebViewFallbackException();
    }

    final start = _epubAnchorStartIndex(elements, anchor);
    final visibleElements = elements.skip(start).toList();
    if (visibleElements.isEmpty) {
      throw const _EpubWebViewFallbackException();
    }

    final heading = visibleElements
        .where((element) => _isHeadingElement(element))
        .map((element) => _cleanReaderText(_extractEpubText(element)))
        .where((text) => text.isNotEmpty)
        .firstOrNull;
    final documentTitle = _cleanReaderText(
      document.querySelector('title')?.text ?? '',
    );
    final title = (heading?.isNotEmpty == true
        ? heading!
        : documentTitle.isNotEmpty
        ? documentTitle
        : chapter.title.trim());
    final blocks = <_FlutterDocumentBlock>[];
    for (final element in visibleElements) {
      final links = _extractEpubLinks(element);
      if (links.isNotEmpty) {
        final surroundingText = _cleanReaderText(
          _extractEpubText(element, skipLinks: true),
        );
        if (surroundingText.isNotEmpty) {
          if (blocks.isEmpty && _sameReaderTitle(surroundingText, title)) {
            continue;
          }
          blocks.add(_FlutterDocumentBlock.paragraph(surroundingText));
        }
        for (final link in links) {
          if (blocks.isEmpty && _sameReaderTitle(link.text, title)) {
            continue;
          }
          blocks.add(_FlutterDocumentBlock.link(link.text, link.href));
        }
      } else {
        final text = _cleanReaderText(_extractEpubText(element));
        if (text.isNotEmpty) {
          if (blocks.isEmpty && _sameReaderTitle(text, title)) {
            continue;
          }
          blocks.add(_FlutterDocumentBlock.paragraph(text));
        }
      }
      for (final source in _extractEpubImageSources(element)) {
        blocks.add(_FlutterDocumentBlock.image(source));
      }
    }
    if (title.trim().isEmpty && blocks.isEmpty) {
      throw const _EpubWebViewFallbackException();
    }
    return _FlutterTxtDocument(title: title, blocks: blocks);
  }

  List<_EpubLinkBlock> _extractEpubLinks(dom.Element element) {
    final links = <_EpubLinkBlock>[];
    for (final link in element.querySelectorAll('a[href]')) {
      final href = link.attributes['href'];
      final text = _cleanReaderText(_extractEpubText(link));
      if (href == null || href.isEmpty || text.isEmpty) {
        continue;
      }
      links.add(_EpubLinkBlock(text: text, href: href));
    }
    return links;
  }

  String _extractEpubText(dom.Node node, {bool skipLinks = false}) {
    if (node is dom.Text) {
      return node.data;
    }
    if (node is! dom.Element) {
      return '';
    }
    if (_isHiddenEpubMarker(node)) {
      return '';
    }
    final tag = node.localName?.toLowerCase() ?? '';
    if (const {
      'script',
      'style',
      'noscript',
      'template',
      'rp',
      'rt',
    }.contains(tag)) {
      return '';
    }
    if (skipLinks && tag == 'a') {
      return '';
    }
    if (tag == 'br') {
      return '\n';
    }
    return node.nodes
        .map((child) => _extractEpubText(child, skipLinks: skipLinks))
        .join();
  }

  List<dom.Element> _epubContentElements(dom.Element body) {
    const primarySelector = 'h1, h2, h3, h4, h5, h6, p, li, blockquote';
    const mediaSelector = '.sq-media';
    final primary = body
        .querySelectorAll('$primarySelector, $mediaSelector')
        .where((element) => !_isHiddenEpubMarker(element))
        .where(
          (element) =>
              _cleanReaderText(_extractEpubText(element)).isNotEmpty ||
              _extractEpubImageSources(element).isNotEmpty,
        )
        .toList();
    if (primary.isNotEmpty) {
      return primary;
    }
    return body
        .querySelectorAll('div, section, article')
        .where((element) => !_isHiddenEpubMarker(element))
        .where(
          (element) =>
              element.querySelector(primarySelector) == null &&
              element.querySelector('div, section, article') == null,
        )
        .where(
          (element) =>
              _cleanReaderText(_extractEpubText(element)).isNotEmpty ||
              _extractEpubImageSources(element).isNotEmpty,
        )
        .toList();
  }

  List<String> _extractEpubImageSources(dom.Element element) {
    final result = <String>[];
    if (element.localName?.toLowerCase() == 'img') {
      final source = element.attributes['src'];
      if (source != null && source.isNotEmpty) {
        result.add(source);
      }
    }
    for (final image in element.querySelectorAll('img')) {
      final source = image.attributes['src'];
      if (source != null && source.isNotEmpty) {
        result.add(source);
      }
    }
    return result.toSet().toList(growable: false);
  }

  bool _shouldFallbackForComplexEpubContent(
    dom.Element body,
    List<dom.Element> readableElements,
  ) {
    final hasInteractiveOrMath =
        body.querySelector('video, audio, iframe, canvas, math') != null;
    if (!hasInteractiveOrMath) {
      return false;
    }
    final textLength = readableElements.fold<int>(
      0,
      (total, element) =>
          total + _compactReaderText(_extractEpubText(element)).length,
    );
    return textLength < 240;
  }

  bool _isHiddenEpubMarker(dom.Element element) {
    return element.classes.contains('sq-spine-marker') ||
        element.attributes.containsKey('hidden');
  }

  String _compactReaderText(String input) {
    return _cleanReaderText(input).replaceAll(RegExp(r'\s+'), '');
  }

  bool _isHeadingElement(dom.Element element) {
    final tag = element.localName?.toLowerCase() ?? '';
    return const {'h1', 'h2', 'h3', 'h4', 'h5', 'h6'}.contains(tag);
  }

  int _epubAnchorStartIndex(List<dom.Element> elements, String? anchor) {
    if (anchor == null || anchor.isEmpty) {
      return 0;
    }
    for (var i = 0; i < elements.length; i++) {
      if (_elementContainsAnchor(elements[i], anchor)) {
        return i;
      }
    }
    return 0;
  }

  bool _elementContainsAnchor(dom.Element element, String anchor) {
    if (element.id == anchor || element.attributes['name'] == anchor) {
      return true;
    }
    return element
        .querySelectorAll('[id], [name]')
        .any(
          (child) => child.id == anchor || child.attributes['name'] == anchor,
        );
  }

  String _cleanReaderText(String input) {
    return input
        .replaceAll('\u00a0', ' ')
        .replaceAll(RegExp(r'\s+'), ' ')
        .trim();
  }

  bool _sameReaderTitle(String a, String b) {
    String normalize(String value) {
      return value
          .replaceAll(RegExp(r'\s+'), '')
          .replaceAll(RegExp(r'[《》「」『』【】\\[\\]()（）:：,，.。!！?？\\-—_]+'), '')
          .toLowerCase();
    }

    final left = normalize(a);
    final right = normalize(b);
    return left.isNotEmpty && right.isNotEmpty && left == right;
  }

  Future<_FlutterTxtDocument> _readFlutterTxtDocument(
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
    return _FlutterTxtDocument(
      title: title,
      blocks: [
        for (final paragraph in paragraphs)
          _FlutterDocumentBlock.paragraph(paragraph),
      ],
    );
  }

  List<_FlutterTxtPage> _paginateFlutterTxt({
    required _FlutterTxtDocument document,
    required _TxtLayoutMetrics metrics,
    required ReadingStyle style,
    required String? fontFamily,
  }) {
    final effectiveFontSize = _effectiveReaderFontSize(style);
    final paragraphStyle = TextStyle(
      fontFamily: fontFamily,
      fontSize: effectiveFontSize,
      height: style.lineHeight,
      letterSpacing: style.letterSpacing,
      color: readerPalette.text,
      fontWeight: AppTextWeight.regular,
    );
    final titleStyle = TextStyle(
      fontFamily: fontFamily,
      fontSize: effectiveFontSize * 1.45,
      height: 1.35,
      letterSpacing: style.letterSpacing,
      color: readerPalette.text,
      fontWeight: AppTextWeight.semibold,
    );
    final linkStyle = TextStyle(
      fontFamily: fontFamily,
      fontSize: effectiveFontSize,
      height: style.lineHeight,
      letterSpacing: style.letterSpacing,
      color: appPalette.primarySoft,
      fontWeight: AppTextWeight.medium,
      decoration: TextDecoration.underline,
      decorationColor: appPalette.primarySoft,
    );
    final pages = <_FlutterTxtPage>[];
    var blocks = <_FlutterTxtBlock>[];
    var usedHeight = 0.0;
    final pageHeight = metrics.contentHeight;
    final pageWidth = metrics.contentWidth;
    final paragraphSpacing = style.paragraphSpacing;
    final minLineHeight = effectiveFontSize * style.lineHeight;

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
      pages.add(_FlutterTxtPage(blocks: blocks));
      blocks = <_FlutterTxtBlock>[];
      usedHeight = 0;
    }

    void ensureSpace(double neededHeight) {
      if (blocks.isNotEmpty && usedHeight + neededHeight > pageHeight) {
        finishPage();
      }
    }

    void addBlock(_FlutterTxtBlock block, double textHeight) {
      blocks.add(block);
      usedHeight += textHeight + block.bottomSpacing;
    }

    void addImageBlock(String source) {
      final imageHeight = (pageWidth * 1.38)
          .clamp(minLineHeight * 5, pageHeight)
          .toDouble();
      ensureSpace(imageHeight);
      final spacing = usedHeight + imageHeight + paragraphSpacing <= pageHeight
          ? paragraphSpacing
          : 0.0;
      addBlock(
        _FlutterTxtBlock(
          text: '',
          kind: _FlutterTxtBlockKind.image,
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
        _FlutterTxtBlock(
          text: normalizedTitle,
          kind: _FlutterTxtBlockKind.title,
          bottomSpacing: titleSpacing,
        ),
        titleHeight,
      );
    }

    for (final sourceBlock in document.blocks) {
      if (sourceBlock.kind == _FlutterDocumentBlockKind.image) {
        final imageSource = sourceBlock.imageSource;
        if (imageSource != null && imageSource.isNotEmpty) {
          addImageBlock(imageSource);
        }
        continue;
      }
      final isLink = sourceBlock.kind == _FlutterDocumentBlockKind.link;
      final textStyle = isLink ? linkStyle : paragraphStyle;
      final blockKind = isLink
          ? _FlutterTxtBlockKind.link
          : _FlutterTxtBlockKind.paragraph;
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
            !isLink && firstFragment && style.firstLineIndent;
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
            _FlutterTxtBlock(
              text: remaining,
              kind: blockKind,
              bottomSpacing: spacing,
              firstLineIndent: indentFirstLine,
              href: sourceBlock.href,
            ),
            fullHeight,
          );
          offset = chars.length;
          continue;
        }

        var fitCount = _fitTxtFragmentCount(
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
          fitCount = _preferTxtBreak(chars, offset, fitCount);
        }
        final fragment = chars.sublist(offset, offset + fitCount).join();
        final measuredFragment = '$prefix$fragment';
        addBlock(
          _FlutterTxtBlock(
            text: fragment,
            kind: blockKind,
            bottomSpacing: 0,
            firstLineIndent: indentFirstLine,
            href: sourceBlock.href,
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

  int _fitTxtFragmentCount({
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

  int _preferTxtBreak(List<String> chars, int start, int count) {
    if (count <= 8) {
      return count;
    }
    final end = start + count;
    final searchStart = (end - 36).clamp(start + 1, end - 1);
    for (var i = end - 1; i >= searchStart; i--) {
      if (RegExp(r'[\s，。！？、；：,.!?:;」』）)]').hasMatch(chars[i])) {
        return i - start + 1;
      }
    }
    return count;
  }

  void _onTxtPageChanged(int page) {
    final safePageCount = _txtPages.isEmpty ? 1 : _txtPages.length;
    final safePage = page.clamp(0, safePageCount - 1);
    if (_page != safePage || _pageCount != safePageCount) {
      setState(() {
        _page = safePage;
        _pageCount = safePageCount;
      });
    }
    unawaited(
      widget.state.updateBookProgress(
        book: book,
        chapterIndex: _chapterIndex,
        page: safePage,
        pageCount: safePageCount,
      ),
    );
  }

  Future<String?> _registerReaderFont(String? fontPath) async {
    if (fontPath == null || fontPath.isEmpty) {
      return null;
    }
    try {
      final file = File(fontPath);
      if (!await file.exists()) {
        return null;
      }
      final family = _readerFontFamilyForPath(fontPath);
      if (_registeredReaderFontFamilies.add(family)) {
        final loader = FontLoader(family);
        loader.addFont(
          file.readAsBytes().then(
            (bytes) => ByteData.view(
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

  String _readerFontFamilyForPath(String fontPath) {
    var hash = 0x811C9DC5;
    for (final codeUnit in fontPath.codeUnits) {
      hash ^= codeUnit;
      hash = (hash * 0x01000193) & 0xFFFFFFFF;
    }
    return 'SQuartorReaderFont_${hash.toRadixString(16)}';
  }

  double _overlaySlideProgress(AnimationController controller) {
    final value = controller.value.clamp(0.0, 1.0);
    if (controller.status == AnimationStatus.reverse) {
      return Curves.easeInCubic.transform(value);
    }
    return Curves.easeOutCubic.transform(value);
  }

  bool _isSideOverlay(_ReaderOverlay overlay) {
    return overlay == _ReaderOverlay.toc || overlay == _ReaderOverlay.settings;
  }

  Future<void> _freezeReaderForSideOverlay(int serial) async {
    if (_usesFlutterTxt) {
      return;
    }
    if (_readerSnapshotImage != null || _isLoading) {
      return;
    }
    final controller = _controller;
    if (controller == null) {
      return;
    }
    try {
      final bytes = await controller.takeScreenshot(
        screenshotConfiguration: ScreenshotConfiguration(
          snapshotWidth: MediaQuery.sizeOf(context).width,
          compressFormat: CompressFormat.PNG,
          quality: 100,
        ),
      );
      if (!mounted ||
          serial != _overlayTransitionSerial ||
          bytes == null ||
          bytes.isEmpty) {
        return;
      }
      final image = MemoryImage(bytes);
      await precacheImage(image, context);
      if (!mounted || serial != _overlayTransitionSerial) {
        image.evict();
        return;
      }
      setState(() {
        _readerSnapshotImage?.evict();
        _readerSnapshotBytes = bytes;
        _readerSnapshotImage = image;
      });
    } catch (error) {
      debugPrint('SQuartor reader snapshot failed: $error');
    }
  }

  Future<void> _refreshFrozenReaderSnapshot() async {
    if (_usesFlutterTxt) {
      return;
    }
    final serial = _overlayTransitionSerial;
    if (!mounted ||
        _overlay != _ReaderOverlay.settings ||
        _readerSnapshotImage == null ||
        _isLoading) {
      return;
    }
    await Future<void>.delayed(const Duration(milliseconds: 90));
    if (!mounted ||
        serial != _overlayTransitionSerial ||
        _overlay != _ReaderOverlay.settings) {
      return;
    }
    final controller = _controller;
    if (controller == null) {
      return;
    }
    try {
      final bytes = await controller.takeScreenshot(
        screenshotConfiguration: ScreenshotConfiguration(
          snapshotWidth: MediaQuery.sizeOf(context).width,
          compressFormat: CompressFormat.PNG,
          quality: 100,
        ),
      );
      if (!mounted ||
          serial != _overlayTransitionSerial ||
          _overlay != _ReaderOverlay.settings ||
          bytes == null ||
          bytes.isEmpty) {
        return;
      }
      final image = MemoryImage(bytes);
      await precacheImage(image, context);
      if (!mounted ||
          serial != _overlayTransitionSerial ||
          _overlay != _ReaderOverlay.settings) {
        image.evict();
        return;
      }
      final oldImage = _readerSnapshotImage;
      setState(() {
        _readerSnapshotBytes = bytes;
        _readerSnapshotImage = image;
      });
      oldImage?.evict();
    } catch (error) {
      debugPrint('SQuartor reader snapshot refresh failed: $error');
    }
  }

  void _clearReaderSnapshot({bool notify = true}) {
    final image = _readerSnapshotImage;
    if (image == null && _readerSnapshotBytes == null) {
      return;
    }
    image?.evict();
    if (notify && mounted) {
      setState(() {
        _readerSnapshotImage = null;
        _readerSnapshotBytes = null;
      });
    } else {
      _readerSnapshotImage = null;
      _readerSnapshotBytes = null;
    }
  }

  void _onReaderStyleChanged() {
    if (!_usesFlutterTxt || !mounted) {
      return;
    }
    _txtPaginationSignature = null;
    _txtRequestedSignature = null;
    final metrics = _txtLayoutMetrics;
    if (metrics != null) {
      _requestTxtPagination(metrics);
    }
    setState(() {});
  }

  dynamic _handleReaderEvent(List<dynamic> arguments) {
    if (!mounted) {
      return null;
    }
    final raw = arguments.isEmpty ? null : arguments.first;
    if (raw is! Map) {
      return null;
    }
    final type = raw['type'];
    if (type == 'progress') {
      final page = (raw['page'] as num?)?.toInt() ?? 0;
      final pages = (raw['pages'] as num?)?.toInt() ?? 1;
      final safePages = pages < 1 ? 1 : pages;
      final safePage = page.clamp(0, safePages - 1);
      if (mounted) {
        if (_page != safePage || _pageCount != safePages) {
          setState(() {
            _page = safePage;
            _pageCount = safePages;
          });
        }
      }
      widget.state.updateBookProgress(
        book: book,
        chapterIndex: _chapterIndex,
        page: safePage,
        pageCount: safePages,
      );
    } else if (type == 'ready') {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _loadError = null;
        });
      }
    } else if (type == 'toggleMenu') {
      if (mounted) {
        _toggleChrome();
      }
    } else if (type == 'linkTap') {
      _linkTapHandled = true;
    } else if (type == 'internalLink') {
      _linkTapHandled = true;
      final href = raw['href'] as String?;
      if (href != null) {
        unawaited(_openInternalLink(href));
      }
    } else if (type == 'nextChapter') {
      WidgetsBinding.instance.addPostFrameCallback(
        (_) => _goToChapter(_chapterIndex + 1),
      );
    } else if (type == 'previousChapter') {
      WidgetsBinding.instance.addPostFrameCallback(
        (_) => _goToChapter(_chapterIndex - 1, atEnd: true),
      );
    }
    return null;
  }

  Future<void> _injectReaderStyle() async {
    final controller = _controller;
    if (controller == null) {
      return;
    }
    final reader = readerPalette;
    final systemPadding = MediaQuery.viewPaddingOf(context);
    final initialProgress =
        _pendingPageProgress ??
        (_pageCount <= 1 ? 0.0 : _page / (_pageCount - 1));
    _pendingPageProgress = null;
    final config = jsonEncode({
      'background': _cssColor(reader.background),
      'text': _cssColor(reader.text),
      'muted': _cssColor(reader.muted),
      'accent': _cssColor(appPalette.primarySoft),
      'fontSize': style.fontSize,
      'lineHeight': style.lineHeight,
      'paragraphSpacing': style.paragraphSpacing,
      'letterSpacing': style.letterSpacing,
      'pageMargin': style.pageMargin,
      'verticalMargin': style.verticalMargin,
      'safeTop': systemPadding.top,
      'safeBottom': systemPadding.bottom,
      'firstLineIndent': style.firstLineIndent,
      'fontName': style.fontName == null ? null : 'SQuartorCustomFont',
      'fontUri': style.fontPath == null
          ? null
          : File(style.fontPath!).uri.toString(),
      'initialProgress': initialProgress,
      'anchor': _pendingAnchor ?? _chapter.anchor,
    });
    _pendingAnchor = null;
    await controller.evaluateJavascript(source: _readerScript(config));
  }

  String _readerScript(String configJson) {
    return '''
(function() {
  const cfg = $configJson;
  if (window.SQuartor && window.SQuartor.dispose) {
    window.SQuartor.dispose();
  }
  const old = document.getElementById('squartor-style');
  if (old) old.remove();

  let root = document.getElementById('squartor-root');
  if (!root) {
    root = document.createElement('div');
    root.id = 'squartor-root';
    while (document.body.firstChild) {
      root.appendChild(document.body.firstChild);
    }
    document.body.appendChild(root);
  }

  let source = document.getElementById('squartor-source');
  if (!source) {
    source = document.createElement('div');
    source.id = 'squartor-source';
    while (root.firstChild) {
      source.appendChild(root.firstChild);
    }
    root.appendChild(source);
  }

  document.querySelectorAll('link[rel~="stylesheet"], style:not(#squartor-style)').forEach(function(node) {
    node.remove();
  });
  const originalText = (source.innerText || '').replace(/\\s+/g, '');
  const originalMedia = source.querySelectorAll('img, svg');
  const isImagePage = document.body.classList.contains('sq-document-image-only') ||
    (originalText.length < 8 && originalMedia.length === 1);

  const style = document.createElement('style');
  style.id = 'squartor-style';
  const fontFace = cfg.fontUri ? "@font-face { font-family: 'SQuartorCustomFont'; src: url('" + cfg.fontUri + "'); }" : '';
  const fontFamily = cfg.fontName ? "'SQuartorCustomFont', sans-serif" : 'system-ui, sans-serif';
  const horizontalMargin = Math.max(0, Number(cfg.pageMargin || 0));
  const pageGap = horizontalMargin * 2;
  const safeTop = cfg.verticalMargin + (cfg.safeTop || 0);
  const safeBottom = cfg.verticalMargin + (cfg.safeBottom || 0);
  const effectiveFontSize = Math.max(16, Math.min(38, cfg.fontSize * 1.12));
  
  style.textContent =
    fontFace + '\\n' +
    'html, body { background: ' + cfg.background + ' !important; width: 100vw !important; height: 100vh !important; margin: 0 !important; padding: 0 !important; overflow: hidden !important; }\\n' +
    '#squartor-root {' +
    ' position: absolute !important;' +
    ' top: ' + safeTop + 'px !important;' +
    ' bottom: ' + safeBottom + 'px !important;' +
    ' left: 0 !important;' +
    ' right: 0 !important;' +
    ' overflow: hidden !important;' +
    '}\\n' +
    '#squartor-source {' +
    ' display: block !important;' +
    ' position: relative !important;' +
    ' left: ' + horizontalMargin + 'px !important;' +
    ' box-sizing: border-box !important;' +
    ' height: 100% !important;' +
    ' width: 100% !important;' +
    ' min-width: 100% !important;' +
    ' max-width: none !important;' +
    ' column-fill: auto !important;' +
    ' overflow: visible !important;' +
    ' background: transparent !important;' +
    ' color: ' + cfg.text + ' !important;' +
    ' font-family: ' + fontFamily + ' !important;' +
    ' font-size: ' + effectiveFontSize + 'px !important;' +
    ' line-height: ' + cfg.lineHeight + ' !important;' +
    ' letter-spacing: ' + cfg.letterSpacing + 'px !important;' +
    ' word-break: break-word !important;' +
    ' white-space: normal !important;' +
    ' text-align: start !important;' +
    ' writing-mode: horizontal-tb !important;' +
    ' text-orientation: mixed !important;' +
    ' will-change: transform !important;' +
    ' backface-visibility: hidden !important;' +
    ' -webkit-text-size-adjust: none !important;' +
    '}\\n' +
    '#squartor-source > .sq-spine-marker { display: none !important; }\\n' +
    '#squartor-source p, #squartor-source h1, #squartor-source h2,' +
    '#squartor-source h3, #squartor-source h4, #squartor-source h5,' +
    '#squartor-source h6, #squartor-source pre {' +
    ' position: static !important;' +
    ' width: auto !important;' +
    ' max-width: none !important;' +
    ' height: auto !important;' +
    ' margin-top: 0 !important;' +
    ' margin-left: 0 !important;' +
    ' margin-right: 0 !important;' +
    ' margin-bottom: ' + cfg.paragraphSpacing + 'px !important;' +
    ' padding: 0 !important;' +
    ' color: inherit !important;' +
    ' text-align: start !important;' +
    ' writing-mode: horizontal-tb !important;' +
    ' break-before: auto !important;' +
    ' break-after: auto !important;' +
    ' break-inside: auto !important;' +
    ' orphans: 1 !important;' +
    ' widows: 1 !important;' +
    '}\\n' +
    '#squartor-source p {' +
    ' text-align: justify !important;' +
    ' text-align-last: auto !important;' +
    '}\\n' +
    '#squartor-source > .sq-title-block {' +
    ' text-indent: 0 !important;' +
    ' break-inside: auto !important;' +
    '}\\n' +
    '#squartor-source > .sq-title-lead {' +
    ' font-size: ' + (effectiveFontSize * 1.28) + 'px !important;' +
    ' font-weight: 700 !important;' +
    ' line-height: 1.38 !important;' +
    ' text-indent: 0 !important;' +
    '}\\n' +
    '#squartor-source.squartor-image-page {' +
    ' display: flex !important;' +
    ' align-items: center !important;' +
    ' justify-content: center !important;' +
    ' position: relative !important;' +
    ' left: 0 !important;' +
    ' width: 100vw !important;' +
    ' min-width: 100vw !important;' +
    ' height: 100% !important;' +
    ' column-width: auto !important;' +
    ' column-count: 1 !important;' +
    ' overflow: hidden !important;' +
    '}\\n' +
    '#squartor-source.squartor-image-page > * {' +
    ' display: flex !important;' +
    ' align-items: center !important;' +
    ' justify-content: center !important;' +
    ' position: static !important;' +
    ' left: auto !important;' +
    ' right: auto !important;' +
    ' top: auto !important;' +
    ' bottom: auto !important;' +
    ' transform: none !important;' +
    ' float: none !important;' +
    ' width: 100% !important;' +
    ' height: 100% !important;' +
    ' margin: 0 !important;' +
    ' padding: 0 !important;' +
    '}\\n' +
    '#squartor-source.squartor-image-page > * * {' +
    ' position: static !important;' +
    ' left: auto !important;' +
    ' right: auto !important;' +
    ' top: auto !important;' +
    ' bottom: auto !important;' +
    ' transform: none !important;' +
    ' float: none !important;' +
    '}\\n' +
    '#squartor-source.squartor-image-page img,' +
    '#squartor-source.squartor-image-page svg {' +
    ' width: auto !important;' +
    ' height: auto !important;' +
    ' max-width: var(--squartor-image-max-width, 100%) !important;' +
    ' max-height: var(--squartor-image-max-height, 100%) !important;' +
    ' object-fit: contain !important;' +
    ' margin: auto !important;' +
    '}\\n' +
    '#squartor-source .sq-media {' +
    ' display: flex !important;' +
    ' align-items: center !important;' +
    ' justify-content: center !important;' +
    ' max-width: 100% !important;' +
    ' margin: 0 0 ' + cfg.paragraphSpacing + 'px 0 !important;' +
    ' padding: 0 !important;' +
    ' break-inside: avoid !important;' +
    '}\\n' +
    '#squartor-source .sq-media img,' +
    '#squartor-source .sq-media svg {' +
    ' width: auto !important;' +
    ' height: auto !important;' +
    ' max-width: 100% !important;' +
    ' max-height: 80vh !important;' +
    '}\\n' +
    '#squartor-source.squartor-image-page .sq-media,' +
    '#squartor-source.squartor-image-page .sq-media * {' +
    ' max-width: 100% !important;' +
    ' max-height: 100% !important;' +
    '}\\n' +
    '#squartor-source.squartor-image-page .sq-media {' +
    ' position: absolute !important;' +
    ' left: 50% !important;' +
    ' top: 50% !important;' +
    ' right: auto !important;' +
    ' bottom: auto !important;' +
    ' transform: translate(-50%, -50%) !important;' +
    ' display: flex !important;' +
    ' align-items: center !important;' +
    ' justify-content: center !important;' +
    ' width: var(--squartor-image-max-width, 100%) !important;' +
    ' height: var(--squartor-image-max-height, 100%) !important;' +
    ' max-width: var(--squartor-image-max-width, 100%) !important;' +
    ' max-height: var(--squartor-image-max-height, 100%) !important;' +
    ' margin: 0 !important;' +
    ' padding: 0 !important;' +
    ' overflow: hidden !important;' +
    '}\\n' +
    '#squartor-source.squartor-image-page .sq-media img,' +
    '#squartor-source.squartor-image-page .sq-media svg {' +
    ' display: block !important;' +
    ' width: 100% !important;' +
    ' height: 100% !important;' +
    ' max-width: 100% !important;' +
    ' max-height: 100% !important;' +
    ' object-fit: contain !important;' +
    ' margin: 0 auto !important;' +
    '}\\n' +
    'p { margin-top: 0 !important; margin-left: 0 !important; margin-right: 0 !important; margin-bottom: ' + cfg.paragraphSpacing + 'px !important; text-indent: ' + (cfg.firstLineIndent ? '2em' : '0') + ' !important; text-align: justify !important; text-align-last: auto !important; break-inside: auto !important; }\\n' +
    'p:first-child, .sq-title-block, .sq-list-item, .sq-table-row, .sq-quote { text-indent: 0 !important; }\\n' +
    'h1 { font-size: ' + (effectiveFontSize * 1.45) + 'px !important; line-height: 1.35 !important; }\\n' +
    'h2 { font-size: ' + (effectiveFontSize * 1.22) + 'px !important; line-height: 1.35 !important; }\\n' +
    'h3, h4, h5, h6 { font-size: ' + (effectiveFontSize * 1.08) + 'px !important; line-height: 1.35 !important; }\\n' +
    'a, a * { color: ' + cfg.accent + ' !important; }\\n' +
    'img, svg, video, canvas { max-width: 100% !important; max-height: 85% !important; height: auto !important; break-inside: avoid !important; }\\n' +
    'ruby, rt { ruby-position: over; }\\n' +
    '* { -webkit-tap-highlight-color: transparent; }';
  document.head.appendChild(style);

  source.classList.toggle('squartor-image-page', isImagePage);

  function layoutPages() {
    const viewportWidth = Math.max(window.innerWidth, document.documentElement.clientWidth);
    const viewportHeight = Math.max(window.innerHeight, document.documentElement.clientHeight);
    if (viewportWidth < 240 || viewportHeight < safeTop + safeBottom + effectiveFontSize * cfg.lineHeight * 4) {
      return false;
    }
    const width = viewportWidth - horizontalMargin * 2;
    const rawHeight = viewportHeight - safeTop - safeBottom;
    const lineHeightPx = Math.max(1, effectiveFontSize * cfg.lineHeight);
    const pageHeight = Math.max(lineHeightPx * 4, Math.floor(rawHeight / lineHeightPx) * lineHeightPx);
    const contentWidth = isImagePage ? viewportWidth : width;
    root.style.setProperty('right', 'auto', 'important');
    root.style.setProperty('bottom', 'auto', 'important');
    root.style.setProperty('width', viewportWidth + 'px', 'important');
    root.style.setProperty('height', pageHeight + 'px', 'important');
    source.style.setProperty('display', isImagePage ? 'flex' : 'block', 'important');
    source.style.setProperty('left', (isImagePage ? 0 : horizontalMargin) + 'px', 'important');
    source.style.setProperty('width', contentWidth + 'px', 'important');
    source.style.setProperty('min-width', contentWidth + 'px', 'important');
    source.style.setProperty('height', pageHeight + 'px', 'important');
    source.style.setProperty('max-width', 'none', 'important');
    source.style.setProperty('--squartor-image-max-width', Math.max(160, width) + 'px');
    source.style.setProperty('--squartor-image-max-height', pageHeight + 'px');
    source.style.setProperty('column-count', isImagePage ? '1' : 'auto', 'important');
    source.style.setProperty('column-width', isImagePage ? 'auto' : width + 'px', 'important');
    source.style.setProperty('column-gap', isImagePage ? '0px' : pageGap + 'px', 'important');
    const stride = viewportWidth;
    const scrollWidth = Math.max(width, source.scrollWidth);
    window.SQuartor.pageStride = stride;
    window.SQuartor.pages = isImagePage ? 1 : Math.max(1, Math.ceil((scrollWidth + pageGap) / stride));
    if (!window.SQuartor.hasRestoredProgress) {
      window.SQuartor.page = Math.round(window.SQuartor.initialProgress * (window.SQuartor.pages - 1));
      window.SQuartor.hasRestoredProgress = true;
    }
    window.SQuartor.page = Math.max(0, Math.min(window.SQuartor.page, window.SQuartor.pages - 1));
    return true;
  }

  function applyTransform(animate) {
    source.style.transition = animate ? 'transform 180ms ease' : 'transform 0ms linear';
    if (isImagePage) {
      source.style.setProperty('transform', 'translate3d(0px, 0, 0)', 'important');
      return;
    }
    source.style.setProperty('transform', 'translate3d(' + (-(window.SQuartor.page * window.SQuartor.pageStride)) + 'px, 0, 0)', 'important');
  }

  function applyDragTransform(dx) {
    const atStart = window.SQuartor.page <= 0 && dx > 0;
    const atEnd = window.SQuartor.page >= window.SQuartor.pages - 1 && dx < 0;
    const resistance = (atStart || atEnd) ? 0.28 : 1;
    source.style.transition = 'transform 0ms linear';
    source.style.setProperty('transform', 'translate3d(' + (-(window.SQuartor.page * window.SQuartor.pageStride) + dx * resistance) + 'px, 0, 0)', 'important');
  }

  function notify() {
    if (window.flutter_inappwebview) {
      window.flutter_inappwebview.callHandler('squartorEvent', { type: 'ready' });
      window.flutter_inappwebview.callHandler('squartorEvent', { type: 'progress', page: window.SQuartor.page, pages: window.SQuartor.pages });
    }
  }

  window.SQuartor = {
    page: 0,
    pages: 1,
    pageStride: window.innerWidth,
    initialProgress: Math.max(0, Math.min(1, Number(cfg.initialProgress || 0))),
    hasRestoredProgress: false,
    dragToken: 0,
    dragActive: false,
    layout() {
      if (!layoutPages()) return false;
      applyTransform(false);
      notify();
      return true;
    },
    nextPage() {
      if (this.page >= this.pages - 1) return 'end';
      this.page += 1;
      applyTransform(true);
      notify();
      return 'ok';
    },
    previousPage() {
      if (this.page <= 0) return 'start';
      this.page -= 1;
      applyTransform(true);
      notify();
      return 'ok';
    },
    jumpToProgress(progress) {
      progress = Math.max(0, Math.min(1, Number(progress || 0)));
      this.page = Math.round(progress * Math.max(0, this.pages - 1));
      applyTransform(false);
      notify();
      return 'ok';
    },
    dragStart(token) {
      this.dragToken = Number(token || 0);
      this.dragActive = true;
      source.style.transition = 'transform 0ms linear';
      return 'ok';
    },
    dragMove(dx, token) {
      if (!this.dragActive || Number(token || 0) !== this.dragToken) {
        return 'stale';
      }
      applyDragTransform(Number(dx || 0));
      return 'ok';
    },
    dragCancel(token) {
      if (Number(token || 0) !== this.dragToken) {
        return 'stale';
      }
      this.dragActive = false;
      applyTransform(true);
      settleAfterAnimation(this.dragToken);
      return 'cancel';
    },
    dragEnd(dx, velocity, token) {
      if (!this.dragActive || Number(token || 0) !== this.dragToken) {
        return 'stale';
      }
      this.dragActive = false;
      dx = Number(dx || 0);
      velocity = Number(velocity || 0);
      const shouldTurn = Math.abs(dx) > window.innerWidth * 0.22 || Math.abs(velocity) > 420;
      if (!shouldTurn) {
        applyTransform(true);
        settleAfterAnimation(this.dragToken);
        return 'cancel';
      }
      if (dx < 0) {
        if (this.page >= this.pages - 1) {
          applyTransform(true);
          settleAfterAnimation(this.dragToken);
          return 'end';
        }
        this.page += 1;
      } else {
        if (this.page <= 0) {
          applyTransform(true);
          settleAfterAnimation(this.dragToken);
          return 'start';
        }
        this.page -= 1;
      }
      applyTransform(true);
      settleAfterAnimation(this.dragToken);
      notify();
      return 'ok';
    },
    jumpToAnchor(anchor) {
      if (!anchor) return;
      const node = source.querySelector('#' + CSS.escape(anchor)) || source.querySelector('[name="' + CSS.escape(anchor) + '"]');
      if (!node) return;
      const offset = Math.max(0, node.getBoundingClientRect().left - source.getBoundingClientRect().left);
      const index = Math.floor(offset / this.pageStride);
      this.page = Math.max(0, Math.min(index, this.pages - 1));
      applyTransform(true);
      notify();
    },
    openLinkAt(x, y) {
      x = Number(x);
      y = Number(y);
      const node = document.elementFromPoint(x, y);
      let link = node && node.closest ? node.closest('a[href]') : null;
      if (!link) {
        link = Array.from(source.querySelectorAll('a[href]')).find(function(candidate) {
          return Array.from(candidate.getClientRects()).some(function(rect) {
            return x >= rect.left - 8 && x <= rect.right + 8 &&
              y >= rect.top - 8 && y <= rect.bottom + 8;
          });
        }) || null;
      }
      if (!link) return false;
      if (window.flutter_inappwebview) {
        window.flutter_inappwebview.callHandler('squartorEvent', { type: 'linkTap' });
      }
      const target = new URL(link.href, document.baseURI);
      const current = new URL(window.location.href);
      if (target.pathname === current.pathname && target.hash) {
        const anchor = decodeURIComponent(target.hash.slice(1));
        this.jumpToAnchor(anchor);
        history.replaceState(null, '', target.hash);
        return true;
      }
      if (window.flutter_inappwebview) {
        window.flutter_inappwebview.callHandler('squartorEvent', {
          type: 'internalLink',
          href: target.href
        });
      }
      return true;
    },
    imageAt(x, y) {
      const node = document.elementFromPoint(Number(x), Number(y));
      const image = node && node.closest ? node.closest('img') : null;
      return image ? image.src : null;
    },
    dispose() {}
  };

  function settleAfterAnimation(token) {
    setTimeout(function() {
      if (window.SQuartor && !window.SQuartor.dragActive && window.SQuartor.dragToken === token) {
        applyTransform(false);
      }
    }, 230);
  }

  let layoutRound = 0;
  function settleLayout() {
    if (!window.SQuartor.layout()) {
      setTimeout(settleLayout, 50);
      return;
    }
    if (layoutRound === 0 && cfg.anchor) window.SQuartor.jumpToAnchor(cfg.anchor);
    layoutRound += 1;
    if (layoutRound < 3) requestAnimationFrame(settleLayout);
  }
  let resizeTimer = null;
  function scheduleLayout() {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(function() { window.SQuartor.layout(); }, 50);
  }
  window.addEventListener('resize', scheduleLayout);
  window.SQuartor.dispose = function() {
    clearTimeout(resizeTimer);
    window.removeEventListener('resize', scheduleLayout);
  };
  requestAnimationFrame(settleLayout);
  if (document.fonts && document.fonts.ready) {
    document.fonts.ready.then(function() { requestAnimationFrame(settleLayout); });
  }
  Array.from(document.images || []).forEach(function(image) {
    if (!image.complete) image.addEventListener('load', function() { requestAnimationFrame(settleLayout); }, { once: true });
  });
})();
''';
  }

  Future<void> _onReaderTap(TapUpDetails details) async {
    final width = MediaQuery.sizeOf(context).width;
    if (!_usesFlutterTxt) {
      _linkTapHandled = false;
      final x = details.localPosition.dx.toStringAsFixed(2);
      final y = details.localPosition.dy.toStringAsFixed(2);
      final openedLink = await _controller?.evaluateJavascript(
        source: 'window.SQuartor && window.SQuartor.openLinkAt($x, $y);',
      );
      if (_linkTapHandled ||
          openedLink == true ||
          openedLink == 'true' ||
          openedLink == 1) {
        return;
      }
    }
    final tapX = details.localPosition.dx;
    if (tapX >= width * .32 && tapX <= width * .68) {
      _toggleChrome();
      return;
    }
    final tappedLeft = tapX < width * .32;
    final previous = style.reverseTapPageTurn ? !tappedLeft : tappedLeft;
    if (previous) {
      _previousPage();
    } else {
      _nextPage();
    }
  }

  Future<void> _onReaderLongPress(LongPressStartDetails details) async {
    final x = details.localPosition.dx.toStringAsFixed(2);
    final y = details.localPosition.dy.toStringAsFixed(2);
    final result = await _controller?.evaluateJavascript(
      source: 'window.SQuartor && window.SQuartor.imageAt($x, $y);',
    );
    if (!mounted || result is! String || result.isEmpty) {
      return;
    }
    await Navigator.of(context).push<void>(
      PageRouteBuilder<void>(
        opaque: true,
        transitionDuration: Duration.zero,
        reverseTransitionDuration: Duration.zero,
        pageBuilder: (context, animation, secondaryAnimation) =>
            _FullscreenImageViewer(source: result),
      ),
    );
  }

  Future<void> _openInternalLink(String href) async {
    final uri = Uri.tryParse(href);
    if (uri == null) {
      return;
    }
    if (_isExternalUri(uri)) {
      await _openExternalLink(uri);
      if (mounted && uri.scheme.isEmpty) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('暂不打开外部链接')));
      }
      return;
    }
    if (uri.scheme != 'file') {
      return;
    }
    final targetPath = path.normalize(
      uri.replace(fragment: '', query: '').toFilePath(),
    );
    final index = book.chapters.indexWhere(
      (chapter) => path.normalize(chapter.filePath) == targetPath,
    );
    if (index == -1) {
      return;
    }
    final anchor = uri.fragment.isEmpty
        ? null
        : Uri.decodeComponent(uri.fragment);
    if (index == _chapterIndex) {
      if (anchor != null) {
        if (_usesFlutterTxt) {
          setState(() {
            _pendingAnchor = anchor;
            _pendingPageProgress = 0;
            _pendingExactPage = null;
            _pendingExactPageCount = null;
            _txtPaginationSignature = null;
            _txtRequestedSignature = null;
            _txtPages = const [];
            _isLoading = true;
            _loadError = null;
          });
          final metrics = _txtLayoutMetrics;
          if (metrics != null) {
            _requestTxtPagination(metrics);
          }
        } else {
          await _controller?.evaluateJavascript(
            source:
                'window.SQuartor && window.SQuartor.jumpToAnchor(${jsonEncode(anchor)});',
          );
        }
      }
      return;
    }
    await _goToChapter(index, anchor: anchor);
  }

  bool _isExternalUri(Uri uri) {
    final scheme = uri.scheme.toLowerCase();
    return scheme == 'http' ||
        scheme == 'https' ||
        scheme == 'mailto' ||
        scheme == 'tel';
  }

  bool _isExternalUriString(String value) {
    final uri = Uri.tryParse(value);
    return uri != null && _isExternalUri(uri);
  }

  Future<void> _openExternalLink(Uri uri) async {
    var opened = false;
    try {
      opened = await launchUrl(uri, mode: LaunchMode.externalApplication);
    } catch (_) {
      opened = false;
    }
    if (!opened && mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('无法打开外部链接')));
    }
  }

  void _flushReadingTime() {
    if (!_readingStopwatch.isRunning) {
      return;
    }
    final seconds = _readingStopwatch.elapsed.inSeconds;
    if (seconds >= 5) {
      widget.state.addReadingSeconds(seconds, widget.book.id);
      _readingStopwatch
        ..reset()
        ..start();
    }
  }

  Future<void> _nextPage() async {
    if (_usesFlutterTxt) {
      await _nextTxtPage();
      return;
    }
    final result = await _controller?.evaluateJavascript(
      source: 'window.SQuartor && window.SQuartor.nextPage();',
    );
    if (result == 'end') {
      await _goToChapter(_chapterIndex + 1);
    }
  }

  Future<void> _previousPage() async {
    if (_usesFlutterTxt) {
      await _previousTxtPage();
      return;
    }
    final result = await _controller?.evaluateJavascript(
      source: 'window.SQuartor && window.SQuartor.previousPage();',
    );
    if (result == 'start') {
      await _goToChapter(_chapterIndex - 1, atEnd: true);
    }
  }

  Future<void> _nextTxtPage() async {
    final safePageCount = _txtPages.isEmpty ? 1 : _txtPages.length;
    if (_page >= safePageCount - 1) {
      await _goToChapter(_chapterIndex + 1);
      return;
    }
    await _txtPageController.nextPage(
      duration: const Duration(milliseconds: 180),
      curve: Curves.easeOutCubic,
    );
  }

  Future<void> _previousTxtPage() async {
    if (_page <= 0) {
      await _goToChapter(_chapterIndex - 1, atEnd: true);
      return;
    }
    await _txtPageController.previousPage(
      duration: const Duration(milliseconds: 180),
      curve: Curves.easeOutCubic,
    );
  }

  void _onReaderDragStart(DragStartDetails details) {
    _dragDx = 0;
    _dragDy = 0;
    _pageDragActive = false;
    _dragMoveScheduled = false;
    _dragSession++;
    final session = _dragSession;
    _controller?.evaluateJavascript(
      source: 'window.SQuartor && window.SQuartor.dragStart($session);',
    );
  }

  void _onReaderDragUpdate(DragUpdateDetails details) {
    _dragDx += details.delta.dx;
    _dragDy += details.delta.dy;
    if (!_pageDragActive &&
        _dragDx.abs() > 8 &&
        _dragDx.abs() > _dragDy.abs()) {
      _pageDragActive = true;
    }
    if (_pageDragActive) {
      _scheduleReaderDragMove();
    }
  }

  void _scheduleReaderDragMove() {
    if (_dragMoveScheduled) {
      return;
    }
    _dragMoveScheduled = true;
    final session = _dragSession;
    SchedulerBinding.instance.scheduleFrameCallback((_) {
      _dragMoveScheduled = false;
      final controller = _controller;
      if (!_pageDragActive || controller == null || session != _dragSession) {
        return;
      }
      final dx = _dragDx.toStringAsFixed(2);
      unawaited(
        controller.evaluateJavascript(
          source: 'window.SQuartor && window.SQuartor.dragMove($dx, $session);',
        ),
      );
    });
    SchedulerBinding.instance.ensureVisualUpdate();
  }

  Future<void> _onReaderDragEnd(DragEndDetails details) async {
    if (!_pageDragActive) {
      _onReaderDragCancel();
      return;
    }
    final velocity = details.primaryVelocity ?? 0;
    final dx = _dragDx.toStringAsFixed(2);
    final v = velocity.toStringAsFixed(2);
    final session = _dragSession;
    _pageDragActive = false;
    _dragMoveScheduled = false;
    _dragDx = 0;
    _dragDy = 0;
    _dragSession++;
    final result = await _controller?.evaluateJavascript(
      source: 'window.SQuartor && window.SQuartor.dragEnd($dx, $v, $session);',
    );
    if (result == 'end') {
      await _goToChapter(_chapterIndex + 1);
    } else if (result == 'start') {
      await _goToChapter(_chapterIndex - 1, atEnd: true);
    }
  }

  void _onReaderDragCancel() {
    final session = _dragSession;
    _pageDragActive = false;
    _dragMoveScheduled = false;
    _dragDx = 0;
    _dragDy = 0;
    _dragSession++;
    _controller?.evaluateJavascript(
      source: 'window.SQuartor && window.SQuartor.dragCancel($session);',
    );
  }

  Future<void> _goToChapter(
    int index, {
    bool atEnd = false,
    double? progress,
    String? anchor,
  }) async {
    if (index < 0 || index > _lastChapterIndex || book.chapters.isEmpty) {
      return;
    }
    _clearReaderSnapshot(notify: false);
    setState(() {
      _chapterIndex = index;
      _page = 0;
      _pageCount = 1;
      _pendingPageProgress = progress ?? (atEnd ? 1 : 0);
      _pendingExactPage = null;
      _pendingExactPageCount = null;
      _pendingAnchor = anchor ?? book.chapters[index].anchor;
      _isLoading = true;
      _loadError = null;
      if (_usesFlutterTxt) {
        _txtPages = const [];
        _txtPaginationSignature = null;
        _txtRequestedSignature = null;
      }
    });
    if (_usesFlutterTxt) {
      final metrics = _txtLayoutMetrics;
      if (metrics != null) {
        _requestTxtPagination(metrics);
      }
      return;
    }
    await _controller?.loadUrl(
      urlRequest: URLRequest(
        url: WebUri.uri(File(book.chapters[index].filePath).uri),
      ),
    );
  }

  void _syncChapterFromUrl(WebUri? url) {
    if (url == null || !url.isScheme('file')) {
      return;
    }
    final loadedPath = path.normalize(url.uriValue.toFilePath());
    final index = book.chapters.indexWhere(
      (chapter) => path.normalize(chapter.filePath) == loadedPath,
    );
    if (index != -1 && index != _chapterIndex && mounted) {
      setState(() => _chapterIndex = index);
    }
  }

  void _showToc() {
    _showSideOverlay(_ReaderOverlay.toc);
  }

  void _showSettings() {
    _showSideOverlay(_ReaderOverlay.settings);
  }

  Future<void> _hideFooter() async {
    if (_footerAnimation.value <= 0.001) {
      _footerAnimation.value = 0;
      return;
    }
    await _footerAnimation.reverse();
  }

  Future<void> _showFooter({bool fromZero = false}) async {
    if (!mounted || _overlay != _ReaderOverlay.hidden) {
      return;
    }
    if (_footerAnimation.value >= 0.999) {
      return;
    }
    await _footerAnimation.forward(from: fromZero ? 0 : _footerAnimation.value);
  }

  void _showSideOverlay(_ReaderOverlay target) {
    if (!_isSideOverlay(target)) {
      return;
    }
    final serial = ++_overlayTransitionSerial;
    unawaited(_hideFooter());
    unawaited(_openSideOverlay(target, serial));
  }

  void _toggleChrome() {
    if (!mounted) {
      return;
    }
    final serial = ++_overlayTransitionSerial;
    unawaited(_toggleChromeAnimated(serial));
  }

  void _hideOverlay() {
    if (!mounted || _overlay == _ReaderOverlay.hidden) {
      return;
    }
    final serial = ++_overlayTransitionSerial;
    unawaited(_hideOverlayAnimated(serial));
  }

  AnimationController _sideAnimation(_ReaderOverlay overlay) {
    return overlay == _ReaderOverlay.settings
        ? _settingsAnimation
        : _tocAnimation;
  }

  Future<void> _openSideOverlay(_ReaderOverlay target, int serial) async {
    if (!mounted) {
      return;
    }
    final current = _overlay;
    await _hideFooter();
    final freezeFuture = _freezeReaderForSideOverlay(serial);
    if (current == target) {
      await freezeFuture;
      await _sideAnimation(target).forward();
      return;
    }
    if (current == _ReaderOverlay.chrome) {
      setState(() => _overlay = _ReaderOverlay.hidden);
      await Future.wait([_chromeAnimation.reverse(), freezeFuture]);
    } else if (_isSideOverlay(current)) {
      setState(() => _overlay = _ReaderOverlay.hidden);
      await _sideAnimation(current).reverse();
    } else {
      await freezeFuture;
    }
    if (!mounted || serial != _overlayTransitionSerial) {
      return;
    }
    final other = target == _ReaderOverlay.toc
        ? _ReaderOverlay.settings
        : _ReaderOverlay.toc;
    if (_sideAnimation(other).value > 0) {
      await _sideAnimation(other).reverse();
    }
    if (!mounted || serial != _overlayTransitionSerial) {
      return;
    }
    setState(() => _overlay = target);
    await _sideAnimation(target).forward();
  }

  Future<void> _toggleChromeAnimated(int serial) async {
    if (_overlay == _ReaderOverlay.chrome) {
      setState(() => _overlay = _ReaderOverlay.hidden);
      await _chromeAnimation.reverse();
      if (mounted && serial == _overlayTransitionSerial) {
        await _showFooter(fromZero: true);
      }
      return;
    }
    final current = _overlay;
    await _hideFooter();
    if (_isSideOverlay(current)) {
      setState(() => _overlay = _ReaderOverlay.hidden);
      await _sideAnimation(current).reverse();
    }
    if (!mounted || serial != _overlayTransitionSerial) {
      return;
    }
    if (_isSideOverlay(current)) {
      _clearReaderSnapshot();
    }
    setState(() => _overlay = _ReaderOverlay.chrome);
    await _chromeAnimation.forward();
  }

  Future<void> _hideOverlayAnimated(int serial) async {
    final current = _overlay;
    await _hideFooter();
    setState(() => _overlay = _ReaderOverlay.hidden);
    if (current == _ReaderOverlay.chrome) {
      await _chromeAnimation.reverse();
    } else if (_isSideOverlay(current)) {
      await _sideAnimation(current).reverse();
    }
    if (!mounted || serial != _overlayTransitionSerial) {
      return;
    }
    if (_isSideOverlay(current)) {
      _clearReaderSnapshot();
    }
    await _showFooter(fromZero: true);
  }

  void _scheduleStyleInjection() {
    if (_usesFlutterTxt) {
      _txtPaginationSignature = null;
      _txtRequestedSignature = null;
      final metrics = _txtLayoutMetrics;
      if (metrics != null) {
        _requestTxtPagination(metrics);
      }
      return;
    }
    _styleInjectTimer?.cancel();
    _styleInjectTimer = Timer(const Duration(milliseconds: 140), () {
      unawaited(
        _injectReaderStyle().then((_) => _refreshFrozenReaderSnapshot()),
      );
    });
  }

  String _cssColor(Color color) {
    String two(int value) => value.toRadixString(16).padLeft(2, '0');
    final r = (color.r * 255).round().clamp(0, 255);
    final g = (color.g * 255).round().clamp(0, 255);
    final b = (color.b * 255).round().clamp(0, 255);
    return '#${two(r)}${two(g)}${two(b)}';
  }
}

class _ReaderPanelScrim extends StatelessWidget {
  const _ReaderPanelScrim({
    required this.visible,
    required this.palette,
    required this.onDismiss,
  });

  final bool visible;
  final AppPalette palette;
  final VoidCallback onDismiss;

  @override
  Widget build(BuildContext context) {
    return Positioned.fill(
      child: IgnorePointer(
        ignoring: !visible,
        child: AnimatedOpacity(
          duration: const Duration(milliseconds: 240),
          curve: Curves.easeOut,
          opacity: visible ? 1 : 0,
          child: GestureDetector(
            behavior: HitTestBehavior.opaque,
            onTap: onDismiss,
            child: ColoredBox(color: _ReaderGlassPalette.from(palette).scrim),
          ),
        ),
      ),
    );
  }
}

class _FloatingPanelSurface extends StatelessWidget {
  const _FloatingPanelSurface({required this.palette, required this.child});

  final AppPalette palette;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final glass = _ReaderGlassPalette.from(palette);
    return Material(
      color: glass.panel,
      elevation: glass.dark ? 2 : 1,
      shadowColor: Colors.black.withValues(alpha: glass.dark ? .28 : .12),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(28),
        side: BorderSide(color: glass.line),
      ),
      child: ClipRRect(borderRadius: BorderRadius.circular(28), child: child),
    );
  }
}

class _ReaderPanelCloseButton extends StatelessWidget {
  const _ReaderPanelCloseButton({
    required this.glass,
    required this.tooltip,
    required this.onPressed,
    this.icon = const Icon(Icons.close_rounded, size: 27),
    this.color,
  });

  final _ReaderGlassPalette glass;
  final String tooltip;
  final VoidCallback onPressed;
  final Widget icon;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 46,
      height: 46,
      child: IconButton(
        tooltip: tooltip,
        style: IconButton.styleFrom(
          foregroundColor: color ?? glass.text,
          backgroundColor: glass.pill,
          padding: EdgeInsets.zero,
          shape: const CircleBorder(),
        ),
        onPressed: onPressed,
        icon: icon,
      ),
    );
  }
}

class _ReaderFooter extends StatelessWidget {
  const _ReaderFooter({
    required this.now,
    required this.chapter,
    required this.chapterCount,
    required this.page,
    required this.pageCount,
    required this.progress,
    required this.palette,
  });

  final DateTime now;
  final int chapter;
  final int chapterCount;
  final int page;
  final int pageCount;
  final double progress;
  final ReaderPalette palette;

  @override
  Widget build(BuildContext context) {
    final time =
        '${now.hour.toString().padLeft(2, '0')}:${now.minute.toString().padLeft(2, '0')}';
    final isDark =
        ThemeData.estimateBrightnessForColor(palette.background) ==
        Brightness.dark;
    final textStyle = TextStyle(
      color: palette.muted.withValues(alpha: .82),
      fontSize: 12,
      fontWeight: AppTextWeight.medium,
    );
    return Row(
      children: [
        _ReaderFooterChip(
          icon: isDark ? Icons.dark_mode_rounded : Icons.wb_sunny_rounded,
          label: time,
          palette: palette,
        ),
        const Spacer(),
        _ReaderFooterChip(
          icon: Icons.bookmark_border_rounded,
          label: '$page/$pageCount',
          palette: palette,
        ),
        const SizedBox(width: 8),
        _ReaderFooterChip(
          icon: Icons.menu_book_rounded,
          label: '$chapter/$chapterCount',
          palette: palette,
        ),
        Offstage(
          child: Text(
            '第 $chapter / $chapterCount 章  ·  本章 $page / $pageCount 页  ·  ${(progress * 100).toStringAsFixed(1)}%',
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            textAlign: TextAlign.right,
            style: textStyle,
          ),
        ),
      ],
    );
  }
}

class _ReaderFooterChip extends StatelessWidget {
  const _ReaderFooterChip({
    required this.icon,
    required this.label,
    required this.palette,
  });

  final IconData icon;
  final String label;
  final ReaderPalette palette;

  @override
  Widget build(BuildContext context) {
    final color = palette.muted.withValues(alpha: .86);
    return DecoratedBox(
      decoration: BoxDecoration(
        color: palette.text.withValues(alpha: .045),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: palette.muted.withValues(alpha: .18)),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 13, color: color),
            const SizedBox(width: 4),
            Text(
              label,
              maxLines: 1,
              style: TextStyle(
                color: color,
                fontSize: 11,
                height: 1,
                fontWeight: AppTextWeight.medium,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _FlutterTxtReaderView extends StatefulWidget {
  const _FlutterTxtReaderView({
    required this.pages,
    required this.controller,
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
  });

  final List<_FlutterTxtPage> pages;
  final PageController controller;
  final _TxtLayoutMetrics metrics;
  final ReaderPalette readerPalette;
  final ReadingStyle style;
  final String? fontFamily;
  final Color linkColor;
  final GestureTapUpCallback onTapUp;
  final ValueChanged<int> onPageChanged;
  final Future<void> Function() onEdgePrevious;
  final Future<void> Function() onEdgeNext;
  final Future<void> Function(String href) onLinkTap;

  @override
  State<_FlutterTxtReaderView> createState() => _FlutterTxtReaderViewState();
}

class _FlutterTxtReaderViewState extends State<_FlutterTxtReaderView> {
  double _edgeOverscroll = 0;
  double _pointerDx = 0;
  double _pointerDy = 0;
  var _edgeTurnInFlight = false;

  @override
  Widget build(BuildContext context) {
    final safePages = widget.pages.isEmpty
        ? [_FlutterTxtPage.empty()]
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
      fontWeight: AppTextWeight.regular,
    );
    final titleStyle = TextStyle(
      fontFamily: widget.fontFamily,
      color: widget.readerPalette.text,
      fontSize: effectiveFontSize * 1.45,
      height: 1.35,
      letterSpacing: widget.style.letterSpacing,
      fontWeight: AppTextWeight.semibold,
    );
    final linkStyle = TextStyle(
      fontFamily: widget.fontFamily,
      color: widget.linkColor,
      fontSize: effectiveFontSize,
      height: widget.style.lineHeight,
      letterSpacing: widget.style.letterSpacing,
      fontWeight: AppTextWeight.medium,
      decoration: TextDecoration.underline,
      decorationColor: widget.linkColor,
    );
    return Listener(
      onPointerDown: (_) {
        _pointerDx = 0;
        _pointerDy = 0;
      },
      onPointerMove: (event) {
        _pointerDx += event.delta.dx;
        _pointerDy += event.delta.dy;
      },
      onPointerUp: (_) => _handleSinglePagePointerEnd(safePages.length),
      onPointerCancel: (_) {
        _pointerDx = 0;
        _pointerDy = 0;
      },
      child: GestureDetector(
        behavior: HitTestBehavior.translucent,
        onTapUp: widget.onTapUp,
        child: ColoredBox(
          color: widget.readerPalette.background,
          child: NotificationListener<ScrollNotification>(
            onNotification: _handleScrollNotification,
            child: PageView.builder(
              controller: widget.controller,
              scrollDirection: Axis.horizontal,
              physics: const PageScrollPhysics(
                parent: AlwaysScrollableScrollPhysics(),
              ),
              onPageChanged: widget.onPageChanged,
              itemCount: safePages.length,
              itemBuilder: (context, index) {
                return RepaintBoundary(
                  child: _FlutterTxtPageView(
                    page: safePages[index],
                    metrics: widget.metrics,
                    paragraphStyle: paragraphStyle,
                    titleStyle: titleStyle,
                    linkStyle: linkStyle,
                    onLinkTap: widget.onLinkTap,
                  ),
                );
              },
            ),
          ),
        ),
      ),
    );
  }

  bool _handleScrollNotification(ScrollNotification notification) {
    if (notification is ScrollStartNotification) {
      _edgeOverscroll = 0;
    } else if (notification is OverscrollNotification) {
      _edgeOverscroll += notification.overscroll;
    } else if (notification is ScrollEndNotification) {
      final overscroll = _edgeOverscroll;
      _edgeOverscroll = 0;
      if (!_edgeTurnInFlight && overscroll.abs() > 36) {
        _edgeTurnInFlight = true;
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (!mounted) {
            return;
          }
          final turn = overscroll > 0
              ? widget.onEdgeNext()
              : widget.onEdgePrevious();
          turn.whenComplete(() {
            if (mounted) {
              _edgeTurnInFlight = false;
            }
          });
        });
      }
    }
    return false;
  }

  void _handleSinglePagePointerEnd(int pageCount) {
    final dx = _pointerDx;
    final dy = _pointerDy;
    _pointerDx = 0;
    _pointerDy = 0;
    if (pageCount != 1 ||
        _edgeTurnInFlight ||
        dx.abs() < 56 ||
        dx.abs() < dy.abs() * 1.2) {
      return;
    }
    _edgeTurnInFlight = true;
    final turn = dx < 0 ? widget.onEdgeNext() : widget.onEdgePrevious();
    turn.whenComplete(() {
      if (mounted) {
        _edgeTurnInFlight = false;
      }
    });
  }
}

class _FlutterTxtPageView extends StatelessWidget {
  const _FlutterTxtPageView({
    required this.page,
    required this.metrics,
    required this.paragraphStyle,
    required this.titleStyle,
    required this.linkStyle,
    required this.onLinkTap,
  });

  final _FlutterTxtPage page;
  final _TxtLayoutMetrics metrics;
  final TextStyle paragraphStyle;
  final TextStyle titleStyle;
  final TextStyle linkStyle;
  final Future<void> Function(String href) onLinkTap;

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
            height: metrics.contentHeight,
            child: ClipRect(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  for (final block in page.blocks)
                    Padding(
                      padding: EdgeInsets.only(bottom: block.bottomSpacing),
                      child: block.kind == _FlutterTxtBlockKind.image
                          ? SizedBox(
                              height: block.imageHeight,
                              child: _FlutterReaderImage(
                                source: block.imageSource ?? '',
                              ),
                            )
                          : block.kind == _FlutterTxtBlockKind.link
                          ? InkWell(
                              onTap: block.href == null
                                  ? null
                                  : () => unawaited(onLinkTap(block.href!)),
                              child: Text(
                                block.text,
                                textScaler: TextScaler.noScaling,
                                style: linkStyle,
                                textAlign: TextAlign.start,
                              ),
                            )
                          : RichText(
                              textScaler: TextScaler.noScaling,
                              textAlign:
                                  block.kind == _FlutterTxtBlockKind.paragraph
                                  ? TextAlign.justify
                                  : TextAlign.start,
                              text: _buildBlockTextSpan(
                                block,
                                paragraphStyle,
                                titleStyle,
                                firstLineIndentWidth,
                              ),
                            ),
                    ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  TextSpan _buildBlockTextSpan(
    _FlutterTxtBlock block,
    TextStyle paragraphStyle,
    TextStyle titleStyle,
    double firstLineIndentWidth,
  ) {
    if (block.kind == _FlutterTxtBlockKind.title) {
      return TextSpan(text: block.text, style: titleStyle);
    }
    if (!block.firstLineIndent) {
      return TextSpan(text: block.text, style: paragraphStyle);
    }
    return TextSpan(
      style: paragraphStyle,
      children: [
        WidgetSpan(
          alignment: PlaceholderAlignment.baseline,
          baseline: TextBaseline.alphabetic,
          child: SizedBox(width: firstLineIndentWidth, height: 0),
        ),
        TextSpan(text: block.text),
      ],
    );
  }
}

class _TxtLayoutMetrics {
  const _TxtLayoutMetrics({
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

class _FlutterTxtDocument {
  const _FlutterTxtDocument({required this.title, required this.blocks});

  final String title;
  final List<_FlutterDocumentBlock> blocks;
}

enum _FlutterDocumentBlockKind { paragraph, image, link }

class _FlutterDocumentBlock {
  const _FlutterDocumentBlock.paragraph(this.text)
    : kind = _FlutterDocumentBlockKind.paragraph,
      imageSource = null,
      href = null;

  const _FlutterDocumentBlock.image(this.imageSource)
    : kind = _FlutterDocumentBlockKind.image,
      text = '',
      href = null;

  const _FlutterDocumentBlock.link(this.text, this.href)
    : kind = _FlutterDocumentBlockKind.link,
      imageSource = null;

  final _FlutterDocumentBlockKind kind;
  final String text;
  final String? imageSource;
  final String? href;
}

class _EpubLinkBlock {
  const _EpubLinkBlock({required this.text, required this.href});

  final String text;
  final String href;
}

class _FlutterTxtPage {
  const _FlutterTxtPage({required this.blocks});

  factory _FlutterTxtPage.empty() {
    return const _FlutterTxtPage(blocks: []);
  }

  final List<_FlutterTxtBlock> blocks;
}

enum _FlutterTxtBlockKind { title, paragraph, image, link }

class _FlutterTxtBlock {
  const _FlutterTxtBlock({
    required this.text,
    required this.kind,
    required this.bottomSpacing,
    this.firstLineIndent = false,
    this.imageSource,
    this.imageHeight = 0,
    this.href,
  });

  final String text;
  final _FlutterTxtBlockKind kind;
  final double bottomSpacing;
  final bool firstLineIndent;
  final String? imageSource;
  final double imageHeight;
  final String? href;
}

class _FlutterReaderImage extends StatelessWidget {
  const _FlutterReaderImage({required this.source});

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
                _FullscreenImageViewer(source: source),
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

class _EpubWebViewFallbackException implements Exception {
  const _EpubWebViewFallbackException();
}

class _FullscreenImageViewer extends StatelessWidget {
  const _FullscreenImageViewer({required this.source});

  final String source;

  @override
  Widget build(BuildContext context) {
    final image = _readerImageForSource(source);
    return Scaffold(
      backgroundColor: Colors.black,
      body: SafeArea(
        child: Stack(
          children: [
            Positioned.fill(
              child: GestureDetector(
                behavior: HitTestBehavior.opaque,
                onLongPress: () => _showReaderImageActions(context, source),
                child: InteractiveViewer(
                  minScale: .8,
                  maxScale: 6,
                  child: Center(child: image),
                ),
              ),
            ),
            Positioned(
              top: 8,
              right: 8,
              child: IconButton.filledTonal(
                onPressed: () => Navigator.pop(context),
                icon: const Icon(Icons.close_rounded),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

Future<void> _showReaderImageActions(
  BuildContext context,
  String source,
) async {
  HapticFeedback.selectionClick();
  await showModalBottomSheet<void>(
    context: context,
    backgroundColor: Colors.transparent,
    builder: (sheetContext) {
      final colors = Theme.of(sheetContext).colorScheme;
      return SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
          child: DecoratedBox(
            decoration: BoxDecoration(
              color: colors.surface,
              borderRadius: BorderRadius.circular(28),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: .28),
                  blurRadius: 24,
                  offset: const Offset(0, 10),
                ),
              ],
            ),
            child: Padding(
              padding: const EdgeInsets.fromLTRB(14, 10, 14, 14),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                    width: 44,
                    height: 5,
                    decoration: BoxDecoration(
                      color: colors.outlineVariant,
                      borderRadius: BorderRadius.circular(999),
                    ),
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton.icon(
                      style: FilledButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(18),
                        ),
                      ),
                      onPressed: () {
                        Navigator.pop(sheetContext);
                        _saveReaderImage(context, source);
                      },
                      icon: const Icon(Icons.save_alt_rounded),
                      label: const Text('保存图片'),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      );
    },
  );
}

Widget _readerImageForSource(String source) {
  final uri = Uri.tryParse(source);
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
        return const Icon(Icons.broken_image_rounded, color: Colors.white70);
      }
    }
  }
  return Image.network(source, fit: BoxFit.contain);
}

Future<void> _saveReaderImage(BuildContext context, String source) async {
  HapticFeedback.mediumImpact();
  try {
    final bytes = await _readerImageBytes(source);
    if (bytes == null || bytes.isEmpty) {
      throw const FileSystemException('Image data is unavailable');
    }
    final extension = _readerImageExtension(source);
    const galleryChannel = MethodChannel('squartor/native_picker');
    await galleryChannel.invokeMethod<String>('saveImageToGallery', {
      'bytes': bytes,
      'fileName':
          'squartor_${DateTime.now().millisecondsSinceEpoch}.$extension',
      'mimeType': _readerImageMimeType(extension),
    });
    if (context.mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('图片已保存到相册')));
    }
  } catch (_) {
    if (context.mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('图片保存失败')));
    }
  }
}

Future<Uint8List?> _readerImageBytes(String source) async {
  final uri = Uri.tryParse(source);
  if (uri?.scheme == 'file') {
    return File(uri!.toFilePath()).readAsBytes();
  }
  if (uri?.scheme == 'data') {
    final comma = source.indexOf(',');
    if (comma > 0 && source.substring(0, comma).contains(';base64')) {
      return base64Decode(source.substring(comma + 1));
    }
    return null;
  }
  if (uri != null && (uri.scheme == 'http' || uri.scheme == 'https')) {
    final client = HttpClient();
    try {
      final request = await client.getUrl(uri);
      final response = await request.close();
      if (response.statusCode < 200 || response.statusCode >= 300) {
        return null;
      }
      final bytes = BytesBuilder(copy: false);
      await for (final chunk in response) {
        bytes.add(chunk);
      }
      return bytes.takeBytes();
    } finally {
      client.close(force: true);
    }
  }
  return null;
}

String _readerImageExtension(String source) {
  final dataType = RegExp(
    r'^data:image/([^;,]+)',
    caseSensitive: false,
  ).firstMatch(source)?.group(1);
  if (dataType != null) {
    return dataType.toLowerCase() == 'jpeg' ? 'jpg' : dataType.toLowerCase();
  }
  final uri = Uri.tryParse(source);
  final extension = path.extension(uri?.path ?? '').replaceFirst('.', '');
  if (extension.isNotEmpty && extension.length <= 5) {
    return extension.toLowerCase();
  }
  return 'jpg';
}

String _readerImageMimeType(String extension) {
  return switch (extension.toLowerCase()) {
    'png' => 'image/png',
    'gif' => 'image/gif',
    'webp' => 'image/webp',
    'svg' => 'image/svg+xml',
    _ => 'image/jpeg',
  };
}

class _FrostedReaderCard extends StatelessWidget {
  const _FrostedReaderCard({
    required this.readerPalette,
    required this.appPalette,
    required this.borderRadius,
    required this.child,
  });

  final ReaderPalette readerPalette;
  final AppPalette appPalette;
  final double borderRadius;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final glass = _ReaderGlassPalette.from(appPalette);
    return Material(
      color: glass.panel,
      elevation: glass.dark ? 2 : 1,
      shadowColor: Colors.black.withValues(alpha: glass.dark ? .28 : .12),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(borderRadius),
        side: BorderSide(color: glass.line),
      ),
      child: child,
    );
  }
}

class _ReaderProgressTrack extends StatelessWidget {
  const _ReaderProgressTrack({
    required this.progress,
    required this.trackColor,
    required this.fillColor,
    required this.thumbColor,
    this.height = 6,
  });

  final double progress;
  final Color trackColor;
  final Color fillColor;
  final Color thumbColor;
  final double height;

  @override
  Widget build(BuildContext context) {
    final safeProgress = progress.clamp(0.0, 1.0);
    const thumbSize = 18.0;
    return LayoutBuilder(
      builder: (context, constraints) {
        final trackWidth = constraints.maxWidth;
        final thumbLeft = (trackWidth - thumbSize) * safeProgress;
        return SizedBox(
          height: thumbSize,
          child: Stack(
            alignment: Alignment.centerLeft,
            clipBehavior: Clip.none,
            children: [
              Container(
                height: height,
                decoration: BoxDecoration(
                  color: trackColor,
                  borderRadius: BorderRadius.circular(height),
                ),
              ),
              Container(
                width: trackWidth * safeProgress,
                height: height,
                decoration: BoxDecoration(
                  color: fillColor,
                  borderRadius: BorderRadius.circular(height),
                ),
              ),
              Positioned(
                left: thumbLeft,
                child: Container(
                  width: thumbSize,
                  height: thumbSize,
                  decoration: BoxDecoration(
                    color: thumbColor,
                    shape: BoxShape.circle,
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _TopMenu extends StatelessWidget {
  const _TopMenu({
    required this.title,
    required this.palette,
    required this.appPalette,
    required this.onBack,
  });

  final String title;
  final ReaderPalette palette;
  final AppPalette appPalette;
  final VoidCallback onBack;

  @override
  Widget build(BuildContext context) {
    final glass = _ReaderGlassPalette.from(appPalette);
    return _FrostedReaderCard(
      readerPalette: palette,
      appPalette: appPalette,
      borderRadius: 24,
      child: SizedBox(
        height: 64,
        child: Row(
          children: [
            IconButton(
              onPressed: onBack,
              icon: Icon(Icons.arrow_back_rounded, color: glass.text),
            ),
            Expanded(
              child: Text(
                title,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: glass.text,
                  fontWeight: AppTextWeight.medium,
                ),
              ),
            ),
            const SizedBox(width: 48),
          ],
        ),
      ),
    );
  }
}

class _ReaderStatusOverlay extends StatelessWidget {
  const _ReaderStatusOverlay({
    required this.readerPalette,
    required this.message,
  });

  final ReaderPalette readerPalette;
  final String message;

  @override
  Widget build(BuildContext context) {
    final isError = message.startsWith('加载失败');
    return ColoredBox(
      color: readerPalette.background,
      child: Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 28),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (!isError)
                SizedBox(
                  width: 26,
                  height: 26,
                  child: CircularProgressIndicator(
                    strokeWidth: 2.6,
                    color: readerPalette.text,
                  ),
                )
              else
                Icon(
                  Icons.error_outline_rounded,
                  color: readerPalette.text,
                  size: 34,
                ),
              const SizedBox(height: 14),
              Text(
                message,
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: readerPalette.muted,
                  fontSize: 15,
                  fontWeight: AppTextWeight.regular,
                  height: 1.5,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// Kept as a fallback layout for wider form factors.
// ignore: unused_element
class _BottomMenu extends StatelessWidget {
  const _BottomMenu({
    required this.page,
    required this.pageCount,
    required this.progress,
    required this.readerPalette,
    required this.appPalette,
    required this.onToc,
    required this.onPreviousChapter,
    required this.onNextChapter,
    required this.onSettings,
  });

  final int page;
  final int pageCount;
  final double progress;
  final ReaderPalette readerPalette;
  final AppPalette appPalette;
  final VoidCallback onToc;
  final VoidCallback onPreviousChapter;
  final VoidCallback onNextChapter;
  final VoidCallback onSettings;

  @override
  Widget build(BuildContext context) {
    final iconButtonStyle = IconButton.styleFrom(
      foregroundColor: readerPalette.text,
      backgroundColor: readerPalette.muted.withValues(alpha: .12),
    );
    return _FrostedReaderCard(
      readerPalette: readerPalette,
      appPalette: appPalette,
      borderRadius: 22,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(12, 12, 12, 10),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              children: [
                IconButton(
                  tooltip: '目录',
                  style: iconButtonStyle,
                  onPressed: onToc,
                  icon: const Icon(Icons.menu_book_rounded),
                ),
                const SizedBox(width: 6),
                TextButton.icon(
                  onPressed: onPreviousChapter,
                  icon: const Icon(Icons.skip_previous_rounded, size: 19),
                  label: const Text('上一章'),
                  style: TextButton.styleFrom(
                    foregroundColor: readerPalette.text,
                  ),
                ),
                Expanded(
                  child: Column(
                    children: [
                      Text(
                        '${(progress * 100).toStringAsFixed(1)}%',
                        style: TextStyle(
                          color: readerPalette.text,
                          fontWeight: AppTextWeight.medium,
                        ),
                      ),
                      Text(
                        '${page + 1} / $pageCount',
                        style: TextStyle(
                          color: readerPalette.muted,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                ),
                TextButton.icon(
                  onPressed: onNextChapter,
                  icon: const Icon(Icons.skip_next_rounded, size: 19),
                  label: const Text('下一章'),
                  iconAlignment: IconAlignment.end,
                  style: TextButton.styleFrom(
                    foregroundColor: readerPalette.text,
                  ),
                ),
                const SizedBox(width: 6),
                IconButton(
                  tooltip: '阅读设置',
                  style: iconButtonStyle,
                  onPressed: onSettings,
                  icon: const Icon(Icons.tune_rounded),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _AdaptiveBottomMenu extends StatelessWidget {
  const _AdaptiveBottomMenu({
    required this.page,
    required this.pageCount,
    required this.progress,
    required this.readerPalette,
    required this.appPalette,
    required this.onToc,
    required this.onPreviousChapter,
    required this.onNextChapter,
    required this.onSettings,
  });

  final int page;
  final int pageCount;
  final double progress;
  final ReaderPalette readerPalette;
  final AppPalette appPalette;
  final VoidCallback onToc;
  final VoidCallback onPreviousChapter;
  final VoidCallback onNextChapter;
  final VoidCallback onSettings;

  @override
  Widget build(BuildContext context) {
    final glass = _ReaderGlassPalette.from(appPalette);
    return _FrostedReaderCard(
      readerPalette: readerPalette,
      appPalette: appPalette,
      borderRadius: 24,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              children: [
                Expanded(
                  child: _ReaderActionPill(
                    icon: Icons.menu_book_rounded,
                    label: '目录',
                    palette: readerPalette,
                    appPalette: appPalette,
                    onPressed: onToc,
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  flex: 2,
                  child: DecoratedBox(
                    decoration: BoxDecoration(
                      color: glass.pill,
                      border: Border.all(
                        color: appPalette.primarySoft.withValues(
                          alpha: glass.dark ? .28 : .34,
                        ),
                      ),
                      borderRadius: BorderRadius.circular(28),
                    ),
                    child: Row(
                      children: [
                        _ReaderPanelCloseButton(
                          glass: glass,
                          tooltip: '上一章',
                          color: glass.text,
                          onPressed: onPreviousChapter,
                          icon: const Icon(Icons.skip_previous_rounded),
                        ),
                        Expanded(
                          child: Text(
                            '${(progress * 100).toStringAsFixed(1)}%',
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              color: glass.text,
                              fontWeight: AppTextWeight.medium,
                            ),
                          ),
                        ),
                        IconButton(
                          tooltip: '下一章',
                          color: glass.text,
                          onPressed: onNextChapter,
                          icon: const Icon(Icons.skip_next_rounded),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: _ReaderActionPill(
                    icon: Icons.tune_rounded,
                    label: '设置',
                    palette: readerPalette,
                    appPalette: appPalette,
                    onPressed: onSettings,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _ReaderActionPill extends StatelessWidget {
  const _ReaderActionPill({
    required this.icon,
    required this.label,
    required this.palette,
    required this.appPalette,
    required this.onPressed,
  });

  final IconData icon;
  final String label;
  final ReaderPalette palette;
  final AppPalette appPalette;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    final glass = _ReaderGlassPalette.from(appPalette);
    return TextButton(
      style: TextButton.styleFrom(
        foregroundColor: glass.text,
        backgroundColor: glass.pill,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
        padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
      ),
      onPressed: onPressed,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 20),
          const SizedBox(height: 2),
          Text(label, maxLines: 1, style: const TextStyle(fontSize: 11)),
        ],
      ),
    );
  }
}

class _ReaderTocDrawer extends StatefulWidget {
  const _ReaderTocDrawer({
    super.key,
    required this.book,
    required this.chapterIndex,
    required this.currentPageCount,
    required this.cachedPageCounts,
    required this.palette,
    required this.onClose,
    required this.onChapterSelected,
  });

  final BookEntry book;
  final int chapterIndex;
  final int currentPageCount;
  final Map<int, int> cachedPageCounts;
  final AppPalette palette;
  final VoidCallback onClose;
  final ValueChanged<int> onChapterSelected;

  @override
  State<_ReaderTocDrawer> createState() => _ReaderTocDrawerState();
}

class _ReaderTocDrawerState extends State<_ReaderTocDrawer> {
  static const _tocItemExtent = 40.0;

  late final ScrollController _scrollController;
  late final Set<int> _expanded;

  @override
  void initState() {
    super.initState();
    _expanded = _currentPath();
    final currentVisibleIndex = _visibleChapterIndices().indexOf(
      widget.chapterIndex,
    );
    _scrollController = ScrollController(
      initialScrollOffset: ((currentVisibleIndex - 1) * _tocItemExtent).clamp(
        0,
        double.infinity,
      ),
    );
  }

  bool _hasChildren(int index) {
    return index + 1 < widget.book.chapters.length &&
        widget.book.chapters[index + 1].tocDepth >
            widget.book.chapters[index].tocDepth;
  }

  Set<int> _currentPath() {
    final result = <int>{};
    if (widget.book.chapters.isEmpty) {
      return result;
    }
    var targetDepth = widget.book.chapters[widget.chapterIndex].tocDepth - 1;
    for (var i = widget.chapterIndex - 1; i >= 0 && targetDepth >= 0; i--) {
      if (widget.book.chapters[i].tocDepth == targetDepth) {
        result.add(i);
        targetDepth -= 1;
      }
    }
    if (_hasChildren(widget.chapterIndex)) {
      result.add(widget.chapterIndex);
    }
    return result;
  }

  List<int> _visibleChapterIndices() {
    final result = <int>[];
    int? collapsedDepth;
    for (var i = 0; i < widget.book.chapters.length; i++) {
      final depth = widget.book.chapters[i].tocDepth;
      if (collapsedDepth != null) {
        if (depth > collapsedDepth) {
          continue;
        }
        collapsedDepth = null;
      }
      result.add(i);
      if (_hasChildren(i) && !_expanded.contains(i)) {
        collapsedDepth = depth;
      }
    }
    return result;
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final progress = widget.book.chapters.isEmpty
        ? 0.0
        : (widget.chapterIndex + 1) / widget.book.chapters.length;
    final visibleIndices = _visibleChapterIndices();
    final glass = _ReaderGlassPalette.from(widget.palette);
    return Material(
      color: glass.panel,
      elevation: glass.dark ? 3 : 2,
      shadowColor: Colors.black.withValues(alpha: glass.dark ? .30 : .14),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(28),
        side: BorderSide(color: glass.line),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(28),
        child: Column(
          children: [
            DecoratedBox(
              decoration: BoxDecoration(
                color: glass.panel,
                border: Border(
                  bottom: BorderSide(color: glass.line.withValues(alpha: .5)),
                ),
              ),
              child: Column(
                children: [
                  Padding(
                    padding: const EdgeInsets.fromLTRB(20, 18, 12, 12),
                    child: Row(
                      children: [
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                '目录',
                                style: TextStyle(
                                  color: glass.text,
                                  fontSize: 26,
                                  fontWeight: AppTextWeight.medium,
                                ),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                widget.book.title,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(color: glass.muted),
                              ),
                            ],
                          ),
                        ),
                        _ReaderPanelCloseButton(
                          glass: glass,
                          tooltip: '关闭目录',
                          onPressed: widget.onClose,
                        ),
                      ],
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 20),
                    child: _ReaderProgressTrack(
                      progress: progress,
                      trackColor: glass.line.withValues(alpha: .45),
                      fillColor: widget.palette.primary,
                      thumbColor: widget.palette.primarySoft,
                      height: 6,
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.fromLTRB(20, 8, 20, 12),
                    child: Align(
                      alignment: Alignment.centerRight,
                      child: Text(
                        '第 ${widget.chapterIndex + 1} / ${widget.book.chapters.length} 章',
                        style: TextStyle(color: glass.muted, fontSize: 12),
                      ),
                    ),
                  ),
                ],
              ),
            ),
            Expanded(
              child: Stack(
                children: [
                  ListView.builder(
                    controller: _scrollController,
                    itemExtent: _tocItemExtent,
                    padding: const EdgeInsets.fromLTRB(16, 8, 42, 18),
                    itemCount: visibleIndices.length,
                    itemBuilder: (context, visibleIndex) {
                      final index = visibleIndices[visibleIndex];
                      final chapter = widget.book.chapters[index];
                      final selected = index == widget.chapterIndex;
                      final hasChildren = _hasChildren(index);
                      if (widget.book.chapters.isNotEmpty) {
                        return _ReaderTocEntry(
                          chapter: chapter,
                          selected: selected,
                          hasChildren: hasChildren,
                          expanded: _expanded.contains(index),
                          palette: widget.palette,
                          glass: glass,
                          onToggle: hasChildren
                              ? () {
                                  setState(() {
                                    if (!_expanded.remove(index)) {
                                      _expanded.add(index);
                                    }
                                  });
                                }
                              : null,
                          onSelected: () => widget.onChapterSelected(index),
                        );
                      }
                      return const SizedBox.shrink();
                    },
                  ),
                  Positioned(
                    right: 4,
                    top: 14,
                    bottom: 22,
                    child: _TocFastScroller(
                      controller: _scrollController,
                      visibleIndices: visibleIndices,
                      totalCount: widget.book.chapters.length,
                      itemExtent: _tocItemExtent,
                      palette: widget.palette,
                      glass: glass,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _TocFastScroller extends StatefulWidget {
  const _TocFastScroller({
    required this.controller,
    required this.visibleIndices,
    required this.totalCount,
    required this.itemExtent,
    required this.palette,
    required this.glass,
  });

  final ScrollController controller;
  final List<int> visibleIndices;
  final int totalCount;
  final double itemExtent;
  final AppPalette palette;
  final _ReaderGlassPalette glass;

  @override
  State<_TocFastScroller> createState() => _TocFastScrollerState();
}

class _TocFastScrollerState extends State<_TocFastScroller> {
  static const _thumbHeight = 46.0;

  bool _dragging = false;
  int? _previewVisibleIndex;

  void _seekFromY(double localY, double height) {
    if (!widget.controller.hasClients || widget.visibleIndices.isEmpty) {
      return;
    }
    final usableHeight = (height - _thumbHeight).clamp(1.0, double.infinity);
    final ratio = ((localY - _thumbHeight / 2) / usableHeight).clamp(0.0, 1.0);
    final position = widget.controller.position;
    final target = (ratio * position.maxScrollExtent).clamp(
      position.minScrollExtent,
      position.maxScrollExtent,
    );
    widget.controller.jumpTo(target.toDouble());
    final visibleIndex = (target / widget.itemExtent)
        .round()
        .clamp(0, widget.visibleIndices.length - 1)
        .toInt();
    setState(() => _previewVisibleIndex = visibleIndex);
  }

  @override
  Widget build(BuildContext context) {
    if (widget.visibleIndices.length < 18) {
      return const SizedBox.shrink();
    }
    final glass = widget.glass;
    return LayoutBuilder(
      builder: (context, constraints) {
        final height = constraints.maxHeight;
        return GestureDetector(
          behavior: HitTestBehavior.translucent,
          onPanStart: (details) {
            setState(() => _dragging = true);
            _seekFromY(details.localPosition.dy, height);
          },
          onPanUpdate: (details) =>
              _seekFromY(details.localPosition.dy, height),
          onPanEnd: (_) => setState(() => _dragging = false),
          onPanCancel: () => setState(() => _dragging = false),
          child: SizedBox(
            width: 54,
            height: double.infinity,
            child: Stack(
              clipBehavior: Clip.none,
              children: [
                Positioned(
                  right: 21,
                  top: 0,
                  bottom: 0,
                  child: DecoratedBox(
                    decoration: BoxDecoration(
                      color: glass.line.withValues(alpha: .42),
                      borderRadius: BorderRadius.circular(999),
                    ),
                    child: const SizedBox(width: 4),
                  ),
                ),
                AnimatedBuilder(
                  animation: widget.controller,
                  builder: (context, _) {
                    final top = _thumbTop(height);
                    return Positioned(
                      right: 10,
                      top: top,
                      child: AnimatedContainer(
                        duration: const Duration(milliseconds: 120),
                        width: _dragging ? 28 : 24,
                        height: _thumbHeight,
                        decoration: BoxDecoration(
                          color: _dragging
                              ? widget.palette.primary
                              : glass.pill,
                          borderRadius: BorderRadius.circular(999),
                          border: Border.all(
                            color: _dragging
                                ? widget.palette.primarySoft
                                : glass.line,
                          ),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withValues(
                                alpha: glass.dark ? .24 : .10,
                              ),
                              blurRadius: 12,
                              offset: const Offset(0, 4),
                            ),
                          ],
                        ),
                        child: Icon(
                          Icons.unfold_more_rounded,
                          size: 18,
                          color: _dragging ? Colors.white : glass.muted,
                        ),
                      ),
                    );
                  },
                ),
                if (_dragging && _previewVisibleIndex != null)
                  Positioned(
                    right: 46,
                    top: (_thumbTop(height) - 8).clamp(
                      0.0,
                      (height - 38).clamp(0.0, double.infinity),
                    ),
                    child: DecoratedBox(
                      decoration: BoxDecoration(
                        color: glass.panel,
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(color: glass.line),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withValues(
                              alpha: glass.dark ? .28 : .12,
                            ),
                            blurRadius: 16,
                            offset: const Offset(0, 6),
                          ),
                        ],
                      ),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 10,
                          vertical: 7,
                        ),
                        child: Text(
                          _label,
                          maxLines: 1,
                          style: TextStyle(
                            color: glass.text,
                            fontSize: 12,
                            fontWeight: AppTextWeight.medium,
                          ),
                        ),
                      ),
                    ),
                  ),
              ],
            ),
          ),
        );
      },
    );
  }

  double _thumbTop(double height) {
    if (!widget.controller.hasClients || height <= _thumbHeight) {
      return 0;
    }
    final position = widget.controller.position;
    final maxScroll = position.maxScrollExtent;
    final ratio = maxScroll <= 0 ? 0.0 : (position.pixels / maxScroll);
    return ((height - _thumbHeight) * ratio).clamp(0.0, height - _thumbHeight);
  }

  String get _label {
    final visibleIndex = _previewVisibleIndex;
    if (visibleIndex == null || widget.visibleIndices.isEmpty) {
      return '';
    }
    final safeIndex = visibleIndex
        .clamp(0, widget.visibleIndices.length - 1)
        .toInt();
    final chapter = widget.visibleIndices[safeIndex];
    return '第 ${chapter + 1} / ${widget.totalCount} 章';
  }
}

class _ReaderTocEntry extends StatelessWidget {
  const _ReaderTocEntry({
    required this.chapter,
    required this.selected,
    required this.hasChildren,
    required this.expanded,
    required this.palette,
    required this.glass,
    required this.onToggle,
    required this.onSelected,
  });

  final ReaderChapter chapter;
  final bool selected;
  final bool hasChildren;
  final bool expanded;
  final AppPalette palette;
  final _ReaderGlassPalette glass;
  final VoidCallback? onToggle;
  final VoidCallback onSelected;

  @override
  Widget build(BuildContext context) {
    final depth = chapter.tocDepth.clamp(0, 6).toInt();
    final textTone = selected
        ? glass.text
        : Color.lerp(glass.text, glass.muted, (depth * .18).clamp(0.0, .76))!;
    final rowColor = selected
        ? palette.primary.withValues(alpha: glass.dark ? .26 : .15)
        : Colors.transparent;
    final borderColor = selected
        ? palette.primarySoft.withValues(alpha: glass.dark ? .46 : .42)
        : Colors.transparent;
    return Padding(
      padding: EdgeInsets.only(left: depth * 12.0, top: 1, bottom: 1),
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: rowColor,
          borderRadius: BorderRadius.circular(15),
          border: Border.all(color: borderColor),
        ),
        child: Row(
          children: [
            Padding(
              padding: const EdgeInsets.only(left: 5),
              child: SizedBox(
                width: 24,
                height: 24,
                child: hasChildren
                    ? IconButton(
                        padding: EdgeInsets.zero,
                        visualDensity: VisualDensity.compact,
                        style: IconButton.styleFrom(
                          backgroundColor: selected
                              ? palette.primarySoft.withValues(
                                  alpha: glass.dark ? .24 : .34,
                                )
                              : glass.pill,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(11),
                            side: BorderSide(
                              color: glass.line.withValues(alpha: .62),
                            ),
                          ),
                        ),
                        onPressed: onToggle,
                        icon: Icon(
                          expanded
                              ? Icons.keyboard_arrow_down_rounded
                              : Icons.keyboard_arrow_right_rounded,
                          color: selected ? palette.primarySoft : glass.muted,
                          size: 18,
                        ),
                      )
                    : Center(
                        child: AnimatedContainer(
                          duration: const Duration(milliseconds: 180),
                          width: selected ? 13 : 5,
                          height: selected ? 13 : 5,
                          decoration: BoxDecoration(
                            color: selected
                                ? palette.primarySoft
                                : glass.subtle.withValues(alpha: .72),
                            shape: BoxShape.circle,
                          ),
                          child: selected
                              ? Icon(
                                  Icons.bookmark_rounded,
                                  size: 8,
                                  color: glass.dark
                                      ? const Color(0xFF1B1019)
                                      : Colors.white,
                                )
                              : null,
                        ),
                      ),
              ),
            ),
            Expanded(
              child: InkWell(
                borderRadius: BorderRadius.circular(15),
                onTap: onSelected,
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(6, 2, 8, 2),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text(
                          chapter.title,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            color: textTone,
                            fontSize: depth == 0 ? 12.9 : 12.0,
                            height: 1.0,
                            fontWeight: selected || depth == 0
                                ? AppTextWeight.medium
                                : AppTextWeight.regular,
                          ),
                        ),
                      ),
                    ],
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

class _ReaderSettingsSheet extends StatefulWidget {
  const _ReaderSettingsSheet({
    required this.state,
    required this.onClose,
    required this.onChanged,
  });

  final AppState state;
  final VoidCallback onClose;
  final VoidCallback onChanged;

  @override
  State<_ReaderSettingsSheet> createState() => _ReaderSettingsSheetState();
}

class _ReaderSettingsHeader extends StatelessWidget {
  const _ReaderSettingsHeader({required this.glass, required this.onClose});

  final _ReaderGlassPalette glass;
  final VoidCallback onClose;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: glass.panel,
        border: Border(
          bottom: BorderSide(color: glass.line.withValues(alpha: .55)),
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 18, 12, 14),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '阅读设置',
                    style: TextStyle(
                      color: glass.text,
                      fontSize: 25,
                      fontWeight: AppTextWeight.semibold,
                    ),
                  ),
                  const SizedBox(height: 5),
                  Text(
                    '调整阅读版式与背景',
                    style: TextStyle(color: glass.muted, fontSize: 13),
                  ),
                ],
              ),
            ),
            _ReaderPanelCloseButton(
              glass: glass,
              tooltip: '关闭设置',
              onPressed: onClose,
            ),
          ],
        ),
      ),
    );
  }
}

class _ReaderSettingsSheetState extends State<_ReaderSettingsSheet> {
  late ReadingStyle _draft = widget.state.style;
  final _acceptInput = true;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: widget.state.appChanges,
      builder: (context, _) {
        final palette = widget.state.palette;
        final glass = _ReaderGlassPalette.from(palette);
        final style = _draft;
        return IgnorePointer(
          ignoring: !_acceptInput,
          child: Column(
            children: [
              _ReaderSettingsHeader(glass: glass, onClose: _close),
              Expanded(
                child: ListView(
                  padding: EdgeInsets.fromLTRB(
                    20,
                    18,
                    20,
                    22 + MediaQuery.viewInsetsOf(context).bottom,
                  ),
                  children: [
                    _ReaderSectionLabel(label: '常用微调', glass: glass),
                    const SizedBox(height: 10),
                    _ReaderSettingsCard(
                      glass: glass,
                      children: [
                        _SheetSlider(
                          label: '字号',
                          valueLabel: style.fontSize.toStringAsFixed(0),
                          value: style.fontSize.clamp(14, 32).toDouble(),
                          min: 14,
                          max: 32,
                          divisions: 18,
                          palette: palette,
                          onChanged: (value) =>
                              _preview(style.copyWith(fontSize: value)),
                          onChangeEnd: (_) => _commit(),
                        ),
                        const _ReaderCardDivider(),
                        _SheetSlider(
                          label: '行高',
                          valueLabel: style.lineHeight.toStringAsFixed(2),
                          value: style.lineHeight,
                          min: 1.2,
                          max: 2.6,
                          divisions: 14,
                          palette: palette,
                          onChanged: (value) =>
                              _preview(style.copyWith(lineHeight: value)),
                          onChangeEnd: (_) => _commit(),
                        ),
                        const _ReaderCardDivider(),
                        _SheetSlider(
                          label: '左右边距',
                          valueLabel: style.pageMargin.toStringAsFixed(0),
                          value: style.pageMargin,
                          min: 0,
                          max: 52,
                          divisions: 26,
                          palette: palette,
                          onChanged: (value) =>
                              _preview(style.copyWith(pageMargin: value)),
                          onChangeEnd: (_) => _commit(),
                        ),
                        const _ReaderCardDivider(),
                        _SheetSlider(
                          label: '上下边距',
                          valueLabel: style.verticalMargin.toStringAsFixed(0),
                          value: style.verticalMargin,
                          min: 4,
                          max: 96,
                          divisions: 24,
                          palette: palette,
                          onChanged: (value) =>
                              _preview(style.copyWith(verticalMargin: value)),
                          onChangeEnd: (_) => _commit(),
                        ),
                      ],
                    ),
                    const SizedBox(height: 18),
                    _ReaderSectionLabel(label: '阅读背景', glass: glass),
                    const SizedBox(height: 10),
                    _ReaderSettingsCard(
                      glass: glass,
                      children: [
                        Wrap(
                          spacing: 10,
                          runSpacing: 10,
                          children: [
                            for (final item in readerPalettes.values)
                              _ReaderChoicePill(
                                palette: palette,
                                glass: glass,
                                label: Text(item.label),
                                selected: style.readerBackground == item.id,
                                selectedColor: palette.primary,
                                backgroundColor: glass.pill,
                                labelStyle: TextStyle(
                                  color: style.readerBackground == item.id
                                      ? Colors.white
                                      : glass.muted,
                                  fontWeight: AppTextWeight.regular,
                                ),
                                onSelected: (_) {
                                  _preview(
                                    style.copyWith(readerBackground: item.id),
                                  );
                                  _commit();
                                },
                              ),
                          ],
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    _ReaderSettingsCard(
                      glass: glass,
                      padding: EdgeInsets.zero,
                      children: [
                        Theme(
                          data: Theme.of(
                            context,
                          ).copyWith(dividerColor: Colors.transparent),
                          child: ExpansionTile(
                            tilePadding: const EdgeInsets.fromLTRB(
                              16,
                              4,
                              12,
                              4,
                            ),
                            childrenPadding: const EdgeInsets.fromLTRB(
                              16,
                              0,
                              16,
                              16,
                            ),
                            iconColor: glass.text,
                            collapsedIconColor: glass.muted,
                            title: Text(
                              '更多细调',
                              style: TextStyle(
                                color: glass.text,
                                fontWeight: AppTextWeight.medium,
                              ),
                            ),
                            subtitle: Text(
                              '段距、字距、点击方向、缩进与书籍字体',
                              style: TextStyle(
                                color: glass.muted,
                                fontSize: 12,
                              ),
                            ),
                            children: [
                              _SheetSlider(
                                label: '段距',
                                valueLabel: style.paragraphSpacing
                                    .toStringAsFixed(0),
                                value: style.paragraphSpacing,
                                min: 0,
                                max: 30,
                                divisions: 15,
                                palette: palette,
                                onChanged: (value) => _preview(
                                  style.copyWith(paragraphSpacing: value),
                                ),
                                onChangeEnd: (_) => _commit(),
                              ),
                              const _ReaderCardDivider(),
                              _SheetSlider(
                                label: '字距',
                                valueLabel: style.letterSpacing.toStringAsFixed(
                                  1,
                                ),
                                value: style.letterSpacing,
                                min: 0,
                                max: 2,
                                divisions: 20,
                                palette: palette,
                                onChanged: (value) => _preview(
                                  style.copyWith(letterSpacing: value),
                                ),
                                onChangeEnd: (_) => _commit(),
                              ),
                              const _ReaderCardDivider(),
                              _ReaderInlineSwitch(
                                palette: palette,
                                glass: glass,
                                title: '反转点击翻页',
                                subtitle: '左右点击区域互换',
                                value: style.reverseTapPageTurn,
                                onChanged: (value) {
                                  _preview(
                                    style.copyWith(reverseTapPageTurn: value),
                                  );
                                  _commit();
                                },
                              ),
                              const _ReaderCardDivider(),
                              _ReaderInlineSwitch(
                                palette: palette,
                                glass: glass,
                                title: '首行缩进',
                                subtitle: '正文段落开头缩进两个汉字',
                                value: style.firstLineIndent,
                                onChanged: (value) {
                                  _preview(
                                    style.copyWith(firstLineIndent: value),
                                  );
                                  _commit();
                                },
                              ),
                              const SizedBox(height: 12),
                              FilledButton.icon(
                                style: FilledButton.styleFrom(
                                  backgroundColor: palette.primary,
                                  foregroundColor: Colors.white,
                                  minimumSize: const Size.fromHeight(46),
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(16),
                                  ),
                                ),
                                onPressed: () {
                                  widget.state.importReaderFont();
                                  widget.onChanged();
                                },
                                icon: const Icon(Icons.upload_file_rounded),
                                label: Text(
                                  style.fontName == null
                                      ? '导入书籍字体 .ttf / .otf'
                                      : '书籍字体：${style.fontName}',
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  void _preview(ReadingStyle style) {
    setState(() => _draft = style);
    unawaited(widget.state.updateStyle(style));
    widget.onChanged();
  }

  void _commit() {
    unawaited(widget.state.updateStyle(_draft, immediate: true));
    widget.onChanged();
  }

  void _close() {
    widget.onClose();
    SchedulerBinding.instance.addPostFrameCallback((_) {
      if (!mounted) {
        return;
      }
      _commit();
    });
  }
}

class _ReaderSectionLabel extends StatelessWidget {
  const _ReaderSectionLabel({required this.label, required this.glass});

  final String label;
  final _ReaderGlassPalette glass;

  @override
  Widget build(BuildContext context) {
    return Text(
      label,
      style: TextStyle(
        color: glass.muted,
        fontSize: 13,
        fontWeight: AppTextWeight.medium,
        letterSpacing: .4,
      ),
    );
  }
}

class _ReaderSettingsCard extends StatelessWidget {
  const _ReaderSettingsCard({
    required this.glass,
    required this.children,
    this.padding = const EdgeInsets.all(12),
  });

  final _ReaderGlassPalette glass;
  final List<Widget> children;
  final EdgeInsetsGeometry padding;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: glass.pill,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: glass.line.withValues(alpha: .42)),
      ),
      child: Padding(
        padding: padding,
        child: Column(mainAxisSize: MainAxisSize.min, children: children),
      ),
    );
  }
}

class _ReaderCardDivider extends StatelessWidget {
  const _ReaderCardDivider();

  @override
  Widget build(BuildContext context) {
    final glass = _ReaderGlassPalette.from(
      context.findAncestorStateOfType<_ReaderScreenState>()?.appPalette ??
          const ReadingStyle().palette,
    );
    return Divider(
      height: 12,
      thickness: 1,
      color: glass.line.withValues(alpha: .34),
    );
  }
}

class _ReaderInlineSwitch extends StatelessWidget {
  const _ReaderInlineSwitch({
    required this.palette,
    required this.glass,
    required this.title,
    required this.subtitle,
    required this.value,
    required this.onChanged,
  });

  final AppPalette palette;
  final _ReaderGlassPalette glass;
  final String title;
  final String subtitle;
  final bool value;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(16),
      onTap: () => onChanged(!value),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 6),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: TextStyle(
                      color: glass.text,
                      fontWeight: AppTextWeight.medium,
                    ),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    subtitle,
                    style: TextStyle(color: glass.muted, fontSize: 12),
                  ),
                ],
              ),
            ),
            Switch(
              value: value,
              activeTrackColor: palette.primary,
              activeThumbColor: palette.primarySoft,
              inactiveTrackColor: glass.line.withValues(alpha: .55),
              inactiveThumbColor: glass.muted,
              onChanged: onChanged,
            ),
          ],
        ),
      ),
    );
  }
}

class _ReaderChoicePill extends StatelessWidget {
  const _ReaderChoicePill({
    required this.palette,
    required this.glass,
    required this.label,
    required this.selected,
    required this.onSelected,
    this.selectedColor,
    this.backgroundColor,
    this.labelStyle,
  });

  final AppPalette palette;
  final _ReaderGlassPalette glass;
  final Widget label;
  final bool selected;
  final ValueChanged<bool> onSelected;
  final Color? selectedColor;
  final Color? backgroundColor;
  final TextStyle? labelStyle;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(999),
      onTap: () => onSelected(!selected),
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: selected
              ? (selectedColor ?? palette.primary)
              : (backgroundColor ?? glass.pill),
          borderRadius: BorderRadius.circular(999),
          border: Border.all(
            color: selected
                ? palette.primarySoft.withValues(alpha: glass.dark ? .55 : .42)
                : glass.line.withValues(alpha: .48),
          ),
          boxShadow: selected
              ? [
                  BoxShadow(
                    color: palette.primary.withValues(alpha: .20),
                    blurRadius: 16,
                    offset: const Offset(0, 6),
                  ),
                ]
              : null,
        ),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 9),
          child: DefaultTextStyle(
            style:
                labelStyle ??
                TextStyle(
                  color: selected ? Colors.white : glass.muted,
                  fontWeight: AppTextWeight.medium,
                ),
            child: label,
          ),
        ),
      ),
    );
  }
}

class _SheetSlider extends StatelessWidget {
  const _SheetSlider({
    required this.label,
    required this.valueLabel,
    required this.value,
    required this.min,
    required this.max,
    required this.divisions,
    required this.palette,
    required this.onChanged,
    required this.onChangeEnd,
  });

  final String label;
  final String valueLabel;
  final double value;
  final double min;
  final double max;
  final int divisions;
  final AppPalette palette;
  final ValueChanged<double> onChanged;
  final ValueChanged<double> onChangeEnd;

  @override
  Widget build(BuildContext context) {
    final glass = _ReaderGlassPalette.from(palette);
    final step = (max - min) / divisions;

    void nudge(int direction) {
      final raw = value + step * direction;
      final snapped = ((raw - min) / step).round() * step + min;
      final next = snapped.clamp(min, max).toDouble();
      if ((next - value).abs() < 0.001) {
        return;
      }
      HapticFeedback.selectionClick();
      onChanged(next);
      onChangeEnd(next);
    }

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Column(
        children: [
          Row(
            children: [
              Text(
                label,
                style: TextStyle(
                  color: glass.muted,
                  fontWeight: AppTextWeight.regular,
                ),
              ),
              const Spacer(),
              Text(
                valueLabel,
                textAlign: TextAlign.right,
                style: TextStyle(
                  color: glass.text,
                  fontSize: 16,
                  fontWeight: AppTextWeight.medium,
                ),
              ),
            ],
          ),
          const SizedBox(height: 1),
          Row(
            children: [
              _SheetStepButton(
                icon: Icons.remove_rounded,
                palette: palette,
                glass: glass,
                onTap: () => nudge(-1),
              ),
              Expanded(
                child: SliderTheme(
                  data: SliderTheme.of(context).copyWith(
                    trackHeight: 5,
                    activeTrackColor: palette.primary,
                    inactiveTrackColor: glass.line.withValues(alpha: .55),
                    thumbColor: palette.primarySoft,
                    overlayColor: palette.primary.withValues(alpha: .14),
                    thumbShape: const RoundSliderThumbShape(
                      enabledThumbRadius: 8,
                    ),
                    overlayShape: const RoundSliderOverlayShape(
                      overlayRadius: 15,
                    ),
                    tickMarkShape: SliderTickMarkShape.noTickMark,
                  ),
                  child: Slider(
                    value: value,
                    min: min,
                    max: max,
                    divisions: divisions,
                    onChanged: onChanged,
                    onChangeEnd: onChangeEnd,
                  ),
                ),
              ),
              _SheetStepButton(
                icon: Icons.add_rounded,
                palette: palette,
                glass: glass,
                onTap: () => nudge(1),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _SheetStepButton extends StatelessWidget {
  const _SheetStepButton({
    required this.icon,
    required this.palette,
    required this.glass,
    required this.onTap,
  });

  final IconData icon;
  final AppPalette palette;
  final _ReaderGlassPalette glass;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 36,
      height: 36,
      child: IconButton.filledTonal(
        style: IconButton.styleFrom(
          backgroundColor: palette.surface,
          foregroundColor: glass.muted,
          padding: EdgeInsets.zero,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
            side: BorderSide(color: glass.line.withValues(alpha: .5)),
          ),
        ),
        onPressed: onTap,
        icon: Icon(icon, size: 19),
      ),
    );
  }
}

class _MissingChapter extends StatelessWidget {
  const _MissingChapter({required this.readerPalette});

  final ReaderPalette readerPalette;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Text(
        '没有可读取的章节',
        style: TextStyle(
          color: readerPalette.text,
          fontSize: 18,
          fontWeight: AppTextWeight.regular,
        ),
      ),
    );
  }
}
