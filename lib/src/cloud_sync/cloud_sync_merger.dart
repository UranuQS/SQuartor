import 'dart:math' as math;

import 'package:flutter/foundation.dart';

import '../models.dart';
import '../repository/book_identity.dart';

/// Result of merging a downloaded cloud sync payload against the local
/// library. Returned to `AppState` which is responsible for actually swapping
/// fields in / persisting / firing notifiers.
class CloudSyncMergeResult {
  const CloudSyncMergeResult({
    required this.shelves,
    required this.shelvesChanged,
    required this.books,
    required this.booksChanged,
    required this.readingStats,
    required this.readingStatsChanged,
  });

  final List<String> shelves;
  final bool shelvesChanged;
  final List<BookEntry> books;
  final bool booksChanged;
  final Map<String, Map<String, int>> readingStats;
  final bool readingStatsChanged;
}

/// Pure logic for the WebDAV cloud-sync feature: payload encode/decode,
/// three-tier identity matching (strict → base → title), and conflict-free
/// merging of remote progress / bookmarks / shelves / per-day reading stats.
///
/// Carved out of `AppState` so the algorithms can be unit-tested without a
/// full app state. The class itself is stateless — every method takes the
/// inputs it needs explicitly.
class CloudSyncMerger {
  const CloudSyncMerger();

  // ---- Encode ---------------------------------------------------------

  /// Build the JSON-serializable payload uploaded to WebDAV.
  Map<String, Object?> buildPayload({
    required List<BookEntry> books,
    required List<String> shelves,
    required Map<String, Map<String, int>> readingStats,
    required DateTime now,
  }) {
    return {
      'schema': 1,
      'updatedAt': now.toIso8601String(),
      'shelves': shelves,
      'readingStats': readingStats,
      'books': [
        for (final book in books)
          {
            'identity': BookIdentity.syncStrictKey(book),
            'identityBase': BookIdentity.syncBaseKey(book),
            'readingStats': readingStats[book.id] ?? const <String, int>{},
            'title': book.title,
            'author': book.author,
            'format': book.format.name,
            'chapterCount': book.chapters.length,
            'wordCount': book.wordCount,
            'currentChapterIndex': book.currentChapterIndex,
            'currentPage': book.currentPage,
            'pageCount': book.pageCount,
            'progress': book.progress,
            'lastReadAt': book.lastReadAt?.toIso8601String(),
            'shelfName': book.shelfName,
            'bookmarks':
                book.bookmarks.map((bookmark) => bookmark.toJson()).toList(),
          },
      ],
    };
  }

  // ---- Apply ----------------------------------------------------------

  /// Compute a merge result against a downloaded cloud payload. Caller is
  /// responsible for actually persisting + notifying when fields change.
  CloudSyncMergeResult mergePayload({
    required Map<String, Object?> payload,
    required List<BookEntry> localBooks,
    required List<String> localShelves,
    required Map<String, Map<String, int>> localReadingStats,
  }) {
    var shelves = localShelves;
    var shelvesChanged = false;
    final shelvesJson = payload['shelves'];
    if (shelvesJson is List) {
      shelves = shelvesJson.whereType<String>().toList(growable: false);
      shelvesChanged = true;
    }

    var books = localBooks;
    var booksChanged = false;
    var readingStats = localReadingStats;
    var readingStatsChanged = false;

    final remoteBooks = payload['books'];
    if (remoteBooks is List) {
      final remoteByIdentity = _indexRemoteBooks(remoteBooks);
      var changed = false;
      books = [
        for (final book in books)
          _mergeBookIfPresent(
            book,
            remoteByIdentity,
            () => changed = true,
          ),
      ];
      // Reading stats per book — merge max(local, remote) per day.
      for (final book in books) {
        final remote = _remoteFor(book, remoteByIdentity);
        final remoteStats = remote?['readingStats'];
        if (remoteStats is Map) {
          final merged = _mergeReadingStats(
            readingStats[book.id] ?? const <String, int>{},
            _decodeBookStats(remoteStats),
          );
          if (!_sameStats(readingStats[book.id], merged)) {
            readingStats = {...readingStats, book.id: merged};
            readingStatsChanged = true;
          }
        }
      }
      booksChanged = changed;
    }

    // If the per-book pass produced no stat changes, fall back to the
    // top-level readingStats blob.
    if (!readingStatsChanged && payload['readingStats'] is Map) {
      final remoteStats =
          _decodeReadingStats(payload['readingStats'] as Map);
      if (remoteStats.isNotEmpty) {
        readingStats = remoteStats;
        readingStatsChanged = true;
      }
    }

    return CloudSyncMergeResult(
      shelves: shelves,
      shelvesChanged: shelvesChanged,
      books: books,
      booksChanged: booksChanged,
      readingStats: readingStats,
      readingStatsChanged: readingStatsChanged,
    );
  }

