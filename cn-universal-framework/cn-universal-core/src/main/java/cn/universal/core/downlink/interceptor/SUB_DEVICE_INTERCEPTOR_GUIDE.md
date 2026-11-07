# 子设备拦截器使用指南

## 📖 概述

`DownlinkSubDeviceInterceptor` 是专门用于处理网关下子设备下行消息的拦截器。它在消息转换之后、业务处理之前执行（MID
阶段），order 值为 400。

## 🎯 适用场景

### 典型使用场景

1. **子设备路由判断**
    - 判断消息是否需要转发到子设备
    - 区分网关直接处理还是转发给子设备

2. **子设备鉴权验证**
    - 验证子设备是否有权接收指令
    - 检查子设备是否绑定到对应网关

3. **子设备状态检查**
    - 检查子设备是否在线
    - 验证子设备健康状态

4. **子设备ID映射**
    - 将网关设备ID映射到子设备ID
    - 处理设备层级关系

5. **协议转换准备**
    - 处理网关到子设备的协议差异
    - 准备子设备专用参数

## 🏗️ 架构设计

### 执行阶段

- **Phase**: `MID`（中置阶段）
- **Order**: `400`
- **时机**: 在消息转换之后，业务处理之前

### 执行流程

```
下行消息
    ↓
PRE 拦截器（校验、日志、监控）
    ↓
消息转换（convert）
    ↓
MID 拦截器 → 【子设备拦截器 Order:400】
    ↓           ├─ preHandle: 子设备路由判断
    ↓           ├─ preHandle: 子设备鉴权
    ↓           ├─ preHandle: 状态检查
    ↓           └─ preHandle: ID映射
    ↓
业务处理（ProcessorChain）
    ↓
POST 拦截器
    ↓           ├─ postHandle: 记录日志
    ↓           ├─ postHandle: 更新状态
    ↓           └─ afterCompletion: 资源清理
    ↓
完成
```

## 💡 快速开始

### 步骤1：启用拦截器

拦截器已自动注册为 Spring Bean，无需额外配置。默认 `supports()` 返回 `false`，需要修改：

```java
@Override
public boolean supports(DownlinkContext<?> context) {
    // 方式1：根据属性判断
    Boolean isSubDevice = context.getAttribute("isSubDevice");
    return Boolean.TRUE.equals(isSubDevice);
    
    // 方式2：根据产品类型判断
    // String productKey = context.getProductKey();
    // return productService.isGatewayProduct(productKey);
    
    // 方式3：根据设备ID格式判断
    // String deviceId = context.getDeviceId();
    // return deviceId != null && deviceId.contains("_"); // 例如：gateway_001
}
```

### 步骤2：实现业务逻辑

根据需要在以下方法中补充业务逻辑：

#### preHandle - 前置处理

```java
@Override
public boolean preHandle(DownlinkContext<?> context) throws Exception {
    // 1. 子设备路由判断
    if (!isGatewayOnline(context)) {
        log.warn("网关离线，无法下发到子设备");
        return false; // 中断执行
    }
    
    // 2. 子设备鉴权
    String subDeviceId = context.getAttribute("subDeviceId");
    if (!isSubDeviceBoundToGateway(subDeviceId, context.getDeviceId())) {
        log.warn("子设备未绑定到该网关");
        return false;
    }
    
    // 3. 填充子设备信息
    fillSubDeviceInfo(context);
    
    return true;
}
```

#### postHandle - 后置处理

```java
@Override
public void postHandle(DownlinkContext<?> context) throws Exception {
    // 1. 记录子设备指令日志
    SubDeviceCommandLog log = SubDeviceCommandLog.builder()
        .gatewayId(context.getDeviceId())
        .subDeviceId(context.getAttribute("subDeviceId"))
        .command(context.getRawMessage())
        .result(context.getResult())
        .createTime(new Date())
        .build();
    commandLogService.save(log);
    
    // 2. 更新子设备最后指令时间
    updateSubDeviceLastCommandTime(context);
}
```

