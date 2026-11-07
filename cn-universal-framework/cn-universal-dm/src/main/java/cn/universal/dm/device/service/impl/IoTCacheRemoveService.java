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

package cn.universal.dm.device.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import cn.universal.persistence.base.IoTDevicePostProcessor;
import cn.universal.persistence.dto.IoTDeviceDTO;
import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 缓存操作类
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/8/12 16:10
 */
@Component
@Slf4j
public class IoTCacheRemoveService {

  @Resource private IoTCacheInvalidationPublisher cacheInvalidationPublisher;

  /** @deprecated 调用方应提供设备唯一身份，避免清空所有设备缓存。 */
  @Deprecated
  public void removeDevInstanceBOCache() {
    log.warn("忽略无设备身份的缓存清理请求，请改用精细失效入口");
  }

  public void removeDevInstanceBOCache(String productKey, String deviceId, String iotId) {
    cacheInvalidationPublisher.publishDeviceChange(
        IoTDevicePostProcessor.Operation.UPDATE,
        IoTDeviceDTO.builder().productKey(productKey).deviceId(deviceId).iotId(iotId).build());
  }

  // 协议缓存按 productKey 精确失效，避免这里的无参兼容入口误清空整个缓存。
  public void removeDevProtocolCache() {}

  /**
   * 清理指定 productKey 的协议定义缓存，并通知集群节点。
   *
   * <p>协议定义使用 productKey 作为缓存 key，协议脚本修改时只需要失效该 key，避免清空整类缓存。
   */
  public void removeDevProtocolCache(String productKey) {
    if (StrUtil.isBlank(productKey)) {
      removeDevProtocolCache();
      return;
    }
    cacheInvalidationPublisher.publishCustomCacheChange(
        "protocol-updated",
        List.of("iot_protocol_def", "selectProtocolDefNoScript"),
        Map.of(
            "iot_protocol_def", List.of(productKey),
            "selectProtocolDefNoScript", List.of(productKey)));
  }

  public void removeIotDeviceSubscribeCache() {
    cacheInvalidationPublisher.publishCustomCacheChange(
        "device-subscribe-updated", List.of("iot_dev_subscribe"), Collections.emptyMap());
  }
}
