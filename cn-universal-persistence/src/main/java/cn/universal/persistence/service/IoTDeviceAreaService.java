package cn.universal.persistence.service;

import cn.universal.persistence.entity.IoTDevice;

/** Unified device area maintenance entrance. */
public interface IoTDeviceAreaService {

  /**
   * Refreshes the device coordinate and tries to resolve the area id by longitude and latitude.
   *
   * <p>If the area lookup is unavailable, the device coordinate is still maintained and
   * {@code areasId} is left empty as a fallback.
   */
  void fillDeviceArea(IoTDevice ioTDevice, String longitude, String latitude);
}
