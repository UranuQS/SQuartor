import 'package:flutter/material.dart';

import '../app_state.dart';
import '../models.dart';
import '../screens/reader_screen.dart';
import '../typography.dart';
import 'home_shell.dart';

const _appUiTextScale = 0.94;

class SQuartorApp extends StatefulWidget {
  const SQuartorApp({super.key, required this.state});

  final AppState state;

  @override
  State<SQuartorApp> createState() => _SQuartorAppState();
}

class _SQuartorAppState extends State<SQuartorApp> with WidgetsBindingObserver {
  final _navigatorKey = GlobalKey<NavigatorState>();
  bool _handlingExternalOpen = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _syncBrightness();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _handleExternalOpenBook();
    });
  }

  @override
  void didChangePlatformBrightness() {
    _syncBrightness();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _handleExternalOpenBook();
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  void _syncBrightness() {
    widget.state.setPlatformBrightness(
      WidgetsBinding.instance.platformDispatcher.platformBrightness,
    );
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: widget.state.appChanges,
      builder: (context, _) {
        final palette = widget.state.palette;
        final brightness = widget.state.effectiveBrightness;
        final appFontFamily = widget.state.appFontFamily ?? 'sans';
        return MaterialApp(
          navigatorKey: _navigatorKey,
          title: 'SQuartor',
          debugShowCheckedModeBanner: false,
          theme: ThemeData(
            useMaterial3: true,
            brightness: brightness,
            scaffoldBackgroundColor: palette.background,
            colorScheme: ColorScheme.fromSeed(
              seedColor: palette.primary,
              brightness: brightness,
              primary: palette.primary,
              secondary: palette.primarySoft,
              surface: palette.surface,
              onPrimary: Colors.white,
              onSurface: palette.text,
            ),
            fontFamily: appFontFamily,
            fontFamilyFallback: const ['sans'],
            splashFactory: NoSplash.splashFactory,
            splashColor: Colors.transparent,
            highlightColor: Colors.transparent,
            hoverColor: Colors.transparent,
            focusColor: Colors.transparent,
            textTheme: ThemeData(brightness: brightness).textTheme.apply(
              fontFamily: appFontFamily,
              bodyColor: palette.text,
              displayColor: palette.text,
            ),
            navigationBarTheme: NavigationBarThemeData(
              labelTextStyle: WidgetStateProperty.resolveWith((states) {
                final selected = states.contains(WidgetState.selected);
                return TextStyle(
                  color: selected ? palette.text : palette.muted,
                  fontSize: 13,
                  fontWeight: selected
                      ? AppTextWeight.medium
                      : AppTextWeight.regular,
                );
              }),
              iconTheme: WidgetStateProperty.resolveWith((states) {
                final selected = states.contains(WidgetState.selected);
                return IconThemeData(
                  color: selected ? palette.text : palette.muted,
                  size: selected ? 25 : 24,
                );
              }),
            ),
            pageTransitionsTheme: const PageTransitionsTheme(
              builders: {
                TargetPlatform.android: FadeUpwardsPageTransitionsBuilder(),
              },
            ),
          ),
          builder: (context, child) {
            final mediaQuery = MediaQuery.of(context);
            return MediaQuery(
              data: mediaQuery.copyWith(
                textScaler: TextScaler.linear(_appUiTextScale),
              ),
              child: child ?? const SizedBox.shrink(),
            );
          },
          home: HomeShell(state: widget.state),
          onGenerateRoute: (settings) {
            if (settings.name != ReaderScreen.routeName) {
              return null;
            }
            final book = settings.arguments! as BookEntry;
            return PageRouteBuilder<void>(
              settings: settings,
              transitionDuration: const Duration(milliseconds: 220),
              reverseTransitionDuration: const Duration(milliseconds: 180),
              pageBuilder: (_, animation, secondaryAnimation) =>
                  ReaderScreen(state: widget.state, book: book),
              transitionsBuilder:
                  (context, animation, secondaryAnimation, child) {
                    final curved = CurvedAnimation(
                      parent: animation,
                      curve: Curves.easeOutCubic,
                      reverseCurve: Curves.easeInCubic,
                    );
                    return FadeTransition(
                      opacity: curved,
                      child: SlideTransition(
                        position: Tween<Offset>(
                          begin: const Offset(0.025, 0),
                          end: Offset.zero,
                        ).animate(curved),
                        child: child,
                      ),
                    );
                  },
            );
          },
        );
      },
    );
  }

  Future<void> _handleExternalOpenBook() async {
    if (_handlingExternalOpen) {
      return;
    }
    _handlingExternalOpen = true;
    try {
      final book = await widget.state.consumeAndOpenExternalBook();
      if (!mounted || book == null) {
        return;
      }
      final navigator = _navigatorKey.currentState;
      if (navigator == null) {
        return;
      }
      navigator.pushNamed(ReaderScreen.routeName, arguments: book);
    } finally {
      _handlingExternalOpen = false;
    }
  }
}
