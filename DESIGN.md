# RIKE Aqua Liminal Future

Status: locked primary design system for PR #27. Internal theme name: `mizuiro-aero`.

## Product truth

RIKE is a long-session learning and teaching product for high-school physics, chemistry, and biology. Decoration must never outrank question text, analysis, tables, permissions, or factual state. The public portal may be atmospheric; authenticated workspaces remain efficient.

## Hallmark audit

The pre-redesign audit ranked these issues highest:

1. The portal followed a familiar hero → feature grid → three equal subject cards → CTA pattern, so its structure did not express the learning loop.
2. Student, teacher, and administrator surfaces shared too much of the same card-and-table language; role differences were mostly headings.
3. Nearly every block used the same white rounded rectangle, weakening hierarchy and creating generated-dashboard sameness.
4. Large quiet areas were not always intentional; some pages lacked a clear focal task or useful empty-state next action.
5. Filters, table identifiers, result review, and mobile navigation had inconsistent density and control widths.

## Reference study DNA

Apple public product pages were studied only as a public design reference. The retained DNA is macro-level: one focal idea per viewport, strong heading/body rhythm, generous but purposeful whitespace, progressive disclosure, material hierarchy, and motion that explains sequence. No copy, imagery, icons, font, device composition, or pixel layout is copied.

## Macrostructure

- Public portal: a five-act narrative, not a card catalogue — Water Horizon, Learning Current, Three Sciences, Three Workspaces, Entrance.
- Student: a learning cockpit with the current task dominant; answers and analysis live on solid reading surfaces.
- Teacher: a pre-dawn teaching studio optimized for scanning class scope, learning state, and high-frequency points.
- Administrator: a dense operational workbench inside a quieter architectural shell; tables and forms remain solid.
- Authentication: an airport-at-night service terminal with a direct route back to the portal.

## Colour and material

Semantic tokens live in `rike-tiku-frontend/src/styles/tokens.css`; the primary theme is in `themes/mizuiro-aero.css`.

- Canvas: water-mist white and cold blue-grey.
- Primary ink: deep ocean blue-black, never pure black.
- Physics: restrained cobalt and ice blue.
- Chemistry: muted periwinkle / mauve-grey, never saturated SaaS purple.
- Biology: jade and forest mist, never fluorescent green.

Material levels:

1. `surface-solid`: tables, forms, questions, answers, long text, logs.
2. `surface-glass`: top bars, login, role selection, floating filters, selected dashboard panels.
3. `optical-glass`: one or two signature portal/auth focal objects only.

Large `backdrop-filter` surfaces are limited and always have an opaque fallback. Repeated rows and answer options never use blur.

## Typography and spacing

- Humanist system Chinese typography; no remote font dependency.
- Display sizes use restrained fluid scaling; workspaces do not use marketing-sized headings.
- 4/8-based spacing; control height is at least 42px and 44px for coarse pointers.
- Reading columns stay near 68 Chinese characters; analysis and topic material prioritize line-height.

## Motion

GSAP is reserved for portal narrative reveal, auth/role entrance, and dashboard metric reveal. Vue component scopes use `gsap.context()`; media conditions use `gsap.matchMedia()`; unmount calls `revert()`. ScrollTrigger is registered once and only used by the portal. Motion uses opacity, x/y, and small scale. Reduced-motion renders content immediately and disables parallax, long stagger, and scroll choreography.

## Interaction rules

- Focus rings are always visible for keyboard interaction.
- One primary action per local task cluster.
- Wrong/danger states use restrained accents, not full saturated surfaces.
- Empty states state what is empty and offer the next legitimate action where one exists.
- Student main navigation has exactly one active item.
- Admin filters use responsive grids; actions are separate; identifiers do not break mid-token.

## Deliberate omissions

The optional `dark-deco` theme is documented as backlog, not implemented in PR #27. The primary theme, business blockers, Topic18, machine acceptance, screenshots, and final human acceptance take precedence; a second theme would expand QA without improving the required learning loop.

## Skill execution record

The installed skills actually discovered and read before visual editing were:

- `hallmark` — `D:/CodexHome/skills/hallmark/SKILL.md` plus its audit, study and redesign references;
- `impeccable` — `D:/CodexHome/skills/impeccable/SKILL.md` plus the relevant audit/critique/normalize/polish/distill guidance;
- `gsap-core`, `gsap-timeline`, `gsap-frameworks`, `gsap-performance`, and `gsap-scrolltrigger` — each installed `SKILL.md` was read in full.

Hallmark was applied as an audit → public Apple product-page study → redesign workflow. The study retained only macrostructure, typography rhythm, whitespace, progressive disclosure, material hierarchy and restrained motion. The resulting structural decision is recorded in `.hallmark/log.json` as “Narrative Workflow / Role-native Workbenches”.

Impeccable was applied as audit → critique → normalize → polish → distill. Its detector was run with:

```text
node D:\CodexHome\skills\impeccable\scripts\detect.mjs src
```

The first run found five thick side-stripe patterns. They were removed instead of suppressed; the final detector run reported no known findings. That result is only a static anti-pattern check, not the design-completion claim—the browser evidence remains the acceptance proof.

GSAP is used on the Portal narrative, Login/Role entrance and Admin Dashboard metric reveal. The shared Vue helper and page-local animations create `gsap.context()` after mount, use `gsap.matchMedia()`, and call `revert()` during unmount. ScrollTrigger is registered once in the application entry and is limited to the Portal. Practice question/result changes use short Vue transitions rather than another global animation system.

## Implemented tokens and signatures

Primary semantic colours use OKLCH: canvas `96.8% 0.018 214`, deep-ocean ink `24% 0.047 226`, aqua brand `55% 0.12 224`, physics cobalt `55% 0.16 253`, chemistry muted periwinkle `58% 0.09 296`, and biology jade `53% 0.105 163`. Element Plus tokens map to the same semantic source; page components do not branch on theme names.

The glass hierarchy is implemented as solid reading/work surfaces, limited 16px frosted interface glass, and optical accent glass only in Portal/Auth focal elements. Browsers without `backdrop-filter` receive a 98%-opaque cool surface through `@supports not`, so text contrast never depends on blur.

- Portal signature: the Water Horizon opens into a continuous learning-current narrative; physics, chemistry and biology use different original CSS/SVG spatial compositions instead of three copied cards.
- Student signature: the current question owns the visual centre; type rules, progress, answer surface, one-question result navigator and next-step actions form one continuous learning path.
- Teacher signature: subject-accented scope studios prioritize class, student, learning-state and high-frequency-point scanning instead of inheriting the admin shell.
- Administrator signature: a deep-ocean navigation rail surrounds solid, information-dense filters/tables; the real Dashboard adds one restrained optical layer without turning operational pages into marketing surfaces.

## Evidence

Production-like browser screenshots are stored under `docs/evidence/pr27-ui/`. They include the requested 1280px and 390px Portal/Login views, all three role workspaces, all three automatic question types, one-question result review, a real wrong-question list, Topic18 and the high-frequency-point dialog. The machine run reported no console error/warning and no failed dynamic import. Human CAPTCHA and final visual approval remain explicitly pending.
