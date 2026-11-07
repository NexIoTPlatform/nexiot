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

package cn.universal.mqtt.protocol.topic;

import cn.universal.mqtt.protocol.config.MqttConstant;

/**
 * MQTT主题类型枚举
 *
 * <p>定义三种标准化的主题分类： - 物模型Topic: 属性上报、事件上报、下行控制 - 系统级Topic: 固件上报、固件更新 - 透传级Topic: 透传协议上行、下行
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
public enum MQTTTopicType {

  // ==================== 物模型Topic ====================

  /** 属性上报 格式: $thing/up/property/${productKey}/${deviceId} */
  THING_PROPERTY_UP(MqttConstant.TOPIC_THING_PREFIX + "/up/property", "物模型-属性上报", true),

  /** 事件上报 格式: $thing/up/event/${productKey}/${deviceId} */
  THING_EVENT_UP(MqttConstant.TOPIC_THING_PREFIX + "/up/event", "物模型-事件上报", true),

  /** 下行控制 格式: $thing/down/${productKey}/${deviceId} */
  THING_DOWN(MqttConstant.TOPIC_THING_PREFIX + "/down", "物模型-下行控制", false),

  // ==================== 系统级Topic ====================

  /** 固件上报 格式: $ota/report/${productKey}/${deviceId} */
  OTA_REPORT(MqttConstant.TOPIC_OTA_PREFIX + "/report", "系统级-固件上报", true),

  /** 固件更新 格式: $ota/update/${productKey}/${deviceId} */
  OTA_UPDATE(MqttConstant.TOPIC_OTA_PREFIX + "/update", "系统级-固件更新", false),

  // ==================== 透传级Topic ====================

  /** 透传协议上行 格式: $thing/up/${productKey}/${deviceId} */
  PASSTHROUGH_UP(MqttConstant.TOPIC_THING_PREFIX + "/up", "透传级-上行数据", true),

  /** 透传协议下行 格式: $thing/down/${productKey}/${deviceId} */
  PASSTHROUGH_DOWN(MqttConstant.TOPIC_THING_PREFIX + "/down", "透传级-下行数据", false);

  /** 主题前缀 */
  private final String topicPrefix;

  /** 主题描述 */
  private final String description;

  /** 是否为上行主题（设备->平台） */
  private final boolean isUpstream;

  MQTTTopicType(String topicPrefix, String description, boolean isUpstream) {
    this.topicPrefix = topicPrefix;
    this.description = description;
    this.isUpstream = isUpstream;
  }

  public String getTopicPrefix() {
    return topicPrefix;
  }

  public String getDescription() {
    return description;
  }

  public boolean isUpstream() {
    return isUpstream;
  }

  public boolean isDownstream() {
    return !isUpstream;
  }

  /**
   * 构建完整的主题路径
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @return 完整主题路径
   */
  public String buildTopic(String productKey, String deviceId) {
    return topicPrefix + "/" + productKey + "/" + deviceId;
  }

  /**
   * 构建网关子设备下行主题
   *
   * <p>格式: $thing/down/${gatewayProductKey}/${gatewayDeviceId}/sub/${subProductKey}/${subDeviceId}
   *
   * @param gatewayProductKey 网关产品Key
   * @param gatewayDeviceId 网关设备ID
   * @param subProductKey 子设备产品Key
   * @param subDeviceId 子设备设备ID
   * @return 子设备下行主题
   */
  public String buildSubDeviceTopic(
      String gatewayProductKey, String gatewayDeviceId, String subProductKey, String subDeviceId) {
    return topicPrefix
        + "/"
        + gatewayProductKey
        + "/"
        + gatewayDeviceId
        + "/sub/"
        + subProductKey
        + "/"
        + subDeviceId;
  }

  /**
   * 从主题中提取产品Key
   *
   * @param topic 主题路径
   * @return 产品Key，如果提取失败返回null
   */
  public String extractProductKey(String topic) {
    String[] parts = topic.split("/");
    // 格式: $thing/up/property/${productKey}/${deviceId}
    // 或: $ota/report/${productKey}/${deviceId}
    if (parts.length >= 4) {
      return parts[parts.length - 2]; // 倒数第二个部分是productKey
    }

    return null;
  }

  /**
   * 从主题中提取设备ID
   *
   * @param topic 主题路径
   * @return 设备ID，如果提取失败返回null
   */
  public String extractDeviceId(String topic) {
    String[] parts = topic.split("/");
    // 格式: $thing/up/property/${productKey}/${deviceId}
    // 或: $ota/report/${productKey}/${deviceId}
    if (parts.length >= 4) {
      return parts[parts.length - 1]; // 最后一个部分是deviceId
    }

    return null;
  }

  /**
   * 获取主题分类
   *
   * @return 主题分类
   */
  public MqttConstant.TopicCategory getCategory() {
    switch (this) {
      case THING_PROPERTY_UP:
      case THING_EVENT_UP:
      case THING_DOWN:
        return MqttConstant.TopicCategory.THING_MODEL;
      case OTA_REPORT:
      case OTA_UPDATE:
        return MqttConstant.TopicCategory.SYSTEM_LEVEL;
      case PASSTHROUGH_UP:
      case PASSTHROUGH_DOWN:
        return MqttConstant.TopicCategory.PASSTHROUGH;
      default:
        return MqttConstant.TopicCategory.UNKNOWN;
    }
  }
}
