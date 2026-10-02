import 'package:flutter/material.dart';
import '../core/theme.dart';

class SerifTitle extends StatelessWidget {
  final String text;
  final double size;
  const SerifTitle(this.text, {super.key, this.size = 34});
  @override
  Widget build(BuildContext context) => Text(
        text,
        style: TextStyle(
          fontFamily: 'serif',
          fontSize: size,
          height: 1.08,
          color: HermesColors.text,
          fontWeight: FontWeight.w400,
        ),
      );
}

class HermesCard extends StatelessWidget {
  final Widget child;
  final EdgeInsets padding;
  const HermesCard({super.key, required this.child, this.padding = const EdgeInsets.all(18)});
  @override
  Widget build(BuildContext context) => Container(
        padding: padding,
        decoration: BoxDecoration(
          color: HermesColors.elevated,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: HermesColors.border),
        ),
        child: child,
      );
}

class StatusChip extends StatelessWidget {
  final String label;
  final Color color;
  final IconData? icon;
  const StatusChip(this.label, {super.key, this.color = HermesColors.secondary, this.icon});
  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 7),
        decoration: BoxDecoration(
          color: color.withOpacity(.10),
          borderRadius: BorderRadius.circular(999),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (icon != null) ...[
              Icon(icon, size: 14, color: color),
              const SizedBox(width: 6),
            ],
            Text(label, style: TextStyle(color: color, fontSize: 12, fontWeight: FontWeight.w600)),
          ],
        ),
      );
}

class ActionPill extends StatelessWidget {
  final String label;
  final VoidCallback? onTap;
  final IconData? icon;
  final bool primary;
  const ActionPill({super.key, required this.label, this.onTap, this.icon, this.primary = false});
  @override
  Widget build(BuildContext context) => OutlinedButton.icon(
        onPressed: onTap,
        icon: icon == null ? const SizedBox.shrink() : Icon(icon, size: 17),
        label: Text(label),
        style: OutlinedButton.styleFrom(
          backgroundColor: primary ? HermesColors.accent : Colors.transparent,
          foregroundColor: primary ? Colors.white : HermesColors.text,
          side: BorderSide(color: primary ? HermesColors.accent : HermesColors.border),
          shape: const StadiumBorder(),
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 13),
        ),
      );
}

class SettingRow extends StatelessWidget {
  final IconData icon;
  final String title;
  final String? subtitle;
  final Widget? trailing;
  final VoidCallback? onTap;
  const SettingRow({super.key, required this.icon, required this.title, this.subtitle, this.trailing, this.onTap});
  @override
  Widget build(BuildContext context) => ListTile(
        onTap: onTap,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 5),
        leading: Icon(icon, size: 21, color: HermesColors.secondary),
        title: Text(title),
        subtitle: subtitle == null ? null : Text(subtitle!, style: const TextStyle(color: HermesColors.secondary, fontSize: 13)),
        trailing: trailing ?? const Icon(Icons.chevron_right, color: HermesColors.secondary),
      );
}

class Composer extends StatelessWidget {
  final VoidCallback? onVoice;
  const Composer({super.key, this.onVoice});
  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 7),
        decoration: BoxDecoration(
          color: HermesColors.input,
          borderRadius: BorderRadius.circular(28),
          border: Border.all(color: HermesColors.border),
        ),
        child: Row(
          children: [
            IconButton(onPressed: () {}, icon: const Icon(Icons.add)),
            const Expanded(
              child: TextField(
                maxLines: 5,
                minLines: 1,
                decoration: InputDecoration(
                  hintText: 'Message Hermes',
                  filled: false,
                  border: InputBorder.none,
                  contentPadding: EdgeInsets.symmetric(horizontal: 8),
                ),
              ),
            ),
            IconButton(onPressed: onVoice, icon: const Icon(Icons.mic_none)),
            Container(
              width: 42,
              height: 42,
              decoration: const BoxDecoration(color: HermesColors.accent, shape: BoxShape.circle),
              child: const Icon(Icons.arrow_upward),
            ),
          ],
        ),
      );
}
