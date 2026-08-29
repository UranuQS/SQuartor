import 'dart:convert';

import 'package:html/dom.dart' as dom;
import 'package:html/parser.dart' as html_parser;

typedef EpubHrefResolver = String Function(String href);

class EpubFlowDocument {
  const EpubFlowDocument({
    required this.blocks,
    required this.textLength,
    required this.mediaCount,
  });

  final List<String> blocks;
  final int textLength;
  final int mediaCount;

  bool get isEmpty => blocks.isEmpty;

  bool get isImageOnly => mediaCount == 1 && textLength == 0;

  bool get isTitlePage =>
      mediaCount == 0 && blocks.length <= 6 && textLength <= 320;

  String renderFlow(String sourceHref) {
    final markerClasses = <String>['sq-spine-marker'];
    if (isTitlePage) {
      markerClasses.add('sq-title-page-marker');
    }
    if (isImageOnly) {
      markerClasses.add('sq-image-page-marker');
    }
    final marker =
        '<span hidden class="${markerClasses.join(' ')}" '
        'data-source="${_attributeEscape.convert(sourceHref)}"></span>';
    if (!isTitlePage) {
      return '$marker${blocks.join()}';
    }

    final fragment = html_parser.parseFragment(blocks.join());
    for (var i = 0; i < fragment.children.length; i++) {
      final element = fragment.children[i];
      element.classes.add('sq-title-block');
      if (i == 0) {
        element.classes.add('sq-title-lead');
      }
    }
    return '$marker${fragment.nodes.map(_outerHtml).join()}';
  }
}

String _outerHtml(dom.Node node) {
  return node is dom.Element
      ? node.outerHtml
      : _textEscape.convert(node.text ?? '');
}

EpubFlowDocument normalizeEpubFlow(
  String source, {
  required EpubHrefResolver resolveLink,
  required EpubHrefResolver resolveResource,
  List<String> extraCssSources = const [],
}) {
  final document = html_parser.parse(source);
  final body = document.body;
  if (body == null) {
    return const EpubFlowDocument(blocks: [], textLength: 0, mediaCount: 0);
  }
  final inlineStyles = document
      .querySelectorAll('style')
      .map((element) => element.text)
      .toList();
  final allCss = [...inlineStyles, ...extraCssSources];
  final cssResolver = _CssStyleResolver(allCss);

  final footnotes = _extractFootnotes(body);
  final builder = _FlowBuilder(
    resolveLink: resolveLink,
    resolveResource: resolveResource,
    footnotes: footnotes,
    cssResolver: cssResolver,
  );
  for (final node in body.nodes) {
    builder.walk(node);
  }

  // Extract body/container CSS background images (e.g. volume/character art covers)
  final bgImage = _extractBackgroundImage(body, allCss);
  if (bgImage != null && bgImage.isNotEmpty) {
    final resolvedBg = resolveResource(bgImage);
    if (resolvedBg.isNotEmpty) {
      builder.insertCoverImage(resolvedBg);
    }
  }

  return EpubFlowDocument(
    blocks: builder.blocks,
    textLength: builder.textLength,
    mediaCount: builder.mediaCount,
  );
}

class _CssStyleResolver {
  _CssStyleResolver(List<String> cssSources) {
    _parseCss(cssSources);
  }

  final Map<String, Map<String, String>> _rules = {};

  void _parseCss(List<String> cssSources) {
    for (final rawCss in cssSources) {
      final css = rawCss.replaceAll(RegExp(r'/\*[\s\S]*?\*/'), '');
      final blockRegex = RegExp(r'([^{]+)\{([^}]+)\}');
      for (final match in blockRegex.allMatches(css)) {
        final rawSelectors = match.group(1) ?? '';
        final rawDecls = match.group(2) ?? '';
        final declMap = <String, String>{};
        for (final decl in rawDecls.split(';')) {
          final colon = decl.indexOf(':');
          if (colon > 0) {
            final prop = decl.substring(0, colon).trim().toLowerCase();
            final val = decl.substring(colon + 1).trim();
            if (prop == 'color' ||
                prop == 'background-color' ||
                prop == 'text-align' ||
                prop == 'font-weight' ||
                prop == 'font-style' ||
                prop == 'text-decoration') {
              declMap[prop] = val;
            }
          }
        }
        if (declMap.isEmpty) continue;
        for (final sel in rawSelectors.split(',')) {
          final s = sel.trim().toLowerCase();
          if (s.isNotEmpty) {
            _rules.putIfAbsent(s, () => {}).addAll(declMap);
          }
        }
      }
    }
  }

