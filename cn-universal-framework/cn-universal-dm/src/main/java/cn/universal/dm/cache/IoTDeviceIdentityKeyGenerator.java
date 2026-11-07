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

import cn.universal.persistence.query.IoTAPIQuery;
import jakarta.annotation.Resource;
import java.lang.reflect.Method;
import java.util.Map;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

/** 为设备唯一身份查询生成稳定、可反向计算的缓存键。 */
@Component("iotDeviceIdentityKeyGenerator")
public class IoTDeviceIdentityKeyGenerator implements KeyGenerator {

  @Resource private IoTDeviceCacheKeyResolver cacheKeyResolver;

  @Override
  public Object generate(Object target, Method method, Object... params) {
    if (params == null || params.length == 0) {
      return "device:unsupported";
    }
    String key = null;
    if (params[0] instanceof Map<?, ?> map) {
      @SuppressWarnings("unchecked")
      Map<String, ?> query = (Map<String, ?>) map;
      key = cacheKeyResolver.resolveMapKey(query);
    } else if (params[0] instanceof IoTAPIQuery query) {
      key = cacheKeyResolver.resolveApiQueryKey(query);
    }
    return key == null ? "device:unsupported" : key;
  }
}
