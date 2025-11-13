class ChuteDto {
  final int id;
  final String name;
  final int servoDeg;

  ChuteDto({
    required this.id,
    required this.name,
    required this.servoDeg,
  });

  factory ChuteDto.fromJson(Map<String, dynamic> json) {
    return ChuteDto(
      id: (json['id'] as num).toInt(),
      name: (json['chuteName'] as String?) ?? 'Chute',
      servoDeg: (json['servoDeg'] as num?)?.toInt() ?? 0,
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'chuteName': name,
        'servoDeg': servoDeg,
      };

  ChuteDto copyWith({String? name, int? servoDeg}) {
    return ChuteDto(
      id: id,
      name: name ?? this.name,
      servoDeg: servoDeg ?? this.servoDeg,
    );
  }
}
