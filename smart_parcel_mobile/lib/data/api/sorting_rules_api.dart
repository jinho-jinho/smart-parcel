import 'package:dio/dio.dart';

import '../../core/network/dio_client.dart';
import '../dto/page_response.dart';
import '../dto/sorting_rule_dto.dart';

Future<PageResponse<SortingRuleDto>> fetchSortingRules(
  int groupId, {
  int page = 0,
  int size = 20,
  String? keyword,
  String? type,
}) async {
  final dio = DioClient().dio;
  final params = {
    'page': page,
    'size': size,
    if (keyword != null && keyword.isNotEmpty) 'q': keyword,
    if (type != null && type.isNotEmpty) 'type': type,
  };
  final res = await dio.get(
    '/api/sorting-groups/$groupId/rules',
    queryParameters: params,
  );
  final data = res.data as Map<String, dynamic>;
  final body = data['data'] as Map<String, dynamic>;
  return PageResponse.fromJson(
    body,
    (json) => SortingRuleDto.fromJson(json as Map<String, dynamic>),
  );
}

Future<SortingRuleDto> createSortingRule({
  required int groupId,
  required String ruleName,
  required String inputType,
  required String inputValue,
  required String itemName,
  List<int>? chuteIds,
}) async {
  final dio = DioClient().dio;
  final res = await dio.post(
    '/api/sorting-groups/$groupId/rules',
    data: {
      'ruleName': ruleName.trim(),
      'inputType': inputType,
      'inputValue': inputValue.trim(),
      'itemName': itemName.trim(),
      'chuteIds': chuteIds ?? [],
    },
    options: Options(contentType: 'application/json'),
  );
  final data = res.data as Map<String, dynamic>;
  return SortingRuleDto.fromJson(data['data'] as Map<String, dynamic>);
}

Future<SortingRuleDto> updateSortingRule({
  required int ruleId,
  String? ruleName,
  String? inputType,
  String? inputValue,
  String? itemName,
  List<int>? chuteIds,
}) async {
  final dio = DioClient().dio;
  final payload = <String, dynamic>{};
  if (ruleName != null) payload['ruleName'] = ruleName.trim();
  if (inputType != null) payload['inputType'] = inputType;
  if (inputValue != null) payload['inputValue'] = inputValue.trim();
  if (itemName != null) payload['itemName'] = itemName.trim();
  if (chuteIds != null) payload['chuteIds'] = chuteIds;

  final res = await dio.patch(
    '/api/sorting-rules/$ruleId',
    data: payload,
    options: Options(contentType: 'application/json'),
  );
  final data = res.data as Map<String, dynamic>;
  return SortingRuleDto.fromJson(data['data'] as Map<String, dynamic>);
}

Future<void> deleteSortingRule(int ruleId) async {
  await DioClient().dio.delete('/api/sorting-rules/$ruleId');
}
