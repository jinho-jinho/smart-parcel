// lib/main.dart
import 'package:flutter/material.dart';
import 'core/network/dio_client.dart';
import 'core/notifications/push_notification_service.dart';
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
import 'presentation/widgets/notification_watcher.dart';

final GlobalKey<NavigatorState> navigatorKey = GlobalKey();

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await DioClient.ensureInitialized();
  await PushNotificationService.instance.ensureInitialized();
  PushNotificationService.instance.registerNavigator(navigatorKey);
  runApp(const App());
}

class App extends StatelessWidget {
  const App({super.key});
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Smart Parcel',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: const ColorScheme(
          brightness: Brightness.light,
          primary: Colors.black,
          onPrimary: Colors.white,
          secondary: Colors.black87,
          onSecondary: Colors.white,
          error: Colors.redAccent,
          onError: Colors.white,
          background: Colors.white,
          onBackground: Colors.black87,
          surface: Colors.white,
          onSurface: Colors.black87,
        ),
        scaffoldBackgroundColor: Colors.white,
        appBarTheme: const AppBarTheme(
          backgroundColor: Colors.white,
          foregroundColor: Colors.black,
          elevation: 0,
        ),
        filledButtonTheme: FilledButtonThemeData(
          style: FilledButton.styleFrom(
            backgroundColor: Colors.black,
            foregroundColor: Colors.white,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
          ),
        ),
        outlinedButtonTheme: OutlinedButtonThemeData(
          style: OutlinedButton.styleFrom(
            foregroundColor: Colors.black,
            side: const BorderSide(color: Colors.black),
          ),
        ),
        textButtonTheme: TextButtonThemeData(
          style: TextButton.styleFrom(foregroundColor: Colors.black),
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: const Color(0xFFF5F5F5),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: BorderSide.none,
          ),
        ),
      ),
      navigatorKey: navigatorKey,
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
      builder: (context, child) => NotificationWatcher(
        navigatorKey: navigatorKey,
        child: child ?? const SizedBox.shrink(),
      ),
    );
  }
}
