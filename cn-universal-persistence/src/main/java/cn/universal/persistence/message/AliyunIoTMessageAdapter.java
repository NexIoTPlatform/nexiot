package cn.universal.persistence.message;

import cn.hutool.json.JSONUtil;
import cn.universal.persistence.base.BaseDownRequest;
import cn.universal.persistence.base.BaseUPRequest;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 阿里云IoT平台消息格式适配器 */
@Component
@Slf4j
public class AliyunIoTMessageAdapter extends AbstractIoTMessageAdapter {

  @Override
  public String name() {
    return "aliyun";
  }

  @Override
  public String formatUpMessage(BaseUPRequest upRequest) {
    try {
      Map<String, Object> result = new HashMap<>();

      // 基础字段映射
      result.put("iotId", upRequest.getDeviceId());
      result.put("requestId", upRequest.getRequestId());
      result.put("productKey", upRequest.getProductKey());
      result.put("deviceName", upRequest.getDeviceName());
      result.put("gmtCreate", upRequest.getTime());
      result.put("deviceType", upRequest.getDeviceNode());
      // 处理物模型数据 - 阿里云格式
      Map<String, Object> items = convertProperties(upRequest.getProperties());
      result.put("items", items);

      return JSONUtil.toJsonStr(result);
    } catch (Exception e) {
      log.error("阿里云上行消息转换失败: {}", e.getMessage(), e);
      throw e;
    }
  }

  @Override
  public String formatDownMessage(BaseDownRequest downRequest) {
    try {
      Map<String, Object> result = new HashMap<>();

      result.put("id", downRequest.getMsgId());
      result.put("version", "1.0");
      result.put("method", downRequest.getCmd().name());
      result.put("params", downRequest.getFunction());

      return JSONUtil.toJsonStr(result);
    } catch (Exception e) {
      log.error("阿里云下行消息转换失败: {}", e.getMessage(), e);
      throw e;
    }
  }

  /** 将属性数据转换为阿里云格式 阿里云格式：{"items":{"propertyId":{"value":"xxx","time":timestamp}}} */
  private Map<String, Object> convertProperties(Map<String, Object> properties) {
    Map<String, Object> items = new HashMap<>();

    if (properties == null || properties.isEmpty()) {
      return items;
    }

    for (Map.Entry<String, Object> entry : properties.entrySet()) {
      String propertyId = entry.getKey();
      Object value = entry.getValue();

      Map<String, Object> item = new HashMap<>();
      item.put("value", value);
      item.put("time", System.currentTimeMillis());

      items.put(propertyId, item);
    }

    return items;
  }
}
