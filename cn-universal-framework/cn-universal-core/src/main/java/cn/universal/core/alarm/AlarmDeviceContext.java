package cn.universal.core.alarm;

import java.util.Map;

/** 告警读取设备最新上报状态的可选 SPI；实现方必须验证设备租户，时间为 UTC 毫秒。 */
public interface AlarmDeviceContext {
  record Snapshot(Map<String, Object> properties, Map<String, Long> timestamps) {}
  Snapshot read(String tenantId, String iotId);
}
