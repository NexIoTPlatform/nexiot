package cn.universal.persistence.query;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 设备日志查询参数封装类
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/12/14
 */
@Schema(description = "设备日志查询参数")
public class DeviceLogQuery {
  @Schema(description = "日志ID")
  private Long logId;

  @Schema(description = "IoT ID")
  private String iotId;

  @Schema(description = "产品Key")
  private String productKey;

  @Schema(description = "设备ID")
  private String deviceId;

  @Schema(description = "指令ID")
  private String commandId;

  @Schema(description = "消息类型")
  private String messageType;

  @Schema(description = "开始时间")
  private Long startTime;

  @Schema(description = "结束时间")
  private Long endTime;

  @Schema(description = "指令状态")
  private Integer commandStatus;

  @Schema(description = "限制数量")
  private int limit = 1;

  @Schema(description = "事件类型")
  private String eventType;

  @Schema(description = "属性键")
  private String propertyKey;

  // 私有构造函数，强制使用Builder模式
  private DeviceLogQuery() {}

  /** Builder模式构建查询参数 */
  public static class Builder {
    private DeviceLogQuery query = new DeviceLogQuery();

    public Builder logId(Long logId) {
      query.logId = logId;
      return this;
    }

    public Builder iotId(String iotId) {
      query.iotId = iotId;
      return this;
    }

    public Builder productKey(String productKey) {
      query.productKey = productKey;
      return this;
    }

    public Builder deviceId(String deviceId) {
      query.deviceId = deviceId;
      return this;
    }

    public Builder commandId(String commandId) {
      query.commandId = commandId;
      return this;
    }

    public Builder messageType(String messageType) {
      query.messageType = messageType;
      return this;
    }

    public Builder startTime(Long startTime) {
      query.startTime = startTime;
      return this;
    }

    public Builder endTime(Long endTime) {
      query.endTime = endTime;
      return this;
    }

    public Builder commandStatus(Integer commandStatus) {
      query.commandStatus = commandStatus;
      return this;
    }

    public Builder limit(int limit) {
      query.limit = limit;
      return this;
    }

    public Builder eventType(String eventType) {
      query.eventType = eventType;
      return this;
    }

    public Builder propertyKey(String propertyKey) {
      query.propertyKey = propertyKey;
      return this;
    }

    public DeviceLogQuery build() {
      return query;
    }
  }

  // Getters
  public Long getLogId() {
    return logId;
  }

  public String getIotId() {
    return iotId;
  }

  public String getProductKey() {
    return productKey;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public String getCommandId() {
    return commandId;
  }

  public String getMessageType() {
    return messageType;
  }

  public Long getStartTime() {
    return startTime;
  }

  public Long getEndTime() {
    return endTime;
  }

  public Integer getCommandStatus() {
    return commandStatus;
  }

  public int getLimit() {
    return limit;
  }

  public String getEventType() {
    return eventType;
  }

  public String getPropertyKey() {
    return propertyKey;
  }
}