  Map<String, String> resolveElementStyles(dom.Element element) {
    final result = <String, String>{};
    final tag = element.localName?.toLowerCase() ?? '';
    final id = element.id.toLowerCase();
    final classes = element.classes.map((c) => c.toLowerCase()).toList();

    // 1. Tag match (e.g. "div", "h2", "p", "span")
    if (_rules.containsKey(tag)) {
      result.addAll(_rules[tag]!);
    }

    // 2. Class matches (e.g. ".orange", "div.orange")
    for (final cls in classes) {
      if (_rules.containsKey('.$cls')) {
        result.addAll(_rules['.$cls']!);
      }
      if (_rules.containsKey('$tag.$cls')) {
        result.addAll(_rules['$tag.$cls']!);
      }
    }

    // 3. ID matches
    if (id.isNotEmpty) {
      if (_rules.containsKey('#$id')) {
        result.addAll(_rules['#$id']!);
      }
    }

    // 4. <font color="..."> legacy tag
    if (tag == 'font') {
      final color = element.attributes['color'];
      if (color != null && color.isNotEmpty) {
        result['color'] = color;
      }
    }

    // 5. Inline style attribute (highest priority)
    final inlineStyle = element.attributes['style'];
    if (inlineStyle != null && inlineStyle.isNotEmpty) {
      for (final decl in inlineStyle.split(';')) {
        final colon = decl.indexOf(':');
        if (colon > 0) {
          final prop = decl.substring(0, colon).trim().toLowerCase();
          final val = decl.substring(colon + 1).trim();
          if (prop == 'color' ||
              prop == 'background-color' ||
              prop == 'text-align' ||
              prop == 'font-weight' ||
              prop == 'font-style' ||
              prop == 'text-decoration') {
            result[prop] = val;
          }
        }
      }
    }

    return result;
  }
}

String? _extractBackgroundImage(dom.Element body, List<String> cssSources) {
  // 1. Check inline style on body
  final style = body.attributes['style'] ?? '';
  final inlineMatch = RegExp(
    r'background(?:-image)?\s*:\s*[^;}]*url\(\s*[\x27\x22]?([^\x27\x22)]+)[\x27\x22]?\s*\)',
    caseSensitive: false,
  ).firstMatch(style);
  if (inlineMatch != null) {
    return inlineMatch.group(1)?.trim();
  }

  // 2. Check body classes, id, or body tag in CSS sources
  final bodyClasses = body.classes.toList();
  final bodyId = body.id;

  for (final css in cssSources) {
    for (final cls in bodyClasses) {
      final classRegex = RegExp(
        r'(?:body)?\.' +
            RegExp.escape(cls) +
            r'\s*\{[^}]*background(?:-image)?\s*:\s*[^;}]*url\(\s*[\x27\x22]?([^\x27\x22)]+)[\x27\x22]?\s*\)',
        caseSensitive: false,
      );
      final match = classRegex.firstMatch(css);
      if (match != null) {
        return match.group(1)?.trim();
      }
    }
    if (bodyId.isNotEmpty) {
      final idRegex = RegExp(
        r'#' +
            RegExp.escape(bodyId) +
            r'\s*\{[^}]*background(?:-image)?\s*:\s*[^;}]*url\(\s*[\x27\x22]?([^\x27\x22)]+)[\x27\x22]?\s*\)',
        caseSensitive: false,
      );
      final match = idRegex.firstMatch(css);
      if (match != null) {
        return match.group(1)?.trim();
      }
    }
    final bodyRegex = RegExp(
      r'(?:^|\})\s*body\s*\{[^}]*background(?:-image)?\s*:\s*[^;}]*url\(\s*[\x27\x22]?([^\x27\x22)]+)[\x27\x22]?\s*\)',
      caseSensitive: false,
    );
    final match = bodyRegex.firstMatch(css);
    if (match != null) {
      return match.group(1)?.trim();
    }
  }

  // 3. Check child elements with background classes if no image found yet
  for (final el in body.children) {
    final elStyle = el.attributes['style'] ?? '';
    final elMatch = RegExp(
      r'background(?:-image)?\s*:\s*[^;}]*url\(\s*[\x27\x22]?([^\x27\x22)]+)[\x27\x22]?\s*\)',
      caseSensitive: false,
    ).firstMatch(elStyle);
    if (elMatch != null) {
      return elMatch.group(1)?.trim();
    }
    for (final cls in el.classes) {
      for (final css in cssSources) {
        final classRegex = RegExp(
          r'\.' +
              RegExp.escape(cls) +
              r'\s*\{[^}]*background(?:-image)?\s*:\s*[^;}]*url\(\s*[\x27\x22]?([^\x27\x22)]+)[\x27\x22]?\s*\)',
          caseSensitive: false,
        );
        final match = classRegex.firstMatch(css);
        if (match != null) {
          return match.group(1)?.trim();
        }
      }
    }
  }

  return null;
}

