import 'package:flutter/material.dart';
import '../core/theme.dart';
import '../widgets/ui.dart';
import 'chat.dart';
import 'tasks.dart';
import 'projects.dart';
import 'settings.dart';
import 'voice.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});
  @override
  State<HomeScreen> createState() => _HomeState();
}

class _HomeState extends State<HomeScreen> {
  int index = 0;
  final pages = const [HomeBody(), TasksScreen(), ProjectsScreen(), SettingsScreen()];
  @override
  Widget build(BuildContext context) => Scaffold(
        body: SafeArea(child: pages[index]),
        bottomNavigationBar: NavigationBar(
          selectedIndex: index,
          onDestinationSelected: (i) => setState(() => index = i),
          backgroundColor: HermesColors.bg,
          indicatorColor: HermesColors.elevated,
          destinations: const [
            NavigationDestination(icon: Icon(Icons.chat_bubble_outline), selectedIcon: Icon(Icons.chat_bubble), label: 'Chat'),
            NavigationDestination(icon: Icon(Icons.task_alt_outlined), selectedIcon: Icon(Icons.task_alt), label: 'Tasks'),
            NavigationDestination(icon: Icon(Icons.folder_outlined), selectedIcon: Icon(Icons.folder), label: 'Projects'),
            NavigationDestination(icon: Icon(Icons.settings_outlined), selectedIcon: Icon(Icons.settings), label: 'Settings'),
          ],
        ),
      );
}

class HomeBody extends StatelessWidget {
  const HomeBody({super.key});
  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.fromLTRB(20, 18, 20, 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(width: 34, height: 34, decoration: const BoxDecoration(color: HermesColors.accent, shape: BoxShape.circle), child: const Icon(Icons.auto_awesome, size: 18)),
                const Spacer(),
                const StatusChip('Ready', color: HermesColors.success),
                IconButton(onPressed: () {}, icon: const Icon(Icons.menu)),
              ],
            ),
            const Spacer(),
            const SerifTitle('Good evening'),
            const SizedBox(height: 8),
            const Text('What can I help you accomplish?', style: TextStyle(color: HermesColors.secondary, fontSize: 16)),
            const SizedBox(height: 24),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                ActionPill(label: 'Read screen', icon: Icons.visibility_outlined, onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ChatScreen(prompt: 'Read what is currently on my screen.')))),
                ActionPill(label: 'Automate task', icon: Icons.auto_fix_high, onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ChatScreen(prompt: 'Help me automate a task.')))),
                ActionPill(label: 'Check memory', icon: Icons.memory, onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const ChatScreen(prompt: 'Show me my relevant memory.')))),
              ],
            ),
            const Spacer(),
            GestureDetector(
              onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const VoiceScreen())),
              child: const HermesCard(
                child: Row(
                  children: [
                    Icon(Icons.graphic_eq, color: HermesColors.accent),
                    SizedBox(width: 12),
                    Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Talk to Hermes', style: TextStyle(fontWeight: FontWeight.w600)), Text('Wake word, streaming voice and barge-in', style: TextStyle(color: HermesColors.secondary, fontSize: 13))])),
                    Icon(Icons.chevron_right, color: HermesColors.secondary),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 12),
            const Text('Self-hosted · OmniRoute', style: TextStyle(color: HermesColors.secondary, fontSize: 12)),
            const SizedBox(height: 10),
            const Composer(),
          ],
        ),
      );
}
