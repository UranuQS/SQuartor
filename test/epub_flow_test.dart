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
}
