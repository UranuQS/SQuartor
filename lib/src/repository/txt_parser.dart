import 'dart:convert';
import 'dart:io';

import 'package:path/path.dart' as path;

import '../models.dart';
import 'txt_types.dart';

class TxtParser {
  static const txtReaderVersion = '4';
  static const txtReaderVersionFile = '.txt-flow-version';
  static const maxTxtDocumentChars = 180000;

  static Future<List<ReaderChapter>> prepareTxtReader({
    required File sourceFile,
    required Directory bookDir,
    required String title,
    required String Function(List<int> bytes) decodeText,
  }) async {
    final readerDir = Directory(path.join(bookDir.path, 'txt-reader'));
    if (await readerDir.exists()) {
      await readerDir.delete(recursive: true);
    }
    await readerDir.create(recursive: true);
    final documents = buildTxtDocuments(
      decodeText(await sourceFile.readAsBytes()),
      title,
    );
    final chapters = <ReaderChapter>[];
    for (final document in documents) {
      final output = File(path.join(readerDir.path, document.fileName));
      await output.writeAsString(
        renderTxtDocument(document),
        encoding: utf8,
        flush: false,
      );
      chapters.add(
        ReaderChapter(
          title: document.title,
          href: '${document.fileName}#top',
          filePath: output.path,
          anchor: 'top',
        ),
      );
    }
    await File(
      path.join(readerDir.path, txtReaderVersionFile),
    ).writeAsString(txtReaderVersion, flush: true);
    return chapters;
  }

  static List<TxtDocument> buildTxtDocuments(String text, String title) {
    final documents = <TxtDocument>[];
    final paragraphs = <String>[];
    final normalized = text
        .replaceAll('\uFEFF', '')
        .replaceAll('\r\n', '\n')
        .replaceAll('\r', '\n');
    var currentTitle = '正文';
    var currentPart = 1;
    var documentChars = 0;
    var hasExplicitTitle = false;
    String? pendingParagraph;

    void commitDocument() {
      if (paragraphs.isEmpty && documents.isNotEmpty) {
        return;
      }
      final index = documents.length;
      final baseTitle = currentTitle.trim().isEmpty ? title : currentTitle;
      final documentTitle = currentPart <= 1
          ? baseTitle
          : '$baseTitle（$currentPart）';
      documents.add(
        TxtDocument(
          title: documentTitle,
          fileName: 'txt-${index.toString().padLeft(4, '0')}.html',
          paragraphs: List<String>.of(paragraphs),
        ),
      );
      paragraphs.clear();
      documentChars = 0;
      currentPart += 1;
    }

    void addParagraph(String content) {
      for (final part in splitLongTxtParagraph(content)) {
        final paragraph = part.trim();
        if (paragraph.isEmpty) {
          continue;
        }
        if (paragraphs.isNotEmpty &&
            documentChars + paragraph.length > maxTxtDocumentChars) {
          commitDocument();
        }
        paragraphs.add(paragraph);
        documentChars += paragraph.length;
      }
    }

    void flushParagraph() {
      final content = pendingParagraph?.trim();
      if (content != null && content.isNotEmpty) {
        addParagraph(content);
      }
      pendingParagraph = null;
    }

    void startChapter(String chapterTitle) {
      flushParagraph();
      if (paragraphs.isNotEmpty || (hasExplicitTitle && documents.isEmpty)) {
        commitDocument();
      }
      currentTitle = normalizeTxtTitle(chapterTitle);
      currentPart = 1;
      hasExplicitTitle = true;
    }

    for (final line in const LineSplitter().convert(normalized)) {
      final raw = line.replaceAll('\t', '  ').trimRight();
      final trimmed = raw.trim();
      if (isTxtChapterTitle(trimmed)) {
        startChapter(trimmed);
        continue;
      }
      if (trimmed.isEmpty) {
        flushParagraph();
        continue;
      }
      final startsIndented = RegExp(r'^[\s　]{2,}').hasMatch(raw);
      if (pendingParagraph == null) {
        pendingParagraph = trimmed;
      } else if (shouldJoinTxtLine(
        previous: pendingParagraph!,
        next: trimmed,
        nextStartsIndented: startsIndented,
      )) {
        pendingParagraph = '$pendingParagraph$trimmed';
      } else {
        flushParagraph();
        pendingParagraph = trimmed;
      }
    }
    flushParagraph();
    commitDocument();
    if (documents.isEmpty) {
      documents.add(
        TxtDocument(
          title: title,
          fileName: 'txt-0000.html',
          paragraphs: const [],
        ),
      );
    }
    return documents;
  }

