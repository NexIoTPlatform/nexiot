/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.rule.scene.deviceUp;

import cn.universal.common.constant.IoTConstant;
import cn.universal.core.message.UPRequest;
import cn.universal.persistence.entity.bo.TriggerBO;
import cn.universal.rule.scene.service.SceneConditionEvaluator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DeviceEventUp extends AbstractDeviceUp implements DeviceUp {

  @Override
  public String messageType() {
    return IoTConstant.MessageType.EVENT.name();
  }

  @Override
  public boolean testAlarm(List<TriggerBO> triggers, String separator, UPRequest upRequest) {
    return " || ".equals(separator)
        ? triggers.stream()
            .anyMatch(t -> matches(t, upRequest))
        : !triggers.isEmpty()
            && triggers.stream()
                .allMatch(t -> matches(t, upRequest));
  }

  private boolean matches(TriggerBO trigger, UPRequest request) {
    return trigger.getModelId() != null
        && trigger.getModelId().equals(request.getEvent())
        && (trigger.getFilters() == null || trigger.getFilters().isEmpty()
            || SceneConditionEvaluator.event(trigger.getFilters(), request.getData()));
  }
}
