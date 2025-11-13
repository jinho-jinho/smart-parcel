import 'package:flutter/material.dart';

import './core/colors.dart';
import '../data/api/history_api.dart';
import '../data/dto/error_history_dto.dart';
import 'error_history_detail_screen.dart';
import 'widgets/app_shell.dart';
import 'widgets/history_widgets.dart';

class ErrorHistoryScreen extends StatefulWidget {
  const ErrorHistoryScreen({super.key});

  @override
  State<ErrorHistoryScreen> createState() => _ErrorHistoryScreenState();
}

class _ErrorHistoryScreenState extends State<ErrorHistoryScreen> {
  final _searchCtrl = TextEditingController();
  final List<ErrorHistorySummaryDto> _items = [];

  bool _initialLoading = true;
  bool _loadingMore = false;
  bool _hasMore = true;
  int _nextPage = 0;
  String? _error;

  bool _useRange = true;
  DateTime? _from;
  DateTime? _to;

  static const _pageSize = 20;

  @override
  void initState() {
    super.initState();
    final now = DateTime.now();
    _to = now;
    _from = now.subtract(const Duration(days: 7));
    _load(reset: true);
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _load({bool reset = false}) async {
    if (reset) {
      setState(() {
        _initialLoading = true;
        _error = null;
        _hasMore = true;
        _nextPage = 0;
        _items.clear();
      });
    } else {
      if (!_hasMore || _loadingMore) return;
      setState(() => _loadingMore = true);
    }

    try {
      final page = await fetchErrorHistory(
        page: _nextPage,
        size: _pageSize,
        keyword: _searchCtrl.text.trim().isEmpty ? null : _searchCtrl.text.trim(),
        from: _useRange ? _from : null,
        to: _useRange ? _to : null,
      );
      setState(() {
        if (reset) {
          _items
            ..clear()
            ..addAll(page.content);
        } else {
          _items.addAll(page.content);
        }
        _hasMore = !page.last;
        _nextPage = page.page + 1;
      });
    } catch (e) {
      setState(() => _error = '오류 이력을 불러오지 못했습니다.');
    } finally {
      if (reset) {
        setState(() => _initialLoading = false);
      } else {
        setState(() => _loadingMore = false);
      }
    }
  }

  Future<void> _pickDate({required bool isStart}) async {
    final base = isStart ? (_from ?? DateTime.now().subtract(const Duration(days: 7))) : (_to ?? DateTime.now());
    final pickedDate = await showDatePicker(
      context: context,
      initialDate: base,
      firstDate: DateTime(2023),
      lastDate: DateTime.now().add(const Duration(days: 365)),
    );
    if (pickedDate == null) return;
    final pickedTime = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(base),
    );
    if (pickedTime == null) return;
    final merged = DateTime(
      pickedDate.year,
      pickedDate.month,
      pickedDate.day,
      pickedTime.hour,
      pickedTime.minute,
    );
    setState(() {
      if (isStart) {
        _from = merged;
        if (_to != null && _to!.isBefore(_from!)) {
          _to = _from;
        }
      } else {
        _to = merged;
        if (_from != null && _from!.isAfter(_to!)) {
          _from = _to;
        }
      }
    });
  }

  Future<void> _onRefresh() => _load(reset: true);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bg,
      drawer: const AppMenuDrawer(),
      appBar: const SmartParcelAppBar(),
      body: Column(
        children: [
          const Padding(
            padding: EdgeInsets.fromLTRB(16, 16, 16, 0),
            child: Align(
              alignment: Alignment.centerLeft,
              child: Text(
                '오류 이력 조회',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800),
              ),
            ),
          ),
          HistoryFilterBar(
            searchController: _searchCtrl,
            searchPlaceholder: '오류 ID, 물품명, 라인명, 오류코드 검색',
            useDateRange: _useRange,
            onToggleRange: (value) => setState(() => _useRange = value),
            from: _from,
            to: _to,
            onPickFrom: () => _pickDate(isStart: true),
            onPickTo: () => _pickDate(isStart: false),
            onSearch: () => _load(reset: true),
          ),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
              child: Text(_error!, style: const TextStyle(color: Colors.redAccent)),
            ),
          Expanded(
            child: RefreshIndicator(
              onRefresh: _onRefresh,
              child: _initialLoading
                  ? const Center(child: CircularProgressIndicator())
                  : ListView.builder(
                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                      itemCount: _items.length + 1,
                      itemBuilder: (context, index) {
                        if (index == _items.length) {
                          if (!_hasMore) {
                            return const SizedBox(height: 80);
                          }
                          return Padding(
                            padding: const EdgeInsets.symmetric(vertical: 16),
                            child: Center(
                              child: _loadingMore
                                  ? const CircularProgressIndicator()
                                  : OutlinedButton(
                                      onPressed: _load,
                                      child: const Text('더 불러오기'),
                                    ),
                            ),
                          );
                        }
                        final item = _items[index];
                        return HistoryListRow(
                          title: item.itemName,
                          lineName: item.lineName,
                          timestamp: item.occurredAt,
                          subtitle: '오류코드 ${item.errorCode}',
                          accent: Colors.redAccent,
                          onTap: () {
                            Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) => ErrorHistoryDetailScreen(historyId: item.id),
                              ),
                            );
                          },
                        );
                      },
                    ),
            ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _load(reset: true),
        child: const Icon(Icons.refresh),
      ),
    );
  }
}
