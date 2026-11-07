package cn.universal.dm.device.service.push.transform;

import cn.hutool.json.JSONObject;
import java.util.Map;

public interface PushTransformPresetTransformer {

  String getCode();

  String getName();

  String getDescription();

  default boolean isImplemented() {
    return true;
  }

  Object transform(Map<String, Object> source, JSONObject options);
}
