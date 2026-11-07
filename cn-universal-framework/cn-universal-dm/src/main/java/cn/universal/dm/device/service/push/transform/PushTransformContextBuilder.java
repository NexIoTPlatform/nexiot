package cn.universal.dm.device.service.push.transform;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.DeviceNode;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PushTransformContextBuilder {

  public BaseUPRequest fromSample(String appUniqueId, String sampleInput) {
    if (StrUtil.isBlank(sampleInput)) {
      throw new IllegalArgumentException("调试样例不能为空");
    }
    JSONObject input = JSONUtil.parseObj(sampleInput);
    BaseUPRequest request = new BaseUPRequest();
    request.setApplicationId(appUniqueId);
    request.setUserUnionId(input.getStr("userUnionId"));
    if (StrUtil.isNotBlank(input.getStr("deviceNode"))) {
      request.setDeviceNode(DeviceNode.valueOf(input.getStr("deviceNode")));
    }
    if (StrUtil.isNotBlank(input.getStr("messageType"))) {
      request.setMessageType(MessageType.valueOf(input.getStr("messageType")));
    }
    request.setEvent(input.getStr("event"));
    request.setEventName(input.getStr("eventName"));
    request.setData(input.getJSONObject("data"));
    request.setProperties(input.getJSONObject("properties"));
    request.setRawData(input.get("rawData"));
    request.setTags(input.getJSONObject("tags"));
    request.setTime(input.getLong("time", System.currentTimeMillis()));
    request.setProductKey(input.getStr("productKey"));
    request.setIotId(input.getStr("iotId"));
    request.setDeviceId(input.getStr("deviceId"));
    request.setDeviceName(input.getStr("deviceName"));
    request.setPayload(input.getStr("payload"));
    request.setTs(input.getStr("ts"));
    request.setIoTDeviceDTO(
        IoTDeviceDTO.builder()
            .applicationId(appUniqueId)
            .deviceId(request.getDeviceId())
            .iotId(request.getIotId())
            .deviceName(request.getDeviceName())
            .productKey(request.getProductKey())
            .deviceNode(request.getDeviceNode())
            .build());
    return request;
  }

  public Map<String, Object> source(BaseUPRequest request) {
    Map<String, Object> source = new LinkedHashMap<>();
    source.put("userUnionId", request.getUserUnionId());
    source.put("deviceNode", request.getDeviceNode() == null ? null : request.getDeviceNode().name());
    source.put("messageType", request.getMessageType() == null ? null : request.getMessageType().name());
    source.put("event", request.getEvent());
    source.put("eventName", request.getEventName());
    source.put("properties", request.getProperties());
    source.put("data", request.getData());
    source.put("tags", request.getTags());
    source.put("time", ObjectUtil.defaultIfNull(request.getTime(), new Date().getTime()));
    source.put("productKey", request.getProductKey());
    source.put("iotId", request.getIotId());
    source.put("deviceId", request.getDeviceId());
    source.put("deviceName", request.getDeviceName());
    source.put("payload", request.getPayload());
    source.put("rawData", request.getRawData());
    source.put("ts", request.getTs());
    source.put("applicationId", request.getApplicationId());
    source.put("device", device(request));
    source.put("product", product(request));
    source.put("application", application(request));
    if (request.getIoTDeviceDTO() != null) {
      source.put("appId", request.getIoTDeviceDTO().getAppId());
      source.put("scope", request.getIoTDeviceDTO().getScope());
    }
    return source;
  }

  public Map<String, Object> templateBindings(Map<String, Object> source) {
    Map<String, Object> bindings = new LinkedHashMap<>(source);
    bindings.put("source", source);
    bindings.put("request", source);
    return bindings;
  }

  public Map<String, Object> scriptBindings(BaseUPRequest request, Map<String, Object> source) {
    Map<String, Object> bindings = new LinkedHashMap<>(source);
    bindings.put("ctx", source);
    bindings.put("source", source);
    bindings.put("request", request);
    bindings.put("requestMap", source);
    return bindings;
  }

  private Map<String, Object> device(BaseUPRequest request) {
    Map<String, Object> device = new LinkedHashMap<>();
    device.put("iotId", request.getIotId());
    device.put("productKey", request.getProductKey());
    device.put("deviceId", request.getDeviceId());
    device.put("deviceName", request.getDeviceName());
    device.put("deviceNode", request.getDeviceNode() == null ? null : request.getDeviceNode().name());
    if (request.getIoTDeviceDTO() != null) {
      IoTDeviceDTO dto = request.getIoTDeviceDTO();
      device.put("iotId", ObjectUtil.defaultIfNull(device.get("iotId"), dto.getIotId()));
      device.put("productKey", ObjectUtil.defaultIfNull(device.get("productKey"), dto.getProductKey()));
      device.put("deviceId", ObjectUtil.defaultIfNull(device.get("deviceId"), dto.getDeviceId()));
      device.put("deviceName", ObjectUtil.defaultIfNull(device.get("deviceName"), dto.getDeviceName()));
      device.put("deviceNode", ObjectUtil.defaultIfNull(device.get("deviceNode"), dto.getDeviceNode() == null ? null : dto.getDeviceNode().name()));
      device.put("extDeviceId", dto.getExtDeviceId());
      device.put("state", dto.getState());
      device.put("tenantId", dto.getTenantId());
      device.put("scope", dto.getScope());
    }
    return device;
  }

  private Map<String, Object> product(BaseUPRequest request) {
    Map<String, Object> product = new LinkedHashMap<>();
    product.put("productKey", request.getProductKey());
    if (request.getIoTProduct() != null) {
      product.put("productKey", ObjectUtil.defaultIfNull(product.get("productKey"), request.getIoTProduct().getProductKey()));
      product.put("productName", request.getIoTProduct().getName());
      product.put("thirdPlatform", request.getIoTProduct().getThirdPlatform());
      product.put("transportProtocol", request.getIoTProduct().getTransportProtocol());
      product.put("tenantId", request.getIoTProduct().getTenantId());
    }
    return product;
  }

  private Map<String, Object> application(BaseUPRequest request) {
    Map<String, Object> application = new LinkedHashMap<>();
    application.put("applicationId", request.getApplicationId());
    if (request.getIoTDeviceDTO() != null) {
      IoTDeviceDTO dto = request.getIoTDeviceDTO();
      application.put("applicationId", ObjectUtil.defaultIfNull(application.get("applicationId"), dto.getApplicationId()));
      application.put("appId", dto.getAppId());
      application.put("scope", dto.getScope());
      application.put("disabled", dto.getAppDisable());
    }
    return application;
  }
}
