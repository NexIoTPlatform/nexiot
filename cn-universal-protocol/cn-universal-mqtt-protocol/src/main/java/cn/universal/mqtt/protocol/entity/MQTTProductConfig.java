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

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MQTT产品配置实体
 *
 * <p>包含MQTT连接、认证、主题订阅等所有配置信息 支持多种MQTT Broker类型和认证方式
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MQTT产品配置实体")
public class MQTTProductConfig {

  /** 网络标识 */
  @Schema(description = "网络标识")
  private String networkUnionId;

  /** 网络配置名称 */
  @Schema(description = "网络配置名称")
  private String networkName;

  /** 网络类型 */
  @Schema(description = "网络类型")
  private String networkType;

  /** 是否启用 */
  @Builder.Default
  @Schema(description = "是否启用")
  private boolean enabled = true;

  /** 创建时间 */
  @Schema(description = "创建时间")
  private LocalDateTime createdTime;

  /** 更新时间 */
  @Schema(description = "更新时间")
  private LocalDateTime updatedTime;

  // MQTT Broker配置
  /**
   * MQTT Broker地址
   *
   * <p>tcp://125.210.48.96:1883
   */
  @Schema(description = "MQTT Broker地址")
  private String host;

  /** 客户端ID */
  @Schema(description = "客户端ID")
  private String clientId;

  /**
   * 集群部署：开启后每个节点使用独立客户端ID，并通过MQTT共享订阅分摊消息；
   * 关闭时使用Redis租约保证单节点消费。
   */
  @Builder.Default
  @Schema(description = "集群部署")
  private boolean clusterEnabled = false;

  // 认证配置
  /** 用户名 */
  @Schema(description = "用户名")
  private String username;

  /** 密码 */
  @Schema(description = "密码")
  private String password;

  /** 是否启用SSL/TLS */
  @Builder.Default
  @Schema(description = "是否启用SSL/TLS")
  private boolean ssl = false;

  /** TLS认证模式：DISABLED、SERVER_AUTH、MUTUAL_AUTH */
  @Builder.Default
  @Schema(description = "TLS认证模式")
  private String sslMode = "DISABLED";

  /** CA证书标识（对应iot_certificate.ssl_key） */
  @Schema(description = "CA证书标识")
  private String sslCaCertKey;

  /** 客户端证书标识（对应iot_certificate.ssl_key，双向认证使用） */
  @Schema(description = "客户端证书标识")
  private String sslClientCertKey;

  /** 是否校验服务端主机名 */
  @Builder.Default
  @Schema(description = "是否校验服务端主机名")
  private boolean sslHostnameVerify = true;

  /** SSL证书路径 */
  @Schema(description = "SSL证书路径")
  private String sslCertPath;

  /** SSL密钥路径 */
  @Schema(description = "SSL密钥路径")
  private String sslKeyPath;

  /** SSL CA证书路径 */
  @Schema(description = "SSL CA证书路径")
  private String sslCaPath;

  // 连接配置
  /** 连接超时时间（秒） */
  @Builder.Default
  @Schema(description = "连接超时时间（秒）")
  private int connectTimeout = 30;

  /** 保活间隔（秒） */
  @Builder.Default
  @Schema(description = "保活间隔（秒）")
  private int keepAliveInterval = 60;

  /** 自动重连 */
  @Builder.Default
  @Schema(description = "自动重连")
  private boolean autoReconnect = true;

  /** 清除会话 */
  @Builder.Default
  @Schema(description = "清除会话")
  private boolean cleanSession = true;

  /** 最大重连间隔（秒） */
  @Builder.Default
  @Schema(description = "最大重连间隔（秒）")
  private int maxReconnectDelay = 300;

  // 主题配置
  /** 订阅主题列表 */
  @Schema(description = "订阅主题列表")
  private List<MqttTopicConfig> subscribeTopics;

  /** 发布主题配置 */
  @Schema(description = "发布主题配置")
  private Map<String, MqttTopicConfig> publishTopics;

