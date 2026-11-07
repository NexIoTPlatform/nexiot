package cn.universal.admin.platform.service.taskcenter.impl;

import cn.universal.admin.platform.service.taskcenter.IoTDeviceJobTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("iotTaskDefinitionQuartzInvoker")
@RequiredArgsConstructor
public class IoTTaskDefinitionQuartzInvoker {

  private final IoTDeviceJobTaskService deviceJobTaskService;

  public void execute(Long definitionId) {
    if (definitionId == null) {
      log.warn("Quartz 触发任务定义执行时缺少 definitionId");
      return;
    }
    deviceJobTaskService.runFunctionJobDefinition(definitionId, null);
  }
}
