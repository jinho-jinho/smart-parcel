import 'dart:async';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import '../data/api/auth_api.dart' show sendCode, verifyCode, resetPassword;

class ResetPasswordScreen extends StatefulWidget {
  const ResetPasswordScreen({super.key});

  @override
  State<ResetPasswordScreen> createState() => _ResetPasswordScreenState();
}

class _ResetPasswordScreenState extends State<ResetPasswordScreen> {
  // ===== 컨트롤러 =====
  final _emailCtrl = TextEditingController();
  final _codeCtrl = TextEditingController();
  final _pwCtrl = TextEditingController();
  final _pw2Ctrl = TextEditingController();

  // ===== 상태 =====
  int _step = 1;                 // 1: 이메일, 2: 코드, 3: 새 비번
  String? _msg;                  // 상단 에러/안내
  bool _loading = false;

  bool _showPw = false;
  bool _showPw2 = false;

  int _remainSec = 0;            // 5분 타이머
  Timer? _timer;

  final _emailRe = RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$');

  @override
  void dispose() {
    _timer?.cancel();
    _emailCtrl.dispose();
    _codeCtrl.dispose();
    _pwCtrl.dispose();
    _pw2Ctrl.dispose();
    super.dispose();
  }

  // ===== 유틸 =====
  void _startCountdown([int sec = 300]) {
    _timer?.cancel();
    setState(() => _remainSec = sec);
    _timer = Timer.periodic(const Duration(seconds: 1), (t) {
      if (_remainSec <= 0) {
        t.cancel();
      } else {
        setState(() => _remainSec--);
      }
    });
  }

  String _fmt(int s) {
  final m = (s ~/ 60).toString().padLeft(2, '0');
  final sec = (s % 60).toString().padLeft(2, '0');
  return '$m:$sec'; // "mm:ss"
}

