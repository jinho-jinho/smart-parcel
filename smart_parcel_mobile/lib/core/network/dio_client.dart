import 'dart:async';
import 'package:dio/dio.dart';
import 'package:dio/io.dart';
import 'package:dio_cookie_manager/dio_cookie_manager.dart';
import 'package:cookie_jar/cookie_jar.dart';
import 'package:flutter/foundation.dart' show kIsWeb, defaultTargetPlatform, TargetPlatform;

import '../config/app_config.dart';
import '../storage/token_storage.dart';

class DioClient {
  DioClient._internal(this._dio, this._tokenStorage);

  final Dio _dio;
  final TokenStorage _tokenStorage;

  bool _isRefreshing = false;
  final List<Completer<void>> _refreshWaiters = [];

  static DioClient? _instance;
  factory DioClient() {
    if (_instance != null) return _instance!;
    final dio = Dio(BaseOptions(
      baseUrl: AppConfig.baseUrl,
      connectTimeout: const Duration(seconds: 12),
      receiveTimeout: const Duration(seconds: 20),
      // 브라우저(F.WEB)에서 쿠키 전송 허용
      extra: {'withCredentials': true},
    ));

    // 모바일/데스크톱에서 RT 쿠키 자동 전송
    try {
      dio.interceptors.add(CookieManager(CookieJar()));
    } catch (_) {}

    // (선택) 요청/응답 로그
    dio.interceptors.add(LogInterceptor(
      request: true, requestHeader: true, requestBody: true,
      responseHeader: false, responseBody: false, error: true,
    ));

    final ts = TokenStorage();
    final client = DioClient._internal(dio, ts);

    // 요청 인터셉터: AT 헤더 부착
    dio.interceptors.add(InterceptorsWrapper(
      onRequest: (opt, handler) async {
        final at = await ts.getAccessToken();
        if (at != null && at.isNotEmpty) {
          opt.headers['Authorization'] = 'Bearer $at';
        }
        handler.next(opt);
      },
    ));

    // 401 처리: RT로 회전 → 대기열 처리 → 원요청 재시도
    dio.interceptors.add(InterceptorsWrapper(
      onError: (e, handler) async {
        final status = e.response?.statusCode;
        final isRefresh = e.requestOptions.path.contains('/user/token/refresh');

        if (status == 401 && !isRefresh) {
          try {
            await client._refreshToken();
            final retried = await client._retry(e.requestOptions);
            return handler.resolve(retried);
          } catch (_) {
            await ts.clear();
          }
        }
        handler.next(e);
      },
    ));

    _instance = client;
    return _instance!;
  }

  Dio get dio => _dio;

  Future<void> _refreshToken() async {
    if (_isRefreshing) {
      final c = Completer<void>();
      _refreshWaiters.add(c);
      return c.future;
    }
    _isRefreshing = true;
    try {
      final res = await _dio.post('/user/token/refresh');
      final data = res.data as Map;
      final newAt = (data['data'] as Map?)?['accessToken'] as String?;
      if (newAt == null) {
        throw StateError('No accessToken in refresh response');
      }
      await _tokenStorage.saveAccessToken(newAt);
    } finally {
      _isRefreshing = false;
      for (final w in _refreshWaiters) {
        if (!w.isCompleted) w.complete();
      }
      _refreshWaiters.clear();
    }
  }

  Future<Response<dynamic>> _retry(RequestOptions ro) {
    return _dio.request<dynamic>(
      ro.path,
      data: ro.data,
      queryParameters: ro.queryParameters,
      options: Options(
        method: ro.method,
        headers: ro.headers,
        contentType: ro.contentType,
        responseType: ro.responseType,
      ),
    );
  }
}
