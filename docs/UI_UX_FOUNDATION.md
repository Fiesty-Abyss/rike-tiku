# UI/UX Foundation 审计与收口

## 审计发现

- Typography：公共门户与工作区缺少共享字级、行高和数字排版规则。
- Spacing：旧全局样式混用大量一次性像素值，页面节奏不一致。
- Hierarchy：学生、教师、管理员标题区与操作区结构不同，角色间连续性弱。
- Navigation：管理员窄屏菜单、学生窄屏导航和教师返回路径需要明确响应式策略。
- Density：表格操作、筛选区和附件编辑区在窄屏容易拥挤。
- Feedback：Element Plus 默认加载和空态可用，但缺少统一边界与恢复动作语言。
- Accessibility：需要统一 focus-visible、40–44px 点击目标、reduced-motion 和防横向溢出规则。
- Route continuity：Portal → Login → Role Selection → Workspace 的品牌、颜色和控件语言不够连续。

## 最终方向

沿用公共门户已经确立的冷色纸面、克制钴蓝信号色和细边界语言，不建立新的视觉世界。登录后采用 Workbench 式任务布局：管理员保持高信息密度，教师突出任课范围，学生突出学科与练习。普通业务页面不增加营销式滚动动画。

## 覆盖页面

- 公共：Portal、Login、Role Selection。
- 管理员：Layout、Dashboard、班级、学生、教师与任课、题库、操作日志。
- 学生：Home、Practice New、Practice Session、Result、Wrong Questions、掌握度、推荐。
- 教师：Home、Scope Workspace、高频考点、学情。
- 共享：Profile、Messages、Dialog、Table、Form、Empty/Error/Loading。

## 不变边界

本轮只调整前端表现和交互连续性，不改变数据库、Flyway、API contract、权限、题目状态机、评分、附件安全或业务路由。
