package cn.universal.dm.device.service.log.writer;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTProduct;

/** Versioned immutable device-log event used by MQ transports. */
public record DeviceLogWriteRecord(
    int schemaVersion,
    String eventId,
    String policy,
    String partitionKey,
    String requestJson,
    String deviceLogJson,
    String deviceJson,
    String productJson,
    long acceptedAtMs) {

  public static final int CURRENT_SCHEMA_VERSION = 1;

  public static DeviceLogWriteRecord request(
      String policy,
      BaseUPRequest request,
      IoTDeviceDTO device,
      IoTProduct product,
      long acceptedAtMs) {
    return new DeviceLogWriteRecord(
        CURRENT_SCHEMA_VERSION,
        IdUtil.fastSimpleUUID(),
        policy,
        partitionKey(request == null ? null : request.getIotId(), device),
        JSONUtil.toJsonStr(request),
        null,
        JSONUtil.toJsonStr(device),
        JSONUtil.toJsonStr(product),
        acceptedAtMs);
  }

  public static DeviceLogWriteRecord entity(
      String policy,
      IoTDeviceLog log,
      IoTDeviceDTO device,
      IoTProduct product,
      long acceptedAtMs) {
    return new DeviceLogWriteRecord(
        CURRENT_SCHEMA_VERSION,
        IdUtil.fastSimpleUUID(),
        policy,
        partitionKey(log == null ? null : log.getIotId(), device),
        null,
        JSONUtil.toJsonStr(log),
        JSONUtil.toJsonStr(device),
        JSONUtil.toJsonStr(product),
        acceptedAtMs);
  }

  public boolean requestWrite() {
    return StrUtil.isNotBlank(requestJson);
  }

  public boolean entityWrite() {
    return StrUtil.isNotBlank(deviceLogJson);
  }

  public BaseUPRequest toRequest() {
    return requestWrite() ? JSONUtil.toBean(requestJson, BaseUPRequest.class) : null;
  }

  public IoTDeviceLog toDeviceLog() {
    return entityWrite() ? JSONUtil.toBean(deviceLogJson, IoTDeviceLog.class) : null;
  }

  public IoTDeviceDTO toDevice() {
    return JSONUtil.toBean(deviceJson, IoTDeviceDTO.class);
  }

  public IoTProduct toProduct() {
    return JSONUtil.toBean(productJson, IoTProduct.class);
  }

  private static String partitionKey(String iotId, IoTDeviceDTO device) {
    if (StrUtil.isNotBlank(iotId)) {
      return iotId;
    }
    if (device == null) {
      return "unknown";
    }
    return StrUtil.blankToDefault(
        device.getIotId(),
        StrUtil.join(":", device.getProductKey(), StrUtil.nullToEmpty(device.getDeviceId())));
  }
}
