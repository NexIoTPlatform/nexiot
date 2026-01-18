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

package cn.universal.dm.device.service.push;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import lombok.extern.slf4j.Slf4j;

/**
 * 规则引擎处理器 - 消息过滤和规则检查
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
@Slf4j
@Component
public class RuleEngineProcessor implements UPProcessor<BaseUPRequest> {

  @Override
  public String getName() {
    return "RuleEngineProcessor";
  }

  @Override
  public String getDescription() {
    return "规则引擎处理器";
  }

  @Override
  public int getOrder() {
    return 200; // 在格式转换之后执行
  }

  @Override
  public List<BaseUPRequest> beforePush(List<BaseUPRequest> upRequests) {
    log.debug("[规则引擎处理器] 开始处理 {} 条消息", upRequests.size());

    // 应用业务规则过滤
    List<BaseUPRequest> filteredRequests =
        upRequests.stream().filter(this::applyBusinessRules).collect(Collectors.toList());

    int filteredCount = upRequests.size() - filteredRequests.size();
    if (filteredCount > 0) {
      log.info("[规则引擎处理器] 过滤掉 {} 条不符合规则的消息", filteredCount);
    }

    log.debug("[规则引擎处理器] 处理完成，剩余 {} 条消息", filteredRequests.size());
    return filteredRequests;
  }

  /**
   * 应用业务规则
   *
   * @param request 请求对象
   * @return 是否通过规则检查
   */
  private boolean applyBusinessRules(BaseUPRequest request) {
    IoTDeviceDTO deviceDTO = request.getIoTDeviceDTO();

    // 规则1：设备必须存在且有效
    if (deviceDTO == null) {
      log.warn("[规则引擎] 【规则1-设备不存在】过滤消息，IotId: {}", request.getIotId());
      return false;
    }

    // 规则2：应用必须启用
    if (deviceDTO.isAppDisable()) {
      log.warn("[规则引擎] 【规则2-应用已禁用】过滤消息，IotId: {}, AppId: {}", 
               request.getIotId(), deviceDTO.getApplicationId());
      return false;
    }

    // 规则3：设备必须在线
    if (deviceDTO.getState() == null || !deviceDTO.getState()) {
      log.warn("[规则引擎] 【规则3-设备离线】过滤消息，IotId: {}, DeviceState: {}", 
               request.getIotId(), deviceDTO.getState());
      return false;
    }

    log.debug("[规则引擎] ✅ 消息通过所有规则检查，IotId: {}, AppId: {}, DeviceState: {}", 
             request.getIotId(), deviceDTO.getApplicationId(), deviceDTO.getState());
    return true;
  }
}
