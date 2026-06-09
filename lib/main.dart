import 'package:flutter/material.dart';

import 'src/app.dart';
import 'src/app_state.dart';
import 'src/book_repository.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final state = AppState(BookRepository());
  runApp(SQuartorApp(state: state));
}
