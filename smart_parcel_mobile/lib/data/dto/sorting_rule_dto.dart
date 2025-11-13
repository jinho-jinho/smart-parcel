import 'chute_dto.dart';

class SortingRuleDto {
  final int id;
  final int groupId;
  final String name;
  final String inputType;
  final String inputValue;
  final String itemName;
  final DateTime? createdAt;
  final List<RuleChuteDto> chutes;

  SortingRuleDto({
    required this.id,
    required this.groupId,
    required this.name,
    required this.inputType,
    required this.inputValue,
    required this.itemName,
    required this.createdAt,
    required this.chutes,
  });

  factory SortingRuleDto.fromJson(Map<String, dynamic> json) {
    final chutesJson = (json['chutes'] as List<dynamic>? ?? []);
    return SortingRuleDto(
      id: (json['id'] as num).toInt(),
      groupId: (json['groupId'] as num?)?.toInt() ?? 0,
      name: (json['ruleName'] as String?) ?? 'Unnamed Rule',
      inputType: (json['inputType'] as String?) ?? 'TEXT',
      inputValue: (json['inputValue'] as String?) ?? '',
      itemName: (json['itemName'] as String?) ?? '',
      createdAt: json['createdAt'] != null ? DateTime.tryParse(json['createdAt']) : null,
      chutes: chutesJson
          .map((c) => RuleChuteDto.fromJson(c as Map<String, dynamic>))
          .toList(growable: false),
    );
  }
}

class RuleChuteDto {
  final int id;
  final String? name;
  final int? angle;

  RuleChuteDto({required this.id, this.name, this.angle});

  factory RuleChuteDto.fromJson(Map<String, dynamic> json) {
    return RuleChuteDto(
      id: (json['id'] as num).toInt(),
      name: json['chuteName'] as String?,
      angle: (json['servoDeg'] as num?)?.toInt(),
    );
  }

  ChuteDto toChute() {
    return ChuteDto(
      id: id,
      name: name ?? 'Chute $id',
      servoDeg: angle ?? 0,
    );
  }
}
