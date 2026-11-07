package cn.universal.dm.device.service.push.transform.preset;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.universal.dm.device.service.push.transform.PushTransformPresetTransformer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class HuaweiIotdaPresetTransformer implements PushTransformPresetTransformer {

  @Override
  public String getCode() {
    return "HUAWEI_IOTDA";
  }

  @Override
  public String getName() {
    return "华为云 IoTDA";
  }

  @Override
  public String getDescription() {
    return "输出华为云 IoTDA 常用 services 结构，支持属性和事件。";
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object transform(Map<String, Object> source, JSONObject options) {
    String serviceId = options == null ? null : options.getStr("serviceId");
    if (StrUtil.isBlank(serviceId)) {
      serviceId = "default";
    }
    Long timestamp = MapUtil.getLong(source, "time", System.currentTimeMillis());
    String messageType = MapUtil.getStr(source, "messageType");

    Map<String, Object> service = new LinkedHashMap<>();
    service.put("service_id", serviceId);

    if ("EVENT".equalsIgnoreCase(messageType)) {
      Map<String, Object> data = (Map<String, Object>) source.get("data");
      service.put("event_type", MapUtil.getStr(source, "event", "event"));
      service.put("event_time", String.valueOf(timestamp));
      service.put("paras", MapUtil.isEmpty(data) ? new LinkedHashMap<>() : data);
    } else {
      Map<String, Object> properties = (Map<String, Object>) source.get("properties");
      service.put("properties", MapUtil.isEmpty(properties) ? new LinkedHashMap<>() : properties);
      service.put("event_time", String.valueOf(timestamp));
    }

    List<Map<String, Object>> services = new ArrayList<>();
    services.add(service);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("services", services);
    return result;
  }
}
