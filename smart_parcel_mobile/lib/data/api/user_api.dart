import 'package:dio/dio.dart';

import '../../core/network/dio_client.dart';
import '../../core/storage/token_storage.dart';

/// Signup request mirroring the web client.
Future<Response<dynamic>> signup({
  required String email,
  required String password,
  required String name,
  String? bizNumber,
  String role = 'STAFF',
  String? managerEmail,
}) {
  final dio = DioClient().dio;
  final body = <String, dynamic>{
    'email': email,
    'password': password,
    'name': name,
    'role': role,
    'bizNumber': (bizNumber?.isEmpty ?? true) ? null : bizNumber,
    'managerEmail':
        role == 'STAFF' ? ((managerEmail?.isEmpty ?? true) ? null : managerEmail) : null,
  };
  return dio.post(
    '/api/auth/signup',
    data: body,
    options: Options(contentType: 'application/json'),
  );
}

/// Login issues access token (refresh token rides on cookie).
Future<Map<String, dynamic>> login({
  required String email,
  required String password,
}) async {
  final dio = DioClient().dio;
  final res = await dio.post(
    '/api/auth/login',
    data: {'email': email, 'password': password},
    options: Options(contentType: 'application/json'),
  );
  final map = res.data as Map<String, dynamic>;
  final at = (map['data'] as Map?)?['accessToken'] as String?;
  if (at != null) {
    await TokenStorage().saveAccessToken(at);
  }
  return map;
}

/// Fetch authenticated user profile.
Future<Map<String, dynamic>> fetchMe() async {
  final dio = DioClient().dio;
  final res = await dio.get('/api/users/me');
  return res.data as Map<String, dynamic>;
}

/// Refresh the access token via refresh cookie.
Future<Map<String, dynamic>> refreshToken() async {
  final dio = DioClient().dio;
  final res = await dio.post('/api/auth/token/refresh');
  final map = res.data as Map<String, dynamic>;
  final at = (map['data'] as Map?)?['accessToken'] as String?;
  if (at != null) {
    await TokenStorage().saveAccessToken(at);
  }
  return map;
}

/// Logout clears tokens on both server and client.
Future<void> logout() async {
  final dio = DioClient().dio;
  try {
    await dio.post('/api/auth/logout');
  } finally {
    await TokenStorage().clear();
  }
}
