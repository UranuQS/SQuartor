import 'reader_state_fields.dart';

mixin ReaderTimeMixin<T extends ReaderScreenWidget> on ReaderStateFields<T> {
  @override
  void flushReadingTime() {
    final rawSeconds = readingStopwatch.elapsed.inSeconds;
    if (rawSeconds < 5) {
      return;
    }
    final isRunning = readingStopwatch.isRunning;
    readingStopwatch.reset();
    if (isRunning) {
      readingStopwatch.start();
    }
    // Cap any single batch to 180s to prevent runaway accumulation during sleep/suspension
    final seconds = rawSeconds.clamp(5, 180);
    appState.addReadingSeconds(seconds, book.id);
  }
}
