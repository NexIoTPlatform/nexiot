/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.databridge.plugin;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.databridge.engine.DataBridgeMagicScriptEngine;
import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.entity.ResourceConnection;
import cn.universal.databridge.enums.PluginStatus;
import cn.universal.persistence.base.BaseUPRequest;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据桥接插件抽象基类 提供公共方法和性能优化
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/15
 */
@Slf4j
public abstract class AbstractDataBridgePlugin implements DataBridgePlugin {

  @Resource protected DataBridgeMagicScriptEngine magicScriptEngine;

  // 性能优化：使用线程池进行异步处理
  private final ExecutorService executorService =
      Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

  protected PluginStatus status = PluginStatus.INITIALIZING;

  /** 初始化插件 */
  public void initialize() {
    try {
      log.info("初始化数据桥接插件: {}", getPluginInfo().getName());
      status = PluginStatus.RUNNING;
    } catch (Exception e) {
      log.error("插件初始化失败: {}", e.getMessage(), e);
      status = PluginStatus.ERROR;
    }
  }

  /** 销毁插件 */
  public void destroy() {
    log.info("销毁数据桥接插件: {}", getPluginInfo().getName());
    executorService.shutdown();
    status = PluginStatus.STOPPED;
  }

  /** 获取插件状态 */
  public PluginStatus getStatus() {
    return status;
  }

  /** 解析配置JSON */
  protected JSONObject parseConfig(DataBridgeConfig config) {
    if (StrUtil.isBlank(config.getConfig())) {
      return new JSONObject();
    }
    try {
      return JSONUtil.parseObj(config.getConfig());
    } catch (Exception e) {
      log.error("配置解析失败: {}", e.getMessage(), e);
      return new JSONObject();
    }
  }

  /** 执行Magic脚本 - 输出方向 (IoT -> 外部系统) */
  protected Object executeIotToYourScript(
      String script,
      BaseUPRequest request,
      DataBridgeConfig config,
      ResourceConnection connection) {
    try {
      return magicScriptEngine.executeIotToYourScript(script, request, config, connection);
    } catch (Exception e) {
      log.error("Magic脚本iotToYour执行失败: {}", e.getMessage(), e);
      throw new RuntimeException("Magic脚本iotToYour执行失败: " + e.getMessage(), e);
    }
  }

  /** 执行Magic脚本 - 输入方向 (外部系统 -> IoT) */
  protected Object executeYourToIotScript(
      String script, Object externalData, DataBridgeConfig config, ResourceConnection connection) {
    try {
      return magicScriptEngine.executeYourToIotScript(script, externalData, config, connection);
    } catch (Exception e) {
      log.error("Magic脚本yourToIot执行失败: {}", e.getMessage(), e);
      throw new RuntimeException("Magic脚本yourToIot执行失败: " + e.getMessage(), e);
    }
  }

  /** 验证Magic脚本 */
  protected boolean validateMagicScript(String script) {
    try {
      return magicScriptEngine.validateScript(script);
    } catch (Exception e) {
      log.error("Magic脚本验证失败: {}", e.getMessage(), e);
      return false;
    }
  }

  /** 异步执行任务 - 性能优化 */
  protected CompletableFuture<Void> executeAsync(Runnable task) {
    return CompletableFuture.runAsync(task, executorService);
  }

  /** 异步执行任务并返回结果 - 性能优化 */
  protected <T> CompletableFuture<T> executeAsync(java.util.function.Supplier<T> task) {
    return CompletableFuture.supplyAsync(task, executorService);
  }

