// lib/main.dart
import 'package:flutter/material.dart';
import 'presentation/error_history_screen.dart';
import 'presentation/login_screen.dart';
import 'presentation/me_screen.dart';
import 'presentation/notifications_screen.dart';
import 'presentation/reset_password_screen.dart';
import 'presentation/sorting_groups_screen.dart';
import 'presentation/sorting_history_screen.dart';
import 'presentation/staff_management_screen.dart';
import 'presentation/stats_dashboard_screen.dart';
import 'presentation/signup_screen.dart';

void main() => runApp(const App());

class App extends StatelessWidget {
  const App({super.key});
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Smart Parcel',
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: Colors.black,
          brightness: Brightness.light,
        ),
        scaffoldBackgroundColor: Colors.white,
        appBarTheme: const AppBarTheme(
          backgroundColor: Colors.white,
          foregroundColor: Colors.black,
          elevation: 0,
        ),
      ),
      initialRoute: '/login',
      routes: {
        '/login': (_) => const LoginScreen(),
        '/signup': (_) => const SignUpScreen(),
        '/reset-password': (_) => const ResetPasswordScreen(),
        '/groups': (_) => const SortingGroupsScreen(),
        '/history/sorting': (_) => const SortingHistoryScreen(),
        '/history/errors': (_) => const ErrorHistoryScreen(),
        '/stats': (_) => const StatsDashboardScreen(),
        '/notifications': (_) => const NotificationsScreen(),
        '/staff': (_) => const StaffManagementScreen(),
        '/me': (_) => const MeScreen(),
      },
    );
  }
}
