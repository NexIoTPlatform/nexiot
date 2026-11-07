package cn.universal.persistence.message;

import cn.hutool.json.JSONUtil;
import cn.universal.persistence.base.BaseDownRequest;
import cn.universal.persistence.base.BaseUPRequest;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("tencentMessageAdapt")
public class TencentIoTMessageAdapter extends AbstractIoTMessageAdapter {

  @Override
  public String name() {
    return "tencent";
  }

  @Override
  public String formatUpMessage(BaseUPRequest upRequest) {
    try {
      Map<String, Object> result = new HashMap<>();

      result.put("msgType", upRequest.getMessageType().name());
      result.put("device_id", upRequest.getDeviceId());
      result.put("product_key", upRequest.getProductKey());
      result.put("device_name", upRequest.getDeviceName());
      result.put("timestamp", upRequest.getTime());
      result.put("event", upRequest.getEvent());
      result.put("eventName", upRequest.getEventName());
      result.put("data", upRequest.getData());
      result.put("properties", upRequest.getProperties());
      result.put("payload", upRequest.getPayload());

      return JSONUtil.toJsonStr(result);
    } catch (Exception e) {
      log.error("标准上行消息转换失败: {}", e.getMessage(), e);
      throw e;
    }
  }

  @Override
  public String formatDownMessage(BaseDownRequest downRequest) {
    try {
      Map<String, Object> result = new HashMap<>();

      result.put("cmd_id", downRequest.getMsgId());
      result.put("cmd_type", downRequest.getCmd().name());
      result.put("device_id", downRequest.getDeviceId());
      result.put("product_key", downRequest.getProductKey());
      result.put("function", downRequest.getFunction());
      result.put("detail", downRequest.getDetail());

      return JSONUtil.toJsonStr(result);
    } catch (Exception e) {
      log.error("标准下行消息转换失败: {}", e.getMessage(), e);
      throw e;
    }
  }
}
