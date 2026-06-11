import 'reader_state_fields.dart';

mixin ReaderTimeMixin<T extends ReaderScreenWidget> on ReaderStateFields<T> {
  @override
  void flushReadingTime() {
    if (!readingStopwatch.isRunning) {
      return;
    }
    final seconds = readingStopwatch.elapsed.inSeconds;
    if (seconds >= 5) {
      appState.addReadingSeconds(seconds, book.id);
      readingStopwatch
        ..reset()
        ..start();
    }
  }
}
