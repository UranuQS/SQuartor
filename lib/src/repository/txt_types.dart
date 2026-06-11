class TxtDocument {
  const TxtDocument({
    required this.title,
    required this.fileName,
    required this.paragraphs,
  });

  final String title;
  final String fileName;
  final List<String> paragraphs;
}