  // ---- Internals ------------------------------------------------------

  Map<String, Map<String, int>> _decodeReadingStats(Map raw) {
    final result = <String, Map<String, int>>{};
    for (final entry in raw.entries) {
      final bookId = entry.key;
      final value = entry.value;
      if (bookId is! String || value is! Map) {
        continue;
      }
      result[bookId] = {
        for (final stat in value.entries)
          if (stat.key is String && stat.value is num)
            stat.key as String: (stat.value as num).round(),
      };
    }
    return result;
  }

  Map<String, int> _decodeBookStats(Map raw) {
    return {
      for (final entry in raw.entries)
        if (entry.key is String && entry.value is num)
          entry.key as String: (entry.value as num).round(),
    };
  }

  Map<String, int> _mergeReadingStats(
    Map<String, int> local,
    Map<String, int> remote,
  ) {
    return {
      ...local,
      for (final entry in remote.entries)
        entry.key: math.max(local[entry.key] ?? 0, entry.value),
    };
  }

  bool _sameStats(Map<String, int>? a, Map<String, int> b) {
    if (a == null || a.length != b.length) {
      return false;
    }
    for (final entry in b.entries) {
      if (a[entry.key] != entry.value) {
        return false;
      }
    }
    return true;
  }

  BookEntry _mergeBookIfPresent(
    BookEntry book,
    Map<String, Map<String, Object?>> remoteByIdentity,
    VoidCallback onChanged,
  ) {
    final remote = _remoteFor(book, remoteByIdentity);
    if (remote == null) {
      return book;
    }
    return _mergeBook(book, remote, onChanged: onChanged);
  }

  BookEntry _mergeBook(
    BookEntry book,
    Map<String, Object?> remote, {
    required VoidCallback onChanged,
  }) {
    final remoteLastRead = DateTime.tryParse(
      remote['lastReadAt'] as String? ?? '',
    );
    final remoteProgress = (remote['progress'] as num?)?.toDouble();
    final remoteIsAhead =
        remoteProgress != null && remoteProgress > book.progress + .001;
    final remoteIsNewer = remoteLastRead != null &&
        (book.lastReadAt == null || remoteLastRead.isAfter(book.lastReadAt!));
    final shouldUseRemotePosition = remoteIsAhead || remoteIsNewer;
    final bookmarksJson = remote['bookmarks'];
    final remoteBookmarks = bookmarksJson is List
        ? bookmarksJson
            .whereType<Map>()
            .map((item) => BookBookmark.fromJson(item.cast<String, Object?>()))
            .where((bookmark) => bookmark.id.isNotEmpty)
            .toList(growable: false)
        : const <BookBookmark>[];
    final bookmarksById = {
      for (final bookmark in book.bookmarks) bookmark.id: bookmark,
      for (final bookmark in remoteBookmarks) bookmark.id: bookmark,
    };
    final mergedBookmarks = bookmarksById.values.toList(growable: false)
      ..sort((a, b) => b.createdAt.compareTo(a.createdAt));
    final shelfName = remote['shelfName'] as String?;
    final bookmarkChanged = mergedBookmarks.length != book.bookmarks.length;
    final shouldUpdateShelf = shelfName != book.shelfName;
    if (!shouldUseRemotePosition && !bookmarkChanged && !shouldUpdateShelf) {
      return book;
    }
    final remoteChapter = remote['currentChapterIndex'] as int?;
    final maxChapterIndex =
        book.chapters.isEmpty ? 0 : book.chapters.length - 1;
    final safeRemoteChapter = remoteChapter == null
        ? book.currentChapterIndex
        : remoteChapter.clamp(0, maxChapterIndex).toInt();
    onChanged();
    return book.copyWith(
      currentChapterIndex: shouldUseRemotePosition
          ? safeRemoteChapter
          : book.currentChapterIndex,
      currentPage: shouldUseRemotePosition
          ? (remote['currentPage'] as int?) ?? book.currentPage
          : book.currentPage,
      pageCount: shouldUseRemotePosition
          ? (remote['pageCount'] as int?) ?? book.pageCount
          : book.pageCount,
      progress:
          shouldUseRemotePosition ? remoteProgress ?? book.progress : book.progress,
      lastReadAt: shouldUseRemotePosition && remoteLastRead != null
          ? remoteLastRead
          : book.lastReadAt,
      shelfName: shelfName,
      clearShelf: shelfName == null,
      bookmarks: mergedBookmarks,
    );
  }

