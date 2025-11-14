import 'package:flutter/material.dart';

import './core/colors.dart';
import '../core/config/app_config.dart';
import '../data/api/history_api.dart';
import '../data/dto/error_history_dto.dart';
import 'widgets/app_shell.dart';
import 'widgets/secure_network_image.dart';

class ErrorHistoryDetailScreen extends StatefulWidget {
  const ErrorHistoryDetailScreen({super.key, required this.historyId});

  final int historyId;

  @override
  State<ErrorHistoryDetailScreen> createState() => _ErrorHistoryDetailScreenState();
}

class _ErrorHistoryDetailScreenState extends State<ErrorHistoryDetailScreen> {
  ErrorHistoryDetailDto? _detail;
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
      final detail = await fetchErrorHistoryDetail(widget.historyId);
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
    final imageUrl = _resolveErrorImage(detail?.images?.primary);

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
                          _DetailRow(label: '오류 ID', value: '${detail?.id ?? '-'}'),
                          _DetailRow(label: '물품명', value: detail?.itemName ?? '-'),
                          _DetailRow(label: '라인명', value: detail?.lineName ?? '-'),
                          _DetailRow(
                            label: '오류코드',
                            value: detail?.errorCode ?? '-',
                            valueStyle: const TextStyle(
                              fontWeight: FontWeight.w700,
                              color: Colors.redAccent,
                            ),
                          ),
                          _DetailRow(label: '처리일시', value: _format(detail?.occurredAt)),
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
                                  : SecureNetworkImage(
                                      imageUrl: imageUrl,
                                      fit: BoxFit.cover,
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

String? _resolveErrorImage(String? url) {
  if (url == null || url.isEmpty) return null;
  if (url.startsWith('http')) return url;
  final base = AppConfig.baseUrl;
  if (url.startsWith('/')) return '$base$url';
  return '$base/$url';
}

class _DetailRow extends StatelessWidget {
  const _DetailRow({
    required this.label,
    required this.value,
    this.valueStyle,
  });

  final String label;
  final String value;
  final TextStyle? valueStyle;

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
              style: valueStyle ?? const TextStyle(fontWeight: FontWeight.w600),
            ),
          ),
        ],
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
