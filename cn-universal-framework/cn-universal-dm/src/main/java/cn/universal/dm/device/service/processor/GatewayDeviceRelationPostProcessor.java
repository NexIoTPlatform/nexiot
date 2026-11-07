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

package cn.universal.dm.device.service.processor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.DeviceNode;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.core.message.DownRequest;
import cn.universal.persistence.base.IoTDevicePostProcessor;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 网关设备关系后置处理器
 *
 * <p>负责维护子设备与网关的地址映射关系
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Component
@Slf4j
public class GatewayDeviceRelationPostProcessor implements IoTDevicePostProcessor {

  @Resource private IoTDeviceMapper ioTDeviceMapper;

  @Override
  public String getName() {
    return "GatewayDeviceRelationPostProcessor";
  }

  @Override
  public int getOrder() {
    return 100; // 较低优先级，确保在其他处理器之后执行
  }

  @Override
  public boolean supports(Operation operation) {
    // 支持设备创建、更新和删除操作
    return operation == Operation.CREATE
        || operation == Operation.UPDATE
        || operation == Operation.DELETE;
  }

  @Override
  public void process(Operation operation, IoTDeviceDTO deviceDTO, DownRequest downRequest) {
    try {
      switch (operation) {
        case CREATE:
        case UPDATE:
          handleCreateOrUpdate(deviceDTO, downRequest);
          break;
        case DELETE:
          handleDelete(deviceDTO, downRequest);
          break;
        default:
          log.debug("不支持的操作类型: {}", operation);
      }
    } catch (Exception e) {
      log.warn(
          "维护网关设备关系失败, operation={}, deviceId={}: {}",
          operation,
          deviceDTO.getDeviceId(),
          e.getMessage(),
          e);
    }
  }

  /** 处理设备创建或更新操作 */
  private void handleCreateOrUpdate(IoTDeviceDTO deviceDTO, DownRequest downRequest) {
    if (downRequest == null) {
      return;
    }

    String gwDeviceId = downRequest.getGwDeviceId();
    String gwProductKey = downRequest.getGwProductKey();
    DownCmd downCmd = downRequest.getCmd();

    // 只处理有网关设备信息且是设备添加或更新操作的情况
    if (gwProductKey == null
        || gwDeviceId == null
        || (!DownCmd.DEV_UPDATE.equals(downCmd) && !DownCmd.DEV_ADD.equals(downCmd))) {
      return;
    }

    updateGatewaySubdeviceMapping(gwProductKey, gwDeviceId);
    handModbusAdd(deviceDTO, downRequest);
  }

  /**
   * 处理一下modbus的从站地址维护
   *
   * @param deviceDTO
   * @param downRequest
   */
  private void handModbusAdd(IoTDeviceDTO deviceDTO, DownRequest downRequest) {
    if (deviceDTO == null
        || deviceDTO.getProductKey() == null
        || deviceDTO.getDeviceId() == null
        || downRequest == null
        || downRequest.getSlaveAddress() == null) {
      return;
    }
    IoTDevice ioTDevice =
        ioTDeviceMapper.selectIoTDevice(deviceDTO.getProductKey(), deviceDTO.getDeviceId());
    if (ioTDevice == null) return;

    // 创建更新对象，只设置需要更新的字段
    IoTDevice updateDevice = new IoTDevice();
    updateDevice.setId(ioTDevice.getId());
    updateDevice.setExt1(downRequest.getSlaveAddress());
    // 根据主键id只更新ext1字段
    ioTDeviceMapper.updateByPrimaryKeySelective(updateDevice);
  }

  /** 处理设备删除操作 */
  private void handleDelete(IoTDeviceDTO deviceDTO, DownRequest downRequest) {
    // 情况1: 删除的是子设备，需要从网关的映射关系中移除
    if (isSubDevice(deviceDTO)) {
      removeSubDeviceFromGatewayMapping(deviceDTO);
    }

    // 情况2: 删除的是网关设备，需要处理其所有子设备
    if (isGatewayDevice(deviceDTO)) {
      handleGatewayDeviceDeletion(deviceDTO);
    }
  }

  /** 判断是否为子设备 */
  private boolean isSubDevice(IoTDeviceDTO deviceDTO) {
    if (deviceDTO == null || deviceDTO.getConfiguration() == null) {
      return false;
    }

    return DeviceNode.GATEWAY_SUB_DEVICE.equals(deviceDTO.getDeviceNode());
  }

  /** 判断是否为网关设备 */
  private boolean isGatewayDevice(IoTDeviceDTO deviceDTO) {
    if (deviceDTO == null || deviceDTO.getProductConfig() == null) {
      return false;
    }

    // 检查产品配置中是否包含网关相关配置
    return deviceDTO.getProductConfig().containsKey("subdeviceMapping")
        || DeviceNode.GATEWAY.getValue().equalsIgnoreCase(deviceDTO.getDeviceNode().getValue());
  }

