import 'package:flutter/material.dart';
import '../core/theme.dart';
import '../widgets/ui.dart';

class TasksScreen extends StatelessWidget {
  const TasksScreen({super.key});
  @override
  Widget build(BuildContext context) => Scaffold(
        backgroundColor: HermesColors.bg,
        appBar: AppBar(backgroundColor: HermesColors.bg, title: const SerifTitle('Tasks', size: 28)),
        body: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            HermesCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Row(children: [StatusChip('Running', color: HermesColors.action), Spacer(), Text('Step 2/4', style: TextStyle(color: HermesColors.secondary))]),
                  const SizedBox(height: 14),
                  const Text('Build the Hermes mobile interface', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 8),
                  const Text('Hermes is implementing components, checking states and preparing the next verification pass.', style: TextStyle(color: HermesColors.secondary, height: 1.4)),
                  const SizedBox(height: 14),
                  LinearProgressIndicator(value: .55, minHeight: 5, borderRadius: BorderRadius.circular(5), color: HermesColors.action, backgroundColor: HermesColors.border),
                  const SizedBox(height: 14),
                  const Wrap(spacing: 8, children: [ActionPill(label: 'View plan', icon: Icons.list_alt), ActionPill(label: 'Pause', icon: Icons.pause)]),
                ],
              ),
            ),
            const SizedBox(height: 12),
            ...['Prepare design tokens', 'Implement chat components', 'Run visual QA', 'Connect backend'].map((x) => Padding(padding: const EdgeInsets.only(bottom: 10), child: HermesCard(child: Row(children: [const Icon(Icons.radio_button_unchecked, color: HermesColors.secondary), const SizedBox(width: 12), Expanded(child: Text(x)), const Icon(Icons.chevron_right, color: HermesColors.secondary)])))),
          ],
        ),
      );
}