  // 消息配置
  /** 默认QoS级别 */
  @Builder.Default
  @Schema(description = "默认QoS级别")
  private int defaultQos = 1;

  /** 是否保留消息 */
  @Builder.Default
  @Schema(description = "是否保留消息")
  private boolean retainMessage = false;

  /** 消息过期时间（秒） */
  @Builder.Default
  @Schema(description = "消息过期时间（秒）")
  private long messageExpiry = 3600;

  // 性能配置
  /** 最大在途消息数 */
  @Builder.Default
  @Schema(description = "最大在途消息数")
  private int maxInflightMessages = 10;

  /** 消息缓冲区大小 */
  @Builder.Default
  @Schema(description = "消息缓冲区大小")
  private int messageBufferSize = 1000;

  /** 线程池大小 */
  @Builder.Default
  @Schema(description = "线程池大小")
  private int threadPoolSize = 4;

  // 监控配置
  /** 是否启用指标统计 */
  @Builder.Default
  @Schema(description = "是否启用指标统计")
  private boolean metricsEnabled = true;

  /** 健康检查间隔（秒） */
  @Builder.Default
  @Schema(description = "健康检查间隔（秒）")
  private int healthCheckInterval = 60;

  // 设备提取配置
  /** 设备ID提取策略 */
  @Builder.Default
  @Schema(description = "设备ID提取策略")
  private String deviceIdExtractStrategy = "TOPIC_PATH";

  /** 设备ID提取配置 */
  @Schema(description = "设备ID提取配置")
  private Map<String, Object> deviceIdExtractConfig;

  /** 主题解析规则 */
  @Schema(description = "主题解析规则")
  private Map<String, String> topicParseRules;

  // 扩展配置
  /** 自定义配置 */
  @Schema(description = "自定义配置")
  private Map<String, Object> customConfig;

  /** MQTT主题配置 */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "MQTT主题配置")
  public static class MqttTopicConfig {

    /** 主题模式 */
    @Schema(description = "主题模式")
    private String topic;

    /** QoS级别 */
    @Builder.Default
    @Schema(description = "QoS级别")
    private int qos = 1;

    /** 是否保留消息 */
    @Builder.Default
    @Schema(description = "是否保留消息")
    private boolean retained = false;

    /** 主题类型 */
    @Schema(description = "主题类型")
    private String topicType;

    /** 描述 */
    @Schema(description = "描述")
    private String description;

    /** 是否启用 */
    @Builder.Default
    @Schema(description = "是否启用")
    private boolean enabled = true;

    /** 关联的产品Key（可选，用于个性化topic映射） 如果未配置，将根据topic pattern和实际消息topic自动匹配提取 */
    @Schema(description = "关联的产品Key")
    private String productKey;

    /** 主题分类：THING_MODEL（物模型）或 PASSTHROUGH（透传） 用于第三方MQTT时，明确指定主题类型，确保消息能正确路由到对应的处理器 */
    @Schema(description = "主题分类")
    private cn.universal.mqtt.protocol.config.MqttConstant.TopicCategory topicCategory;

    /** 兼容方法，获取主题 */
    public String getTopic() {
      return topic;
    }

    /** 兼容方法，设置主题 */
    public void setTopic(String topic) {
      this.topic = topic;
    }
  }

  /** 检查配置有效性 */
  public boolean isValid() {
    return networkUnionId != null && !networkUnionId.isEmpty() && host != null && !host.isEmpty();
  }

  // 兼容方法，为了与其他模块保持一致的接口
  public int getConnectionTimeout() {
    return connectTimeout;
  }

  public String getHost() {
    return host;
  }

  /** 兼容方法，获取Broker URL */
  public String getBrokerUrl() {
    return host;
  }

  public boolean isCleanSession() {
    return cleanSession;
  }

  public int getDefaultQos() {
    return defaultQos;
  }
}
