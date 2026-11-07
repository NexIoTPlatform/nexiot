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

package cn.universal.dm.device.service.wrapper;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.dm.device.constant.DeviceManagerConstant;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.impl.IoTDeviceShadowService;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.admin.SysDictData;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.SupportMapAreasMapper;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

/**
 * 设备上行消息单独处理
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/9/21
 */
@Service("ioTDeviceUPIntercept")
@Slf4j
public class IoTDeviceUPIntercept {

  @Resource private IoTDeviceMapper ioTDeviceMapper;
  @Resource private SupportMapAreasMapper supportMapAreasMapper;
  @Resource private IoTDeviceShadowService ioTDeviceShadowService;
  @Resource private IoTDeviceService ioTDeviceService;

  @Async("deviceAttributeTaskExecutor")
  public void messageProcess(BaseUPRequest upRequest) {
    if (upRequest == null || upRequest.getMessageType() == null) {
      return;
    }
    MessageType messageType = upRequest.getMessageType();
    switch (messageType) {
      case PROPERTIES:
        properties(upRequest);
        break;
      case EVENT:
        event(upRequest);
        break;
      default:
        break;
    }
  }

  private void event(BaseUPRequest baseUPRequest) {
    try {
    } catch (Exception e) {
      log.error("device UP messageProcess [event] error={}", ExceptionUtil.getRootCauseMessage(e));
    }
  }

  private void properties(BaseUPRequest baseUPRequest) {
    Map<String, Object> properties = baseUPRequest.getProperties();
    if (MapUtil.isEmpty(properties)) {
      return;
    }
    try {
      propertiesGeoPoint(baseUPRequest, properties);
      propertiesCommonAttributes(baseUPRequest, properties);
    } catch (Exception e) {
      log.error(
          "device UP messageProcess [properties] error={}", ExceptionUtil.getRootCauseMessage(e));
    }
  }

  /** 处理定位设备经纬度 */
  private void propertiesGeoPoint(BaseUPRequest baseUPRequest, Map<String, Object> properties) {
    JSONObject cfg = baseUPRequest.getIoTDeviceDTO().getProductConfig();
    if (cfg != null
        && cfg.containsKey(IoTConstant.IS_GPS_PRODUCT)
        && cfg.getBool(IoTConstant.IS_GPS_PRODUCT)
        && properties.containsKey(DeviceManagerConstant.COORDINATES)) {
      String geoPoint = (String) properties.get(DeviceManagerConstant.COORDINATES);
      log.info("处理定位类经纬度数据处理，deviceId={},coordinate={}", baseUPRequest.getDeviceId(), geoPoint);
      if (StrUtil.isNotBlank(geoPoint)) {
        IoTDevice ioTDevice = new IoTDevice();
        ioTDevice.setIotId(baseUPRequest.getIotId());
        ioTDevice.setCoordinate(geoPoint);
        //        String[] coors = geoPoint.split(",");
        //        SupportMapAreas supportMapAreas = supportMapAreasMapper.selectMapAreas(coors[0],
        // coors[1]);
        //        if (supportMapAreas == null) {
        //          log.info("查询区域id为空,longitude={},latitude={}", coors[0], coors[1]);
        //        } else {
        //          ioTDevice.setAreasId(supportMapAreas.getId());
        //        }
        Example example = new Example(IoTDevice.class);
        example.createCriteria().andEqualTo("iotId", baseUPRequest.getIotId());
        ioTDeviceMapper.updateByExampleSelective(ioTDevice, example);
      }
    }
  }

  /** 处理公共属性更新（基于字典配置） 优化策略： 1. 先从缓存获取设备信息，避免数据库查询 2. 只更新值变化的属性，减少数据库写入 3. 异步处理，不阻塞主流程 4. 支持最终一致性 */
  private void propertiesCommonAttributes(
      BaseUPRequest baseUPRequest, Map<String, Object> properties) {
    // 1. 获取公共属性字典配置（带缓存）
    List<SysDictData> commonPropertyDict = ioTDeviceShadowService.queryCommonPropertyDict();
    if (commonPropertyDict == null || commonPropertyDict.isEmpty()) {
      return;
    }

    if (commonPropertyDict.stream().noneMatch(dict -> properties.containsKey(dict.getDictValue())))
      return;

    // 2. 从缓存获取设备信息（性能优化）
    IoTDeviceDTO cachedDevice =
        ioTDeviceService.selectDevInstanceBO(
            baseUPRequest.getProductKey(), baseUPRequest.getDeviceId());
    if (cachedDevice == null) {
      log.warn("[公共属性更新] 设备缓存不存在: iotId={}", baseUPRequest.getIotId());
      return;
    }

    // 3. 解析现有配置
    JSONObject existingConfig = new JSONObject();
    if (StrUtil.isNotBlank(cachedDevice.getConfiguration())) {
      existingConfig = JSONUtil.parseObj(cachedDevice.getConfiguration());
    }

    // 4. 检查哪些属性需要更新
    boolean needUpdate = false;
    JSONObject updatedConfig = JSONUtil.parseObj(existingConfig.toString());

    for (SysDictData dictData : commonPropertyDict) {
      String propertyKey = dictData.getDictValue();

      // 检查上报属性中是否包含此公共属性
      if (properties.containsKey(propertyKey)) {
        Object newValue = properties.get(propertyKey);
        Object existingValue = existingConfig.get(propertyKey);

        // 值不同才更新（避免无意义写入）
        if (newValue != null && !newValue.equals(existingValue)) {
          updatedConfig.set(propertyKey, newValue);
          needUpdate = true;
          log.debug(
              "[公共属性更新] 检测到变化: iotId={}, key={}, old={}, new={}",
              baseUPRequest.getIotId(),
              propertyKey,
              existingValue,
              newValue);
        }
      }
    }

    // 5. 仅在有变化时才更新数据库
    if (needUpdate) {
      try {
        Example example = new Example(IoTDevice.class);
        example.createCriteria().andEqualTo("iotId", baseUPRequest.getIotId());

        IoTDevice updateDevice = new IoTDevice();
        updateDevice.setConfiguration(updatedConfig.toString());

        ioTDeviceMapper.updateByExampleSelective(updateDevice, example);

        log.info(
            "[公共属性更新] 更新成功: iotId={}, updatedKeys={}",
            baseUPRequest.getIotId(),
            updatedConfig.keySet());
      } catch (Exception e) {
        log.error(
            "[公共属性更新] 更新失败: iotId={}, error={}",
            baseUPRequest.getIotId(),
            ExceptionUtil.getRootCauseMessage(e));
      }
    }
  }
}