  static String normalizeTxtTitle(String title) {
    return title.replaceAll(RegExp(r'\s+'), ' ').trim();
  }

  static bool isTxtChapterTitle(String trimmed) {
    if (trimmed.isEmpty || trimmed.length > 64) {
      return false;
    }
    final title = normalizeTxtTitle(trimmed);
    if (title.length < 2) {
      return false;
    }
    if (RegExp(
      r'^(?:https?://|www\.|qq|QQ群|群号|书友群|作者|PS[:：])',
      caseSensitive: false,
    ).hasMatch(title)) {
      return false;
    }
    if (RegExp(r'^[0-9０-９]{1,4}[:：][0-9０-９]{1,2}$').hasMatch(title)) {
      return false;
    }

    const cnNumber = '零〇一二三四五六七八九十百千万两壹贰叁肆伍陆柒捌玖拾佰仟';
    final chapterNumber = '[$cnNumber\\d０-９]+';
    final chapterUnits = '[章章节卷回部幕集话話]';
    final shortTail = r'(?:[\s:：、．.\-—].{0,48}|.{0,24})$';
    final patterns = [
      RegExp('^第\\s*$chapterNumber\\s*$chapterUnits$shortTail'),
      RegExp('^$chapterNumber\\s*$chapterUnits$shortTail'),
      RegExp(r'^[0-9０-９]{3,5}\s*[.．、:：\-]?\s*\S.{0,56}$'),
      RegExp(
        r'^(?:chapter|chap|ch\.?|episode|ep\.?|volume|vol\.?)\s+[0-9ivxlcdm]+(?:[\s:：.\-].{0,56})?$',
        caseSensitive: false,
      ),
      RegExp(r'^(?:序章|楔子|终章|尾声|后记|番外|幕间|插章)(?:[\s:：、．.\-].{0,56}|.{0,20})$'),
    ];
    return patterns.any((pattern) => pattern.hasMatch(title));
  }

  static bool shouldJoinTxtLine({
    required String previous,
    required String next,
    required bool nextStartsIndented,
  }) {
    if (nextStartsIndented) {
      return false;
    }
    if (endsTxtParagraph(previous)) {
      return false;
    }
    if (previous.length > 48 || next.length > 48) {
      return false;
    }
    if (RegExp(r'^[""「『（(《]').hasMatch(next)) {
      return false;
    }
    return true;
  }

  static bool endsTxtParagraph(String text) {
    return RegExp(r'[。！？!?；;：:…」』”’）)\]\]》]$').hasMatch(text.trim());
  }

  static List<String> splitLongTxtParagraph(String paragraph) {
    if (paragraph.length <= 420) {
      return [paragraph];
    }
    final parts = <String>[];
    final buffer = StringBuffer();
    for (final rune in paragraph.runes) {
      final char = String.fromCharCode(rune);
      buffer.write(char);
      final canBreak = RegExp(r'[。！？!?；;」』”’）)]').hasMatch(char);
      if ((buffer.length >= 160 && canBreak) || buffer.length >= 280) {
        parts.add(buffer.toString().trim());
        buffer.clear();
      }
    }
    final rest = buffer.toString().trim();
    if (rest.isNotEmpty) {
      parts.add(rest);
    }
    return parts;
  }

  static String renderTxtDocument(TxtDocument document) {
    final buffer = StringBuffer()
      ..writeln('<!doctype html><html><head><meta charset="utf-8">')
      ..writeln(
        '<meta name="viewport" content="width=device-width, initial-scale=1">',
      )
      ..writeln('<title>${htmlEscape.convert(document.title)}</title></head>')
      ..writeln('<body class="sq-txt-document">')
      ..writeln('<h1 id="top">${htmlEscape.convert(document.title)}</h1>');
    for (final paragraph in document.paragraphs) {
      buffer.writeln('<p>${htmlEscape.convert(paragraph)}</p>');
    }
    buffer.writeln('</body></html>');
    return buffer.toString();
  }
}
