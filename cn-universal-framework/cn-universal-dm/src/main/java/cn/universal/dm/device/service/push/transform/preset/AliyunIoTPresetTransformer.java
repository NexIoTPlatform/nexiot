package cn.universal.dm.device.service.push.transform.preset;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.universal.dm.device.service.push.transform.PushTransformPresetTransformer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AliyunIoTPresetTransformer implements PushTransformPresetTransformer {

  @Override
  public String getCode() {
    return "ALIYUN_ALINK";
  }

  @Override
  public String getName() {
    return "阿里云 Alink";
  }

  @Override
  public String getDescription() {
    return "输出阿里云物模型常用上报结构，支持属性和事件。";
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object transform(Map<String, Object> source, JSONObject options) {
    String messageType = MapUtil.getStr(source, "messageType");
    Long timestamp = MapUtil.getLong(source, "time", System.currentTimeMillis());
    Map<String, Object> payload = extractPayload(source);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", String.valueOf(timestamp));
    result.put("version", "1.0");
    if ("EVENT".equalsIgnoreCase(messageType)) {
      String event = MapUtil.getStr(source, "event", "event");
      result.put("method", "thing.event." + event + ".post");
      result.put("params", payload);
      return result;
    }
    result.put("method", "thing.event.property.post");
    result.put("params", payload);
    return result;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> extractPayload(Map<String, Object> source) {
    Map<String, Object> properties = (Map<String, Object>) source.get("properties");
    if (MapUtil.isNotEmpty(properties)) {
      return properties;
    }
    Map<String, Object> data = (Map<String, Object>) source.get("data");
    if (MapUtil.isNotEmpty(data)) {
      return data;
    }
    Map<String, Object> fallback = new LinkedHashMap<>();
    if (StrUtil.isNotBlank(MapUtil.getStr(source, "event"))) {
      fallback.put("event", MapUtil.getStr(source, "event"));
    }
    if (StrUtil.isNotBlank(MapUtil.getStr(source, "eventName"))) {
      fallback.put("eventName", MapUtil.getStr(source, "eventName"));
    }
    return fallback;
  }
}