  /** 构建模板变量 - 通用方法 */
  protected Map<String, Object> buildTemplateVariables(
      BaseUPRequest request, JSONObject configJson) {
    Map<String, Object> variables = new java.util.HashMap<>();
    if (request == null) {
      return variables;
    }

    // 基础字段 - 优先从request直接获取，如果取不到则从IoTDeviceDTO获取
    String deviceId = request.getDeviceId();
    if (deviceId == null && request.getIoTDeviceDTO() != null) {
      deviceId = request.getIoTDeviceDTO().getDeviceId();
    }

    String productKey = request.getProductKey();
    if (productKey == null && request.getIoTDeviceDTO() != null) {
      productKey = request.getIoTDeviceDTO().getProductKey();
    }

    String deviceName = request.getDeviceName();
    if (deviceName == null && request.getIoTDeviceDTO() != null) {
      deviceName = request.getIoTDeviceDTO().getDeviceName();
    }

    String iotId = request.getIotId();
    if (iotId == null && request.getIoTDeviceDTO() != null) {
      iotId = request.getIoTDeviceDTO().getIotId();
    }

    String applicationId = request.getApplicationId();
    if (applicationId == null && request.getIoTDeviceDTO() != null) {
      applicationId = request.getIoTDeviceDTO().getApplicationId();
    }

    String tenantId = request.getTenantId();
    if (tenantId == null && request.getIoTDeviceDTO() != null) {
      tenantId = request.getIoTDeviceDTO().getTenantId();
    }

    IoTConstant.DeviceNode deviceNode = request.getDeviceNode();
    if (deviceNode == null && request.getIoTDeviceDTO() != null) {
      deviceNode = request.getIoTDeviceDTO().getDeviceNode();
    }

    variables.put("deviceId", deviceId);
    variables.put("productKey", productKey);
    variables.put("deviceName", deviceName);
    variables.put("iotId", iotId);
    variables.put("applicationId", applicationId);
    variables.put("tenantId", tenantId);
    variables.put("deviceNode", deviceNode == null ? null : deviceNode.name());
    variables.put(
        "device", buildDeviceContext(request, deviceId, productKey, deviceName, iotId, deviceNode));
    variables.put("product", buildProductContext(request, productKey));
    variables.put("application", buildApplicationContext(request, applicationId));
    variables.put("properties", request.getProperties());
    variables.put("data", request.getData());
    variables.put("rawData", request.getRawData());
    variables.put("payload", request.getPayload());
    variables.put(
        "messageType", request.getMessageType() == null ? null : request.getMessageType().name());
    variables.put("time", request.getTime());
    variables.put("configJson", configJson == null ? new JSONObject() : configJson);
    // 处理字段映射
    if (configJson != null && configJson.containsKey("field_mapping")) {
      JSONObject fieldMapping = configJson.getJSONObject("field_mapping");
      fieldMapping.forEach(
          (key, value) -> {
            variables.put(key, value);
          });
    }

    return variables;
  }

  private Map<String, Object> buildDeviceContext(
      BaseUPRequest request,
      String deviceId,
      String productKey,
      String deviceName,
      String iotId,
      IoTConstant.DeviceNode deviceNode) {
    Map<String, Object> device = new java.util.LinkedHashMap<>();
    device.put("iotId", iotId);
    device.put("productKey", productKey);
    device.put("deviceId", deviceId);
    device.put("deviceName", deviceName);
    device.put("deviceNode", deviceNode == null ? null : deviceNode.name());
    if (request.getIoTDeviceDTO() != null) {
      device.put("extDeviceId", request.getIoTDeviceDTO().getExtDeviceId());
      device.put("state", request.getIoTDeviceDTO().getState());
      device.put("tenantId", request.getIoTDeviceDTO().getTenantId());
      device.put("scope", request.getIoTDeviceDTO().getScope());
    }
    return device;
  }

  private Map<String, Object> buildProductContext(BaseUPRequest request, String productKey) {
    Map<String, Object> product = new java.util.LinkedHashMap<>();
    product.put("productKey", productKey);
    if (request.getIoTProduct() != null) {
      product.put("productName", request.getIoTProduct().getName());
      product.put("thirdPlatform", request.getIoTProduct().getThirdPlatform());
      product.put("transportProtocol", request.getIoTProduct().getTransportProtocol());
      product.put("tenantId", request.getIoTProduct().getTenantId());
    }
    return product;
  }

