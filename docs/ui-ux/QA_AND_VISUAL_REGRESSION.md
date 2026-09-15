# Hermes UI QA and Visual Regression

## Quality gates

Every UI feature passes four gates:

1. Functional QA
2. Accessibility QA
3. Visual QA
4. Integration QA

## Functional QA

Verify:
- Navigation
- Input
- Submit
- Streaming
- Task controls
- Approval controls
- Error handling
- Offline handling
- Reconnection
- Deep links
- Back behavior

## Accessibility QA

Verify:
- Focus order
- Screen reader labels
- Touch targets
- Contrast
- Keyboard behavior where applicable
- Reduced motion
- Non-color-only status

## Visual QA

For each supported screen:

```text
Approved reference/spec
 → Render at target viewport
 → Capture screenshot
 → Compare
 → Fix layout/type/spacing/state
 → Capture again
```

Compare:
- Surface colors
- Typography
- Baseline alignment
- Spacing
- Radius
- Border treatment
- Icon size
- Button height
- Composer geometry
- Navigation geometry
- Empty/loading/error states

## Responsive matrix

Minimum targets:
- Android phone portrait
- Android phone landscape
- Desktop narrow
- Desktop wide

## State matrix

Each major screen must be tested in:
- Loading
- Ready
- Running
- Waiting
- Approval
- Recovery
- Completed
- Failed
- Offline

## Regression rule

A UI change is not complete when it merely compiles. The resulting screen must remain consistent with the Hermes design system and the approved UX contract.

## Release checklist

- [ ] Feature contract updated
- [ ] Mobile tested
- [ ] Desktop tested where supported
- [ ] All states tested
- [ ] Accessibility checked
- [ ] Visual comparison passed
- [ ] No secret/credential data in logs
- [ ] Backend integration verified
- [ ] Notion updated
- [ ] GitHub documentation updated
