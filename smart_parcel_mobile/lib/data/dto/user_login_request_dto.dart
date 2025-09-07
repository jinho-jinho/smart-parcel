// lib/data/dto/user_login_request_dto.dart
class UserLoginRequestDto {
  final String email;
  final String password;
  UserLoginRequestDto({required this.email, required this.password});
  Map<String, dynamic> toJson() => {'email': email, 'password': password};
}
