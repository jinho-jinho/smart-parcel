class UserResponseDto {
  final int id;
  final String email;
  final String name;
  final String role;
  final DateTime? createdAt;

  UserResponseDto({
    required this.id,
    required this.email,
    required this.name,
    required this.role,
    this.createdAt,
  });

  factory UserResponseDto.fromJson(Map<String, dynamic> json) => UserResponseDto(
        id: (json['id'] as num).toInt(),
        email: json['email'] as String,
        name: json['name'] as String,
        role: (json['role'] as String?) ?? 'STAFF',
        createdAt: json['createdAt'] != null ? DateTime.tryParse(json['createdAt']) : null,
      );
}
