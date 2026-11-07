package cn.universal.admin.platform.service.taskcenter;

import cn.universal.persistence.entity.IoTTaskDefinition;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.bo.IoTDeviceJobTaskSubmitBO;
import cn.universal.persistence.entity.vo.IoTTaskSubmitResultVO;

public interface IoTDeviceJobTaskService {

  IoTTaskSubmitResultVO submitFunctionJobTask(
      IoTDeviceJobTaskSubmitBO submitBO, IoTUser creator);

  IoTTaskDefinition createFunctionJobDefinition(
      IoTDeviceJobTaskSubmitBO submitBO, IoTUser creator);

  IoTTaskDefinition updateFunctionJobDefinition(
      Long definitionId, IoTDeviceJobTaskSubmitBO submitBO, IoTUser creator);

  IoTTaskSubmitResultVO runFunctionJobDefinition(Long definitionId, IoTUser operator);

  void changeDefinitionScheduleStatus(Long definitionId, String scheduleStatus, IoTUser operator);

  void deleteFunctionJobDefinitions(Long[] definitionIds, IoTUser operator);
}
