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
public class DevicePropertiesUp extends AbstractDeviceUp implements DeviceUp {

  @Override
  public String messageType() {
    return IoTConstant.MessageType.PROPERTIES.name();
  }

  @Override
  public boolean testAlarm(List<TriggerBO> triggers, String separator, UPRequest upRequest) {
    boolean any = " || ".equals(separator);
    return any
        ? triggers.stream()
            .anyMatch(
                t ->
                    SceneConditionEvaluator.properties(
                        t.getFilters(), upRequest.getProperties(), true))
        : !triggers.isEmpty()
            && triggers.stream()
                .allMatch(
                    t ->
                        SceneConditionEvaluator.properties(
                            t.getFilters(), upRequest.getProperties(), false));
  }
}
