class NotificationDto {
  final int id;
  final bool read;
  final DateTime? createdAt;
  final int? errorLogId;
  final String? errorCode;
  final DateTime? occurredAt;
  final String? groupName;
  final String? chuteName;

  NotificationDto({
    required this.id,
    required this.read,
    required this.createdAt,
    this.errorLogId,
    this.errorCode,
    this.occurredAt,
    this.groupName,
    this.chuteName,
  });

  factory NotificationDto.fromJson(Map<String, dynamic> json) {
    return NotificationDto(
      id: (json['id'] as num).toInt(),
      read: json['read'] as bool? ?? false,
      createdAt: json['createdAt'] != null ? DateTime.tryParse(json['createdAt']) : null,
      errorLogId: (json['errorLogId'] as num?)?.toInt(),
      errorCode: json['errorCode'] as String?,
      occurredAt: json['occurredAt'] != null ? DateTime.tryParse(json['occurredAt']) : null,
      groupName: json['groupName'] as String?,
      chuteName: json['chuteName'] as String?,
    );
  }

  NotificationDto copyWith({bool? read}) {
    return NotificationDto(
      id: id,
      read: read ?? this.read,
      createdAt: createdAt,
      errorLogId: errorLogId,
      errorCode: errorCode,
      occurredAt: occurredAt,
      groupName: groupName,
      chuteName: chuteName,
    );
  }
}
