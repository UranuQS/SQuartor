import 'dart:io';

import 'package:flutter/material.dart';

import '../models.dart';
import '../typography.dart';

class BookCover extends StatelessWidget {
  const BookCover({
    super.key,
    required this.book,
    required this.palette,
    required this.width,
    required this.height,
    this.radius = 14,
    this.hero = false,
  });

  final BookEntry book;
  final AppPalette palette;
  final double width;
  final double height;
  final double radius;
  final bool hero;

  @override
  Widget build(BuildContext context) {
    return _CoverImage(
      book: book,
      palette: palette,
      width: width,
      height: height,
      radius: radius,
    );
  }
}

class _CoverImage extends StatelessWidget {
  const _CoverImage({
    required this.book,
    required this.palette,
    required this.width,
    required this.height,
    required this.radius,
  });

  final BookEntry book;
  final AppPalette palette;
  final double width;
  final double height;
  final double radius;

  @override
  Widget build(BuildContext context) {
    final coverPath = book.coverPath;
    final devicePixelRatio = MediaQuery.devicePixelRatioOf(context);
    final cacheWidth = (width * devicePixelRatio).round();
    final cacheHeight = (height * devicePixelRatio).round();
    final placeholder = _CoverPlaceholder(
      book: book,
      palette: palette,
      radius: radius,
    );
    return RepaintBoundary(
      child: SizedBox(
        width: width,
        height: height,
        child: DecoratedBox(
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(radius),
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [palette.primarySoft, palette.primary, palette.cardAlt],
            ),
          ),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(radius),
            child: coverPath == null
                ? placeholder
                : Image.file(
                    File(coverPath),
                    fit: BoxFit.cover,
                    gaplessPlayback: true,
                    cacheWidth: cacheWidth,
                    cacheHeight: cacheHeight,
                    filterQuality: FilterQuality.medium,
                    errorBuilder: (_, _, _) => placeholder,
                  ),
          ),
        ),
      ),
    );
  }
}

class _CoverPlaceholder extends StatelessWidget {
  const _CoverPlaceholder({
    required this.book,
    required this.palette,
    required this.radius,
  });

  final BookEntry book;
  final AppPalette palette;
  final double radius;

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        Positioned.fill(
          child: ColoredBox(color: palette.primary.withValues(alpha: .2)),
        ),
        Positioned(
          left: 11,
          top: 12,
          right: 11,
          child: Text(
            book.title,
            maxLines: 5,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 13,
              fontWeight: AppTextWeight.regular,
              height: 1.25,
            ),
          ),
        ),
        Positioned(
          right: 9,
          bottom: 8,
          child: Text(
            book.formatLabel,
            style: TextStyle(
              color: Colors.white.withValues(alpha: .72),
              fontSize: 20,
              fontWeight: AppTextWeight.regular,
            ),
          ),
        ),
      ],
    );
  }
}
