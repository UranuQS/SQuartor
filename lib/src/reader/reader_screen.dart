import 'dart:async';
import 'dart:io';
import 'dart:math' as math;

import 'package:flutter/material.dart';
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
        ReaderStateFields<ReaderScreen>,
        ReaderEpubMixin<ReaderScreen>,
        ReaderTxtMixin<ReaderScreen>,
        ReaderNavigationMixin<ReaderScreen>,
        ReaderGestureMixin<ReaderScreen>,
        ReaderOverlayMixin<ReaderScreen>,
        ReaderTimeMixin<ReaderScreen> {
  String get _flutterChapterContentKey => [
    readerNavigationToken,
    txtPaginationSignature ?? 'loading',
    usesVerticalScroll ? 'scroll' : 'paged',
    isLoading ? 'loading' : 'ready',
  ].join('|');

  @override
  void initState() {
    super.initState();
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
    readingStopwatch.start();
    readingTimer = Timer.periodic(
      const Duration(seconds: 30),
      (_) => flushReadingTime(),
    );
    clockTimer = Timer.periodic(const Duration(minutes: 1), (_) {
      if (mounted) {
        setState(() => now = DateTime.now());
      }
    });
  }

  @override
  void dispose() {
    flushReadingTime();
    widget.state.settingsChanges.removeListener(onReaderStyleChanged);
    readingTimer?.cancel();
    styleInjectTimer?.cancel();
    txtPaginationTimer?.cancel();
    progressSeekTimer?.cancel();
    clockTimer?.cancel();
    webEdgeTurnResetTimer?.cancel();
    txtScrollController.dispose();
    readerSnapshotImage?.evict();
    controller = null;
    chromeAnimation.dispose();
    tocAnimation.dispose();
    settingsAnimation.dispose();
    footerAnimation.dispose();
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
                    child: usesFlutterTxt
                        ? AnimatedSwitcher(
                            duration: const Duration(milliseconds: 180),
                            reverseDuration: const Duration(milliseconds: 120),
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
                                if (request.isForMainFrame == true && mounted) {
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
                                    openExternalLink(Uri.parse(url.toString())),
                                  );
                                  if (context.mounted &&
                                      !isExternalUriString(url.toString())) {
                                    ScaffoldMessenger.of(context).showSnackBar(
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
                  if (readerSnapshotImage case final image?)
                    Positioned.fill(
                      child: Image(
                        image: image,
                        fit: BoxFit.cover,
                        filterQuality: FilterQuality.low,
                      ),
                    ),
                  if (!usesFlutterTxt)
                    Positioned.fill(
                      child: GestureDetector(
                        behavior: HitTestBehavior.translucent,
                        onTapUp: onReaderTap,
                        onLongPressStart: onReaderLongPress,
                        onHorizontalDragStart: onReaderDragStart,
                        onHorizontalDragUpdate: onReaderDragUpdate,
                        onHorizontalDragEnd: onReaderDragEnd,
                        onHorizontalDragCancel: onReaderDragCancel,
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
                      bottom: systemPadding.bottom + 8,
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
                            now: now,
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
                  if (usesVerticalScroll &&
                      !usesFlutterTxt &&
                      webEdgeTurnProgress > 0)
                    ScrollEdgeTurnHintPositioned(
                      direction: webEdgeTurnDirection == 'previous'
                          ? ScrollEdgeTurnDirection.previous
                          : ScrollEdgeTurnDirection.next,
                      progress: webEdgeTurnProgress,
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
                                      child: ReaderTocDrawer(
                                        key: ValueKey('toc-$chapterIndex'),
                                        book: book,
                                        chapterIndex: chapterIndex,
                                        currentPageCount: pageCount,
                                        cachedPageCounts: const <int, int>{},
                                        palette: appPalette,
                                        onChapterSelected: (index) {
                                          unawaited(goToChapter(index));
                                          hideOverlay();
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
                                    child: FloatingPanelSurface(
                                      palette: appPalette,
                                      child: ReaderSettingsSheet(
                                        state: widget.state,
                                        onChanged: scheduleStyleInjection,
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
                                currentChapter: chapterIndex,
                                chapterCount: book.chapters.length,
                                tocProgress: tocAnimation,
                                settingsProgress: settingsAnimation,
                                sideOverlayDismissing: sideOverlayDismissing,
                                readerPalette: readerPalette,
                                appPalette: appPalette,
                                onToc: showToc,
                                onPreviousChapter: () =>
                                    goToChapter(chapterIndex - 1, atEnd: true),
                                onNextChapter: () =>
                                    goToChapter(chapterIndex + 1),
                                onProgressSeek: seekToOverallProgress,
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
