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

package cn.universal.dm.device.shadow;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

/**
 * 设备影子包装器 提供便捷的设备影子数据访问方法
 *
 * <p>影子结构说明：
 *
 * <pre>
 * {
 *   "state": {
 *     "desired": { ... },    // 期望值
 *     "reported": { ... }    // 上报值
 *   },
 *   "metadata": {
 *     "desired": { ... },
 *     "reported": { ... }
 *   },
 *   "timestamp": 1765454964,
 *   "version": 25404
 * }
 * </pre>
 *
 * <p>使用示例：
 *
 * <pre>
 * // 从Service获取影子并包装
 * JSONObject shadowJson = ioTDeviceShadowService.getDeviceShadowObj(productKey, deviceId);
 * DeviceShadowWrapper shadow = new DeviceShadowWrapper(shadowJson);
 *
 * // 便捷获取值
 * String meterNo = shadow.getReported("meterNo");
 * Integer voltage = shadow.getReported("voltage", Integer.class);
 * String host = shadow.getDesired("host", "默认值");
 *
 * // 批量获取
 * Map<String, Object> params = shadow.getReportedBatch("meterNo", "voltage", "current");
 * </pre>
 *
 * @version 1.0
 * @since 2025/12/11
 */
@Data
public class DeviceShadowWrapper {

  /** 原始影子JSON对象 */
  private final JSONObject rawShadow;

  /** state.reported */
  private JSONObject reported;

  /** state.desired */
  private JSONObject desired;

  /** metadata.reported */
  private JSONObject reportedMetadata;

  /** metadata.desired */
  private JSONObject desiredMetadata;

  /** 时间戳 */
  private Long timestamp;

  /** 版本号 */
  private Integer version;

  /**
   * 构造函数
   *
   * @param shadowJson 影子JSON对象
   */
  public DeviceShadowWrapper(JSONObject shadowJson) {
    this.rawShadow = shadowJson;
    if (shadowJson != null) {
      parseState(shadowJson);
      parseMetadata(shadowJson);
      this.timestamp = shadowJson.getLong("timestamp");
      this.version = shadowJson.getInt("version");
    }
  }

  /**
   * 从JSON字符串构造
   *
   * @param shadowJsonStr 影子JSON字符串
   */
  public DeviceShadowWrapper(String shadowJsonStr) {
    this(JSONUtil.parseObj(shadowJsonStr));
  }

  /** 解析state节点 */
  private void parseState(JSONObject shadowJson) {
    JSONObject state = shadowJson.getJSONObject("state");
    if (state != null) {
      this.reported = state.getJSONObject("reported");
      this.desired = state.getJSONObject("desired");
    }
  }

  /** 解析metadata节点 */
  private void parseMetadata(JSONObject shadowJson) {
    JSONObject metadata = shadowJson.getJSONObject("metadata");
    if (metadata != null) {
      this.reportedMetadata = metadata.getJSONObject("reported");
      this.desiredMetadata = metadata.getJSONObject("desired");
    }
  }

  // ==================== Reported 相关方法 ====================

  /**
   * 获取reported中的值
   *
   * @param key 属性键
   * @return 属性值，不存在返回null
   */
  public Object getReported(String key) {
    return reported != null ? reported.get(key) : null;
  }

  /**
   * 获取reported中的值（带类型转换）
   *
   * @param key 属性键
   * @param clazz 目标类型
   * @return 属性值，不存在返回null
   */
  public <T> T getReported(String key, Class<T> clazz) {
    return reported != null ? reported.get(key, clazz) : null;
  }

  /**
   * 获取reported中的值（带默认值）
   *
   * @param key 属性键
   * @param defaultValue 默认值
   * @return 属性值，不存在返回默认值
   */
  @SuppressWarnings("unchecked")
  public <T> T getReported(String key, T defaultValue) {
    if (reported == null) {
      return defaultValue;
    }
    Object value = reported.get(key);
    return value != null ? (T) value : defaultValue;
  }

  /**
   * 批量获取reported中的值
   *
   * @param keys 属性键列表
   * @return Map，键为属性名，值为属性值（不存在的键不包含在结果中）
   */
  public Map<String, Object> getReportedBatch(String... keys) {
    Map<String, Object> result = new HashMap<>();
    if (reported != null) {
      for (String key : keys) {
        Object value = reported.get(key);
        if (value != null) {
          result.put(key, value);
        }
      }
    }
    return result;
  }

