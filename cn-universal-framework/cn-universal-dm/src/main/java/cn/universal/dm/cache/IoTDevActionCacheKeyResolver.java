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

package cn.universal.dm.cache;

import cn.hutool.core.util.StrUtil;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.query.IoTDeviceQuery;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * iot_dev_action 缓存 Key 解析器
 *
 * <p>统一收敛查询入参与设备实体上的缓存 key 规则，避免查询和驱逐使用不同语义。
 */
@Component("iotDevActionCacheKeyResolver")
public class IoTDevActionCacheKeyResolver {

  public static final String CACHE_NAME = "iot_dev_action";

  @Resource private IoTDeviceCacheKeyResolver deviceCacheKeyResolver;

  public boolean canCache(IoTDeviceQuery query) {
    return StrUtil.isNotBlank(resolveQueryKey(query));
  }

  public String resolveQueryKey(IoTDeviceQuery query) {
    return deviceCacheKeyResolver.resolveDeviceQueryKey(query);
  }

  public Set<Object> resolveDeviceKeys(IoTDeviceDTO deviceDTO) {
    if (deviceDTO == null) {
      return Collections.emptySet();
    }

    return deviceCacheKeyResolver.resolveDeviceIdentityKeys(deviceDTO);
  }

  public Map<String, Set<Object>> buildCacheKeys(IoTDeviceDTO deviceDTO) {
    Set<Object> keys = resolveDeviceKeys(deviceDTO);
    if (keys.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, Set<Object>> cacheKeys = new LinkedHashMap<>();
    cacheKeys.put(CACHE_NAME, keys);
    return cacheKeys;
  }

}
