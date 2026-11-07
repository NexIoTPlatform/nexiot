package cn.universal.dm.cache;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.event.EventMessage;
import cn.universal.common.event.processer.DeviceCacheInvalidationProcessor;
import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class ClusterIoTCacheInvalidationHandler implements DeviceCacheInvalidationProcessor {

  @Resource private ApplicationEventPublisher applicationEventPublisher;

  @Override
  public void handleDeviceCacheInvalidated(EventMessage message) {
    if (message == null || message.getData() == null) {
      return;
    }
    JSONObject data = JSONUtil.parseObj(message.getData());
    applicationEventPublisher.publishEvent(
        new IoTCacheInvalidationEvent(
            data.getStr("reason"),
            data.getStr("productKey"),
            data.getStr("deviceId"),
            data.getStr("iotId"),
            stringSet(data.get("cacheNames")),
            cacheKeys(data.get("cacheKeys")),
            stringSet(data.get("redisKeys"))));
  }

  private Set<String> stringSet(Object value) {
    if (value == null) {
      return Collections.emptySet();
    }
    JSONArray array = JSONUtil.parseArray(value);
    Set<String> result = new LinkedHashSet<>();
    array.forEach(item -> result.add(String.valueOf(item)));
    return result;
  }

  private Map<String, Collection<?>> cacheKeys(Object value) {
    if (value == null) {
      return Collections.emptyMap();
    }
    JSONObject object = JSONUtil.parseObj(value);
    Map<String, Collection<?>> result = new LinkedHashMap<>();
    object.forEach(
        (cacheName, keys) -> result.put(cacheName, new LinkedHashSet<>(JSONUtil.parseArray(keys))));
    return result;
  }
}
