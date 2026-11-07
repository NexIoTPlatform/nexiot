package cn.universal.dm.device.service.log.writer;

import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;

/** Immutable request log write item used by the shared device-log writer. */
public record DeviceLogWriteRequest(
    BaseUPRequest upRequest, IoTDeviceDTO ioTDeviceDTO, IoTProduct ioTProduct) {}
