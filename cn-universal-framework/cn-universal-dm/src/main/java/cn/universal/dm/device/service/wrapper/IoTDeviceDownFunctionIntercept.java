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

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.domain.R;
import cn.universal.core.message.DownRequest;
import cn.universal.core.metadata.AbstractPropertyMetadata;
import cn.universal.core.metadata.DeviceMetadata;
import cn.universal.core.metadata.PropertyMode;
import cn.universal.core.metadata.PropertySource;
import cn.universal.dm.device.service.impl.IoTDeviceShadowService;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.persistence.base.IoTDownWrapper;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

@Service("ioTDeviceDownFunctionIntercept")
@Slf4j
public class IoTDeviceDownFunctionIntercept implements IoTDownWrapper {

  @Resource private IoTDeviceShadowService ioTDeviceShadowService;

  @Resource private IoTProductDeviceService ioTProductDeviceService;

  @Resource private IoTDeviceMapper ioTDeviceMapper;

  // 保存功能期望值
  @Override
  public R beforeFunctionOrConfigDown(
      IoTProduct product, IoTDevice ioTDevice, DownRequest downRequest) {
    if (downRequest.getFunction() == null) {
      return null;
    }
    String function = downRequest.getFunction().get("function").toString();
    JSONObject data = JSONUtil.parseObj(downRequest.getFunction().get("data"));
    // 当方法是set开头且data里不为空 保存data内的数据到影子期望值
    if (IoTConstant.SET_DEVICE_DESIRED_PROPERTIES.equalsIgnoreCase(function)
        && ObjectUtil.isNotEmpty(data)) {
      // 这里可能是网关代理子设备，那么物模型的填充必须用真实的产品,也就是子设备
      String productKey = product.getProductKey();
      if (Boolean.TRUE.equals(downRequest.getIsGatewayProxy())
          && StrUtil.isNotBlank(downRequest.getSubProductKey())
          && StrUtil.isNotBlank(downRequest.getSubDeviceId())) {
        productKey = downRequest.getSubProductKey();
        IoTDevice query =
            IoTDevice.builder()
                .productKey(downRequest.getSubProductKey())
                .deviceId(downRequest.getSubDeviceId())
                .build();
        ioTDevice = ioTDeviceMapper.selectOne(query);
      }
      // 过滤掉只读属性，只保留可读写（rw）的属性
      JSONObject filteredData = filterReadWriteProperties(productKey, data);
      boolean shadowUpdated = true;
      if (ObjectUtil.isNotEmpty(filteredData)) {
        shadowUpdated = doShadow(ioTDevice, filteredData);
        if (shadowUpdated) {
          doDeviceDesiredToDB(ioTDevice, filteredData);
        }
      }
      if (IoTConstant.PAYLOAD_SOURCE_SHADOW_ONLY.equalsIgnoreCase(downRequest.getPayloadSource())) {
        if (!shadowUpdated) {
          return R.error("期望值保存失败，请稍后重试");
        }
        log.info(
            "期望值功能按 shadow-only 处理，停止继续下发: productKey={}, deviceId={}",
            ioTDevice.getProductKey(),
            ioTDevice.getDeviceId());
        return R.ok("期望值设置成功");
      }
    }

    return null;
  }

  /**
   * 把期望值的字段统一放入iot_device扩展字段中，编译搜索
   *
   * @param ioTDevice
   * @param filteredData
   */
  private void doDeviceDesiredToDB(IoTDevice ioTDevice, JSONObject filteredData) {
    // 1. 入参合法性校验（快速失败）
    if (ioTDevice == null) {
      log.warn("设备期望属性更新DB失败：入参IoTDevice为空");
      return;
    }
    String deviceId = ioTDevice.getDeviceId();
    String productKey = ioTDevice.getProductKey();
    if (StrUtil.isBlank(deviceId) || StrUtil.isBlank(productKey)) {
      log.warn("设备期望属性更新DB失败：设备标识为空，productKey={}, deviceId={}", productKey, deviceId);
      return;
    }
    if (MapUtil.isEmpty(filteredData)) {
      log.info("设备期望属性更新DB：无需要更新的属性数据，productKey={}, deviceId={}", productKey, deviceId);
      return;
    }

    JSONObject deviceTagData = filterExtendedPropertiesForDeviceTag(productKey, filteredData);
    if (MapUtil.isEmpty(deviceTagData)) {
      log.info(
          "设备期望属性更新DB：过滤 source=device 后无需要更新的属性，productKey={}, deviceId={}",
          productKey,
          deviceId);
      return;
    }

    // 2. 查询数据库中已有设备信息
    IoTDevice dbDevice = ioTDeviceMapper.selectIoTDevice(productKey, deviceId);
    if (dbDevice == null) {
      log.warn("设备期望属性更新DB失败：数据库未查询到设备，productKey={}, deviceId={}", productKey, deviceId);
      return;
    }

    try {
      // 3. 解析数据库中已有的设备标签（旧值）
      String oldDesiredStr = dbDevice.getDeviceTag();
      JSONObject oldKv = new JSONObject(); // 初始化空对象，避免空指针
      if (StrUtil.isNotBlank(oldDesiredStr)) {
        // 校验JSON格式合法性，避免非法格式导致解析异常
        if (JSONUtil.isJsonObj(oldDesiredStr)) {
          oldKv = JSONUtil.parseObj(oldDesiredStr);
        } else {
          log.warn(
              "设备期望属性更新DB：数据库中deviceTag格式非法，将重置为空，productKey={}, deviceId={}",
              productKey,
              deviceId);
          oldKv = new JSONObject();
        }
      }

      // 4. 合并新旧值：旧值兜底，新值覆盖（核心逻辑修正）
      JSONObject finalKv = new JSONObject();
      finalKv.putAll(oldKv); // 先放旧值（保留历史未更新的键）
      finalKv.putAll(deviceTagData); // 再放新值（覆盖同名键，实现新值优先）

      // 5. 构造更新参数并执行数据库更新
      IoTDevice updateDevice = new IoTDevice();
      updateDevice.setDeviceTag(JSONUtil.toJsonStr(finalKv)); // 转换为标准JSON字符串

      Example example = new Example(IoTDevice.class);
      example.createCriteria().andEqualTo("id", dbDevice.getId());
      int updateCount = ioTDeviceMapper.updateByExampleSelective(updateDevice, example);

      // 6. 更新结果日志
      if (updateCount > 0) {
        log.info(
            "设备期望属性更新DB成功，productKey={}, deviceId={}, 更新字段={}",
            productKey,
            deviceId,
            deviceTagData.keySet());
      } else {
        log.warn("设备期望属性更新DB无数据变更，productKey={}, deviceId={}", productKey, deviceId);
      }

    } catch (Exception e) {
      log.error(
          "设备期望属性更新DB异常，productKey={}, deviceId={}, errorType={}",
          productKey,
          deviceId,
          e.getClass().getSimpleName());
    }
  }

