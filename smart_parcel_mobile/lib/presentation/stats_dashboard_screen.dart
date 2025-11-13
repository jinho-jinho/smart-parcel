import 'package:flutter/material.dart';

import './core/colors.dart';
import '../data/api/sorting_groups_api.dart';
import '../data/api/stats_api.dart';
import '../data/dto/sorting_group_dto.dart';
import '../data/dto/stats_dto.dart';
import 'widgets/app_shell.dart';

class StatsDashboardScreen extends StatefulWidget {
  const StatsDashboardScreen({super.key});

  @override
  State<StatsDashboardScreen> createState() => _StatsDashboardScreenState();
}

class _StatsDashboardScreenState extends State<StatsDashboardScreen> {
  DateTime _fromDate = DateTime.now().subtract(const Duration(days: 6));
  DateTime _toDate = DateTime.now();
  int? _groupId;

  bool _loading = true;
  String? _error;

  List<SortingGroupDto> _groups = [];
  List<CountStatDto> _byChute = [];
  List<DailyCountStatDto> _daily = [];
  List<CountStatDto> _byErrorCode = [];
  ErrorRateDto? _errorRate;

  @override
  void initState() {
    super.initState();
    _initialize();
  }

  Future<void> _initialize() async {
    await Future.wait([_loadGroups(), _loadStats()]);
  }

  Future<void> _loadGroups() async {
    try {
      final page = await fetchSortingGroups(size: 100);
      setState(() => _groups = page.content);
    } catch (_) {
      // ignore dropdown errors
    }
  }

  Future<void> _loadStats() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final from = DateTime(_fromDate.year, _fromDate.month, _fromDate.day, 0, 0, 0);
      final to = DateTime(_toDate.year, _toDate.month, _toDate.day, 23, 59, 59);
      final results = await Future.wait([
        fetchStatsByChute(from: from, to: to, groupId: _groupId),
        fetchStatsDaily(from: from, to: to, groupId: _groupId),
        fetchStatsByErrorCode(from: from, to: to, groupId: _groupId),
        fetchErrorRate(from: from, to: to, groupId: _groupId),
      ]);

      setState(() {
        _byChute = results[0] as List<CountStatDto>;
        _daily = results[1] as List<DailyCountStatDto>;
        _byErrorCode = results[2] as List<CountStatDto>;
        _errorRate = results[3] as ErrorRateDto;
      });
    } catch (e) {
      setState(() => _error = '통계 데이터를 불러오지 못했습니다.');
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _pickDate(bool isStart) async {
    final initial = isStart ? _fromDate : _toDate;
    final picked = await showDatePicker(
      context: context,
      initialDate: initial,
      firstDate: DateTime(2023),
      lastDate: DateTime.now().add(const Duration(days: 365)),
    );
    if (picked == null) return;
    setState(() {
      if (isStart) {
        _fromDate = picked;
        if (_toDate.isBefore(_fromDate)) _toDate = _fromDate;
      } else {
        _toDate = picked;
        if (_fromDate.isAfter(_toDate)) _fromDate = _toDate;
      }
    });
  }

  String _formatDate(DateTime date) {
    final y = date.year.toString().padLeft(4, '0');
    final m = date.month.toString().padLeft(2, '0');
    final d = date.day.toString().padLeft(2, '0');
    return '$y-$m-$d';
  }

  Future<void> _onRefresh() => _loadStats();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bg,
      drawer: const AppMenuDrawer(),
      appBar: const SmartParcelAppBar(),
      body: RefreshIndicator(
        onRefresh: _onRefresh,
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                '통계 대시보드',
                style: TextStyle(fontSize: 22, fontWeight: FontWeight.w800),
              ),
              const SizedBox(height: 12),
              _FilterCard(
                fromLabel: _formatDate(_fromDate),
                toLabel: _formatDate(_toDate),
                onPickFrom: () => _pickDate(true),
                onPickTo: () => _pickDate(false),
                groupId: _groupId,
                groups: _groups,
                onGroupSelected: (value) => setState(() => _groupId = value),
                onApply: _loadStats,
              ),
              if (_error != null)
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 8),
                  child: Text(_error!, style: const TextStyle(color: Colors.redAccent)),
                ),
              if (_loading)
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 60),
                  child: Center(child: CircularProgressIndicator()),
                )
              else ...[
                _StatsSection(
                  title: '라인별 분류 건수',
                  child: _HorizontalBarChart(data: _byChute),
                ),
                _StatsSection(
                  title: '일자별 분류 건수',
                  child: _VerticalBarChart(data: _daily),
                ),
                _StatsSection(
                  title: '오류 발생 건수',
                  child: Column(
                    children: [
                      _HorizontalBarChart(data: _byErrorCode, accentColor: Colors.redAccent),
                      const SizedBox(height: 16),
                      _ErrorRateCard(rate: _errorRate),
                    ],
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _FilterCard extends StatelessWidget {
  const _FilterCard({
    required this.fromLabel,
    required this.toLabel,
    required this.onPickFrom,
    required this.onPickTo,
    required this.groupId,
    required this.groups,
    required this.onGroupSelected,
    required this.onApply,
  });

  final String fromLabel;
  final String toLabel;
  final VoidCallback onPickFrom;
  final VoidCallback onPickTo;
  final int? groupId;
  final List<SortingGroupDto> groups;
  final ValueChanged<int?> onGroupSelected;
  final VoidCallback onApply;

  @override
  Widget build(BuildContext context) {
    return Card(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('조회 조건', style: TextStyle(fontWeight: FontWeight.w700)),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: _DateButton(label: fromLabel, onTap: onPickFrom),
                ),
                const Padding(
                  padding: EdgeInsets.symmetric(horizontal: 8),
                  child: Text('~'),
                ),
                Expanded(
                  child: _DateButton(label: toLabel, onTap: onPickTo),
                ),
              ],
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<int>(
              value: groupId,
              decoration: const InputDecoration(
                labelText: '분류 그룹',
                border: OutlineInputBorder(),
              ),
              items: [
                const DropdownMenuItem<int>(value: null, child: Text('전체')),
                ...groups.map((group) => DropdownMenuItem<int>(
                      value: group.id,
                      child: Text(group.name),
                    )),
              ],
              onChanged: onGroupSelected,
            ),
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: onApply,
                child: const Text('적용'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _StatsSection extends StatelessWidget {
  const _StatsSection({required this.title, required this.child});

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 20),
      child: Card(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700)),
              const SizedBox(height: 12),
              child,
            ],
          ),
        ),
      ),
    );
  }
}

