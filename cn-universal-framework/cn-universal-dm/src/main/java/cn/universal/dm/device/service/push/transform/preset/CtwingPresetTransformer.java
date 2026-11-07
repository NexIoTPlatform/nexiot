package cn.universal.dm.device.service.push.transform.preset;

import cn.hutool.json.JSONObject;
import cn.universal.dm.device.service.push.transform.PushTransformPresetTransformer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CtwingPresetTransformer implements PushTransformPresetTransformer {

  @Override
  public String getCode() {
    return "CTWING_STANDARD";
  }

  @Override
  public String getName() {
    return "中国电信 CTWing";
  }

  @Override
  public String getDescription() {
    return "预留 CTWing 平台格式扩展点，便于后续补充事件和属性映射。";
  }

  @Override
  public boolean isImplemented() {
    return false;
  }

  @Override
  public Object transform(Map<String, Object> source, JSONObject options) {
    return new LinkedHashMap<>(source);
  }
}
