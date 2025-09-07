import 'package:dio/dio.dart';
import '../../core/network/dio_client.dart';

/// React: sendCode({ email, purpose="SIGNUP" })
/// POST /api/auth/send-code?email=...&purpose=SIGNUP|RESET_PASSWORD
Future<Map<String, dynamic>> sendCode({
  required String email,
  String purpose = 'SIGNUP',
}) async {
  final dio = DioClient().dio;
  final res = await dio.post(
    '/api/auth/send-code',
    queryParameters: {'email': email, 'purpose': purpose.toUpperCase()},
  );
  return res.data as Map<String, dynamic>; // { success, data, message }
}

/// React: verifyCode({ email, code, purpose="SIGNUP" })
/// POST /api/auth/verify-code?email=...&code=...&purpose=SIGNUP|RESET_PASSWORD
Future<Map<String, dynamic>> verifyCode({
  required String email,
  required String code,
  String purpose = 'SIGNUP',
}) async {
  final dio = DioClient().dio;
  final res = await dio.post(
    '/api/auth/verify-code',
    queryParameters: {
      'email': email,
      'code': code,
      'purpose': purpose.toUpperCase(),
    },
  );
  return res.data as Map<String, dynamic>;
}

/// React: resetPassword({ email, code, newPassword })
/// POST /user/password/reset  (JSON)
Future<Map<String, dynamic>> resetPassword({
  required String email,
  required String code,
  required String newPassword,
}) async {
  final dio = DioClient().dio;
  final res = await dio.post(
    '/user/password/reset',
    data: {'email': email, 'code': code, 'newPassword': newPassword},
    options: Options(contentType: 'application/json'),
  );
  return res.data as Map<String, dynamic>; // { success, data:null, message:... }
}
