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

import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 消息格式处理器 - 统一消息格式转换
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/9
 */
@Slf4j
@Component
public class MessageFormatProcessor implements UPProcessor<BaseUPRequest> {

  @Override
  public String getName() {
    return "MessageFormatProcessor";
  }

  @Override
  public String getDescription() {
    return "统一消息格式转换处理器";
  }

  @Override
  public int getOrder() {
    return 100; // 高优先级，优先执行
  }

  @Override
  public boolean isAfterPushEnabled() {
    return false;
  }

  @Override
  public List<BaseUPRequest> beforePush(List<BaseUPRequest> upRequests) {
    log.debug("[消息格式处理器] 开始处理 {} 条消息", upRequests.size());

    upRequests.forEach(
        request -> {
          // 统一消息格式转换
          formatMessage(request);
        });

    log.debug("[消息格式处理器] 处理完成");
    return upRequests;
  }

  /**
   * 格式化消息
   *
   * @param request 请求对象
   */
  private void formatMessage(BaseUPRequest request) {
    IoTDeviceDTO deviceDTO = request.getIoTDeviceDTO();
    if (deviceDTO != null) {
      // 添加时间戳
      if (request.getTime() == null) {
        request.setTime(System.currentTimeMillis());
      }

      // 添加消息来源标识
      if (deviceDTO.getThirdPlatform() == null) {
        deviceDTO.setThirdPlatform("universal");
      }

      log.debug("[消息格式处理器] 格式化消息: {}", request.getIotId());
    }
  }
}
