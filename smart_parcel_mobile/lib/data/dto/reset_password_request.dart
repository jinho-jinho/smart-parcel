// lib/data/dto/reset_password_request.dart
class ResetPasswordRequest {
  final String email;
  final String code;
  final String newPassword;
  ResetPasswordRequest({
    required this.email,
    required this.code,
    required this.newPassword,
  });
  Map<String, dynamic> toJson() =>
      {'email': email, 'code': code, 'newPassword': newPassword};
}
