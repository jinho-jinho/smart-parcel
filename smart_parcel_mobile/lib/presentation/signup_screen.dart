import 'dart:async';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import '../data/api/user_api.dart' show signup;
import '../data/api/auth_api.dart' show sendCode, verifyCode;

class SignUpScreen extends StatefulWidget {
  const SignUpScreen({super.key});
  @override
  State<SignUpScreen> createState() => _SignUpScreenState();
}

class _SignUpScreenState extends State<SignUpScreen> {
  // 입력 컨트롤러
  final _nameCtrl = TextEditingController();
  final _emailCtrl = TextEditingController();
  final _pwCtrl = TextEditingController();
  final _confirmCtrl = TextEditingController();
  final _managerCtrl = TextEditingController();
  final _bizCtrl = TextEditingController(); // 필요 시 활성화

  // 상태
  bool _showPw = false;
  bool _showConfirm = false;
  String _role = 'STAFF'; // MANAGER | STAFF
  String? _msg;
  bool _loading = false;

  // 이메일 인증
  bool _codeBoxVisible = false;
  final _codeCtrl = TextEditingController();
  bool _emailVerified = false;
  bool _sending = false;
  bool _verifying = false;
  int _remainSec = 0;
  Timer? _timer;

  final _emailRe = RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$');
  bool get _isStaff => _role == 'STAFF';
  bool get _isManager => _role == 'MANAGER';

  @override
  void dispose() {
    _timer?.cancel();
    _nameCtrl.dispose();
    _emailCtrl.dispose();
    _pwCtrl.dispose();
    _confirmCtrl.dispose();
    _managerCtrl.dispose();
    _codeCtrl.dispose();
    _bizCtrl.dispose();
    super.dispose();
  }

  void _startCountdown([int sec = 300]) {
    _timer?.cancel();
    setState(() => _remainSec = sec);
    _timer = Timer.periodic(const Duration(seconds: 1), (t) {
      if (_remainSec <= 0) {
        t.cancel();
        setState(() {});
      } else {
        setState(() => _remainSec--);
      }
    });
  }

  String _fmt(int s) => '${(s ~/ 60).toString().padLeft(2, '0')}:${(s % 60).toString().padLeft(2, '0')}';

  Future<void> _handleSendCode() async {
    setState(() {
      _msg = null;
      _emailVerified = false;
      _codeCtrl.clear();
    });
    final email = _emailCtrl.text.trim();
    if (!_emailRe.hasMatch(email)) {
      setState(() => _msg = '올바른 이메일을 입력하세요.');
      return;
    }
    setState(() => _sending = true);
    try {
      final res = await sendCode(email: email, purpose: 'SIGNUP');
      setState(() {
        _codeBoxVisible = true;
        _msg = (res['message'] as String?) ?? '인증번호가 전송되었습니다.';
      });
      _startCountdown(300);
    } on DioException catch (e) {
      setState(() => _msg = (e.response?.data?['message'] as String?) ?? '인증번호 전송 실패');
    } catch (_) {
      setState(() => _msg = '인증번호 전송 실패');
    } finally {
      setState(() => _sending = false);
    }
  }

  Future<void> _handleVerifyCode() async {
    setState(() => _msg = null);
    final email = _emailCtrl.text.trim();
    final code = _codeCtrl.text.trim();
    if (!_emailRe.hasMatch(email) || code.isEmpty) {
      setState(() => _msg = '이메일과 인증번호를 확인하세요.');
      return;
    }
    if (_remainSec == 0) {
      setState(() => _msg = '인증 시간이 만료되었습니다. 다시 전송해주세요.');
      return;
    }
    setState(() => _verifying = true);
    try {
      final res = await verifyCode(email: email, code: code, purpose: 'SIGNUP');
      _timer?.cancel();
      setState(() {
        _emailVerified = true;
        _remainSec = 0;
        _msg = (res['message'] as String?) ?? '이메일 인증 성공';
      });
    } on DioException catch (e) {
      setState(() => _msg = (e.response?.data?['message'] as String?) ?? '이메일 인증 실패');
    } catch (_) {
      setState(() => _msg = '이메일 인증 실패');
    } finally {
      setState(() => _verifying = false);
    }
  }

