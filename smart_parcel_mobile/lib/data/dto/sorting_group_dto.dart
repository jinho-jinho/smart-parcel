class SortingGroupDto {
  final int id;
  final String name;
  final bool enabled;
  final DateTime? updatedAt;
  final int? managerId;
  final String? managerName;
  final int processingCount;

  SortingGroupDto({
    required this.id,
    required this.name,
    required this.enabled,
    required this.updatedAt,
    required this.managerId,
    required this.managerName,
    required this.processingCount,
  });

  factory SortingGroupDto.fromJson(Map<String, dynamic> json) {
    return SortingGroupDto(
      id: (json['id'] as num).toInt(),
      name: (json['groupName'] as String?) ?? 'Unnamed Group',
      enabled: json['enabled'] as bool? ?? false,
      updatedAt: json['updatedAt'] != null ? DateTime.tryParse(json['updatedAt']) : null,
      managerId: (json['managerId'] as num?)?.toInt(),
      managerName: json['managerName'] as String?,
      processingCount: (json['processingCount'] as num?)?.toInt() ?? 0,
    );
  }

  SortingGroupDto copyWith({
    String? name,
    bool? enabled,
    int? processingCount,
  }) {
    return SortingGroupDto(
      id: id,
      name: name ?? this.name,
      enabled: enabled ?? this.enabled,
      updatedAt: updatedAt,
      managerId: managerId,
      managerName: managerName,
      processingCount: processingCount ?? this.processingCount,
    );
  }
}