class _FlowBuilder {
  _FlowBuilder({
    required this.resolveLink,
    required this.resolveResource,
    required this.footnotes,
    this.cssResolver,
  });

  static const _ignoredTags = {'script', 'style', 'noscript', 'template'};
  static const _headingTags = {'h1', 'h2', 'h3', 'h4', 'h5', 'h6'};
  static const _containerTags = {
    'body',
    'main',
    'article',
    'section',
    'header',
    'footer',
    'aside',
    'nav',
    'div',
    'figure',
    'figcaption',
  };
  static const _inlineTags = {
    'a',
    'abbr',
    'b',
    'cite',
    'code',
    'del',
    'em',
    'i',
    'ins',
    'kbd',
    'mark',
    'q',
    'rp',
    'rt',
    'ruby',
    's',
    'samp',
    'small',
    'strong',
    'sub',
    'sup',
    'time',
    'u',
    'var',
  };

  final EpubHrefResolver resolveLink;
  final EpubHrefResolver resolveResource;
  final Map<String, String> footnotes;
  final _CssStyleResolver? cssResolver;
  final List<String> blocks = [];
  var textLength = 0;
  var mediaCount = 0;

  void insertCoverImage(String resolvedSrc) {
    blocks.insert(
      0,
      '<div class="sq-media sq-cover-media"><img src="${_attributeEscape.convert(resolvedSrc)}" alt="" /></div>',
    );
    mediaCount += 1;
  }

  String _elementAttributes(
    dom.Element element, {
    String? extraClass,
    bool skipStyle = false,
  }) {
    final attributes = <String>[];
    final id = element.attributes['id'];
    final name = element.attributes['name'];
    if (id != null && id.isNotEmpty) {
      attributes.add('id="${_attributeEscape.convert(id)}"');
    }
    if (name != null && name.isNotEmpty && name != id) {
      attributes.add('name="${_attributeEscape.convert(name)}"');
    }

    final classList = <String>[];
    if (extraClass != null && extraClass.isNotEmpty) {
      classList.add(extraClass);
    }
    for (final c in element.classes) {
      if (!classList.contains(c)) {
        classList.add(c);
      }
    }
    if (classList.isNotEmpty) {
      attributes.add('class="${_attributeEscape.convert(classList.join(' '))}"');
    }

    if (!skipStyle && cssResolver != null) {
      final styles = cssResolver!.resolveElementStyles(element);
      if (styles.isNotEmpty) {
        final styleStr = styles.entries
            .map((e) => '${e.key}: ${e.value}')
            .join('; ');
        attributes.add('style="${_attributeEscape.convert(styleStr)}"');
      }
    }

    return attributes.isEmpty ? '' : ' ${attributes.join(' ')}';
  }

