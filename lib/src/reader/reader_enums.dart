enum ReaderOverlay { hidden, chrome, toc, settings }

enum ScrollEdgeTurnDirection { previous, next }

class ScrollEdgeTurnState {
  const ScrollEdgeTurnState({required this.direction, required this.progress});

  const ScrollEdgeTurnState.hidden() : direction = null, progress = 0;

  final ScrollEdgeTurnDirection? direction;
  final double progress;
}

class ScrollPageEstimate {
  const ScrollPageEstimate({required this.page, required this.pageCount});

  final int page;
  final int pageCount;
}

String decodeLooseUriComponent(String value) {
  try {
    return Uri.decodeComponent(value);
  } on FormatException {
    return Uri.decodeComponent(
      value.replaceAllMapped(RegExp(r'%(?![0-9A-Fa-f]{2})'), (_) => '%25'),
    );
  }
}
