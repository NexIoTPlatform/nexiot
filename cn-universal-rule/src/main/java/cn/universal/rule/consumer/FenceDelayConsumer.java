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

package cn.universal.rule.consumer;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.dm.device.constant.DeviceManagerConstant;
import cn.universal.dm.device.service.IoTUPPushAdapter;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.dm.device.service.log.IIoTDeviceDataService;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDeviceGeoFence;
import cn.universal.persistence.entity.IoTDeviceLog;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTDeviceGeoFenceMapper;
import cn.universal.persistence.mapper.IoTDeviceLogShardMapper;
import cn.universal.rule.fence.enums.FenceTouchWay;
import cn.universal.rule.fence.enums.FenceType;
import cn.universal.rule.fence.utils.RegionUtil;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 基础功能 */
@Slf4j
@Component
public class FenceDelayConsumer extends IoTUPPushAdapter {

  @Resource private IoTDeviceGeoFenceMapper ioTDeviceGeoFenceMapper;
  @Resource private IoTDeviceLogShardMapper ioTDeviceLogShardMapper;
  @Resource private IIoTDeviceDataService iIoTDeviceDataService;

  @Resource private IoTProductDeviceService iotProductDeviceService;

  public void consumer(String message) {
    JSONObject jsonObject = JSONUtil.parseObj(message);
    String fenceId = jsonObject.getStr("fenceId");
    String[] lastCoordinate = jsonObject.getStr("coordinate").split(",");
    IoTDeviceDTO instanceBO =
        BeanUtil.toBean(JSONUtil.parseObj(jsonObject.getStr("instanceBO")), IoTDeviceDTO.class);
    IoTDeviceGeoFence ioTDeviceGeoFence = ioTDeviceGeoFenceMapper.selectByPrimaryKey(fenceId);
    IoTDeviceLog ioTDeviceLog =
        ioTDeviceLogShardMapper.queryLatestCoordinatesLogByIotId(instanceBO.getIotId());
    double lastLng = Double.parseDouble(lastCoordinate[0]);
    double lastLat = Double.parseDouble(lastCoordinate[1]);
    // 最近一次坐标日志
    cn.hutool.json.JSONObject properties =
        JSONUtil.parseObj(ioTDeviceLog.getContent()).getJSONObject("properties");
    double lng;
    double lat;
    String[] coordinate = properties.getStr(DeviceManagerConstant.COORDINATES).split(",");
    lng = Double.parseDouble(coordinate[0]);
    lat = Double.parseDouble(coordinate[1]);

    boolean thisIn = false;
    boolean lastIn = false;

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
                    properties.get(DeviceManagerConstant.COORDINATES));
              }
            };
        request.setData(data);
        IoTProduct product = iotProductDeviceService.getProduct(request.getProductKey());
        iIoTDeviceDataService.saveDeviceLog(request, request.getIoTDeviceDTO(), product);
        doUp(Stream.of(request).collect(Collectors.toList()));
      }
    }
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
}
