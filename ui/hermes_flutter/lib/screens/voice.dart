import 'package:flutter/material.dart';
import '../core/theme.dart';
import '../widgets/ui.dart';

class VoiceScreen extends StatefulWidget {
  const VoiceScreen({super.key});
  @override
  State<VoiceScreen> createState() => _VoiceState();
}

class _VoiceState extends State<VoiceScreen> {
  bool live = false;
  @override
  Widget build(BuildContext context) => Scaffold(
        backgroundColor: HermesColors.bg,
        body: SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              children: [
                Row(children: [IconButton(onPressed: () => Navigator.pop(context), icon: const Icon(Icons.close)), const Spacer(), const StatusChip('Hermes', color: HermesColors.accent)]),
                const Spacer(),
                SerifTitle(live ? 'Listening' : 'Talk to Hermes', size: 34),
                const SizedBox(height: 12),
                Text(live ? 'Say anything. I will listen and act.' : 'Wake phrase, streaming speech and interruption.', style: const TextStyle(color: HermesColors.secondary), textAlign: TextAlign.center),
                const SizedBox(height: 45),
                AnimatedContainer(
                  duration: const Duration(milliseconds: 300),
                  width: live ? 180 : 150,
                  height: live ? 180 : 150,
                  decoration: BoxDecoration(shape: BoxShape.circle, color: HermesColors.accent.withOpacity(.12), border: Border.all(color: HermesColors.accent.withOpacity(.5), width: 2)),
                  child: Center(child: Container(width: 90, height: 90, decoration: const BoxDecoration(shape: BoxShape.circle, color: HermesColors.accent), child: const Icon(Icons.auto_awesome, size: 40, color: Colors.white))),
                ),
                const Spacer(),
                Text(live ? 'Listening…' : 'Ready', style: const TextStyle(fontSize: 16)),
                const SizedBox(height: 20),
                GestureDetector(onTap: () => setState(() => live = !live), child: Container(width: 70, height: 70, decoration: BoxDecoration(shape: BoxShape.circle, color: live ? HermesColors.danger : HermesColors.elevated), child: Icon(live ? Icons.stop : Icons.mic, size: 28))),
                const SizedBox(height: 20),
                const Text('Barge-in enabled', style: TextStyle(color: HermesColors.secondary, fontSize: 12)),
              ],
            ),
          ),
        ),
      );
}
