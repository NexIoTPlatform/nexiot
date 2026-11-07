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

package cn.universal.mqtt.protocol.processor;

import cn.hutool.json.JSONUtil;
import cn.universal.common.domain.R;
import cn.universal.dm.device.service.plugin.ProcessorExecutor;
import cn.universal.mqtt.protocol.entity.MQTTDownRequest;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * HTTP下行消息处理器链
 *
 * <p>使用通用的ProcessorExecutor执行处理器逻辑 专注于HTTP下行业务逻辑，通用逻辑由ProcessorExecutor处理
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/01/20
 */
@Slf4j(topic = "mqtt")
@Component
public class MQTTDownProcessorChain {

  @Autowired private ProcessorExecutor processorExecutor;

  @Autowired private List<MQTTDownMessageProcessor> processors;

  /**
   * 处理HTTP下行消息
   *
   * @param request HTTP下行请求
   * @return 处理结果
   */
  public R<?> process(MQTTDownRequest request) {
    if (request == null) {
      log.warn("[MQTT_DOWN] 请求为空，跳过处理");
      return R.error("请求为空");
    }

    log.info("[MQTT_DOWN] 开始处理下行消息，请求ID: {}", request.getRequestId());

    R<?> result =
        processorExecutor.executeChainWithResult(
            processors,
            "MQTT_DOWN",
            processor -> {
              if (!processor.preCheck(request)) {
                log.debug("[MQTT_DOWN] 处理器 {} 预检查失败", processor.getName());
                return null;
              }
              var r = processor.process(request);
              processor.postProcess(request, r);
              return r;
            },
            Objects::nonNull, // 成功检查
            processor -> processor.supports(request));

    log.info("[MQTT_DOWN] 下行消息处理完成，结果: {}",  JSONUtil.toJsonStr(result));
    return result != null ? result : R.error("无处理器成功处理该请求");
  }

  /** 获取处理器数量 */
  public int getProcessorCount() {
    return processors.size();
  }

  /** 获取处理器名称列表（用于调试） */
  public List<String> getProcessorNames() {
    return processorExecutor.getProcessorNames(processors);
  }

  /** 检查是否有指定名称的处理器 */
  public boolean hasProcessor(String name) {
    return processors.stream().anyMatch(p -> p.getName().equals(name));
  }
}
