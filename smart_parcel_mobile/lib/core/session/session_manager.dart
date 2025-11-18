import '../../data/api/user_api.dart';
import '../../data/dto/user_response_dto.dart';

class SessionManager {
  SessionManager._();

  static final SessionManager instance = SessionManager._();

  UserResponseDto? _cachedUser;
  Future<UserResponseDto?>? _pendingFetch;

  UserResponseDto? get currentUser => _cachedUser;

  Future<UserResponseDto?> ensureUserLoaded({bool force = false}) async {
    if (force) {
      _cachedUser = null;
      _pendingFetch = null;
    }
    if (_cachedUser != null) return _cachedUser;
    _pendingFetch ??= _fetchProfile();
    try {
      _cachedUser = await _pendingFetch;
    } finally {
      _pendingFetch = null;
    }
    return _cachedUser;
  }

  Future<bool> isManager() async {
    final user = await ensureUserLoaded();
    return user?.role == 'MANAGER';
  }

  Future<UserResponseDto?> _fetchProfile() async {
    try {
      final res = await fetchMe();
      final data = res['data'];
      if (data is Map<String, dynamic>) {
        return UserResponseDto.fromJson(data);
      }
    } catch (_) {}
    return null;
  }

  void clear() {
    _cachedUser = null;
    _pendingFetch = null;
  }
}
