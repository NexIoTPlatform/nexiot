package cn.universal.persistence.entity.bo;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketPushSettingsBO implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String DEVICE_CREATE = "deviceCreate";
  public static final String DEVICE_ONLINE = "deviceOnline";
  public static final String DEVICE_OFFLINE = "deviceOffline";
  public static final String COMMAND_RESP = "commandResp";
  public static final String ALARM = "alarm";
  public static final String WORK_ORDER = "workOrder";

  public static final String DEVICE_MESSAGE = "deviceMessage";
  public static final String DEVICE_PROPERTY = "deviceProperty";
  public static final String DEVICE_EVENT = "deviceEvent";

  private Boolean enabled;

  private Boolean deviceCreate;

  private Boolean deviceOnline;

  private Boolean deviceOffline;

  private Boolean commandResp;

  private Boolean alarm;

  private Boolean workOrder;

  private Boolean deviceMessage;

  private Boolean deviceProperty;

  private Boolean deviceEvent;

  public static List<String> categories() {
    return List.of(DEVICE_CREATE, DEVICE_ONLINE, DEVICE_OFFLINE, COMMAND_RESP, ALARM, WORK_ORDER);
  }

  public static WebSocketPushSettingsBO defaults() {
    return WebSocketPushSettingsBO.builder()
        .enabled(Boolean.FALSE)
        .deviceCreate(Boolean.TRUE)
        .deviceOnline(Boolean.TRUE)
        .deviceOffline(Boolean.TRUE)
        .commandResp(Boolean.FALSE)
        .alarm(Boolean.TRUE)
        .workOrder(Boolean.TRUE)
        .deviceMessage(Boolean.FALSE)
        .deviceProperty(Boolean.FALSE)
        .deviceEvent(Boolean.FALSE)
        .build();
  }

  public WebSocketPushSettingsBO normalized() {
    WebSocketPushSettingsBO defaults = defaults();
    Boolean normalizedCommandResp =
        commandResp == null
            ? defaultBoolean(deviceMessage, defaults.getCommandResp())
            : commandResp;
    return WebSocketPushSettingsBO.builder()
        .enabled(defaultBoolean(enabled, defaults.getEnabled()))
        .deviceCreate(defaultBoolean(deviceCreate, defaults.getDeviceCreate()))
        .deviceOnline(defaultBoolean(deviceOnline, defaults.getDeviceOnline()))
        .deviceOffline(defaultBoolean(deviceOffline, defaults.getDeviceOffline()))
        .commandResp(defaultBoolean(normalizedCommandResp, defaults.getCommandResp()))
        .alarm(defaultBoolean(alarm, defaults.getAlarm()))
        .workOrder(defaultBoolean(workOrder, defaults.getWorkOrder()))
        .deviceMessage(Boolean.FALSE)
        .deviceProperty(Boolean.FALSE)
        .deviceEvent(Boolean.FALSE)
        .build();
  }

  public boolean supportsCategory(String category) {
    WebSocketPushSettingsBO settings = normalized();
    if (!Boolean.TRUE.equals(settings.getEnabled())) {
      return false;
    }
    return switch (category) {
      case DEVICE_CREATE -> Boolean.TRUE.equals(settings.getDeviceCreate());
      case DEVICE_ONLINE -> Boolean.TRUE.equals(settings.getDeviceOnline());
      case DEVICE_OFFLINE -> Boolean.TRUE.equals(settings.getDeviceOffline());
      case COMMAND_RESP -> Boolean.TRUE.equals(settings.getCommandResp());
      case ALARM -> Boolean.TRUE.equals(settings.getAlarm());
      case WORK_ORDER -> Boolean.TRUE.equals(settings.getWorkOrder());
      default -> false;
    };
  }

  public Map<String, Boolean> toCategoryMap() {
    WebSocketPushSettingsBO settings = normalized();
    Map<String, Boolean> categoryMap = new LinkedHashMap<>();
    categoryMap.put(DEVICE_CREATE, Boolean.TRUE.equals(settings.getDeviceCreate()));
    categoryMap.put(DEVICE_ONLINE, Boolean.TRUE.equals(settings.getDeviceOnline()));
    categoryMap.put(DEVICE_OFFLINE, Boolean.TRUE.equals(settings.getDeviceOffline()));
    categoryMap.put(COMMAND_RESP, Boolean.TRUE.equals(settings.getCommandResp()));
    categoryMap.put(ALARM, Boolean.TRUE.equals(settings.getAlarm()));
    categoryMap.put(WORK_ORDER, Boolean.TRUE.equals(settings.getWorkOrder()));
    return categoryMap;
  }

  private static Boolean defaultBoolean(Boolean value, Boolean defaultValue) {
    return value == null ? defaultValue : value;
  }
}
