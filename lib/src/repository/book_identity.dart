import '../models.dart';

/// Pure helpers for computing book identity / dedup / sync match keys.
///
/// Carved out of `AppState` so the algorithms can be unit-tested without
/// spinning up a full app state. No mutable state lives here.
class BookIdentity {
  const BookIdentity._();

  /// Normalize free-form identity text (title / author): lowercase, drop all
  /// whitespace, then trim. Two strings whose normalized forms match are
  /// considered the same identity for matching purposes.
  static String normalize(String value) {
    return value.toLowerCase().replaceAll(RegExp(r'\s+'), '').trim();
  }

  /// Normalize a path-like value down to its filename, lowercased and trimmed.
  /// Used to match Android "open with" pending books against the existing
  /// library by their source path.
  static String normalizedFileName(String value) {
    final normalized = value.replaceAll('\\', '/');
    final slash = normalized.lastIndexOf('/');
    return (slash >= 0 ? normalized.substring(slash + 1) : normalized)
        .trim()
        .toLowerCase();
  }

  /// Library dedup key — strict identity within the local library.
  /// Two books that produce the same key are considered duplicates of each
  /// other, regardless of file path.
  static String duplicateKey(BookEntry book) {
    return [
      book.format.name,
      normalize(book.title),
      normalize(book.author),
      book.chapters.length.toString(),
      (book.wordCount ?? -1).toString(),
    ].join('|');
  }

  // ---- Cloud-sync three-tier match keys ----

  /// Strict identity: format + title + author + chapter count + word count.
  static String syncStrictKey(BookEntry book) {
    return strictFromParts(
      book.format.name,
      normalize(book.title),
      normalize(book.author),
      book.chapters.length,
      book.wordCount ?? -1,
    );
  }

  /// Base identity: drops word count, useful when a book has been re-imported
  /// and word count drifts but chapter count is stable.
  static String syncBaseKey(BookEntry book) {
    return baseFromParts(
      book.format.name,
      normalize(book.title),
      normalize(book.author),
      book.chapters.length,
    );
  }

  /// Title identity: format + title + author only. Last-resort fallback.
  static String syncTitleKey(BookEntry book) {
    return titleFromParts(
      book.format.name,
      normalize(book.title),
      normalize(book.author),
    );
  }

  static String strictFromParts(
    String format,
    String title,
    String author,
    int chapterCount,
    int wordCount,
  ) {
    return [
      format,
      title,
      author,
      chapterCount.toString(),
      wordCount.toString(),
    ].join('|');
  }

  static String baseFromParts(
    String format,
    String title,
    String author,
    int chapterCount,
  ) {
    return [format, title, author, chapterCount.toString()].join('|');
  }

  static String titleFromParts(String format, String title, String author) {
    return [format, title, author].join('|');
  }
}
