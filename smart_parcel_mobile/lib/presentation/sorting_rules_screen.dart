import 'package:flutter/material.dart';

import './core/colors.dart';
import '../data/api/chute_api.dart';
import '../data/api/sorting_rules_api.dart';
import '../data/dto/chute_dto.dart';
import '../data/dto/sorting_group_dto.dart';
import '../data/dto/sorting_rule_dto.dart';
import 'sorting_lines_screen.dart';

const _inputTypes = [
  {'label': '텍스트', 'value': 'TEXT'},
  {'label': '색상', 'value': 'COLOR'},
];

class SortingRulesScreen extends StatefulWidget {
  const SortingRulesScreen({super.key, required this.group});

  final SortingGroupDto group;

  @override
  State<SortingRulesScreen> createState() => _SortingRulesScreenState();
}

class _SortingRulesScreenState extends State<SortingRulesScreen> {
  final List<SortingRuleDto> _rules = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadRules();
  }

  Future<void> _loadRules() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final page = await fetchSortingRules(widget.group.id, size: 100);
      setState(() {
        _rules
          ..clear()
          ..addAll(page.content);
      });
    } catch (e) {
      setState(() => _error = '분류 기준을 불러오지 못했습니다.');
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _deleteRule(SortingRuleDto rule) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('분류 기준 삭제'),
        content: Text('"${rule.name}" 기준을 삭제할까요?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('취소')),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('삭제')),
        ],
      ),
    );
    if (confirm != true) return;
    try {
      await deleteSortingRule(rule.id);
      setState(() => _rules.removeWhere((r) => r.id == rule.id));
    } catch (e) {
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('삭제에 실패했습니다.')));
    }
  }

  Future<void> _showRuleSheet({SortingRuleDto? rule}) async {
    final nameCtrl = TextEditingController(text: rule?.name ?? '');
    final itemCtrl = TextEditingController(text: rule?.itemName ?? '');
    final inputValueCtrl = TextEditingController(text: rule?.inputValue ?? '');
    String inputType = rule?.inputType ?? 'TEXT';
    int? selectedChuteId = rule?.chutes.isNotEmpty == true ? rule!.chutes.first.id : null;
    bool saving = false;

    final chutePage = await fetchChutes(groupId: widget.group.id, size: 100);
    final chuteOptions = chutePage.content;

    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (context) {
        return StatefulBuilder(builder: (context, setSheetState) {
          final viewInsets = MediaQuery.of(context).viewInsets;
          return Padding(
            padding: EdgeInsets.only(
              bottom: viewInsets.bottom,
              left: 20,
              right: 20,
              top: 24,
            ),
            child: SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Text(
                        rule == null ? '새 분류 기준' : '분류 기준 수정',
                        style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
                      ),
                      const Spacer(),
                      IconButton(
                        onPressed: () => Navigator.pop(context),
                        icon: const Icon(Icons.close),
                      )
                    ],
                  ),
                  const SizedBox(height: 12),
                  _sheetField(
                    label: '기준명',
                    child: TextField(
                      controller: nameCtrl,
                      decoration: const InputDecoration(hintText: '예) 방탄헬멧 - 대형'),
                    ),
                  ),
                  _sheetField(
                    label: '물품명',
                    child: TextField(
                      controller: itemCtrl,
                      decoration: const InputDecoration(hintText: '정확한 물품명을 입력하세요'),
                    ),
                  ),
                  _sheetField(
                    label: '분류 표식',
                    child: Row(
                      children: [
                        Expanded(
                          flex: 2,
                          child: DropdownButtonFormField<String>(
                            value: inputType,
                            decoration: const InputDecoration(),
                            items: _inputTypes
                                .map((type) => DropdownMenuItem<String>(
                                      value: type['value'] as String,
                                      child: Text(type['label'] as String),
                                    ))
                                .toList(),
                            onChanged: (value) {
                              if (value == null) return;
                              setSheetState(() => inputType = value);
                            },
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          flex: 3,
                          child: TextField(
                            controller: inputValueCtrl,
                            decoration: const InputDecoration(hintText: '표식 값'),
                          ),
                        ),
                      ],
                    ),
                  ),
                  _sheetField(
                    label: '분류 라인',
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (chuteOptions.isEmpty)
                          const Text('등록된 라인이 없습니다.', style: TextStyle(color: AppColors.muted))
                        else
                          ...chuteOptions.map(
                            (chute) => RadioListTile<int>(
                              value: chute.id,
                              groupValue: selectedChuteId,
                              onChanged: (value) => setSheetState(() => selectedChuteId = value),
                              title: Text('${chute.name} (${chute.servoDeg}°)'),
                            ),
                          ),
                        TextButton(
                          onPressed: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) => SortingLinesScreen(groupId: widget.group.id),
                              ),
                            ).then((_) => _loadRules());
                          },
                          child: const Text('분류 라인 관리'),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton(
                      onPressed: saving
                          ? null
                          : () async {
                              final ruleName = nameCtrl.text.trim();
                              final itemName = itemCtrl.text.trim();
                              final markerValue = inputValueCtrl.text.trim();
                              if (ruleName.isEmpty ||
                                  itemName.isEmpty ||
                                  markerValue.isEmpty) {
                                ScaffoldMessenger.of(context).showSnackBar(
                                  const SnackBar(content: Text('필수 항목을 채워주세요.')),
                                );
                                return;
                              }
                              setSheetState(() => saving = true);
                              final chuteIds = selectedChuteId != null
                                  ? <int>[selectedChuteId!]
                                  : <int>[];
                              try {
                                if (rule == null) {
                                  await createSortingRule(
                                    groupId: widget.group.id,
                                    ruleName: ruleName,
                                    inputType: inputType,
                                    inputValue: markerValue,
                                    itemName: itemName,
                                    chuteIds: chuteIds,
                                  );
                                } else {
                                  await updateSortingRule(
                                    ruleId: rule.id,
                                    ruleName: ruleName,
                                    inputType: inputType,
                                    inputValue: markerValue,
                                    itemName: itemName,
                                    chuteIds: chuteIds,
                                  );
                                }
                                if (mounted) Navigator.pop(context);
                                await _loadRules();
                              } catch (e) {
                                if (mounted) {
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    const SnackBar(content: Text('저장에 실패했습니다.')),
                                  );
                                }
                              } finally {
                                setSheetState(() => saving = false);
                              }
                            },
                      child: Text(saving ? '저장 중...' : '저장하기'),
                    ),
                  ),
                  const SizedBox(height: 24),
                ],
              ),
            ),
          );
        });
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bg,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text('분류 기준 설정'),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!, style: const TextStyle(color: Colors.redAccent)))
              : ListView(
                  padding: const EdgeInsets.all(20),
                  children: [
                    _groupSummary(widget.group),
                    const SizedBox(height: 16),
                    ..._rules.map((rule) => _RuleCard(
                          rule: rule,
                          onEdit: () => _showRuleSheet(rule: rule),
                          onDelete: () => _deleteRule(rule),
                        )),
                    if (_rules.isEmpty)
                      Container(
                        alignment: Alignment.center,
                        padding: const EdgeInsets.symmetric(vertical: 60),
                        child: const Text('등록된 기준이 없습니다.'),
                      ),
                    const SizedBox(height: 24),
                    OutlinedButton.icon(
                      onPressed: _showRuleSheet,
                      icon: const Icon(Icons.add),
                      label: const Text('+ 기준 추가하기'),
                    ),
                    const SizedBox(height: 32),
                  ],
                ),
    );
  }

  Widget _groupSummary(SortingGroupDto group) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFE5E5E5)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(group.name, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w700)),
          const SizedBox(height: 4),
          Text(
            group.enabled ? '활성화됨' : '비활성화됨',
            style: TextStyle(color: group.enabled ? Colors.green : Colors.grey),
          ),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              _infoTile('현재 처리건', '${group.processingCount}건'),
              _infoTile('마지막 업데이트', _formatDate(group.updatedAt)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _infoTile(String label, String value) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(color: AppColors.muted)),
        const SizedBox(height: 4),
        Text(value, style: const TextStyle(fontWeight: FontWeight.w600)),
      ],
    );
  }
}

