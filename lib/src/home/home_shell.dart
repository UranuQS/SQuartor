import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';

import '../app_state.dart';
import '../screens/settings_screen.dart';
import '../screens/shelf_screen.dart';
import '../screens/stats_screen.dart';
import 'error_overlay.dart';
import 'import_overlay.dart';
import 'lazy_page_stack.dart';
import 'navigation.dart';
import 'reading_now_page.dart';

class HomeShell extends StatefulWidget {
  const HomeShell({super.key, required this.state});

  final AppState state;

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  var _index = 1;
  var _shelfSelectionMode = false;
  var _navigationHiddenByScroll = false;
  var _scrollDeltaAccumulator = 0.0;
  late final List<Widget?> _pages;

  @override
  void initState() {
    super.initState();
    _pages = List<Widget?>.filled(4, null)..[_index] = _buildShelfPage();
  }

  Widget _buildShelfPage() {
    return ShelfScreen(
      state: widget.state,
      onSelectionModeChanged: _handleShelfSelectionModeChanged,
    );
  }

  void _handleShelfSelectionModeChanged(bool selectionMode) {
    if (_shelfSelectionMode == selectionMode || !mounted) {
      return;
    }
    setState(() => _shelfSelectionMode = selectionMode);
  }

  bool _handleScrollNotification(ScrollNotification notification) {
    if (notification.metrics.axis != Axis.vertical) {
      return false;
    }
    if (notification.metrics.pixels <= notification.metrics.minScrollExtent) {
      _scrollDeltaAccumulator = 0;
      _setNavigationHiddenByScroll(false);
      return false;
    }
    if (notification is ScrollUpdateNotification) {
      final delta = notification.scrollDelta;
      if (delta == null || delta == 0) {
        return false;
      }
      if (_scrollDeltaAccumulator.sign != delta.sign) {
        _scrollDeltaAccumulator = 0;
      }
      _scrollDeltaAccumulator += delta;
      final triggerDistance = kTouchSlop / 4;
      if (_scrollDeltaAccumulator > triggerDistance) {
        _scrollDeltaAccumulator = 0;
        _setNavigationHiddenByScroll(true);
      } else if (_scrollDeltaAccumulator < -triggerDistance) {
        _scrollDeltaAccumulator = 0;
        _setNavigationHiddenByScroll(false);
      }
      return false;
    }
    if (notification is UserScrollNotification &&
        notification.direction == ScrollDirection.idle) {
      _scrollDeltaAccumulator = 0;
    }
    return false;
  }

  void _setNavigationHiddenByScroll(bool hidden) {
    if (_navigationHiddenByScroll == hidden || !mounted) {
      return;
    }
    setState(() => _navigationHiddenByScroll = hidden);
  }

  Widget _pageAt(int index) {
    return _pages[index] ??= switch (index) {
      0 => ReadingNowPage(state: widget.state),
      1 => _buildShelfPage(),
      2 => StatsScreen(state: widget.state),
      3 => SettingsScreen(state: widget.state),
      _ => const SizedBox.shrink(),
    };
  }

  @override
  Widget build(BuildContext context) {
    final palette = widget.state.palette;
    final bottomInset = MediaQuery.paddingOf(context).bottom;
    final navBottom = bottomInset < 8
        ? 14.0
        : (bottomInset * .65 + 10).clamp(24.0, 34.0).toDouble();
    final hideNavigation =
        (_index == 1 && _shelfSelectionMode) || _navigationHiddenByScroll;

    return Scaffold(
      resizeToAvoidBottomInset: false,
      backgroundColor: palette.background,
      body: SafeArea(
        bottom: false,
        child: Stack(
          children: [
            NotificationListener<ScrollNotification>(
              onNotification: _handleScrollNotification,
              child: LazyPageStack(
                index: _index,
                pages: _pages..[_index] = _pageAt(_index),
                backgroundColor: palette.background,
              ),
            ),
            ImportActivityOverlay(
              state: widget.state,
              hidden: hideNavigation,
              bottom: navBottom + M3Navigation.barHeight + 16,
            ),
            ErrorOverlay(state: widget.state),
            Positioned(
              left: 20,
              right: 20,
              bottom: navBottom,
              child: M3Navigation(
                index: _index,
                palette: palette,
                hidden: hideNavigation,
                bottomPadding: 0,
                onChanged: (index) {
                  HapticFeedback.selectionClick();
                  if (index == _index) {
                    return;
                  }
                  setState(() {
                    _index = index;
                    _navigationHiddenByScroll = false;
                  });
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}
