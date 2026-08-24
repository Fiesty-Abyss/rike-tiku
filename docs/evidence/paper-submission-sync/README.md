# 试卷提交与教师同步机器浏览器证据

- 证据时间：2026-08-24（Asia/Shanghai）
- 分支：`fix/paper-submission-grading-sync`
- 数据库：`rike_tiku_demo`（真实 Demo 发布、学生、试卷和提交接口）
- 证据性质：`MACHINE_BROWSER_VERIFIED`
- 浏览器：隔离的 headless Chromium；未接管或清理用户 Chrome Profile
- 页面：`/student/papers/{releaseId}`、`/student/papers`、`/teacher/papers`

## 验证路径

1. 教师先打开 199 班物理试卷的“发布管理 → 作答情况”。
2. 从该 release 的未提交名单中选择一个真实 Demo 学生。
3. 学生完成两道单选和一道多选并提交；网络请求按发布题目 `itemId` 发送当前完整答案。
4. 学生详情重新读取 MySQL 提交事实，显示已提交、客观题自动得分、学生答案、正确答案、本题得分和 STANDARD。
5. 学生返回“我的试卷”后，同一 release 显示已提交和客观题得分。
6. 教师已打开的作答弹窗经 5 秒只读轮询重新读取 stats/submissions，出现该学生的 `SUBMITTED`、客观得分和“查看答卷”。
7. 教师答卷使用用户可读答案，不展示内部 JSON。

## 文件

- [`results.json`](results.json)：8 项断言、提交请求形状、console/page/request 失败记录。
- [`student-submitted-result.png`](student-submitted-result.png)：学生提交后的逐题结果。
- [`teacher-submission-synced.png`](teacher-submission-synced.png)：教师端同步后的已提交答卷。

## 结果与边界

结果为 8/8 assertions、0 console errors、0 page errors、0 failed requests、0 横向溢出。该证据证明真实 Demo 的浏览器/API/MySQL 闭环，不等同 `USER_MANUAL_ACCEPTANCE`，也不证明主观题自动评分；主观题仍只保存为 `SUBJECTIVE_PENDING`。
