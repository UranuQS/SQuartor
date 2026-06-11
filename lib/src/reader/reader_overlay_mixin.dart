import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';

import 'reader_enums.dart';
import 'reader_state_fields.dart';
import 'reader_txt_view.dart';

mixin ReaderOverlayMixin<T extends ReaderScreenWidget> on ReaderStateFields<T> {
  @override
  double overlaySlideProgress(AnimationController controller) {
    final value = controller.value.clamp(0.0, 1.0);
    if (controller.status == AnimationStatus.reverse) {
      return Curves.easeInCubic.transform(value);
    }
    return Curves.easeOutCubic.transform(value);
  }

  @override
  bool isSideOverlay(ReaderOverlay overlay) {
    return overlay == ReaderOverlay.toc || overlay == ReaderOverlay.settings;
  }

  @override
  Future<void> freezeReaderForSideOverlay(int serial) async {
    if (usesFlutterTxt || usesVerticalScroll) {
      return;
    }
    if (readerSnapshotImage != null || isLoading) {
      return;
    }
    final ctrl = controller;
    if (ctrl == null) {
      return;
    }
    try {
      final bytes = await ctrl.takeScreenshot(
        screenshotConfiguration: ScreenshotConfiguration(
          snapshotWidth: MediaQuery.sizeOf(context).width,
          compressFormat: CompressFormat.PNG,
          quality: 100,
        ),
      );
      if (!mounted ||
          serial != overlayTransitionSerial ||
          bytes == null ||
          bytes.isEmpty) {
        return;
      }
      final image = MemoryImage(bytes);
      await precacheImage(image, context);
      if (!mounted || serial != overlayTransitionSerial) {
        image.evict();
        return;
      }
      setState(() {
        readerSnapshotImage?.evict();
        readerSnapshotBytes = bytes;
        readerSnapshotImage = image;
      });
    } catch (error) {
      debugPrint('SQuartor reader snapshot failed: $error');
    }
  }

  @override
  Future<void> refreshFrozenReaderSnapshot() async {
    if (usesFlutterTxt || usesVerticalScroll) {
      return;
    }
    final serial = overlayTransitionSerial;
    if (!mounted ||
        overlay != ReaderOverlay.settings ||
        readerSnapshotImage == null ||
        isLoading) {
      return;
    }
    await Future<void>.delayed(const Duration(milliseconds: 90));
    if (!mounted ||
        serial != overlayTransitionSerial ||
        overlay != ReaderOverlay.settings) {
      return;
    }
    final ctrl = controller;
    if (ctrl == null) {
      return;
    }
    try {
      final bytes = await ctrl.takeScreenshot(
        screenshotConfiguration: ScreenshotConfiguration(
          snapshotWidth: MediaQuery.sizeOf(context).width,
          compressFormat: CompressFormat.PNG,
          quality: 100,
        ),
      );
      if (!mounted ||
          serial != overlayTransitionSerial ||
          overlay != ReaderOverlay.settings ||
          bytes == null ||
          bytes.isEmpty) {
        return;
      }
      final image = MemoryImage(bytes);
      await precacheImage(image, context);
      if (!mounted ||
          serial != overlayTransitionSerial ||
          overlay != ReaderOverlay.settings) {
        image.evict();
        return;
      }
      final oldImage = readerSnapshotImage;
      setState(() {
        readerSnapshotBytes = bytes;
        readerSnapshotImage = image;
      });
      oldImage?.evict();
    } catch (error) {
      debugPrint('SQuartor reader snapshot refresh failed: $error');
    }
  }

  @override
  void clearReaderSnapshot({bool notify = true}) {
    final image = readerSnapshotImage;
    if (image == null && readerSnapshotBytes == null) {
      return;
    }
    image?.evict();
    if (notify && mounted) {
      setState(() {
        readerSnapshotImage = null;
        readerSnapshotBytes = null;
      });
    } else {
      readerSnapshotImage = null;
      readerSnapshotBytes = null;
    }
  }

  @override
  void showFootnote(String text, Offset? globalPosition) {
    if (!mounted) {
      return;
    }
    setState(() {
      footnotePopup = FootnotePopupData(
        text: text.trim(),
        anchor: globalPosition,
        serial: DateTime.now().microsecondsSinceEpoch,
      );
    });
  }

  @override
  void hideFootnote() {
    if (!mounted || footnotePopup == null) {
      return;
    }
    setState(() => footnotePopup = null);
  }

  @override
  void showToc() {
    HapticFeedback.selectionClick();
    showSideOverlay(ReaderOverlay.toc);
  }

  @override
  void showSettings() {
    HapticFeedback.selectionClick();
    showSideOverlay(ReaderOverlay.settings);
  }

  @override
  void onReaderStyleChanged() {
    if (!usesFlutterTxt || !mounted) {
      return;
    }
    txtPaginationSignature = null;
    txtRequestedSignature = null;
    final metrics = txtLayoutMetrics;
    if (metrics != null) {
      requestTxtPagination(metrics);
    }
    setState(() {});
  }

  @override
  void applyReaderProgressPayload(Object? payload, {int? expectedToken}) {
    Map<dynamic, dynamic>? map;
    if (payload is Map) {
      map = payload;
    } else if (payload is String && payload.isNotEmpty) {
      try {
        final decoded = jsonDecode(payload);
        if (decoded is Map) {
          map = decoded;
        }
      } catch (_) {
        map = null;
      }
    }
    if (map == null) {
      return;
    }
    final token = (map['token'] as num?)?.toInt();
    if (token != null && token != (expectedToken ?? readerNavigationToken)) {
      return;
    }
    if (token == null && expectedToken != null) {
      map['token'] = expectedToken;
    }
    final pageParam = (map['page'] as num?)?.toInt() ?? 0;
    final pages = (map['pages'] as num?)?.toInt() ?? 1;
    final safePages = pages < 1 ? 1 : pages;
    final safePage = pageParam.clamp(0, safePages - 1);
    if (mounted && (page != safePage || pageCount != safePages)) {
      setState(() {
        page = safePage;
        pageCount = safePages;
      });
    }
    unawaited(
      appState.updateBookProgress(
        book: book,
        chapterIndex: chapterIndex,
        page: safePage,
        pageCount: safePages,
      ),
    );
  }

  @override
  Future<void> hideFooter() async {
    if (footerAnimation.value <= 0.001) {
      footerAnimation.value = 0;
      return;
    }
    await footerAnimation.reverse();
  }

  @override
  Future<void> showFooter({bool fromZero = false}) async {
    if (!mounted || overlay != ReaderOverlay.hidden) {
      return;
    }
    if (footerAnimation.value >= 0.999) {
      return;
    }
    await footerAnimation.forward(from: fromZero ? 0 : footerAnimation.value);
  }

  @override
  void showSideOverlay(ReaderOverlay target) {
    if (!isSideOverlay(target)) {
      return;
    }
    final serial = ++overlayTransitionSerial;
    if (sideOverlayDismissing) {
      setState(() => sideOverlayDismissing = false);
    }
    unawaited(openSideOverlay(target, serial));
  }

  @override
  void toggleChrome() {
    if (!mounted) {
      return;
    }
    final serial = ++overlayTransitionSerial;
    unawaited(toggleChromeAnimated(serial));
  }

  @override
  void hideOverlay() {
    if (!mounted || overlay == ReaderOverlay.hidden) {
      return;
    }
    HapticFeedback.selectionClick();
    final serial = ++overlayTransitionSerial;
    unawaited(hideOverlayAnimated(serial));
  }

  @override
  void returnToChromeFromSideOverlay() {
    if (!mounted || !isSideOverlay(overlay)) {
      return;
    }
    HapticFeedback.selectionClick();
    final serial = ++overlayTransitionSerial;
    unawaited(returnToChromeFromSideOverlayAnimated(serial));
  }

  @override
  AnimationController sideAnimation(ReaderOverlay overlay) {
    return overlay == ReaderOverlay.settings ? settingsAnimation : tocAnimation;
  }

  @override
  Future<void> openSideOverlay(ReaderOverlay target, int serial) async {
    if (!mounted) {
      return;
    }
    final current = overlay;
    final freezeFuture = freezeReaderForSideOverlay(serial);
    if (current == target) {
      await freezeFuture;
      await sideAnimation(target).forward();
      return;
    }
    if (current == ReaderOverlay.chrome) {
      final other = target == ReaderOverlay.toc
          ? ReaderOverlay.settings
          : ReaderOverlay.toc;
      if (sideAnimation(other).value > 0) {
        sideAnimation(other).value = 0;
      }
      setState(() => overlay = target);
      await freezeFuture;
      await Future.wait([
        chromeAnimation.reverse(),
        sideAnimation(target).forward(),
      ]);
      return;
    } else if (isSideOverlay(current)) {
      final other = target == ReaderOverlay.toc
          ? ReaderOverlay.settings
          : ReaderOverlay.toc;
      if (sideAnimation(other).value > 0 && other != current) {
        sideAnimation(other).value = 0;
      }
      setState(() => overlay = target);
      await Future.wait([
        sideAnimation(current).reverse(),
        sideAnimation(target).forward(),
      ]);
      return;
    } else {
      await freezeFuture;
    }
    if (!mounted || serial != overlayTransitionSerial) {
      return;
    }
    final other = target == ReaderOverlay.toc
        ? ReaderOverlay.settings
        : ReaderOverlay.toc;
    if (sideAnimation(other).value > 0) {
      await sideAnimation(other).reverse();
    }
    if (!mounted || serial != overlayTransitionSerial) {
      return;
    }
    setState(() => overlay = target);
    await sideAnimation(target).forward();
  }

  @override
  Future<void> toggleChromeAnimated(int serial) async {
    if (overlay == ReaderOverlay.chrome) {
      setState(() => overlay = ReaderOverlay.hidden);
      await chromeAnimation.reverse();
      if (mounted && serial == overlayTransitionSerial) {
        await showFooter(fromZero: true);
      }
      return;
    }
    final current = overlay;
    await hideFooter();
    if (isSideOverlay(current)) {
      setState(() => overlay = ReaderOverlay.hidden);
      await sideAnimation(current).reverse();
    }
    if (!mounted || serial != overlayTransitionSerial) {
      return;
    }
    if (isSideOverlay(current)) {
      clearReaderSnapshot();
    }
    setState(() => overlay = ReaderOverlay.chrome);
    await chromeAnimation.forward();
  }

  @override
  Future<void> returnToChromeFromSideOverlayAnimated(int serial) async {
    final current = overlay;
    if (!isSideOverlay(current)) {
      return;
    }
    setState(() {
      sideOverlayDismissing = true;
      chromeReturningFromSide = true;
    });
    await sideAnimation(current).reverse();
    if (!mounted || serial != overlayTransitionSerial) {
      return;
    }
    clearReaderSnapshot();
    setState(() {
      overlay = ReaderOverlay.chrome;
      sideOverlayDismissing = false;
    });
    await chromeAnimation.forward();
    if (!mounted || serial != overlayTransitionSerial) {
      return;
    }
    setState(() => chromeReturningFromSide = false);
  }

  @override
  Future<void> hideOverlayAnimated(int serial) async {
    final current = overlay;
    if (current == ReaderOverlay.chrome) {
      setState(() => overlay = ReaderOverlay.hidden);
      await chromeAnimation.reverse();
    } else if (isSideOverlay(current)) {
      setState(() => sideOverlayDismissing = true);
      await sideAnimation(current).reverse();
      if (mounted && serial == overlayTransitionSerial) {
        setState(() {
          overlay = ReaderOverlay.hidden;
          sideOverlayDismissing = false;
        });
      }
    }
    if (!mounted || serial != overlayTransitionSerial) {
      return;
    }
    if (isSideOverlay(current)) {
      clearReaderSnapshot();
      return;
    }
    await showFooter(fromZero: true);
  }
}
