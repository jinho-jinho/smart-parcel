import '../../core/network/dio_client.dart';
import '../dto/notification_dto.dart';
import '../dto/page_response.dart';

Future<PageResponse<NotificationDto>> fetchNotifications({
  int page = 0,
  int size = 10,
  bool? unreadOnly,
}) async {
  final dio = DioClient().dio;
  final res = await dio.get(
    '/api/notifications',
    queryParameters: {
      'page': page,
      'size': size,
      if (unreadOnly != null) 'unreadOnly': unreadOnly,
    },
  );
  final data = res.data as Map<String, dynamic>;
  final pageJson = data['data'] as Map<String, dynamic>;
  return PageResponse.fromJson(
    pageJson,
    (json) => NotificationDto.fromJson(json as Map<String, dynamic>),
  );
}

Future<void> markNotificationRead(int notificationId) async {
  await DioClient().dio.patch('/api/notifications/$notificationId/read');
}
