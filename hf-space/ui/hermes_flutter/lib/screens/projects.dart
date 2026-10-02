import 'package:flutter/material.dart';
import '../core/theme.dart';
import '../widgets/ui.dart';

class ProjectsScreen extends StatelessWidget {
  const ProjectsScreen({super.key});
  @override
  Widget build(BuildContext context) => Scaffold(
        backgroundColor: HermesColors.bg,
        appBar: AppBar(backgroundColor: HermesColors.bg, title: const SerifTitle('Projects', size: 28), actions: [IconButton(onPressed: () {}, icon: const Icon(Icons.add))]),
        body: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            HermesCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Row(children: [Icon(Icons.folder, color: HermesColors.accent), SizedBox(width: 10), Text('Hermes Agent', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600)), Spacer(), StatusChip('Active', color: HermesColors.success)]),
                  const SizedBox(height: 10),
                  const Text('Autonomous assistant, Android client and server infrastructure.', style: TextStyle(color: HermesColors.secondary)),
                  const SizedBox(height: 14),
                  const Wrap(spacing: 8, children: [StatusChip('GitHub'), StatusChip('Notion'), StatusChip('3 agents', color: HermesColors.accent)]),
                ],
              ),
            ),
            const SizedBox(height: 12),
            const HermesCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Project workspace', style: TextStyle(fontWeight: FontWeight.w600)),
                  SizedBox(height: 14),
                  SettingRow(icon: Icons.code, title: 'GitHub', subtitle: 'Repository connected'),
                  SettingRow(icon: Icons.description_outlined, title: 'Notion', subtitle: 'Project knowledge connected'),
                  SettingRow(icon: Icons.folder_open, title: 'Files', subtitle: 'Workspace and artifacts'),
                  SettingRow(icon: Icons.timeline, title: 'Activity', subtitle: 'Tasks, agents and releases'),
                ],
              ),
            ),
          ],
        ),
      );
}
