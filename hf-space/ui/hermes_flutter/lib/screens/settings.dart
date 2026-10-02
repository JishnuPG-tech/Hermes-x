import 'package:flutter/material.dart';
import '../core/theme.dart';
import '../widgets/ui.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});
  @override
  Widget build(BuildContext context) => Scaffold(
        backgroundColor: HermesColors.bg,
        appBar: AppBar(backgroundColor: HermesColors.bg, title: const SerifTitle('Settings', size: 28)),
        body: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            const HermesCard(child: Column(children: [
              SettingRow(icon: Icons.person_outline, title: 'Profile', subtitle: 'Hermes owner'),
              SettingRow(icon: Icons.auto_awesome, title: 'Capabilities', subtitle: '8 enabled'),
              SettingRow(icon: Icons.extension_outlined, title: 'Integrations', subtitle: 'GitHub · Notion'),
              SettingRow(icon: Icons.shield_outlined, title: 'Permissions', subtitle: 'Trust and approvals'),
            ])),
            const SizedBox(height: 12),
            const HermesCard(child: Column(children: [
              SettingRow(icon: Icons.record_voice_over_outlined, title: 'Voice', subtitle: 'Wake mode · Streaming TTS'),
              SettingRow(icon: Icons.palette_outlined, title: 'Appearance', subtitle: 'Dark'),
              SettingRow(icon: Icons.notifications_none, title: 'Notifications'),
              SettingRow(icon: Icons.memory, title: 'Memory', subtitle: 'Local + durable'),
              SettingRow(icon: Icons.lock_outline, title: 'Privacy & Security'),
            ])),
            const SizedBox(height: 12),
            const HermesCard(child: Column(children: [
              SettingRow(icon: Icons.dns_outlined, title: 'Backend', subtitle: 'OmniRoute · OpenAI compatible'),
              SettingRow(icon: Icons.route, title: 'Model routing', subtitle: 'Automatic fallback'),
              SettingRow(icon: Icons.monitor_heart_outlined, title: 'System health', subtitle: 'All systems nominal'),
            ])),
            const SizedBox(height: 12),
            Container(decoration: BoxDecoration(color: HermesColors.elevated, borderRadius: BorderRadius.circular(20)), child: const SettingRow(icon: Icons.logout, title: 'Log out')),
          ],
        ),
      );
}
