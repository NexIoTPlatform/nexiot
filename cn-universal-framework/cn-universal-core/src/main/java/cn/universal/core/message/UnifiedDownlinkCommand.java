/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权,未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.core.message;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.DownCmd;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一下行指令模型 作为所有下行指令的标准化入口，统一参数格式，避免String/JSONObject来回转换
 *
 * <p>设计目标：
 *
 * <ul>
 *   <li>类型安全：通过强类型字段避免类型转换错误
 *   <li>参数统一：将分散的参数集中管理
 *   <li>易于扩展：通过extensions支持协议特定参数
 *   <li>向后兼容：支持从旧格式（String/JSONObject）构建
 * </ul>
 *
 * @version 1.0
 * @since 2025/10/25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedDownlinkCommand implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Set<String> RESERVED_ROOT_KEYS =
      Set.of(
          "productKey",
          "deviceId",
          "iotId",
          "cmd",
          "appUnionId",
          "applicationId",
          "tenantId",
          "deptId",
          "msgId",
          "timestamp",
          "detail",
          "deviceAddress",
          "gwProductKey",
          "gwDeviceId",
          "extDeviceId",
          "slaveAddress",
          "function",
          "properties",
          "event",
          "rawData",
          "data",
          "extensions",
          "deviceReuse",
          "isAdmin",
          "allowInsert",
          "messageType",
          "metadata");

  // ===== 核心参数（所有指令必须） =====

  /** 产品标识 */
  private String productKey;

  /** 设备ID */
  private String deviceId;

  /** IoT统一设备ID */
  private String iotId;

  /** 指令类型（设备增删改查、功能下发等） */
  private DownCmd cmd;

  // ===== 通用参数 =====

  /** 应用联合ID */
  private String appUnionId;

  /** 应用实例ID */
  private String applicationId;

  /** 租户ID */
  private String tenantId;

  /** 资源归属部门 */
  private Long deptId;

  /** 消息ID（用于追踪） */
  private String msgId;

  /** 时间戳（毫秒） */
  private Long timestamp;

  /** 备注说明 */
  private String detail;
  private String deviceAddress;
  // ===== 网关相关参数 =====

  /** 网关产品Key（子设备场景） */
  private String gwProductKey;

  /** 网关设备ID（子设备场景） */
  private String gwDeviceId;

  /** 扩展设备ID（兼容字段） */
  private String extDeviceId;

  /** 从站地址（Modbus等工业协议） */
  private String slaveAddress;

  // ===== 功能参数（可选） =====

  /** 功能调用参数（功能下发场景） */
  private Map<String, Object> function;

  /** 属性设置参数（属性设置场景） */
  private Map<String, Object> properties;

  /** 事件触发参数（事件上报场景） */
  private Map<String, Object> event;

  /** 原始数据（兼容旧系统，保留JSONObject格式） */
  private JSONObject rawData;

  // ===== 扩展参数（协议特定） =====

  /** 扩展字段（用于协议特定参数，避免污染核心字段） */
  @Builder.Default private Map<String, Object> extensions = new HashMap<>();

  // ===== 控制标志 =====

  /** 设备是否复用 */
  private boolean deviceReuse;

  /** 是否超级管理员 */
  private boolean isAdmin;

  /** 是否允许自动注册 */
  private boolean allowInsert;

  // ===== 元数据 =====

  /** 指令元数据（用于记录来源、优先级等） */
  @Builder.Default private CommandMetadata metadata = new CommandMetadata();

  /** 指令元数据 */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CommandMetadata implements Serializable {
    /** 来源（web/api/rule_engine等） */
    private String source;

    /** 优先级（0-最高 10-最低） */
    private Integer priority;

    /** 超时时间（毫秒） */
    private Long timeout;

    /** 重试次数 */
    private Integer retryCount;

    /** 是否需要响应 */
    private Boolean requireResponse;
  }

  // ===== 工厂方法：支持多种构建方式 =====

  /**
   * 从JSONObject构建命令
   *
   * @param json JSON对象
   * @return 统一命令对象
   */
  public static UnifiedDownlinkCommand fromJson(JSONObject json) {
    if (json == null || json.isEmpty()) {
      throw new IllegalArgumentException("JSON对象不能为空");
    }

    UnifiedDownlinkCommand command = new UnifiedDownlinkCommand();

    // 核心字段
    command.setProductKey(json.getStr("productKey"));
    command.setDeviceId(json.getStr("deviceId"));
    command.setIotId(json.getStr("iotId"));

    // 指令类型
    String cmdStr = json.getStr("cmd");
    if (StrUtil.isNotBlank(cmdStr)) {
      DownCmd downCmd = DownCmd.find(cmdStr);
      command.setCmd(downCmd);
    }

    // 通用字段
    command.setAppUnionId(json.getStr("appUnionId"));
    command.setApplicationId(json.getStr("applicationId"));
    command.setTenantId(json.getStr("tenantId"));
    command.setDeptId(json.getLong("deptId"));
    command.setMsgId(json.getStr("msgId"));
    command.setTimestamp(json.getLong("timestamp", System.currentTimeMillis()));
    command.setDetail(json.getStr("detail"));

    // 网关相关
    command.setGwProductKey(json.getStr("gwProductKey"));
    command.setGwDeviceId(json.getStr("gwDeviceId"));
    command.setExtDeviceId(json.getStr("extDeviceId"));
    command.setSlaveAddress(json.getStr("slaveAddress"));
    command.setDeviceAddress(json.getStr("deviceAddress"));
    // 功能参数
    if (json.containsKey("function")) {
      command.setFunction(json.getJSONObject("function"));
    }
    if (json.containsKey("properties")) {
      command.setProperties(json.getJSONObject("properties"));
    }
    if (json.containsKey("event")) {
      command.setEvent(json.getJSONObject("event"));
    }

    // 控制标志
    command.setDeviceReuse(json.getBool("deviceReuse", false));
    command.setAdmin(json.getBool("isAdmin", false)); // 使用setAdmin而非setIsAdmin
    command.setAllowInsert(json.getBool("allowInsert", false));

    // 保存原始数据（用于协议特定处理）
    command.setRawData(json);

    // 提取扩展字段：
    // 1. 优先兼容平铺请求体中的自定义字段
    // 2. 继续兼容 data/extensions 中的动态参数
    command.setExtensions(extractExtensions(json));

    return command;
  }

  /**
   * 从字符串构建命令
   *
   * @param jsonString JSON字符串
   * @return 统一命令对象
   */
  public static UnifiedDownlinkCommand fromString(String jsonString) {
    if (StrUtil.isBlank(jsonString)) {
      throw new IllegalArgumentException("JSON字符串不能为空");
    }
    return fromJson(JSONUtil.parseObj(jsonString));
  }

  /**
   * 从Map构建命令
   *
   * @param map 参数Map
   * @return 统一命令对象
   */
  public static UnifiedDownlinkCommand fromMap(Map<String, Object> map) {
    if (map == null || map.isEmpty()) {
      throw new IllegalArgumentException("Map不能为空");
    }
    return fromJson(new JSONObject(map));
  }

  // ===== 链式调用方法（便于构建） =====

  /** 设置产品Key（链式调用） */
  public UnifiedDownlinkCommand withProductKey(String productKey) {
    this.productKey = productKey;
    return this;
  }

  /** 设置设备ID（链式调用） */
  public UnifiedDownlinkCommand withDeviceId(String deviceId) {
    this.deviceId = deviceId;
    return this;
  }

  /** 设置应用ID（链式调用） */
  public UnifiedDownlinkCommand withAppUnionId(String appUnionId) {
    this.appUnionId = appUnionId;
    return this;
  }

  /** 设置应用实例ID（链式调用） */
  public UnifiedDownlinkCommand withApplicationId(String applicationId) {
    this.applicationId = applicationId;
    return this;
  }

  /** 设置指令类型（链式调用） */
  public UnifiedDownlinkCommand withCmd(DownCmd cmd) {
    this.cmd = cmd;
    return this;
  }

  /** 添加扩展字段（链式调用） */
  public UnifiedDownlinkCommand addExtension(String key, Object value) {
    if (this.extensions == null) {
      this.extensions = new HashMap<>();
    }
    this.extensions.put(key, value);
    return this;
  }

  /** 设置元数据来源（链式调用） */
  public UnifiedDownlinkCommand withSource(String source) {
    this.metadata.setSource(source);
    return this;
  }

  // ===== 验证方法 =====

  /**
   * 验证必填参数
   *
   * @return 当前对象（支持链式调用）
   * @throws IllegalArgumentException 参数验证失败
   */
  public UnifiedDownlinkCommand validate() {
    if (StrUtil.isBlank(productKey)) {
      throw new IllegalArgumentException("productKey不能为空");
    }
    if (cmd == null) {
      throw new IllegalArgumentException("cmd不能为空");
    }

    // 根据不同指令类型验证特定参数
    switch (cmd) {
      case DEV_ADD:
      case DEV_DEL: // 使用DEV_DEL而非DEV_DELETE
      case DEV_UPDATE:
      case DEV_FUNCTION:
        if (StrUtil.isBlank(deviceId) && StrUtil.isBlank(iotId)) {
          throw new IllegalArgumentException(cmd.getValue() + "指令必须提供deviceId或iotId");
        }
        break;
      default:
        // 其他指令类型的验证逻辑可以后续扩展
        break;
    }

    return this;
  }

  /**
   * 转换为JSONObject（用于协议层处理）
   *
   * @return JSONObject对象
   */
  public JSONObject toJson() {
    JSONObject json = new JSONObject();

    // 核心字段
    json.set("productKey", productKey);
    json.set("deviceId", deviceId);
    json.set("iotId", iotId);
    json.set("cmd", cmd != null ? cmd.getValue() : null);

    // 通用字段
    json.set("appUnionId", appUnionId);
    json.set("applicationId", applicationId);
    json.set("tenantId", tenantId);
    json.set("deptId", deptId);
    json.set("msgId", msgId);
    json.set("timestamp", timestamp);
    json.set("detail", detail);

    // 网关相关
    json.set("gwProductKey", gwProductKey);
    json.set("gwDeviceId", gwDeviceId);
    json.set("extDeviceId", extDeviceId);
    json.set("slaveAddress", slaveAddress);

    // 功能参数
    if (function != null) {
      json.set("function", function);
    }
    if (properties != null) {
      json.set("properties", properties);
    }
    if (event != null) {
      json.set("event", event);
    }

    // 控制标志
    json.set("deviceReuse", deviceReuse);
    json.set("isAdmin", isAdmin);
    json.set("allowInsert", allowInsert);

    // 扩展字段
    if (extensions != null && !extensions.isEmpty()) {
      json.set("data", extensions);
    }

    return json;
  }

  /**
   * 转换为DownRequest（兼容旧格式）
   *
   * @return DownRequest对象
   */
  public DownRequest toDownRequest() {
    DownRequest request = new DownRequest();
    BeanUtil.copyProperties(this, request);
    return request;
  }

  /**
   * 获取扩展字段值
   *
   * @param key 字段名
   * @param <T> 值类型
   * @return 字段值
   */
  @SuppressWarnings("unchecked")
  public <T> T getExtension(String key) {
    return (T) extensions.get(key);
  }

  /**
   * 获取扩展字段值（带默认值）
   *
   * @param key 字段名
   * @param defaultValue 默认值
   * @param <T> 值类型
   * @return 字段值或默认值
   */
  @SuppressWarnings("unchecked")
  public <T> T getExtension(String key, T defaultValue) {
    return (T) extensions.getOrDefault(key, defaultValue);
  }

  private static Map<String, Object> extractExtensions(JSONObject json) {
    Map<String, Object> result = new LinkedHashMap<>();
    if (json == null || json.isEmpty()) {
      return result;
    }

    for (String key : json.keySet()) {
      if (key != null && !RESERVED_ROOT_KEYS.contains(key)) {
        result.put(key, json.get(key));
      }
    }

    mergeMapLikeObject(result, json.get("extensions"));
    mergeMapLikeObject(result, json.get("data"));
    return result;
  }

  private static void mergeMapLikeObject(Map<String, Object> target, Object source) {
    if (!(source instanceof Map<?, ?> map) || map.isEmpty()) {
      return;
    }
    map.forEach(
        (key, value) -> {
          if (key != null) {
            target.put(String.valueOf(key), value);
          }
        });
  }

  @Override
  public String toString() {
    return "UnifiedDownlinkCommand{"
        + "productKey='"
        + productKey
        + '\''
        + ", deviceId='"
        + deviceId
        + '\''
        + ", cmd="
        + cmd
        + ", msgId='"
        + msgId
        + '\''
        + '}';
  }
}
