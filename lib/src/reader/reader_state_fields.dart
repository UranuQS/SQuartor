import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:html/dom.dart' as dom;
import 'package:path/path.dart' as path;

import '../app_state.dart';
import '../models.dart';
import 'reader_enums.dart';
import 'reader_txt_view.dart';

/// Interface that the ReaderScreen widget must implement.
/// This allows the mixins to access widget properties without
/// importing the concrete widget class (avoiding circular imports).
abstract interface class ReaderScreenWidget implements StatefulWidget {
  AppState get state;
  BookEntry get book;
}

// --- State fields mixin ---

mixin ReaderStateFields<T extends ReaderScreenWidget> on State<T> {
  // --- Instance fields ---

  InAppWebViewController? controller;
  late int chapterIndex;
  var page = 0;
  var pageCount = 1;
  var overlay = ReaderOverlay.chrome;
  var isLoading = true;
  var sideOverlayDismissing = false;
  var chromeReturningFromSide = false;
  String? loadError;
  String? pendingAnchor;
  var pendingWebJumpToEnd = false;
  var readerNavigationToken = 0;
  String? pendingWebLoadPath;
  Timer? readingTimer;
  Timer? styleInjectTimer;
  Timer? txtPaginationTimer;
  Timer? progressSeekTimer;
  Timer? clockTimer;
  var overlayTransitionSerial = 0;
  var progressSeekSerial = 0;
  int? pendingProgressSeekChapter;
  late final AnimationController chromeAnimation;
  late final AnimationController tocAnimation;
  late final AnimationController settingsAnimation;
  late final AnimationController footerAnimation;
  MemoryImage? readerSnapshotImage;
  Uint8List? readerSnapshotBytes;
  var now = DateTime.now();
  final readingStopwatch = Stopwatch();
  double dragDx = 0;
  double dragDy = 0;
  var pageDragActive = false;
  var dragMoveScheduled = false;
  var dragSession = 0;
  var linkTapHandled = false;
  double? pendingPageProgress;
  int? pendingExactPage;
  int? pendingExactPageCount;
  late final ScrollController txtScrollController;
  List<FlutterTxtPage> txtPages = const [];
  List<FlutterTxtBlock> txtScrollBlocks = const [];
  TxtLayoutMetrics? txtLayoutMetrics;
  String? txtPaginationSignature;
  String? txtRequestedSignature;
  String? flutterReaderFontFamily;
  double txtScrollInitialProgress = 0;
  var txtWaitingForLayout = false;
  var txtLayoutRetryScheduled = false;
  FootnotePopupData? footnotePopup;
  String? webEdgeTurnDirection;
  double webEdgeTurnProgress = 0;
  Timer? webEdgeTurnResetTimer;
  final Set<String> epubWebViewFallbackChapters = {};
  final Set<String> registeredReaderFontFamilies = {};

  // --- Accessors for widget properties ---

  BookEntry get book => widget.book;
  AppState get appState => widget.state;

  ReadingStyle get style => appState.style;
  AppPalette get appPalette => appState.palette;

  ReaderPalette get readerPalette {
    if (style.readerBackground == ReaderBackgroundId.theme) {
      return ReaderPalette(
        id: ReaderBackgroundId.theme,
        label: '主题',
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

  bool get usesFlutterTxt {
    if (book.format == BookFormat.txt) {
      return true;
    }
    if (book.format != BookFormat.epub || currentChapter.filePath.isEmpty) {
      return false;
    }
    return !epubWebViewFallbackChapters.contains(currentChapter.filePath);
  }

  bool get usesVerticalScroll => style.readingFlow == ReadingFlowMode.scroll;

  ReaderChapter get currentChapter {
    if (book.chapters.isEmpty) {
      return const ReaderChapter(title: '正文', href: '', filePath: '');
    }
    return book.chapters[chapterIndex.clamp(0, lastChapterIndex)];
  }

  int get lastChapterIndex =>
      book.chapters.isEmpty ? 0 : book.chapters.length - 1;

  double get overallProgress {
    if (book.chapters.isEmpty) {
      return 0;
    }
    final chapterCount = book.chapters.length;
    final safeChapter = chapterIndex.clamp(0, chapterCount - 1);
    final safePageCount = pageCount < 1 ? 1 : pageCount;
    final safePage = page.clamp(0, safePageCount - 1);
    final isLastChapter = safeChapter == chapterCount - 1;
    if (safePageCount <= 1) {
      return isLastChapter ? 1.0 : (safeChapter / chapterCount);
    }
    final pageProgress = safePage / (safePageCount - 1);
    return ((safeChapter + pageProgress) / chapterCount).clamp(0.0, 1.0);
  }

  // --- Helper methods ---

  String? chapterEndAnchor(int index) {
    if (index < 0 || index >= book.chapters.length) {
      return null;
    }
    final chapter = book.chapters[index];
    final storedEndAnchor = chapter.endAnchor;
    if (storedEndAnchor != null && storedEndAnchor.isNotEmpty) {
      return storedEndAnchor;
    }
    final chapterPath = path.normalize(chapter.filePath);
    for (var i = index + 1; i < book.chapters.length; i++) {
      final nextChapter = book.chapters[i];
      if (path.normalize(nextChapter.filePath) != chapterPath) {
        break;
      }
      final nextAnchor = nextChapter.anchor;
      if (nextAnchor != null && nextAnchor.isNotEmpty) {
        return nextAnchor;
      }
    }
    return null;
  }

  bool isCurrentReaderToken(int token) => token == readerNavigationToken;

  bool isCurrentReaderEvent(Map<dynamic, dynamic> raw) {
    final token = (raw['token'] as num?)?.toInt();
    return token == readerNavigationToken;
  }

  void readerLog(String message) {
    assert(() {
      debugPrint('SQuartorReader $message');
      return true;
    }());
  }

  // --- Abstract methods provided by functional mixins ---
  // These are declared here so cross-mixin calls resolve at the static type level.

  // From ReaderEpubMixin
  Future<void> loadCurrentWebViewChapter();
  Future<void> injectReaderStyle();
  Future<void> onReaderTap(TapUpDetails details);
  Future<void> onReaderLongPress(LongPressStartDetails details);
  Future<void> openInternalLink(String href);
  bool isExternalUri(Uri uri);
  bool isExternalUriString(String value);
  Future<void> openExternalLink(Uri uri);
  void setWebEdgeTurnProgress(String direction, double progress);
  dynamic handleReaderEvent(List<dynamic> arguments);
  void scheduleStyleInjection();
  String cssColor(Color color);
  String? readerJumpStatus(Object? payload);
  Future<FlutterTxtDocument> readFlutterEpubDocument(
    ReaderChapter chapter, {
    String? anchor,
    String? endAnchor,
  });
  String extractEpubText(dom.Node node, {bool skipLinks = false});
  String cleanReaderText(String input);
  bool sameReaderTitle(String a, String b);
  bool isHiddenEpubMarker(dom.Element element);
  bool isHeadingElement(dom.Element element);
  List<String> extractEpubImageSources(dom.Element element);
  bool shouldFallbackForComplexEpubContent(
    dom.Element body,
    List<dom.Element> readableElements,
  );
  List<dom.Element> epubContentElements(dom.Element body);
  int epubAnchorStartIndex(List<dom.Element> elements, String? anchor);
  int epubAnchorEndIndex(List<dom.Element> elements, String? anchor, int start);
  bool elementContainsAnchor(dom.Element element, String anchor);
  String compactReaderText(String input);

  // From ReaderTxtMixin
  Future<void> loadAndPaginateTxt(
    TxtLayoutMetrics metrics,
    String signature,
    int token,
    int chapterIndex,
  );
  Future<FlutterTxtDocument> readFlutterTxtDocument(ReaderChapter chapter);
  void onTxtPageChanged(int token, int page);
  void onTxtScrollProgressChanged(int token, int page, int pageCount);
  double effectiveReaderFontSize(ReadingStyle style);
  void requestTxtPagination(TxtLayoutMetrics metrics);
  String buildTxtPaginationSignature(TxtLayoutMetrics metrics);
  bool hasTxtLayoutSize(BoxConstraints constraints);
  TxtLayoutMetrics resolveTxtLayoutMetrics({
    required BoxConstraints constraints,
    required EdgeInsets systemPadding,
  });
  List<FlutterTxtPage> paginateFlutterTxt({
    required FlutterTxtDocument document,
    required TxtLayoutMetrics metrics,
    required ReadingStyle readingStyle,
    required String? fontFamily,
  });
  List<FlutterTxtBlock> buildFlutterScrollBlocks({
    required FlutterTxtDocument document,
    required TxtLayoutMetrics metrics,
    required ReadingStyle readingStyle,
  });
  List<InlineTextSegment>? sliceSegmentsByTextRange(
    List<InlineTextSegment>? segments,
    int start,
    int end,
  );
  Future<String?> registerReaderFont(String? fontPath);
  String readerFontFamilyForPath(String fontPath);
  int preferTxtBreak(List<String> chars, int start, int count);
  int fitTxtFragmentCount({
    required List<String> chars,
    required int start,
    required String prefix,
    required double availableHeight,
    required double width,
    required TextStyle style,
  });
  int preferScrollBreak(
    List<String> chars,
    int start,
    int target,
    int hardLimit,
  );
  List<String> splitScrollText(String text);
  Widget buildFlutterTxtContent(
    ReaderChapter chapter,
    EdgeInsets systemPadding,
  );

  // From ReaderNavigationMixin
  void seekToOverallProgress(double progress);
  void requestProgressSeekToChapter(int targetIndex);
  void flushPendingProgressSeek();
  void cancelPendingProgressSeek();
  Future<void> goToChapter(
    int index, {
    bool atEnd = false,
    double? progress,
    String? anchor,
  });
  Future<void> nextPage();
  Future<void> previousPage();
  Future<void> nextTxtPage();
  Future<void> previousTxtPage();
  Future<void> scrollTxtByViewport(int direction);

  // From ReaderGestureMixin
  void onReaderDragStart(DragStartDetails details);
  void onReaderDragUpdate(DragUpdateDetails details);
  void scheduleReaderDragMove();
  Future<void> onReaderDragEnd(DragEndDetails details);
  void onReaderDragCancel();

  // From ReaderOverlayMixin
  double overlaySlideProgress(AnimationController controller);
  bool isSideOverlay(ReaderOverlay overlay);
  Future<void> freezeReaderForSideOverlay(int serial);
  Future<void> refreshFrozenReaderSnapshot();
  void clearReaderSnapshot({bool notify = true});
  void showFootnote(String text, Offset? globalPosition);
  void hideFootnote();
  void showToc();
  void showSettings();
  void onReaderStyleChanged();
  void applyReaderProgressPayload(Object? payload, {int? expectedToken});
  Future<void> hideFooter();
  Future<void> showFooter({bool fromZero = false});
  void showSideOverlay(ReaderOverlay target);
  void toggleChrome();
  void hideOverlay();
  void returnToChromeFromSideOverlay();
  AnimationController sideAnimation(ReaderOverlay overlay);
  Future<void> openSideOverlay(ReaderOverlay target, int serial);
  Future<void> toggleChromeAnimated(int serial);
  Future<void> returnToChromeFromSideOverlayAnimated(int serial);
  Future<void> hideOverlayAnimated(int serial);

  // From ReaderTimeMixin
  void flushReadingTime();
}