#### afterCompletion - 完成处理

```java
@Override
public void afterCompletion(DownlinkContext<?> context, Exception ex) {
    if (ex != null) {
        // 记录错误
        SubDeviceError error = SubDeviceError.builder()
            .subDeviceId(context.getAttribute("subDeviceId"))
            .errorMessage(ex.getMessage())
            .createTime(new Date())
            .build();
        errorService.save(error);
        
        // 发送告警
        alertService.sendAlert("子设备下行失败", error);
    }
    
    // 清理资源
    context.removeAttribute("tempData");
}
```

### 步骤3：注入依赖服务

```java
@Slf4j
@Component
@Order(400)
public class DownlinkSubDeviceInterceptor implements DownlinkInterceptor {

    @Resource
    private SubDeviceService subDeviceService;
    
    @Resource
    private GatewayService gatewayService;
    
    @Resource
    private SubDeviceCommandLogService commandLogService;
    
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    
    // ... 实现方法
}
```

## 📋 完整示例

### 示例1：Modbus 网关子设备

```java
@Override
public boolean supports(DownlinkContext<?> context) {
    // 仅处理 Modbus 网关的子设备
    String protocol = context.getProtocolCode();
    Boolean isSubDevice = context.getAttribute("isSubDevice");
    return "modbus".equals(protocol) && Boolean.TRUE.equals(isSubDevice);
}

@Override
public boolean preHandle(DownlinkContext<?> context) throws Exception {
    // 1. 获取子设备信息
    String gatewayId = context.getDeviceId();
    Integer slaveId = context.getAttribute("slaveId"); // Modbus 从站地址
    
    // 2. 检查从站是否在线
    if (!modbusSlaveManager.isSlaveOnline(gatewayId, slaveId)) {
        log.warn("[Modbus子设备] 从站离线: gateway={}, slave={}", gatewayId, slaveId);
        return false;
    }
    
    // 3. 设置从站地址到上下文
    context.setAttribute("targetSlaveId", slaveId);
    
    log.info("[Modbus子设备] 路由到从站: gateway={}, slave={}", gatewayId, slaveId);
    return true;
}
```

### 示例2：MQTT 网关子设备

```java
@Override
public boolean supports(DownlinkContext<?> context) {
    // 仅处理 MQTT 网关的子设备
    String protocol = context.getProtocolCode();
    String deviceId = context.getDeviceId();
    // 假设子设备ID格式：gateway_001/subdevice_001
    return "mqtt".equals(protocol) && deviceId != null && deviceId.contains("/");
}

@Override
public boolean preHandle(DownlinkContext<?> context) throws Exception {
    String fullDeviceId = context.getDeviceId();
    String[] parts = fullDeviceId.split("/");
    
    if (parts.length != 2) {
        log.warn("[MQTT子设备] 设备ID格式错误: {}", fullDeviceId);
        return false;
    }
    
    String gatewayId = parts[0];
    String subDeviceId = parts[1];
    
    // 检查网关是否在线
    if (!mqttGatewayService.isOnline(gatewayId)) {
        log.warn("[MQTT子设备] 网关离线: {}", gatewayId);
        return false;
    }
    
    // 设置分离后的ID
    context.setAttribute("gatewayId", gatewayId);
    context.setAttribute("subDeviceId", subDeviceId);
    
    // 构造子设备专用 Topic
    String subTopic = String.format("/gateway/%s/subdevice/%s/down", gatewayId, subDeviceId);
    context.setAttribute("subDeviceTopic", subTopic);
    
    log.info("[MQTT子设备] 路由到: gateway={}, subDevice={}, topic={}", 
        gatewayId, subDeviceId, subTopic);
    
    return true;
}
```

### 示例3：带缓存的子设备鉴权

