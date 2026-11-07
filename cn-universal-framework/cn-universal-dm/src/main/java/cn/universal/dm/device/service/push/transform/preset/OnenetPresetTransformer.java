package cn.universal.dm.device.service.push.transform.preset;

import cn.hutool.json.JSONObject;
import cn.universal.dm.device.service.push.transform.PushTransformPresetTransformer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OnenetPresetTransformer implements PushTransformPresetTransformer {

  @Override
  public String getCode() {
    return "ONENET_STANDARD";
  }

  @Override
  public String getName() {
    return "中国移动 OneNET";
  }

  @Override
  public String getDescription() {
    return "预留 OneNET 物联网平台格式扩展点，后续可按项目需要补充字段映射。";
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
