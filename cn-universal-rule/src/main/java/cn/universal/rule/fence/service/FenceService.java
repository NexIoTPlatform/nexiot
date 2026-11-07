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

package cn.universal.rule.fence.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.constant.IoTConstant.ERROR_CODE;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.common.domain.R;
import cn.universal.common.event.EventTopics;
import cn.universal.common.event.processer.EventPublisher;
import cn.universal.common.utils.DelayedTaskUtil;
import cn.universal.core.message.DownRequest;
import cn.universal.dm.device.constant.DeviceManagerConstant;
import cn.universal.dm.device.service.IoTUPPushAdapter;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.dm.device.service.log.IIoTDeviceDataService;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTDeviceFenceRel;
import cn.universal.persistence.entity.IoTDeviceGeoFence;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTDeviceFenceRelMapper;
import cn.universal.persistence.mapper.IoTDeviceGeoFenceMapper;
import cn.universal.persistence.mapper.IoTDeviceLogMapper;
import cn.universal.persistence.mapper.IoTDeviceLogShardMapper;
import cn.universal.rule.fence.enums.FenceTouchWay;
import cn.universal.rule.fence.enums.FenceType;
import cn.universal.rule.fence.utils.RegionUtil;
import jakarta.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * 设备围栏 @Author ruoyi
 *
 * @since 2023/8/5 8:57
 */
@Slf4j
@Service
public class FenceService extends IoTUPPushAdapter {

  @Resource private DelayedTaskUtil delayedTaskUtil;

  @Resource private IoTDeviceGeoFenceMapper ioTDeviceGeoFenceMapper;

  @Resource private IoTDeviceFenceRelMapper ioTDeviceFenceRelMapper;

  @Resource private IoTDeviceLogMapper ioTDeviceLogMapper;
  @Resource private IoTDeviceLogShardMapper ioTDeviceLogShardMapper;

  @Resource private IIoTDeviceDataService iIoTDeviceDataService;

  @Autowired private IoTProductDeviceService iotProductDeviceService;
  @Resource private StringRedisTemplate stringRedisTemplate;
  @Resource private EventPublisher eventPublisher;

  /** 日志分表是否开启 */
  @Value("${shard.log.enable:true}")
  private Boolean enable;

  /** 支持上报坐标设备的电子围栏功能 */
  public R callFenceFunction(IoTProduct product, IoTDevice ioTDevice, DownRequest downRequest) {
    R r = null;

    String function = (String) downRequest.getFunction().get("function");

    switch (function) {
      case "setFence":
        r = setFence(ioTDevice, downRequest);
        break;
      case "getFence":
        r = getFence(ioTDevice, downRequest);
        break;
      case "delFence":
        r = delFence(ioTDevice, downRequest);
        break;
      default:
    }
    return r;
  }

  private R delFence(IoTDevice ioTDevice, DownRequest downRequest) {
    Map data = (Map) downRequest.getFunction().get("data");
    Long id = Long.parseLong(data.get("id").toString());
    IoTDeviceFenceRel instanceFence = new IoTDeviceFenceRel();
    instanceFence.setFenceId(id);
    instanceFence.setIotId(ioTDevice.getIotId());
    instanceFence.setCreatorId(ioTDevice.getCreatorId());
    IoTDeviceFenceRel ins = ioTDeviceFenceRelMapper.selectOne(instanceFence);
    if (Objects.isNull(ins)) {
      // 数据不存在
      return R.error(ERROR_CODE.DATA_NOT_FIND.getCode(), ERROR_CODE.DATA_NOT_FIND.getName());
    }
    ioTDeviceFenceRelMapper.deleteByPrimaryKey(ins.getId());

    ioTDeviceGeoFenceMapper.deleteByPrimaryKey(ins.getFenceId());
    return R.ok();
  }

