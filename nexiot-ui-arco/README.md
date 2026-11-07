# NexIoT Vue3 控制台

开源版的 Vue3 / Arco Design 前端，包含 MQTT、HTTP 接入，产品、物模型、设备及系统管理。

## 开发

使用 Node.js 22 或更高版本，在本目录执行：

```bash
npm ci --no-audit --no-fund
npm run dev -- --port 8000
```

默认将 `/api` 代理到 `http://localhost:9092`。登录账号以根目录数据库初始化说明为准。
如需覆盖后端地址、地图服务或通用请求签名配置，创建不会提交到 Git 的 `.env.development.local`；不要提交真实凭据。地图 Key 默认为空。

## 检查与构建

```bash
npm run typecheck
node --test tests/*.test.mjs
npm run build
```

构建结果在 `dist/`。部署时将 `/api` 和 `/magic` 转发到后端；Vue Router 使用 Hash 模式。