  void walk(dom.Node node) {
    if (node is dom.Text) {
      final text = _cleanText(node.data);
      if (text.isNotEmpty) {
        _addTextBlock('p', _textEscape.convert(text));
      }
      return;
    }
    if (node is! dom.Element) {
      return;
    }
    final tag = node.localName?.toLowerCase() ?? '';
    if (_ignoredTags.contains(tag)) {
      return;
    }
    if (_isFootnoteContainer(node)) {
      return;
    }
    if (_headingTags.contains(tag)) {
      _addElementBlock(tag, node);
      return;
    }
    if (tag == 'p' || tag == 'li' || tag == 'dt' || tag == 'dd') {
      _addElementBlock(
        'p',
        node,
        extraClass: tag == 'li' ? 'sq-list-item' : null,
      );
      return;
    }
    if (tag == 'blockquote') {
      _addElementBlock('p', node, extraClass: 'sq-quote');
      return;
    }
    if (tag == 'pre') {
      final text = _cleanText(node.text, preserveLines: true);
      if (text.isNotEmpty) {
        textLength += _compactLength(text);
        blocks.add(
          '<pre${_elementAttributes(node)}>${_textEscape.convert(text)}</pre>',
        );
      }
      return;
    }
    if (tag == 'img' || tag == 'svg') {
      if (_isAnnotationMarkerMedia(node)) {
        return;
      }
      _addMediaBlock(node);
      return;
    }
    if (tag == 'hr') {
      blocks.add('<hr${_elementAttributes(node)} />');
      return;
    }
    if (tag == 'table') {
      for (final row in node.querySelectorAll('tr')) {
        final cells = row.querySelectorAll('th, td');
        final text = cells
            .map((cell) => _cleanText(cell.text))
            .where((value) => value.isNotEmpty)
            .join('　');
        if (text.isNotEmpty) {
          _addTextBlock(
            'p',
            _textEscape.convert(text),
            extraClass: 'sq-table-row',
          );
        }
      }
      return;
    }
    if (tag == 'br') {
      return;
    }
    if (_containerTags.contains(tag)) {
      if (_hasDirectBlockChild(node)) {
        for (final child in node.nodes) {
          walk(child);
        }
      } else {
        _addElementBlock('p', node);
      }
      return;
    }
    if (_inlineTags.contains(tag) || tag == 'span') {
      _addElementBlock('p', node);
      return;
    }
    for (final child in node.nodes) {
      walk(child);
    }
  }

  bool _hasDirectBlockChild(dom.Element element) {
    return element.children.any((child) {
      final tag = child.localName?.toLowerCase() ?? '';
      return _headingTags.contains(tag) ||
          _containerTags.contains(tag) ||
          const {
            'p',
            'li',
            'dt',
            'dd',
            'blockquote',
            'pre',
            'table',
            'hr',
            'img',
            'svg',
          }.contains(tag);
    });
  }

  void _addElementBlock(
    String outputTag,
    dom.Element source, {
    String? extraClass,
  }) {
    final text = _cleanText(source.text);
    final inner = source.nodes.map(_inlineHtml).join();
    final hasMedia = source.querySelector('img, svg') != null;
    if (text.isEmpty && !hasMedia) {
      return;
    }
    if (hasMedia && text.isEmpty) {
      for (final media in source.querySelectorAll('img, svg')) {
        if (_isAnnotationMarkerMedia(media)) {
          continue;
        }
        _addMediaBlock(media);
      }
      return;
    }
    textLength += _compactLength(text);
    blocks.add(
      '<$outputTag${_elementAttributes(source, extraClass: extraClass)}>$inner</$outputTag>',
    );
  }

  void _addTextBlock(String tag, String escapedText, {String? extraClass}) {
    final plain = _cleanText(escapedText);
    if (plain.isEmpty) {
      return;
    }
    textLength += _compactLength(plain);
    final classAttribute = extraClass == null ? '' : ' class="$extraClass"';
    blocks.add('<$tag$classAttribute>$escapedText</$tag>');
  }

  void _addMediaBlock(dom.Element source) {
    final html = _mediaHtml(source);
    if (html.isEmpty) {
      return;
    }
    mediaCount += 1;
    blocks.add('<div class="sq-media"${_elementAttributes(source)}>$html</div>');
  }

  String _inlineHtml(dom.Node node) {
    if (node is dom.Text) {
      return _textEscape.convert(node.data);
    }
    if (node is! dom.Element) {
      return '';
    }
    final tag = node.localName?.toLowerCase() ?? '';
    if (_ignoredTags.contains(tag)) {
      return '';
    }
    if (_isFootnoteContainer(node)) {
      return '';
    }
    if (tag == 'img' || tag == 'svg') {
      if (_isAnnotationMarkerMedia(node)) {
        return '';
      }
      return _mediaHtml(node);
    }
    if (tag == 'br') {
      return '<br />';
    }
    final children = node.nodes.map(_inlineHtml).join();
    if (tag == 'a') {
      final rawHref = node.attributes['href'];
      final footnoteId = _footnoteIdFromHref(rawHref);
      final footnoteText = footnoteId == null ? null : footnotes[footnoteId];
      if (footnoteText != null && footnoteText.isNotEmpty) {
        final href = rawHref == null || rawHref.isEmpty
            ? ''
            : ' href="${_attributeEscape.convert(resolveLink(rawHref))}"';
        return '<a class="sq-footnote-ref"$href '
            'data-footnote-id="${_attributeEscape.convert(footnoteId!)}" '
            'data-footnote="${_attributeEscape.convert(footnoteText)}">'
            '\u6ce8</a>';
      }
      final href = rawHref == null || rawHref.isEmpty
          ? ''
          : ' href="${_attributeEscape.convert(resolveLink(rawHref))}"';
      return '<a$href${_elementAttributes(node)}>$children</a>';
    }
    if (_inlineTags.contains(tag)) {
      return '<$tag${_elementAttributes(node)}>$children</$tag>';
    }
    if (tag == 'font') {
      return '<span${_elementAttributes(node)}>$children</span>';
    }
    return '<span${_elementAttributes(node)}>$children</span>';
  }

