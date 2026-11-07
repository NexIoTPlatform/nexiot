package cn.universal.admin.platform.service.taskcenter;

import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.vo.IoTTaskSubmitResultVO;
import java.util.List;

public interface IoTDeviceImportTaskService {

  IoTTaskSubmitResultVO submitImportTask(
      List<IoTDevice> devicesImport,
      IoTProduct product,
      String fileName,
      String gwDeviceId,
      String gwProductKey,
      IoTUser creator,
      String tenantId,
      Long industrialTemplateId,
      Integer industrialTemplateVersion);

  default IoTTaskSubmitResultVO submitImportTask(
      List<IoTDevice> devicesImport,
      IoTProduct product,
      String fileName,
      String gwDeviceId,
      String gwProductKey,
      IoTUser creator) {
    return submitImportTask(
        devicesImport,
        product,
        fileName,
        gwDeviceId,
        gwProductKey,
        creator,
        creator == null ? null : creator.getTenantId(),
        null,
        null);
  }
}
