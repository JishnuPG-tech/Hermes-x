import 'package:flutter/material.dart';
import 'core/theme.dart';
import 'screens/home.dart';

void main() => runApp(const HermesApp());

class HermesApp extends StatelessWidget {
  const HermesApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
        debugShowCheckedModeBanner: false,
        title: 'Hermes',
        theme: HermesTheme.dark(),
        home: const HomeScreen(),
      );
}
