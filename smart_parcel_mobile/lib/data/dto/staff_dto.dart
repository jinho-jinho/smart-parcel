class StaffSummaryDto {
  final int id;
  final String name;
  final String email;
  final String role;
  final DateTime? createdAt;

  StaffSummaryDto({
    required this.id,
    required this.name,
    required this.email,
    required this.role,
    required this.createdAt,
  });

  factory StaffSummaryDto.fromJson(Map<String, dynamic> json) {
    return StaffSummaryDto(
      id: (json['id'] as num).toInt(),
      name: (json['name'] as String?) ?? '-',
      email: (json['email'] as String?) ?? '-',
      role: (json['role'] as String?) ?? 'STAFF',
      createdAt: json['createdAt'] != null ? DateTime.tryParse(json['createdAt']) : null,
    );
  }
}
