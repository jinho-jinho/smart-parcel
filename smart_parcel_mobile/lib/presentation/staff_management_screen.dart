import 'package:flutter/material.dart';

import './core/colors.dart';
import '../data/api/staff_admin_api.dart';
import '../data/dto/staff_dto.dart';
import 'widgets/app_shell.dart';

class StaffManagementScreen extends StatefulWidget {
  const StaffManagementScreen({super.key});

  @override
  State<StaffManagementScreen> createState() => _StaffManagementScreenState();
}

class _StaffManagementScreenState extends State<StaffManagementScreen> {
  final TextEditingController _searchCtrl = TextEditingController();
  List<StaffSummaryDto> _staff = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _load({String? keyword}) async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final page = await fetchStaff(keyword: keyword);
      setState(() => _staff = page.content);
    } catch (e) {
      setState(() => _error = '직원 목록을 불러오지 못했습니다.');
    } finally {
      setState(() => _loading = false);
    }
  }

  String _roleLabel(String role) {
    switch (role) {
      case 'ADMIN':
        return '어드민';
      case 'MANAGER':
        return '관리자';
      default:
        return '직원';
    }
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
                '직원 관리',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _searchCtrl,
                    decoration: InputDecoration(
                      hintText: '이름, 이메일 검색',
                      prefixIcon: const Icon(Icons.search),
                      filled: true,
                      fillColor: Colors.white,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                        borderSide: BorderSide.none,
                      ),
                    ),
                    textInputAction: TextInputAction.search,
                    onSubmitted: (value) => _load(keyword: value.trim()),
                  ),
                ),
                const SizedBox(width: 12),
                FilledButton(
                  onPressed: () => _load(keyword: _searchCtrl.text.trim()),
                  child: const Text('검색'),
                ),
              ],
            ),
          ),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Text(_error!, style: const TextStyle(color: Colors.redAccent)),
            ),
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : _staff.isEmpty
                    ? const Center(child: Text('등록된 직원이 없습니다.'))
                    : ListView.separated(
                        padding: const EdgeInsets.all(16),
                        itemBuilder: (context, index) {
                          final staff = _staff[index];
                          return Card(
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                            child: Padding(
                              padding: const EdgeInsets.all(16),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(staff.name,
                                      style: const TextStyle(
                                        fontSize: 16,
                                        fontWeight: FontWeight.w700,
                                      )),
                                  const SizedBox(height: 4),
                                  Text(staff.email, style: const TextStyle(color: AppColors.muted)),
                                  const SizedBox(height: 8),
                                  Row(
                                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                    children: [
                                      Text('권한: ${_roleLabel(staff.role)}'),
                                      Text(
                                        staff.createdAt != null
                                            ? '${staff.createdAt!.year}-${staff.createdAt!.month.toString().padLeft(2, '0')}-${staff.createdAt!.day.toString().padLeft(2, '0')}'
                                            : '-',
                                        style: const TextStyle(color: AppColors.muted, fontSize: 12),
                                      ),
                                    ],
                                  ),
                                ],
                              ),
                            ),
                          );
                        },
                        separatorBuilder: (_, __) => const SizedBox(height: 8),
                        itemCount: _staff.length,
                      ),
          ),
        ],
      ),
    );
  }
}
