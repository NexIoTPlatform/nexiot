package cn.universal.core.alarm;

import java.util.*;

/**
 * Common envelope for device uplinks, video/AI events and Flow actions; contains no tenant
 * selector.
 */
public class AlarmSignal {
  public String eventId = "";
  public String sourceType = "DEVICE";
  public String sourceId = "";
  public String policyId = "";
  public String typeCode = "";
  public String iotId = "";
  public String productKey = "";
  public String deviceId = "";
  public String deviceName = "";
  public String channelId = "";
  public String targetKey = "";
  public String eventType = "";
  public String phase = "TRIGGER";
  public String title = "";
  public String description = "";
  public String severity = "MAJOR";
  public String signalState = "ACTIVE";
  public long occurredAt;
  /** 参与属性判断的原始采样时间（UTC 毫秒），与告警快照一起保存。 */
  public Map<String, Long> propertyTimestamps = new LinkedHashMap<>();
  public Map<String, Object> data = new LinkedHashMap<>();
  public List<EvidenceRef> evidence = new ArrayList<>();

  public static class EvidenceRef {
    public String id = "";
    public String name = "";
    public String mimeType = "";
    public String kind = "ORIGINAL";
    public String url = "";
    public String objectKey = "";
    public String bucket = "";
    public String provider = "";
  }
}
