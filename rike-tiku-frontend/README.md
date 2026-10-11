# rike-tiku-frontend

RIKE 理科学习辅助系统的 Web 前端：Vue 3 + TypeScript + Vite + Pinia + Vue Router + Element Plus，科学公式使用 KaTeX（含 mhchem），门户动效使用 GSAP。

## 本地配置

复制 `.env.example` 为不受 Git 跟踪的 `.env.local`，按需修改后端地址（必须包含 `/api/v1`）：

```text
VITE_API_BASE_URL=http://localhost:8081/api/v1
```

## 常用命令

需要 Node.js 22+（推荐 24）。

```bash
npm ci              # 安装依赖
npm run dev         # 开发服务器，默认 http://localhost:8080
npm run type-check  # vue-tsc 类型检查
npm test            # Vitest 单元测试
npm run build       # 生产构建，输出 dist/
npm run preview     # 预览生产构建
```

## 目录

```text
src/
├─ api/          # 按角色划分的 Axios 接口封装
├─ auth/         # 会话与令牌存储
├─ components/   # 通用组件（ScientificText、AnswerDisplay、StandardAnalysis 等）
├─ layouts/      # 学生 / 教师 / 管理员工作区布局
├─ router/       # 路由与角色守卫
├─ stores/       # Pinia 状态
├─ styles/       # 设计系统（RIKE Aqua Future）
└─ views/        # 页面：Portal、登录、学生、教师、管理员
```