  /** 设置围栏 */
  public R setFence(IoTDevice ioTDevice, DownRequest downRequest) {
    Map data = (Map) downRequest.getFunction().get("data");

    IoTDeviceGeoFence ioTDeviceGeoFence = BeanUtil.toBean(data, IoTDeviceGeoFence.class);

    if (Objects.isNull(ioTDeviceGeoFence.getId())) {
      ioTDeviceGeoFence.setCreatorId(ioTDevice.getCreatorId());
      ioTDeviceGeoFence.setCreateDate(new Date());
      ioTDeviceGeoFence.setUpdateDate(new Date());
      ioTDeviceGeoFenceMapper.insertSelective(ioTDeviceGeoFence);
      IoTDeviceFenceRel instanceFence = new IoTDeviceFenceRel();
      instanceFence.setFenceId(ioTDeviceGeoFence.getId());
      instanceFence.setIotId(ioTDevice.getIotId());
      instanceFence.setDeviceId(downRequest.getDeviceId());
      instanceFence.setCreateDate(new Date());
      instanceFence.setCreatorId(ioTDevice.getCreatorId());
      ioTDeviceFenceRelMapper.insertSelective(instanceFence);
    } else {
      ioTDeviceGeoFence.setCreatorId(ioTDevice.getCreatorId());
      ioTDeviceGeoFence.setUpdateDate(new Date());
      ioTDeviceGeoFenceMapper.updateByPrimaryKeySelective(ioTDeviceGeoFence);
    }
    return R.ok();
  }

  /** 获取电子围栏 */
  public R getFence(IoTDevice ioTDevice, DownRequest downRequest) {

    List<IoTDeviceGeoFence> res =
        ioTDeviceGeoFenceMapper.selectByIotId(ioTDevice.getIotId(), ioTDevice.getCreatorId());
    Map<String, Object> map = new HashMap<>();
    map.put("fences", res);
    return R.ok(map);
  }

  public static void main(String[] args) {
    Date date = new Date();
    SimpleDateFormat format = new SimpleDateFormat("EEEE");
    String week = format.format(date);
    int w = "星期日,星期一,星期二,星期三,星期四,星期五,星期六".indexOf(week);
    if (w == -1) {
      log.info("定位设备不在判断星期内");
      System.out.println(111);
    } else {
      System.out.println(2222);
    }
  }

