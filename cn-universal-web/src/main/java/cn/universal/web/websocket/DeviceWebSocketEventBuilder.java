package cn.universal.web.websocket;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.core.message.DownRequest;
import cn.universal.persistence.base.IoTDevicePostProcessor;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.bo.DeviceWebSocketPushEvent;
import cn.universal.persistence.entity.bo.WebSocketPushSettingsBO;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DeviceWebSocketEventBuilder {

  public DeviceWebSocketPushEvent build(
      IoTDevicePostProcessor.Operation operation, IoTDeviceDTO deviceDTO, DownRequest downRequest) {
    if (operation == null || deviceDTO == null) {
      return null;
    }
    return switch (operation) {
      case CREATE -> buildCreateEvent(deviceDTO);
      case ONLINE -> buildOnlineEvent(deviceDTO);
      case OFFLINE -> buildOfflineEvent(deviceDTO);
      case COMMAND_RESP -> buildCommandRespEvent(deviceDTO, downRequest);
      default -> null;
    };
  }

  private DeviceWebSocketPushEvent buildCreateEvent(IoTDeviceDTO deviceDTO) {
    return baseBuilder(deviceDTO)
        .category(WebSocketPushSettingsBO.DEVICE_CREATE)
        .title("设备注册")
        .routePath("/iot/device")
        .summary(String.format("%s 已注册", deviceLabel(deviceDTO)))
        .content(String.format("产品名称：%s", StrUtil.blankToDefault(deviceDTO.getProductName(), "-")))
        .level("success")
        .build();
  }

  private DeviceWebSocketPushEvent buildOnlineEvent(IoTDeviceDTO deviceDTO) {
    return baseBuilder(deviceDTO)
        .category(WebSocketPushSettingsBO.DEVICE_ONLINE)
        .title("设备上线")
        .routePath("/iot/device")
        .summary(String.format("%s 已上线", deviceLabel(deviceDTO)))
        .content(deviceIdMeta(deviceDTO))
        .level("success")
        .build();
  }

  private DeviceWebSocketPushEvent buildOfflineEvent(IoTDeviceDTO deviceDTO) {
    return baseBuilder(deviceDTO)
        .category(WebSocketPushSettingsBO.DEVICE_OFFLINE)
        .title("设备离线")
        .routePath("/iot/device")
        .summary(String.format("%s 已离线", deviceLabel(deviceDTO)))
        .content(deviceIdMeta(deviceDTO))
        .level("warning")
        .build();
  }

  private DeviceWebSocketPushEvent buildCommandRespEvent(
      IoTDeviceDTO deviceDTO, DownRequest downRequest) {
    Map<String, Object> payload = downRequest == null ? null : downRequest.getFunction();
    Object responsePayload = payload == null ? null : payload.getOrDefault("payload", payload);
    String commandId = downRequest == null ? null : downRequest.getMsgId();
    String functionLabel = resolveFunctionLabel(downRequest, payload);
    return baseBuilder(deviceDTO)
        .category(WebSocketPushSettingsBO.COMMAND_RESP)
        .title("执行成功")
        .summary(buildCommandSummary(deviceDTO, functionLabel))
        .content(buildCommandMeta(deviceDTO, commandId))
        .level("success")
        .commandId(commandId)
        .payload(responsePayload)
        .build();
  }

  private String resolveFunctionLabel(DownRequest downRequest, Map<String, Object> payload) {
    if (MapUtil.isNotEmpty(payload)) {
      String functionName = simplifyFunctionLabel(MapUtil.getStr(payload, "functionName"));
      if (StrUtil.isNotBlank(functionName)) {
        return functionName;
      }
    }

    String detail = simplifyFunctionLabel(downRequest == null ? null : downRequest.getDetail());
    if (StrUtil.isNotBlank(detail)) {
      return detail;
    }

    if (MapUtil.isNotEmpty(payload)) {
      String function = simplifyFunctionLabel(MapUtil.getStr(payload, "function"));
      if (StrUtil.isNotBlank(function)) {
        return function;
      }
    }
    return null;
  }

  private DeviceWebSocketPushEvent.DeviceWebSocketPushEventBuilder baseBuilder(IoTDeviceDTO deviceDTO) {
    return DeviceWebSocketPushEvent.builder()
        .timestamp(System.currentTimeMillis())
        .tenantId(deviceDTO.getTenantId())
        .productKey(deviceDTO.getProductKey())
        .deviceId(deviceDTO.getDeviceId())
        .deviceName(deviceDTO.getDeviceName())
        .ownerUnionId(
            StrUtil.blankToDefault(deviceDTO.getUserUnionId(), deviceDTO.getProductCreatorId()));
  }

  private String deviceLabel(IoTDeviceDTO deviceDTO) {
    return StrUtil.blankToDefault(deviceDTO.getDeviceName(), deviceDTO.getDeviceId());
  }

  private String deviceIdMeta(IoTDeviceDTO deviceDTO) {
    return "设备ID：" + StrUtil.blankToDefault(deviceDTO.getDeviceId(), "-");
  }

  private String buildCommandSummary(IoTDeviceDTO deviceDTO, String functionLabel) {
    if (StrUtil.isBlank(functionLabel)) {
      return String.format("%s 指令执行成功", deviceLabel(deviceDTO));
    }

    return String.format("%s：%s 执行成功", deviceLabel(deviceDTO), functionLabel);
  }

  private String buildCommandMeta(IoTDeviceDTO deviceDTO, String commandId) {
    String deviceId = deviceIdMeta(deviceDTO);
    if (StrUtil.isBlank(commandId)) {
      return deviceId;
    }

    return "指令ID：" + commandId;
  }

  private String simplifyFunctionLabel(String functionLabel) {
    String value = StrUtil.trim(functionLabel);
    if (StrUtil.isBlank(value)) {
      return null;
    }

    String wrappedLabel = extractWrappedLabel(value, '（', '）');
    if (StrUtil.isNotBlank(wrappedLabel)) {
      return wrappedLabel;
    }

    wrappedLabel = extractWrappedLabel(value, '(', ')');
    if (StrUtil.isNotBlank(wrappedLabel)) {
      return wrappedLabel;
    }

    return value;
  }

  private String extractWrappedLabel(String value, char left, char right) {
    int start = value.indexOf(left);
    int end = value.lastIndexOf(right);
    if (start < 0 || end <= start + 1) {
      return null;
    }

    return StrUtil.trim(value.substring(start + 1, end));
  }
}
