# 下行消息拦截器系统

## 📖 概述

下行消息拦截器系统提供了一个优雅、可扩展的方式，在下行消息处理流程中注入自定义逻辑，无需修改现有的协议处理代码。

## 🏗️ 架构设计

```
┌──────────────────────────────────────────────────────────┐
│                    IDown.doAction()                       │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ↓
┌──────────────────────────────────────────────────────────┐
│          DownlinkInterceptorChain（拦截器链）             │
│                                                           │
│  ┌─────────────────────────────────────────────────┐    │
│  │  PRE 阶段（前置拦截器）                          │    │
│  │  - 数据校验 ValidationInterceptor               │    │
│  │  - 日志记录 LoggingInterceptor                  │    │
│  │  - 监控统计 MetricsInterceptor                  │    │
│  │  - 鉴权认证 AuthenticationInterceptor           │    │
│  │  - 限流控制 RateLimitInterceptor                │    │
│  └─────────────────────────────────────────────────┘    │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ↓
┌──────────────────────────────────────────────────────────┐
│            AbstractDownService.convert()                  │
│          （消息转换 + 上下文构建）                          │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ↓
┌──────────────────────────────────────────────────────────┐
│          DownlinkInterceptorChain（拦截器链）             │
│                                                           │
│  ┌─────────────────────────────────────────────────┐    │
│  │  MID 阶段（中置拦截器）                          │    │
│  │  - 影子服务 ShadowInterceptor                   │    │
│  │  - 编解码 CodecInterceptor                      │    │
│  │  - 数据增强 EnrichmentInterceptor               │    │
│  └─────────────────────────────────────────────────┘    │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ↓
┌──────────────────────────────────────────────────────────┐
│        XxxDownProcessorChain.process()                    │
│           （协议特定处理器链）                             │
└────────────────────────┬─────────────────────────────────┘
                         │
                         ↓
┌──────────────────────────────────────────────────────────┐
│          DownlinkInterceptorChain（拦截器链）             │
│                                                           │
│  ┌─────────────────────────────────────────────────┐    │
│  │  POST 阶段（后置拦截器）                         │    │
│  │  - 结果处理 ResultInterceptor                   │    │
│  │  - 通知推送 NotificationInterceptor             │    │
│  │  - 审计日志 AuditInterceptor                    │    │
│  └─────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────┘
```

## 🚀 快速开始

### 1. 创建自定义拦截器

```java
@Slf4j
@Component
@Order(100)  // 定义执行顺序
public class CustomDownlinkInterceptor implements DownlinkInterceptor {

    @Override
    public String getName() {
        return "自定义拦截器";
    }

    @Override
    public int getOrder() {
        return 100;  // 数字越小越先执行
    }

    @Override
    public InterceptorPhase getPhase() {
        return InterceptorPhase.PRE;  // 选择执行阶段：PRE, MID, POST
    }

    @Override
    public boolean supports(DownlinkContext<?> context) {
        // 判断是否支持当前上下文
        // 例如：仅处理MQTT协议
        return "mqtt".equals(context.getProtocolCode());
    }

    @Override
    public boolean preHandle(DownlinkContext<?> context) throws Exception {
        // 前置处理逻辑
        log.info("开始处理下行消息: {}", context.getProtocolCode());
        
        // 可以在上下文中存储数据
        context.setAttribute("customKey", "customValue");
        
        // 返回true继续执行，false中断执行
        return true;
    }

    @Override
    public void postHandle(DownlinkContext<?> context) throws Exception {
        // 后置处理逻辑
        log.info("下行消息处理完成: {}", context.getResult());
    }

    @Override
    public void afterCompletion(DownlinkContext<?> context, Exception ex) {
        // 完成处理（无论成功失败都会执行）
        if (ex != null) {
            log.error("处理异常", ex);
        }
        // 清理资源等操作
    }
}
```

### 2. 在协议实现中使用

拦截器会自动应用到所有实现了 `IDown` 接口的协议服务中。无需额外配置，只要将拦截器注册为 Spring Bean
即可。

## 📋 拦截器执行阶段

### PRE 阶段（前置阶段）

- **执行时机**：在消息转换（convert）之前
- **适用场景**：
    - ✅ 鉴权认证
    - ✅ 参数校验
    - ✅ 限流控制
    - ✅ 日志记录
    - ✅ 监控统计

### MID 阶段（中置阶段）

- **执行时机**：在消息转换（convert）之后，协议处理器（processor）之前
- **适用场景**：
    - ✅ 数据增强
    - ✅ 编解码处理
    - ✅ 影子服务集成
    - ✅ 业务规则校验

### POST 阶段（后置阶段）

- **执行时机**：在协议处理器（processor）执行之后
- **适用场景**：
    - ✅ 结果处理
    - ✅ 通知推送
    - ✅ 审计日志
    - ✅ 数据持久化

## 🔧 上下文对象

`DownlinkContext` 提供了以下关键信息：

```java
// 基本信息
context.getProtocolCode();      // 协议代码（如：mqtt, tcp, http）
context.getProtocolName();      // 协议名称
context.getRawMessage();        // 原始消息（字符串）
context.getJsonMessage();       // 原始消息（JSON）
context.getDownRequest();       // 转换后的请求对象

// 设备信息
context.getProductKey();        // 产品Key
context.getDeviceId();          // 设备ID
context.getIotId();             // IoT ID

// 处理信息
context.getResult();            // 处理结果
context.getDuration();          // 处理耗时（毫秒）
context.isIntercepted();        // 是否被拦截
context.getInterruptReason();   // 中断原因

// 自定义属性
context.setAttribute(key, value);  // 设置属性
context.getAttribute(key);         // 获取属性
context.removeAttribute(key);      // 移除属性
```

## 🎯 内置拦截器

