import 'dart:async';

import 'package:flutter/material.dart';

import '../../core/notifications/push_notification_service.dart';
import '../../data/api/notifications_api.dart';
import '../../data/dto/notification_dto.dart';

class NotificationWatcher extends StatefulWidget {
  const NotificationWatcher({
    super.key,
    required this.child,
    required this.navigatorKey,
  });

  final Widget child;
  final GlobalKey<NavigatorState> navigatorKey;

  @override
  State<NotificationWatcher> createState() => _NotificationWatcherState();
}

class _NotificationWatcherState extends State<NotificationWatcher> {
  Timer? _timer;
  int? _lastNotificationId;

  @override
  void initState() {
    super.initState();
    PushNotificationService.instance.registerNavigator(widget.navigatorKey);
    _poll();
    _timer = Timer.periodic(const Duration(seconds: 30), (_) => _poll());
  }

  Future<void> _poll() async {
    try {
      final page = await fetchNotifications(size: 1, unreadOnly: true);
      if (page.content.isEmpty) return;
      final latest = page.content.first;
      if (latest.id == _lastNotificationId) return;
      _lastNotificationId = latest.id;
      await _showDeviceNotification(latest);
    } catch (_) {}
  }

  Future<void> _showDeviceNotification(NotificationDto dto) async {
    final code = (dto.errorCode?.trim().isNotEmpty ?? false) ? dto.errorCode!.trim() : '알림';
    final title = '[$code] 오류 알림';
    final parts = <String>[];
    if (dto.groupName != null && dto.groupName!.trim().isNotEmpty) {
      parts.add(dto.groupName!.trim());
    }
    if (dto.chuteName != null && dto.chuteName!.trim().isNotEmpty) {
      parts.add(dto.chuteName!.trim());
    }
    final location = parts.isEmpty ? '장비' : parts.join(' • ');
    final body = '$location에서 새로운 알림이 발생했습니다.';
    try {
      await PushNotificationService.instance.showNotification(
        id: dto.id,
        title: title,
        body: body,
      );
    } catch (_) {}
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => widget.child;
}