  private Map<String, Object> buildApplicationContext(
      BaseUPRequest request, String applicationId) {
    Map<String, Object> application = new java.util.LinkedHashMap<>();
    application.put("applicationId", applicationId);
    if (request.getIoTDeviceDTO() != null) {
      application.put("appId", request.getIoTDeviceDTO().getAppId());
      application.put("scope", request.getIoTDeviceDTO().getScope());
      application.put("disabled", request.getIoTDeviceDTO().getAppDisable());
    }
    return application;
  }

  /** 处理模板 - 通用方法，支持嵌套属性访问 */
  protected String processTemplate(String template, Map<String, Object> variables) {
    if (template == null || template.trim().isEmpty()) {
      log.warn("模板为空，无法处理");
      return "";
    }

    // 使用正则表达式匹配所有 #{...} 占位符，支持嵌套属性
    String result = template;
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#\\{([a-zA-Z0-9_.]+)\\}");
    java.util.regex.Matcher matcher = pattern.matcher(template);

    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      String paramPath = matcher.group(1); // 获取变量路径，如 "properties.csq"
      Object value = getNestedValue(variables, paramPath);
      String replacement = value != null ? value.toString() : "";
      matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(sb);
    result = sb.toString();

    return result;
  }

  /** 获取嵌套属性值，支持如 properties.csq 这样的路径 */
  @SuppressWarnings("unchecked")
  protected Object getNestedValue(Map<String, Object> variables, String path) {
    if (path == null || path.trim().isEmpty()) {
      return null;
    }

    String[] keys = path.split("\\.");
    Object current = variables;

    for (String key : keys) {
      if (current == null) {
        return null;
      }

      if (current instanceof Map) {
        current = ((Map<String, Object>) current).get(key);
      } else if (current instanceof List<?> list && key.matches("\\d+")) {
        int index = Integer.parseInt(key);
        current = index >= 0 && index < list.size() ? list.get(index) : null;
      } else {
        // 如果不是Map/List类型，无法继续访问嵌套属性
        return null;
      }
    }

    return current;
  }

  /** 检查是否匹配过滤条件 - 通用方法 */
  protected boolean matchesFilterConditions(BaseUPRequest request, JSONObject filterConditions) {
    try {
      // 检查消息类型过滤
      if (filterConditions.containsKey("messageType")) {
        cn.hutool.json.JSONArray allowedTypes = filterConditions.getJSONArray("messageType");
        if (allowedTypes != null && !allowedTypes.contains(request.getMessageType().name())) {
          return false;
        }
      }

      // 检查产品过滤
      if (filterConditions.containsKey("productKey")) {
        String allowedProduct = filterConditions.getStr("productKey");
        String currentProduct =
            request.getIoTDeviceDTO() != null ? request.getIoTDeviceDTO().getProductKey() : null;
        if (!java.util.Objects.equals(allowedProduct, currentProduct)) {
          return false;
        }
      }

      return true;
    } catch (Exception e) {
      log.error("过滤条件检查失败: {}", e.getMessage(), e);
      return true; // 过滤条件错误时默认通过
    }
  }

  /** 过滤请求 - 通用方法 */
  protected List<BaseUPRequest> filterRequests(
      List<BaseUPRequest> requests, JSONObject configJson) {
    if (!configJson.containsKey("filter_conditions")) {
      return requests;
    }

    JSONObject filterConditions = configJson.getJSONObject("filter_conditions");
    List<BaseUPRequest> filteredRequests = new java.util.ArrayList<>();

    for (BaseUPRequest request : requests) {
      if (matchesFilterConditions(request, filterConditions)) {
        filteredRequests.add(request);
      }
    }

    return filteredRequests;
  }
}
