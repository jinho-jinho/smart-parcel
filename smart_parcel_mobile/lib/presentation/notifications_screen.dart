import 'package:flutter/material.dart';

import './core/colors.dart';
import '../data/api/notifications_api.dart';
import '../data/dto/notification_dto.dart';
import 'widgets/app_shell.dart';

class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({super.key});

  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  final List<NotificationDto> _items = [];
  bool _loading = true;
  bool _loadingMore = false;
  bool _hasMore = true;
  int _nextPage = 0;
  String? _error;

  static const _pageSize = 12;

  @override
  void initState() {
    super.initState();
    _load(reset: true);
  }

  Future<void> _load({bool reset = false}) async {
    if (reset) {
      setState(() {
        _loading = true;
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
      final page =
          await fetchNotifications(page: _nextPage, size: _pageSize, unreadOnly: true);
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
      setState(() => _error = '알림을 불러오지 못했습니다.');
    } finally {
      if (reset) {
        setState(() => _loading = false);
      } else {
        setState(() => _loadingMore = false);
      }
    }
  }

  Future<void> _markAsRead(NotificationDto notification) async {
    if (notification.read) return;
    try {
      await markNotificationRead(notification.id);
      setState(() => _items.removeWhere((n) => n.id == notification.id));
    } catch (_) {
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('읽음 처리에 실패했습니다.')));
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
    return '$y.$m.$d $h:$min:$sec';
  }

  String _buildMessage(NotificationDto dto) {
    final group = dto.groupName ?? '-';
    final chute = dto.chuteName ?? '-';
    return '$group / $chute 장비에서 오류가 발생했습니다.\n오류 이력을 확인해 주세요.';
  }

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
                '알림 센터',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800),
              ),
            ),
          ),
          Expanded(
            child: RefreshIndicator(
              onRefresh: () => _load(reset: true),
              child: _loading
                  ? const Center(child: CircularProgressIndicator())
                  : _error != null
                      ? ListView(
                          children: [
                            Padding(
                              padding: const EdgeInsets.all(24),
                              child: Text(_error!, style: const TextStyle(color: Colors.redAccent)),
                            ),
                          ],
                        )
                      : ListView.builder(
                          padding: const EdgeInsets.all(16),
                          itemCount: (_items.isEmpty ? 1 : _items.length + 1),
                          itemBuilder: (context, index) {
                            if (_items.isEmpty) {
                              return const Padding(
                                padding: EdgeInsets.symmetric(vertical: 80),
                                child: Center(child: Text('읽지 않은 알림이 없습니다.')),
                              );
                            }
                            if (index == _items.length) {
                              if (!_hasMore) return const SizedBox(height: 80);
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
                            return Container(
                              margin: const EdgeInsets.only(bottom: 12),
                              padding: const EdgeInsets.all(18),
                              decoration: BoxDecoration(
                                color: const Color(0xFFFFF4F4),
                                borderRadius: BorderRadius.circular(18),
                                border: Border.all(
                                  color: Colors.redAccent,
                                  width: 1.2,
                                ),
                              ),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                    children: [
                                      Text(
                                        '[에러코드: ${item.errorCode ?? '-'}]',
                                        style: const TextStyle(
                                          color: Colors.redAccent,
                                          fontWeight: FontWeight.w700,
                                        ),
                                      ),
                                      TextButton(
                                        onPressed: () => _markAsRead(item),
                                        child: const Text('읽음 처리'),
                                      )
                                    ],
                                  ),
                                  const SizedBox(height: 4),
                                  Text(
                                    _buildMessage(item),
                                    style: const TextStyle(height: 1.3),
                                  ),
                                  const SizedBox(height: 8),
                                  Text(
                                    _format(item.createdAt ?? item.occurredAt),
                                    style: const TextStyle(color: AppColors.muted, fontSize: 12),
                                  ),
                                ],
                              ),
                            );
                          },
                        ),
            ),
          ),
        ],
      ),
    );
  }
}
