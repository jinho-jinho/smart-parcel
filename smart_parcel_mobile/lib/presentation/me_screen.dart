import 'package:flutter/material.dart';

import './core/colors.dart';
import '../core/session/session_manager.dart';
import '../core/storage/auth_preference_storage.dart';
import '../data/api/user_api.dart' as user_api;
import '../data/dto/user_response_dto.dart';
import 'widgets/app_shell.dart';

class MeScreen extends StatefulWidget {
  const MeScreen({super.key});

  @override
  State<MeScreen> createState() => _MeScreenState();
}

class _MeScreenState extends State<MeScreen> {
  UserResponseDto? _user;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final res = await user_api.fetchMe();
      final data = res['data'] as Map<String, dynamic>?;
      if (data != null) {
        setState(() => _user = UserResponseDto.fromJson(data));
      } else {
        setState(() => _error = '내 정보를 불러올 수 없습니다.');
      }
    } catch (e) {
      setState(() => _error = '내 정보를 불러올 수 없습니다.');
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _logout() async {
    await user_api.logout();
    SessionManager.instance.clear();
    await AuthPreferenceStorage().setAutoLoginEnabled(false);
    if (!mounted) return;
    Navigator.of(context).pushNamedAndRemoveUntil('/login', (route) => false);
  }

  String _roleLabel(String role) {
    switch (role) {
      case 'ADMIN':
        return '어드민';
      case 'MANAGER':
        return '관리자';
      default:
        return '직원';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bg,
      drawer: const AppMenuDrawer(),
      appBar: const SmartParcelAppBar(),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(_error!, style: const TextStyle(color: Colors.redAccent)),
                      const SizedBox(height: 12),
                      OutlinedButton(onPressed: _load, child: const Text('다시 시도')),
                    ],
                  ),
                )
              : Padding(
                  padding: const EdgeInsets.all(24),
                  child: Card(
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
                    child: Padding(
                      padding: const EdgeInsets.all(24),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          _infoField('이름', _user?.name ?? '-'),
                          const SizedBox(height: 12),
                          _infoField('이메일', _user?.email ?? '-'),
                          const SizedBox(height: 12),
                          _infoField('권한/역할', _roleLabel(_user?.role ?? 'STAFF')),
                          const SizedBox(height: 24),
                          SizedBox(
                            width: double.infinity,
                            child: FilledButton(
                              onPressed: () => Navigator.pushNamed(context, '/reset-password'),
                              child: const Text('비밀번호 변경'),
                            ),
                          ),
                          const SizedBox(height: 12),
                          SizedBox(
                            width: double.infinity,
                            child: OutlinedButton(
                              onPressed: _logout,
                              child: const Text('로그아웃'),
                            ),
                          )
                        ],
                      ),
                    ),
                  ),
                ),
    );
  }

  Widget _infoField(String label, String value) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontWeight: FontWeight.w600)),
        const SizedBox(height: 8),
        TextFormField(
          initialValue: value,
          readOnly: true,
          decoration: InputDecoration(
            filled: true,
            fillColor: AppColors.bg,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide.none,
            ),
          ),
        ),
      ],
    );
  }
}