  /**
   * 获取reported的时间戳
   *
   * @param key 属性键
   * @return 时间戳，不存在返回null
   */
  public Long getReportedTimestamp(String key) {
    if (reportedMetadata != null) {
      JSONObject meta = reportedMetadata.getJSONObject(key);
      return meta != null ? meta.getLong("timestamp") : null;
    }
    return null;
  }

  // ==================== Desired 相关方法 ====================

  /**
   * 获取desired中的值
   *
   * @param key 属性键
   * @return 属性值，不存在返回null
   */
  public Object getDesired(String key) {
    return desired != null ? desired.get(key) : null;
  }

  /**
   * 获取desired中的值（带类型转换）
   *
   * @param key 属性键
   * @param clazz 目标类型
   * @return 属性值，不存在返回null
   */
  public <T> T getDesired(String key, Class<T> clazz) {
    return desired != null ? desired.get(key, clazz) : null;
  }

  /**
   * 获取desired中的值（带默认值）
   *
   * @param key 属性键
   * @param defaultValue 默认值
   * @return 属性值，不存在返回默认值
   */
  @SuppressWarnings("unchecked")
  public <T> T getDesired(String key, T defaultValue) {
    if (desired == null) {
      return defaultValue;
    }
    Object value = desired.get(key);
    return value != null ? (T) value : defaultValue;
  }

  /**
   * 批量获取desired中的值
   *
   * @param keys 属性键列表
   * @return Map，键为属性名，值为属性值（不存在的键不包含在结果中）
   */
  public Map<String, Object> getDesiredBatch(String... keys) {
    Map<String, Object> result = new HashMap<>();
    if (desired != null) {
      for (String key : keys) {
        Object value = desired.get(key);
        if (value != null) {
          result.put(key, value);
        }
      }
    }
    return result;
  }

  /**
   * 获取desired的时间戳
   *
   * @param key 属性键
   * @return 时间戳，不存在返回null
   */
  public Long getDesiredTimestamp(String key) {
    if (desiredMetadata != null) {
      JSONObject meta = desiredMetadata.getJSONObject(key);
      return meta != null ? meta.getLong("timestamp") : null;
    }
    return null;
  }

  // ==================== 通用方法 ====================

  /**
   * 检查reported中是否包含某个键
   *
   * @param key 属性键
   * @return true-包含，false-不包含
   */
  public boolean hasReported(String key) {
    return reported != null && reported.containsKey(key);
  }

  /**
   * 检查desired中是否包含某个键
   *
   * @param key 属性键
   * @return true-包含，false-不包含
   */
  public boolean hasDesired(String key) {
    return desired != null && desired.containsKey(key);
  }

  /**
   * 优先从reported获取，不存在则从desired获取
   *
   * @param key 属性键
   * @return 属性值，都不存在返回null
   */
  public Object getReportedOrDesired(String key) {
    Object value = getReported(key);
    return value != null ? value : getDesired(key);
  }

  /**
   * 优先从desired获取，不存在则从reported获取
   *
   * @param key 属性键
   * @return 属性值，都不存在返回null
   */
  public Object getDesiredOrReported(String key) {
    Object value = getDesired(key);
    return value != null ? value : getReported(key);
  }

  /**
   * 批量获取值（优先从reported获取）
   *
   * @param keys 属性键列表
   * @return Map，键为属性名，值为属性值
   */
  public Map<String, Object> getBatch(String... keys) {
    Map<String, Object> result = new HashMap<>();
    for (String key : keys) {
      Object value = getReportedOrDesired(key);
      if (value != null) {
        result.put(key, value);
      }
    }
    return result;
  }

  /**
   * 检查影子是否为空
   *
   * @return true-影子为空，false-影子不为空
   */
  public boolean isEmpty() {
    return rawShadow == null || rawShadow.isEmpty();
  }

  /**
   * 获取reported的所有键值对
   *
   * @return Map，如果reported为空返回空Map
   */
  public Map<String, Object> getAllReported() {
    return reported != null ? new HashMap<>(reported) : new HashMap<>();
  }

  /**
   * 获取desired的所有键值对
   *
   * @return Map，如果desired为空返回空Map
   */
  public Map<String, Object> getAllDesired() {
    return desired != null ? new HashMap<>(desired) : new HashMap<>();
  }

  /**
   * 转换为JSON字符串
   *
   * @return JSON字符串
   */
  public String toJsonString() {
    return rawShadow != null ? rawShadow.toString() : "{}";
  }
}
