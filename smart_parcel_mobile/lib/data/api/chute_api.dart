import 'package:dio/dio.dart';

import '../../core/network/dio_client.dart';
import '../dto/chute_dto.dart';
import '../dto/page_response.dart';

Future<PageResponse<ChuteDto>> fetchChutes({
  int page = 0,
  int size = 50,
  int? groupId,
  String? keyword,
}) async {
  final dio = DioClient().dio;
  final params = {
    'page': page,
    'size': size,
    if (groupId != null) 'groupId': groupId,
    if (keyword != null && keyword.isNotEmpty) 'q': keyword,
  };
  final res = await dio.get('/api/chutes', queryParameters: params);
  final data = res.data as Map<String, dynamic>;
  final body = data['data'] as Map<String, dynamic>;
  return PageResponse.fromJson(
    body,
    (json) => ChuteDto.fromJson(json as Map<String, dynamic>),
  );
}

Future<ChuteDto> createChute({
  required String name,
  required int servoDeg,
}) async {
  final dio = DioClient().dio;
  final res = await dio.post(
    '/api/chutes',
    data: {'chuteName': name.trim(), 'servoDeg': servoDeg},
    options: Options(contentType: 'application/json'),
  );
  final data = res.data as Map<String, dynamic>;
  return ChuteDto.fromJson(data['data'] as Map<String, dynamic>);
}

Future<ChuteDto> updateChute({
  required int chuteId,
  String? name,
  int? servoDeg,
}) async {
  final dio = DioClient().dio;
  final payload = <String, dynamic>{};
  if (name != null) payload['chuteName'] = name.trim();
  if (servoDeg != null) payload['servoDeg'] = servoDeg;

  final res = await dio.patch(
    '/api/chutes/$chuteId',
    data: payload,
    options: Options(contentType: 'application/json'),
  );
  final data = res.data as Map<String, dynamic>;
  return ChuteDto.fromJson(data['data'] as Map<String, dynamic>);
}

Future<void> deleteChute(int chuteId) async {
  await DioClient().dio.delete('/api/chutes/$chuteId');
}