  Map<String, Map<String, Object?>> _indexRemoteBooks(List remoteBooks) {
    final remoteByKey = <String, Map<String, Object?>>{};
    final ambiguousKeys = <String>{};

    void addKey(String key, Map<String, Object?> remote) {
      if (key.isEmpty || ambiguousKeys.contains(key)) {
        return;
      }
      final existing = remoteByKey[key];
      if (existing == null || identical(existing, remote)) {
        remoteByKey[key] = remote;
        return;
      }
      remoteByKey.remove(key);
      ambiguousKeys.add(key);
    }

    for (final item in remoteBooks.whereType<Map>()) {
      final json = item.cast<String, Object?>();
      for (final key in _matchKeysForRemote(json)) {
        addKey(key, json);
      }
    }
    return remoteByKey;
  }

  Map<String, Object?>? _remoteFor(
    BookEntry book,
    Map<String, Map<String, Object?>> remoteByIdentity,
  ) {
    for (final key in _matchKeysForBook(book)) {
      final remote = remoteByIdentity[key];
      if (remote != null) {
        return remote;
      }
    }
    return null;
  }

  Iterable<String> _matchKeysForBook(BookEntry book) {
    return <String>{
      'strict:${BookIdentity.syncStrictKey(book)}',
      'base:${BookIdentity.syncBaseKey(book)}',
      'title:${BookIdentity.syncTitleKey(book)}',
    };
  }

  Iterable<String> _matchKeysForRemote(Map<String, Object?> remote) {
    final keys = <String>{};
    final strictIdentity = remote['identity'] as String? ?? '';
    if (strictIdentity.isNotEmpty) {
      keys.add('strict:$strictIdentity');
    }
    final baseIdentity = remote['identityBase'] as String? ?? '';
    if (baseIdentity.isNotEmpty) {
      keys.add('base:$baseIdentity');
    }
    final format = (remote['format'] as String? ?? '').trim().toLowerCase();
    final title = BookIdentity.normalize(remote['title'] as String? ?? '');
    final author = BookIdentity.normalize(remote['author'] as String? ?? '');
    final chapterCount = (remote['chapterCount'] as num?)?.round();
    final wordCount = (remote['wordCount'] as num?)?.round();
    if (format.isEmpty || title.isEmpty) {
      return keys;
    }
    if (chapterCount != null) {
      keys.add(
        'base:${BookIdentity.baseFromParts(format, title, author, chapterCount)}',
      );
      if (wordCount != null) {
        keys.add(
          'strict:${BookIdentity.strictFromParts(format, title, author, chapterCount, wordCount)}',
        );
      }
    }
    keys.add('title:${BookIdentity.titleFromParts(format, title, author)}');
    return keys;
  }
}
