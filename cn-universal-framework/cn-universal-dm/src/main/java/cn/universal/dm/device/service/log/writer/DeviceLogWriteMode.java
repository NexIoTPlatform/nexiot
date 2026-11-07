package cn.universal.dm.device.service.log.writer;

import java.util.Locale;

public enum DeviceLogWriteMode {
  DIRECT,
  BUFFER,
  MQ;

  static DeviceLogWriteMode parse(String value) {
    if (value == null || value.isBlank()) {
      return BUFFER;
    }
    try {
      return valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ignored) {
      return BUFFER;
    }
  }
}
