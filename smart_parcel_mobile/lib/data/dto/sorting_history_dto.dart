import 'image_resource_bundle.dart';

DateTime? _parseDateTime(String? raw) {
  if (raw == null) return null;
  final parsed = DateTime.tryParse(raw);
  return parsed?.toLocal();
}

class SortingHistorySummaryDto {
  final int id;
  final String itemName;
  final String lineName;
  final DateTime? processedAt;

  SortingHistorySummaryDto({
    required this.id,
    required this.itemName,
    required this.lineName,
    required this.processedAt,
  });

  factory SortingHistorySummaryDto.fromJson(Map<String, dynamic> json) {
    return SortingHistorySummaryDto(
      id: (json['id'] as num).toInt(),
      itemName: (json['itemName'] as String?) ?? '-',
      lineName: (json['lineName'] as String?) ?? '-',
      processedAt: _parseDateTime(json['processedAt'] as String?),
    );
  }
}

class SortingHistoryDetailDto {
  final int id;
  final String itemName;
  final String lineName;
  final DateTime? processedAt;
  final ImageResourceBundleDto? images;

  SortingHistoryDetailDto({
    required this.id,
    required this.itemName,
    required this.lineName,
    required this.processedAt,
    required this.images,
  });

  factory SortingHistoryDetailDto.fromJson(Map<String, dynamic> json) {
    return SortingHistoryDetailDto(
      id: (json['id'] as num).toInt(),
      itemName: (json['itemName'] as String?) ?? '-',
      lineName: (json['lineName'] as String?) ?? '-',
      processedAt: _parseDateTime(json['processedAt'] as String?),
      images: json['images'] != null
          ? ImageResourceBundleDto.fromJson(json['images'] as Map<String, dynamic>)
          : null,
    );
  }
}
