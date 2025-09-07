// lib/data/dto/user_response_dto.dart
class UserResponseDto {
  final int id;
  final String email;
  final String name;
  final String? bizNumber;

  UserResponseDto({
    required this.id,
    required this.email,
    required this.name,
    this.bizNumber,
  });

  factory UserResponseDto.fromJson(Map<String, dynamic> json) =>
      UserResponseDto(
        id: json['id'] as int,
        email: json['email'] as String,
        name: json['name'] as String,
        bizNumber: json['bizNumber'] as String?,
      );
}
