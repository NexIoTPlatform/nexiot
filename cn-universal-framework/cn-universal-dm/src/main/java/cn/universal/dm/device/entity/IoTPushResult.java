package cn.universal.dm.device.entity;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 消息推送结果
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/7/31 23:06
 */
@Data
@Builder
public class IoTPushResult {

  /** 推送是否成功 */
  private boolean ok;

  /** 第三方平台类型 */
  private String platform;

  /** 产品Key */
  private String productKey;

  /** 设备ID */
  private String deviceId;

  /** 推送渠道 (HTTP, MQTT, Kafka, RocketMQ等) */
  private String channel;

  /** 推送消息内容 */
  private String messageContent;

  /** 推送时间 */
  private LocalDateTime pushTime;

  /** 响应时间(毫秒) */
  @Builder.Default private Long responseTime = 0L;

  /** 错误信息 */
  private String errorMessage;

  /** 错误代码 */
  private String errorCode;

  /** 重试次数 */
  private Integer retryCount;

  /** 最大重试次数 */
  private Integer maxRetryCount;

  /** 推送状态 (SUCCESS, FAILED, RETRYING, TIMEOUT) */
  private PushStatus status;

  /** 推送配置ID */
  private String configId;

  /** 请求ID，用于追踪 */
  private String requestId;

  /** 推送状态枚举 */
  public enum PushStatus {
    SUCCESS("成功"),
    FAILED("失败"),
    RETRYING("重试中"),
    TIMEOUT("超时"),
    CANCELLED("已取消");

    private final String description;

    PushStatus(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /** 创建成功结果 */
  public static IoTPushResult success(
      String platform,
      String productKey,
      String deviceId,
      String channel,
      String messageContent,
      Long responseTime) {
    return IoTPushResult.builder()
        .ok(true)
        .platform(platform)
        .productKey(productKey)
        .deviceId(deviceId)
        .channel(channel)
        .messageContent(messageContent)
        .pushTime(LocalDateTime.now())
        .responseTime(responseTime)
        .status(PushStatus.SUCCESS)
        .retryCount(0)
        .build();
  }

  /** 创建失败结果 */
  public static IoTPushResult failed(
      String platform,
      String productKey,
      String deviceId,
      String channel,
      String messageContent,
      String errorMessage,
      String errorCode) {
    return IoTPushResult.builder()
        .ok(false)
        .platform(platform)
        .productKey(productKey)
        .deviceId(deviceId)
        .channel(channel)
        .messageContent(messageContent)
        .pushTime(LocalDateTime.now())
        .errorMessage(errorMessage)
        .errorCode(errorCode)
        .status(PushStatus.FAILED)
        .retryCount(0)
        .build();
  }

  /** 创建重试结果 */
  public static IoTPushResult retry(IoTPushResult original, String errorMessage) {
    return IoTPushResult.builder()
        .ok(false)
        .platform(original.getPlatform())
        .productKey(original.getProductKey())
        .deviceId(original.getDeviceId())
        .channel(original.getChannel())
        .messageContent(original.getMessageContent())
        .pushTime(LocalDateTime.now())
        .errorMessage(errorMessage)
        .errorCode(original.getErrorCode())
        .status(PushStatus.RETRYING)
        .retryCount(original.getRetryCount() + 1)
        .maxRetryCount(original.getMaxRetryCount())
        .configId(original.getConfigId())
        .requestId(original.getRequestId())
        .build();
  }
}
