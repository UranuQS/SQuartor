import 'dart:convert';
import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:html/dom.dart' as dom;
import 'package:html/parser.dart' as html_parser;
import 'package:path/path.dart' as path;
import 'package:url_launcher/url_launcher.dart';

import '../models.dart';
import 'reader_enums.dart';
import 'reader_epub_fallback.dart';
import 'reader_state_fields.dart';
import 'reader_txt_view.dart';

mixin ReaderEpubMixin<T extends ReaderScreenWidget> on ReaderStateFields<T> {
  @override
  Future<void> loadCurrentWebViewChapter() async {
    final ctrl = controller;
    final chapter = currentChapter;
    if (ctrl == null || chapter.filePath.isEmpty) {
      readerLog(
        'skip webview load controller=${ctrl != null} file=${chapter.filePath}',
      );
      return;
    }
    try {
      readerLog('load webview chapter=$chapterIndex file=${chapter.filePath}');
      await ctrl.loadUrl(
        urlRequest: URLRequest(url: WebUri.uri(File(chapter.filePath).uri)),
      );
    } catch (error) {
      readerLog('load webview error $error');
      if (!mounted) {
        return;
      }
      setState(() {
        isLoading = false;
        loadError = error.toString();
        overlay = ReaderOverlay.chrome;
      });
      flushPendingProgressSeek();
    }
  }

  @override
  Future<void> injectReaderStyle() async {
    final ctrl = controller;
    if (ctrl == null) {
      return;
    }
    final reader = readerPalette;
    final systemPadding = MediaQuery.viewPaddingOf(context);
    final initialProgress =
        pendingPageProgress ?? (pageCount <= 1 ? 0.0 : page / (pageCount - 1));
    pendingPageProgress = null;
    final webAnchor = pendingWebJumpToEnd
        ? null
        : (pendingAnchor ?? currentChapter.anchor);
    final webEndAnchor = pendingWebJumpToEnd
        ? chapterEndAnchor(chapterIndex)
        : null;
    final config = jsonEncode({
      'token': readerNavigationToken,
      'background': cssColor(reader.background),
      'text': cssColor(reader.text),
      'muted': cssColor(reader.muted),
      'accent': cssColor(appPalette.primarySoft),
      'fontSize': style.fontSize,
      'lineHeight': style.lineHeight,
      'paragraphSpacing': style.paragraphSpacing,
      'letterSpacing': style.letterSpacing,
      'pageMargin': style.pageMargin,
      'verticalMargin': style.verticalMargin,
      'safeTop': systemPadding.top,
      'safeBottom': systemPadding.bottom,
      'firstLineIndent': style.firstLineIndent,
      'readingFlow': style.readingFlow.name,
      'fontName': style.fontName == null ? null : 'SQuartorCustomFont',
      'fontUri': style.fontPath == null
          ? null
          : File(style.fontPath!).uri.toString(),
      'initialProgress': initialProgress,
      'anchor': webAnchor,
      'endAnchor': webEndAnchor,
    });
    pendingAnchor = null;
    pendingWebJumpToEnd = false;
    await ctrl.evaluateJavascript(source: readerScript(config));
  }

  String readerScript(String configJson) {
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
  const scrollMode = cfg.readingFlow === 'scroll';

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
    if (scrollMode) {
      root.style.setProperty('right', 'auto', 'important');
      root.style.setProperty('bottom', 'auto', 'important');
      root.style.setProperty('width', viewportWidth + 'px', 'important');
      root.style.setProperty('height', Math.max(lineHeightPx * 4, rawHeight) + 'px', 'important');
      root.style.setProperty('overflow-x', 'hidden', 'important');
      root.style.setProperty('overflow-y', 'auto', 'important');
      root.style.setProperty('-webkit-overflow-scrolling', 'touch', 'important');
      source.style.setProperty('display', isImagePage ? 'flex' : 'block', 'important');
      source.style.setProperty('left', (isImagePage ? 0 : horizontalMargin) + 'px', 'important');
      source.style.setProperty('width', contentWidth + 'px', 'important');
      source.style.setProperty('min-width', contentWidth + 'px', 'important');
      source.style.setProperty('min-height', Math.max(lineHeightPx * 4, rawHeight) + 'px', 'important');
      source.style.setProperty('height', isImagePage ? Math.max(lineHeightPx * 4, rawHeight) + 'px' : 'auto', 'important');
      source.style.setProperty('max-width', 'none', 'important');
      source.style.setProperty('--squartor-image-max-width', Math.max(160, width) + 'px');
      source.style.setProperty('--squartor-image-max-height', Math.max(lineHeightPx * 4, rawHeight) + 'px');
      source.style.setProperty('column-count', '1', 'important');
      source.style.setProperty('column-width', 'auto', 'important');
      source.style.setProperty('column-gap', '0px', 'important');
      source.style.setProperty('transition', 'transform 0ms linear', 'important');
      source.style.setProperty('transform', 'translate3d(0px, 0px, 0px)', 'important');
      if (!window.SQuartor.hasRestoredProgress) {
        requestAnimationFrame(function() {
          const maxScroll = Math.max(0, root.scrollHeight - root.clientHeight);
          root.scrollTop = Math.max(0, Math.min(maxScroll, window.SQuartor.initialProgress * maxScroll));
          window.SQuartor.hasRestoredProgress = true;
          notify();
        });
      }
      updatePageMetrics();
      return true;
    }
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

  function updatePageMetrics() {
    if (!scrollMode) return;
    const viewport = Math.max(1, root.clientHeight || window.innerHeight || 1);
    const maxScroll = Math.max(0, root.scrollHeight - root.clientHeight);
    window.SQuartor.pages = Math.max(1, Math.ceil(maxScroll / viewport) + 1);
    window.SQuartor.page = Math.max(0, Math.min(window.SQuartor.pages - 1, Math.round(root.scrollTop / viewport)));
  }

  function notify() {
    updatePageMetrics();
    if (window.flutter_inappwebview) {
      window.flutter_inappwebview.callHandler('squartorEvent', { type: 'ready', token: window.SQuartor.token });
      window.flutter_inappwebview.callHandler('squartorEvent', { type: 'progress', token: window.SQuartor.token, page: window.SQuartor.page, pages: window.SQuartor.pages });
    }
  }

  function progressPayload(status) {
    updatePageMetrics();
    return JSON.stringify({
      status: status || 'ok',
      token: window.SQuartor.token,
      page: window.SQuartor.page,
      pages: window.SQuartor.pages
    });
  }

  window.SQuartor = {
    token: Number(cfg.token || 0),
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
      if (scrollMode) {
        updatePageMetrics();
        const maxScroll = Math.max(0, root.scrollHeight - root.clientHeight);
        if (root.scrollTop >= maxScroll - 2) return 'end';
        root.scrollTo({ top: Math.min(maxScroll, root.scrollTop + root.clientHeight * 0.88), behavior: 'smooth' });
        setTimeout(notify, 240);
        return 'ok';
      }
      if (this.page >= this.pages - 1) return 'end';
      this.page += 1;
      applyTransform(true);
      notify();
      return 'ok';
    },
    previousPage() {
      if (scrollMode) {
        updatePageMetrics();
        if (root.scrollTop <= 2) return 'start';
        root.scrollTo({ top: Math.max(0, root.scrollTop - root.clientHeight * 0.88), behavior: 'smooth' });
        setTimeout(notify, 240);
        return 'ok';
      }
      if (this.page <= 0) return 'start';
      this.page -= 1;
      applyTransform(true);
      notify();
      return 'ok';
    },
    jumpToProgress(progress) {
      progress = Math.max(0, Math.min(1, Number(progress || 0)));
      if (scrollMode) {
        const maxScroll = Math.max(0, root.scrollHeight - root.clientHeight);
        root.scrollTop = progress * maxScroll;
        updatePageMetrics();
        notify();
        return progressPayload('ok');
      }
      this.page = Math.round(progress * Math.max(0, this.pages - 1));
      applyTransform(false);
      notify();
      return progressPayload('ok');
    },
    dragStart(token) {
      if (scrollMode) return 'disabled';
      this.dragToken = Number(token || 0);
      this.dragActive = true;
      source.style.transition = 'transform 0ms linear';
      return 'ok';
    },
    dragMove(dx, token) {
      if (scrollMode) return 'disabled';
      if (!this.dragActive || Number(token || 0) !== this.dragToken) {
        return 'stale';
      }
      applyDragTransform(Number(dx || 0));
      return 'ok';
    },
    dragCancel(token) {
      if (scrollMode) return 'disabled';
      if (Number(token || 0) !== this.dragToken) {
        return 'stale';
      }
      this.dragActive = false;
      applyTransform(true);
      settleAfterAnimation(this.dragToken);
      return 'cancel';
    },
    dragEnd(dx, velocity, token) {
      if (scrollMode) return 'disabled';
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
      if (!anchor) return progressPayload('missing');
      const node = source.querySelector('#' + CSS.escape(anchor)) || source.querySelector('[name="' + CSS.escape(anchor) + '"]');
      if (!node) return progressPayload('missing');
      if (scrollMode) {
        const offset = Math.max(0, node.getBoundingClientRect().top - source.getBoundingClientRect().top + root.scrollTop);
        root.scrollTop = offset;
        updatePageMetrics();
        notify();
        return progressPayload('ok');
      }
      const offset = Math.max(0, node.getBoundingClientRect().left - source.getBoundingClientRect().left);
      const index = Math.floor(offset / this.pageStride);
      this.page = Math.max(0, Math.min(index, this.pages - 1));
      applyTransform(true);
      notify();
      return progressPayload('ok');
    },
    jumpToChapterEnd(endAnchor) {
      if (!endAnchor) return this.jumpToProgress(1);
      const node = source.querySelector('#' + CSS.escape(endAnchor)) || source.querySelector('[name="' + CSS.escape(endAnchor) + '"]');
      if (!node) return progressPayload('missing');
      if (scrollMode) {
        const offset = Math.max(0, node.getBoundingClientRect().top - source.getBoundingClientRect().top + root.scrollTop);
        root.scrollTop = Math.max(0, offset - root.clientHeight + 24);
        updatePageMetrics();
        notify();
        return progressPayload('ok');
      }
      const offset = Math.max(0, node.getBoundingClientRect().left - source.getBoundingClientRect().left);
      const index = Math.ceil(offset / this.pageStride) - 1;
      this.page = Math.max(0, Math.min(index, this.pages - 1));
      applyTransform(true);
      notify();
      return progressPayload('ok');
    },
    openLinkAt(x, y) {
      x = Number(x);
      y = Number(y);
      const node = document.elementFromPoint(x, y);
      let footnote = node && node.closest ? node.closest('.sq-footnote-ref') : null;
      if (!footnote) {
        footnote = Array.from(source.querySelectorAll('.sq-footnote-ref')).find(function(candidate) {
          return Array.from(candidate.getClientRects()).some(function(rect) {
            return x >= rect.left - 10 && x <= rect.right + 10 &&
              y >= rect.top - 10 && y <= rect.bottom + 10;
          });
        }) || null;
      }
      if (footnote && footnote.dataset && footnote.dataset.footnote) {
        if (window.flutter_inappwebview) {
          window.flutter_inappwebview.callHandler('squartorEvent', {
            type: 'footnote',
            token: window.SQuartor.token,
            text: footnote.dataset.footnote,
            x: x,
            y: y
          });
        }
        return true;
      }
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
        window.flutter_inappwebview.callHandler('squartorEvent', { type: 'linkTap', token: window.SQuartor.token });
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
          token: window.SQuartor.token,
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
    if (layoutRound === 0 && cfg.endAnchor) {
      window.SQuartor.jumpToChapterEnd(cfg.endAnchor);
    } else if (layoutRound === 0 && cfg.anchor) {
      window.SQuartor.jumpToAnchor(cfg.anchor);
    }
    layoutRound += 1;
    if (layoutRound < 3) requestAnimationFrame(settleLayout);
  }
  let resizeTimer = null;
  let scrollTimer = null;
  let edgeProgressTimer = null;
  let touchLastY = null;
  let edgePull = 0;
  const edgeTurnThreshold = 132;
  function scheduleLayout() {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(function() { window.SQuartor.layout(); }, 50);
  }
  function scheduleScrollNotify() {
    if (!scrollMode) return;
    clearTimeout(scrollTimer);
    scrollTimer = setTimeout(notify, 120);
  }
  function sendEdgeProgress(pull) {
    if (!scrollMode || !window.flutter_inappwebview) return;
    const progress = Math.min(1, Math.abs(pull) / edgeTurnThreshold);
    clearTimeout(edgeProgressTimer);
    if (progress <= 0.02) {
      window.flutter_inappwebview.callHandler('squartorEvent', {
        type: 'edgeTurnProgress',
        token: window.SQuartor.token,
        direction: pull > 0 ? 'next' : 'previous',
        progress: 0
      });
      return;
    }
    edgeProgressTimer = setTimeout(function() {
      window.flutter_inappwebview.callHandler('squartorEvent', {
        type: 'edgeTurnProgress',
        token: window.SQuartor.token,
        direction: pull > 0 ? 'next' : 'previous',
        progress: progress
      });
    }, 16);
  }
  function onTouchStart(event) {
    if (!scrollMode || !event.touches || event.touches.length !== 1) return;
    touchLastY = event.touches[0].clientY;
    edgePull = 0;
  }
  function onTouchMove(event) {
    if (!scrollMode || touchLastY === null || !event.touches || event.touches.length !== 1) return;
    const y = event.touches[0].clientY;
    const fingerDelta = touchLastY - y;
    touchLastY = y;
    const maxScroll = Math.max(0, root.scrollHeight - root.clientHeight);
    if (root.scrollTop >= maxScroll - 2 && fingerDelta > 0) {
      edgePull += fingerDelta;
    } else if (root.scrollTop <= 2 && fingerDelta < 0) {
      edgePull += fingerDelta;
    } else if (edgePull !== 0) {
      edgePull += fingerDelta;
      if (edgePull > edgeTurnThreshold) edgePull = edgeTurnThreshold;
      if (edgePull < -edgeTurnThreshold) edgePull = -edgeTurnThreshold;
      if (Math.abs(edgePull) < 2) edgePull = 0;
    }
    sendEdgeProgress(edgePull);
  }
  function onTouchEnd() {
    if (!scrollMode) return;
    const pull = edgePull;
    touchLastY = null;
    edgePull = 0;
    clearTimeout(edgeProgressTimer);
    if (window.flutter_inappwebview) {
      window.flutter_inappwebview.callHandler('squartorEvent', {
        type: 'edgeTurnProgress',
        token: window.SQuartor.token,
        direction: pull > 0 ? 'next' : 'previous',
        progress: 0
      });
    }
    if (Math.abs(pull) < edgeTurnThreshold || !window.flutter_inappwebview) return;
    window.flutter_inappwebview.callHandler('squartorEvent', {
      token: window.SQuartor.token,
      type: pull > 0 ? 'nextChapter' : 'previousChapter'
    });
  }
  window.addEventListener('resize', scheduleLayout);
  root.addEventListener('scroll', scheduleScrollNotify, { passive: true });
  root.addEventListener('touchstart', onTouchStart, { passive: true });
  root.addEventListener('touchmove', onTouchMove, { passive: true });
  root.addEventListener('touchend', onTouchEnd, { passive: true });
  root.addEventListener('touchcancel', onTouchEnd, { passive: true });
  window.SQuartor.dispose = function() {
    clearTimeout(resizeTimer);
    clearTimeout(scrollTimer);
    clearTimeout(edgeProgressTimer);
    window.removeEventListener('resize', scheduleLayout);
    root.removeEventListener('scroll', scheduleScrollNotify);
    root.removeEventListener('touchstart', onTouchStart);
    root.removeEventListener('touchmove', onTouchMove);
    root.removeEventListener('touchend', onTouchEnd);
    root.removeEventListener('touchcancel', onTouchEnd);
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

  @override
  Future<void> onReaderTap(TapUpDetails details) async {
    final width = MediaQuery.sizeOf(context).width;
    if (!usesFlutterTxt) {
      linkTapHandled = false;
      final x = details.localPosition.dx.toStringAsFixed(2);
      final y = details.localPosition.dy.toStringAsFixed(2);
      final openedLink = await controller?.evaluateJavascript(
        source: 'window.SQuartor && window.SQuartor.openLinkAt($x, $y);',
      );
      if (linkTapHandled ||
          openedLink == true ||
          openedLink == 'true' ||
          openedLink == 1) {
        return;
      }
    }
    final tapX = details.localPosition.dx;
    if (tapX >= width * .32 && tapX <= width * .68) {
      toggleChrome();
      return;
    }
    if (overlay != ReaderOverlay.hidden || chromeAnimation.value > .001) {
      toggleChrome();
      return;
    }
    if (usesVerticalScroll) {
      return;
    }
    final tappedLeft = tapX < width * .32;
    final previous = style.reverseTapPageTurn ? !tappedLeft : tappedLeft;
    if (previous) {
      await previousPage();
    } else {
      await nextPage();
    }
  }

  @override
  Future<void> onReaderLongPress(LongPressStartDetails details) async {
    final x = details.localPosition.dx.toStringAsFixed(2);
    final y = details.localPosition.dy.toStringAsFixed(2);
    final result = await controller?.evaluateJavascript(
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
            FullscreenImageViewer(source: result),
      ),
    );
  }

  @override
  Future<void> openInternalLink(String href) async {
    final uri = Uri.tryParse(href);
    if (uri == null) {
      return;
    }
    if (isExternalUri(uri)) {
      await openExternalLink(uri);
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
        : decodeLooseUriComponent(uri.fragment);
    if (index == chapterIndex) {
      if (anchor != null) {
        if (usesFlutterTxt) {
          setState(() {
            pendingAnchor = anchor;
            pendingPageProgress = 0;
            pendingExactPage = null;
            pendingExactPageCount = null;
            txtPaginationSignature = null;
            txtRequestedSignature = null;
            txtPages = const [];
            txtScrollBlocks = const [];
            isLoading = true;
            loadError = null;
          });
          final metrics = txtLayoutMetrics;
          if (metrics != null) {
            requestTxtPagination(metrics);
          }
        } else {
          await controller?.evaluateJavascript(
            source:
                'window.SQuartor && window.SQuartor.jumpToAnchor(${jsonEncode(anchor)});',
          );
        }
      }
      return;
    }
    await goToChapter(index, anchor: anchor);
  }

  @override
  bool isExternalUri(Uri uri) {
    final scheme = uri.scheme.toLowerCase();
    return scheme == 'http' ||
        scheme == 'https' ||
        scheme == 'mailto' ||
        scheme == 'tel';
  }

  @override
  bool isExternalUriString(String value) {
    final uri = Uri.tryParse(value);
    return uri != null && isExternalUri(uri);
  }

  @override
  Future<void> openExternalLink(Uri uri) async {
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

  @override
  void setWebEdgeTurnProgress(String direction, double progress) {
    if (!mounted) {
      return;
    }
    webEdgeTurnResetTimer?.cancel();
    final clamped = progress.clamp(0.0, 1.0).toDouble();
    if (webEdgeTurnDirection == direction &&
        (webEdgeTurnProgress - clamped).abs() < .02) {
      return;
    }
    setState(() {
      webEdgeTurnDirection = direction;
      webEdgeTurnProgress = clamped;
    });
    if (clamped > 0) {
      webEdgeTurnResetTimer = Timer(const Duration(milliseconds: 180), () {
        if (mounted) {
          setState(() => webEdgeTurnProgress = 0);
        }
      });
    }
  }

  @override
  dynamic handleReaderEvent(List<dynamic> arguments) {
    if (!mounted) {
      return null;
    }
    final raw = arguments.isEmpty ? null : arguments.first;
    if (raw is! Map) {
      return null;
    }
    final type = raw['type'];
    if (!isCurrentReaderEvent(raw) &&
        (type == 'ready' ||
            type == 'progress' ||
            type == 'edgeTurnProgress' ||
            type == 'nextChapter' ||
            type == 'previousChapter')) {
      return null;
    }
    if (type == 'progress') {
      if (isLoading) {
        return null;
      }
      applyReaderProgressPayload(raw);
    } else if (type == 'ready') {
      if (mounted) {
        setState(() {
          isLoading = false;
          loadError = null;
        });
      }
    } else if (type == 'toggleMenu') {
      if (mounted) {
        toggleChrome();
      }
    } else if (type == 'linkTap') {
      linkTapHandled = true;
    } else if (type == 'footnote') {
      linkTapHandled = true;
      final text = raw['text'] as String?;
      final x = (raw['x'] as num?)?.toDouble();
      final y = (raw['y'] as num?)?.toDouble();
      if (text != null && text.trim().isNotEmpty) {
        showFootnote(text, x == null || y == null ? null : Offset(x, y));
      }
    } else if (type == 'internalLink') {
      linkTapHandled = true;
      final href = raw['href'] as String?;
      if (href != null) {
        unawaited(openInternalLink(href));
      }
    } else if (type == 'edgeTurnProgress') {
      final rawDirection = raw['direction'] as String?;
      final rawProgress = (raw['progress'] as num?)?.toDouble() ?? 0;
      setWebEdgeTurnProgress(
        rawDirection == 'previous' ? 'previous' : 'next',
        rawProgress,
      );
    } else if (type == 'nextChapter') {
      setWebEdgeTurnProgress('next', 0);
      WidgetsBinding.instance.addPostFrameCallback(
        (_) => goToChapter(chapterIndex + 1),
      );
    } else if (type == 'previousChapter') {
      setWebEdgeTurnProgress('previous', 0);
      WidgetsBinding.instance.addPostFrameCallback(
        (_) => goToChapter(chapterIndex - 1, atEnd: true),
      );
    }
    return null;
  }

  @override
  void scheduleStyleInjection() {
    if (usesFlutterTxt) {
      return;
    }
    styleInjectTimer?.cancel();
    styleInjectTimer = Timer(const Duration(milliseconds: 140), () {
      unawaited(injectReaderStyle().then((_) => refreshFrozenReaderSnapshot()));
    });
  }

  @override
  String cssColor(Color color) {
    String two(int value) => value.toRadixString(16).padLeft(2, '0');
    final r = (color.r * 255).round().clamp(0, 255);
    final g = (color.g * 255).round().clamp(0, 255);
    final b = (color.b * 255).round().clamp(0, 255);
    return '#${two(r)}${two(g)}${two(b)}';
  }

  @override
  String? readerJumpStatus(Object? payload) {
    if (payload is! String || payload.isEmpty) {
      return null;
    }
    try {
      final decoded = jsonDecode(payload);
      if (decoded is Map) {
        return decoded['status'] as String?;
      }
    } catch (_) {
      return null;
    }
    return null;
  }

  // --- EPUB content parsing helpers ---

  @override
  Future<FlutterTxtDocument> readFlutterEpubDocument(
    ReaderChapter chapter, {
    String? anchor,
    String? endAnchor,
  }) async {
    final raw = await File(chapter.filePath).readAsString();
    final document = html_parser.parse(raw);
    final body = document.body;
    if (body == null ||
        (!body.classes.contains('sq-document-flow') &&
            !body.classes.contains('sq-document-image-only'))) {
      throw const EpubWebViewFallbackException();
    }
    final elements = epubContentElements(body);
    final readableElements = elements
        .where(
          (element) => cleanReaderText(extractEpubText(element)).isNotEmpty,
        )
        .toList();
    if (elements.isEmpty) {
      throw const EpubWebViewFallbackException();
    }
    if (shouldFallbackForComplexEpubContent(body, readableElements)) {
      throw const EpubWebViewFallbackException();
    }

    final start = epubAnchorStartIndex(elements, anchor);
    final end = epubAnchorEndIndex(elements, endAnchor, start);
    final visibleElements = elements.sublist(start, end);
    if (visibleElements.isEmpty) {
      throw const EpubWebViewFallbackException();
    }

    final heading = visibleElements
        .where((element) => isHeadingElement(element))
        .map((element) => cleanReaderText(extractEpubText(element)))
        .where((text) => text.isNotEmpty)
        .firstOrNull;
    final documentTitle = cleanReaderText(
      document.querySelector('title')?.text ?? '',
    );
    final title = (heading?.isNotEmpty == true
        ? heading!
        : documentTitle.isNotEmpty
        ? documentTitle
        : chapter.title.trim());
    final blocks = <FlutterDocumentBlock>[];
    for (final element in visibleElements) {
      final inlineSegments = extractEpubInlineSegments(element);
      if (inlineSegments.any((segment) => segment.footnote != null)) {
        final text = cleanReaderText(segmentsText(inlineSegments));
        if (text.isNotEmpty) {
          if (blocks.isEmpty && sameReaderTitle(text, title)) {
            continue;
          }
          blocks.add(FlutterDocumentBlock.rich(inlineSegments));
        }
        for (final source in extractEpubImageSources(element)) {
          blocks.add(FlutterDocumentBlock.image(source));
        }
        continue;
      }
      final links = extractEpubLinks(element);
      if (links.isNotEmpty) {
        final surroundingText = cleanReaderText(
          extractEpubText(element, skipLinks: true),
        );
        if (surroundingText.isNotEmpty) {
          if (blocks.isEmpty && sameReaderTitle(surroundingText, title)) {
            continue;
          }
          blocks.add(FlutterDocumentBlock.paragraph(surroundingText));
        }
        for (final link in links) {
          if (blocks.isEmpty && sameReaderTitle(link.text, title)) {
            continue;
          }
          blocks.add(FlutterDocumentBlock.link(link.text, link.href));
        }
      } else {
        final text = cleanReaderText(extractEpubText(element));
        if (text.isNotEmpty) {
          if (blocks.isEmpty && sameReaderTitle(text, title)) {
            continue;
          }
          blocks.add(FlutterDocumentBlock.paragraph(text));
        }
      }
      for (final source in extractEpubImageSources(element)) {
        blocks.add(FlutterDocumentBlock.image(source));
      }
    }
    if (title.trim().isEmpty && blocks.isEmpty) {
      throw const EpubWebViewFallbackException();
    }
    return FlutterTxtDocument(title: title, blocks: blocks);
  }

  List<InlineTextSegment> extractEpubInlineSegments(dom.Node node) {
    if (node is dom.Text) {
      return [InlineTextSegment(text: node.data)];
    }
    if (node is! dom.Element) {
      return const [];
    }
    if (isHiddenEpubMarker(node)) {
      return const [];
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
      return const [];
    }
    if (tag == 'br') {
      return const [InlineTextSegment(text: '\n')];
    }
    if (tag == 'a' && node.classes.contains('sq-footnote-ref')) {
      final footnote = node.attributes['data-footnote'];
      if (footnote != null && footnote.trim().isNotEmpty) {
        return [
          InlineTextSegment(
            text: cleanReaderText(node.text).isEmpty
                ? '\u6ce8'
                : cleanReaderText(node.text),
            href: node.attributes['href'],
            footnote: footnote,
          ),
        ];
      }
    }
    if (tag == 'a') {
      final href = node.attributes['href'];
      final text = cleanReaderText(extractEpubText(node));
      if (href != null && href.isNotEmpty && text.isNotEmpty) {
        return [InlineTextSegment(text: text, href: href)];
      }
    }
    return node.nodes.expand(extractEpubInlineSegments).toList();
  }

  List<EpubLinkBlock> extractEpubLinks(dom.Element element) {
    final links = <EpubLinkBlock>[];
    for (final link in element.querySelectorAll('a[href]')) {
      final href = link.attributes['href'];
      final text = cleanReaderText(extractEpubText(link));
      if (href == null || href.isEmpty || text.isEmpty) {
        continue;
      }
      links.add(EpubLinkBlock(text: text, href: href));
    }
    return links;
  }

  @override
  String extractEpubText(dom.Node node, {bool skipLinks = false}) {
    if (node is dom.Text) {
      return node.data;
    }
    if (node is! dom.Element) {
      return '';
    }
    if (isHiddenEpubMarker(node)) {
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
        .map((child) => extractEpubText(child, skipLinks: skipLinks))
        .join();
  }

  @override
  List<dom.Element> epubContentElements(dom.Element body) {
    const primarySelector = 'h1, h2, h3, h4, h5, h6, p, li, blockquote';
    const mediaSelector = '.sq-media';
    final primary = body
        .querySelectorAll('$primarySelector, $mediaSelector')
        .where((element) => !isHiddenEpubMarker(element))
        .where(
          (element) =>
              cleanReaderText(extractEpubText(element)).isNotEmpty ||
              extractEpubImageSources(element).isNotEmpty,
        )
        .toList();
    if (primary.isNotEmpty) {
      return primary;
    }
    return body
        .querySelectorAll('div, section, article')
        .where((element) => !isHiddenEpubMarker(element))
        .where(
          (element) =>
              element.querySelector(primarySelector) == null &&
              element.querySelector('div, section, article') == null,
        )
        .where(
          (element) =>
              cleanReaderText(extractEpubText(element)).isNotEmpty ||
              extractEpubImageSources(element).isNotEmpty,
        )
        .toList();
  }

  @override
  List<String> extractEpubImageSources(dom.Element element) {
    final result = <String>[];
    if (element.localName?.toLowerCase() == 'img') {
      final source = element.attributes['src'];
      if (source != null &&
          source.isNotEmpty &&
          !isTinyAnnotationImage(element)) {
        result.add(source);
      }
    }
    for (final image in element.querySelectorAll('img')) {
      final source = image.attributes['src'];
      if (source != null &&
          source.isNotEmpty &&
          !isTinyAnnotationImage(image)) {
        result.add(source);
      }
    }
    return result.toSet().toList(growable: false);
  }

  bool isTinyAnnotationImage(dom.Element element) {
    final mediaHref = element.attributes['src'] ?? '';
    final markerText = [
      mediaHref,
      element.attributes['alt'],
      element.attributes['title'],
      element.attributes['class'],
      element.attributes['id'],
      element.parent?.attributes['class'],
      element.parent?.attributes['id'],
      element.parent?.attributes['epub:type'],
      element.parent?.attributes['type'],
      element.parent?.attributes['role'],
    ].whereType<String>().join(' ').toLowerCase();
    final looksLikeNote =
        RegExp(
          r'(footnote|noteref|note|annotation|marker|kobo|duokan|kindle)',
        ).hasMatch(markerText) ||
        markerText.contains('\u6ce8') ||
        markerText.contains('\u811a') ||
        markerText.contains('\u8a3b');
    if (!looksLikeNote) {
      return false;
    }
    final width = numericHtmlDimension(element, 'width');
    final height = numericHtmlDimension(element, 'height');
    final styleAttr = element.attributes['style'] ?? '';
    final styleWidth = stylePixelDimension(styleAttr, 'width');
    final styleHeight = stylePixelDimension(styleAttr, 'height');
    final effectiveWidth = width ?? styleWidth;
    final effectiveHeight = height ?? styleHeight;
    if (effectiveWidth == null || effectiveHeight == null) {
      return RegExp(
        r'(^|/)(note|footnote|noteref|marker)\.(png|jpe?g|gif|webp|svg)$',
      ).hasMatch(mediaHref.toLowerCase());
    }
    return effectiveWidth <= 96 && effectiveHeight <= 96;
  }

  double? numericHtmlDimension(dom.Element element, String name) {
    final value = element.attributes[name];
    if (value == null) {
      return null;
    }
    return double.tryParse(
      RegExp(r'[-+]?\d*\.?\d+').firstMatch(value)?.group(0) ?? '',
    );
  }

  double? stylePixelDimension(String style, String name) {
    final match = RegExp(
      '$name\\s*:\\s*([-+]?\\d*\\.?\\d+)\\s*px',
      caseSensitive: false,
    ).firstMatch(style);
    return double.tryParse(match?.group(1) ?? '');
  }

  @override
  bool shouldFallbackForComplexEpubContent(
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
          total + compactReaderText(extractEpubText(element)).length,
    );
    return textLength < 240;
  }

  @override
  bool isHiddenEpubMarker(dom.Element element) {
    return element.classes.contains('sq-spine-marker') ||
        element.attributes.containsKey('hidden');
  }

  @override
  String compactReaderText(String input) {
    return cleanReaderText(input).replaceAll(RegExp(r'\s+'), '');
  }

  @override
  bool isHeadingElement(dom.Element element) {
    final tag = element.localName?.toLowerCase() ?? '';
    return const {'h1', 'h2', 'h3', 'h4', 'h5', 'h6'}.contains(tag);
  }

  @override
  int epubAnchorStartIndex(List<dom.Element> elements, String? anchor) {
    if (anchor == null || anchor.isEmpty) {
      return 0;
    }
    for (var i = 0; i < elements.length; i++) {
      if (elementContainsAnchor(elements[i], anchor)) {
        return i;
      }
    }
    return 0;
  }

  @override
  int epubAnchorEndIndex(
    List<dom.Element> elements,
    String? anchor,
    int start,
  ) {
    if (anchor == null || anchor.isEmpty) {
      return elements.length;
    }
    for (
      var i = (start + 1).clamp(0, elements.length);
      i < elements.length;
      i++
    ) {
      if (elementContainsAnchor(elements[i], anchor)) {
        return i;
      }
    }
    return elements.length;
  }

  @override
  bool elementContainsAnchor(dom.Element element, String anchor) {
    if (element.id == anchor || element.attributes['name'] == anchor) {
      return true;
    }
    return element
        .querySelectorAll('[id], [name]')
        .any(
          (child) => child.id == anchor || child.attributes['name'] == anchor,
        );
  }

  @override
  String cleanReaderText(String input) {
    return input
        .replaceAll('\u00a0', ' ')
        .replaceAll(RegExp(r'\s+'), ' ')
        .trim();
  }

  @override
  bool sameReaderTitle(String a, String b) {
    String normalize(String value) {
      return value
          .replaceAll(RegExp(r'\s+'), '')
          .replaceAll(
            RegExp(
              r'[\u300a\u300b\u300c\u300d\u300e\u300f\u3010\u3011\[\]()（）:：,，.。?？\-—_]+',
            ),
            '',
          )
          .toLowerCase();
    }

    final left = normalize(a);
    final right = normalize(b);
    return left.isNotEmpty && right.isNotEmpty && left == right;
  }
}
