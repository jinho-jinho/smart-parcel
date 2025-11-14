import 'package:flutter/material.dart';

import './core/colors.dart';
import '../data/api/sorting_groups_api.dart';
import '../data/dto/sorting_group_dto.dart';
import 'sorting_rules_screen.dart';
import 'widgets/app_shell.dart';

class SortingGroupsScreen extends StatefulWidget {
  const SortingGroupsScreen({super.key});

  @override
  State<SortingGroupsScreen> createState() => _SortingGroupsScreenState();
}

class _SortingGroupsScreenState extends State<SortingGroupsScreen> {
  final List<SortingGroupDto> _groups = [];
  bool _loading = true;
  bool _refreshing = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadGroups();
  }

  Future<void> _loadGroups() async {
    setState(() {
      _error = null;
      _loading = !_refreshing;
    });
    try {
      final page = await fetchSortingGroups(size: 50);
      setState(() {
        _groups
          ..clear()
          ..addAll(page.content);
      });
    } catch (e) {
      setState(() => _error = '분류 그룹을 불러오지 못했습니다.');
    } finally {
      setState(() {
        _loading = false;
        _refreshing = false;
      });
    }
  }

  Future<void> _showGroupDialog({SortingGroupDto? group}) async {
    final controller = TextEditingController(text: group?.name ?? '');
    final result = await showDialog<String>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: Text(group == null ? '새 분류그룹 추가' : '분류그룹 수정'),
          content: TextField(
            controller: controller,
            autofocus: true,
            decoration: const InputDecoration(hintText: '예) 방탄헬멧 (A-01)'),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('취소'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(context, controller.text.trim()),
              child: const Text('확인'),
            ),
          ],
        );
      },
    );
    if (result == null || result.isEmpty) return;
    try {
      if (group == null) {
        final created = await createSortingGroup(result);
        setState(() => _groups.insert(0, created));
      } else {
        final updated = await updateSortingGroup(group.id, result);
        setState(() {
          final idx = _groups.indexWhere((g) => g.id == group.id);
          if (idx >= 0) _groups[idx] = updated;
        });
      }
    } catch (e) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('저장 중 오류가 발생했습니다.')));
    }
  }

  Future<void> _toggleGroup(SortingGroupDto group) async {
    final enable = !group.enabled;
    try {
      await toggleSortingGroup(group.id, enable);
      setState(() {
        final idx = _groups.indexWhere((g) => g.id == group.id);
        if (idx >= 0) {
          _groups[idx] = _groups[idx].copyWith(enabled: enable);
        }
      });
      _refreshing = true;
      _loadGroups();
    } catch (e) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(enable ? '활성화 실패' : '비활성화 실패')));
    }
  }

  Future<void> _deleteGroup(SortingGroupDto group) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder:
          (context) => AlertDialog(
            title: const Text('분류그룹 삭제'),
            content: Text('"${group.name}" 그룹을 삭제할까요?'),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context, false),
                child: const Text('취소'),
              ),
              FilledButton(
                onPressed: () => Navigator.pop(context, true),
                child: const Text('삭제'),
              ),
            ],
          ),
    );
    if (confirmed != true) return;
    try {
      await deleteSortingGroup(group.id);
      setState(() => _groups.removeWhere((g) => g.id == group.id));
    } catch (e) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('삭제에 실패했습니다.')));
    }
  }

  void _openRules(SortingGroupDto group) {
    Navigator.of(context)
        .push(
          MaterialPageRoute(builder: (_) => SortingRulesScreen(group: group)),
        )
        .then((_) {
          _refreshing = true;
          _loadGroups();
        });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bg,
      drawer: const AppMenuDrawer(),
      appBar: SmartParcelAppBar(
        actions: [
          IconButton(
            tooltip: '새로고침',
            onPressed: () {
              _refreshing = true;
              _loadGroups();
            },
            icon: const Icon(Icons.refresh, color: Colors.black),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () {
          _refreshing = true;
          return _loadGroups();
        },
        child:
            _loading
                ? const Center(child: CircularProgressIndicator())
                : _error != null
                ? ListView(
                  children: [
                    Padding(
                      padding: const EdgeInsets.all(24),
                      child: Text(
                        _error!,
                        style: const TextStyle(color: Colors.redAccent),
                      ),
                    ),
                  ],
                )
                : ListView(
                  padding: const EdgeInsets.all(20),
                  children: [
                    _header(context),
                    const SizedBox(height: 16),
                    ..._groups.map(
                      (group) => _GroupCard(
                        group: group,
                        onView: () => _openRules(group),
                        onToggle: () => _toggleGroup(group),
                        onRename: () => _showGroupDialog(group: group),
                        onDelete: () => _deleteGroup(group),
                      ),
                    ),
                    if (_groups.isEmpty)
                      Container(
                        padding: const EdgeInsets.symmetric(vertical: 60),
                        alignment: Alignment.center,
                        child: const Text('등록된 분류그룹이 없습니다.'),
                      ),
                  ],
                ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _showGroupDialog,
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _header(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: const [
              Text(
                '분류 그룹',
                style: TextStyle(fontSize: 24, fontWeight: FontWeight.w700),
              ),
              SizedBox(height: 6),
              Text(
                '장비 상태와 처리 건수를 실시간으로 확인하세요.',
                style: TextStyle(color: AppColors.muted),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _GroupCard extends StatelessWidget {
  const _GroupCard({
    required this.group,
    required this.onView,
    required this.onToggle,
    required this.onRename,
    required this.onDelete,
  });

  final SortingGroupDto group;
  final VoidCallback onView;
  final VoidCallback onToggle;
  final VoidCallback onRename;
  final VoidCallback onDelete;

  Color get statusColor => group.enabled ? Colors.green : Colors.grey;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFE8E8E8)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(.04),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 10,
                height: 10,
                decoration: BoxDecoration(
                  color: statusColor,
                  shape: BoxShape.circle,
                ),
              ),
              const SizedBox(width: 8),
              Text(
                group.enabled ? '활성' : '비활성',
                style: const TextStyle(fontWeight: FontWeight.w600),
              ),
              const Spacer(),
              PopupMenuButton<String>(
                onSelected: (value) {
                  if (value == 'view') {
                    onView();
                  } else if (value == 'toggle') {
                    onToggle();
                  } else if (value == 'rename') {
                    onRename();
                  } else if (value == 'delete') {
                    onDelete();
                  }
                },
                itemBuilder:
                    (context) => [
                      const PopupMenuItem(
                        value: 'view',
                        child: Text('분류 기준 보기'),
                      ),
                      PopupMenuItem(
                        value: 'toggle',
                        child: Text(group.enabled ? '비활성화' : '활성화'),
                      ),
                      const PopupMenuItem(
                        value: 'rename',
                        child: Text('이름 수정'),
                      ),
                      const PopupMenuItem(
                        value: 'delete',
                        child: Text(
                          '삭제',
                          style: TextStyle(color: Colors.redAccent),
                        ),
                      ),
                    ],
              ),
            ],
          ),
          const SizedBox(height: 12),
          GestureDetector(
            onTap: onView,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  group.name,
                  style: const TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.w800,
                    color: AppColors.text,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  '현재 처리 건수 : ${group.processingCount}건',
                  style: const TextStyle(color: AppColors.muted),
                ),
                const SizedBox(height: 6),
                Text(
                  '마지막 업데이트: ${_formatDate(group.updatedAt)}',
                  style: const TextStyle(color: AppColors.muted, fontSize: 13),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  static String _formatDate(DateTime? date) {
    if (date == null) return '-';
    return '${date.year}-${_two(date.month)}-${_two(date.day)} ${_two(date.hour)}:${_two(date.minute)}';
  }

  static String _two(int v) => v.toString().padLeft(2, '0');
}
