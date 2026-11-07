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

package cn.universal.dm.device.service;

import cn.universal.dm.device.entity.IoTPushResult;
import cn.universal.dm.device.service.push.PushStrategyManager;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.bo.UPPushBO;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 上行推送执行器
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
@Slf4j
@Component
public class UPPushExecutor {

  @Autowired private PushStrategyManager pushStrategyManager;

  /**
   * 根据配置执行推送
   *
   * @param request 上行请求
   * @param config 推送配置
   * @return 推送结果列表
   */
  public List<IoTPushResult> executePush(BaseUPRequest request, UPPushBO config) {
    if (request == null || config == null) {
      log.warn("[推送执行器] 请求或配置为空，跳过推送");
      return null;
    }

    IoTDeviceDTO deviceDTO = request.getIoTDeviceDTO();
    if (deviceDTO == null) {
      log.warn("[推送执行器] 设备信息为空，跳过推送");
      return null;
    }

    log.debug("[推送执行器] 开始推送消息: {}", request.getIotId());

    // 使用策略管理器执行推送
    List<IoTPushResult> results = pushStrategyManager.executePush(request, config);

    log.debug("[推送执行器] 推送完成: {}", request.getIotId());
    return results;
  }

  public boolean hasEnabledChannel(BaseUPRequest request, UPPushBO config) {
    return pushStrategyManager.hasEnabledChannel(request, config);
  }

  /**
   * 批量推送
   *
   * @param requests 请求列表
   * @param config 推送配置
   * @return 推送结果列表
   */
  public List<IoTPushResult> executeBatchPush(List<BaseUPRequest> requests, UPPushBO config) {
    if (requests == null || requests.isEmpty()) {
      log.warn("[推送执行器] 请求列表为空，跳过批量推送");
      return null;
    }

    log.debug("[推送执行器] 开始批量推送，消息数量: {}", requests.size());

    // 使用策略管理器执行批量推送
    List<IoTPushResult> results = pushStrategyManager.executeBatchPush(requests, config);

    log.debug("[推送执行器] 批量推送完成，消息数量: {}", requests.size());
    return results;
  }
}
