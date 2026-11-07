package cn.universal.admin.platform.service.taskcenter.impl;

import cn.universal.admin.platform.service.IIoTDeviceService;
import cn.universal.common.domain.R;
import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.bo.IoTDeviceBO;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Atomic unit for one imported device. */
@Service
public class DeviceImportRowTransactionService {

  private final IIoTDeviceService deviceService;
  private final IoTProductMapper productMapper;
  private final IoTDeviceMapper deviceMapper;

  public DeviceImportRowTransactionService(
      IIoTDeviceService deviceService,
      IoTProductMapper productMapper,
      IoTDeviceMapper deviceMapper) {
    this.deviceService = deviceService;
    this.productMapper = productMapper;
    this.deviceMapper = deviceMapper;
  }

  @Transactional(rollbackFor = Exception.class)
  public void create(IoTDeviceBO device) {
    validateAndLockImportScope(device);
    R result = deviceService.insertDevInstance(device);
    if (result == null || result.getCode() != 0) {
      throw new BaseException(result == null ? "导入失败" : result.getMsg());
    }
  }

  private void validateAndLockImportScope(IoTDeviceBO device) {
    if (device == null || device.getTenantId() == null || device.getTenantId().isBlank()) {
      throw new BaseException("设备导入行缺少租户信息");
    }
    IoTProduct product = productMapper.getProductByProductKey(device.getProductKey());
    if (product == null || !Objects.equals(device.getTenantId(), product.getTenantId())) {
      throw new BaseException("导入产品不存在或不属于任务租户: " + device.getProductKey());
    }
    if (device.getGwDeviceId() == null || device.getGwDeviceId().isBlank()) {
      return;
    }
    if (device.getGwProductKey() == null || device.getGwProductKey().isBlank()) {
      throw new BaseException("设备导入行缺少网关产品编号");
    }
    IoTDevice gateway =
        deviceMapper.selectForUpdateByProductKeyAndDeviceId(
            device.getGwProductKey(), device.getGwDeviceId());
    if (gateway == null || !Objects.equals(device.getTenantId(), gateway.getTenantId())) {
      throw new BaseException("所属网关不存在或不属于任务租户");
    }
  }
}
