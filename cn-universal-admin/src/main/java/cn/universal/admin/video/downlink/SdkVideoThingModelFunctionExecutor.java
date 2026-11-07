package cn.universal.admin.video.downlink;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.video.service.VideoPlatformInstanceService;
import cn.universal.common.constant.CameraCommand;
import cn.universal.common.domain.R;
import cn.universal.common.exception.BaseException;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.VideoPlatformInstance;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 将视频物模型函数调用桥接到视频平台统一能力。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SdkVideoThingModelFunctionExecutor {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final VideoPlatformInstanceService instanceService;
  private final ObjectMapper objectMapper;

  public R<?> execute(String protocolCode, IoTProduct product, UnifiedDownlinkCommand command) {
    if (product == null) {
      return R.error("产品不存在");
    }
    if (command == null) {
      return R.error("下行指令为空");
    }
    String instanceKey = resolveInstanceKey(product);
    if (StrUtil.isBlank(instanceKey)) {
      instanceKey = product.getProductKey();
    }
    VideoPlatformInstance instance = instanceService.getByInstanceKey(instanceKey);
    if (instance == null) {
      return R.error("视频平台实例不存在: " + instanceKey);
    }
    if (!java.util.Objects.equals(product.getTenantId(), instance.getTenantId()))
      return R.error("产品与视频实例租户归属不一致");
    if (StrUtil.isNotBlank(protocolCode)
        && StrUtil.isNotBlank(instance.getPlatformType())
        && !protocolCode.equalsIgnoreCase(instance.getPlatformType())) {
      return R.error(
          "产品协议与视频实例类型不一致: productProtocol="
              + protocolCode
              + ", instancePlatform="
              + instance.getPlatformType());
    }

    FunctionCall call = resolveFunctionCall(command);
    if (StrUtil.isBlank(call.functionName())) {
      return R.error("缺少物模型函数名");
    }

    try {
      return switch (call.functionName()) {
        case "cameraLiveStream" -> R.ok(handleLiveStream(instanceKey, command, call.data()));
        case "stopPreview" -> R.ok(handleStopPreview(instanceKey, command, call.data()));
        case "cameraPlayback" -> R.ok(handlePlayback(instanceKey, command, call.data()));
        case "stopPlayback" -> R.ok(handleStopPlayback(instance, command, call.data()));
        case "cameraTurn" -> handlePtz(instanceKey, command, call.data());
        case "cameraSnapshot" -> R.ok(handleSnapshot(instanceKey, command, call.data()));
        case "queryRecords" -> R.ok(handleQueryRecords(instanceKey, command, call.data()));
        case "queryCloudRecords" -> handleQueryCloudRecords(instanceKey, command, call.data());
        case "loadCloudRecord" -> handleLoadCloudRecord(instanceKey, command, call.data());
        default -> {
          if (!"hik_isup".equals(instance.getPlatformType())) yield R.error("不支持的视频物模型函数: " + call.functionName());
          var definition = cn.universal.dm.video.VideoOperationRegistry.requirePublic(call.functionName());
          Map<String, Object> input = new LinkedHashMap<>(call.data());
          if (definition.scope() != cn.universal.dm.video.VideoOperationRegistry.Scope.INSTANCE)
            input.put("deviceId", requiredDeviceId(command, input));
          yield R.ok(instanceService.executePlatformOperation(instanceKey, definition.id(), input));
        }
      };
    } catch (BaseException e) {
      log.warn("[视频物模型下行] 执行失败: protocol={}, function={}, error={}", protocolCode, call.functionName(), e.getMessage());
      return R.error(e.getMessage());
    } catch (Exception e) {
      log.error("[视频物模型下行] 执行异常: protocol={}, function={}", protocolCode, call.functionName(), e);
      return R.error("视频物模型函数执行失败: " + e.getMessage());
    }
  }

  private Map<String, Object> handleLiveStream(
      String instanceKey, UnifiedDownlinkCommand command, Map<String, Object> data) {
    String deviceId = requiredDeviceId(command, data);
    String channelId = requiredString(data, "channelId");
    String streamType = StrUtil.blankToDefault(stringValue(data.get("streamType")), "main");
    Map<String, String> options = toStringOptions(data);
    options.remove("deviceId");
    options.remove("channelId");
    options.remove("streamType");
    Map<String, String> urls =
        instanceService.getStreamUrl(instanceKey, deviceId, channelId, streamType, options);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("deviceId", deviceId);
    result.put("channelId", channelId);
    result.put("streamType", streamType);
    if (urls != null) {
      result.putAll(urls);
      result.putIfAbsent("mediaServerId", firstNonBlank(urls.get("mediaServerId"), urls.get("requestedMediaServerId")));
      result.putIfAbsent("app", firstNonBlank(urls.get("app"), urls.get("zlmApp")));
      result.putIfAbsent("stream", firstNonBlank(urls.get("stream"), urls.get("zlmStream")));
    }
    result.put("ts", System.currentTimeMillis());
    return result;
  }

  private Map<String, Object> handleStopPreview(
      String instanceKey, UnifiedDownlinkCommand command, Map<String, Object> data) {
    String deviceId = requiredDeviceId(command, data);
    String channelId = requiredString(data, "channelId");
    Map<String, String> options = toStringOptions(data);
    options.remove("deviceId");
    options.remove("channelId");
    return instanceService.stopPreview(instanceKey, deviceId, channelId, options);
  }

  private Map<String, Object> handlePlayback(
      String instanceKey, UnifiedDownlinkCommand command, Map<String, Object> data) {
    String deviceId = requiredDeviceId(command, data);
    String channelId = requiredString(data, "channelId");
    long startTime = parseTime(data.get("startTime"), 0L);
    long endTime = parseTime(data.get("endTime"), System.currentTimeMillis());
    Map<String, String> options = toStringOptions(data);
    options.remove("deviceId");
    options.remove("channelId");
    options.remove("startTime");
    options.remove("endTime");
    Map<String, Object> playback;
    if ("hik_isup".equals(instanceService.getByInstanceKey(instanceKey).getPlatformType())) {
      Map<String, Object> request = new LinkedHashMap<>(data);
      request.put("deviceId", deviceId);
      request.put("channelId", channelId);
      request.put("startTime", startTime);
      request.put("endTime", endTime);
      playback = instanceService.executePlatformOperation(instanceKey, "playbackStreamStart", request);
    } else {
      playback = instanceService.getPlaybackUrl(instanceKey, deviceId, channelId, startTime, endTime, options);
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("deviceId", deviceId);
    result.put("channelId", channelId);
    result.put("startTime", startTime);
    result.put("endTime", endTime);
    if (playback != null) {
      result.putAll(playback);
    }
    result.put("ts", System.currentTimeMillis());
    return result;
  }

  private Map<String, Object> handleStopPlayback(
      VideoPlatformInstance instance, UnifiedDownlinkCommand command, Map<String, Object> data) {
    if (!"hik_isup".equals(instance.getPlatformType()))
      return handleStopPreview(instance.getInstanceKey(), command, data);
    String playbackId = requiredString(data, "playbackId");
    return instanceService.executePlatformOperation(instance.getInstanceKey(), "playbackStreamStop",
        Map.of("playbackId", playbackId, "deviceId", requiredDeviceId(command, data)));
  }

  private R<?> handlePtz(String instanceKey, UnifiedDownlinkCommand command, Map<String, Object> data) {
    String deviceId = requiredDeviceId(command, data);
    String channelId = requiredString(data, "channelId");
    String action = requiredString(data, "command");
    int speed = parseInt(data.get("speed"), 50);
    instanceService.controlPTZ(instanceKey, deviceId, channelId, action, speed);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("deviceId", deviceId);
    result.put("channelId", channelId);
    result.put("command", action);
    result.put("speed", speed);
    result.put("executed", true);
    result.put("ts", System.currentTimeMillis());
    return R.ok(result);
  }

  private List<Map<String, Object>> handleQueryRecords(
      String instanceKey, UnifiedDownlinkCommand command, Map<String, Object> data) {
    String deviceId = requiredDeviceId(command, data);
    String channelId = requiredString(data, "channelId");
    long startTime = parseTime(data.get("startTime"), 0L);
    long endTime = parseTime(data.get("endTime"), System.currentTimeMillis());
    Map<String, String> options = toStringOptions(data);
    options.remove("deviceId");
    options.remove("channelId");
    options.remove("startTime");
    options.remove("endTime");
    List<Map<String, Object>> records =
        instanceService.queryRecords(instanceKey, deviceId, channelId, startTime, endTime, options);
    return records == null ? List.of() : records;
  }

  private Map<String, Object> handleSnapshot(
      String instanceKey, UnifiedDownlinkCommand command, Map<String, Object> data) {
    String deviceId = requiredDeviceId(command, data);
    String channelId = requiredString(data, "channelId");
    Map<String, Object> params = new LinkedHashMap<>(data);
    params.put("deviceId", deviceId);
    params.put("channelId", channelId);
    Map<String, Object> operation =
        instanceService.captureSnapshot(instanceKey, deviceId, channelId, params);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("deviceId", deviceId);
    result.put("channelId", channelId);
    if (operation != null) {
      result.putAll(operation);
    }
    result.putIfAbsent("snapshotUrl", firstNonBlank(
        stringValue(result.get("snapshotUrl")),
        stringValue(result.get("snap")),
        stringValue(result.get("url")),
        stringValue(result.get("imageUrl"))));
    result.putIfAbsent("provider", "sdk_operation");
    result.put("ts", System.currentTimeMillis());
    return result;
  }

  private R<?> handleQueryCloudRecords(
      String instanceKey, UnifiedDownlinkCommand command, Map<String, Object> data) {
    String deviceId = stringValue(data.get("deviceId"));
    String channelId = stringValue(data.get("channelId"));
    Map<String, String> params = toStringOptions(data);
    cn.universal.common.domain.R<Map<String, Object>> response =
        instanceService.queryCloudRecords(instanceKey, deviceId, channelId, params);
    return response == null ? R.error("查询云端录像失败") : response;
  }

  private R<?> handleLoadCloudRecord(
      String instanceKey, UnifiedDownlinkCommand command, Map<String, Object> data) {
    String deviceId = stringValue(data.get("deviceId"));
    String channelId = stringValue(data.get("channelId"));
    cn.universal.common.domain.R<Map<String, Object>> response =
        instanceService.loadCloudRecord(instanceKey, deviceId, channelId, data);
    return response == null ? R.error("加载云端录像失败") : response;
  }

  private FunctionCall resolveFunctionCall(UnifiedDownlinkCommand command) {
    Map<String, Object> function = command.getFunction();
    String functionName = null;
    Map<String, Object> data = new LinkedHashMap<>();
    if (function != null && !function.isEmpty()) {
      Object nestedFunction = function.get("function");
      if (nestedFunction instanceof Map<?, ?> nestedMap) {
        functionName = stringValue(nestedMap.get("function"));
        Object nestedData = nestedMap.get("data");
        if (nestedData instanceof Map<?, ?> map) {
          data.putAll(toLinkedMap(map));
        } else if (nestedData != null) {
          data.putAll(readMap(nestedData));
        }
      } else {
        functionName = stringValue(nestedFunction);
      }
      Object nestedData = function.get("data");
      if (nestedData instanceof Map<?, ?> map) {
        data.putAll(toLinkedMap(map));
      } else if (nestedData != null) {
        data.putAll(readMap(nestedData));
      }
      if (StrUtil.isBlank(functionName)) {
        functionName = stringValue(function.get("id"));
      }
    }
    JSONObject rawData = command.getRawData();
    if (rawData != null) {
      if (StrUtil.isBlank(functionName)) {
        functionName = rawData.getStr("function");
      }
      Object rawDataValue = rawData.get("data");
      if (rawDataValue != null) {
        data.putAll(readMap(rawDataValue));
      }
    }
    // 参数优先级：嵌套 function.data < 外层 data < rawData.data < extensions。
    if (command.getExtensions() != null) {
      data.putAll(command.getExtensions());
    }
    // 将历史函数别名归一为 CameraCommand，避免厂商执行器各自维护一套命名。
    CameraCommand cameraCommand = CameraCommand.fromFunctionName(functionName);
    return new FunctionCall(cameraCommand == null ? functionName : cameraCommand.getFunctionName(), data);
  }

  private String resolveInstanceKey(IoTProduct product) {
    Map<String, Object> thirdConfig = readMap(product.getThirdConfiguration());
    return firstNonBlank(stringValue(thirdConfig.get("instanceKey")), product.getProductKey());
  }

  private String requiredDeviceId(UnifiedDownlinkCommand command, Map<String, Object> data) {
    if (StrUtil.isNotBlank(command.getDeviceId()) && StrUtil.isNotBlank(stringValue(data.get("deviceId")))
        && !command.getDeviceId().equals(stringValue(data.get("deviceId")))) throw new BaseException("操作设备与下行指令上下文不一致");
    String deviceId = firstNonBlank(stringValue(data.get("deviceId")), command.getDeviceId());
    if (StrUtil.isBlank(deviceId)) {
      throw new BaseException("缺少参数: deviceId");
    }
    return deviceId;
  }

  private String requiredString(Map<String, Object> data, String key) {
    String value = stringValue(data.get(key));
    if (StrUtil.isBlank(value)) {
      throw new BaseException("缺少参数: " + key);
    }
    return value;
  }

  private long parseTime(Object value, long defaultValue) {
    String text = stringValue(value);
    if (StrUtil.isBlank(text)) {
      return defaultValue;
    }
    try {
      if (text.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(text).getTime();
      }
      long timestamp = Long.parseLong(text);
      return text.length() == 10 ? timestamp * 1000 : timestamp;
    } catch (Exception e) {
      throw new BaseException("时间格式错误: " + e.getMessage());
    }
  }

  private int parseInt(Object value, int defaultValue) {
    String text = stringValue(value);
    if (StrUtil.isBlank(text)) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(text);
    } catch (Exception e) {
      return defaultValue;
    }
  }

  private Map<String, String> toStringOptions(Map<String, Object> data) {
    Map<String, String> options = new LinkedHashMap<>();
    if (data == null) {
      return options;
    }
    data.forEach(
        (key, value) -> {
          if (value != null) {
            options.put(key, stringValue(value));
          }
        });
    return options;
  }

  private Map<String, Object> readMap(Object value) {
    if (value == null) {
      return new LinkedHashMap<>();
    }
    if (value instanceof JSONObject jsonObject) {
      return JSONUtil.toBean(jsonObject, Map.class);
    }
    if (value instanceof Map<?, ?> map) {
      return toLinkedMap(map);
    }
    if (value instanceof String text && JSONUtil.isTypeJSON(text)) {
      return JSONUtil.toBean(text, Map.class);
    }
    return objectMapper.convertValue(value, MAP_TYPE);
  }

  private Map<String, Object> toLinkedMap(Map<?, ?> map) {
    Map<String, Object> result = new LinkedHashMap<>();
    if (map != null) {
      map.forEach((key, value) -> result.put(String.valueOf(key), value));
    }
    return result;
  }

  private String stringValue(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Date date) {
      return String.valueOf(date.getTime());
    }
    return String.valueOf(value).trim();
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (StrUtil.isNotBlank(value)) {
        return value;
      }
    }
    return null;
  }

  private record FunctionCall(String functionName, Map<String, Object> data) {}
}
