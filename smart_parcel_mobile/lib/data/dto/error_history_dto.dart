import 'image_resource_bundle.dart';

class ErrorHistorySummaryDto {
  final int id;
  final String itemName;
  final String lineName;
  final String errorCode;
  final DateTime? occurredAt;

  ErrorHistorySummaryDto({
    required this.id,
    required this.itemName,
    required this.lineName,
    required this.errorCode,
    required this.occurredAt,
  });

  factory ErrorHistorySummaryDto.fromJson(Map<String, dynamic> json) {
    return ErrorHistorySummaryDto(
      id: (json['id'] as num).toInt(),
      itemName: (json['itemName'] as String?) ?? '-',
      lineName: (json['lineName'] as String?) ?? '-',
      errorCode: (json['errorCode'] as String?) ?? '-',
      occurredAt: json['occurredAt'] != null ? DateTime.tryParse(json['occurredAt']) : null,
    );
  }
}

class ErrorHistoryDetailDto {
  final int id;
  final String itemName;
  final String lineName;
  final String errorCode;
  final DateTime? occurredAt;
  final ImageResourceBundleDto? images;

  ErrorHistoryDetailDto({
    required this.id,
    required this.itemName,
    required this.lineName,
    required this.errorCode,
    required this.occurredAt,
    required this.images,
  });

  factory ErrorHistoryDetailDto.fromJson(Map<String, dynamic> json) {
    return ErrorHistoryDetailDto(
      id: (json['id'] as num).toInt(),
      itemName: (json['itemName'] as String?) ?? '-',
      lineName: (json['lineName'] as String?) ?? '-',
      errorCode: (json['errorCode'] as String?) ?? '-',
      occurredAt: json['occurredAt'] != null ? DateTime.tryParse(json['occurredAt']) : null,
      images: json['images'] != null
          ? ImageResourceBundleDto.fromJson(json['images'] as Map<String, dynamic>)
          : null,
    );
  }
}
