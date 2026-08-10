# PR #27 UI browser evidence

Captured on 2026-08-10 from the production-like acceptance frontend (`vite preview`, 1280×900 unless the filename says `390`) against the isolated `rike_tiku_demo` database.

The automated browser used the demo-only CAPTCHA evidence path while the machine backend exposed `testCode`; this is recorded as `MACHINE_BROWSER_LOGIN_WITH_DEMO_TEST_CODE = PASS`, not as a human CAPTCHA pass. Before handoff, the database is reset and the backend is restarted with `testCode=false`.

Required views:

- `portal-1280.png`, `portal-390.png`
- `login-1280.png`, `login-390.png`
- `role-selection-1280.png`
- `admin-dashboard-1280.png`, `admin-classes-1280.png`, `admin-teachers-1280.png`, `admin-questions-1280.png`
- `student-dashboard-1280.png`
- `practice-single-1280.png`, `practice-multiple-1280.png`, `practice-fill-1280.png`
- `practice-result-one-by-one-1280.png`
- `wrong-questions-real-1280.png`
- `topic-learning-1280.png`, `topic-learning-analysis-1280.png`
- `teacher-scope-1280.png`, `teacher-high-frequency-dialog-1280.png`

The browser run verified one active primary navigation item, no visible horizontal overflow at the requested Portal/Login breakpoints, no blank page or failed dynamic route import, and no console error/warning. Password reset output and JWT values are intentionally absent from the evidence set.
