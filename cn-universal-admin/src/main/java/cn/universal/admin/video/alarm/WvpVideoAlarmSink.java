package cn.universal.admin.video.alarm;

import cn.universal.dm.video.VideoPlatformAlarmSink;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 国标父设备负责 IoT 路由，通道编号作为事件字段保存，不能拿通道号查父设备。 */
@Component
@RequiredArgsConstructor
public class WvpVideoAlarmSink implements VideoPlatformAlarmSink {
  private final SdkVideoAlarmUpService alarms;

  @Override
  public void accept(String platformType, String instanceKey, Map<String, Object> alarm) {
    if (!"wvp".equals(platformType)) throw new IllegalArgumentException("不支持的告警来源");
    SdkVideoAlarmCallbackRequest request = new SdkVideoAlarmCallbackRequest();
    request.setInstanceKey(instanceKey);
    request.setPlatformType("wvp");
    request.setVendor("GB28181");
    request.setDeviceId(text(alarm, "deviceId"));
    request.setDeviceName(text(alarm, "deviceName"));
    request.setChannelId(text(alarm, "channelId"));
    // 和直连 SDK 一样交给统一目录解析，不能在适配层把所有告警定义成同一事件。
    request.setAlarmType("vendor_raw");
    request.setAlarmName(text(alarm, "alarmTypeDescription"));
    request.setAlarmLevel(text(alarm, "alarmPriority"));
    request.setAlarmState("unknown");
    request.setDescription(text(alarm, "alarmDescription"));
    request.setEventId("wvp:" + instanceKey + ":" + text(alarm, "id"));
    request.setAlarmTime(parseTime(text(alarm, "alarmTime")));
    Map<String, Object> data = new LinkedHashMap<>(alarm);
    data.put("gbAlarmType", alarm.get("alarmType"));
    data.put("sdkEventFamily", "GB28181");
    String method = text(alarm, "alarmMethod");
    String type = text(alarm, "alarmType");
    if (method != null && !method.isBlank())
      data.put("vendorEventCode", method + (type == null || type.isBlank() ? "" : ":" + type));
    request.setData(data);
    request.setRaw(new LinkedHashMap<>(alarm));
    alarms.publishFromWvp(request);
  }

  static long parseTime(String value) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException("告警缺少时间");
    try {
      return OffsetDateTime.parse(value).toInstant().toEpochMilli();
    } catch (DateTimeParseException ignored) {
      return LocalDateTime.parse(value.replace('T', ' '), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
          .atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
    }
  }

  private static String text(Map<String, Object> values, String key) {
    return values.get(key) == null ? null : String.valueOf(values.get(key));
  }
}
