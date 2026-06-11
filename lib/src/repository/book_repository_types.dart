import '../models.dart';

class BookRepositorySnapshot {
  const BookRepositorySnapshot({
    required this.books,
    required this.fonts,
    required this.shelves,
    required this.readingStats,
    required this.style,
  });

  final List<BookEntry> books;
  final List<ImportedFont> fonts;
  final List<String> shelves;
  final Map<String, Map<String, int>> readingStats;
  final ReadingStyle style;
}

class PendingOpenBook {
  const PendingOpenBook({required this.path, required this.name, this.size});

  final String path;
  final String name;
  final int? size;
}
