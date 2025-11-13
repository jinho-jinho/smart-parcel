import 'package:flutter/material.dart';

import '../core/colors.dart';

class HistoryFilterBar extends StatelessWidget {
  const HistoryFilterBar({
    super.key,
    required this.searchController,
    required this.searchPlaceholder,
    required this.useDateRange,
    required this.onToggleRange,
    required this.from,
    required this.to,
    required this.onPickFrom,
    required this.onPickTo,
    required this.onSearch,
  });

  final TextEditingController searchController;
  final String searchPlaceholder;
  final bool useDateRange;
  final ValueChanged<bool> onToggleRange;
  final DateTime? from;
  final DateTime? to;
  final VoidCallback onPickFrom;
  final VoidCallback onPickTo;
  final VoidCallback onSearch;

  String _format(DateTime? dt) {
    if (dt == null) return '날짜 선택';
    final y = dt.year.toString().padLeft(4, '0');
    final m = dt.month.toString().padLeft(2, '0');
    final d = dt.day.toString().padLeft(2, '0');
    final h = dt.hour.toString().padLeft(2, '0');
    final min = dt.minute.toString().padLeft(2, '0');
    return '$y-$m-$d $h:$min';
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.white,
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: searchController,
                  decoration: InputDecoration(
                    hintText: searchPlaceholder,
                    prefixIcon: const Icon(Icons.search),
                    filled: true,
                    fillColor: AppColors.bg,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: BorderSide.none,
                    ),
                  ),
                  textInputAction: TextInputAction.search,
                  onSubmitted: (_) => onSearch(),
                ),
              ),
              const SizedBox(width: 12),
              SizedBox(
                height: 48,
                child: FilledButton(
                  onPressed: onSearch,
                  child: const Text('검색'),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Switch(
                value: useDateRange,
                onChanged: onToggleRange,
              ),
              const SizedBox(width: 8),
              Expanded(
                child: GestureDetector(
                  onTap: useDateRange ? onPickFrom : null,
                  child: _DateChip(label: _format(from), enabled: useDateRange),
                ),
              ),
              const SizedBox(width: 8),
              const Text('~'),
              const SizedBox(width: 8),
              Expanded(
                child: GestureDetector(
                  onTap: useDateRange ? onPickTo : null,
                  child: _DateChip(label: _format(to), enabled: useDateRange),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _DateChip extends StatelessWidget {
  const _DateChip({required this.label, required this.enabled});
  final String label;
  final bool enabled;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color: enabled ? AppColors.bg : AppColors.bg.withOpacity(.6),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFFE0E0E0)),
      ),
      child: Row(
        children: [
          const Icon(Icons.calendar_month, size: 18),
          const SizedBox(width: 6),
          Expanded(
            child: Text(
              label,
              style: const TextStyle(fontSize: 13),
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }
}

class HistoryListRow extends StatelessWidget {
  const HistoryListRow({
    super.key,
    required this.title,
    required this.lineName,
    required this.timestamp,
    required this.onTap,
    this.subtitle,
    this.trailing,
    this.accent,
  });

  final String title;
  final String lineName;
  final DateTime? timestamp;
  final String? subtitle;
  final Widget? trailing;
  final Color? accent;
  final VoidCallback onTap;

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
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 6),
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      title,
                      style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w700),
                    ),
                  ),
                  trailing ?? Text(lineName, style: const TextStyle(color: AppColors.muted)),
                ],
              ),
              if (subtitle != null) ...[
                const SizedBox(height: 4),
                Text(subtitle!, style: TextStyle(color: accent ?? AppColors.muted)),
              ],
              const SizedBox(height: 4),
              Text(lineName, style: const TextStyle(color: AppColors.muted)),
              const SizedBox(height: 4),
              Text(_format(timestamp), style: const TextStyle(fontWeight: FontWeight.w600)),
            ],
          ),
        ),
      ),
    );
  }
}