  Future<void> _handleSubmit() async {
    setState(() => _msg = null);

    final name = _nameCtrl.text.trim();
    final email = _emailCtrl.text.trim();
    final pw = _pwCtrl.text;
    final confirm = _confirmCtrl.text;
    final managerEmail = _managerCtrl.text.trim();
    final biz = _bizCtrl.text.trim();

    if (name.isEmpty) return setState(() => _msg = '이름을 입력해주세요.');
    if (!_emailRe.hasMatch(email)) return setState(() => _msg = '올바른 이메일을 입력하세요.');
    if (!_emailVerified) return setState(() => _msg = '이메일 인증을 완료해주세요.');
    if (pw.length < 8) return setState(() => _msg = '비밀번호는 최소 8자 이상이어야 합니다.');
    if (pw != confirm) return setState(() => _msg = '비밀번호가 일치하지 않습니다.');
    if (_isStaff) {
      if (managerEmail.isEmpty) {
        return setState(() => _msg = '소속 관리자 이메일을 입력해 주세요.');
      }
      if (!_emailRe.hasMatch(managerEmail)) {
        return setState(() => _msg = '소속 관리자 이메일 형식을 확인해 주세요.');
      }
    }

    setState(() => _loading = true);
    try {
      final res = await signup(
        email: email,
        password: pw,
        name: name,
        bizNumber: _isManager ? biz : null,
        role: _role,
        managerEmail: _isStaff ? managerEmail : null,
      );
      // 성공 메시지
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text((res.data?['message'] as String?) ?? '회원가입 성공')),
      );
      if (mounted) Navigator.of(context).pop(); // 로그인 화면으로 복귀
    } on DioException catch (e) {
      setState(() => _msg = (e.response?.data?['message'] as String?) ?? '회원가입에 실패했습니다.');
    } catch (_) {
      setState(() => _msg = '회원가입에 실패했습니다.');
    } finally {
      setState(() => _loading = false);
    }
  }

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
      BoxShadow(color: Colors.black.withOpacity(.06), blurRadius: 18, offset: const Offset(0, 6)),
    ];

    final inputBorder = OutlineInputBorder(
      borderRadius: BorderRadius.circular(radiusLg),
      borderSide: const BorderSide(color: inputBd, width: 1),
    );
    final managerEmail = _managerCtrl.text.trim();
    final managerValid = !_isStaff || _emailRe.hasMatch(managerEmail);
    final passwordsMatch = _pwCtrl.text.isNotEmpty && _pwCtrl.text == _confirmCtrl.text;
    final canSubmit =
        !_loading && _emailVerified && _pwCtrl.text.length >= 8 && passwordsMatch && managerValid;

    Widget label(String t) => Text(t, style: const TextStyle(fontSize: 14, color: textColor));

    return Scaffold(
      backgroundColor: bg,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 720),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  // 헤더
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
                    width: 560, // 모바일에선 화면 폭에 맞게 줄어듦
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
                            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                            decoration: BoxDecoration(
                              color: const Color(0xFFFEF3F2),
                              border: Border.all(color: const Color(0xFFFECACA)),
                              borderRadius: BorderRadius.circular(10),
                            ),
                            child: Text(_msg!, style: const TextStyle(color: Color(0xFF7F1D1D), fontSize: 14)),
                          ),
                          const SizedBox(height: 12),
                        ],

                        // 이름
                        label('이름'),
                        const SizedBox(height: 8),
                        TextField(
                          controller: _nameCtrl,
                          decoration: InputDecoration(
                            hintText: '홍길동',
                            isDense: true,
                            contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                            border: inputBorder, enabledBorder: inputBorder,
                            focusedBorder: inputBorder.copyWith(
                              borderSide: const BorderSide(color: Colors.black),
                            ),
                          ),
                        ),
                        const SizedBox(height: 14),

                        // 이메일 + 인증 전송
                        label('이메일'),
                        const SizedBox(height: 8),
                        Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Expanded(
                              child: TextField(
                                controller: _emailCtrl,
                                keyboardType: TextInputType.emailAddress,
                                onChanged: (_) {
                                  setState(() {
                                    _emailVerified = false;
                                    _codeBoxVisible = false;
                                    _remainSec = 0;
                                    _codeCtrl.clear();
                                  });
                                },
                                decoration: InputDecoration(
                                  hintText: 'name@example.com',
                                  isDense: true,
                                  contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                                  border: inputBorder, enabledBorder: inputBorder,
                                  focusedBorder: inputBorder.copyWith(
                                    borderSide: const BorderSide(color: Colors.black),
                                  ),
                                ),
                              ),
                            ),
                            const SizedBox(width: 8),
                            SizedBox(
                              width: 76,
                              height: 44,
                              child: OutlinedButton(
                                onPressed: _sending ? null : _handleSendCode,
                                style: OutlinedButton.styleFrom(
                                  side: const BorderSide(color: Color(0xFF757575)),
                                  foregroundColor: const Color(0xFF49454F),
                                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                                ),
                                child: Text(_sending ? '전송중' : '인증', style: const TextStyle(fontSize: 14)),
                              ),
                            ),
                          ],
                        ),

                        if (_codeBoxVisible) ...[
                          const SizedBox(height: 8),
                          Row(
                            children: [
                              Expanded(
                                child: TextField(
                                  controller: _codeCtrl,
                                  decoration: InputDecoration(
                                    hintText: '인증번호 입력',
                                    isDense: true,
                                    contentPadding:
                                        const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                                    border: inputBorder, enabledBorder: inputBorder,
                                    focusedBorder: inputBorder.copyWith(
                                      borderSide: const BorderSide(color: Colors.black),
                                    ),
                                  ),
                                  keyboardType: TextInputType.number,
                                  enabled: !_emailVerified,
                                ),
                              ),
                              const SizedBox(width: 8),
                              SizedBox(
                                width: 76,
                                height: 44,
                                child: OutlinedButton(
                                  onPressed: (!_emailRe.hasMatch(_emailCtrl.text.trim()) ||
                                          _codeCtrl.text.trim().isEmpty ||
                                          _remainSec == 0 ||
                                          _verifying)
                                      ? null
                                      : _handleVerifyCode,
                                  style: OutlinedButton.styleFrom(
                                    side: const BorderSide(color: Color(0xFF757575)),
                                    foregroundColor: const Color(0xFF49454F),
                                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                                  ),
                                  child: Text(_verifying ? '확인중' : '확인', style: const TextStyle(fontSize: 14)),
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 4),
                          Text(
                            _remainSec > 0 ? '남은 시간 ${_fmt(_remainSec)}' : '인증 시간이 만료되었습니다.',
                            style: const TextStyle(fontSize: 12, color: muted),
                          ),
                          if (_emailVerified)
                            const Padding(
                              padding: EdgeInsets.only(top: 4),
                              child: Text('이메일 인증 완료', style: TextStyle(fontSize: 12, color: Color(0xFF166534))),
                            ),
                        ],

                        const SizedBox(height: 14),

                        // 비밀번호
                        label('비밀번호'),
                        const SizedBox(height: 8),
                        TextField(
                          controller: _pwCtrl,
                          onChanged: (_) => setState(() {}),
                          obscureText: !_showPw,
                          decoration: InputDecoration(
                            hintText: '최소 8자',
                            isDense: true,
                            contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                            border: inputBorder, enabledBorder: inputBorder,
                            focusedBorder: inputBorder.copyWith(
                              borderSide: const BorderSide(color: Colors.black),
                            ),
                            suffixIcon: Padding(
                              padding: const EdgeInsets.only(right: 6),
                              child: TextButton(
                                onPressed: () => setState(() => _showPw = !_showPw),
                                style: TextButton.styleFrom(
                                  padding: const EdgeInsets.symmetric(horizontal: 10),
                                  minimumSize: const Size(0, 28),
                                  side: const BorderSide(color: inputBd),
                                  backgroundColor: Colors.white,
                                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                                  foregroundColor: textColor,
                                ),
                                child: Text(_showPw ? '숨기기' : '표시', style: const TextStyle(fontSize: 12)),
                              ),
                            ),
                            suffixIconConstraints: const BoxConstraints(minWidth: 0, minHeight: 0),
                          ),
                        ),
                        const SizedBox(height: 14),

                        // 비밀번호 확인
                        label('비밀번호 확인'),
                        const SizedBox(height: 8),
                        TextField(
                          controller: _confirmCtrl,
                          onChanged: (_) => setState(() {}),
                          obscureText: !_showConfirm,
                          decoration: InputDecoration(
                            hintText: '비밀번호 확인',
                            isDense: true,
                            contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                            border: inputBorder, enabledBorder: inputBorder,
                            focusedBorder: inputBorder.copyWith(
                              borderSide: const BorderSide(color: Colors.black),
                            ),
                            suffixIcon: Padding(
                              padding: const EdgeInsets.only(right: 6),
                              child: TextButton(
                                onPressed: () => setState(() => _showConfirm = !_showConfirm),
                                style: TextButton.styleFrom(
                                  padding: const EdgeInsets.symmetric(horizontal: 10),
                                  minimumSize: const Size(0, 28),
                                  side: const BorderSide(color: inputBd),
                                  backgroundColor: Colors.white,
                                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                                  foregroundColor: textColor,
                                ),
                                child: Text(_showConfirm ? '숨기기' : '표시', style: const TextStyle(fontSize: 12)),
                              ),
                            ),
                            suffixIconConstraints: const BoxConstraints(minWidth: 0, minHeight: 0),
                          ),
                        ),
                        const SizedBox(height: 14),

                        // 역할 선택 (반반)
                        Row(
                          children: [
                            Expanded(child: _RoleBtn(label: '관리자', active: _role == 'MANAGER', onTap: () => setState(() => _role = 'MANAGER'))),
                            const SizedBox(width: 8),
                            Expanded(child: _RoleBtn(label: '직원', active: _role == 'STAFF', onTap: () => setState(() => _role = 'STAFF'))),
                          ],
                        ),
                        const SizedBox(height: 14),

                        // 추가 정보 (역할별)
                        if (_isStaff) ...[
                          label('소속 관리자 등록'),
                          const SizedBox(height: 8),
                          TextField(
                            controller: _managerCtrl,
                            keyboardType: TextInputType.emailAddress,
                            onChanged: (_) => setState(() {}),
                            decoration: InputDecoration(
                              hintText: '관리자 이메일을 입력해 주세요',
                              isDense: true,
                              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                              border: inputBorder,
                              enabledBorder: inputBorder,
                              focusedBorder: inputBorder.copyWith(
                                borderSide: const BorderSide(color: Colors.black),
                              ),
                              suffixIcon: const Icon(Icons.search, color: muted),
                            ),
                          ),
                          if (managerEmail.isNotEmpty && !managerValid)
                            const Padding(
                              padding: EdgeInsets.only(top: 6),
                              child: Text(
                                '이메일 형식을 확인해 주세요.',
                                style: TextStyle(fontSize: 12, color: Colors.redAccent),
                              ),
                            ),
                        ] else ...[
                          label('사업자등록번호 (선택)'),
                          const SizedBox(height: 8),
                          TextField(
                            controller: _bizCtrl,
                            keyboardType: TextInputType.number,
                            onChanged: (_) => setState(() {}),
                            decoration: InputDecoration(
                              hintText: '숫자만 입력 (예: 1234567890)',
                              isDense: true,
                              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                              border: inputBorder,
                              enabledBorder: inputBorder,
                              focusedBorder: inputBorder.copyWith(
                                borderSide: const BorderSide(color: Colors.black),
                              ),
                            ),
                          ),
                        ],
                        const SizedBox(height: 14),

                        // 회원가입 버튼
                        SizedBox(
                          height: 44,
                          child: FilledButton(
                            onPressed: canSubmit ? _handleSubmit : null,
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
                                    height: 20, width: 20,
                                    child: CircularProgressIndicator(strokeWidth: 2, color: primaryContr),
                                  )
                                : const Text('회원가입', style: TextStyle(fontSize: 15)),
                          ),
                        ),
                        const SizedBox(height: 8),

                        // 하단 링크
                        Wrap(
                          alignment: WrapAlignment.center,
                          crossAxisAlignment: WrapCrossAlignment.center,
                          spacing: 6,
                          runSpacing: 4,
                          children: [
                            const Text('이미 계정이 있으신가요?', style: TextStyle(fontSize: 12, color: muted)),
                            TextButton(
                              onPressed: () => Navigator.of(context).pop(),
                              style: TextButton.styleFrom(
                                padding: EdgeInsets.zero,
                                minimumSize: const Size(0, 0),
                                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                                foregroundColor: textColor,
                                textStyle: const TextStyle(
                                  decoration: TextDecoration.underline,
                                  fontSize: 12,
                                ),
                              ),
                              child: const Text('로그인 화면으로 돌아가기'),
                            ),
                          ],
                        ),
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

class _RoleBtn extends StatelessWidget {
  final String label;
  final bool active;
  final VoidCallback onTap;
  const _RoleBtn({required this.label, required this.active, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 44,
      child: OutlinedButton(
        onPressed: onTap,
        style: OutlinedButton.styleFrom(
          backgroundColor: active ? const Color(0xFF2C2C2C) : Colors.white,
          foregroundColor: active ? const Color(0xFFF5F5F5) : const Color(0xFF49454F),
          side: BorderSide(color: active ? const Color(0xFF2C2C2C) : const Color(0xFF757575)),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ),
        child: Text(label, style: const TextStyle(fontSize: 14)),
      ),
    );
  }
}