```java
@Resource
private RedisTemplate<String, String> redisTemplate;

private static final String SUB_DEVICE_CACHE_KEY = "subdevice:binding:";
private static final int CACHE_EXPIRE_SECONDS = 300; // 5分钟

@Override
public boolean preHandle(DownlinkContext<?> context) throws Exception {
    String gatewayId = context.getDeviceId();
    String subDeviceId = context.getAttribute("subDeviceId");
    
    // 1. 从缓存检查绑定关系
    String cacheKey = SUB_DEVICE_CACHE_KEY + gatewayId + ":" + subDeviceId;
    String cached = redisTemplate.opsForValue().get(cacheKey);
    
    boolean isBound;
    if ("1".equals(cached)) {
        // 缓存命中：已绑定
        isBound = true;
        log.debug("[子设备鉴权] 缓存命中: 已绑定");
    } else if ("0".equals(cached)) {
        // 缓存命中：未绑定
        isBound = false;
        log.debug("[子设备鉴权] 缓存命中: 未绑定");
    } else {
        // 缓存未命中：查询数据库
        isBound = subDeviceService.isBound(gatewayId, subDeviceId);
        
        // 更新缓存
        redisTemplate.opsForValue().set(
            cacheKey, 
            isBound ? "1" : "0", 
            CACHE_EXPIRE_SECONDS, 
            TimeUnit.SECONDS
        );
        
        log.debug("[子设备鉴权] 查询数据库: {}", isBound ? "已绑定" : "未绑定");
    }
    
    if (!isBound) {
        log.warn("[子设备鉴权] 子设备未绑定: gateway={}, subDevice={}", 
            gatewayId, subDeviceId);
        return false;
    }
    
    return true;
}
```

## 🔧 配置项

### 启用/禁用拦截器

```java
@Override
public boolean isEnabled() {
    // 方式1：通过配置文件控制
    // return environment.getProperty("downlink.interceptor.subdevice.enabled", 
    //                                 Boolean.class, true);
    
    // 方式2：直接返回
    return true; // 启用
}
```

### 调整执行顺序

```java
@Override
public int getOrder() {
    // 建议值：
    // - 在数据校验(50)之后
    // - 在编解码(100)之前
    // - 在其他业务拦截器(500+)之前
    return 400;
}
```

## 🐛 调试技巧

### 启用调试日志

```yaml
# application.yml
logging:
  level:
    cn.universal.core.downlink.interceptor.DownlinkSubDeviceInterceptor: DEBUG
```

### 查看拦截器执行情况

```java
@Override
public boolean preHandle(DownlinkContext<?> context) throws Exception {
    log.debug("[子设备拦截器] 开始处理");
    log.debug("  - Protocol: {}", context.getProtocolCode());
    log.debug("  - DeviceId: {}", context.getDeviceId());
    log.debug("  - ProductKey: {}", context.getProductKey());
    log.debug("  - Attributes: {}", context.getAttributes());
    
    // ... 业务逻辑
    
    return true;
}
```

## ⚠️ 注意事项

1. **性能考虑**
    - 避免在拦截器中执行耗时操作
    - 使用缓存减少数据库查询
    - 异步处理非关键逻辑

2. **异常处理**
    - `preHandle` 中的异常会导致整个流程中断
    - 使用 `try-catch` 妥善处理异常
    - 在 `afterCompletion` 中记录错误

3. **上下文传递**
    - 使用 `context.setAttribute()` 传递数据
    - 注意数据的生命周期
    - 在 `afterCompletion` 中清理临时数据

4. **条件判断**
    - `supports()` 方法要尽量轻量
    - 复杂判断放在 `preHandle()` 中
    - 避免重复查询

## 📚 相关文档

- [拦截器系统 README](../README.md)
- [协议服务集成指南](../INTEGRATION_GUIDE.md)
- [DownlinkInterceptor 接口](../DownlinkInterceptor.java)
- [DownlinkContext 上下文](../DownlinkContext.java)

## 🎯 下一步

1. 根据业务需求实现 `supports()` 方法
2. 填充 `preHandle()` 中的业务逻辑
3. 注入必要的服务依赖
4. 测试验证功能是否正常
5. 完善异常处理和日志记录

祝你使用愉快！ 🚀
