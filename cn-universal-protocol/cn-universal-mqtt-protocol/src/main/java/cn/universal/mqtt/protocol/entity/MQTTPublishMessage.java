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

import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MQTT发布消息实体
 *
 * <p>用于表示需要发布到MQTT Broker的消息 支持完整的MQTT消息特性
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MQTT发布消息实体")
public class MQTTPublishMessage {

  /** 消息唯一标识 */
  @Schema(description = "消息唯一标识")
  private String messageId;

  /** 目标主题 */
  @Schema(description = "目标主题")
  private String topic;

  /** 消息载荷 */
  @Schema(description = "消息载荷")
  private byte[] payload;

  //   /**
  //    * 消息载荷字符串形式（可选）！！！有坑lombok没有走setContent
  //    */
  //   private String content;

  /** QoS级别 (0, 1, 2) */
  @Builder.Default
  @Schema(description = "QoS级别")
  private int qos = 1;

  /** 是否保留消息 */
  @Builder.Default
  @Schema(description = "是否保留消息")
  private boolean retained = false;

  /** 是否重复消息 */
  @Builder.Default
  @Schema(description = "是否重复消息")
  private boolean duplicate = false;

  /** 消息过期时间（秒） */
  @Schema(description = "消息过期时间（秒）")
  private Long messageExpiryInterval;

  /** 响应主题（MQTT 5.0） */
  @Schema(description = "响应主题")
  private String responseTopic;

  /** 相关数据（MQTT 5.0） */
  @Schema(description = "相关数据")
  private byte[] correlationData;

  /** 用户属性（MQTT 5.0） */
  @Schema(description = "用户属性")
  private Map<String, String> userProperties;

  /** 内容类型（MQTT 5.0） */
  @Schema(description = "内容类型")
  private String contentType;

  /** 载荷格式指示器（MQTT 5.0） */
  @Schema(description = "载荷格式指示器")
  private Integer payloadFormatIndicator;

  /** 消息创建时间 */
  @Builder.Default
  @Schema(description = "消息创建时间")
  private LocalDateTime createdTime = LocalDateTime.now();

  /** 发送时间 */
  @Schema(description = "发送时间")
  private LocalDateTime publishTime;

  /** 来源产品Key */
  @Schema(description = "来源产品Key")
  private String productKey;

  /** 来源设备ID */
  @Schema(description = "来源设备ID")
  private String deviceId;

  /** 消息类型 */
  @Schema(description = "消息类型")
  private String messageType;

  /** 优先级 */
  @Builder.Default
  @Schema(description = "优先级")
  private int priority = 0;

  /** 重试次数 */
  @Builder.Default
  @Schema(description = "重试次数")
  private int retryCount = 0;

  /** 最大重试次数 */
  @Builder.Default
  @Schema(description = "最大重试次数")
  private int maxRetries = 3;

  /** 是否需要确认 */
  @Builder.Default
  @Schema(description = "是否需要确认")
  private boolean ackRequired = false;

  /** 确认超时时间（毫秒） */
  @Builder.Default
  @Schema(description = "确认超时时间（毫秒）")
  private long ackTimeout = 30000;

  /** 扩展属性 */
  @Schema(description = "扩展属性")
  private Map<String, Object> attributes;

  /** 错误信息 */
  @Schema(description = "错误信息")
  private String errorMessage;

  /** 消息状态 */
  @Builder.Default
  @Schema(description = "消息状态")
  private MessageStatus status = MessageStatus.CREATED;

  /** 消息状态枚举 */
  public enum MessageStatus {
    CREATED, // 已创建
    PUBLISHING, // 发布中
    PUBLISHED, // 已发布
    ACKNOWLEDGED, // 已确认
    FAILED, // 发布失败
    EXPIRED, // 已过期
    CANCELLED // 已取消
  }

  /** 获取字符串形式的载荷 */
  public String getPayloadAsString() {
    return StrUtil.str(payload, StandardCharsets.UTF_8);
  }

  /** 获取字节形式的载荷 */
  public byte[] getPayloadAsBytes() {
    if (payload != null) {
      return payload;
    }
    return new byte[0];
  }

  /** 设置字节载荷 */
  public void setPayload(byte[] payload) {
    this.payload = payload;
  }

  /** 检查消息是否有效 */
  public boolean isValid() {
    return topic != null && !topic.trim().isEmpty() && (payload != null) && qos >= 0 && qos <= 2;
  }
}
