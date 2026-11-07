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

package cn.universal.core.downlink;

import cn.hutool.json.JSONObject;
import cn.universal.common.domain.R;
import cn.universal.core.message.UnifiedDownlinkCommand;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

/**
 * 下行处理上下文 贯穿整个下行处理流程，用于拦截器间传递数据
 *
 * @version 1.0
 * @since 2025/10/24
 */
@Data
public class DownlinkContext<T> {

  /** 统一命令对象（推荐使用，类型安全） */
  private UnifiedDownlinkCommand command;

  /** 原始消息（字符串格式） */
  private String rawMessage;

  /** 原始消息（JSON格式） */
  private JSONObject jsonMessage;

  /** 转换后的请求对象 */
  private T downRequest;

  /** 处理结果 */
  private R<?> result;

  /** 上下文属性（用于拦截器间传递数据） */
  private Map<String, Object> attributes = new HashMap<>();

  /** 协议类型代码 */
  private String protocolCode;

  /** 协议名称 */
  private String protocolName;

  /** 开始时间戳 */
  private long startTime;

  /** 结束时间戳 */
  private long endTime;

  /** 是否被拦截 */
  private boolean intercepted = false;

  /** 中断原因 */
  private String interruptReason;

  /** 异常信息 */
  private Exception exception;

  /**
   * 设置属性
   *
   * @param key 属性键
   * @param value 属性值
   */
  public void setAttribute(String key, Object value) {
    if (attributes == null) {
      attributes = new HashMap<>();
    }
    attributes.put(key, value);
  }

  /**
   * 获取属性
   *
   * @param key 属性键
   * @return 属性值
   */
  @SuppressWarnings("unchecked")
  public <V> V getAttribute(String key) {
    if (attributes == null) {
      return null;
    }
    return (V) attributes.get(key);
  }

  /**
   * 获取属性（带默认值）
   *
   * @param key 属性键
   * @param defaultValue 默认值
   * @return 属性值或默认值
   */
  @SuppressWarnings("unchecked")
  public <V> V getAttribute(String key, V defaultValue) {
    if (attributes == null) {
      return defaultValue;
    }
    return (V) attributes.getOrDefault(key, defaultValue);
  }

  /**
   * 移除属性
   *
   * @param key 属性键
   */
  public void removeAttribute(String key) {
    if (attributes != null) {
      attributes.remove(key);
    }
  }

  /**
   * 检查是否包含属性
   *
   * @param key 属性键
   * @return 是否包含
   */
  public boolean hasAttribute(String key) {
    return attributes != null && attributes.containsKey(key);
  }

  /**
   * 获取处理耗时（毫秒）
   *
   * @return 耗时
   */
  public long getDuration() {
    if (startTime == 0) {
      return 0;
    }
    long end = endTime > 0 ? endTime : System.currentTimeMillis();
    return end - startTime;
  }

  /**
   * 标记为已拦截
   *
   * @param reason 拦截原因
   */
  public void markIntercepted(String reason) {
    this.intercepted = true;
    this.interruptReason = reason;
  }

  /**
   * 获取产品Key（从属性中提取）
   *
   * @return 产品Key
   */
  public String getProductKey() {
    return getAttribute("productKey");
  }

  /**
   * 设置产品Key
   *
   * @param productKey 产品Key
   */
  public void setProductKey(String productKey) {
    setAttribute("productKey", productKey);
  }

  /**
   * 获取设备ID（从属性中提取）
   *
   * @return 设备ID
   */
  public String getDeviceId() {
    return getAttribute("deviceId");
  }

  /**
   * 设置设备ID
   *
   * @param deviceId 设备ID
   */
  public void setDeviceId(String deviceId) {
    setAttribute("deviceId", deviceId);
  }

  /**
   * 获取IoT ID（从属性中提取）
   *
   * @return IoT ID
   */
  public String getIotId() {
    return getAttribute("iotId");
  }

  /**
   * 设置IoT ID
   *
   * @param iotId IoT ID
   */
  public void setIotId(String iotId) {
    setAttribute("iotId", iotId);
  }
}
