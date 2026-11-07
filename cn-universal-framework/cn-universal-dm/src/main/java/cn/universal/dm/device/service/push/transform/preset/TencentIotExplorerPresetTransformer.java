package cn.universal.dm.device.service.push.transform.preset;

import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONObject;
import cn.universal.dm.device.service.push.transform.PushTransformPresetTransformer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TencentIotExplorerPresetTransformer implements PushTransformPresetTransformer {

  @Override
  public String getCode() {
    return "TENCENT_IOT_EXPLORER";
  }

  @Override
  public String getName() {
    return "腾讯云 IoT Explorer";
  }

  @Override
  public String getDescription() {
    return "输出腾讯云 IoT Explorer 常用上报结构，支持属性和事件。";
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object transform(Map<String, Object> source, JSONObject options) {
    String messageType = MapUtil.getStr(source, "messageType");
    Long timestamp = MapUtil.getLong(source, "time", System.currentTimeMillis());
    String deviceId = MapUtil.getStr(source, "deviceId", "device");
    String clientToken = deviceId + "-" + timestamp;
    if ("EVENT".equalsIgnoreCase(messageType)) {
      Map<String, Object> data = (Map<String, Object>) source.get("data");
      Map<String, Object> payload = new LinkedHashMap<>();
      Map<String, Object> eventData = new LinkedHashMap<>();
      eventData.put("timestamp", timestamp);
      eventData.put("params", MapUtil.isEmpty(data) ? new LinkedHashMap<>() : data);
      payload.put(MapUtil.getStr(source, "event", "event"), eventData);

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("method", "event_post");
      result.put("clientToken", clientToken);
      result.put("params", payload);
      return result;
    }
    Map<String, Object> properties = (Map<String, Object>) source.get("properties");
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("method", "report");
    result.put("clientToken", clientToken);
    result.put("params", MapUtil.isEmpty(properties) ? new LinkedHashMap<>() : properties);
    return result;
  }
}
