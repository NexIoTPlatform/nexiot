package cn.universal.persistence.dto;

import cn.universal.persistence.service.device.IoTDeviceDeleteImpact;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/** Deletion impact returned before the irreversible device delete command is sent. */
@Value
@Builder
public class IoTDeviceDeletePreview {
  Long id;
  String iotId;
  String tenantId;
  String productKey;
  String deviceId;
  String deviceName;
  int childDeviceCount;
  boolean blocked;
  @Builder.Default List<String> blockers = List.of();
  @Builder.Default List<IoTDeviceDeleteImpact> moduleImpacts = List.of();
}
