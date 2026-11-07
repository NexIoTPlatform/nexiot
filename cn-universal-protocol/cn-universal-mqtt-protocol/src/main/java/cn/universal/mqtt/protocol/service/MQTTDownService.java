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

package cn.universal.mqtt.protocol.service;

import cn.hutool.json.JSONUtil;
import cn.universal.common.domain.R;
import cn.universal.core.downlink.DownlinkInterceptorChain;
import cn.universal.core.service.IDown;
import cn.universal.dm.device.service.AbstractDownService;
import cn.universal.mqtt.protocol.config.MqttModuleInfo;
import cn.universal.mqtt.protocol.entity.MQTTDownRequest;
import cn.universal.mqtt.protocol.processor.MQTTDownProcessorChain;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 系统内置MQTT下行处理类
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/07/09 22:19
 */
@Service("mqttDownService")
@Slf4j(topic = "mqtt")
public class MQTTDownService extends AbstractDownService<MQTTDownRequest> implements IDown {

  @Resource private MqttModuleInfo mqttModuleInfo;
  @Resource private MQTTDownProcessorChain mqttDownProcessorChain;
  // ← 注入拦截器链管理器（包含所有拦截器）
  @Resource private DownlinkInterceptorChain downlinkInterceptorChain;

  @Override
  public DownlinkInterceptorChain getInterceptorChain() {
    return downlinkInterceptorChain; // ← 返回给 IDown 接口使用
  }

  /**
   * @deprecated 该方法已废弃，请使用MQTTDownRequestConverter进行转换 保留此方法仅为了兼容遗留代码
   */
  @Deprecated
  @Override
  protected MQTTDownRequest convert(String request) {
    // 旧的转换逻辑已移至MQTTDownRequestConverter
    // 这里只做基本的JSON解析
    return JSONUtil.toBean(request, MQTTDownRequest.class);
  }

  @Override
  public String code() {
    return mqttModuleInfo.getCode();
  }

  @Override
  public String name() {
    return mqttModuleInfo.getName();
  }

  @Override
  @SuppressWarnings("unchecked")
  public R doProcess(cn.universal.core.downlink.DownlinkContext<?> context) {
    try {
      // 从上下文获取已转换好的请求对象（由Converter自动转换）
      MQTTDownRequest downRequest = (MQTTDownRequest) context.getDownRequest();

      if (downRequest == null) {
        return R.error("请求对象为空，请检查Converter配置");
      }

      log.info(
          "[MQTT下行] deviceId={} productKey={} cmd={}",
          downRequest.getDeviceId(),
          downRequest.getProductKey(),
          downRequest.getCmd() != null ? downRequest.getCmd().getValue() : null);

      // 执行处理链
      return mqttDownProcessorChain.process(downRequest);

    } catch (Exception e) {
      log.error("[MQTT下行] 处理异常", e);
      return R.error("处理异常: " + e.getMessage());
    }
  }
}
