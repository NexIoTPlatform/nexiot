package cn.universal.dm.device.service.log.writer;

import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTProduct;

/** Immutable prebuilt device-log write item used by the shared device-log writer. */
public record DeviceLogEntityWrite(
    IoTDeviceLog ioTDeviceLog, IoTDeviceDTO ioTDeviceDTO, IoTProduct ioTProduct) {}
