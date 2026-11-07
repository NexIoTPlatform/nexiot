# Magic IDE 同源免二次登录

## 行为

- 单独打开 `/magic/debug/index.html` 时，若当前 **同源**（协议、主机、端口均相同）已有主平台
  `localStorage.token`，先通过同源代理调用 `POST /api/admin/v1/magic-ide/session`（后端端点为 `/admin/v1/magic-ide/session`），成功后再挂载编辑器。
- 换票必须通过平台 JWT 校验，账号有效、租户一致，且具有 `protocol:protocol:codec` 或 `*:*:*` 权限。应用
  `client_credentials` 令牌不能换取交互式 IDE 会话。
- 换票返回 `token`、`userId`、`tenantId`、`workspaceAdmin`、`expiresIn`、`expiresAt`，禁止缓存；两种凭据均不放进
  URL。IDE 会话只存在前端内存中，不覆盖旧 `magic-token`。
- IDE 凭据最长 5 分钟，不能超过来源 JWT 的到期时间。前端每 30 秒检查，剩余不足 45 秒时重新换票并重连
  WebSocket。同账号平台 JWT 正常刷新保留编辑状态；账号/租户切换、退出、无权限或会话失效时关闭当前工作区，要求重新检查登录，不回退复用旧凭据。
- 没有主平台登录时保留原独立账号密码登录。已登录主平台也可通过工具栏或免登录失败页的「使用 Magic 账号登录」主动切换到 `index.html?login=magic`；该入口强制显示原账号密码表单，不自动复用旧 Magic 凭据，不清除主平台 token。登录页可切回主平台免登录。旧产品驱动 iframe/postMessage 桥接已按用户要求撤回，见下文。
- 独立登录是用户显式选择的兼容入口，不是 SSO 无权限、失效或换票失败时的自动降级。

## 权限与兼容边界

- 每次 SSO HTTP/WebSocket 认证均通过真实平台 `JwtDecoder` 和账号/权限服务重新验证来源身份，不是仅凭前端隐藏登录表单。
- 普通 SSO 用户的资源归属使用全局唯一 `unionId`；读取及保存、父文件夹和移动目标均校验归属。历史按用户名或无归属保存的资源不会自动公开给新
  SSO 身份。本次不自动迁移历史资源，无数据库变更。
- 非平台管理员禁止全局数据源、备份、上传、推送和重载等管理 API；前端不挂载全局备份历史页。平台管理员保留全局管理能力。
- 独立账号密码/DES 登录为既有兼容路径，不应将本次 SSO 的权限策略描述为对所有历史登录方式的全面加固。脚本执行本身不是安全沙箱。
- 会话在单 JVM 内存，进程重启后会失效。多实例部署需要粘性路由（换票、IDE HTTP、WebSocket
  同节点），或后续实现共享会话，不能直接声称任意负载均衡可用。
- 既有在线用户列表/协作广播仍是工作区全局能力，本次未实现租户级协作消息隔离，不代表整个 Magic
  工作区已完成多租户安全审计。
- 仅发布静态文件不够：前端、换票端点和插件鉴权代码必须成套更新。不同端口或 HTTP/HTTPS 不同不能共享
  localStorage；生产应通过 HTTPS 使用。

## 源码与重新打包

实际 IDE 工程为后端仓库的兄弟目录 `../magic-editor`，不是 SCADA 工程 `../nexiot-scada-vue`。

在后端仓库根目录执行：

```sh
# 可传入实际独立 IDE 工程目录；依赖需预先安装。
bash cn-universal-plugins/nexiot-web-ide-debug-plugins/tools/sync-magic-editor.sh ../magic-editor
mvn -pl cn-universal-web -am install -Dmaven.test.skip=true -Ppostgresql -nsu
```

同步脚本先运行前端单元测试和生产构建，再复制到插件 `src/main/resources/magic-editor`
。不清空目标目录，保留后端专用资源及旧哈希文件供缓存页面使用。请保持两个源码仓库的改动一起交付。

## 首轮本地回归与验收（2026-11-04，历史记录）

```sh
node --test ../magic-editor/tests/*.test.mjs
mvn -pl cn-universal-plugins/nexiot-web-ide-debug-plugins,cn-universal-web test \
  -Dmaven.test.skip=false \
  -Dtest=MagicSsoSessionServiceTest,CustomAuthorizationInterceptorSsoTest,MagicSsoWebSocketTest,MagicIdeSsoControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false -Ppostgresql -nsu
```

