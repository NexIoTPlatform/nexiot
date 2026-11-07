package cn.universal.admin.platform.service.taskcenter;

import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.bo.IoTDeviceCredentialGenerateTaskBO;
import cn.universal.persistence.entity.vo.IoTTaskSubmitResultVO;
import jakarta.servlet.http.HttpServletResponse;

public interface IoTDeviceCredentialTaskService {

  IoTTaskSubmitResultVO submitGenerateTask(
      IoTDeviceCredentialGenerateTaskBO submitBO, IoTUser creator);

  void exportCredentials(Long taskId, String unionId, boolean admin, HttpServletResponse response)
      throws Exception;
}
