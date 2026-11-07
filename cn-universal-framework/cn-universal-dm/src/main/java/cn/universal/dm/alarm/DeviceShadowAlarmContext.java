package cn.universal.dm.alarm;

import cn.hutool.json.JSONUtil;
import cn.universal.core.alarm.AlarmDeviceContext;
import cn.universal.dm.device.service.impl.IoTDeviceShadowService;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** 复用设备影子，不维护另一份最新属性缓存；每个属性独立保留上报时间。 */
@Component
public class DeviceShadowAlarmContext implements AlarmDeviceContext {
  private final IoTDeviceMapper devices;
  private final IoTDeviceShadowService shadows;

  public DeviceShadowAlarmContext(IoTDeviceMapper devices, IoTDeviceShadowService shadows) {
    this.devices = devices;
    this.shadows = shadows;
  }

  @Override
  public Snapshot read(String tenantId, String iotId) {
    var device = devices.selectOne(IoTDevice.builder().iotId(iotId).build());
    if (device == null) return new Snapshot(Map.of(), Map.of());
    String tenant = device.getTenantId();
    if (tenant == null || tenant.isBlank()) tenant = device.getCreatorId();
    if (!Objects.equals(tenantId, tenant)) throw new IllegalArgumentException("不能读取其他租户的设备状态");
    var shadow = shadows.getLatestDeviceShadow(iotId);
    if (shadow == null || shadow.getMetadata() == null) return new Snapshot(Map.of(), Map.of());
    var root = JSONUtil.parseObj(shadow.getMetadata());
    var state = root.getJSONObject("state");
    var metadata = root.getJSONObject("metadata");
    var reported = state == null ? null : state.getJSONObject("reported");
    var times = metadata == null ? null : metadata.getJSONObject("reported");
    Map<String, Object> properties = new LinkedHashMap<>();
    Map<String, Long> timestamps = new LinkedHashMap<>();
    if (reported != null) for (String key : reported.keySet()) {
      var stamp = times == null ? null : times.getJSONObject(key);
      Long time = stamp == null ? null : stamp.getLong("timestamp");
      // 没有属性级时间的旧影子不用于自动判断，避免把缓存读取时间当作采样时间。
      if (time == null || time <= 0) continue;
      properties.put(key, reported.get(key));
      timestamps.put(key, time < 100000000000L ? time * 1000 : time);
    }
    return new Snapshot(properties, timestamps);
  }
}
