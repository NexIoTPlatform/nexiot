package cn.universal.dm.device.event;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Device shadow hot-state update event for in-process consumers. */
public class DeviceShadowUpdatedEvent {

  private final String iotId;
  private final String productKey;
  private final String deviceId;
  private final String extDeviceId;
  private final Map<String, Object> properties;
  private final long timestamp;
  private final long version;

  public DeviceShadowUpdatedEvent(
      String iotId,
      String productKey,
      String deviceId,
      String extDeviceId,
      Map<String, Object> properties,
      long timestamp,
      long version) {
    this.iotId = iotId;
    this.productKey = productKey;
    this.deviceId = deviceId;
    this.extDeviceId = extDeviceId;
    this.properties =
        properties == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    this.timestamp = timestamp;
    this.version = version;
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

  public String getExtDeviceId() {
    return extDeviceId;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public long getVersion() {
    return version;
  }
}