  private BaseUPRequest doTestEvent(
      BaseUPRequest downRequest,
      IoTDeviceLog ioTDeviceLog,
      IoTDeviceGeoFence ioTDeviceGeoFence,
      IoTDeviceDTO instanceBO) {
    if (Objects.isNull(ioTDeviceLog)) {
      return null;
    }
    if (!downRequest.getProperties().containsKey(DeviceManagerConstant.COORDINATES)) {
      return null;
    }
    if (StrUtil.isNotEmpty(ioTDeviceGeoFence.getWeekTime())) {
      // 判断星期几
      Date date = new Date();
      SimpleDateFormat format = new SimpleDateFormat("EEEE", Locale.ENGLISH);
      String week = format.format(date);
      int w = ioTDeviceGeoFence.getWeekTime().indexOf(week);
      if (w == -1) {
        log.info("定位设备不在判断星期内:{}", week);
        return null;
      }
      // 判断时间在不在范围内
      SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
      String d = format1.format(date);
      String beginTime = ioTDeviceGeoFence.getBeginTime();
      String endTime = ioTDeviceGeoFence.getEndTime();
      String beginTimeLast = d + " " + beginTime;
      String endTimeLast = d + " " + endTime;
      LocalDateTime now = LocalDateTime.now();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      LocalDateTime t1 = LocalDateTime.parse(beginTimeLast, formatter);
      LocalDateTime t2 = LocalDateTime.parse(endTimeLast, formatter);
      if (!(now.isAfter(t1) && now.isBefore(t2))) {
        log.info(
            "定位设备不在判断时间内,fenceId={},deviceId={}",
            ioTDeviceGeoFence.getId(),
            instanceBO.getDeviceId());
        return null;
      }
    }
    // 上次日志
    JSONObject properties =
        JSONUtil.parseObj(ioTDeviceLog.getContent()).getJSONObject("properties");
    double lastLng;
    double lastLat;
    if (properties.containsKey(DeviceManagerConstant.COORDINATES)) {
      String[] coordinate = properties.getStr(DeviceManagerConstant.COORDINATES).split(",");
      lastLng = Double.parseDouble(coordinate[0]);
      lastLat = Double.parseDouble(coordinate[1]);
    } else {
      return null;
    }

    // 不触发时间判断
    if (StrUtil.isNotEmpty(ioTDeviceGeoFence.getNoTriggerTime())) {
      JSONArray noTriggerTime = JSONUtil.parseArray(ioTDeviceGeoFence.getNoTriggerTime());
      DateTimeFormatter sdf = DateTimeFormatter.ofPattern("HH:mm:ss");
      LocalTime now = LocalTime.now();
      int size = noTriggerTime.size();
      for (int i = 0; i < size; i++) {
        String[] time = noTriggerTime.get(i).toString().split("-");
        LocalTime start = LocalTime.parse(time[0]);
        LocalTime end = LocalTime.parse(time[1]);
        if (now.isAfter(start) && now.isBefore(end)) {
          log.info(
              "定位设备在不触发时间内,fenceId={},deviceId={}",
              ioTDeviceGeoFence.getId(),
              instanceBO.getDeviceId());
          String key =
              IoTConstant.FENCE_TRIGGER_SIGN
                  + ":"
                  + downRequest.getDeviceId()
                  + ":"
                  + ioTDeviceGeoFence.getId();
          if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(key))) {
            long seconds = Duration.between(now, end).getSeconds();
            stringRedisTemplate.opsForValue().set(key, "1", seconds, TimeUnit.SECONDS);
            JSONObject object = new JSONObject();
            object.set("coordinate", lastLng + "," + lastLat);
            object.set("fenceId", ioTDeviceGeoFence.getId());
            object.set("instanceBO", JSONUtil.toJsonStr(instanceBO));
            eventPublisher.publishEvent(EventTopics.FENCE_EVENT, object, seconds);
          }
          return null;
        }
      }
    }

    boolean thisIn = false;
    boolean lastIn = false;

    String[] coordinate =
        downRequest.getProperties().get(DeviceManagerConstant.COORDINATES).toString().split(",");
    double lng = Double.parseDouble(coordinate[0]);
    double lat = Double.parseDouble(coordinate[1]);

    // 判断是否在范围内
    if (FenceType.circle.name().equals(ioTDeviceGeoFence.getType())) {
      String[] point = ioTDeviceGeoFence.getPoint().split(",");
      thisIn =
          RegionUtil.isInCircle(
              lng,
              lat,
              Double.parseDouble(point[0]),
              Double.parseDouble(point[1]),
              ioTDeviceGeoFence.getRadius().doubleValue());

      lastIn =
          RegionUtil.isInCircle(
              lastLng,
              lastLat,
              Double.parseDouble(point[0]),
              Double.parseDouble(point[1]),
              ioTDeviceGeoFence.getRadius().doubleValue());
    } else {
      JSONArray fences = JSONUtil.parseArray(ioTDeviceGeoFence.getFence());
      double[] fLng = new double[fences.size()];
      double[] fLat = new double[fences.size()];
      for (int i = 0; i < fences.size(); i++) {
        String[] arr = fences.get(i).toString().split(",");
        fLng[i] = Double.parseDouble(arr[0]);
        fLat[i] = Double.parseDouble(arr[1]);
      }
      thisIn = RegionUtil.isInPolygon(lng, lat, fLng, fLat);
      lastIn = RegionUtil.isInPolygon(lastLng, lastLat, fLng, fLat);
    }

    if (ioTDeviceGeoFence.getTouchWay().equals(FenceTouchWay.in.name())
        || ioTDeviceGeoFence.getTouchWay().equals(FenceTouchWay.all.name())) {

      if (thisIn && !lastIn) {
        // 进入事件
        BaseUPRequest request = build(instanceBO);
        request.setEvent("entryFence");
        request.setEventName("进入电子围栏");
        Map<String, Object> data =
            new HashMap<String, Object>() {
              {
                put("fenceId", ioTDeviceGeoFence.getId());
                put("fenceName", ioTDeviceGeoFence.getName());
                put(
                    DeviceManagerConstant.COORDINATES,
                    downRequest.getProperties().get(DeviceManagerConstant.COORDINATES));
              }
            };
        request.setData(data);
        // 延迟触发判断
        request = doDelayTest(request, ioTDeviceGeoFence);
        return request;
      }
    }

    if (ioTDeviceGeoFence.getTouchWay().equals(FenceTouchWay.out.name())
        || ioTDeviceGeoFence.getTouchWay().equals(FenceTouchWay.all.name())) {
      if (!thisIn && lastIn) {
        // 离开事件
        BaseUPRequest request = build(instanceBO);
        request.setEvent("leaveFence");
        request.setEventName("离开电子围栏");
        Map<String, Object> data =
            new HashMap<String, Object>() {
              {
                put("fenceId", ioTDeviceGeoFence.getId());
                put("fenceName", ioTDeviceGeoFence.getName());
                put(
                    DeviceManagerConstant.COORDINATES,
                    downRequest.getProperties().get(DeviceManagerConstant.COORDINATES));
              }
            };
        request.setData(data);
        // 延迟触发判断
        request = doDelayTest(request, ioTDeviceGeoFence);
        return request;
      }
    }

    return null;
  }

  private BaseUPRequest doDelayTest(BaseUPRequest request, IoTDeviceGeoFence ioTDeviceGeoFence) {
    if (ioTDeviceGeoFence.getDelayTime() == null || ioTDeviceGeoFence.getDelayTime() <= 0) {
      return request;
    }
    String key =
        IoTConstant.FENCE_DELAY_SIGN
            + ":"
            + request.getDeviceId()
            + ":"
            + ioTDeviceGeoFence.getId();
    String lastEvent = stringRedisTemplate.opsForValue().get(key);
    if (StrUtil.isNotBlank(lastEvent)) {
      //      BaseUPRequest lastRequest = BeanUtil.toBean(JSONUtil.parseObj(value),
      // BaseUPRequest.class);
      if (lastEvent.equals(request.getEvent())) {
        return null;
      }
      stringRedisTemplate.delete(key);
      return null;
    }
    stringRedisTemplate
        .opsForValue()
        .set(key, request.getEvent(), ioTDeviceGeoFence.getDelayTime() + 1, TimeUnit.MINUTES);
    delayedTaskUtil.putTask(
        () -> {
          String lastEvent2 = stringRedisTemplate.opsForValue().get(key);
          if (StrUtil.isNotBlank(lastEvent2)) {
            //        BaseUPRequest lastRequest = BeanUtil.toBean(JSONUtil.parseObj(value2),
            // BaseUPRequest.class);
            IoTProduct product = iotProductDeviceService.getProduct(request.getProductKey());
            iIoTDeviceDataService.saveDeviceLog(request, request.getIoTDeviceDTO(), product);
            doUp(Stream.of(request).collect(Collectors.toList()));
            stringRedisTemplate.delete(key);
          }
        },
        ioTDeviceGeoFence.getDelayTime(),
        TimeUnit.MINUTES);
    return null;
  }

  private BaseUPRequest build(IoTDeviceDTO instanceBO) {
    BaseUPRequest request = new BaseUPRequest();
    // 基础信息
    request.setIoTDeviceDTO(instanceBO);
    request.setIotId(instanceBO.getIotId());
    request.setDeviceName(instanceBO.getDeviceName());
    request.setDeviceId(instanceBO.getDeviceId());
    request.setTime(System.currentTimeMillis());
    request.setProductKey(instanceBO.getProductKey());
    request.setUserUnionId(instanceBO.getUserUnionId());
    request.setMessageType(MessageType.EVENT);
    request.setDeviceNode(instanceBO.getDeviceNode());
    return request;
  }

  /**
   * 设备围栏监测
   *
   * @param upRequest 上行参数
   * @param instanceBO 设备参数
   */
  @Async("sceneTaskExecutor")
  public void testFence(BaseUPRequest upRequest, IoTDeviceDTO instanceBO) {
    List<IoTDeviceGeoFence> ioTDeviceGeoFences =
        ioTDeviceGeoFenceMapper.selectByIotId(instanceBO.getIotId(), upRequest.getUserUnionId());
    if (CollectionUtils.isEmpty(ioTDeviceGeoFences)) {
      return;
    }
    ioTDeviceGeoFences =
        ioTDeviceGeoFences.stream().filter(f -> f.getStatus() == 0).collect(Collectors.toList());
    if (CollectionUtils.isEmpty(ioTDeviceGeoFences)) {
      return;
    }
    IoTDeviceLog ioTDeviceLog;
    // 开启分表时查新表
    if (enable) {
      ioTDeviceLog = ioTDeviceLogShardMapper.queryCoordinatesLogByIotId(instanceBO.getIotId());
    } else {
      ioTDeviceLog = ioTDeviceLogMapper.queryCoordinatesLogByIotId(instanceBO.getIotId());
    }

    ioTDeviceGeoFences.forEach(
        f -> {
          BaseUPRequest request = doTestEvent(upRequest, ioTDeviceLog, f, instanceBO);
          if (Objects.nonNull(request)) {
            IoTProduct product = iotProductDeviceService.getProduct(request.getProductKey());
            iIoTDeviceDataService.saveDeviceLog(request, request.getIoTDeviceDTO(), product);
            doUp(Stream.of(request).collect(Collectors.toList()));
          }
        });
  }
}
