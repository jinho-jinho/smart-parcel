import 'package:flutter/material.dart';

import './core/colors.dart';
import '../core/config/app_config.dart';
import '../data/api/history_api.dart';
import '../data/dto/sorting_history_dto.dart';
import 'widgets/app_shell.dart';

class SortingHistoryDetailScreen extends StatefulWidget {
  const SortingHistoryDetailScreen({super.key, required this.historyId});

  final int historyId;

  @override
  State<SortingHistoryDetailScreen> createState() => _SortingHistoryDetailScreenState();
}

class _SortingHistoryDetailScreenState extends State<SortingHistoryDetailScreen> {
  SortingHistoryDetailDto? _detail;
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
      final detail = await fetchSortingHistoryDetail(widget.historyId);
      setState(() => _detail = detail);
    } catch (e) {
      setState(() => _error = '상세 정보를 불러오지 못했습니다.');
    } finally {
      setState(() => _loading = false);
    }
  }

  String _format(DateTime? dt) {
    if (dt == null) return '-';
    final y = dt.year.toString().padLeft(4, '0');
    final m = dt.month.toString().padLeft(2, '0');
    final d = dt.day.toString().padLeft(2, '0');
    final h = dt.hour.toString().padLeft(2, '0');
    final min = dt.minute.toString().padLeft(2, '0');
    final sec = dt.second.toString().padLeft(2, '0');
    return '$y-$m-$d $h:$min:$sec';
  }

  @override
  Widget build(BuildContext context) {
    final detail = _detail;
    final imageUrl = _resolveImage(detail?.images?.primary);

    return Scaffold(
      backgroundColor: AppColors.bg,
      appBar: const SmartParcelAppBar(showBack: true, enableDrawer: false),
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
                    elevation: 0,
                    child: Padding(
                      padding: const EdgeInsets.all(24),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          _DetailItem(label: '분류 ID', value: '${detail?.id ?? '-'}'),
                          _DetailItem(label: '물품명', value: detail?.itemName ?? '-'),
                          _DetailItem(label: '라인명', value: detail?.lineName ?? '-'),
                          _DetailItem(label: '처리일시', value: _format(detail?.processedAt)),
                          const SizedBox(height: 16),
                          const Text('이미지', style: TextStyle(fontWeight: FontWeight.w700)),
                          const SizedBox(height: 12),
                          AspectRatio(
                            aspectRatio: 4 / 3,
                            child: Container(
                              decoration: BoxDecoration(
                                color: AppColors.bg,
                                borderRadius: BorderRadius.circular(16),
                                border: Border.all(color: const Color(0xFFDDDDDD)),
                              ),
                              clipBehavior: Clip.antiAlias,
                              child: imageUrl == null
                                  ? const Center(child: Text('{Image}'))
                                  : Image.network(
                                      imageUrl,
                                      fit: BoxFit.cover,
                                      errorBuilder: (_, __, ___) => const Center(
                                        child: Text('이미지를 불러올 수 없습니다.'),
                                      ),
                                    ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
    );
  }
}

String? _resolveImage(String? url) {
  if (url == null || url.isEmpty) return null;
  if (url.startsWith('http')) return url;
  final base = AppConfig.baseUrl;
  if (url.startsWith('/')) return '$base$url';
  return '$base/$url';
}

class _DetailItem extends StatelessWidget {
  const _DetailItem({required this.label, required this.value});
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(color: AppColors.muted)),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
          ),
        ],
      ),
    );
  }
}
