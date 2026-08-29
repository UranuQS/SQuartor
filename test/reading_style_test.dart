import 'dart:ui';

import 'package:flutter_test/flutter_test.dart';
import 'package:squartor/src/models.dart';

void main() {
  group('ReadingStyle font weight', () {
    test('defaults to 400 (FontWeight.w400)', () {
      const style = ReadingStyle();
      expect(style.fontWeightValue, 400);
      expect(style.fontWeight, FontWeight.w400);
      expect(style.fontWeightLabel, '常规 (400)');
    });

    test('updates fontWeightValue via copyWith', () {
      const style = ReadingStyle();
      final light = style.copyWith(fontWeightValue: 300);
      expect(light.fontWeightValue, 300);
      expect(light.fontWeight, FontWeight.w300);
      expect(light.fontWeightLabel, '细体 (300)');

      final bold = style.copyWith(fontWeightValue: 700);
      expect(bold.fontWeightValue, 700);
      expect(bold.fontWeight, FontWeight.w700);
      expect(bold.fontWeightLabel, '粗体 (700)');
    });

    test('serializes to and deserializes from JSON correctly', () {
      const original = ReadingStyle(fontWeightValue: 600);
      final json = original.toJson();
      expect(json['fontWeightValue'], 600);

      final restored = ReadingStyle.fromJson(json);
      expect(restored.fontWeightValue, 600);
      expect(restored.fontWeight, FontWeight.w600);
      expect(restored.fontWeightLabel, '半粗 (600)');
    });

    test('clamps invalid font weight in fromJson to 100..900', () {
      final restored = ReadingStyle.fromJson({'fontWeightValue': 1500});
      expect(restored.fontWeightValue, 900);
      expect(restored.fontWeight, FontWeight.w900);

      final restoredLow = ReadingStyle.fromJson({'fontWeightValue': 20});
      expect(restoredLow.fontWeightValue, 100);
      expect(restoredLow.fontWeight, FontWeight.w100);
    });
  });
}