### 1. DownlinkLoggingInterceptor（日志拦截器）

- **Order**: 1000
- **Phase**: PRE
- **功能**: 记录下行消息的详细日志

### 2. DownlinkMetricsInterceptor（监控拦截器）

- **Order**: 10
- **Phase**: PRE
- **功能**: 收集性能指标（耗时、成功率等）

### 3. DownlinkValidationInterceptor（校验拦截器）

- **Order**: 50
- **Phase**: PRE
- **功能**: 校验必要参数

## 💡 最佳实践

### 1. 拦截器命名规范

```java
// 建议使用：Downlink + 功能 + Interceptor
DownlinkAuthenticationInterceptor  // 鉴权拦截器
DownlinkRateLimitInterceptor       // 限流拦截器
DownlinkCodecInterceptor           // 编解码拦截器
```

### 2. Order 值建议

```
0-100:    系统级拦截器（鉴权、限流、监控）
100-500:  业务级拦截器（数据处理、编解码）
500-1000: 辅助级拦截器（日志、审计）
```

### 3. 错误处理

```java
@Override
public boolean preHandle(DownlinkContext<?> context) throws Exception {
    try {
        // 业务逻辑
        return true;
    } catch (Exception e) {
        log.error("拦截器执行异常", e);
        // 返回false会中断执行
        return false;
    }
}
```

### 4. 使用上下文传递数据

```java
// 在前置拦截器中存储数据
@Override
public boolean preHandle(DownlinkContext<?> context) {
    context.setAttribute("startTime", System.currentTimeMillis());
    return true;
}

// 在后置拦截器中获取数据
@Override
public void postHandle(DownlinkContext<?> context) {
    Long startTime = context.getAttribute("startTime");
    long duration = System.currentTimeMillis() - startTime;
    log.info("处理耗时: {}ms", duration);
}
```

### 5. 条件执行

```java
@Override
public boolean supports(DownlinkContext<?> context) {
    // 仅处理MQTT协议
    if (!"mqtt".equals(context.getProtocolCode())) {
        return false;
    }
    
    // 仅处理特定产品
    String productKey = context.getProductKey();
    return "specific-product".equals(productKey);
}
```

## 🔌 扩展示例

### 限流拦截器示例

```java
@Slf4j
@Component
@Order(20)
public class DownlinkRateLimitInterceptor implements DownlinkInterceptor {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public String getName() {
        return "限流拦截器";
    }

    @Override
    public int getOrder() {
        return 20;
    }

    @Override
    public InterceptorPhase getPhase() {
        return InterceptorPhase.PRE;
    }

    @Override
    public boolean preHandle(DownlinkContext<?> context) {
        String key = "rate_limit:" + context.getProtocolCode() + ":" + context.getProductKey();
        
        // 使用Redis实现简单的计数限流
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, 1, TimeUnit.SECONDS);
        }
        
        // 每秒最多100次
        if (count > 100) {
            log.warn("触发限流: key={}, count={}", key, count);
            return false;
        }
        
        return true;
    }

    @Override
    public void postHandle(DownlinkContext<?> context) {
        // 无需后置处理
    }
}
```

### 鉴权拦截器示例

```java
@Slf4j
@Component
@Order(30)
public class DownlinkAuthenticationInterceptor implements DownlinkInterceptor {

    @Resource
    private AuthService authService;

    @Override
    public String getName() {
        return "鉴权拦截器";
    }

    @Override
    public int getOrder() {
        return 30;
    }

    @Override
    public InterceptorPhase getPhase() {
        return InterceptorPhase.PRE;
    }

    @Override
    public boolean preHandle(DownlinkContext<?> context) {
        String productKey = context.getProductKey();
        String deviceId = context.getDeviceId();
        
        // 检查是否有权限下发指令
        if (!authService.hasPermission(productKey, deviceId)) {
            log.warn("鉴权失败: productKey={}, deviceId={}", productKey, deviceId);
            return false;
        }
        
        return true;
    }

    @Override
    public void postHandle(DownlinkContext<?> context) {
        // 无需后置处理
    }
}
```

## 🐛 调试和测试

### 启用调试日志

```yaml
logging:
  level:
    cn.universal.core.downlink: DEBUG
```

### 查看拦截器链信息

```java
@Resource
private DownlinkInterceptorChain interceptorChain;

// 获取拦截器数量
int count = interceptorChain.getInterceptorCount();

// 获取启用的拦截器数量
long enabledCount = interceptorChain.getEnabledInterceptorCount();

// 获取所有拦截器名称
List<String> names = interceptorChain.getInterceptorNames();
```

## 📝 注意事项

1. **性能考虑**：拦截器会在每次下行消息时执行，避免在拦截器中执行耗时操作
2. **异常处理**：拦截器中的异常会导致整个流程中断，需要妥善处理
3. **顺序控制**：通过 `@Order` 和 `getOrder()` 控制执行顺序
4. **阶段选择**：根据业务需求选择合适的执行阶段（PRE/MID/POST）
5. **条件判断**：使用 `supports()` 方法实现条件执行，避免不必要的处理

## 🔄 迁移指南

如果你需要将现有的下行处理逻辑迁移到拦截器模式：

1. **识别横切关注点**：日志、监控、鉴权等通用逻辑
2. **创建对应拦截器**：将通用逻辑封装为拦截器
3. **移除重复代码**：从各协议实现中移除已封装的逻辑
4. **测试验证**：确保拦截器正常工作

## 📚 更多资源

- [IDown 接口文档](./IDown.java)
- [DownlinkContext 上下文文档](./DownlinkContext.java)
- [DownlinkInterceptor 拦截器接口](./DownlinkInterceptor.java)
- [DownlinkInterceptorChain 拦截器链](./DownlinkInterceptorChain.java)
