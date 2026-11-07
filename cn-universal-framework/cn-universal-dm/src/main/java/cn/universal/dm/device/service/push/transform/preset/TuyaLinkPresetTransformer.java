package cn.universal.dm.device.service.push.transform.preset;

import cn.hutool.json.JSONObject;
import cn.universal.dm.device.service.push.transform.PushTransformPresetTransformer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TuyaLinkPresetTransformer implements PushTransformPresetTransformer {

  @Override
  public String getCode() {
    return "TUYA_LINK";
  }

  @Override
  public String getName() {
    return "涂鸦 IoT";
  }

  @Override
  public String getDescription() {
    return "预留涂鸦 IoT 平台格式扩展点，可后续单独实现标准报文结构。";
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
