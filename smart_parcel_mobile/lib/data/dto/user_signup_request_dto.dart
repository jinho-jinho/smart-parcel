// lib/data/dto/user_signup_request_dto.dart
class UserSignupRequestDto {
  final String email;
  final String password;
  final String name;
  final String? bizNumber;
  UserSignupRequestDto({
    required this.email,
    required this.password,
    required this.name,
    this.bizNumber,
  });
  Map<String, dynamic> toJson() => {
        'email': email,
        'password': password,
        'name': name,
        'bizNumber': bizNumber,
      };
}
