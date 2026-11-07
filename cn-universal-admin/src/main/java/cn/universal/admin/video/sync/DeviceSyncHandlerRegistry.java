/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 设备同步处理器注册中心
 * @Author: gitee.com/NexIoT
 *
 */
package cn.universal.admin.video.sync;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 设备同步处理器注册中心
 */
@Slf4j
@Component
public class DeviceSyncHandlerRegistry {

  private final Map<String, DeviceSyncHandler> handlerMap = new ConcurrentHashMap<>();

  @Autowired(required = false)
  public void setHandlers(List<DeviceSyncHandler> handlers) {
    if (handlers != null) {
      for (DeviceSyncHandler handler : handlers) {
        String platformType = normalizePlatformType(handler.getPlatformType());
        if (platformType != null && !platformType.isEmpty()) {
          handlerMap.put(platformType, handler);
          if ("isc".equals(platformType)) {
            handlerMap.put("ics", handler);
          }
          log.info("[设备同步] 注册处理器: platformType={}, class={}", platformType, handler.getClass().getSimpleName());
        }
      }
    }
  }

  /**
   * 根据平台类型获取同步处理器
   * 
   * @param platformType 平台类型
   * @return 同步处理器，如果不存在返回null
   */
  public DeviceSyncHandler getHandler(String platformType) {
    if (platformType == null) {
      return null;
    }
    return handlerMap.get(normalizePlatformType(platformType));
  }

  /**
   * 检查是否支持指定平台的同步
   */
  public boolean supports(String platformType) {
    return platformType != null && handlerMap.containsKey(normalizePlatformType(platformType));
  }

  private String normalizePlatformType(String platformType) {
    if (platformType == null) {
      return null;
    }
    return "ics".equalsIgnoreCase(platformType) ? "isc" : platformType.toLowerCase();
  }
}
