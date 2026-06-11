import 'package:lpinyin/lpinyin.dart';

import '../models.dart';

enum ShelfSortMode {
  name('按名称'),
  recent('最近阅读');

  const ShelfSortMode(this.label);

  final String label;
}

enum BookMenuAction { edit, series, select }

enum ShelfMenuAction { import, sort, create }

class ShelfBookBlock {
  const ShelfBookBlock({
    required this.key,
    required this.title,
    required this.author,
    required this.books,
    this.manual = false,
  });

  final String key;
  final String title;
  final String author;
  final List<BookEntry> books;
  final bool manual;

  int get chapterCount =>
      books.fold(0, (sum, book) => sum + book.chapters.length);

  int? get wordCount {
    var total = 0;
    var hasValue = false;
    for (final book in books) {
      final count = book.wordCount;
      if (count != null && count > 0) {
        hasValue = true;
        total += count;
      }
    }
    return hasValue ? total : null;
  }

  double get progress {
    if (books.isEmpty) {
      return 0;
    }
    return books.fold<double>(0, (sum, book) => sum + book.progress) /
        books.length;
  }

  DateTime get lastTouchedAt {
    return books
        .map((book) => book.lastReadAt ?? book.importedAt)
        .reduce((a, b) => a.isAfter(b) ? a : b);
  }
}

class _NaturalSortToken {
  const _NaturalSortToken.text(this.text) : number = null;
  const _NaturalSortToken.number(this.number) : text = null;

  final String? text;
  final int? number;
}

int compareNaturalText(String left, String right) {
  final leftTokens = _naturalSortTokens(left);
  final rightTokens = _naturalSortTokens(right);
  final length = leftTokens.length < rightTokens.length
      ? leftTokens.length
      : rightTokens.length;
  for (var i = 0; i < length; i++) {
    final leftToken = leftTokens[i];
    final rightToken = rightTokens[i];
    final leftNumber = leftToken.number;
    final rightNumber = rightToken.number;
    if (leftNumber != null && rightNumber != null) {
      final compared = leftNumber.compareTo(rightNumber);
      if (compared != 0) {
        return compared;
      }
      continue;
    }
    if (leftNumber != null || rightNumber != null) {
      return leftNumber != null ? -1 : 1;
    }
    final compared = pinyinSortKey(
      leftToken.text!,
    ).compareTo(pinyinSortKey(rightToken.text!));
    if (compared != 0) {
      return compared;
    }
  }
  final byLength = leftTokens.length.compareTo(rightTokens.length);
  if (byLength != 0) {
    return byLength;
  }
  return left.toLowerCase().compareTo(right.toLowerCase());
}

String pinyinSortKey(String value) {
  final normalized = value.trim().toLowerCase();
  if (normalized.isEmpty) {
    return normalized;
  }
  return PinyinHelper.getPinyinE(
    normalized,
    separator: '',
    defPinyin: normalized,
  ).toLowerCase();
}

List<_NaturalSortToken> _naturalSortTokens(String value) {
  final tokens = <_NaturalSortToken>[];
  final text = <int>[];
  final runes = value.trim().toLowerCase().runes.toList();

  void flushText() {
    if (text.isEmpty) {
      return;
    }
    tokens.add(_NaturalSortToken.text(String.fromCharCodes(text)));
    text.clear();
  }

  var index = 0;
  while (index < runes.length) {
    final rune = runes[index];
    if (_isAsciiDigit(rune)) {
      flushText();
      final start = index;
      while (index < runes.length && _isAsciiDigit(runes[index])) {
        index++;
      }
      final number = int.tryParse(
        String.fromCharCodes(runes.sublist(start, index)),
      );
      tokens.add(_NaturalSortToken.number(number ?? 0));
      continue;
    }
    if (_isChineseNumberRune(rune)) {
      flushText();
      final start = index;
      while (index < runes.length && _isChineseNumberRune(runes[index])) {
        index++;
      }
      tokens.add(
        _NaturalSortToken.number(
          parseChineseNumber(runes.sublist(start, index)),
        ),
      );
      continue;
    }
    text.add(rune);
    index++;
  }
  flushText();
  return tokens;
}

bool _isAsciiDigit(int rune) => rune >= 48 && rune <= 57;

bool _isChineseNumberRune(int rune) {
  return _chineseDigitValue(rune) >= 0 || _chineseUnitValue(rune) > 0;
}

int parseChineseNumber(List<int> runes) {
  var hasUnit = false;
  for (final rune in runes) {
    if (_chineseUnitValue(rune) > 0) {
      hasUnit = true;
      break;
    }
  }
  if (!hasUnit) {
    var value = 0;
    for (final rune in runes) {
      value = value * 10 + _chineseDigitValue(rune).clamp(0, 9).toInt();
    }
    return value;
  }

  var total = 0;
  var section = 0;
  var digit = 0;
  for (final rune in runes) {
    final digitValue = _chineseDigitValue(rune);
    if (digitValue >= 0) {
      digit = digitValue;
      continue;
    }
    final unit = _chineseUnitValue(rune);
    if (unit == 10000) {
      section += digit;
      total += (section == 0 ? 1 : section) * unit;
      section = 0;
      digit = 0;
    } else if (unit > 0) {
      section += (digit == 0 ? 1 : digit) * unit;
      digit = 0;
    }
  }
  return total + section + digit;
}

int _chineseDigitValue(int rune) {
  return switch (rune) {
    12295 || 38646 => 0,
    19968 => 1,
    20108 || 20004 => 2,
    19977 => 3,
    22235 => 4,
    20115 => 5,
    20845 => 6,
    19971 => 7,
    20843 => 8,
    20061 => 9,
    _ => -1,
  };
}

int _chineseUnitValue(int rune) {
  return switch (rune) {
    21313 => 10,
    30334 => 100,
    21315 => 1000,
    19975 => 10000,
    _ => 0,
  };
}

const defaultShelfName = '已收藏';
const defaultShelfLabel = '全部';
