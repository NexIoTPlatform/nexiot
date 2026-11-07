/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.dm.device.service.action;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.DevLifeCycle;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.core.message.DownRequest;
import cn.universal.core.metadata.AbstractEventMetadata;
import cn.universal.core.metadata.AbstractFunctionMetadata;
import cn.universal.core.metadata.DeviceMetadata;
import cn.universal.dm.device.service.IoTUPPushAdapter;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.dm.device.service.log.IIoTDeviceDataService;
import cn.universal.dm.device.service.processor.DevicePostProcessorManager;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.base.IoTDeviceLifeCycle;
import cn.universal.persistence.base.IoTDevicePostProcessor;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTDeviceLogMapper;
import cn.universal.persistence.mapper.IoTDeviceLogShardMapper;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import jakarta.annotation.Resource;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

/**
 * 生命周期后置处理
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/1/20
 */
@Service("ioTDeviceActionAfterService")
@Slf4j
public class IoTDeviceActionAfterService extends IoTUPPushAdapter<BaseUPRequest>
    implements IoTDeviceLifeCycle {

  @Resource private IoTDeviceMapper ioTDeviceMapper;

  @Resource private IIoTDeviceDataService iIoTDeviceDataService;

  @Resource private IoTProductDeviceService iotProductDeviceService;

  @Resource private StringRedisTemplate stringRedisTemplate;

  @Resource private IoTDeviceService iotDeviceService;

  @Resource private IoTDeviceLogMapper ioTDeviceLogMapper;
  @Resource private IoTDeviceLogShardMapper ioTDeviceLogShardMapper;

  @Resource private DevicePostProcessorManager devicePostProcessorManager;

  /** 日志分表是否开启 */
  @Value("${shard.log.enable:true}")
  private Boolean enable;

  /** 加载第三方平台的生命周期支持情况 */
  private Map<String, Set<String>> lifecycleSupportedConfig = new HashMap<>();

  @Autowired
  void setLifecycleConfig(
      @Value("${universal.iot.third.lifecycle.supported:{\"ctaiot\":[]}}}")
          String lifecyclesupported) {
    if (StrUtil.isBlank(lifecyclesupported)) {
      return;
    }
    JSONObject obj = JSONUtil.parseObj(lifecyclesupported);
    obj.keySet().stream()
        .forEach(
            s -> {
              lifecycleSupportedConfig.put(
                  s,
                  obj.getJSONArray(s).stream().map(c -> c.toString()).collect(Collectors.toSet()));
            });
  }

  /** 校验三分平台是否支持此生命周期 */
  @Override
  public boolean thirdSupport(IoTDeviceDTO ioTDeviceDTO, DevLifeCycle devLifeCycle) {
    if (lifecycleSupportedConfig.containsKey(ioTDeviceDTO.getThirdPlatform())) {
      return lifecycleSupportedConfig
          .get(ioTDeviceDTO.getThirdPlatform())
          .contains(devLifeCycle.name());
    }
    return false;
  }

  @Override
  public void create(String productKey, String deviceId, DownRequest downRequest) {

    log.info("设备创建productKey={},deviceId={}", productKey, deviceId);
    IoTDeviceDTO ioTDeviceDTO = queryDevInstance(null, productKey, deviceId);
    if (ioTDeviceDTO == null) {
      return;
    }
    BaseUPRequest upRequest = build(ioTDeviceDTO);
    upRequest.setEvent(DevLifeCycle.create.name());
    upRequest.setEventName(DevLifeCycle.create.getValue());
    // 暂不推送消息，会错误认为设备已经产生通信
    doUp(Stream.of(upRequest).collect(Collectors.toList()));

    // 执行设备创建后置处理
    devicePostProcessorManager.executePostProcessors(
        IoTDevicePostProcessor.Operation.CREATE, ioTDeviceDTO, downRequest);
  }

  @Override
  public void online(String productKey, String deviceId) {
    String onlineKey = "online:" + productKey + deviceId;
    String offlineKey = "offline:" + productKey + deviceId;

    IoTDeviceDTO dev = queryDevInstance(null, productKey, deviceId);
    if (dev == null) {
      log.warn("设备不存在: deviceId={}", deviceId);
      stringRedisTemplate.delete(onlineKey);
      return;
    }

    // 立即更新数据库状态（条件更新，只更新离线设备）
    int updated = updateDeviceOnlineStatus(productKey, deviceId);

    if (updated > 0) {
      // 清理离线标记
      stringRedisTemplate.delete(offlineKey);

      // 清除缓存
      evictDeviceCache(dev);

      // 触发上线事件
      finalOnline(dev);
    } else {
      log.debug("设备已在线，跳过上线事件: deviceId={}", deviceId);
    }
  }

  private void finalOnline(IoTDeviceDTO dev) {
    BaseUPRequest upRequest = build(dev);
    upRequest.setEvent(DevLifeCycle.online.name());
    upRequest.setEventName(DevLifeCycle.online.getValue());
    // 处理物模型有上下线的事件推送
    doOnOffline(dev);
    // 避免短期时间内无法下线
    log.info(
        "设备上线,deviceId={},设备名称={},所属用户={},上次通信时间={},",
        dev.getDeviceId(),
        dev.getDeviceName(),
        dev.getUserUnionId(),
        dev.getOnlineTime() == null
            ? 0
            : DateUtil.format(DateUtil.date(dev.getOnlineTime() * 1000L), "yyyy-MM-dd HH:mm:ss"));
    doUp(Stream.of(upRequest).collect(Collectors.toList()));

    // 执行设备上线后置处理
    devicePostProcessorManager.executePostProcessors(
        IoTDevicePostProcessor.Operation.ONLINE, dev, null);
    saveLog(upRequest, dev);
  }

  /** 上下线事件 */
  private void doOnOffline(IoTDeviceDTO ioTDeviceDTO) {
    DeviceMetadata deviceMetadata = ioTDeviceDTO.getDeviceMetadata();
    AbstractEventMetadata onOffline = deviceMetadata.getEventOrNull("onOffline");
    if (onOffline != null) {
      BaseUPRequest upRequest = build(ioTDeviceDTO);
      upRequest.setEvent(DevLifeCycle.onOffline.name());
      upRequest.setEventName(DevLifeCycle.onOffline.getValue());
      doUp(Stream.of(upRequest).collect(Collectors.toList()));
    }
  }

  private void finalOffline(IoTDeviceDTO dev) {
    BaseUPRequest upRequest = build(dev);
    upRequest.setEvent(DevLifeCycle.offline.name());
    upRequest.setEventName(DevLifeCycle.offline.getValue());
    saveLog(upRequest, dev);
    doOnOffline(dev);
    log.info(
        "设备离线,deviceId={},设备名称={},所属用户={},上次通信时间={},",
        dev.getDeviceId(),
        dev.getDeviceName(),
        dev.getUserUnionId(),
        dev.getOnlineTime() == null
            ? 0
            : DateUtil.format(DateUtil.date(dev.getOnlineTime() * 1000L), "yyyy-MM-dd HH:mm:ss"));
    doUp(Stream.of(upRequest).collect(Collectors.toList()));

    // 执行设备离线后置处理
    devicePostProcessorManager.executePostProcessors(
        IoTDevicePostProcessor.Operation.OFFLINE, dev, null);
  }

  @Override
  public void offline(String productKey, String deviceId) {
    String onlineKey = "online:" + productKey + deviceId;
    String offlineKey = "offline:" + productKey + deviceId;

    IoTDeviceDTO dev = queryDevInstance(null, productKey, deviceId);
    if (dev == null) {
      return;
    }

    // 设置离线标记（TTL与离线检测间隔一致）
    stringRedisTemplate.opsForValue().set(offlineKey, "1", 40, TimeUnit.MINUTES);

    // 删除在线标记
    stringRedisTemplate.delete(onlineKey);

    // 条件更新数据库
    int count = doDbOffline(productKey, deviceId);

    if (count > 0) {
      // 清除缓存
      evictDeviceCache(dev);
      finalOffline(dev);
    }
  }

  @Override
  public void update(String productKey, String deviceId, DownRequest downRequest) {
    IoTDeviceDTO dev = queryDevInstance(null, productKey, deviceId);
    if (dev == null) {
      return;
    }
    BaseUPRequest upRequest = build(dev);
    upRequest.setEvent(DevLifeCycle.update.name());
    upRequest.setEventName(DevLifeCycle.update.getValue());
    doUp(Stream.of(upRequest).collect(Collectors.toList()));
    log.info(
        "设备修改,deviceId={},设备名称={},所属用户={},上次通信时间={},",
        dev.getDeviceId(),
        dev.getDeviceName(),
        dev.getUserUnionId(),
        dev.getOnlineTime() == null
            ? 0
            : DateUtil.format(DateUtil.date(dev.getOnlineTime() * 1000L), "yyyy-MM-dd HH:mm:ss"));

    // 执行设备更新后置处理
    devicePostProcessorManager.executePostProcessors(
        IoTDevicePostProcessor.Operation.UPDATE, dev, downRequest);
  }

  /** 条件更新设备上线状态 */
  private int updateDeviceOnlineStatus(String productKey, String deviceId) {
    if (StrUtil.isBlank(productKey) || StrUtil.isBlank(deviceId)) {
      return 0;
    }
    Example example = new Example(IoTDevice.class);
    example
        .createCriteria()
        .andEqualTo("productKey", productKey)
        .andEqualTo("deviceId", deviceId)
        .andEqualTo("state", IoTDevice.STATE_OFFLINE);

    IoTDevice ioTDevice = new IoTDevice();
    ioTDevice.setState(IoTDevice.STATE_ONLINE);
    ioTDevice.setOnlineTime(DateUtil.currentSeconds());

    int count = ioTDeviceMapper.updateByConditionSelective(ioTDevice, example);
    log.info("执行设备上线状态变更，deviceId={},影响条数={}", deviceId, count);
    return count;
  }

  /** 清除设备缓存 */
  private void evictDeviceCache(IoTDeviceDTO deviceDTO) {
    try {
      iotDeviceService.evictDeviceActionCache(deviceDTO);
    } catch (Exception e) {
      log.warn(
          "清除设备缓存失败: productKey={}, deviceId={}, error={}",
          deviceDTO == null ? null : deviceDTO.getProductKey(),
          deviceDTO == null ? null : deviceDTO.getDeviceId(),
          e.getMessage());
    }
  }

  /** 执行离线状态变更 */
  private int doDbOffline(String productKey, String deviceId) {
    if (StrUtil.isBlank(productKey) || StrUtil.isBlank(deviceId)) {
      return 0;
    }
    Example example = new Example(IoTDevice.class);
    example.createCriteria().andEqualTo("productKey", productKey).andEqualTo("deviceId", deviceId);
    IoTDevice ioTDevice = new IoTDevice();
    ioTDevice.setState(IoTDevice.STATE_OFFLINE);
    example.and().andEqualTo("state", IoTDevice.STATE_ONLINE);
    int count = ioTDeviceMapper.updateByConditionSelective(ioTDevice, example);
    log.info("执行设备离线状态变更，deviceId={},影响条数={}", deviceId, count);
    return count;
  }

  @Override
  public void enable(String iotId) {
    IoTDeviceDTO dev = queryDevInstance(iotId, null, null);
    if (dev == null) {
      return;
    }
    BaseUPRequest upRequest = build(dev);
    upRequest.setEvent(DevLifeCycle.enable.name());
    upRequest.setEventName(DevLifeCycle.enable.getValue());
    doUp(Stream.of(upRequest).collect(Collectors.toList()));
    log.info(
        "设备启用,deviceId={},设备名称={},所属用户={},上次通信时间={},",
        dev.getDeviceId(),
        dev.getDeviceName(),
        dev.getUserUnionId(),
        dev.getOnlineTime() == null
            ? 0
            : DateUtil.format(DateUtil.date(dev.getOnlineTime() * 1000L), "yyyy-MM-dd HH:mm:ss"));

    // 执行设备启用后置处理
    devicePostProcessorManager.executePostProcessors(
        IoTDevicePostProcessor.Operation.ENABLE, dev, null);
  }

  @Override
  public void disable(String iotId) {
    IoTDeviceDTO dev = queryDevInstance(iotId, null, null);
    if (dev == null) {
      return;
    }
    BaseUPRequest upRequest = build(dev);
    upRequest.setEvent(DevLifeCycle.disable.name());
    upRequest.setEventName(DevLifeCycle.disable.getValue());
    doUp(Stream.of(upRequest).collect(Collectors.toList()));
    log.info(
        "设备禁用,deviceId={},设备名称={},所属用户={},上次通信时间={},",
        dev.getDeviceId(),
        dev.getDeviceName(),
        dev.getUserUnionId(),
        dev.getOnlineTime() == null
            ? 0
            : DateUtil.format(DateUtil.date(dev.getOnlineTime() * 1000L), "yyyy-MM-dd HH:mm:ss"));

    // 执行设备禁用后置处理
    devicePostProcessorManager.executePostProcessors(
        IoTDevicePostProcessor.Operation.DISABLE, dev, null);
  }

  @Override
  public void delete(IoTDeviceDTO ioTDeviceDTO, DownRequest downRequest) {
    if (ioTDeviceDTO == null) {
      return;
    }

    log.info(
        "设备删除,deviceId={},productKey={},unionId={}",
        ioTDeviceDTO.getDeviceId(),
        ioTDeviceDTO.getProductKey(),
        ioTDeviceDTO.getUserUnionId());
    BaseUPRequest upRequest = build(ioTDeviceDTO);
    upRequest.setEvent(DevLifeCycle.delete.name());
    upRequest.setEventName(DevLifeCycle.delete.getValue());
    doUp(Stream.of(upRequest).collect(Collectors.toList()));

    // 执行设备删除后置处理
    devicePostProcessorManager.executePostProcessors(
        IoTDevicePostProcessor.Operation.DELETE, ioTDeviceDTO, downRequest);
  }

  @Override
  public void commandSuccess(IoTDeviceDTO ioTDeviceDTO, String commandId, Object functions) {
    command(ioTDeviceDTO, commandId, functions, true);
  }

  private void command(
      IoTDeviceDTO ioTDeviceDTO, String commandId, Object functions, boolean success) {
    command(ioTDeviceDTO, commandId, functions);
  }

  @Override
  public void command(IoTDeviceDTO ioTDeviceDTO, String commandId, Object functions) {
    // 默认function
    String REPLY_DEFAULT_FUNCTION = "codecReply";
    String REPLY_DEFAULT_FUNCTION_NAME = "编解码自动回复";

    if (ioTDeviceDTO == null) {
      return;
    }
    JSONObject jsonObject = null;
    String functionObj = StrUtil.str(functions, Charset.defaultCharset());
    if (JSONUtil.isTypeJSON(functionObj)) {
      jsonObject = JSONUtil.parseObj(functions);
    } else {
      jsonObject = new JSONObject();
      jsonObject.set("payload", functionObj);
    }

    if (!jsonObject.containsKey("function")) {
      jsonObject.set("function", REPLY_DEFAULT_FUNCTION);
      jsonObject.set("functionName", REPLY_DEFAULT_FUNCTION_NAME);
    }
    log.info(
        "设备功能调用,deviceId={},productKey={},unionId={},commandId={},functions={}",
        ioTDeviceDTO.getDeviceId(),
        ioTDeviceDTO.getProductKey(),
        ioTDeviceDTO.getUserUnionId(),
        commandId,
        functionObj);
    IoTDeviceLog ioTDeviceLog = buildCommandLog(ioTDeviceDTO, commandId);
    ioTDeviceLog.setContent(JSONUtil.toJsonStr(jsonObject));
    DeviceMetadata deviceMetadata =
        iotProductDeviceService.getDeviceMetadata(ioTDeviceDTO.getProductKey());
    AbstractFunctionMetadata functionMetadata =
        deviceMetadata.getFunctionOrNull(jsonObject.getStr("function"));
    if (functionMetadata != null && StrUtil.isNotBlank(functionMetadata.getName())) {
      ioTDeviceLog.setEvent(functionMetadata.getId() + "(" + functionMetadata.getName() + ")");
    } else if (functionMetadata != null) {
      ioTDeviceLog.setEvent(functionMetadata.getId());
    }
    saveLog(ioTDeviceLog, ioTDeviceDTO);
    // 不推送日志
  }

  @Override
  public void commandResp(IoTDeviceDTO ioTDeviceDTO, String commandId, Object resp) {
    if (ioTDeviceDTO == null) {
      return;
    }
    log.info(
        "设备功能下发回复,deviceId={},productKey={},unionId={},commandId={},functions={}",
        ioTDeviceDTO.getDeviceId(),
        ioTDeviceDTO.getProductKey(),
        ioTDeviceDTO.getUserUnionId(),
        commandId,
        JSONUtil.toJsonStr(resp));

    // 获取产品信息
    IoTProduct product = iotProductDeviceService.getProduct(ioTDeviceDTO.getProductKey());

    // 只查询2天前内的指令
    long twoDaysAgo = DateUtil.currentSeconds() - (3 * 24 * 60 * 60);

    // 使用统一的查询接口查询指令记录
    IoTDeviceLog devShardLog =
        iIoTDeviceDataService.getRecentCommand(
            product,
            ioTDeviceDTO.getProductKey(),
            ioTDeviceDTO.getDeviceId(),
            commandId,
            MessageType.FUNCTIONS.name(),
            twoDaysAgo,
            0);

    if (devShardLog != null) {
      IoTDeviceLog newlog =
          IoTDeviceLog.builder()
              .commandStatus(1)
              .iotId(devShardLog.getIotId())
              .id(devShardLog.getId())
              .build();
      ioTDeviceLogShardMapper.updateRecentCommandSuccess(newlog, devShardLog.getIotId());
      /** 推送消息 */
      BaseUPRequest upRequest = build(ioTDeviceDTO);
      upRequest.setMessageType(MessageType.REPLY);
      upRequest.setFunction(devShardLog.getEvent());
      upRequest.setCommandId(commandId);
      upRequest.setCommandStatus(1);
      upRequest.setReplyPayload(JSONUtil.toJsonStr(resp));
      // 保存回复日志
      saveLog(upRequest, ioTDeviceDTO);
      doUp(Stream.of(upRequest).collect(Collectors.toList()));
      devicePostProcessorManager.executePostProcessors(
          IoTDevicePostProcessor.Operation.COMMAND_RESP,
          ioTDeviceDTO,
          buildCommandRespDownRequest(ioTDeviceDTO, commandId, devShardLog.getEvent(), resp));
    }
  }

  private DownRequest buildCommandRespDownRequest(
      IoTDeviceDTO ioTDeviceDTO, String commandId, String functionLabel, Object resp) {
    Map<String, Object> function = new HashMap<>();
    function.put("commandId", commandId);
    function.put("payload", resp);
    if (StrUtil.isNotBlank(functionLabel)) {
      function.put("functionName", functionLabel);
    }
    return DownRequest.builder()
        .productKey(ioTDeviceDTO.getProductKey())
        .deviceId(ioTDeviceDTO.getDeviceId())
        .deviceName(ioTDeviceDTO.getDeviceName())
        .appUnionId(ioTDeviceDTO.getUserUnionId())
        .msgId(commandId)
        .detail(functionLabel)
        .function(function)
        .build();
  }

  private IoTDeviceLog buildCommandLog(IoTDeviceDTO instanceBO, String commandId) {
    IoTDeviceLog ioTDeviceLog =
        IoTDeviceLog.builder()
            .deviceId(instanceBO.getDeviceId())
            .deviceName(instanceBO.getDeviceName())
            .productKey(instanceBO.getProductKey())
            .messageType(MessageType.FUNCTIONS.name())
            .iotId(instanceBO.getIotId())
            .createTime(LocalDateTime.now())
            .commandStatus(0)
            // 使用此字段作为校验标识
            .commandId(commandId)
            .build();
    return ioTDeviceLog;
  }

  private BaseUPRequest build(IoTDeviceDTO instanceBO) {
    BaseUPRequest request = new BaseUPRequest();
    // 基础信息
    request.setIotId(instanceBO.getIotId());
    request.setDeviceName(instanceBO.getDeviceName());
    request.setDeviceId(instanceBO.getDeviceId());
    request.setTime(System.currentTimeMillis());
    request.setProductKey(instanceBO.getProductKey());
    request.setUserUnionId(instanceBO.getUserUnionId());
    request.setMessageType(MessageType.EVENT);
    request.setDeviceNode(instanceBO.getDeviceNode());
    request.setIoTDeviceDTO(instanceBO);
    return request;
  }

  private IoTDeviceDTO queryDevInstance(String iotId, String productKey, String deviceId) {
    Map<String, Object> param = new HashMap<>();
    param.put("iotId", iotId);
    param.put("deviceId", deviceId);
    param.put("productKey", productKey);
    IoTDeviceDTO ioTDeviceDTO = iotDeviceService.selectDevInstanceBONoCache(param);
    return ioTDeviceDTO;
  }

  public void saveLog(BaseUPRequest upRequest, IoTDeviceDTO ioTDeviceDTO) {
    IoTProduct product = iotProductDeviceService.getProduct(upRequest.getProductKey());
    iIoTDeviceDataService.saveDeviceLog(upRequest, ioTDeviceDTO, product);
  }

  public void saveLog(IoTDeviceLog ioTDeviceLog, IoTDeviceDTO ioTDeviceDTO) {
    IoTProduct product = iotProductDeviceService.getProduct(ioTDeviceDTO.getProductKey());
    iIoTDeviceDataService.saveDeviceLog(ioTDeviceLog, ioTDeviceDTO, product);
  }
}
