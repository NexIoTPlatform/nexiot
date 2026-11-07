package cn.universal.persistence.entity.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * 上行推送配置BO
 *
 * @author gitee.com/NexIoT
 * @since 2025/7/12 10:47
 */
@Data
@Builder
@Schema(description = "上行推送配置业务对象")
public class UPPushBO implements Serializable {

  private static final long serialVersionUID = 1L;

  // HTTP 推送配置
  @Schema(description = "HTTP推送配置")
  private HttpPushConfig http;

  // MQTT 推送配置
  @Schema(description = "MQTT推送配置")
  private MqttPushConfig mqtt;

  @Schema(description = "外部MQTT推送配置")
  private ExternalMqttPushConfig externalMqtt;

  @Schema(description = "TCP推送配置")
  private TcpPushConfig tcp;

  // Kafka 推送配置
  @Schema(description = "Kafka推送配置")
  private KafkaPushConfig kafka;

  // RocketMQ 推送配置
  @Schema(description = "RocketMQ推送配置")
  private RocketMQPushConfig rocketMQ;

  // 统计配置
  @Schema(description = "统计配置")
  private StatisticsConfig statistics;

  // 重试配置
  @Schema(description = "重试配置")
  private RetryConfig retry;

  // 数据转换配置
  @Schema(description = "数据转换配置")
  private TransformConfig transform;

  /** HTTP 推送配置 */
  @Data
  @Builder
  @Schema(description = "HTTP推送配置类")
  public static class HttpPushConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "推送URL")
    private String url;

    @Schema(description = "是否启用")
    private boolean enable;

    @Schema(description = "请求头")
    private String header;

    @Schema(description = "密钥")
    private String secret;

    @Schema(description = "是否支持")
    private boolean support;
  }

  /** MQTT 推送配置 */
  @Data
  @Builder
  @Schema(description = "MQTT推送配置类")
  public static class MqttPushConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "MQTT服务器URL")
    private String url;

    @Schema(description = "推送主题")
    private String topic;

    @Schema(description = "是否启用")
    private boolean enable;

    @Schema(description = "是否支持")
    private boolean support;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "用户名")
    private String username;
  }

  /** 外部 MQTT 推送配置。与平台内置 MQTT 订阅通道相互独立。 */
  @Data
  @Builder
  @Schema(description = "外部MQTT推送配置类")
  public static class ExternalMqttPushConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String brokerUrl;
    private String clientId;
    private String username;
    private String password;
    private String topic;
    private Integer qos;
    private boolean retained;
    private Integer keepAliveSeconds;
    private Integer connectTimeoutSeconds;
    private Integer publishTimeoutSeconds;
    private boolean clusterClientId;
    private boolean enable;
    private boolean support;
  }

  /** TCP 单向推送配置。 */
  @Data
  @Builder
  @Schema(description = "TCP推送配置类")
  public static class TcpPushConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String host;
    private Integer port;
    private boolean tls;
    private String connectionMode;
    private String frameMode;
    private String delimiter;
    private String charset;
    private Integer connectTimeoutMillis;
    private Integer writeTimeoutMillis;
    private boolean enable;
    private boolean support;
  }

  /** Kafka 推送配置 */
  @Data
  @Builder
  @Schema(description = "Kafka推送配置类")
  public static class KafkaPushConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Kafka服务器地址")
    private String bootstrapServers;

    @Schema(description = "Kafka主题")
    private String topic;

    @Schema(description = "Kafka用户名")
    private String username;

    @Schema(description = "Kafka密码")
    private String password;

    @Schema(description = "Kafka安全协议，支持PLAINTEXT、SASL_PLAINTEXT、SASL_SSL、SSL")
    private String securityProtocol;

    @Schema(description = "Kafka SASL机制，默认PLAIN")
    private String saslMechanism;

    @Schema(description = "是否启用")
    private boolean enable;

    @Schema(description = "是否支持")
    private boolean support;
  }

  /** RocketMQ 推送配置 */
  @Data
  @Builder
  @Schema(description = "RocketMQ推送配置类")
  public static class RocketMQPushConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "RocketMQ NameServer地址")
    private String namesrvAddr;

    @Schema(description = "RocketMQ主题")
    private String topic;

    @Schema(description = "RocketMQ分组")
    private String group;

    @Schema(description = "RocketMQ AccessKey")
    private String accessKey;

    @Schema(description = "RocketMQ SecretKey")
    private String secretKey;

    @Schema(description = "RocketMQ SecurityToken")
    private String securityToken;

    @Schema(description = "RocketMQ命名空间")
    private String namespace;

    @Schema(description = "是否启用TLS")
    private boolean useTLS;

    @Schema(description = "是否启用")
    private boolean enable;

    @Schema(description = "是否支持")
    private boolean support;
  }

  /** 统计配置 */
  @Data
  @Builder
  @Schema(description = "统计配置类")
  public static class StatisticsConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否启用统计 */
    @Schema(description = "是否启用统计")
    private boolean enable = true;

    /** 统计数据保留时间（天） */
    @Schema(description = "统计数据保留时间(天)")
    private Integer retentionDays = 30;

    /** 异步统计 */
    @Schema(description = "是否异步统计")
    private boolean async = true;
  }

  /** 重试配置 */
  @Data
  @Builder
  @Schema(description = "重试配置类")
  public static class RetryConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否启用重试 */
    @Schema(description = "是否启用重试")
    private boolean enable = true;

    /** 最大重试次数 */
    @Schema(description = "最大重试次数")
    private Integer maxCount = 3;

    /** 重试间隔（分钟） */
    @Schema(description = "重试间隔(分钟)")
    private Integer intervalMinutes = 5;

    /** 重试队列保留时间（天） */
    @Schema(description = "重试队列保留时间(天)")
    private Integer queueRetentionDays = 1;

    /** 失败记录保留时间（天） */
    @Schema(description = "失败记录保留时间(天)")
    private Integer failedRetentionDays = 7;
  }

  /** 数据转换配置 */
  @Data
  @Builder
  @Schema(description = "数据转换配置类")
  public static class TransformConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否启用")
    private boolean enable;

    @Schema(description = "是否支持")
    private boolean support;

    @Schema(description = "默认全局规则ID")
    private Long defaultRuleId;

    @Schema(description = "失败回退模式 ORIGINAL/BLOCK")
    private String fallbackMode;
  }
}
