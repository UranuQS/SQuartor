// This is a basic Flutter widget test.
//
// To perform an interaction with a widget in your test, use the WidgetTester
// utility in the flutter_test package. For example, you can send tap and scroll
// gestures. You can also use WidgetTester to find child widgets in the widget
// tree, read text, and verify that the values of widget properties are correct.

import 'package:flutter_test/flutter_test.dart';

import 'package:squartor/src/app.dart';
import 'package:squartor/src/app_state.dart';
import 'package:squartor/src/book_repository.dart';

void main() {
  testWidgets('SQuartor app boots', (WidgetTester tester) async {
    final state = AppState(BookRepository());
    await tester.pumpWidget(SQuartorApp(state: state));

    expect(find.text('书架'), findsAtLeastNWidgets(1));
  });
}