  String _mediaHtml(dom.Element source) {
    final tag = source.localName?.toLowerCase() ?? '';
    if (tag == 'img') {
      final rawSrc = source.attributes['src'];
      if (rawSrc == null || rawSrc.isEmpty) {
        return '';
      }
      final src = _attributeEscape.convert(resolveResource(rawSrc));
      final alt = _attributeEscape.convert(source.attributes['alt'] ?? '');
      return '<img src="$src" alt="$alt" />';
    }
    if (tag != 'svg') {
      return '';
    }
    final svgImage = _singleSvgImageHtml(source);
    if (svgImage != null) {
      return svgImage;
    }
    return _svgHtml(source);
  }

  String? _singleSvgImageHtml(dom.Element source) {
    final images = source.querySelectorAll('image');
    if (images.length != 1) {
      return null;
    }
    final image = images.single;
    final rawHref =
        image.attributes['href'] ??
        image.attributes['xlink:href'] ??
        image.attributes.entries
            .where((entry) => entry.key.toString().endsWith(':href'))
            .map((entry) => entry.value)
            .firstOrNull;
    if (rawHref == null || rawHref.isEmpty) {
      return null;
    }
    final src = _attributeEscape.convert(resolveResource(rawHref));
    return '<img src="$src" alt="" />';
  }

  String _svgHtml(dom.Element element) {
    final tag = element.localName?.toLowerCase() ?? 'g';
    final attributes = <String>[];
    for (final entry in element.attributes.entries) {
      final name = entry.key.toString();
      if (name == 'style' || name == 'class') {
        continue;
      }
      final isResource =
          name == 'href' || name == 'xlink:href' || name.endsWith(':href');
      final value = isResource ? resolveResource(entry.value) : entry.value;
      attributes.add(
        '${_attributeEscape.convert(name)}="${_attributeEscape.convert(value)}"',
      );
    }
    final attributeText = attributes.isEmpty ? '' : ' ${attributes.join(' ')}';
    final children = element.nodes.map((node) {
      if (node is dom.Text) {
        return _textEscape.convert(node.data);
      }
      if (node is dom.Element) {
        return _svgHtml(node);
      }
      return '';
    }).join();
    return '<$tag$attributeText>$children</$tag>';
  }

  String _anchorAttributes(dom.Element element) {
    final attributes = <String>[];
    final id = element.attributes['id'];
    final name = element.attributes['name'];
    if (id != null && id.isNotEmpty) {
      attributes.add('id="${_attributeEscape.convert(id)}"');
    }
    if (name != null && name.isNotEmpty && name != id) {
      attributes.add('name="${_attributeEscape.convert(name)}"');
    }
    return attributes.isEmpty ? '' : ' ${attributes.join(' ')}';
  }

