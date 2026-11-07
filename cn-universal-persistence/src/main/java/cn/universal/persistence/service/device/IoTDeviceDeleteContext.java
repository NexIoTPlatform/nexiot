package cn.universal.persistence.service.device;

/** Immutable device identity passed to module-specific delete participants. */
public record IoTDeviceDeleteContext(
    Long id,
    String iotId,
    String tenantId,
    String productKey,
    String deviceId,
    String deviceName,
    String creatorId) {}
