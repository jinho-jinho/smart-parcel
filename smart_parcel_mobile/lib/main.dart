// lib/main.dart
import 'package:flutter/material.dart';
import 'presentation/login_screen.dart';
import 'presentation/signup_screen.dart';
import 'presentation/reset_password_screen.dart';

void main() => runApp(const App());

class App extends StatelessWidget {
  const App({super.key});
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Smart Parcel',
      theme: ThemeData(useMaterial3: true, colorSchemeSeed: Colors.black),
      initialRoute: '/login',
      routes: {
        '/login': (_) => const LoginScreen(),
        '/signup': (_) => const SignUpScreen(),
        '/reset-password': (_) => const ResetPasswordScreen(),
      },
    );
  }
}
