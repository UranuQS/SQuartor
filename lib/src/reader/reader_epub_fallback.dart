import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:path/path.dart' as path;

import '../typography.dart';

// ---------------------------------------------------------------------------
// EpubWebViewFallbackException
// ---------------------------------------------------------------------------

class EpubWebViewFallbackException implements Exception {
  const EpubWebViewFallbackException();
}

// ---------------------------------------------------------------------------
// FullscreenImageViewer
// ---------------------------------------------------------------------------

class FullscreenImageViewer extends StatefulWidget {
  const FullscreenImageViewer({super.key, required this.source});

  final String source;

  @override
  State<FullscreenImageViewer> createState() => _FullscreenImageViewerState();
}

class _FullscreenImageViewerState extends State<FullscreenImageViewer> {
  late final Widget _image;

  @override
  void initState() {
    super.initState();
    _image = readerImageForSource(widget.source);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: SafeArea(
        child: Stack(
          children: [
            Positioned.fill(
              child: GestureDetector(
                behavior: HitTestBehavior.opaque,
                onLongPress: () =>
                    showReaderImageActions(context, widget.source),
                child: InteractiveViewer(
                  minScale: .8,
                  maxScale: 6,
                  child: Center(child: _image),
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

PageRouteBuilder<void> readerImageViewerRoute(String source) {
  return PageRouteBuilder<void>(
    opaque: true,
    transitionDuration: const Duration(milliseconds: 280),
    reverseTransitionDuration: const Duration(milliseconds: 180),
    pageBuilder: (context, animation, secondaryAnimation) =>
        FullscreenImageViewer(source: source),
    transitionsBuilder: (context, animation, secondaryAnimation, child) {
      final curved = CurvedAnimation(
        parent: animation,
        curve: Curves.easeOutBack,
        reverseCurve: Curves.easeInCubic,
      );
      return FadeTransition(
        opacity: animation.drive(CurveTween(curve: Curves.easeOutCubic)),
        child: ScaleTransition(
          scale: Tween<double>(begin: .92, end: 1).animate(curved),
          child: child,
        ),
      );
    },
  );
}

// ---------------------------------------------------------------------------
// Image action helpers
// ---------------------------------------------------------------------------

Future<void> showReaderImageActions(BuildContext context, String source) async {
  HapticFeedback.selectionClick();
  await showModalBottomSheet<void>(
    context: context,
    backgroundColor: Colors.transparent,
    barrierColor: Colors.black.withValues(alpha: .48),
    builder: (sheetContext) {
      final colors = Theme.of(sheetContext).colorScheme;
      return SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
          child: TweenAnimationBuilder<double>(
            tween: Tween(begin: 0, end: 1),
            duration: const Duration(milliseconds: 260),
            curve: Curves.easeOutBack,
            builder: (context, value, child) {
              final opacity = value.clamp(0.0, 1.0).toDouble();
              final scale = .92 + opacity * .08;
              final dy = 28 * (1 - opacity);
              return Opacity(
                opacity: opacity,
                child: Transform.translate(
                  offset: Offset(0, dy),
                  child: Transform.scale(
                    scale: scale,
                    alignment: Alignment.bottomCenter,
                    child: child,
                  ),
                ),
              );
            },
            child: ClipRRect(
              borderRadius: BorderRadius.circular(30),
              child: BackdropFilter(
                filter: ui.ImageFilter.blur(sigmaX: 22, sigmaY: 22),
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: colors.surface.withValues(alpha: .62),
                    borderRadius: BorderRadius.circular(30),
                    border: Border.all(
                      color: colors.onSurface.withValues(alpha: .08),
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withValues(alpha: .22),
                        blurRadius: 28,
                        offset: const Offset(0, 14),
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
                            color: colors.onSurface.withValues(alpha: .20),
                            borderRadius: BorderRadius.circular(999),
                          ),
                        ),
                        const SizedBox(height: 12),
                        _ImageSaveGlassButton(
                          colors: colors,
                          onPressed: () {
                            Navigator.pop(sheetContext);
                            saveReaderImage(context, source);
                          },
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      );
    },
  );
}

class _ImageSaveGlassButton extends StatelessWidget {
  const _ImageSaveGlassButton({required this.colors, required this.onPressed});

  final ColorScheme colors;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(22),
      child: BackdropFilter(
        filter: ui.ImageFilter.blur(sigmaX: 18, sigmaY: 18),
        child: Material(
          color: colors.primary.withValues(alpha: .30),
          child: InkWell(
            onTap: onPressed,
            splashColor: colors.primary.withValues(alpha: .14),
            highlightColor: colors.primary.withValues(alpha: .10),
            child: Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(vertical: 15),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(22),
                border: Border.all(
                  color: colors.onPrimary.withValues(alpha: .18),
                ),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    Icons.save_alt_rounded,
                    color: colors.onPrimary.withValues(alpha: .92),
                    size: 22,
                  ),
                  const SizedBox(width: 10),
                  Text(
                    '保存图片',
                    style: TextStyle(
                      color: colors.onPrimary.withValues(alpha: .94),
                      fontSize: 17,
                      fontWeight: AppTextWeight.medium,
                      height: 1.1,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
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
