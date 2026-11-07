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

package cn.universal.mqtt.protocol.entity;

import cn.universal.mqtt.protocol.config.MqttConstant;
import cn.universal.persistence.base.BaseUPRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * MQTT消息实体 - 贯穿整个处理链路的核心数据对象
 *
 * <p>包含MQTT消息处理过程中的所有上下文信息，支持处理器链模式
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/20
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "MQTT上行请求")
public class MQTTUPRequest extends BaseUPRequest {

  /** 回复上层平台的 */
  @Schema(description = "回复上层平台的请求列表")
  private List<BaseUPRequest> upRequestList;

  /** 消息唯一标识 */
  @Schema(description = "消息唯一标识")
  private String messageId;

  /** MQTT主题 */
  @Schema(description = "MQTT主题")
  private String upTopic;

  /** QoS级别 */
  @Builder.Default
  @Schema(description = "QoS级别")
  private int qos = 1;

  /** 是否保留消息 */
  @Schema(description = "是否保留消息")
  private boolean retained;

  /** 是否重复消息 */
  @Schema(description = "是否重复消息")
  private boolean duplicate;

  /** 设备唯一标识 */
  @Schema(description = "设备唯一标识")
  private String deviceUniqueId;

  /** 处理阶段标记 */
  @Schema(description = "处理阶段标记")
  private transient ProcessingStage stage;

  /** 处理上下文 - 用于在处理器间传递自定义数据 */
  @Builder.Default
  @Schema(description = "处理上下文")
  private transient Map<String, Object> mqttContext = new ConcurrentHashMap<>();

  /** 错误信息 */
  @Schema(description = "错误信息")
  private String errorMessage;

  /** 网络唯一标识（iot_product的network_union_id字段) */
  @Schema(description = "网络唯一标识")
  private String networkUnionId;

  /** 是否用的系统内置MQTT通道 */
  @Schema(description = "是否用的系统内置MQTT通道")
  private boolean isSysMQTTBroker;

  @Schema(description = "主题分类")
  private MqttConstant.TopicCategory topicCategory;

  /** 编解码上下文 */
  @Builder.Default
  @Schema(description = "编解码上下文")
  private Map<String, Object> codecContext = new ConcurrentHashMap<>();

  /** 设置编解码上下文 */
  public void setCodecContextValue(String key, Object value) {
    if (codecContext == null) {
      synchronized (this) {
        codecContext = new ConcurrentHashMap<>();
      }
    }
    if (key != null && value != null) {
      codecContext.put(key, value);
    }
  }

  /** 设置处理上下文 */
  public void setContextValue(String key, Object value) {
    if (key != null && value != null) {
      mqttContext.put(key, value);
    }
  }

  /** 获取处理上下文 */
  @SuppressWarnings("unchecked")
  public <T> T getContextValue(String key) {
    return (T) mqttContext.get(key);
  }

  /** 检查是否包含上下文 */
  public boolean hasContextValue(String key) {
    return mqttContext.containsKey(key);
  }

  /** 移除上下文 */
  public void removeContextValue(String key) {
    mqttContext.remove(key);
  }

  /** 获取设备唯一标识 */
  public String getDeviceUniqueId() {
    if (getProductKey() != null && getDeviceId() != null) {
      return getProductKey() + ":" + getDeviceId();
    }
    return null;
  }

  /** 设置错误状态 */
  public void setError(String errorMessage) {
    this.errorMessage = errorMessage;
    this.stage = ProcessingStage.ERROR;
  }
}
