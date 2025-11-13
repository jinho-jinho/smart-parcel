import 'package:flutter/material.dart';

import '../../core/config/app_config.dart';
import '../../data/api/user_api.dart' as user_api;

class SmartParcelAppBar extends StatelessWidget implements PreferredSizeWidget {
  const SmartParcelAppBar({
    super.key,
    this.actions,
    this.enableDrawer = true,
    this.showBack = false,
  });

  final List<Widget>? actions;
  final bool enableDrawer;
  final bool showBack;

  @override
  Widget build(BuildContext context) {
    Widget? leading;
    if (showBack) {
      leading = IconButton(
        icon: const Icon(Icons.arrow_back, color: Colors.black),
        onPressed: () => Navigator.pop(context),
      );
    } else if (enableDrawer) {
      leading = Builder(
        builder: (context) => IconButton(
          icon: const Icon(Icons.menu, color: Colors.black),
          onPressed: () => Scaffold.of(context).openDrawer(),
        ),
      );
    }

    return AppBar(
      backgroundColor: Colors.white,
      elevation: 0,
      leading: leading,
      titleSpacing: 0,
      title: Row(
        children: [
          Image.asset(
            'assets/icon.png',
            width: 28,
            height: 28,
          ),
          const SizedBox(width: 8),
          const Text(
            'SmartParcel',
            style: TextStyle(
              color: Colors.black,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
      actions: actions,
    );
  }

  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight);
}

class AppMenuDrawer extends StatelessWidget {
  const AppMenuDrawer({super.key});

  @override
  Widget build(BuildContext context) {
    return Drawer(
      child: SafeArea(
        child: ListView(
          padding: EdgeInsets.zero,
          children: [
            DrawerHeader(
              decoration: BoxDecoration(
                color: Colors.grey.shade100,
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Image.asset('assets/icon.png', width: 48, height: 48),
                  const SizedBox(height: 12),
                  const Text(
                    'SmartParcel',
                    style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    AppConfig.baseUrl,
                    style: const TextStyle(fontSize: 12, color: Colors.black54),
                  ),
                ],
              ),
            ),
            _NavTile(
              icon: Icons.dashboard_customize,
              label: '분류 그룹',
              onTap: () => _navigate(context, '/groups'),
            ),
            _NavTile(
              icon: Icons.bar_chart,
              label: '통계 대시보드',
              onTap: () => _navigate(context, '/stats'),
            ),
            _NavTile(
              icon: Icons.list_alt,
              label: '분류 이력',
              onTap: () => _navigate(context, '/history/sorting'),
            ),
            _NavTile(
              icon: Icons.error_outline,
              label: '오류 이력',
              onTap: () => _navigate(context, '/history/errors'),
            ),
            _NavTile(
              icon: Icons.notifications_none,
              label: '알림 센터',
              onTap: () => _navigate(context, '/notifications'),
            ),
            _NavTile(
              icon: Icons.badge_outlined,
              label: '직원 관리',
              onTap: () => _navigate(context, '/staff'),
            ),
            _NavTile(
              icon: Icons.person_outline,
              label: '내 정보',
              onTap: () => _navigate(context, '/me'),
            ),
            const Divider(),
            _NavTile(
              icon: Icons.logout,
              label: '로그아웃',
              onTap: () async {
                Navigator.pop(context);
                await user_api.logout();
                if (context.mounted) {
                  Navigator.of(context).pushNamedAndRemoveUntil('/login', (route) => false);
                }
              },
            ),
          ],
        ),
      ),
    );
  }

  void _navigate(BuildContext context, String route) {
    Navigator.pop(context);
    if (ModalRoute.of(context)?.settings.name == route) return;
    Navigator.of(context).pushNamed(route);
  }
}

class _NavTile extends StatelessWidget {
  const _NavTile({required this.icon, required this.label, required this.onTap});

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon),
      title: Text(label),
      onTap: onTap,
    );
  }
}
