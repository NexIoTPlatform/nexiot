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

package cn.universal.rule.wrapper;

import cn.hutool.core.collection.CollectionUtil;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.dm.device.service.wrapper.IoTDeviceUPIntercept;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.base.IoTUPWrapper;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.rule.fence.service.FenceService;
import cn.universal.rule.scene.service.SceneLinkageService;
import cn.universal.rule.service.RuleService;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/1/20
 */
@Service("iotUPRuleService")
public class IoTUPRuleService implements IoTUPWrapper<BaseUPRequest> {

  @Resource private io.micrometer.core.instrument.MeterRegistry metrics;

  @Resource private SceneLinkageService sceneLinkageService;

  @Resource private RuleService ruleService;

  @Resource private FenceService fenceService;

  @Resource private IoTDeviceUPIntercept iotDeviceUPWrapper;

  @Override
  //  @Async
  public void beforePush(List<BaseUPRequest> baseUPRequests) {
    if (CollectionUtil.isEmpty(baseUPRequests)) {
      return;
    }
    baseUPRequests.stream()
        .filter(s -> s != null)
        .forEach(
            baseUPRequest -> {
              IoTDeviceDTO dev = baseUPRequest.getIoTDeviceDTO();
              if (dev == null) {
                return;
              }
              // 调用规则引擎
              if (ruleService.hasRules(dev))
                submit("sql", () -> ruleService.rule(baseUPRequest, dev));
              // 调用场景联动
              submit("scene", () -> sceneLinkageService.rule(baseUPRequest, dev));
              /*
               * 上行消息，属性和事件上报的后置处理，包括
               */
              submit("attributes", () -> iotDeviceUPWrapper.messageProcess(baseUPRequest));

              if (dev != null && dev.getProductConfig() != null) {
                if (dev.getProductConfig().containsKey("isGps")
                    && dev.getProductConfig().getBool("isGps")
                    && baseUPRequest.getMessageType().equals(MessageType.PROPERTIES)) {
                  submit("fence", () -> fenceService.testFence(baseUPRequest, dev));
                }
              }
            });
  }

  private void submit(String kind, Runnable action) {
    try {
      action.run();
    } catch (java.util.concurrent.RejectedExecutionException rejected) {
      metrics.counter("iot.uplink.sideeffect.rejected", "kind", kind).increment();
    }
  }
}
