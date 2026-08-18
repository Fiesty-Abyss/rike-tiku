# V30 / PR34 正式机器浏览器验收证据

验收环境为本机独立 Chromium profile，前端 `http://localhost:8080`、后端 `http://localhost:8081`、正式数据库 `rike_tiku`。本目录记录的是机器浏览器验收，不代表用户逐页人工验收。

`results.json` 记录 56 项断言：0 断言失败、0 控制台错误、0 页面错误、0 非预期失败请求、0 横向溢出。验收覆盖物理、化学、生物专题学习；教师检索并手动组入专题主观大题、质量建议、发布、学生版与答案版打印；以及学生在含图片的主观题试卷中保存作答并提交。测试使用受控的 V30 本地验收数据，不记录账户凭据。

截图文件按页面和步骤命名。学生版不显示答案或 STANDARD；答案版显示 STANDARD；主观题在学生端明确标记为不自动评分，提交后为 `SUBJECTIVE_PENDING`，不计入客观题自动得分。内容同步后的补充复核确认：物理专题图片附件返回 HTTP 200 且显示，专题正文和 STANDARD 不再呈现字面量 `\\n`。

状态：`MACHINE_BROWSER_VERIFIED`。PR34-MA-001 的补充 handler 结果位于 `pr34-print-handler-results.json` 和 `pr34-print-*.png`：它们只验证 `BUTTON_CLICK → printPaper() → window.print()`，不声称 headless Chromium 打开了 OS 系统打印对话框。用户随后已在真实 Chrome 点击学生版预览并确认系统打印窗口打开，状态为 `PRINT_USER_VERIFIED` / `OS_PRINT_DIALOG_USER_VERIFIED`；未声称实际打印纸张。
