import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

/// Loads imported app-level fonts on demand and caches the resulting Flutter
/// font family per file path so a given file is only registered once.
///
/// Carved out of `AppState`: the registrar holds the only mutable state
/// (the set of registered families). Resolving `fontPath` -> `family` is a
/// pure function on the path itself, so the result is stable across runs as
/// long as the path doesn't move.
class AppFontRegistrar {
  AppFontRegistrar();

  final Set<String> _registered = <String>{};

  /// Register the font at [fontPath] with Flutter's font loader, returning
  /// the resolved family name (or null if the path is empty / unreadable).
  /// Safe to call repeatedly with the same path; subsequent calls just
  /// return the cached family.
  Future<String?> register(String? fontPath) async {
    if (fontPath == null || fontPath.isEmpty) {
      return null;
    }
    try {
      final file = File(fontPath);
      if (!await file.exists()) {
        return null;
      }
      final family = familyForPath(fontPath);
      if (_registered.add(family)) {
        final loader = FontLoader(family);
        loader.addFont(
          file.readAsBytes().then(
            (bytes) => ByteData.view(
              bytes.buffer,
              bytes.offsetInBytes,
              bytes.lengthInBytes,
            ),
          ),
        );
        await loader.load();
      }
      return family;
    } catch (error) {
      debugPrint('SQuartor app font load failed: $error');
      return null;
    }
  }

  /// Pure FNV-1a-ish hash of the path -> stable family name. Exposed so the
  /// caller can compute the family in advance without touching IO.
  static String familyForPath(String fontPath) {
    var hash = 0x811C9DC5;
    for (final codeUnit in fontPath.codeUnits) {
      hash ^= codeUnit;
      hash = (hash * 0x01000193) & 0xFFFFFFFF;
    }
    return 'SQuartorAppFont_${hash.toRadixString(16)}';
  }
}
