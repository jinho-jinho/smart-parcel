import 'dart:convert';
import 'package:flutter/material.dart';
import './core/colors.dart'; 
import '../data/api/user_api.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  // 입력
  final _emailCtrl = TextEditingController();
  final _pwCtrl = TextEditingController();

  // 상태
  bool _showPw = false;
  bool _loading = false;
  String? _msg; // 상단 에러/안내

  final _emailRegex = RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$');

  @override
  void dispose() {
    _emailCtrl.dispose();
    _pwCtrl.dispose();
    super.dispose();
  }

  Future<void> _onSubmit() async {
    setState(() => _msg = null);

    final email = _emailCtrl.text.trim();
    final pw = _pwCtrl.text;

    if (!_emailRegex.hasMatch(email)) {
      setState(() => _msg = '올바른 이메일을 입력하세요.');
      return;
    }
    if (pw.isEmpty) {
      setState(() => _msg = '비밀번호를 입력해주세요.');
      return;
    }

    setState(() => _loading = true);
    try {
      final res = await login(email: email, password: pw); // /user/login
      // 선택: /user/me 호출
      // final me = await fetchMe();
      // print(jsonEncode(me));

      if (mounted) {
        Navigator.of(context).pushReplacementNamed('/groups');
      }
    } catch (e) {
      // 서버 ApiResponse 포맷 가정
      String msg = '로그인에 실패했습니다.';
      try {
        final map = jsonDecode(e.toString()) as Map<String, dynamic>;
        msg = map['message'] ?? msg;
      } catch (_) {}
      setState(() => _msg = msg);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    // CSS 변수와 매핑된 색/스타일
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
      borderSide: const BorderSide(color: inputBd, width: 1),
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
                  // ===== 헤더 (로고 + 브랜드) =====
                  Column(
                    children: [
                      Image.asset(
                        'assets/icon.png',
                        width: 160, // clamp(120~160) 느낌
                        fit: BoxFit.contain,
                      ),
                      const SizedBox(height: 10),
                      const Text(
                        'Smart Parcel',
                        style: TextStyle(
                          fontWeight: FontWeight.w800,
                          fontSize: 32, // clamp 적용 느낌
                          letterSpacing: -0.02,
                          color: textColor,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 24),

                  // ===== 카드 =====
                  Container(
                    width: 360, // clamp(320~360) 느낌
                    padding: const EdgeInsets.symmetric(
                      horizontal: 22,
                      vertical: 18,
                    ),
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
                            padding: const EdgeInsets.symmetric(
                                horizontal: 12, vertical: 10),
                            child: Text(
                              _msg!,
                              style: const TextStyle(
                                color: Color(0xFF7F1D1D),
                                fontSize: 14,
                              ),
                            ),
                          ),
                          const SizedBox(height: 12),
                        ],

                        // 이메일
                        const Text('이메일',
                            style: TextStyle(fontSize: 14, color: textColor)),
                        const SizedBox(height: 8),
                        TextField(
                          controller: _emailCtrl,
                          keyboardType: TextInputType.emailAddress,
                          decoration: InputDecoration(
                            hintText: 'name@example.com',
                            hintStyle: const TextStyle(color: Color(0xFFB3B3B3)),
                            filled: true,
                            fillColor: inputBg,
                            isDense: true,
                            contentPadding: const EdgeInsets.symmetric(
                                horizontal: 16, vertical: 12),
                            border: inputBorder,
                            enabledBorder: inputBorder,
                            focusedBorder: inputBorder.copyWith(
                              borderSide:
                                  const BorderSide(color: Colors.black, width: 1),
                            ),
                          ),
                        ),
                        const SizedBox(height: 14),

                        // 비밀번호 (표시/숨기기)
                        const Text('비밀번호',
                            style: TextStyle(fontSize: 14, color: textColor)),
                        const SizedBox(height: 8),
                        TextField(
                          controller: _pwCtrl,
                          obscureText: !_showPw,
                          decoration: InputDecoration(
                            hintText: '비밀번호',
                            hintStyle: const TextStyle(color: Color(0xFFB3B3B3)),
                            filled: true,
                            fillColor: inputBg,
                            isDense: true,
                            contentPadding: const EdgeInsets.symmetric(
                                horizontal: 16, vertical: 12),
                            border: inputBorder,
                            enabledBorder: inputBorder,
                            focusedBorder: inputBorder.copyWith(
                              borderSide:
                                  const BorderSide(color: Colors.black, width: 1),
                            ),
                            suffixIcon: Padding(
                              padding: const EdgeInsets.only(right: 6),
                              child: TextButton(
                                style: TextButton.styleFrom(
                                  padding:
                                      const EdgeInsets.symmetric(horizontal: 10),
                                  minimumSize: const Size(0, 28),
                                  visualDensity: VisualDensity.compact,
                                  side: const BorderSide(color: inputBd),
                                  backgroundColor: Colors.white,
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  foregroundColor: textColor,
                                ),
                                onPressed: () =>
                                    setState(() => _showPw = !_showPw),
                                child: Text(_showPw ? '숨기기' : '표시',
                                    style: const TextStyle(fontSize: 12)),
                              ),
                            ),
                            suffixIconConstraints:
                                const BoxConstraints(minWidth: 0, minHeight: 0),
                          ),
                        ),
                        const SizedBox(height: 14),

                        // 로그인 버튼
                        SizedBox(
                          height: 44,
                          child: FilledButton(
                            onPressed: _loading ? null : _onSubmit,
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
                                    child: CircularProgressIndicator(
                                      strokeWidth: 2,
                                      color: primaryContr,
                                    ),
                                  )
                                : const Text('로그인',
                                    style: TextStyle(fontSize: 15)),
                          ),
                        ),
                        const SizedBox(height: 8),

                        // 링크
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            TextButton(
                              style: TextButton.styleFrom(
                                foregroundColor: textColor,
                                textStyle: const TextStyle(
                                  decoration: TextDecoration.underline,
                                  fontSize: 12,
                                ),
                              ),
                              onPressed: () {
                                Navigator.pushNamed(context, '/signup');
                              },
                              child: const Text('회원가입'),
                            ),
                            TextButton(
                              style: TextButton.styleFrom(
                                foregroundColor: textColor,
                                textStyle: const TextStyle(
                                  decoration: TextDecoration.underline,
                                  fontSize: 12,
                                ),
                              ),
                              onPressed: () {
                                Navigator.pushNamed(context, '/reset-password');
                              },
                              child: const Text('비밀번호 찾기'),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: 8),
                  const Text('© 2025 SmartParcel',
                      style: TextStyle(fontSize: 12, color: muted)),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
