# Redis事件处理系统

## 架构图

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   EventPublisher│    │  Redis Pub/Sub   │    │RedisEventSubscriber│
│                 │───▶│                  │───▶│                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │EventProcessorFactory│
                       │                  │
                       └──────────────────┘
                                │
                ┌───────────────┼───────────────┐
                ▼               ▼               ▼
    ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
    │ProductFlushProc │ │ TcpDownProcessor│ │FenceDelayProcessor│
    │                 │ │                 │ │                 │
    └─────────────────┘ └─────────────────┘ └─────────────────┘
                │               │               │
                ▼               ▼               ▼
    ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
    │NettyAndCodec    │ │TcpDownRedis     │ │FenceDelayRedis  │
    │RedisHandler     │ │Handler          │ │Handler          │
    └─────────────────┘ └─────────────────┘ └─────────────────┘
```

## 事件流程

### 1. 事件发布

- `EventPublisher.publishEvent()` 发布事件到Redis
- 事件包含：事件类型、数据、节点ID、时间戳等

### 2. 事件订阅

- `RedisEventSubscriber` 监听Redis事件
- 使用 `EventTopics` 统一管理事件主题
- 支持模式匹配订阅（如 `tcp:command:*`）

### 3. 事件分发

- `RedisEventSubscriber` 接收事件后调用 `EventProcessorFactory`
- `EventProcessorFactory` 根据事件类型分发到对应的处理器

### 4. 事件处理

- 具体的处理器实现业务逻辑
- 处理器可以注入其他服务来处理具体业务

## 事件主题

| 事件类型    | 主题                       | 处理器                         | 说明         |
|---------|--------------------------|-----------------------------|------------|
| 协议更新    | `protocol:updated`       | -                           | 协议配置更新     |
| 电子围栏    | `fence:event`            | -                           | 电子围栏触发     |
| 电子围栏延迟  | `fence:delay`            | `FenceDelayRedisHandler`    | 电子围栏延迟处理   |
| TCP指令   | `tcp:command:*`          | -                           | TCP指令转发    |
| TCP下行指令 | `tcp:down:*`             | `TcpDownRedisHandler`       | TCP下行指令处理  |
| TCP推送   | `tcp:push:*`             | -                           | TCP主动推送    |
| 产品配置更新  | `product:config:updated` | -                           | 产品配置变更     |
| 产品刷新    | `product:flush`          | `NettyAndCodecRedisHandler` | 产品刷新和编解码重载 |
| 测试TCP   | `test:tcp:rel`           | -                           | 测试TCP功能    |

## 使用示例

### 发布事件

```java
// 发布协议更新事件
eventPublisher.publishEvent(EventTopics.PROTOCOL_UPDATED, eventData);

// 发布TCP指令事件（带实例ID）
eventPublisher.publishEvent(EventTopics.getTcpCommandTopic(instanceId), command);
```

### 订阅事件

```java
// 在RedisEventSubscriber中订阅
container.addMessageListener(
    new MessageListenerAdapter(this, "handleProtocolUpdated"),
    new ChannelTopic(EventTopics.PROTOCOL_UPDATED));
```

### 处理事件

```java
// 实现处理器接口
@Component
public class MyHandler implements ProductFlushProcessor {
    @Override
    public void handleProductFlushEvent(Object message) {
        // 处理产品刷新逻辑
    }
}
```

## 优势

1. **解耦**: 事件发布者和订阅者完全解耦
2. **扩展性**: 新增事件类型只需实现对应接口
3. **统一管理**: 所有事件主题通过 `EventTopics` 统一管理
4. **容错性**: 处理器不存在时不会影响其他事件处理
5. **集群支持**: 支持多实例集群部署，自动过滤自己发送的事件 