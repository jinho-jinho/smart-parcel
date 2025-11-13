import 'package:dio/dio.dart';

import '../../core/network/dio_client.dart';
import '../dto/page_response.dart';
import '../dto/sorting_group_dto.dart';

Future<PageResponse<SortingGroupDto>> fetchSortingGroups({
  int page = 0,
  int size = 20,
  bool? enabled,
  String? keyword,
}) async {
  final dio = DioClient().dio;
  final params = {
    'page': page,
    'size': size,
    if (enabled != null) 'enabled': enabled,
    if (keyword != null && keyword.isNotEmpty) 'q': keyword,
  };
  final res = await dio.get('/api/sorting-groups', queryParameters: params);
  final data = res.data as Map<String, dynamic>;
  final pageJson = data['data'] as Map<String, dynamic>;
  return PageResponse.fromJson(
    pageJson,
    (json) => SortingGroupDto.fromJson(json as Map<String, dynamic>),
  );
}

Future<SortingGroupDto> createSortingGroup(String name) async {
  final dio = DioClient().dio;
  final res = await dio.post(
    '/api/sorting-groups',
    data: {'groupName': name.trim()},
    options: Options(contentType: 'application/json'),
  );
  final data = res.data as Map<String, dynamic>;
  final body = data['data'] as Map<String, dynamic>;
  return SortingGroupDto.fromJson(body);
}

Future<SortingGroupDto> updateSortingGroup(int id, String name) async {
  final dio = DioClient().dio;
  final res = await dio.patch(
    '/api/sorting-groups/$id',
    data: {'groupName': name.trim()},
    options: Options(contentType: 'application/json'),
  );
  final data = res.data as Map<String, dynamic>;
  return SortingGroupDto.fromJson(data['data'] as Map<String, dynamic>);
}

Future<void> deleteSortingGroup(int id) async {
  await DioClient().dio.delete('/api/sorting-groups/$id');
}

Future<void> toggleSortingGroup(int id, bool enable) async {
  final path = enable ? 'enable' : 'disable';
  await DioClient().dio.post('/api/sorting-groups/$id/$path');
}
