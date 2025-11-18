import 'package:flutter/material.dart';

import './core/colors.dart';
import '../core/session/session_manager.dart';
import '../data/api/chute_api.dart';
import '../data/dto/chute_dto.dart';
import 'widgets/read_only_banner.dart';

class SortingLinesScreen extends StatefulWidget {
  const SortingLinesScreen({super.key});

  @override
  State<SortingLinesScreen> createState() => _SortingLinesScreenState();
}

class _SortingLinesScreenState extends State<SortingLinesScreen> {
  final List<ChuteDto> _chutes = [];
  bool _loading = true;
  String? _error;
  bool? _isManager;

  bool get _canManage => _isManager == true;

  @override
  void initState() {
    super.initState();
    _resolvePermissions();
    _load();
  }

  void _resolvePermissions() {
    SessionManager.instance.isManager().then((value) {
      if (!mounted) return;
      setState(() => _isManager = value);
    });
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final page = await fetchChutes(size: 100);
      setState(() {
        _chutes
          ..clear()
          ..addAll(page.content);
      });
    } catch (e) {
      setState(() => _error = '분류 라인을 불러오지 못했습니다.');
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _showChuteDialog({ChuteDto? chute}) async {
    if (!_canManage) return;
    final nameCtrl = TextEditingController(text: chute?.name ?? '');
    final angleCtrl = TextEditingController(
      text: chute != null ? chute.servoDeg.toString() : '',
    );
    final result = await showDialog<bool>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text(chute == null ? '새 분류 라인' : '분류 라인 수정'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: nameCtrl,
                decoration: const InputDecoration(labelText: '라인 이름'),
              ),
              TextField(
                controller: angleCtrl,
                decoration: const InputDecoration(labelText: '서보 각도 (0-180)'),
                keyboardType: TextInputType.number,
              ),
            ],
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('취소')),
            FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('저장')),
          ],
        );
      },
    );
    if (result != true) return;
    final angle = int.tryParse(angleCtrl.text) ?? 0;

    try {
      if (chute == null) {
        final created = await createChute(name: nameCtrl.text, servoDeg: angle);
        setState(() => _chutes.insert(0, created));
      } else {
        final updated =
            await updateChute(chuteId: chute.id, name: nameCtrl.text, servoDeg: angle);
        setState(() {
          final idx = _chutes.indexWhere((c) => c.id == chute.id);
          if (idx >= 0) _chutes[idx] = updated;
        });
      }
    } catch (e) {
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('저장에 실패했습니다.')));
    }
  }

  Future<void> _deleteChute(ChuteDto chute) async {
    if (!_canManage) return;
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('삭제 확인'),
        content: Text('"${chute.name}" 라인을 삭제할까요?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('취소')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('삭제')),
        ],
      ),
    );
    if (confirm != true) return;
    try {
      await deleteChute(chute.id);
      setState(() => _chutes.removeWhere((c) => c.id == chute.id));
    } catch (e) {
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('삭제에 실패했습니다.')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bg,
      appBar: AppBar(
        title: const Text('분류 라인'),
        backgroundColor: Colors.white,
        elevation: 0,
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!, style: const TextStyle(color: Colors.redAccent)))
              : Column(
                  children: [
                    if (_isManager == false)
                      const ReadOnlyBanner(
                        message: '직원 계정은 분류 라인을 조회만 할 수 있습니다.',
                      ),
                    Expanded(
                      child: ListView.builder(
                        padding: const EdgeInsets.all(20),
                        itemCount: _chutes.length,
                        itemBuilder: (context, index) {
                          final chute = _chutes[index];
                          return Container(
                            margin: const EdgeInsets.only(bottom: 16),
                            padding: const EdgeInsets.all(16),
                            decoration: BoxDecoration(
                              color: Colors.white,
                              borderRadius: BorderRadius.circular(16),
                              border: Border.all(color: const Color(0xFFE6E6E6)),
                            ),
                            child: Row(
                              children: [
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(chute.name,
                                          style: const TextStyle(
                                              fontSize: 16, fontWeight: FontWeight.w700)),
                                      const SizedBox(height: 4),
                                      Text('각도: ${chute.servoDeg}°',
                                          style: const TextStyle(color: AppColors.muted)),
                                    ],
                                  ),
                                ),
                                if (_canManage)
                                  PopupMenuButton<String>(
                                    onSelected: (value) {
                                      if (value == 'edit') {
                                        _showChuteDialog(chute: chute);
                                      } else if (value == 'delete') {
                                        _deleteChute(chute);
                                      }
                                    },
                                    itemBuilder: (context) => const [
                                      PopupMenuItem(value: 'edit', child: Text('수정')),
                                      PopupMenuItem(
                                        value: 'delete',
                                        child: Text('삭제', style: TextStyle(color: Colors.redAccent)),
                                      ),
                                    ],
                                  ),
                              ],
                            ),
                          );
                        },
                      ),
                    ),
                  ],
                ),
      floatingActionButton: _canManage
          ? FloatingActionButton(
              onPressed: _showChuteDialog,
              child: const Icon(Icons.add),
            )
          : null,
    );
  }
}
