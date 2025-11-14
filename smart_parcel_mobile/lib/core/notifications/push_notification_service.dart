import 'package:flutter/widgets.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

class PushNotificationService {
  PushNotificationService._();

  static final PushNotificationService instance = PushNotificationService._();

  final FlutterLocalNotificationsPlugin _plugin = FlutterLocalNotificationsPlugin();
  bool _initialized = false;
  GlobalKey<NavigatorState>? _navigatorKey;
  String? _pendingRoute;

  Future<void> ensureInitialized() async {
    if (_initialized) return;
    const androidSettings = AndroidInitializationSettings('@mipmap/ic_launcher');
    const iosSettings = DarwinInitializationSettings();
    const initSettings = InitializationSettings(android: androidSettings, iOS: iosSettings);
    await _plugin.initialize(
      initSettings,
      onDidReceiveNotificationResponse: _handleNotificationResponse,
    );
    _initialized = true;
  }

  void registerNavigator(GlobalKey<NavigatorState> navigatorKey) {
    _navigatorKey = navigatorKey;
    _schedulePendingNavigation();
  }

  Future<void> showNotification({
    required int id,
    required String title,
    required String body,
    String payload = '/notifications',
  }) async {
    await ensureInitialized();
    await _requestPermissions();

    const androidDetails = AndroidNotificationDetails(
      'smartparcel_alerts',
      'SmartParcel Alerts',
      channelDescription: '실시간 장비 및 오류 알림',
      importance: Importance.max,
      priority: Priority.high,
      ticker: 'SmartParcel Alert',
    );
    const iosDetails = DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: true,
      presentSound: true,
    );
    const details = NotificationDetails(android: androidDetails, iOS: iosDetails);
    await _plugin.show(id, title, body, details, payload: payload);
  }

  Future<void> _requestPermissions() async {
    final androidImpl =
        _plugin.resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>();
    await androidImpl?.requestNotificationsPermission();
    final iosImpl =
        _plugin.resolvePlatformSpecificImplementation<IOSFlutterLocalNotificationsPlugin>();
    await iosImpl?.requestPermissions(alert: true, badge: true, sound: true);
  }

  void _handleNotificationResponse(NotificationResponse response) {
    final payload = response.payload;
    if (payload == null || payload.isEmpty) {
      return;
    }
    _pendingRoute = payload;
    _schedulePendingNavigation();
  }

  void _schedulePendingNavigation() {
    if (_pendingRoute == null) return;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final navigator = _navigatorKey?.currentState;
      final route = _pendingRoute;
      if (navigator != null && route != null) {
        navigator.pushNamed(route);
        _pendingRoute = null;
      }
    });
  }
}
