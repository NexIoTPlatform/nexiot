/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 视频平台适配器注册中心
 * @Author: gitee.com/NexIoT
 *
 */
package cn.universal.dm.video;

import cn.universal.common.exception.BaseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 适配器注册中心，自动发现并管理所有视频平台适配器 */
@Slf4j
@Component
public class VideoPlatformAdapterRegistry {

  private final Map<String, VideoPlatformAdapter> adapterMap = new ConcurrentHashMap<>();

  @Autowired
  public VideoPlatformAdapterRegistry(List<VideoPlatformAdapter> adapters) {
    for (VideoPlatformAdapter adapter : adapters) {
      String platformType = normalizePlatformType(adapter.getSupportedPlatformType());
      adapterMap.put(platformType, adapter);
      if ("isc".equals(platformType)) {
        adapterMap.put("ics", adapter);
      }
      log.info("注册视频平台适配器：{} -> {}", platformType, adapter.getClass().getSimpleName());
    }
  }

  /** 根据平台类型获取适配器 */
  public VideoPlatformAdapter getAdapter(String platformType) {
    VideoPlatformAdapter adapter = adapterMap.get(normalizePlatformType(platformType));
    if (adapter == null) {
      throw new BaseException("不支持的视频平台类型：" + platformType);
    }
    return adapter;
  }

  /** 检查是否支持指定平台类型 */
  public boolean isSupported(String platformType) {
    return adapterMap.containsKey(normalizePlatformType(platformType));
  }

  /** Platform types this process can execute; used before claiming shared background tasks. */
  public Set<String> getSupportedPlatformTypes() {
    return Set.copyOf(adapterMap.keySet());
  }

  private String normalizePlatformType(String platformType) {
    if (platformType == null) {
      return null;
    }
    return "ics".equalsIgnoreCase(platformType) ? "isc" : platformType.toLowerCase();
  }
}
