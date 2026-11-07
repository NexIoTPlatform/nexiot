# 数据桥接模块

## 概述

数据桥接模块是一个可扩展的插件化系统，用于将IoT设备数据桥接到各种外部数据源，如MySQL、Kafka、MQTT、HTTP等。

## 特性

- **插件化架构**: 支持JDBC、Kafka、MQTT、HTTP等多种数据桥接插件
- **轻量级日志**: 执行日志仅打印到控制台，不存储数据库
- **动态配置**: 支持运行时启用/禁用插件
- **模板引擎**: 支持SQL、JSON、TEXT模板处理
- **异步处理**: 使用独立线程池异步处理数据桥接
- **资源中心**: 统一管理各种数据源连接配置

## 模块结构

```
cn-universal-data-bridge/
├── cn-universal-data-bridge-core/          # 核心模块
├── cn-universal-data-bridge-plugins/       # 插件模块
│   ├── cn-universal-data-bridge-plugin-jdbc/
│   ├── cn-universal-data-bridge-plugin-kafka/
│   ├── cn-universal-data-bridge-plugin-mqtt/
│   └── cn-universal-data-bridge-plugin-http/
├── cn-universal-data-bridge-starter/       # 自动配置
└── cn-universal-data-bridge-web/           # Web接口
```

## 配置

### 基础配置

```properties
# 启用数据桥接模块
databridge.enabled=true

# 插件启用配置
databridge.plugins.jdbc.enabled=true
databridge.plugins.kafka.enabled=true
databridge.plugins.mqtt.enabled=true
databridge.plugins.http.enabled=true
```

### 线程池配置

```properties
# 数据桥接线程池配置
databridge.executor.core-pool-size=4
databridge.executor.max-pool-size=16
databridge.executor.queue-capacity=1000
```

## API接口

### 插件管理

- `GET /api/databridge/plugins/status` - 获取插件状态
- `GET /api/databridge/plugins/info` - 获取插件信息

### 资源连接管理

- `GET /api/databridge/resources` - 获取资源连接列表
- `POST /api/databridge/resources` - 创建资源连接
- `PUT /api/databridge/resources/{id}/status` - 更新连接状态
- `DELETE /api/databridge/resources/{id}` - 删除资源连接
- `POST /api/databridge/resources/{id}/test` - 测试连接

### 桥接配置管理

- `GET /api/databridge/configs` - 获取桥接配置列表
- `POST /api/databridge/configs` - 创建桥接配置
- `PUT /api/databridge/configs/{id}/status` - 更新配置状态
- `DELETE /api/databridge/configs/{id}` - 删除桥接配置
- `POST /api/databridge/configs/{id}/validate` - 验证配置

## 使用示例

### 1. 创建资源连接

```json
POST /api/databridge/resources
{
  "name": "MySQL数据库",
  "type": "MYSQL",
  "host": "localhost",
  "port": 3306,
  "username": "root",
  "password": "password",
  "databaseName": "iot_data",
  "status": 1
}
```

### 2. 创建桥接配置

```json
POST /api/databridge/configs
{
  "name": "设备数据桥接",
  "sourceProductKey": "product001",
  "targetResourceId": 1,
  "bridgeType": "JDBC",
  "template": "INSERT INTO device_data (device_key, message_type, timestamp, properties) VALUES ('{deviceKey}', '{messageType}', '{timestamp}', '{properties}')",
  "status": 1
}
```

### 3. 模板变量

支持的模板变量：

- `{deviceKey}` - 设备ID
- `{productKey}` - 产品KEY
- `{messageType}` - 消息类型
- `{timestamp}` - 时间戳
- `{properties}` - 属性JSON
- `{property_xxx}` - 具体属性值

## 插件开发

### 实现DataBridgePlugin接口

```java
@Component("custom")
@ConditionalOnProperty(prefix = "databridge.plugins.custom", name = "enabled", havingValue = "true")
public class CustomDataBridgePlugin implements DataBridgePlugin {
    
    @Override
    public PluginInfo getPluginInfo() {
        return PluginInfo.builder()
            .name("自定义插件")
            .version("1.0.0")
            .description("自定义数据桥接插件")
            .author("开发者")
            .pluginType("CUSTOM")
            .supportedResourceTypes(List.of("CUSTOM"))
            .build();
    }
    
    // 实现其他方法...
}
```

## 集成说明

数据桥接模块已集成到 `IoTUPPushAdapter.afterPush` 方法中，当设备数据推送完成后会自动触发数据桥接处理。

## 注意事项

1. 插件默认启用，可通过配置禁用
2. 数据桥接采用异步处理，不影响主流程性能
3. 执行日志仅打印到控制台，不存储数据库
4. 支持批量处理，提高处理效率
5. 模板引擎支持变量替换和条件过滤
