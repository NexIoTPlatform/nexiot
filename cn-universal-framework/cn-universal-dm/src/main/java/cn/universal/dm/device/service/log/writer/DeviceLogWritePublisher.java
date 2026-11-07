package cn.universal.dm.device.service.log.writer;

/**
 * MQ transport for immutable device-log events.
 */
public interface DeviceLogWritePublisher {

  String provider();

  void publish(java.util.List<DeviceLogWriteRecord> records);

  default void publish(DeviceLogWriteRecord record) {
    publish(java.util.List.of(record));
  }
}