  // ===== 액션 =====
  Future<void> _handleSend() async {
    setState(() => _msg = null);
    final email = _emailCtrl.text.trim();
    if (!_emailRe.hasMatch(email)) {
      setState(() => _msg = '올바른 이메일을 입력하세요.');
      return;
    }

    setState(() => _loading = true);
    try {
      final res = await sendCode(email: email, purpose: 'RESET_PASSWORD');
      setState(() {
        _step = 2;
        _msg = (res['message'] as String?) ?? '코드가 전송되었습니다.';
      });
      _startCountdown(300);
    } on DioException catch (e) {
      setState(() => _msg = (e.response?.data?['message'] as String?) ?? '코드 전송 실패');
    } catch (_) {
      setState(() => _msg = '코드 전송 실패');
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _handleVerify() async {
    setState(() => _msg = null);
    final email = _emailCtrl.text.trim();
    final code = _codeCtrl.text.trim();
    if (code.isEmpty) {
      setState(() => _msg = '코드를 입력해주세요.');
      return;
    }
    try {
      final res = await verifyCode(email: email, code: code, purpose: 'RESET_PASSWORD');
      setState(() {
        _step = 3;
        _msg = (res['message'] as String?) ?? '인증 성공';
      });
    } on DioException catch (e) {
      setState(() => _msg = (e.response?.data?['message'] as String?) ?? '코드 확인 실패');
    } catch (_) {
      setState(() => _msg = '코드 확인 실패');
    }
  }

  Future<void> _handleReset() async {
    setState(() => _msg = null);

    final email = _emailCtrl.text.trim();
    final code = _codeCtrl.text.trim();
    final pw = _pwCtrl.text;
    final pw2 = _pw2Ctrl.text;

    if (pw.length < 8) {
      setState(() => _msg = '비밀번호는 8자 이상이어야 합니다.');
      return;
    }
    if (pw != pw2) {
      setState(() => _msg = '비밀번호가 일치하지 않습니다.');
      return;
    }

    setState(() => _loading = true);
    try {
      final res = await resetPassword(email: email, code: code, newPassword: pw);
      setState(() => _msg = (res['message'] as String?) ?? '비밀번호가 변경되었습니다.');
      if (mounted) {
        await Future.delayed(const Duration(milliseconds: 700));
        Navigator.of(context).pop(); // 로그인 화면으로 복귀
      }
    } on DioException catch (e) {
      setState(() => _msg = (e.response?.data?['message'] as String?) ?? '비밀번호 변경 실패');
    } catch (_) {
      setState(() => _msg = '비밀번호 변경 실패');
    } finally {
      setState(() => _loading = false);
    }
  }

  // ===== UI =====
  @override
  Widget build(BuildContext context) {
    // CSS 변수 매핑
    const bg = Color(0xFFF5F5F5);
    const textColor = Color(0xFF2C2C2C);
    const muted = Color(0xFF757575);
    const inputBg = Colors.white;
    const inputBd = Color(0xFFD9D9D9);
    const primary = Color(0xFF2C2C2C);
    const primaryContr = Color(0xFFF5F5F5);
    const radiusLg = 12.0;
    const radiusMd = 10.0;
    final shadow = [
      BoxShadow(
        color: Colors.black.withOpacity(.06),
        blurRadius: 18,
        offset: const Offset(0, 6),
      )
    ];

    final inputBorder = OutlineInputBorder(
      borderRadius: BorderRadius.circular(radiusLg),
      borderSide: const BorderSide(color: inputBd),
    );

    Widget _primaryBtn({required String label, VoidCallback? onTap}) => SizedBox(
          height: 44,
          width: double.infinity,
          child: FilledButton(
            onPressed: onTap,
            style: FilledButton.styleFrom(
              backgroundColor: primary,
              foregroundColor: primaryContr,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(radiusLg),
                side: const BorderSide(color: primary),
              ),
            ),
            child: _loading
                ? const SizedBox(
                    height: 20,
                    width: 20,
                    child: CircularProgressIndicator(strokeWidth: 2, color: primaryContr),
                  )
                : Text(label, style: const TextStyle(fontSize: 15)),
          ),
        );

    Widget _secondaryBtn({required String label, VoidCallback? onTap, bool enabled = true}) =>
        SizedBox(
          height: 44,
          width: double.infinity,
          child: OutlinedButton(
            onPressed: enabled ? onTap : null,
            style: OutlinedButton.styleFrom(
              side: const BorderSide(color: inputBd),
              foregroundColor: textColor,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(radiusLg)),
            ),
            child: Text(label, style: const TextStyle(fontSize: 15)),
          ),
        );

    return Scaffold(
      backgroundColor: bg,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 560),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  // 헤더 (로고 + 브랜드)
                  Column(
                    children: [
                      Image.asset('assets/icon.png', width: 160, fit: BoxFit.contain),
                      const SizedBox(height: 10),
                      const Text('Smart Parcel',
                          style: TextStyle(
                            fontWeight: FontWeight.w800,
                            fontSize: 32,
                            letterSpacing: -0.02,
                            color: textColor,
                          )),
                    ],
                  ),
                  const SizedBox(height: 24),

                  // 카드
                  Container(
                    width: 360,
                    padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 18),
                    decoration: BoxDecoration(
                      color: inputBg,
                      border: Border.all(color: inputBd),
                      borderRadius: BorderRadius.circular(radiusMd),
                      boxShadow: shadow,
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        if (_msg != null) ...[
                          Container(
                            decoration: BoxDecoration(
                              color: const Color(0xFFFEF3F2),
                              border: Border.all(color: const Color(0xFFFECACA)),
                              borderRadius: BorderRadius.circular(10),
                            ),
                            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                            child: Text(
                              _msg!,
                              style: const TextStyle(color: Color(0xFF7F1D1D), fontSize: 14),
                            ),
                          ),
                          const SizedBox(height: 12),
                        ],

                        // ===== STEP 1: 이메일 =====
                        const Text('이메일', style: TextStyle(fontSize: 14, color: textColor)),
                        const SizedBox(height: 8),
                        TextField(
                          controller: _emailCtrl,
                          enabled: _step == 1, // 전송 후 비활성화(스샷처럼)
                          keyboardType: TextInputType.emailAddress,
                          onChanged: (_) {
                            setState(() {
                              _step = 1;
                              _remainSec = 0;
                              _codeCtrl.clear();
                            });
                          },
                          decoration: InputDecoration(
                            hintText: 'you@example.com',
                            hintStyle: const TextStyle(color: Color(0xFFB3B3B3)),
                            isDense: true,
                            contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                            border: inputBorder,
                            enabledBorder: inputBorder,
                            focusedBorder: inputBorder.copyWith(
                                borderSide: const BorderSide(color: Colors.black)),
                          ),
                        ),
                        const SizedBox(height: 12),
                        _primaryBtn(
                          label: '코드 전송',
                          onTap: (!_emailRe.hasMatch(_emailCtrl.text.trim()) || _loading)
                              ? null
                              : _handleSend,
                        ),

                        // ===== STEP 2: 코드 =====
                        if (_step >= 2) ...[
                          const SizedBox(height: 16),
                          const Text('인증번호', style: TextStyle(fontSize: 14, color: textColor)),
                          const SizedBox(height: 8),
                          TextField(
                            controller: _codeCtrl,
                            keyboardType: TextInputType.number,
                            decoration: InputDecoration(
                              hintText: '이메일로 받은 6자리 코드',
                              hintStyle: const TextStyle(color: Color(0xFFB3B3B3)),
                              isDense: true,
                              contentPadding:
                                  const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                              border: inputBorder,
                              enabledBorder: inputBorder,
                              focusedBorder: inputBorder.copyWith(
                                  borderSide: const BorderSide(color: Colors.black)),
                            ),
                          ),
                          const SizedBox(height: 12),
                          _secondaryBtn(
                            label: '코드 확인',
                            onTap: _handleVerify,
                            enabled: _codeCtrl.text.trim().isNotEmpty,
                          ),
                          const SizedBox(height: 6),
                          Text(
                            _remainSec > 0
                                ? '남은 시간 ${_fmt(_remainSec)}'
                                : '만료되었습니다. 다시 전송하세요.',
                            textAlign: TextAlign.center,
                            style: const TextStyle(fontSize: 12, color: muted),
                          ),
                        ],

                        // ===== STEP 3: 새 비밀번호 =====
                        if (_step >= 3) ...[
                          const SizedBox(height: 18),
                          const Divider(height: 1, color: inputBd),
                          const SizedBox(height: 18),

                          const Text('새 비밀번호 (8자 이상)',
                              style: TextStyle(fontSize: 14, color: textColor)),
                          const SizedBox(height: 8),
                          TextField(
                            controller: _pwCtrl,
                            obscureText: !_showPw,
                            decoration: InputDecoration(
                              hintText: '새 비밀번호',
                              isDense: true,
                              contentPadding:
                                  const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                              border: inputBorder,
                              enabledBorder: inputBorder,
                              focusedBorder: inputBorder.copyWith(
                                  borderSide: const BorderSide(color: Colors.black)),
                              suffixIcon: Padding(
                                padding: const EdgeInsets.only(right: 6),
                                child: TextButton(
                                  onPressed: () => setState(() => _showPw = !_showPw),
                                  style: TextButton.styleFrom(
                                    padding: const EdgeInsets.symmetric(horizontal: 10),
                                    minimumSize: const Size(0, 28),
                                    side: const BorderSide(color: inputBd),
                                    backgroundColor: Colors.white,
                                    shape: RoundedRectangleBorder(
                                        borderRadius: BorderRadius.circular(8)),
                                    foregroundColor: textColor,
                                  ),
                                  child: Text(_showPw ? '숨기기' : '표시',
                                      style: const TextStyle(fontSize: 12)),
                                ),
                              ),
                              suffixIconConstraints:
                                  const BoxConstraints(minWidth: 0, minHeight: 0),
                            ),
                          ),
                          const SizedBox(height: 14),

                          const Text('새 비밀번호 (8자 이상)',
                              style: TextStyle(fontSize: 14, color: textColor)),
                          const SizedBox(height: 8),
                          TextField(
                            controller: _pw2Ctrl,
                            obscureText: !_showPw2,
                            decoration: InputDecoration(
                              hintText: '다시 입력',
                              isDense: true,
                              contentPadding:
                                  const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                              border: inputBorder,
                              enabledBorder: inputBorder,
                              focusedBorder: inputBorder.copyWith(
                                  borderSide: const BorderSide(color: Colors.black)),
                              suffixIcon: Padding(
                                padding: const EdgeInsets.only(right: 6),
                                child: TextButton(
                                  onPressed: () => setState(() => _showPw2 = !_showPw2),
                                  style: TextButton.styleFrom(
                                    padding: const EdgeInsets.symmetric(horizontal: 10),
                                    minimumSize: const Size(0, 28),
                                    side: const BorderSide(color: inputBd),
                                    backgroundColor: Colors.white,
                                    shape: RoundedRectangleBorder(
                                        borderRadius: BorderRadius.circular(8)),
                                    foregroundColor: textColor,
                                  ),
                                  child: Text(_showPw2 ? '숨기기' : '표시',
                                      style: const TextStyle(fontSize: 12)),
                                ),
                              ),
                              suffixIconConstraints:
                                  const BoxConstraints(minWidth: 0, minHeight: 0),
                            ),
                          ),
                          const SizedBox(height: 14),

                          _primaryBtn(
                            label: '비밀번호 변경',
                            onTap: _loading ? null : _handleReset,
                          ),
                        ],
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
