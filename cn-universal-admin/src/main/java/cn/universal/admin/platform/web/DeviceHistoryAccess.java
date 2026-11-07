package cn.universal.admin.platform.web;

import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.query.LogQuery;
import jakarta.annotation.Resource;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Check ownership before any history query, including cache hits and exports. */
@Component
public class DeviceHistoryAccess {
  @Resource private IoTProductMapper productMapper;
  @Resource private IoTDeviceMapper deviceMapper;

  public void requireAccess(String tenantId, boolean platformAdmin, LogQuery query) {
    if ((!platformAdmin && blank(tenantId)) || blank(query.getProductKey())) deny();
    IoTProduct product = productMapper.getProductByProductKey(query.getProductKey());
    if (product == null || (!platformAdmin && !Objects.equals(tenantId, product.getTenantId())))
      deny();
    if (!blank(query.getIotId())) {
      IoTDevice device =
          deviceMapper.selectOne(IoTDevice.builder().iotId(query.getIotId()).build());
      if (device == null
          || !Objects.equals(product.getProductKey(), device.getProductKey())
          || !Objects.equals(product.getTenantId(), device.getTenantId())
          || (!blank(query.getDeviceId())
              && !Objects.equals(query.getDeviceId(), device.getDeviceId()))) deny();
      query.setDeviceId(device.getDeviceId());
    }
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private static void deny() {
    throw new IoTException("没有访问该设备日志的权限", 403);
  }
}
