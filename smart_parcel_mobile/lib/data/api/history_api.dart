import '../../core/network/dio_client.dart';
import '../dto/error_history_dto.dart';
import '../dto/page_response.dart';
import '../dto/sorting_history_dto.dart';

String? _toIso(DateTime? dt) {
  if (dt == null) return null;
  return dt.toUtc().toIso8601String();
}

Map<String, dynamic> _buildParams(Map<String, dynamic> params) {
  final filtered = <String, dynamic>{};
  params.forEach((key, value) {
    if (value == null) return;
    if (value is String && value.isEmpty) return;
    filtered[key] = value;
  });
  return filtered;
}

Future<PageResponse<SortingHistorySummaryDto>> fetchSortingHistory({
  int page = 0,
  int size = 20,
  String? keyword,
  DateTime? from,
  DateTime? to,
}) async {
  final dio = DioClient().dio;
  final res = await dio.get(
    '/api/sorting/history',
    queryParameters: _buildParams({
      'page': page,
      'size': size,
      'q': keyword?.trim(),
      'from': _toIso(from),
      'to': _toIso(to),
    }),
  );
  final data = res.data as Map<String, dynamic>;
  final pageJson = data['data'] as Map<String, dynamic>;
  return PageResponse.fromJson(
    pageJson,
    (json) => SortingHistorySummaryDto.fromJson(json as Map<String, dynamic>),
  );
}

Future<SortingHistoryDetailDto> fetchSortingHistoryDetail(int id) async {
  final dio = DioClient().dio;
  final res = await dio.get('/api/sorting/history/$id');
  final data = res.data as Map<String, dynamic>;
  return SortingHistoryDetailDto.fromJson(data['data'] as Map<String, dynamic>);
}

Future<PageResponse<ErrorHistorySummaryDto>> fetchErrorHistory({
  int page = 0,
  int size = 20,
  String? keyword,
  DateTime? from,
  DateTime? to,
}) async {
  final dio = DioClient().dio;
  final res = await dio.get(
    '/api/errors/history',
    queryParameters: _buildParams({
      'page': page,
      'size': size,
      'q': keyword?.trim(),
      'from': _toIso(from),
      'to': _toIso(to),
    }),
  );
  final data = res.data as Map<String, dynamic>;
  final pageJson = data['data'] as Map<String, dynamic>;
  return PageResponse.fromJson(
    pageJson,
    (json) => ErrorHistorySummaryDto.fromJson(json as Map<String, dynamic>),
  );
}

Future<ErrorHistoryDetailDto> fetchErrorHistoryDetail(int id) async {
  final dio = DioClient().dio;
  final res = await dio.get('/api/errors/history/$id');
  final data = res.data as Map<String, dynamic>;
  return ErrorHistoryDetailDto.fromJson(data['data'] as Map<String, dynamic>);
}
