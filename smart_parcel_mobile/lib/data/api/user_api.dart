import 'package:dio/dio.dart';
import '../../core/network/dio_client.dart';
import '../../core/storage/token_storage.dart';

/// React: signup({ email, password, name, bizNumber, role })
Future<Response<dynamic>> signup({
  required String email,
  required String password,
  required String name,
  String? bizNumber,
  String? role, // 서버가 받으면 전달 (없으면 제외)
}) {
  final dio = DioClient().dio;
  final body = <String, dynamic>{
    'email': email,
    'password': password,
    'name': name,
    'bizNumber': (bizNumber?.isEmpty ?? true) ? null : bizNumber,
  };
  if (role != null && role.isNotEmpty) body['role'] = role; // "ADMIN" 등
  return dio.post('/user/signup',
      data: body, options: Options(contentType: 'application/json'));
}

/// React: login({ email, password }) → AT 저장, RT는 쿠키
Future<Map<String, dynamic>> login({
  required String email,
  required String password,
}) async {
  final dio = DioClient().dio;
  final res = await dio.post(
    '/user/login',
    data: {'email': email, 'password': password},
    options: Options(contentType: 'application/json'),
  );
  final map = res.data as Map<String, dynamic>;
  final at = (map['data'] as Map?)?['accessToken'] as String?;
  if (at != null) {
    await TokenStorage().saveAccessToken(at);
  }
  return map; // { success, data: { accessToken, tokenType, expiresInMs }, message }
}

/// React: fetchMe() → user 상태 업데이트는 호출측에서 처리
Future<Map<String, dynamic>> fetchMe() async {
  final dio = DioClient().dio;
  final res = await dio.get('/user/me');
  return res.data as Map<String, dynamic>; // { success, data: UserResponseDto, message }
}

/// React: refreshToken()
Future<Map<String, dynamic>> refreshToken() async {
  final dio = DioClient().dio;
  final res = await dio.post('/user/token/refresh');
  final map = res.data as Map<String, dynamic>;
  final at = (map['data'] as Map?)?['accessToken'] as String?;
  if (at != null) {
    await TokenStorage().saveAccessToken(at);
  }
  return map;
}

/// React: logout()
Future<void> logout() async {
  final dio = DioClient().dio;
  try {
    await dio.post('/user/logout');
  } finally {
    await TokenStorage().clear();
  }
}
