package cn.universal.dm.video;

import java.util.Map;

/** 已认证的平台连接接收到的告警；实现方负责核对实例、设备、租户和订阅范围。 */
public interface VideoPlatformAlarmSink {
  void accept(String platformType, String instanceKey, Map<String, Object> alarm);
}
