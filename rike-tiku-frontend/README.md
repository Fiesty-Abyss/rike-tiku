# rike-tiku-frontend

本工程是最小前端技术验证页面，只展示系统名称并读取后端及数据库健康状态，不包含正式角色工作台。

## 本地配置

复制 `.env.example` 为不受 Git 跟踪的 `.env.local`，按需修改：

```text
VITE_API_BASE_URL=http://localhost:8081/api/v1
```

## 启动

在 WebStorm 中打开本目录，使用 Node.js 24，执行：

```powershell
npm install
npm run dev
```

默认前端地址为 `http://localhost:8080`。

