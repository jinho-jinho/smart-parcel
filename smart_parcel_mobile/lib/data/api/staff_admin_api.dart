import '../../core/network/dio_client.dart';
import '../dto/page_response.dart';
import '../dto/staff_dto.dart';

Future<PageResponse<StaffSummaryDto>> fetchStaff({
  int page = 0,
  int size = 20,
  String? keyword,
}) async {
  final dio = DioClient().dio;
  final res = await dio.get(
    '/api/admin/staff',
    queryParameters: {
      'page': page,
      'size': size,
      if (keyword != null && keyword.isNotEmpty) 'q': keyword,
    },
  );
  final data = res.data as Map<String, dynamic>;
  final pageJson = data['data'] as Map<String, dynamic>;
  return PageResponse.fromJson(
    pageJson,
    (json) => StaffSummaryDto.fromJson(json as Map<String, dynamic>),
  );
}

Future<void> deleteStaffMember(int staffId) async {
  await DioClient().dio.delete('/api/admin/staff/$staffId');
}
