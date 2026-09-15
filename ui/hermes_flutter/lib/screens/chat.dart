import 'package:flutter/material.dart';
import '../core/theme.dart';
import '../widgets/ui.dart';

class ChatScreen extends StatelessWidget {
  final String? prompt;
  const ChatScreen({super.key, this.prompt});
  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(backgroundColor: HermesColors.bg, title: const Text('Hermes'), actions: [const StatusChip('Thinking', color: HermesColors.accent), IconButton(onPressed: () {}, icon: const Icon(Icons.more_horiz))]),
        body: Column(
          children: [
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(20),
                children: [
                  if (prompt != null)
                    Align(alignment: Alignment.centerRight, child: Container(padding: const EdgeInsets.all(14), margin: const EdgeInsets.only(bottom: 18), decoration: BoxDecoration(color: HermesColors.elevated, borderRadius: BorderRadius.circular(20)), child: Text(prompt!))),
                  const Text('I can plan the task, execute the required tools, verify the result, and continue until the objective is complete.', style: TextStyle(fontSize: 16, height: 1.5)),
                  const SizedBox(height: 18),
                  HermesCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Row(children: [Icon(Icons.auto_awesome, color: HermesColors.accent), SizedBox(width: 8), Text('Working plan', style: TextStyle(fontWeight: FontWeight.w600))]),
                        const SizedBox(height: 14),
                        ...['Understand objective', 'Execute tools', 'Verify result', 'Finish and report'].asMap().entries.map((e) => Padding(padding: const EdgeInsets.symmetric(vertical: 7), child: Row(children: [Icon(e.key < 2 ? Icons.check_circle : Icons.radio_button_unchecked, size: 18, color: e.key < 2 ? HermesColors.success : HermesColors.secondary), const SizedBox(width: 10), Text(e.value)]))),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const Padding(padding: EdgeInsets.fromLTRB(12, 4, 12, 12), child: Composer()),
          ],
        ),
      );
}