class _RuleCard extends StatelessWidget {
  const _RuleCard({
    required this.rule,
    required this.onEdit,
    required this.onDelete,
  });

  final SortingRuleDto rule;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: const Color(0xFFE5E5E5)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(rule.name, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700)),
              const Spacer(),
              PopupMenuButton<String>(
                onSelected: (value) {
                  if (value == 'edit') {
                    onEdit();
                  } else if (value == 'delete') {
                    onDelete();
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
          const SizedBox(height: 12),
          _fieldRow('물품명', rule.itemName),
          const SizedBox(height: 8),
          _fieldRow('분류 표식', '${rule.inputType} · ${rule.inputValue}'),
          const SizedBox(height: 8),
          _fieldRow(
            '서보 각도',
            rule.chutes.isEmpty
                ? '-'
                : '${rule.chutes.first.angle ?? 0}° (${rule.chutes.first.name ?? '미지정'})',
          ),
          if (rule.chutes.length > 1)
            Padding(
              padding: const EdgeInsets.only(top: 6),
              child: Text('외 ${rule.chutes.length - 1}개 라인',
                  style: const TextStyle(color: AppColors.muted, fontSize: 13)),
            ),
        ],
      ),
    );
  }

  Widget _fieldRow(String label, String value) {
    return Row(
      children: [
        SizedBox(
          width: 90,
          child: Text(label, style: const TextStyle(color: AppColors.muted)),
        ),
        Expanded(
          child: Text(value, style: const TextStyle(fontWeight: FontWeight.w600)),
        ),
      ],
    );
  }
}

Widget _sheetField({required String label, required Widget child}) {
  return Padding(
    padding: const EdgeInsets.only(bottom: 16),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontWeight: FontWeight.w600)),
        const SizedBox(height: 8),
        child,
      ],
    ),
  );
}

String _formatDate(DateTime? date) {
  if (date == null) return '-';
  return '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
}
