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

import cn.universal.persistence.query.IoTDeviceQuery;
import jakarta.annotation.Resource;
import java.lang.reflect.Method;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

/**
 * iot_dev_action 专用 KeyGenerator
 */
@Component("iotDevActionKeyGenerator")
public class IoTDevActionKeyGenerator implements KeyGenerator {

  @Resource private IoTDevActionCacheKeyResolver iotDevActionCacheKeyResolver;

  @Override
  public Object generate(Object target, Method method, Object... params) {
    if (params == null || params.length == 0 || !(params[0] instanceof IoTDeviceQuery query)) {
      return "iot_dev_action:unsupported";
    }

    String key = iotDevActionCacheKeyResolver.resolveQueryKey(query);
    return key == null ? "iot_dev_action:unsupported" : key;
  }
}
