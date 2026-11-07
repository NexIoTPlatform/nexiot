package cn.universal.admin.video.alarm;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.DeviceNode;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.common.exception.BaseException;
import cn.universal.core.alarm.VideoAlarmPayload;
import cn.universal.dm.device.service.IoTUPPushAdapter;
import cn.universal.dm.device.service.impl.IoTDeviceShadowService;
import cn.universal.dm.device.service.impl.IoTDeviceSubscribeService;
import cn.universal.dm.device.service.log.IIoTDeviceDataService;
import cn.universal.dm.video.VideoSdkEventCatalog;
import cn.universal.dm.video.VideoThingModelSynchronizer;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTDeviceSubscribe;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SdkVideoAlarmUpService extends IoTUPPushAdapter<BaseUPRequest> {

  private static final String TOKEN_HEADER = "X-NexIoT-Video-Alarm-Token";
  private static final com.fasterxml.jackson.databind.ObjectMapper PROCESS_REQUEST_JSON=new com.fasterxml.jackson.databind.ObjectMapper();

  private final VideoPlatformInstanceMapper instanceMapper;
  private final SdkVideoAlarmContextCache contextCache;
  private final VideoSdkEventCatalog eventCatalog;
  private final IIoTDeviceDataService deviceDataService;
  private final IoTDeviceShadowService shadowService;
  private final IoTDeviceSubscribeService subscribeService;
  @org.springframework.beans.factory.annotation.Autowired
  private VideoThingModelSynchronizer thingModelSynchronizer;
  @org.springframework.beans.factory.annotation.Autowired
  private VideoSdkAlarmReceiptService alarmReceipts;
  @org.springframework.beans.factory.annotation.Autowired
  private org.springframework.beans.factory.ObjectProvider<cn.universal.admin.video.service.VideoSdkRecordingTaskService> recordingTasks;
  @org.springframework.beans.factory.annotation.Autowired
  private org.springframework.beans.factory.ObjectProvider<cn.universal.admin.video.service.VideoPlatformInstanceService> videoInstances;
  @org.springframework.beans.factory.annotation.Autowired
  private org.springframework.beans.factory.ObjectProvider<VideoSdkAlarmProcessService> alarmProcesses;

  @Value("${video.sdk.alarm.callback-token:}")
  private String globalCallbackToken;

  public static String tokenHeader() {
    return TOKEN_HEADER;
  }

  public Map<String, Object> publish(SdkVideoAlarmCallbackRequest request, String token) {
    if (request == null || StrUtil.isBlank(request.getInstanceKey())) {
      throw new BaseException("缺少平台实例 instanceKey");
    }
    // 实例承载 token、停用开关和订阅范围，每次从库读取，避免安全配置被缓存延迟。
    VideoPlatformInstance instance = findInstance(request.getInstanceKey());
    verifyToken(instance, token);
    if ("hik_isup".equals(instance.getPlatformType()) && alarmReceipts != null) {
      Map<String, Object> subscription = toMap(readJsonMap(instance.getOptions()).get("subscription"));
      VideoSdkEventCatalog.Event event = eventCatalog.resolve(instance.getPlatformType(), request.getAlarmType(), eventAttributes(request));
      if (!subscribedTo(subscription, event, eventAttributes(request).get("sourceEventType")))
        return Map.of("accepted", true, "filtered", true);
      VideoPlatformDevice device = contextCache.platformDevice(instance.getInstanceKey(), request.getDeviceId(), request.getDeviceIp());
      if (device == null || !Objects.equals(device.getTenantId(), instance.getTenantId())) throw new BaseException("告警设备不存在或归属不匹配");
      request.setDeviceId(device.getDeviceId());
      if (Integer.valueOf(0).equals(device.getEnabled()) || !subscribedDevice(subscription, device.getDeviceId()))
        return Map.of("accepted", true, "filtered", true);
      return alarmReceipts.accept(instance, request);
    }
    return publishTrusted(request, instance);
  }

  Map<String, Object> publishReceipt(SdkVideoAlarmCallbackRequest request, cn.universal.persistence.entity.VideoSdkAlarmReceipt receipt) {
    VideoPlatformInstance instance = instanceMapper.selectOne(VideoPlatformInstance.builder().instanceKey(request.getInstanceKey()).build());
    if(instance==null||!Integer.valueOf(1).equals(instance.getEnabled()))return Map.of("accepted",true,"filtered",true,"ruleDispatched",false);
    if (!Objects.equals(instance.getTenantId(), receipt.getTenantId())) throw new BaseException("告警收据租户已变更");
    if ("platform_frame_request".equals(request.getAlarmType())) return publishPlatformFrame(instance,request,receipt);
    return publishTrusted(request, instance, receipt);
  }

  private Map<String,Object> publishPlatformFrame(VideoPlatformInstance instance,SdkVideoAlarmCallbackRequest request,
      cn.universal.persistence.entity.VideoSdkAlarmReceipt receipt) {
    Map<String,Object> subscription=toMap(readJsonMap(instance.getOptions()).get("subscription"));
    if(!Boolean.TRUE.equals(subscription.get("enabled"))||!"platform_frame".equals(toMap(subscription.get("eventPolicy")).get("pictureSource"))
        ||!subscribedTo(subscription,eventCatalog.resolve(instance.getPlatformType(),request.getSourceEventType(),Map.of()),null)
        ||!subscribedDevice(subscription,request.getDeviceId())
        ||System.currentTimeMillis()-receipt.getCreatedAt()>600_000)
      return Map.of("accepted",true,"filtered",true,"ruleDispatched",false);
    VideoPlatformDevice device=contextCache.platformDevice(instance.getInstanceKey(),request.getDeviceId(),null);
    if(device==null||!Objects.equals(device.getTenantId(),instance.getTenantId())||Integer.valueOf(0).equals(device.getEnabled()))
      return Map.of("accepted",true,"filtered",true,"ruleDispatched",false);
    Map<String,Object> snapshot=videoInstances.getObject().captureAlarmPlatformFrame(instance.getInstanceKey(),receipt.getTenantId(),request.getDeviceId(),request.getChannelId());
    String url=firstNonBlank(snapshot.get("ossUrl"),snapshot.get("snapshotOssUrl"),snapshot.get("snapshotUrl"),snapshot.get("url"),snapshot.get("imageUrl"));
    if(url.isBlank())throw new BaseException("告警平台画面尚未返回可访问证据");
    SdkVideoAlarmCallbackRequest evidence=new SdkVideoAlarmCallbackRequest();
    evidence.setInstanceKey(instance.getInstanceKey());evidence.setDeviceId(request.getDeviceId());evidence.setChannelId(request.getChannelId());
    evidence.setAlarmType("evidence_ready");evidence.setSdkEventFamily("gateway");evidence.setEventId("frame:"+receipt.getId());
    evidence.setSourceEventId(request.getSourceEventId());evidence.setSourceEventType(request.getSourceEventType());evidence.setAlarmTime(request.getAlarmTime());
    evidence.setImageStatus("ready");evidence.setEvidence(List.of(Map.of("url",url,"kind","image","status","ready","imageSource","platform_frame","capturedAt",System.currentTimeMillis())));
    alarmReceipts.accept(instance,evidence);
    return Map.of("accepted",true,"ruleDispatched",false,"imageSource","platform_frame","url",url);
  }

  public void publishRecordingEvidence(VideoPlatformInstance instance, cn.universal.persistence.entity.VideoSdkRecordTask task,
      Map<String,Object> metadata, Map<String,Object> result) {
    String url = firstNonBlank(result.get("url"), result.get("mp4Url"), result.get("downloadUrl"), result.get("localUrl"));
    if (url.isBlank()) throw new BaseException("片段尚未返回可访问文件");
    SdkVideoAlarmCallbackRequest request = new SdkVideoAlarmCallbackRequest();
    request.setInstanceKey(instance.getInstanceKey()); request.setDeviceId(task.getDeviceId()); request.setChannelId(task.getChannelId());
    request.setAlarmType("evidence_ready"); request.setSdkEventFamily("gateway"); request.setEventId("clip:" + task.getId());
    request.setSourceEventId(Objects.toString(metadata.get("sourceEventId"))); request.setSourceEventType(Objects.toString(metadata.get("sourceEventType"), "motion"));
    request.setAlarmTime(metadata.get("alarmTime") instanceof Number n ? n.longValue() : task.getCreatedAt());
    String status="partial".equals(result.get("status"))?"partial":"ready";
    Map<String,Object> reference=new LinkedHashMap<>(Map.of("url", url, "status", status, "kind", "video", "taskId", task.getId(), "imageSource", "device_recording"));
    for(String key:List.of("downloadUrl","mp4Url","hlsUrl","sizeBytes","contentType","durationSeconds","actualDurationSeconds","requestedDurationSeconds","coverageStatus"))
      if(result.get(key)!=null)reference.put(key,result.get(key));
    request.setEvidence(List.of(reference));
    alarmReceipts.accept(instance, request);
  }

  /** 仅供后端 WVP 已认证连接调用；HTTP SDK 回调仍严格校验平台类型和 token。 */
  public Map<String, Object> publishFromWvp(SdkVideoAlarmCallbackRequest request) {
    VideoPlatformInstance probe = new VideoPlatformInstance();
    probe.setInstanceKey(request.getInstanceKey());
    VideoPlatformInstance instance = instanceMapper.selectOne(probe);
    if (instance == null || !"wvp".equals(instance.getPlatformType())
        || !Integer.valueOf(1).equals(instance.getEnabled())) {
      throw new BaseException("WVP 平台实例不存在或未启用");
    }
    return publishTrusted(request, instance);
  }

  private Map<String, Object> publishTrusted(
      SdkVideoAlarmCallbackRequest request, VideoPlatformInstance instance) {
    return publishTrusted(request, instance, null);
  }
  private Map<String, Object> publishTrusted(SdkVideoAlarmCallbackRequest request, VideoPlatformInstance instance,
      cn.universal.persistence.entity.VideoSdkAlarmReceipt receipt) {
    Map<String, Object> activeSubscription =
        toMap(readJsonMap(instance.getOptions()).get("subscription"));
    if (!Boolean.TRUE.equals(activeSubscription.get("enabled")))
      return Map.of("accepted", true, "filtered", true);

    // 先过滤事件，避免不订阅的高频事件触发设备查询、物模型补齐和日志落库。
    Map<String, Object> attributes = eventAttributes(request);
    VideoSdkEventCatalog.Event event =
        eventCatalog.resolve(
            instance.getPlatformType(), request.getAlarmType(), attributes);
    if (!subscribedTo(activeSubscription, event, attributes.get("sourceEventType"))) {
      return Map.of("accepted", true, "filtered", true);
    }
    VideoPlatformDevice platformDevice =
        contextCache.platformDevice(
            instance.getInstanceKey(), request.getDeviceId(), request.getDeviceIp());
    if (platformDevice == null) throw new BaseException("未找到实例下的摄像头，请先同步设备");
    if (Integer.valueOf(0).equals(platformDevice.getEnabled()))
      return Map.of("accepted", true, "filtered", true);
    String deviceId = resolveDeviceId(request, platformDevice);
    // selected 缺少有效设备列表时按空列表处理，不能退化为订阅全部设备。
    if (!subscribedDevice(activeSubscription, deviceId)) {
      return Map.of("accepted", true, "filtered", true);
    }
    IoTDevice iotDevice =
        contextCache.device(instance.getInstanceKey(), deviceId, platformDevice.getDeviceIp());
    if (iotDevice == null)
      throw new BaseException("未找到关联 IoT 设备：" + instance.getInstanceKey() + "/" + deviceId);
    // 先核对设备归属，再加载可能补齐物模型的产品，不能替其他租户修改配置。
    if (!Objects.equals(instance.getInstanceKey(), iotDevice.getProductKey())
        || !Objects.equals(instance.getTenantId(), iotDevice.getTenantId())) {
      throw new BaseException("设备归属与视频实例不匹配");
    }
    IoTProduct product = contextCache.product(instance);
    if (product == null) throw new BaseException("产品不存在：" + iotDevice.getProductKey());
    if (!Objects.equals(iotDevice.getProductKey(), product.getProductKey())
        || !Objects.equals(instance.getTenantId(), iotDevice.getTenantId())
        || !Objects.equals(instance.getTenantId(), product.getTenantId())) {
      throw new BaseException("设备产品或租户与视频实例不匹配");
    }

    IoTDeviceDTO deviceDTO = toDeviceDTO(iotDevice, product);
    // 时间只取一次，保证事件外层 time 与 data.alarmTime 一致。
    long alarmTime = resolveAlarmTime(request);
    Map<String, Object> data = buildEventData(instance, platformDevice, request, alarmTime, attributes);
    if ("wvp".equals(instance.getPlatformType())) {
      // 国标编号已按 AlarmMethod + AlarmType 在目录中解析；原始编号保留在 gbAlarmType。
      data.put("alarmType", event.type());
      data.put("alarmName", event.name());
    }
    data.put("cameraEventType", event.type());
    data.put("cameraEventName", event.name());
    if (receipt != null) {
      data.put("receiptId", receipt.getId());
      String source = firstNonBlank(request.getSourceEventId(), request.getEventId());
      Map<String, Object> evidenceContext = new LinkedHashMap<>();
      evidenceContext.put("instanceKey", instance.getInstanceKey()); evidenceContext.put("deviceId", deviceId);
      evidenceContext.put("channelId", Objects.toString(request.getChannelId(), "")); evidenceContext.put("sourceEventId", source);
      evidenceContext.put("startTime", alarmTime - 30_000); evidenceContext.put("endTime", alarmTime + 30_000);
      data.put("evidenceContext", evidenceContext);
    }
    // ISUP 图片补充是原告警的取证更新；再次发布会让宽范围 Flow 重复执行告警联动。
    boolean isupEvidenceUpdate = "hik_isup".equals(instance.getPlatformType())
        && "evidence_ready".equals(event.type());
    String processId=null;
    if(receipt!=null&&alarmProcesses!=null) {
      var projection=alarmProcesses.getIfAvailable();
      if(projection!=null) {
        // Catalog normalization does not change the durable callback identity.
        SdkVideoAlarmCallbackRequest normalized=PROCESS_REQUEST_JSON.convertValue(request,SdkVideoAlarmCallbackRequest.class);
        normalized.setAlarmType(event.type());
        processId=projection.record(instance,receipt,normalized);
        if(processId!=null)data.put("processId",processId);
      }
    }
    Map<String, Object> policy = toMap(activeSubscription.get("eventPolicy"));
    int cooldown = policy.get("cooldownSeconds") instanceof Number number ? Math.max(0, Math.min(3600, number.intValue())) : 0;
    boolean continuous = Boolean.TRUE.equals(policy.get("firstOnly")) && request.getActivePostCount() != null && request.getActivePostCount() > 1;
    boolean cooled = !isupEvidenceUpdate && !continuous && receipt != null && alarmReceipts.cooledDown(receipt, cooldown);
    boolean dispatch = !isupEvidenceUpdate && !cooled && !continuous;
    boolean frameCooled = false;
    if (dispatch && receipt != null && "platform_frame".equals(policy.get("pictureSource")) && StrUtil.isNotBlank(request.getChannelId())) {
      int frameCooldown = policy.get("frameCooldownSeconds") instanceof Number number
          ? Math.max(1, Math.min(3600, number.intValue())) : 60;
      frameCooled = alarmReceipts.frameCooledDown(receipt, frameCooldown);
      if (frameCooled) data.put("platformFrameCooled", true);
      else {
      // The evidence budget does not change original alarm dispatch; retries keep their acquired slot.
      SdkVideoAlarmCallbackRequest frame = new SdkVideoAlarmCallbackRequest();
      frame.setInstanceKey(instance.getInstanceKey());frame.setDeviceId(deviceId);frame.setChannelId(request.getChannelId());
      frame.setAlarmType("platform_frame_request");frame.setEventId("frame-request:"+receipt.getId());
      frame.setSourceEventId(firstNonBlank(request.getSourceEventId(),request.getEventId()));frame.setSourceEventType(event.type());frame.setAlarmTime(alarmTime);
      data.put("platformFrameReceiptId",alarmReceipts.accept(instance,frame).get("receiptId"));
      }
    }
    if (dispatch && receipt != null && Boolean.TRUE.equals(policy.get("alarmClip")) && StrUtil.isNotBlank(request.getChannelId())) {
      long pre = policy.get("preSeconds") instanceof Number n ? n.longValue() : 5;
      long post = policy.get("postSeconds") instanceof Number n ? n.longValue() : 10;
      String taskId = java.util.UUID.nameUUIDFromBytes((receipt.getId() + ":clip").getBytes(StandardCharsets.UTF_8)).toString();
      try {
        Map<String, Object> task = recordingTasks.getObject().create(instance, Map.of("requestId", taskId, "deviceId", deviceId,
            "channelId", request.getChannelId(), "mode", "export", "startTime", alarmTime - pre * 1000,
            "endTime", alarmTime + Math.max(1, post) * 1000, "taskName", "告警片段 " + request.getEventId(), "sourceEventId", firstNonBlank(request.getSourceEventId(),request.getEventId()),
            "sourceEventType", event.type(), "alarmTime", alarmTime), instance.getCreatorId());
        data.put("alarmClipTaskId", task.get("id"));
      } catch (Exception ex) { throw new BaseException("告警片段任务尚未持久创建，请重试"); }
    }
    // 入库、订阅和规则共用精简结果；不能仅在界面隐藏原始副本。
    data = VideoAlarmEventData.compact(data, event.name());
    thingModelSynchronizer.observe(product, event.id(), event.name(), data, null);
    deviceDTO.setMetadata(product.getMetadata());
    BaseUPRequest upRequest =
        BaseUPRequest.builder()
            .deviceNode(DeviceNode.DEVICE)
            .ioTDeviceDTO(deviceDTO)
            .ioTProduct(product)
            .iotId(deviceDTO.getIotId())
            .deviceId(deviceDTO.getDeviceId())
            .deviceName(deviceDTO.getDeviceName())
            .productKey(deviceDTO.getProductKey())
            .userUnionId(deviceDTO.getUserUnionId())
            .messageType(MessageType.EVENT)
            .event(event.id())
            .eventName(event.name())
            .data(data)
            .time(alarmTime)
            .build();
    List<IoTDeviceSubscribe> subscribes =
        subscribeService.selectByProductKeyAndMsgType(
            upRequest.getProductKey(), upRequest.getIotId(), upRequest.getMessageType());
    upRequest.setDevSubscribe(subscribes);

    // 依次进入统一日志、影子和订阅推送链路；不在回调中另建旁路推送。
    if (receipt == null || !Integer.valueOf(1).equals(receipt.getLogPersisted())) {
    if ("wvp".equals(instance.getPlatformType())) {
      deviceDataService.saveDeviceLogDirect(upRequest, deviceDTO, product);
    } else {
      deviceDataService.saveDeviceLog(upRequest, deviceDTO, product);
    }
    shadowService.doShadow(upRequest, deviceDTO);
    if (receipt != null) alarmReceipts.logged(receipt);
    }
    if (dispatch) doUp(new java.util.ArrayList<>(List.of(upRequest)));

    log.debug(
        "[视频SDK告警] 已发布物模型事件 instanceKey={}, platformType={}, deviceId={}, iotId={}, alarmType={},"
            + " channelId={}",
        instance.getInstanceKey(),
        instance.getPlatformType(),
        deviceId,
        upRequest.getIotId(),
        data.get("cameraEventType"),
        data.get("channelId"));

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("accepted", true);
    result.put("event", event.id());
    result.put("eventName", upRequest.getEventName());
    result.put("productKey", upRequest.getProductKey());
    result.put("deviceId", upRequest.getDeviceId());
    result.put("iotId", upRequest.getIotId());
    result.put("time", upRequest.getTime());
    if(processId!=null)result.put("processId",processId);
    if(frameCooled)result.put("platformFrameCooled",true);
    if (receipt != null) result.put("ruleDispatched", dispatch);
    if (cooled || continuous) result.put("aggregated", true);
    if (isupEvidenceUpdate) {
      result.put("evidenceUpdate", true);
      result.put("ruleDispatched", false);
    }
    return result;
  }

  private VideoPlatformInstance findInstance(String instanceKey) {
    VideoPlatformInstance probe = new VideoPlatformInstance();
    probe.setInstanceKey(instanceKey);
    VideoPlatformInstance instance = instanceMapper.selectOne(probe);
    if (instance == null || instance.getEnabled() != null && instance.getEnabled() == 0) {
      throw new BaseException("平台实例不存在或未启用：" + instanceKey);
    }
    if (!List.of("hik_sdk", "dahua_sdk", "tiandy_sdk", "uniview_sdk", "hik_isup")
        .contains(StrUtil.blankToDefault(instance.getPlatformType(), ""))) {
      throw new BaseException("该实例不是直连 SDK 视频平台");
    }
    return instance;
  }

  private void verifyToken(VideoPlatformInstance instance, String token) {
    String configured = resolveCallbackToken(instance);
    if (StrUtil.isBlank(configured)) {
      throw new BaseException("SDK告警回调 token 未配置");
    }
    // 按字节常量时间比较 token，不把 token 或原始请求写入日志。
    if (token == null
        || !MessageDigest.isEqual(
            configured.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
      throw new BaseException("SDK告警回调 token 校验失败");
    }
  }

  private String resolveCallbackToken(VideoPlatformInstance instance) {
    Map<String, Object> options = readJsonMap(instance.getOptions());
    Map<String, Object> subscription = toMap(options.get("subscription"));
    return firstNonBlank(
        subscription.get("token"),
        subscription.get("callbackToken"),
        subscription.get("secret"),
        options.get("alarmCallbackToken"),
        options.get("callbackToken"),
        globalCallbackToken);
  }

  private String resolveDeviceId(
      SdkVideoAlarmCallbackRequest request, VideoPlatformDevice platformDevice) {
    return firstNonBlank(
        platformDevice == null ? null : platformDevice.getDeviceId(),
        request.getDeviceId(),
        request.getDeviceIp());
  }

  private IoTDeviceDTO toDeviceDTO(IoTDevice device, IoTProduct product) {
    return IoTDeviceDTO.builder()
        .id(device.getId())
        .applicationId(device.getApplication())
        .appId(device.getApplication())
        .userUnionId(device.getCreatorId())
        .tenantId(device.getTenantId())
        .iotId(device.getIotId())
        .deviceId(device.getDeviceId())
        .extDeviceId(device.getExtDeviceId())
        .deviceName(device.getDeviceName())
        .productKey(device.getProductKey())
        .productName(device.getProductName())
        .productCreatorId(product.getCreatorId())
        .thirdPlatform(product.getThirdPlatform())
        .gwProductKey(device.getGwProductKey())
        .deviceSecret(device.getDeviceSecret())
        .metadata(product.getMetadata())
        .configuration(product.getConfiguration())
        .state(device.getState())
        .devConfiguration(device.getConfiguration())
        .payload("")
        .build();
  }

  private Map<String, Object> buildEventData(
      VideoPlatformInstance instance,
      VideoPlatformDevice platformDevice,
      SdkVideoAlarmCallbackRequest request,
      long alarmTime,
      Map<String, Object> attributes) {
    Map<String, Object> data = new LinkedHashMap<>();
    // 原报文中独有的已解码业务值仍可作为规则条件，不再复制整个 rawData 对象。
    if (request.getRaw() != null) data.putAll(VideoAlarmPayload.safeData(request.getRaw()));
    if (request.getData() != null) {
      data.putAll(VideoAlarmPayload.safeData(request.getData()));
    }
    // 可信实例和设备标识覆盖来源报文中的同名字段。
    data.put("instanceKey", instance.getInstanceKey());
    data.put("platformType", instance.getPlatformType());
    putIfNotBlank(data, "vendor", request.getVendor());
    putIfNotBlank(data, "deviceId", resolveDeviceId(request, platformDevice));
    putIfNotBlank(
        data,
        "deviceName",
        firstNonBlank(
            request.getDeviceName(),
            platformDevice == null ? null : platformDevice.getDeviceName()));
    putIfNotBlank(
        data,
        "deviceIp",
        firstNonBlank(
            request.getDeviceIp(), platformDevice == null ? null : platformDevice.getDeviceIp()));
    putIfNotBlank(data, "channelId", request.getChannelId());
    putIfNotBlank(data, "channelName", request.getChannelName());
    putIfNotBlank(data, "alarmType", request.getAlarmType());
    putIfNotBlank(data, "alarmName", request.getAlarmName());
    putIfNotBlank(data, "alarmState", request.getAlarmState());
    putIfNotBlank(data, "alarmLevel", request.getAlarmLevel());
    putIfNotBlank(data, "description", request.getDescription());
    putIfNotBlank(data, "eventId", request.getEventId());
    putIfAbsentNotBlank(data, "sourceEventId", request.getSourceEventId());
    putIfAbsentNotBlank(data, "sourceEventType", request.getSourceEventType());
    putIfAbsentNotBlank(data, "imageStatus", request.getImageStatus());
    if (request.getPictureCount() != null) data.putIfAbsent("pictureCount", request.getPictureCount());
    if (request.getActivePostCount() != null) data.putIfAbsent("activePostCount", request.getActivePostCount());
    if (request.getImages() != null) data.putIfAbsent("images",
        VideoAlarmPayload.safeData(Map.of("images", request.getImages())).get("images"));
    if (request.getEvidence() != null) data.putIfAbsent("evidence",
        VideoAlarmPayload.safeData(Map.of("evidence", request.getEvidence())).get("evidence"));
    putIfAbsentNotBlank(
        data,
        "cameraEventType",
        firstNonBlank(data.get("alarmType"), request.getAlarmType(), data.get("type")));
    putIfAbsentNotBlank(
        data,
        "cameraEventName",
        firstNonBlank(data.get("alarmName"), request.getAlarmName(), data.get("name")));
    putIfAbsentNotBlank(
        data,
        "vendorEventCode",
        firstNonBlank(data.get("vendorEventCode"), data.get("command"), data.get("code")));
    putIfAbsentNotBlank(
        data,
        "vendorEventCodeHex",
        firstNonBlank(data.get("vendorEventCodeHex"), toHex(data.get("vendorEventCode"))));
    // 顶层编号与 data/raw 使用相同优先级，日志保留用于字典匹配的实际编号。
    putIfNotBlank(data, "vendorCameraEventType", attributes.get("cameraEventType"));
    for (String key :
        List.of("vendorEventCode", "vendorEventCodeHex", "vendorAlarmType", "sdkEventFamily")) {
      if (attributes.get(key) != null) data.put(key, attributes.get(key));
    }
    putIfNotBlank(data, "vendorEventCodeHex", toHex(data.get("vendorEventCode")));
    data.put("alarmTime", alarmTime);
    return data;
  }

  private static boolean subscribedTo(Map<String, Object> subscription, VideoSdkEventCatalog.Event event, Object sourceType) {
    if (!Boolean.TRUE.equals(subscription.get("enabled"))
        || !(subscription.get("topics") instanceof List<?> topics) || topics.isEmpty()) return false;
    return topics.contains("cameraAlarm") || topics.contains("alarm")
        || topics.contains(event.type())
        || ("evidence_ready".equals(event.type()) && sourceType != null && topics.contains(sourceType));
  }

  private static boolean subscribedDevice(Map<String, Object> subscription, String deviceId) {
    return !"selected".equals(subscription.get("scope"))
        || subscription.get("deviceIds") instanceof List<?> ids && ids.contains(deviceId);
  }

  private Map<String, Object> eventAttributes(SdkVideoAlarmCallbackRequest request) {
    Map<String, Object> attributes = new LinkedHashMap<>();
    if (request.getRaw() != null) attributes.putAll(request.getRaw());
    if (request.getData() != null) attributes.putAll(request.getData());
    putIfNotBlank(attributes, "vendorEventCode", request.getVendorEventCode());
    putIfNotBlank(attributes, "vendorEventCodeHex", request.getVendorEventCodeHex());
    putIfNotBlank(attributes, "vendorAlarmType", request.getVendorAlarmType());
    putIfNotBlank(attributes, "sdkEventFamily", request.getSdkEventFamily());
    putIfNotBlank(attributes, "cameraEventType", request.getCameraEventType());
    putIfAbsentNotBlank(attributes, "sourceEventType", request.getSourceEventType());
    return attributes;
  }

  private long resolveAlarmTime(SdkVideoAlarmCallbackRequest request) {
    return request.getAlarmTime() == null || request.getAlarmTime() <= 0
        ? System.currentTimeMillis()
        : request.getAlarmTime();
  }

  private Map<String, Object> readJsonMap(String json) {
    if (StrUtil.isBlank(json) || !JSONUtil.isTypeJSON(json)) {
      return new LinkedHashMap<>();
    }
    return toMap(JSONUtil.parseObj(json));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> toMap(Object value) {
    Map<String, Object> result = new LinkedHashMap<>();
    if (value instanceof JSONObject json) {
      json.forEach((key, item) -> result.put(String.valueOf(key), item));
    } else if (value instanceof Map<?, ?> map) {
      map.forEach(
          (key, item) -> {
            if (key != null) {
              result.put(String.valueOf(key), item);
            }
          });
    }
    return result;
  }

  private void putIfNotBlank(Map<String, Object> map, String key, Object value) {
    String text = value == null ? null : String.valueOf(value);
    if (StrUtil.isNotBlank(text)) {
      map.put(key, value);
    }
  }

  private void putIfAbsentNotBlank(Map<String, Object> map, String key, Object value) {
    if (map == null || map.containsKey(key)) {
      return;
    }
    putIfNotBlank(map, key, value);
  }

  private String toHex(Object value) {
    if (value == null) {
      return "";
    }
    try {
      if (value instanceof Number number) {
        long code = number.longValue();
        return "0x"
            + Long.toHexString(code < 0 && code >= Integer.MIN_VALUE ? code & 0xffffffffL : code);
      }
      String text = String.valueOf(value).trim();
      if (StrUtil.isBlank(text) || text.startsWith("0x") || text.startsWith("0X")) {
        return text;
      }
      long code = Long.parseLong(text);
      return "0x"
          + Long.toHexString(code < 0 && code >= Integer.MIN_VALUE ? code & 0xffffffffL : code);
    } catch (Exception ignore) {
      return "";
    }
  }

  private String firstNonBlank(Object... values) {
    if (values == null) {
      return "";
    }
    for (Object value : values) {
      if (value != null && StrUtil.isNotBlank(String.valueOf(value))) {
        return String.valueOf(value);
      }
    }
    return "";
  }
}
