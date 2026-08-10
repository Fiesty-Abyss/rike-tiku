# Design — RIKE 理科学习辅助系统

Locked multi-page design foundation. Future UI work inherits this system and
amends it intentionally; `rike-tiku-frontend/tokens.css` is the source of truth.

## System

- Genre · modern-minimal
- Macrostructure · public Map / Diagram; authenticated Workbench
- Theme · incumbent cool cobalt, extended from the existing public portal
- Axes · cool paper / compact Chinese grotesk / cobalt signal
- Role variants · admin dense; teacher operational; student learning-focused

## Tokens

```css
:root {
  --color-paper: oklch(97.5% 0.008 248);
  --color-surface: oklch(99% 0.005 248);
  --color-ink: oklch(24% 0.035 249);
  --color-ink-soft: oklch(39% 0.034 249);
  --color-rule: oklch(86% 0.022 248);
  --color-accent: oklch(48% 0.145 251);
  --color-focus: oklch(58% 0.19 251);

  --font-display: "Microsoft YaHei UI", "Segoe UI Variable Display", sans-serif;
  --font-body: "PingFang SC", "Microsoft YaHei", sans-serif;
  --font-brand: "Segoe UI Variable Display", "Microsoft YaHei UI", sans-serif;
}
```

## Layout language

- 4pt spacing scale; page content uses fluid gutters and a 1420px maximum.
- Hairlines define structure; one quiet shadow is reserved for raised task surfaces.
- Page title, supporting copy and primary action form one consistent heading row.
- Avoid nested cards. Use panels for real task grouping and rules for internal sections.
- Tables remain dense on desktop and scroll inside their own boundary on narrow screens.

## Navigation

- Public · existing N9 edge-aligned portal navigation.
- Admin · persistent dark role rail on desktop; compact wrapped task menu on mobile.
- Student · light task navigation with subject-first home.
- Teacher · light workbench header with clear return paths.
- Every authenticated header exposes current role, profile and logout; multi-role accounts expose “切换身份”.

## CTA voice

- Primary · cobalt fill, 8px radius, explicit action verb.
- Secondary · surface or plain treatment with the same target height.
- Destructive · danger colour plus explicit destructive wording.
- Clickable labels remain one line; controls target at least 40px, 44px on touch layouts.

## Motion stance

- Public portal keeps its single restrained entrance sequence.
- Work pages use only state transitions and a short first-content settle; no marketing animation.
- Reduced-motion fallback removes spatial movement and keeps state changes at or below 150ms.

## State language

- Loading preserves the page shell and task location.
- Empty states name the missing data and keep the next legal action discoverable.
- Errors appear near the affected form or action and state a recovery step.
- Focus rings are immediate, high-contrast and shared across native and Element Plus controls.

## Responsive contract

- 1280px · full role navigation and normal data density.
- 1024px · tighter gutters and adaptive panels.
- 768px · single-column task composition and wrapped navigation.
- 390px · 44px touch targets, no document-level horizontal overflow, actions stack or wrap.
