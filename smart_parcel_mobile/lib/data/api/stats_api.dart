import '../../core/network/dio_client.dart';
import '../dto/stats_dto.dart';

String? _toIso(DateTime? dt) {
  if (dt == null) return null;
  return dt.toUtc().toIso8601String();
}

Map<String, dynamic> _params({
  DateTime? from,
  DateTime? to,
  int? groupId,
}) {
  final map = <String, dynamic>{};
  if (from != null) map['from'] = _toIso(from);
  if (to != null) map['to'] = _toIso(to);
  if (groupId != null) map['groupId'] = groupId;
  return map;
}

Future<List<CountStatDto>> fetchStatsByChute({
  DateTime? from,
  DateTime? to,
  int? groupId,
}) async {
  final dio = DioClient().dio;
  final res = await dio.get(
    '/api/stats/by-chute',
    queryParameters: _params(from: from, to: to, groupId: groupId),
  );
  final data = res.data as Map<String, dynamic>;
  final list = (data['data'] as List<dynamic>? ?? []);
  return list.map((e) => CountStatDto.fromJson(e as Map<String, dynamic>)).toList();
}

Future<List<DailyCountStatDto>> fetchStatsDaily({
  DateTime? from,
  DateTime? to,
  int? groupId,
}) async {
  final dio = DioClient().dio;
  final res = await dio.get(
    '/api/stats/daily',
    queryParameters: _params(from: from, to: to, groupId: groupId),
  );
  final data = res.data as Map<String, dynamic>;
  final list = (data['data'] as List<dynamic>? ?? []);
  return list.map((e) => DailyCountStatDto.fromJson(e as Map<String, dynamic>)).toList();
}

Future<List<CountStatDto>> fetchStatsByErrorCode({
  DateTime? from,
  DateTime? to,
  int? groupId,
}) async {
  final dio = DioClient().dio;
  final res = await dio.get(
    '/api/stats/by-error-code',
    queryParameters: _params(from: from, to: to, groupId: groupId),
  );
  final data = res.data as Map<String, dynamic>;
  final list = (data['data'] as List<dynamic>? ?? []);
  return list.map((e) => CountStatDto.fromJson(e as Map<String, dynamic>)).toList();
}

Future<ErrorRateDto> fetchErrorRate({
  DateTime? from,
  DateTime? to,
  int? groupId,
}) async {
  final dio = DioClient().dio;
  final res = await dio.get(
    '/api/stats/error-rate',
    queryParameters: _params(from: from, to: to, groupId: groupId),
  );
  final data = res.data as Map<String, dynamic>;
  return ErrorRateDto.fromJson(data['data'] as Map<String, dynamic>);
}