- 前端 12 项换票/并发/续期/退出/切换身份测试。
- 后端 25 项会话、权限、租户、资源归属、WebSocket、换票端点测试。
- Chromium 已对生产构建完成模拟接口的页面验收：同源启动、HTTP/WS 使用同一 IDE 凭据、不持久化凭据、JWT
  正常刷新、切换账号、平台退出、无权限、IDE 凭据失效、独立登录兼容。模拟接口测试不等同于远程环境验收。
- 全量 reactor 的 test-compile 被既有 HTTP 协议测试引用缺失的
  `HttpIntegrationConfig.HttpOperationConfig` 阻断；上述定向测试通过，全量打包跳过测试。
- 首轮当时仅完成本地构建和模拟验收，未发布。随后已完成真实同源部署及浏览器验收，见下文，不能将首轮记录理解为当前部署状态。

## 2026-11-04 真实同源部署修复

浏览器实测原请求 `POST /admin/v1/magic-ide/session` 被静态站点返回 HTTP 405。主平台通过 `/api/` 代理到后端，换票也必须使用该代理；仅修改 URL 后仍会被平台请求签名拦截器返回 HTTP 403，不能将它误判为未登录或无调试权限。

- 默认公开地址：`POST /api/admin/v1/magic-ide/session`，开发代理同步去掉 `/api` 前缀。
- 签名与 Vue3 平台一致：`POST`、后端路径 `/admin/v1/magic-ide/session`、空 query、秒级时间戳、每次唯一 nonce 用换行拼接，HMAC-SHA256 输出大写十六进制；携带 `X-Sign-Timestamp`、`X-Sign-Nonce`、`X-Signature`。普通 HTTP 下使用 CryptoJS，不依赖安全上下文的 `crypto.subtle`。
- 签名默认配置与主平台相同；自定义部署在构建时设置 `VITE_SIGN_SECRET`，或通过 `MAGIC_EDITOR_CONFIG.platformSignSecret` 配置（前端签名配置不是服务端身份凭据，不替代 JWT 鉴权）。
- 默认 `/api` 支持页面部署前缀。直接访问后端而无 `/api` 代理时，通过既有 editor config 设置 `MAGIC_EDITOR_CONFIG.platformApiPrefix = ''`；自定义代理前缀也必须是同源路径。客户端不因 401/403 而改走其他鉴权路由。
- HTTP 404/405 显示部署/代理错误，签名 403 与无权限 403 区分提示；保留手动 Magic 登录入口，不关闭签名校验或扩大授权白名单。
- 源码依赖记录在独立工程 `package.json` 和 `pnpm-lock.yaml`；发布仍运行同步脚本，将生产构建复制进插件资源，不能仅手工修改服务器。

## 2026-11-04 定向撤回旧产品驱动桥接

- 独立 IDE 仓库的 `dev` 与 `codex/magic-driver-workbench` 均指向 `dda2c01`（2026-09-23 10:40:50 +08:00）。旧桥接组件是未提交的本地文件，没有可单独 `git revert` 的提交；相关源文件的修改时间集中在 2026-09-24 上午，但文件时间不是提交时间。本次构建曾把这些旧本地改动一起带入测试服。
- 按 `dev` 对比后定向撤回：删除“应用到产品驱动”按钮、关联状态及“保存用于保留调试脚本；应用时核对正式驱动源码”提示，删除桥接组件、跨页面驱动传递、专用 iframe/postMessage 握手和 dirty 上报，恢复旧分支改动前的 API 信息面板及原有文案。不是整个分支重置。
- 保留：同源 SSO、Magic 独立登录入口、会话续期及 WebSocket 重连、正常脚本编辑/保存，以及与这套桥接无关的修复。SSO 重连事件独立命名为 `session-reconnect`。
- 不改动后来单独实现的平台原生驱动调试器、驱动版本管理、其接口或数据库迁移（后端 `611239e54`、`391417fff` 等提交不回退）。
- 定向撤回备份、逐文件摘要与 patch 保存在本机 `output/magic-ide-sso/driver-ui-rollback/`，仅用于追溯恢复，不属于交付界面或运行功能。
- 撤回后前端回归 23/23 通过，包含“旧桥接消失”和“SSO/独立登录/续期保留”两项回归。

## 真实同源验收记录

- 路由和签名修复已在测试服发布。使用有效的演示租户登录状态打开 `/magic/debug/index.html`，换票 HTTP 200，进入 IDE，WebSocket 调试服务器连接成功。
- `index.html?login=magic` 显示原账号密码表单；不发起 SSO 换票、不删除或覆盖主平台 token。通过页面入口可切回主平台免登录。
- 旧桥接撤回版的打包和远程验收结果以本机 `output/magic-ide-sso/driver-ui-rollback/` 下的验证记录为准；撤回前截图不能作为撤回完成证据。
