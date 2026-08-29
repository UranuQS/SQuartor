import 'package:flutter_test/flutter_test.dart';
import 'package:squartor/src/epub_flow.dart';

void main() {
  String sameHref(String href) => href;

  test('normalizes a title page without creating empty blocks', () {
    final flow = normalizeEpubFlow(
      '''
      <html><body><div class="main">
        <p><br /></p>
        <div class="font-1em50">
          <p id="toc-1">特典ショートストーリー</p>
          <p>特典短篇小说</p>
        </div>
        <p>&#160;</p>
      </div></body></html>
      ''',
      resolveLink: sameHref,
      resolveResource: sameHref,
    );

    expect(flow.blocks, hasLength(2));
    expect(flow.isTitlePage, isTrue);
    expect(flow.renderFlow('title.xhtml'), contains('sq-title-page-marker'));
    expect(flow.renderFlow('title.xhtml'), contains('sq-title-lead'));
    expect(flow.renderFlow('title.xhtml'), contains('id="toc-1"'));
    expect(flow.renderFlow('title.xhtml'), isNot(contains('<section')));
  });

  test('keeps semantic inline content and resolves links and images', () {
    final flow = normalizeEpubFlow(
      '''
      <html><body>
        <h2 id="chapter">标题</h2>
        <p>正文<ruby>漢<rt>かん</rt></ruby><a href="next.xhtml#x">下一章</a></p>
        <p><img src="../image/pic.jpg" alt="插图" /></p>
      </body></html>
      ''',
      resolveLink: (href) => 'link:$href',
      resolveResource: (href) => 'resource:$href',
    );

    final html = flow.blocks.join();
    expect(html, contains('<h2 id="chapter">标题</h2>'));
    expect(html, contains('<ruby>漢<rt>かん</rt></ruby>'));
    expect(html, contains('href="link:next.xhtml#x"'));
    expect(html, contains('src="resource:../image/pic.jpg"'));
    expect(flow.mediaCount, 1);
  });

  test('normalizes a fixed-layout svg cover into one image page', () {
    final flow = normalizeEpubFlow(
      '''
      <html><body><svg viewBox="0 0 100 200">
        <image width="100" height="200" xlink:href="../image/page.jpg" />
      </svg></body></html>
      ''',
      resolveLink: sameHref,
      resolveResource: (href) => 'resource:$href',
    );

    expect(flow.isImageOnly, isTrue);
    expect(flow.blocks.single, contains('resource:../image/page.jpg'));
    expect(flow.blocks.single, contains('<img'));
    expect(flow.blocks.single, isNot(contains('<svg')));
  });

  test('ignores tiny note marker images', () {
    final flow = normalizeEpubFlow(
      '''
      <html><body>
        <p>正文<a epub:type="noteref" href="#note-1">
          <img class="footnote-marker" src="../image/note.png" width="24" height="24" alt="注" />
        </a>继续。</p>
        <p><img src="../image/insert.jpg" width="800" height="1200" alt="插图" /></p>
      </body></html>
      ''',
      resolveLink: sameHref,
      resolveResource: (href) => 'resource:$href',
    );

    final html = flow.blocks.join();
    expect(html, isNot(contains('note.png')));
    expect(html, contains('insert.jpg'));
    expect(flow.mediaCount, 1);
  });

  test('converts known note marker images into clickable footnote refs', () {
    final flow = normalizeEpubFlow(
      '''
      <html><body>
        <p>Text<a class="duokan-footnote" epub:type="noteref" href="#note1">
          <sup><img alt="note" class="footnote" src="../Images/note.png" /></sup>
        </a>continues</p>
        <aside epub:type="footnote" id="note1">
          <ol><li>Editor note text.</li></ol>
        </aside>
      </body></html>
      ''',
      resolveLink: sameHref,
      resolveResource: (href) => 'resource:$href',
    );

    final html = flow.blocks.join();
    expect(html, isNot(contains('note.png')));
    expect(html, contains('class="sq-footnote-ref"'));
    expect(html, contains('data-footnote-id="note1"'));
    expect(html, contains('data-footnote='));
    expect(html, contains('</a>continues'));
    expect(html, contains('Text'));
    expect(html, contains('continues'));
    expect(html, isNot(contains('<aside')));
    expect(html, isNot(contains('<ol')));
    expect(flow.mediaCount, 0);
  });

  test('resolves CSS classes, tags, and inline styles into element attributes', () {
    final flow = normalizeEpubFlow(
      '''
      <html>
        <head>
          <style>
            div { color: #f07813; text-align: center; }
            .highlight { color: #ff0000; font-weight: bold; }
          </style>
        </head>
        <body>
          <div>境界设定</div>
          <p class="highlight">重要提示</p>
          <p><font color="#00b4e8">特殊青色</font></p>
        </body>
      </html>
      ''',
      resolveLink: sameHref,
      resolveResource: sameHref,
    );

    final html = flow.blocks.join();
    expect(html, contains('color: #f07813'));
    expect(html, contains('text-align: center'));
    expect(html, contains('境界设定'));
    expect(html, contains('color: #ff0000'));
    expect(html, contains('重要提示'));
    expect(html, contains('color: #00b4e8'));
    expect(html, contains('特殊青色'));
  });
}