class _HorizontalBarChart extends StatelessWidget {
  const _HorizontalBarChart({required this.data, this.accentColor});

  final List<CountStatDto> data;
  final Color? accentColor;

  @override
  Widget build(BuildContext context) {
    if (data.isEmpty) {
      return const _EmptyPlaceholder();
    }
    final maxCount = data.map((e) => e.count).fold<int>(0, (prev, el) => el > prev ? el : prev);
    return Column(
      children: data.map((item) {
        final percent = maxCount == 0 ? 0.0 : item.count / maxCount;
        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 6),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(item.label, style: const TextStyle(fontWeight: FontWeight.w600)),
                  Text('${item.count}건'),
                ],
              ),
              const SizedBox(height: 6),
              ClipRRect(
                borderRadius: BorderRadius.circular(999),
                child: LinearProgressIndicator(
                  minHeight: 8,
                  value: percent,
                  valueColor: AlwaysStoppedAnimation<Color>(
                    accentColor ?? Colors.black87,
                  ),
                  backgroundColor: AppColors.bg,
                ),
              ),
            ],
          ),
        );
      }).toList(),
    );
  }
}

class _VerticalBarChart extends StatelessWidget {
  const _VerticalBarChart({required this.data});

  final List<DailyCountStatDto> data;

  @override
  Widget build(BuildContext context) {
    if (data.isEmpty) return const _EmptyPlaceholder();
    final maxCount = data.map((e) => e.count).fold<int>(0, (prev, el) => el > prev ? el : prev);
    return SizedBox(
      height: 220,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: data.map((item) {
          final percent = maxCount == 0 ? 0.0 : item.count / maxCount;
          return Expanded(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 6),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  Text('${item.count}', style: const TextStyle(fontWeight: FontWeight.w700)),
                  const SizedBox(height: 6),
                  Expanded(
                    child: Align(
                      alignment: Alignment.bottomCenter,
                      child: Container(
                        height: 160 * percent,
                        decoration: BoxDecoration(
                          color: Colors.black87,
                          borderRadius: BorderRadius.circular(10),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    item.date,
                    textAlign: TextAlign.center,
                    style: const TextStyle(fontSize: 12, color: AppColors.muted),
                  ),
                ],
              ),
            ),
          );
        }).toList(),
      ),
    );
  }
}

class _ErrorRateCard extends StatelessWidget {
  const _ErrorRateCard({required this.rate});

  final ErrorRateDto? rate;

  @override
  Widget build(BuildContext context) {
    final totalProcessed = rate?.totalProcessed ?? 0;
    final totalErrors = rate?.totalErrors ?? 0;
    final total = totalProcessed + totalErrors;
    final errorPercent = total == 0 ? 0.0 : totalErrors / total;

    return Row(
      children: [
        SizedBox(
          width: 120,
          height: 120,
          child: Stack(
            alignment: Alignment.center,
            children: [
              SizedBox(
                width: 120,
                height: 120,
                child: CircularProgressIndicator(
                  strokeWidth: 12,
                  value: errorPercent,
                  color: Colors.redAccent,
                  backgroundColor: AppColors.bg,
                ),
              ),
              Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Text('오류율', style: TextStyle(fontSize: 12)),
                  Text('${(errorPercent * 100).toStringAsFixed(1)}%',
                      style: const TextStyle(fontWeight: FontWeight.bold)),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _StatLine(label: '총 처리', value: '$total건'),
              _StatLine(label: '정상 처리', value: '$totalProcessed건'),
              _StatLine(label: '오류 건수', value: '$totalErrors건', accent: Colors.redAccent),
            ],
          ),
        ),
      ],
    );
  }
}

class _StatLine extends StatelessWidget {
  const _StatLine({required this.label, required this.value, this.accent});

  final String label;
  final String value;
  final Color? accent;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(color: AppColors.muted)),
          Text(value, style: TextStyle(fontWeight: FontWeight.w700, color: accent)),
        ],
      ),
    );
  }
}

class _DateButton extends StatelessWidget {
  const _DateButton({required this.label, required this.onTap});

  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return OutlinedButton.icon(
      onPressed: onTap,
      icon: const Icon(Icons.calendar_today, size: 16),
      label: Text(label),
    );
  }
}

class _EmptyPlaceholder extends StatelessWidget {
  const _EmptyPlaceholder();

  @override
  Widget build(BuildContext context) {
    return Container(
      alignment: Alignment.center,
      padding: const EdgeInsets.symmetric(vertical: 24),
      child: const Text('데이터가 없습니다.', style: TextStyle(color: AppColors.muted)),
    );
  }
}
