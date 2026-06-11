import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:path/path.dart' as path;

// ---------------------------------------------------------------------------
// EpubWebViewFallbackException
// ---------------------------------------------------------------------------

class EpubWebViewFallbackException implements Exception {
  const EpubWebViewFallbackException();
}

// ---------------------------------------------------------------------------
// FullscreenImageViewer
// ---------------------------------------------------------------------------

class FullscreenImageViewer extends StatelessWidget {
  const FullscreenImageViewer({super.key, required this.source});

  final String source;

  @override
  Widget build(BuildContext context) {
    final image = readerImageForSource(source);
    return Scaffold(
      backgroundColor: Colors.black,
      body: SafeArea(
        child: Stack(
          children: [
            Positioned.fill(
              child: GestureDetector(
                behavior: HitTestBehavior.opaque,
                onLongPress: () => showReaderImageActions(context, source),
                child: InteractiveViewer(
                  minScale: .8,
                  maxScale: 6,
                  child: Center(child: image),
                ),
              ),
            ),
            Positioned(
              top: 8,
              right: 8,
              child: IconButton.filledTonal(
                onPressed: () => Navigator.pop(context),
                icon: const Icon(Icons.close_rounded),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Image action helpers
// ---------------------------------------------------------------------------

Future<void> showReaderImageActions(BuildContext context, String source) async {
  HapticFeedback.selectionClick();
  await showModalBottomSheet<void>(
    context: context,
    backgroundColor: Colors.transparent,
    builder: (sheetContext) {
      final colors = Theme.of(sheetContext).colorScheme;
      return SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
          child: DecoratedBox(
            decoration: BoxDecoration(
              color: colors.surface,
              borderRadius: BorderRadius.circular(28),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: .28),
                  blurRadius: 24,
                  offset: const Offset(0, 10),
                ),
              ],
            ),
            child: Padding(
              padding: const EdgeInsets.fromLTRB(14, 10, 14, 14),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                    width: 44,
                    height: 5,
                    decoration: BoxDecoration(
                      color: colors.outlineVariant,
                      borderRadius: BorderRadius.circular(999),
                    ),
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton.icon(
                      style: FilledButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 14),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(18),
                        ),
                      ),
                      onPressed: () {
                        Navigator.pop(sheetContext);
                        saveReaderImage(context, source);
                      },
                      icon: const Icon(Icons.save_alt_rounded),
                      label: const Text('\u4fdd\u5b58\u56fe\u7247'),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      );
    },
  );
}

Widget readerImageForSource(String source) {
  final uri = Uri.tryParse(source);
  if (uri?.scheme == 'file') {
    return Image.file(File(uri!.toFilePath()), fit: BoxFit.contain);
  }
  if (uri?.scheme == 'data') {
    final comma = source.indexOf(',');
    if (comma > 0 && source.substring(0, comma).contains(';base64')) {
      try {
        return Image.memory(
          base64Decode(source.substring(comma + 1)),
          fit: BoxFit.contain,
        );
      } catch (_) {
        return const Icon(Icons.broken_image_rounded, color: Colors.white70);
      }
    }
  }
  return Image.network(source, fit: BoxFit.contain);
}

Future<void> saveReaderImage(BuildContext context, String source) async {
  HapticFeedback.mediumImpact();
  try {
    final bytes = await readerImageBytes(source);
    if (bytes == null || bytes.isEmpty) {
      throw const FileSystemException('Image data is unavailable');
    }
    final extension = readerImageExtension(source);
    const galleryChannel = MethodChannel('squartor/native_picker');
    await galleryChannel.invokeMethod<String>('saveImageToGallery', {
      'bytes': bytes,
      'fileName':
          'squartor_${DateTime.now().millisecondsSinceEpoch}.$extension',
      'mimeType': readerImageMimeType(extension),
    });
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('\u56fe\u7247\u5df2\u4fdd\u5b58\u5230\u76f8\u518c'),
        ),
      );
    }
  } catch (_) {
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('\u56fe\u7247\u4fdd\u5b58\u5931\u8d25')),
      );
    }
  }
}

Future<Uint8List?> readerImageBytes(String source) async {
  final uri = Uri.tryParse(source);
  if (uri?.scheme == 'file') {
    return File(uri!.toFilePath()).readAsBytes();
  }
  if (uri?.scheme == 'data') {
    final comma = source.indexOf(',');
    if (comma > 0 && source.substring(0, comma).contains(';base64')) {
      return base64Decode(source.substring(comma + 1));
    }
    return null;
  }
  if (uri != null && (uri.scheme == 'http' || uri.scheme == 'https')) {
    final client = HttpClient();
    try {
      final request = await client.getUrl(uri);
      final response = await request.close();
      if (response.statusCode < 200 || response.statusCode >= 300) {
        return null;
      }
      final bytes = BytesBuilder(copy: false);
      await for (final chunk in response) {
        bytes.add(chunk);
      }
      return bytes.takeBytes();
    } finally {
      client.close(force: true);
    }
  }
  return null;
}

String readerImageExtension(String source) {
  final dataType = RegExp(
    r'^data:image/([^;,]+)',
    caseSensitive: false,
  ).firstMatch(source)?.group(1);
  if (dataType != null) {
    return dataType.toLowerCase() == 'jpeg' ? 'jpg' : dataType.toLowerCase();
  }
  final uri = Uri.tryParse(source);
  final extension = path.extension(uri?.path ?? '').replaceFirst('.', '');
  if (extension.isNotEmpty && extension.length <= 5) {
    return extension.toLowerCase();
  }
  return 'jpg';
}

String readerImageMimeType(String extension) {
  return switch (extension.toLowerCase()) {
    'png' => 'image/png',
    'gif' => 'image/gif',
    'webp' => 'image/webp',
    'svg' => 'image/svg+xml',
    _ => 'image/jpeg',
  };
}
