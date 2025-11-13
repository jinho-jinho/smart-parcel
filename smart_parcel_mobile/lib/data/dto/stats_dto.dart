class CountStatDto {
  final String label;
  final int count;

  CountStatDto({required this.label, required this.count});

  factory CountStatDto.fromJson(Map<String, dynamic> json) {
    return CountStatDto(
      label: (json['label'] as String?) ?? '-',
      count: (json['count'] as num?)?.toInt() ?? 0,
    );
  }
}

class DailyCountStatDto {
  final String date;
  final int count;

  DailyCountStatDto({required this.date, required this.count});

  factory DailyCountStatDto.fromJson(Map<String, dynamic> json) {
    return DailyCountStatDto(
      date: (json['date'] as String?) ?? '',
      count: (json['count'] as num?)?.toInt() ?? 0,
    );
  }
}

class ErrorRateDto {
  final int totalProcessed;
  final int totalErrors;
  final double errorRatePercent;

  ErrorRateDto({
    required this.totalProcessed,
    required this.totalErrors,
    required this.errorRatePercent,
  });

  factory ErrorRateDto.fromJson(Map<String, dynamic> json) {
    return ErrorRateDto(
      totalProcessed: (json['totalProcessed'] as num?)?.toInt() ?? 0,
      totalErrors: (json['totalErrors'] as num?)?.toInt() ?? 0,
      errorRatePercent: (json['errorRatePercent'] as num?)?.toDouble() ?? 0,
    );
  }
}
