class ImageResourceBundleDto {
  final String? original;
  final String? thumbnail;
  final String? snapshot;

  const ImageResourceBundleDto({
    this.original,
    this.thumbnail,
    this.snapshot,
  });

  factory ImageResourceBundleDto.fromJson(Map<String, dynamic> json) {
    return ImageResourceBundleDto(
      original: json['original'] as String?,
      thumbnail: json['thumbnail'] as String?,
      snapshot: json['snapshot'] as String?,
    );
  }

  String? get primary =>
      original ??
      thumbnail ??
      snapshot;
}
