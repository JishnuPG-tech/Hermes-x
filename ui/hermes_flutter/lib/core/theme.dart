import 'package:flutter/material.dart';

class HermesColors {
  static const bg = Color(0xFF141414);
  static const elevated = Color(0xFF202020);
  static const input = Color(0xFF2E2E2E);
  static const accent = Color(0xFF5B8CFF);
  static const system = Color(0xFF4A7FE8);
  static const action = Color(0xFFFFB454);
  static const danger = Color(0xFFE5484D);
  static const success = Color(0xFF3FC97D);
  static const text = Color(0xFFF5F2ED);
  static const secondary = Color(0xFF8E8E93);
  static const border = Color(0xFF333333);
}

class HermesTheme {
  static ThemeData dark() => ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: HermesColors.bg,
        colorScheme: const ColorScheme.dark(
          surface: HermesColors.elevated,
          primary: HermesColors.accent,
          error: HermesColors.danger,
        ),
        fontFamily: 'sans-serif',
        cardTheme: CardThemeData(
          color: HermesColors.elevated,
          elevation: 0,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: HermesColors.input,
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(28),
            borderSide: BorderSide.none,
          ),
        ),
      );
}
