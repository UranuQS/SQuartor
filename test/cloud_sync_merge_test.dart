import 'package:flutter_test/flutter_test.dart';
import 'package:squartor/src/app_state.dart';
import 'package:squartor/src/book_repository.dart';
import 'package:squartor/src/models.dart';

void main() {
  test(
    'cloud sync keeps remote ahead progress for the same imported book',
    () async {
      final localBook = _book(
        id: 'local',
        wordCount: 1000,
        currentChapterIndex: 0,
        progress: .01,
        lastReadAt: DateTime.parse('2026-06-12T21:00:00'),
      );
      final repository = _MemoryBookRepository(books: [localBook]);
      final state = AppState(repository);
      await state.ready;

      await state.applyCloudSyncPayloadForTest({
        'schema': 1,
        'updatedAt': '2026-06-12T20:30:00',
        'books': [
          {
            // Simulates the first shipped payload: strict identity is present,
            // but word count differs and identityBase was not uploaded yet.
            'identity': 'txt|测试书|本地txt|10|2000',
            'title': '测试书',
            'author': '本地 TXT',
            'format': 'txt',
            'chapterCount': 10,
            'wordCount': 2000,
            'currentChapterIndex': 5,
            'currentPage': 2,
            'pageCount': 8,
            'progress': .52,
            'lastReadAt': '2026-06-12T20:00:00',
            'bookmarks': <Object?>[],
          },
        ],
      });

      final synced = state.books.single;
      expect(synced.currentChapterIndex, 5);
      expect(synced.currentPage, 2);
      expect(synced.pageCount, 8);
      expect(synced.progress, moreOrLessEquals(.52));
      expect(repository.savedBooks.single.progress, moreOrLessEquals(.52));
    },
  );

  test('cloud sync ignores style in payload', () async {
    final repository = _MemoryBookRepository(books: []);
    final state = AppState(repository);
    await state.ready;
    final initialStyle = state.style;

    await state.applyCloudSyncPayloadForTest({
      'schema': 1,
      'updatedAt': '2026-06-12T20:30:00',
      'style': {'fontSize': 30.0},
    });

    expect(state.style.fontSize, initialStyle.fontSize);
  });
}

BookEntry _book({
  required String id,
  required int wordCount,
  required int currentChapterIndex,
  required double progress,
  required DateTime lastReadAt,
}) {
  return BookEntry(
    id: id,
    title: '测试书',
    author: '本地 TXT',
    format: BookFormat.txt,
    bookDir: '',
    sourcePath: '',
    importedAt: DateTime.parse('2026-06-12T18:00:00'),
    chapters: [
      for (var i = 0; i < 10; i++)
        ReaderChapter(title: '第 $i 章', href: 'chapter-$i', filePath: ''),
    ],
    currentChapterIndex: currentChapterIndex,
    currentPage: 0,
    pageCount: 1,
    progress: progress,
    lastReadAt: lastReadAt,
    wordCount: wordCount,
  );
}

class _MemoryBookRepository extends BookRepository {
  _MemoryBookRepository({required List<BookEntry> books})
    : savedBooks = List.of(books);

  List<BookEntry> savedBooks;
  ReadingStyle savedStyle = const ReadingStyle();
  List<String> savedShelves = const [];
  Map<String, Map<String, int>> savedReadingStats = const {};
  CloudSyncSettings savedCloudSyncSettings = const CloudSyncSettings();

  @override
  Future<BookRepositorySnapshot> loadSnapshot() async {
    return BookRepositorySnapshot(
      books: savedBooks,
      fonts: const [],
      shelves: savedShelves,
      readingStats: savedReadingStats,
      style: savedStyle,
      cloudSyncSettings: savedCloudSyncSettings,
    );
  }

  @override
  Future<void> saveBooks(List<BookEntry> books) async {
    savedBooks = List.of(books);
  }

  @override
  Future<void> saveStyle(ReadingStyle style) async {
    savedStyle = style;
  }

  @override
  Future<void> saveShelves(List<String> shelves) async {
    savedShelves = List.of(shelves);
  }

  @override
  Future<void> saveReadingStats(Map<String, Map<String, int>> stats) async {
    savedReadingStats = {
      for (final entry in stats.entries) entry.key: Map.of(entry.value),
    };
  }

  @override
  Future<void> saveCloudSyncSettings(CloudSyncSettings settings) async {
    savedCloudSyncSettings = settings;
  }
}
