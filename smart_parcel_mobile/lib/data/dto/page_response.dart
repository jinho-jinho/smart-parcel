class PageResponse<T> {
  final List<T> content;
  final int totalElements;
  final int totalPages;
  final int page;
  final int size;
  final bool first;
  final bool last;
  final String? sort;

  const PageResponse({
    required this.content,
    required this.totalElements,
    required this.totalPages,
    required this.page,
    required this.size,
    required this.first,
    required this.last,
    this.sort,
  });

  factory PageResponse.fromJson(
    Map<String, dynamic> json,
    T Function(Object? json) fromJsonT,
  ) {
    final items = (json['content'] as List<dynamic>? ?? [])
        .map((item) => fromJsonT(item))
        .toList();
    return PageResponse(
      content: items,
      totalElements: (json['totalElements'] as num?)?.toInt() ?? 0,
      totalPages: (json['totalPages'] as num?)?.toInt() ?? 0,
      page: (json['page'] as num?)?.toInt() ?? 0,
      size: (json['size'] as num?)?.toInt() ?? items.length,
      first: json['first'] as bool? ?? true,
      last: json['last'] as bool? ?? true,
      sort: json['sort'] as String?,
    );
  }
}
