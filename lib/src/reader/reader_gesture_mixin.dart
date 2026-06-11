import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';

import 'reader_state_fields.dart';

mixin ReaderGestureMixin<T extends ReaderScreenWidget> on ReaderStateFields<T> {
  @override
  void onReaderDragStart(DragStartDetails details) {
    if (usesVerticalScroll) {
      return;
    }
    dragDx = 0;
    dragDy = 0;
    pageDragActive = false;
    dragMoveScheduled = false;
    dragSession++;
    final session = dragSession;
    controller?.evaluateJavascript(
      source: 'window.SQuartor && window.SQuartor.dragStart($session);',
    );
  }

  @override
  void onReaderDragUpdate(DragUpdateDetails details) {
    if (usesVerticalScroll) {
      return;
    }
    dragDx += details.delta.dx;
    dragDy += details.delta.dy;
    if (!pageDragActive && dragDx.abs() > 8 && dragDx.abs() > dragDy.abs()) {
      pageDragActive = true;
    }
    if (pageDragActive) {
      scheduleReaderDragMove();
    }
  }

  @override
  void scheduleReaderDragMove() {
    if (dragMoveScheduled) {
      return;
    }
    dragMoveScheduled = true;
    final session = dragSession;
    SchedulerBinding.instance.scheduleFrameCallback((_) {
      dragMoveScheduled = false;
      final ctrl = controller;
      if (!pageDragActive || ctrl == null || session != dragSession) {
        return;
      }
      final dx = dragDx.toStringAsFixed(2);
      unawaited(
        ctrl.evaluateJavascript(
          source: 'window.SQuartor && window.SQuartor.dragMove($dx, $session);',
        ),
      );
    });
    SchedulerBinding.instance.ensureVisualUpdate();
  }

  @override
  Future<void> onReaderDragEnd(DragEndDetails details) async {
    if (usesVerticalScroll) {
      return;
    }
    if (!pageDragActive) {
      onReaderDragCancel();
      return;
    }
    final velocity = details.primaryVelocity ?? 0;
    final dx = dragDx.toStringAsFixed(2);
    final v = velocity.toStringAsFixed(2);
    final session = dragSession;
    pageDragActive = false;
    dragMoveScheduled = false;
    dragDx = 0;
    dragDy = 0;
    dragSession++;
    final result = await controller?.evaluateJavascript(
      source: 'window.SQuartor && window.SQuartor.dragEnd($dx, $v, $session);',
    );
    if (result == 'end') {
      await goToChapter(chapterIndex + 1);
    } else if (result == 'start') {
      await goToChapter(chapterIndex - 1, atEnd: true);
    }
  }

  @override
  void onReaderDragCancel() {
    if (usesVerticalScroll) {
      return;
    }
    final session = dragSession;
    pageDragActive = false;
    dragMoveScheduled = false;
    dragDx = 0;
    dragDy = 0;
    dragSession++;
    controller?.evaluateJavascript(
      source: 'window.SQuartor && window.SQuartor.dragCancel($session);',
    );
  }
}