  /**
   * 仅保留可同步到 deviceTag 的属性。
   *
   * <p>source=extended 或未配置 source 的属性继续写入 deviceTag；source=device 的属性跳过。
   */
  private JSONObject filterExtendedPropertiesForDeviceTag(String productKey, JSONObject data) {
    if (productKey == null || data == null || data.isEmpty()) {
      return data;
    }
    try {
      DeviceMetadata metadata = ioTProductDeviceService.getDeviceMetadata(productKey);
      if (metadata == null) {
        log.warn("无法获取产品物模型元数据，deviceTag source 过滤跳过，productKey: {}", productKey);
        return data;
      }

      JSONObject filteredData = new JSONObject();
      for (String propertyId : data.keySet()) {
        AbstractPropertyMetadata property = metadata.getPropertyOrNull(propertyId);
        if (property == null) {
          filteredData.set(propertyId, data.get(propertyId));
          continue;
        }
        if (PropertySource.device.name().equalsIgnoreCase(property.getSource())) {
          log.debug("deviceTag 更新忽略 source=device 属性: {}", propertyId);
          continue;
        }
        filteredData.set(propertyId, data.get(propertyId));
      }
      return filteredData;
    } catch (Exception e) {
      log.error("过滤 deviceTag 可同步属性时发生异常，productKey: {}", productKey, e);
      return data;
    }
  }

  private boolean doShadow(IoTDevice ioTDevice, JSONObject data) {
    if (ioTDevice == null
        || StrUtil.isBlank(ioTDevice.getProductKey())
        || StrUtil.isBlank(ioTDevice.getDeviceId())) {
      return false;
    }
    try {
      IoTDeviceDTO device =
          IoTDeviceDTO.builder()
              .iotId(
                  StrUtil.blankToDefault(
                      ioTDevice.getIotId(), ioTDevice.getProductKey() + ioTDevice.getDeviceId()))
              .productKey(ioTDevice.getProductKey())
              .deviceId(ioTDevice.getDeviceId())
              .extDeviceId(ioTDevice.getExtDeviceId())
              .tenantId(ioTDevice.getTenantId())
              .deviceName(ioTDevice.getDeviceName())
              .build();
      ioTDeviceShadowService.doDesiredShadow(device, data, DateUtil.currentSeconds());
      return true;
    } catch (Exception e) {
      log.error(
          "期望值保存失败，productKey={}, deviceId={}, errorType={}",
          ioTDevice.getProductKey(),
          ioTDevice.getDeviceId(),
          e.getClass().getSimpleName());
      return false;
    }
  }

  /**
   * 过滤掉只读属性，只保留可读写（rw）的属性
   *
   * @param productKey 产品Key
   * @param data 原始数据
   * @return 过滤后的数据，只包含可读写属性
   */
  private JSONObject filterReadWriteProperties(String productKey, JSONObject data) {
    if (productKey == null || data == null || data.isEmpty()) {
      return data;
    }
    try {
      // 获取产品的物模型元数据
      DeviceMetadata metadata = ioTProductDeviceService.getDeviceMetadata(productKey);
      if (metadata == null) {
        log.warn("无法获取产品物模型元数据，productKey: {}", productKey);
        return data;
      }
      // 创建过滤后的数据对象
      JSONObject filteredData = new JSONObject();

      // 遍历原始数据，只保留可读写属性
      for (String propertyId : data.keySet()) {
        AbstractPropertyMetadata property = metadata.getPropertyOrNull(propertyId);
        if (property != null) {
          String mode = property.getMode();
          // 只保留 mode 为 rw（读写）的属性
          if (PropertyMode.rw.name().equals(mode)) {
            filteredData.set(propertyId, data.get(propertyId));
          } else {
            log.debug("过滤掉只读属性: {}, mode: {}", propertyId, mode);
          }
        } else {
          // 如果物模型中找不到该属性，默认保留（向后兼容）
          log.warn("物模型中未找到属性: {}, 默认不保留", propertyId);
          //          filteredData.set(propertyId, data.get(propertyId));
        }
      }

      return filteredData;
    } catch (Exception e) {
      log.error("过滤可读写属性时发生异常，productKey: {}", productKey, e);
      // 发生异常时返回原始数据，避免影响功能
      return data;
    }
  }

}
