enum BookFormat { txt, epub }

String bookWordCountLabel(int? wordCount) {
  final count = wordCount ?? 0;
  if (count <= 0) {
    return '字数未知';
  }
  if (count >= 1000000) {
    final value = count / 10000;
    return '${value.toStringAsFixed(value >= 100 ? 0 : 1)}万字';
  }
  if (count >= 10000) {
    return '${(count / 10000).toStringAsFixed(1)}万字';
  }
  return '$count 字';
}
