# NexIoT HTTP 接入

当前实现使用 `schemaVersion: 2`，目录 `cn-http-access-protocol`。旧 `cn-universal-http-protocol` 不再参与构建。

HTTP 网络保存接入边界：`accessCode`、入站认证、报文大小和速率限制、出站访问策略。厂商路径、设备身份识别、协议编解码、请求和响应转换由绑定的 HTTP 产品驱动负责，不能直接使用旧 schemaVersion 1 的 operations/authProfiles 配置。

设备上报入口：

```text
/iot/http/v2/{accessCode}
/iot/http/v2/{accessCode}/{routePath}
```

网络配置示例：

```json
{
  "schemaVersion": 2,
  "revision": 1,
  "accessCode": "meter-ingest",
  "inbound": {
    "auth": {"type": "API_KEY", "headerName": "X-API-Key", "value": "replace-with-a-private-key"},
    "maxBodyBytes": 262144,
    "rateLimitPerMinute": 600
  },
  "outbound": {
    "allowPrivateNetwork": false,
    "connectTimeoutMs": 5000,
    "requestTimeoutMs": 10000,
    "maxRequestBytes": 1048576,
    "maxResponseBytes": 4194304
  }
}
```

通过 Vue3 的“通信组件 → HTTP接入”创建网络，给 HTTP 产品绑定网络和产品驱动，然后增加设备。增加、修改和删除设备属于平台本地生命周期，不要求设备已经存在、不执行远端 encode/CONTROL；实际设备控制仍经过现有设备校验及产品驱动。

管理 API 位于 `/admin/http-access/networks`，提供 validate、status、reload、poll。入站支持 API-Key 与 HMAC-SHA256，出站策略负责私网和请求边界；认证材料由管理 API 脱敏。

`src/test` 覆盖配置、认证与重放、请求策略、注册冲突、驱动交互和设备生命周期。根目录 `validation/api_acceptance.py` 给出本轮独立本地环境的真实 API/驱动/HTTP 上报验收流程；它通过环境读取凭据，报告不记录认证材料。
