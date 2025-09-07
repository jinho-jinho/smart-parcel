// lib/data/dto/login_response_dto.dart
class LoginResponseDto {
  final String accessToken;
  final String tokenType;
  final int expiresInMs;
  LoginResponseDto({
    required this.accessToken,
    required this.tokenType,
    required this.expiresInMs,
  });
  factory LoginResponseDto.fromJson(Map<String, dynamic> json) =>
      LoginResponseDto(
        accessToken: json['accessToken'] as String,
        tokenType: json['tokenType'] as String,
        expiresInMs: json['expiresInMs'] as int,
      );
}
