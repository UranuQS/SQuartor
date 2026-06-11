import 'package:flutter/material.dart';

import '../app_state.dart';
import '../models.dart';
import '../typography.dart';

class ErrorOverlay extends StatelessWidget {
  const ErrorOverlay({super.key, required this.state});

  final AppState state;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: state.messageChanges,
      builder: (context, _) {
        final message = state.error;
        if (message == null) {
          return const SizedBox.shrink();
        }
        return Positioned(
          left: 18,
          right: 18,
          bottom: 104,
          child: ErrorBanner(
            message: message,
            onClose: state.clearError,
            palette: state.palette,
          ),
        );
      },
    );
  }
}

class ErrorBanner extends StatelessWidget {
  const ErrorBanner({
    super.key,
    required this.message,
    required this.onClose,
    required this.palette,
  });

  final String message;
  final VoidCallback onClose;
  final AppPalette palette;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.transparent,
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: palette.cardAlt,
          borderRadius: BorderRadius.circular(18),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: .35),
              blurRadius: 24,
            ),
          ],
        ),
        child: Row(
          children: [
            Icon(Icons.error_outline_rounded, color: palette.primarySoft),
            const SizedBox(width: 10),
            Expanded(
              child: Text(message, style: TextStyle(color: palette.text)),
            ),
            IconButton(
              onPressed: onClose,
              icon: Icon(Icons.close_rounded, color: palette.muted),
            ),
          ],
        ),
      ),
    );
  }
}

class EmptyCard extends StatelessWidget {
  const EmptyCard({
    super.key,
    required this.palette,
    required this.title,
    required this.subtitle,
  });

  final AppPalette palette;
  final String title;
  final String subtitle;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: cardDecoration(palette),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: TextStyle(
              color: palette.text,
              fontSize: 18,
              fontWeight: AppTextWeight.regular,
            ),
          ),
          const SizedBox(height: 10),
          Text(subtitle, style: TextStyle(color: palette.muted, height: 1.6)),
        ],
      ),
    );
  }
}

BoxDecoration cardDecoration(AppPalette palette) {
  final isLight = palette.background.computeLuminance() > .5;
  return BoxDecoration(
    color: palette.card,
    borderRadius: BorderRadius.circular(24),
    boxShadow: [
      BoxShadow(
        color: isLight
            ? palette.primary.withValues(alpha: .10)
            : Colors.black.withValues(alpha: .22),
        blurRadius: isLight ? 30 : 24,
        offset: const Offset(0, 12),
      ),
    ],
  );
}