  bool _isAnnotationMarkerMedia(dom.Element element) {
    final tag = element.localName?.toLowerCase() ?? '';
    if (tag != 'img' && tag != 'svg') {
      return false;
    }
    final mediaHref =
        element.attributes['src'] ??
        element.attributes['href'] ??
        element.attributes['xlink:href'] ??
        '';
    final source = [
      mediaHref,
      element.attributes['alt'],
      element.attributes['title'],
      element.attributes['class'],
      element.attributes['id'],
    ].whereType<String>().join(' ').toLowerCase();
    final parentText = [
      element.parent?.attributes['class'],
      element.parent?.attributes['id'],
      element.parent?.attributes['epub:type'],
      element.parent?.attributes['type'],
      element.parent?.attributes['role'],
    ].whereType<String>().join(' ').toLowerCase();
    final markerText = '$source $parentText';
    final looksLikeNote =
        RegExp(
          r'(footnote|noteref|note|annotation|marker|kobo|duokan|kindle)',
        ).hasMatch(markerText) ||
        markerText.contains('注') ||
        markerText.contains('脚') ||
        markerText.contains('註');
    if (!looksLikeNote) {
      return false;
    }
    final width = _numericAttribute(element, 'width');
    final height = _numericAttribute(element, 'height');
    final style = element.attributes['style'] ?? '';
    final styleWidth = _stylePixelValue(style, 'width');
    final styleHeight = _stylePixelValue(style, 'height');
    final effectiveWidth = width ?? styleWidth;
    final effectiveHeight = height ?? styleHeight;
    if (effectiveWidth == null || effectiveHeight == null) {
      return RegExp(
        r'(^|/)(note|footnote|noteref|marker)\.(png|jpe?g|gif|webp|svg)$',
      ).hasMatch(mediaHref.toLowerCase());
    }
    return effectiveWidth <= 96 && effectiveHeight <= 96;
  }

  double? _numericAttribute(dom.Element element, String name) {
    final value = element.attributes[name];
    if (value == null) {
      return null;
    }
    return double.tryParse(
      RegExp(r'[-+]?\d*\.?\d+').firstMatch(value)?.group(0) ?? '',
    );
  }

  double? _stylePixelValue(String style, String name) {
    final match = RegExp(
      '$name\\s*:\\s*([-+]?\\d*\\.?\\d+)\\s*px',
      caseSensitive: false,
    ).firstMatch(style);
    return double.tryParse(match?.group(1) ?? '');
  }
}

Map<String, String> _extractFootnotes(dom.Element body) {
  final result = <String, String>{};
  for (final element in body.querySelectorAll('[id], [name]')) {
    if (!_isFootnoteContainer(element)) {
      continue;
    }
    final id = element.attributes['id'] ?? element.attributes['name'];
    if (id == null || id.isEmpty || result.containsKey(id)) {
      continue;
    }
    final text = _cleanText(_footnoteReadableText(element));
    if (text.isNotEmpty) {
      result[id] = text;
    }
  }
  return result;
}

String _footnoteReadableText(dom.Node node) {
  if (node is dom.Text) {
    return node.data;
  }
  if (node is! dom.Element) {
    return '';
  }
  final tag = node.localName?.toLowerCase() ?? '';
  if (const {'script', 'style', 'noscript', 'template'}.contains(tag)) {
    return '';
  }
  if (tag == 'a') {
    final href = node.attributes['href'] ?? '';
    if (href.startsWith('#') && _cleanText(node.text).isEmpty) {
      return '';
    }
  }
  if (tag == 'br') {
    return '\n';
  }
  return node.nodes.map(_footnoteReadableText).join(' ');
}

bool _isFootnoteContainer(dom.Element element) {
  final tag = element.localName?.toLowerCase() ?? '';
  final epubType = [
    element.attributes['epub:type'],
    element.attributes['type'],
    element.attributes['role'],
  ].whereType<String>().join(' ').toLowerCase();
  if (epubType.contains('noteref')) {
    return false;
  }
  final marker = [
    tag,
    epubType,
    element.attributes['class'],
    element.attributes['id'],
  ].whereType<String>().join(' ').toLowerCase();
  return tag == 'aside' && marker.contains('footnote') ||
      marker.contains('duokan-footnote-content') ||
      marker.contains('duokan-footnote-item') ||
      marker.contains('endnote') ||
      marker.contains('sq-footnote-body');
}

String? _footnoteIdFromHref(String? href) {
  if (href == null || href.isEmpty) {
    return null;
  }
  final hash = href.lastIndexOf('#');
  if (hash < 0 || hash == href.length - 1) {
    return null;
  }
  return href.substring(hash + 1);
}

String _cleanText(String input, {bool preserveLines = false}) {
  final normalized = input.replaceAll('\u00a0', ' ');
  if (preserveLines) {
    return normalized
        .replaceAll(RegExp(r'[ \t]+'), ' ')
        .replaceAll(RegExp(r'\n{3,}'), '\n\n')
        .trim();
  }
  return normalized.replaceAll(RegExp(r'\s+'), ' ').trim();
}

int _compactLength(String input) => input.replaceAll(RegExp(r'\s+'), '').length;

final _textEscape = const HtmlEscape(HtmlEscapeMode.element);
final _attributeEscape = const HtmlEscape(HtmlEscapeMode.attribute);