  /** 从网关映射关系中移除子设备 */
  private void removeSubDeviceFromGatewayMapping(IoTDeviceDTO subDeviceDTO) {
    try {
      // 从子设备的配置中获取网关信息
      String gwProductKey = subDeviceDTO.getGwProductKey();

      if (gwProductKey == null) {
        log.debug("子设备无网关产品信息, subDeviceId={}", subDeviceDTO.getDeviceId());
        return;
      }

      // 从子设备的配置中获取网关设备ID
      String gwDeviceId = subDeviceDTO.getExtDeviceId();
      if (gwDeviceId == null) {
        log.debug("子设备无网关设备ID信息, subDeviceId={}", subDeviceDTO.getDeviceId());
        return;
      }

      // 更新网关设备的子设备映射关系
      updateGatewaySubdeviceMapping(gwProductKey, gwDeviceId);

      log.info(
          "从网关映射关系中移除子设备, gatewayDeviceId={}, subDeviceId={}",
          gwDeviceId,
          subDeviceDTO.getDeviceId());

    } catch (Exception e) {
      log.warn(
          "从网关映射关系中移除子设备失败, subDeviceId={}: {}", subDeviceDTO.getDeviceId(), e.getMessage(), e);
    }
  }

  /** 处理网关设备删除 */
  private void handleGatewayDeviceDeletion(IoTDeviceDTO gatewayDeviceDTO) {
    try {
      // 查询该网关下的所有子设备
      List<IoTDevice> subDevices =
          ioTDeviceMapper.selectSubDeviceByGw(
              gatewayDeviceDTO.getProductKey(), gatewayDeviceDTO.getDeviceId());

      if (CollUtil.isEmpty(subDevices)) {
        log.debug("网关设备下无子设备, gatewayDeviceId={}", gatewayDeviceDTO.getDeviceId());
        return;
      }

      // 处理每个子设备
      for (IoTDevice subDevice : subDevices) {
        // 可以选择以下策略之一：
        // 1. 删除所有子设备
        // 2. 将子设备标记为孤儿设备
        // 3. 尝试将子设备迁移到其他网关

        // 这里采用策略2：将子设备标记为孤儿设备
        markSubDeviceAsOrphan(subDevice);
      }

      log.info(
          "处理网关设备删除完成, gatewayDeviceId={}, 影响子设备数量={}",
          gatewayDeviceDTO.getDeviceId(),
          subDevices.size());

    } catch (Exception e) {
      log.warn(
          "处理网关设备删除失败, gatewayDeviceId={}: {}", gatewayDeviceDTO.getDeviceId(), e.getMessage(), e);
    }
  }

  /** 将子设备标记为孤儿设备 */
  private void markSubDeviceAsOrphan(IoTDevice subDevice) {
    try {
      JSONObject cfg = JSONUtil.parseObj(subDevice.getConfiguration());
      if (cfg == null) {
        cfg = new JSONObject();
      }

      // 添加孤儿设备标记
      cfg.set("isOrphan", true);
      cfg.set("orphanTime", System.currentTimeMillis());

      subDevice.setConfiguration(JSONUtil.toJsonStr(cfg));
      ioTDeviceMapper.updateDevInstance(subDevice);

      log.info("子设备标记为孤儿设备, subDeviceId={}", subDevice.getDeviceId());

    } catch (Exception e) {
      log.warn("标记子设备为孤儿设备失败, subDeviceId={}: {}", subDevice.getDeviceId(), e.getMessage(), e);
    }
  }

  /** 更新网关设备的子设备映射关系 */
  private void updateGatewaySubdeviceMapping(String gwProductKey, String gwDeviceId) {
    try {
      // 查询网关设备下的所有子设备
      List<IoTDevice> ioTDevices = ioTDeviceMapper.selectSubDeviceByGw(gwProductKey, gwDeviceId);
      IoTDevice gwIoTDevice =
          ioTDeviceMapper.selectOne(
              IoTDevice.builder().deviceId(gwDeviceId).productKey(gwProductKey).build());

      if (gwIoTDevice == null) {
        log.debug("网关设备不存在, gwDeviceId={}, gwProductKey={}", gwDeviceId, gwProductKey);
        return;
      }

      // 构建子设备映射关系
      Map<String, Object> subdeviceMapping = new HashMap<>();
      for (IoTDevice subDevice : ioTDevices) {
        JSONObject cfg = JSONUtil.parseObj(subDevice.getConfiguration());
        if (cfg == null || !cfg.containsKey("slaveAddress")) {
          continue;
        }

        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("productKey", subDevice.getProductKey());
        deviceInfo.put("productName", subDevice.getProductName());
        subdeviceMapping.put(cfg.getStr("slaveAddress"), deviceInfo);
      }

      // 更新网关设备的configuration字段
      JSONObject gwCfg = JSONUtil.parseObj(gwIoTDevice.getConfiguration());
      if (gwCfg == null) {
        gwCfg = new JSONObject();
      }
      gwCfg.set("subdeviceMapping", subdeviceMapping);

      // 更新设备配置
      gwIoTDevice.setConfiguration(JSONUtil.toJsonStr(gwCfg));
      ioTDeviceMapper.updateDevInstance(gwIoTDevice);

      log.info(
          "更新网关设备映射关系完成, gwDeviceId={}, gwProductKey={}, 子设备数量={}",
          gwDeviceId,
          gwProductKey,
          subdeviceMapping.size());

    } catch (Exception e) {
      log.warn(
          "更新网关设备映射关系失败, gwDeviceId={}, gwProductKey={}: {}",
          gwDeviceId,
          gwProductKey,
          e.getMessage(),
          e);
    }
  }
}
