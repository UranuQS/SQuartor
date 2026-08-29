import 'dart:async';
import 'dart:io';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:path/path.dart' as path;

import '../app_state.dart';
import '../models.dart';
import 'reader_enums.dart';
import 'reader_panel_scrim.dart';
import 'reader_scroll_edge.dart';
import 'reader_floating_panel.dart';
import 'reader_footer.dart';
import 'reader_txt_view.dart';
import 'reader_menu.dart';
import 'reader_toc.dart';
import 'reader_settings.dart';
import 'reader_state_fields.dart';
import 'reader_epub_mixin.dart';
import 'reader_txt_mixin.dart';
import 'reader_navigation_mixin.dart';
import 'reader_gesture_mixin.dart';
import 'reader_bookmark_mixin.dart';
import 'reader_bookmark_pull.dart';
import 'reader_overlay_mixin.dart';
import 'reader_time_mixin.dart';

class ReaderScreen extends StatefulWidget implements ReaderScreenWidget {
  const ReaderScreen({super.key, required this.state, required this.book});

  static const routeName = '/reader';

  @override
  final AppState state;
  @override
  final BookEntry book;

  @override
  State<ReaderScreen> createState() => ReaderScreenState();
}

class ReaderScreenState extends State<ReaderScreen>
    with
        TickerProviderStateMixin,
        WidgetsBindingObserver,
        ReaderStateFields<ReaderScreen>,
        ReaderEpubMixin<ReaderScreen>,
        ReaderTxtMixin<ReaderScreen>,
        ReaderNavigationMixin<ReaderScreen>,
        ReaderGestureMixin<ReaderScreen>,
        ReaderBookmarkMixin<ReaderScreen>,
        ReaderOverlayMixin<ReaderScreen>,
        ReaderTimeMixin<ReaderScreen> {
  String get _flutterChapterContentKey => [
    readerNavigationToken,
    txtPaginationSignature ?? 'loading',
    usesVerticalScroll ? 'scroll' : 'paged',
    isLoading ? 'loading' : 'ready',
  ].join('|');

  static const _tocBookmarkModeSpacing = 96.0;
  static const Curve _tocBookmarkSettleCurve = Cubic(0.16, 1.0, 0.30, 1.0);

  double get tocBookmarkBasePosition => tocShowsBookmarks ? 1.0 : 0.0;

  double get tocBookmarkVisualPosition =>
      (tocBookmarkModePosition ?? tocBookmarkBasePosition)
          .clamp(-0.18, 1.18)
          .toDouble();

  void _restoreSystemUi() {
    SystemChrome.setEnabledSystemUIMode(
      SystemUiMode.manual,
      overlays: SystemUiOverlay.values,
    );
    SystemChrome.setSystemUIOverlayStyle(
      SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        systemNavigationBarColor: appPalette.background,
        systemNavigationBarDividerColor: Colors.transparent,
        statusBarIconBrightness: appPalette.isLight
            ? Brightness.dark
            : Brightness.light,
        systemNavigationBarIconBrightness: appPalette.isLight
            ? Brightness.dark
            : Brightness.light,
      ),
    );
    WidgetsBinding.instance.addPostFrameCallback((_) {
      SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    });
  }

  void updateTocBookmarkDrag(double delta) {
    tocBookmarkSettleAnimation.stop();
    tocBookmarkSettle = null;
    final current = tocBookmarkModePosition ?? tocBookmarkBasePosition;
    final next = (current - delta / _tocBookmarkModeSpacing)
        .clamp(-0.18, 1.18)
        .toDouble();
    tocBookmarkModePosition = next;
    tocBookmarkVisualNotifier.value = tocBookmarkVisualPosition;
  }

  @override
  void animateTocBookmarkModeTo(bool showBookmarks) {
    final target = showBookmarks ? 1 : 0;
    final current = tocBookmarkVisualPosition.clamp(0.0, 1.0).toDouble();
    settleTocBookmarkDrag(target: target, from: current);
  }

  void settleTocBookmarkDrag({required int target, required double from}) {
    tocBookmarkSettleAnimation.stop();
    final start = from.clamp(-0.18, 1.18).toDouble();
    final end = target.toDouble();
    final distance = (end - start).abs();
    if (distance <= 0.001) {
      setState(() {
        tocBookmarkModePosition = null;
        tocBookmarkLastHapticTick = null;
        tocShowsBookmarks = target == 1;
      });
      tocBookmarkVisualNotifier.value = tocBookmarkVisualPosition;
      return;
    }
    tocBookmarkSettle = Tween<double>(begin: start, end: end).animate(
      CurvedAnimation(
        parent: tocBookmarkSettleAnimation,
        curve: _tocBookmarkSettleCurve,
      ),
    );
    tocBookmarkModePosition = start;
    tocBookmarkVisualNotifier.value = tocBookmarkVisualPosition;
    tocBookmarkSettleAnimation
      ..duration = Duration(
        milliseconds: (160 + distance * 180).clamp(170, 320).round(),
      )
      ..forward(from: 0).whenComplete(() {
        if (!mounted) {
          return;
        }
        tocBookmarkModePosition = null;
        tocBookmarkLastHapticTick = null;
        tocShowsBookmarks = target == 1;
        tocBookmarkVisualNotifier.value = tocBookmarkVisualPosition;
      });
  }

  void endTocBookmarkDrag(DragEndDetails details) {
    final current = tocBookmarkModePosition ?? tocBookmarkBasePosition;
    final velocityModes =
        details.velocity.pixelsPerSecond.dx / _tocBookmarkModeSpacing;
    final projected = (current - velocityModes * .045)
        .clamp(-0.18, 1.18)
        .toDouble();
    final target = projected.clamp(0.0, 1.0).round().clamp(0, 1).toInt();
    settleTocBookmarkDrag(target: target, from: current);
  }

  void cancelTocBookmarkDrag() {
    final current = tocBookmarkModePosition;
    if (current == null) {
      return;
    }
    settleTocBookmarkDrag(
      target: tocBookmarkBasePosition.round().clamp(0, 1).toInt(),
      from: current,
    );
  }

  @override
  void initState() {
    super.initState();
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
    readerBook = widget.book;
    chromeAnimation = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 260),
      value: 1,
    );
    tocAnimation = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 220),
    );
    settingsAnimation = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 220),
    );
    footerAnimation = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 240),
    );
    bookmarkPullReturnAnimation = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 360),
    );
    bookmarkPullReturnAnimation.addListener(() {
      if (mounted) {
        setState(() {});
      }
    });
    tocBookmarkSettleAnimation = AnimationController(vsync: this)
      ..addListener(() {
        final animation = tocBookmarkSettle;
        if (animation == null || !mounted) {
          return;
        }
        tocBookmarkModePosition = animation.value;
        tocBookmarkVisualNotifier.value = tocBookmarkVisualPosition;
      });
    chapterIndex = widget.book.currentChapterIndex.clamp(0, lastChapterIndex);
    final savedPageCount = widget.book.pageCount < 1
        ? 1
        : widget.book.pageCount;
    page = widget.book.currentPage.clamp(0, savedPageCount - 1);
    pageCount = savedPageCount;
    txtScrollController = ScrollController();
    pendingExactPage = page;
    pendingExactPageCount = savedPageCount;
    pendingPageProgress = savedPageCount <= 1 ? 0 : page / (savedPageCount - 1);
    widget.state.settingsChanges.addListener(onReaderStyleChanged);
    WidgetsBinding.instance.addObserver(this);
    readingStopwatch.start();
    readingTimer = Timer.periodic(
      const Duration(seconds: 30),
      (_) => flushReadingTime(),
    );
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    super.didChangeAppLifecycleState(state);
    switch (state) {
      case AppLifecycleState.resumed:
        if (!readingStopwatch.isRunning) {
          readingStopwatch.reset();
          readingStopwatch.start();
        }
        break;
      case AppLifecycleState.inactive:
      case AppLifecycleState.paused:
      case AppLifecycleState.hidden:
      case AppLifecycleState.detached:
        flushReadingTime();
        readingStopwatch.stop();
        readingStopwatch.reset();
        break;
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _restoreSystemUi();
    flushReadingTime();
    // Flush current reading position to disk immediately so progress is
    // never lost even if the user exits the reader very quickly.
    unawaited(
      appState.updateBookProgress(
        book: book,
        chapterIndex: chapterIndex,
        page: page,
        pageCount: pageCount,
        displayProgress: overallProgress,
        force: true,
      ),
    );
    widget.state.settingsChanges.removeListener(onReaderStyleChanged);
    readingTimer?.cancel();
    styleInjectTimer?.cancel();
    txtPaginationTimer?.cancel();
    progressSeekTimer?.cancel();
    webEdgeTurnResetTimer?.cancel();
    txtScrollController.dispose();
    readerSnapshotImage?.evict();
    controller = null;
    chromeAnimation.dispose();
    tocAnimation.dispose();
    settingsAnimation.dispose();
    footerAnimation.dispose();
    bookmarkPullReturnAnimation.dispose();
    tocBookmarkSettleAnimation.dispose();
    widget.state.refreshLibraryViews();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final chapter = currentChapter;
    return AnimatedBuilder(
      animation: widget.state.readerChanges,
      builder: (context, _) {
        final systemPadding = MediaQuery.viewPaddingOf(context);
        final readerDockBottom = systemPadding.bottom < 8
            ? 14.0
            : (systemPadding.bottom * .65 + 10).clamp(24.0, 34.0).toDouble();
        return PopScope(
          canPop: !isSideOverlay(overlay),
          onPopInvokedWithResult: (didPop, _) {
            if (!didPop && isSideOverlay(overlay)) {
              hideOverlay();
            }
          },
          child: Scaffold(
            backgroundColor: readerPalette.background,
            body: SizedBox.expand(
              child: Stack(
                fit: StackFit.expand,
                children: [
                  Positioned.fill(
                    child: AnimatedBuilder(
                      animation: bookmarkPullReturnAnimation,
                      builder: (context, child) {
                        return Transform.translate(
                          offset: Offset(0, readerContentPullOffset()),
                          child: child,
                        );
                      },
                      child: usesFlutterTxt
                          ? AnimatedSwitcher(
                              duration: const Duration(milliseconds: 180),
                              reverseDuration: const Duration(
                                milliseconds: 120,
                              ),
                              switchInCurve: Curves.easeOutCubic,
                              switchOutCurve: Curves.easeInCubic,
                              transitionBuilder: (child, animation) {
                                return FadeTransition(
                                  opacity: animation,
                                  child: child,
                                );
                              },
                              child: KeyedSubtree(
                                key: ValueKey(_flutterChapterContentKey),
                                child: buildFlutterTxtContent(
                                  chapter,
                                  systemPadding,
                                ),
                              ),
                            )
                          : chapter.filePath.isEmpty
                          ? MissingChapter(readerPalette: readerPalette)
                          : Opacity(
                              opacity: readerSnapshotImage != null ? 0 : 1,
                              child: IgnorePointer(
                                ignoring: overlay != ReaderOverlay.hidden,
                                child: InAppWebView(
                                  key: ValueKey('reader-${book.id}'),
                                  initialUrlRequest:
                                      chapter.filePath.startsWith('sq-') ||
                                              !File(
                                                chapter.filePath,
                                              ).existsSync()
                                          ? null
                                          : URLRequest(
                                              url: WebUri.uri(
                                                File(chapter.filePath).uri,
                                              ),
                                            ),
                                  initialSettings: InAppWebViewSettings(
                                    javaScriptEnabled: true,
                                    transparentBackground: false,
                                    useHybridComposition: true,
                                    useShouldOverrideUrlLoading: true,
                                    allowFileAccess: true,
                                    allowFileAccessFromFileURLs: true,
                                    allowUniversalAccessFromFileURLs: true,
                                    disableVerticalScroll: false,
                                    disableHorizontalScroll: true,
                                    supportZoom: false,
                                  ),
                                  onWebViewCreated: (ctrl) {
                                    readerLog(
                                      'webview created chapter=$chapterIndex file=${chapter.filePath}',
                                    );
                                    controller = ctrl;
                                    ctrl.addJavaScriptHandler(
                                      handlerName: 'squartorEvent',
                                      callback: handleReaderEvent,
                                    );
                                    if (chapter.filePath.startsWith('sq-') ||
                                        !File(chapter.filePath).existsSync()) {
                                      unawaited(loadCurrentWebViewChapter());
                                    }
                                  },
                                  onLoadStop: (ctrl, url) async {
                                    readerLog('webview loadStop url=$url');
                                    if (url == null || !url.isScheme('file')) {
                                      return;
                                    }
                                    final loadedPath = path.normalize(
                                      url.uriValue.toFilePath(),
                                    );
                                    final currentChapterPath = path.normalize(
                                      currentChapter.filePath,
                                    );
                                    if (loadedPath != currentChapterPath) {
                                      readerLog(
                                        'drop stale webview loadStop loaded=$loadedPath current=$currentChapterPath',
                                      );
                                      return;
                                    }
                                    if (pendingWebLoadPath != null &&
                                        pendingWebLoadPath != loadedPath) {
                                      readerLog(
                                        'drop unexpected webview loadStop loaded=$loadedPath pending=$pendingWebLoadPath',
                                      );
                                      return;
                                    }
                                    if (url.fragment.isNotEmpty == true) {
                                      pendingAnchor = decodeLooseUriComponent(
                                        url.fragment,
                                      );
                                    }
                                    try {
                                      await injectReaderStyle();
                                    } catch (error) {
                                      readerLog('inject failed $error');
                                      debugPrint(
                                        'SQuartor inject style failed: $error',
                                      );
                                    }
                                    if (mounted) {
                                      setState(() {
                                        pendingWebLoadPath = null;
                                        isLoading = false;
                                        loadError = null;
                                      });
                                      flushPendingProgressSeek();
                                    }
                                  },
                                  onReceivedError: (ctrl, request, error) {
                                    readerLog(
                                      'webview error main=${request.isForMainFrame} ${error.description}',
                                    );
                                    if (request.isForMainFrame == true &&
                                        mounted) {
                                      setState(() {
                                        isLoading = false;
                                        loadError = error.description;
                                        overlay = ReaderOverlay.chrome;
                                      });
                                    }
                                  },
                                  onConsoleMessage: (ctrl, message) {
                                    debugPrint(
                                      'SQuartor WebView: ${message.message}',
                                    );
                                  },
                                  shouldOverrideUrlLoading: (ctrl, action) async {
                                    final url = action.request.url;
                                    if (url == null) {
                                      return NavigationActionPolicy.ALLOW;
                                    }
                                    if (isExternalUriString(url.toString())) {
                                      unawaited(
                                        openExternalLink(
                                          Uri.parse(url.toString()),
                                        ),
                                      );
                                      if (context.mounted &&
                                          !isExternalUriString(url.toString())) {
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
                    ),
                  ),
                  if (readerSnapshotImage case final image?)
                    Positioned.fill(
                      child: AnimatedBuilder(
                        animation: bookmarkPullReturnAnimation,
                        builder: (context, child) {
                          return Transform.translate(
                            offset: Offset(0, readerContentPullOffset()),
                            child: child,
                          );
                        },
                        child: Image(
                          image: image,
                          fit: BoxFit.cover,
                          filterQuality: FilterQuality.low,
                        ),
                      ),
                    ),
                  if (!usesFlutterTxt)
                    Positioned.fill(
                      child: IgnorePointer(
                        ignoring: overlay != ReaderOverlay.hidden,
                        child: GestureDetector(
                          behavior: HitTestBehavior.translucent,
                          onTapUp: onReaderTap,
                          onLongPressStart: onReaderLongPress,
                          onVerticalDragStart: (details) => onBookmarkPullStart(
                            details,
                            MediaQuery.sizeOf(context),
                          ),
                          onVerticalDragUpdate: onBookmarkPullUpdate,
                          onVerticalDragEnd: onBookmarkPullEnd,
                          onVerticalDragCancel: onBookmarkPullCancel,
                          onHorizontalDragStart: onReaderDragStart,
                          onHorizontalDragUpdate: onReaderDragUpdate,
                          onHorizontalDragEnd: onReaderDragEnd,
                          onHorizontalDragCancel: onReaderDragCancel,
                        ),
                      ),
                    ),
                  if (isLoading)
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
                  if (loadError != null)
                    Positioned.fill(
                      child: ReaderStatusOverlay(
                        readerPalette: readerPalette,
                        message: '加载失败：_loadError',
                      ),
                    ),
                  Positioned(
                    left: 20,
                    right: 20,
                    top: systemPadding.top + 16,
                    child: IgnorePointer(
                      ignoring: overlay != ReaderOverlay.chrome,
                      child: AnimatedBuilder(
                        animation: chromeAnimation,
                        builder: (context, child) {
                          final t = overlaySlideProgress(chromeAnimation);
                          return FractionalTranslation(
                            translation: Offset(0, -2.35 * (1 - t)),
                            child: child,
                          );
                        },
                        child: TopMenu(
                          title: chapter.title,
                          palette: readerPalette,
                          appPalette: appPalette,
                          onBack: () => Navigator.of(context).pop(),
                        ),
                      ),
                    ),
                  ),
                  if (!usesVerticalScroll)
                    Positioned(
                      left: 16,
                      right: 16,
                      bottom: systemPadding.bottom + 18,
                      child: IgnorePointer(
                        child: AnimatedBuilder(
                          animation: footerAnimation,
                          builder: (context, child) {
                            return Opacity(
                              opacity: Curves.easeOut.transform(
                                footerAnimation.value,
                              ),
                              child: child,
                            );
                          },
                          child: ReaderFooter(
                            chapter: chapterIndex + 1,
                            chapterCount: book.chapters.length,
                            page: page + 1,
                            pageCount: pageCount,
                            progress: overallProgress,
                            palette: readerPalette,
                          ),
                        ),
                      ),
                    ),
                  ReaderPanelScrim(
                    visible:
                        overlay == ReaderOverlay.toc ||
                        overlay == ReaderOverlay.settings ||
                        sideOverlayDismissing,
                    palette: appPalette,
                    onDismiss: hideOverlay,
                  ),
                  FootnotePopupOverlay(
                    data: footnotePopup,
                    palette: appPalette,
                    readerPalette: readerPalette,
                    onDismiss: hideFootnote,
                  ),
                  ReaderBookmarkPullOverlay(
                    pullDy: bookmarkPullDy,
                    active: bookmarkPullActive,
                    committed: bookmarkPullCommitted,
                    hasBookmark: currentPageBookmarked,
                    palette: appPalette,
                    readerPalette: readerPalette,
                    systemPadding: systemPadding,
                  ),
                  if (usesVerticalScroll &&
                      scrollEdgeTurnDirection != null &&
                      scrollEdgeTurnProgress > 0)
                    ScrollEdgeTurnHintPositioned(
                      direction: scrollEdgeTurnDirection!,
                      progress: scrollEdgeTurnProgress,
                      readerPalette: readerPalette,
                      palette: appPalette,
                      systemPadding: systemPadding,
                    ),
                  Positioned.fill(
                    child: IgnorePointer(
                      ignoring: overlay != ReaderOverlay.toc,
                      child: LayoutBuilder(
                        builder: (context, constraints) {
                          final panelWidth =
                              (constraints.maxWidth -
                                      systemPadding.left -
                                      systemPadding.right -
                                      32)
                                  .clamp(300.0, 760.0)
                                  .toDouble();
                          final availableHeight =
                              (constraints.maxHeight -
                                      systemPadding.top -
                                      systemPadding.bottom -
                                      132)
                                  .clamp(360.0, double.infinity)
                                  .toDouble();
                          final panelHeight = (availableHeight * .72)
                              .clamp(390.0, 720.0)
                              .toDouble();
                          Widget buildTocPanelCard(
                            bool showBookmarks, {
                            double blurSigma = 34,
                            bool transparent = false,
                          }) {
                            return FloatingPanelSurface(
                              key: ValueKey(
                                showBookmarks
                                    ? 'toc-card-bookmarks-$chapterIndex'
                                    : 'toc-card-chapters-$chapterIndex',
                              ),
                              palette: appPalette,
                              blurSigma: blurSigma,
                              transparent: transparent,
                              child: ReaderTocDrawer(
                                book: book,
                                chapterIndex: chapterIndex,
                                showBookmarks: showBookmarks,
                                currentPageCount: pageCount,
                                cachedPageCounts: const <int, int>{},
                                palette: appPalette,
                                onChapterSelected: (index) {
                                  unawaited(goToChapter(index));
                                  hideOverlay();
                                },
                                onBookmarkSelected: (bookmark) {
                                  final progress = bookmark.pageCount <= 1
                                      ? bookmark.progress
                                      : bookmark.page /
                                            (bookmark.pageCount - 1);
                                  unawaited(
                                    goToChapter(
                                      bookmark.chapterIndex,
                                      progress: progress.clamp(0.0, 1.0),
                                    ),
                                  );
                                  hideOverlay();
                                },
                              ),
                            );
                          }

                          final tocBookmarkPanelGap = (panelWidth * .08)
                              .clamp(22.0, 44.0)
                              .toDouble();
                          final tocBookmarkPanelTravel =
                              panelWidth + tocBookmarkPanelGap;
                          final tocCard = RepaintBoundary(
                            child: SizedBox.expand(
                              child: buildTocPanelCard(
                                false,
                                blurSigma: 0,
                                transparent: true,
                              ),
                            ),
                          );
                          final bookmarkCard = RepaintBoundary(
                            child: SizedBox.expand(
                              child: buildTocPanelCard(
                                true,
                                blurSigma: 0,
                                transparent: true,
                              ),
                            ),
                          );

                          return AnimatedBuilder(
                            animation: tocAnimation,
                            builder: (context, child) {
                              final raw = tocAnimation.value;
                              if (raw <= 0.001 &&
                                  overlay != ReaderOverlay.toc) {
                                return const SizedBox.shrink();
                              }
                              final t = overlaySlideProgress(tocAnimation);
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
                                  16 + systemPadding.left,
                                  44 + systemPadding.top,
                                  16 + systemPadding.right,
                                  116 + systemPadding.bottom,
                                ),
                                child: SizedBox(
                                  width: panelWidth,
                                  height: panelHeight,
                                  child: RepaintBoundary(
                                    child: FloatingPanelSurface(
                                      palette: appPalette,
                                      blurSigma: 12,
                                      child: ValueListenableBuilder<double>(
                                        valueListenable:
                                            tocBookmarkVisualNotifier,
                                        builder: (context, value, _) {
                                          final pos = value.clamp(0.0, 1.0);
                                          return ClipRect(
                                            child: Stack(
                                              fit: StackFit.expand,
                                              clipBehavior: Clip.hardEdge,
                                              children: [
                                                Transform.translate(
                                                  offset: Offset(
                                                    -pos *
                                                        tocBookmarkPanelTravel,
                                                    0,
                                                  ),
                                                  child: IgnorePointer(
                                                    ignoring: pos > .55,
                                                    child: tocCard,
                                                  ),
                                                ),
                                                Transform.translate(
                                                  offset: Offset(
                                                    (1 - pos) *
                                                        tocBookmarkPanelTravel,
                                                    0,
                                                  ),
                                                  child: IgnorePointer(
                                                    ignoring: pos < .45,
                                                    child: bookmarkCard,
                                                  ),
                                                ),
                                              ],
                                            ),
                                          );
                                        },
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
                  Positioned.fill(
                    child: IgnorePointer(
                      ignoring: overlay != ReaderOverlay.settings,
                      child: LayoutBuilder(
                        builder: (context, constraints) {
                          final panelWidth =
                              (constraints.maxWidth -
                                      systemPadding.left -
                                      systemPadding.right -
                                      32)
                                  .clamp(300.0, 760.0)
                                  .toDouble();
                          final availableHeight =
                              (constraints.maxHeight -
                                      systemPadding.top -
                                      systemPadding.bottom -
                                      132)
                                  .clamp(360.0, double.infinity)
                                  .toDouble();
                          final panelHeight = (availableHeight * .72)
                              .clamp(390.0, 720.0)
                              .toDouble();
                          return AnimatedBuilder(
                            animation: settingsAnimation,
                            builder: (context, child) {
                              final raw = settingsAnimation.value;
                              if (raw <= 0.001 &&
                                  overlay != ReaderOverlay.settings) {
                                return const SizedBox.shrink();
                              }
                              final t = overlaySlideProgress(settingsAnimation);
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
                                  16 + systemPadding.left,
                                  44 + systemPadding.top,
                                  16 + systemPadding.right,
                                  116 + systemPadding.bottom,
                                ),
                                child: SizedBox(
                                  width: panelWidth,
                                  height: panelHeight,
                                  child: RepaintBoundary(
                                    child: Listener(
                                      behavior: HitTestBehavior.opaque,
                                      onPointerSignal: (_) {},
                                      child: FloatingPanelSurface(
                                        palette: appPalette,
                                        blurSigma: 12,
                                        child: ReaderSettingsSheet(
                                          state: widget.state,
                                          onChanged: scheduleStyleInjection,
                                        ),
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
                  Positioned(
                    left: 16,
                    right: 16,
                    bottom: readerDockBottom,
                    child: IgnorePointer(
                      ignoring:
                          overlay != ReaderOverlay.chrome &&
                          !isSideOverlay(overlay),
                      child: AnimatedBuilder(
                        animation: Listenable.merge([
                          chromeAnimation,
                          tocAnimation,
                          settingsAnimation,
                          tocBookmarkVisualNotifier,
                        ]),
                        builder: (context, child) {
                          final sideActive =
                              isSideOverlay(overlay) ||
                              tocAnimation.value > 0 ||
                              settingsAnimation.value > 0 ||
                              sideOverlayDismissing;
                          final sideValue = math.max(
                            tocAnimation.value,
                            settingsAnimation.value,
                          );
                          final t = chromeReturningFromSide
                              ? 1.0
                              : sideOverlayDismissing
                              ? Curves.easeOutCubic.transform(sideValue)
                              : sideActive
                              ? 1.0
                              : overlaySlideProgress(chromeAnimation);
                          return FractionalTranslation(
                            translation: Offset(0, 2.1 * (1 - t)),
                            child: Opacity(
                              opacity: Curves.easeOut.transform(t),
                              child: AdaptiveBottomMenu(
                                page: page,
                                pageCount: pageCount,
                                progress: overallProgress,
                                overlay: overlay,
                                tocShowsBookmarks: tocShowsBookmarks,
                                tocBookmarkModePosition:
                                    overlay == ReaderOverlay.toc
                                    ? tocBookmarkVisualPosition
                                    : 0,
                                currentChapter: chapterIndex,
                                chapterCount: book.chapters.length,
                                tocProgress: tocAnimation,
                                settingsProgress: settingsAnimation,
                                sideOverlayDismissing: sideOverlayDismissing,
                                readerPalette: readerPalette,
                                appPalette: appPalette,
                                onToc: showToc,
                                onTocModeDragUpdate: updateTocBookmarkDrag,
                                onTocModeDragEnd: endTocBookmarkDrag,
                                onTocModeDragCancel: cancelTocBookmarkDrag,
                                onPreviousChapter: () =>
                                    goToChapter(chapterIndex - 1, atEnd: true),
                                onNextChapter: () =>
                                    goToChapter(chapterIndex + 1),
                                onProgressChapterSeek:
                                    requestProgressSeekToChapter,
                                onProgressScrubStart: cancelPendingProgressSeek,
                                onSettings: showSettings,
                                onProgressPressed: () {
                                  if (isSideOverlay(overlay)) {
                                    returnToChromeFromSideOverlay();
                                  }
                                },
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
          ),
        );
      },
    );
  }
}
