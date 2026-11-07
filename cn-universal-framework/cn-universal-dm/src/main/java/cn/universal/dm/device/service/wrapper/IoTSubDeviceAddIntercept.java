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

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.common.constant.IoTConstant.DeviceNode;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.domain.R;
import cn.universal.core.message.DownRequest;
import cn.universal.core.message.SubDevice;
import cn.universal.dm.device.service.sub.strategy.SubDeviceIdGeneratorStrategy;
import cn.universal.persistence.base.DownRequestDataHelper;
import cn.universal.persistence.base.IoTDownWrapper;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 子设备逻辑校验 */
@Service("ioTSubDeviceAddIntercept")
@Slf4j
public class IoTSubDeviceAddIntercept implements IoTDownWrapper {

  @Resource private IoTDeviceMapper ioTDeviceMapper;
  @Resource private SubDeviceIdGeneratorStrategy subDeviceIdGeneratorStrategy;

  @Override
  public R beforeDownAction(IoTProduct product, Object data, DownRequest downRequest) {
    /*返回的R不为空，会影响后续流程，此处只做参数的补充*/
    if (product == null) {
      return null;
    }
    if (downRequest.getCmd() == null) {
      return null;
    }
    if (DownCmd.DEV_ADD.equals(downRequest.getCmd())
        || DownCmd.DEV_ADDS.equals(downRequest.getCmd())) {
      return doGwSubDeviceAdd(product, data, downRequest);
    }
    if (DownCmd.DEV_DEL.equals(downRequest.getCmd())) {
      List<IoTDevice> ioTDevices =
          ioTDeviceMapper.selectSubDeviceByGw(
              downRequest.getProductKey(), downRequest.getDeviceId());
      if (CollUtil.size(ioTDevices) > 0) {
        return R.error("存在子设备，无法删除");
      }
    }
    return null;
  }

  private R doGwSubDeviceAdd(IoTProduct product, Object data, DownRequest downRequest) {
    if (!DeviceNode.GATEWAY_SUB_DEVICE.name().equals(product.getDeviceNode())) {
      return null;
    }
    String gwProductKey = StrUtil.blankToDefault(downRequest.getGwProductKey(), product.getGwProductKey());
    if (StrUtil.isBlank(gwProductKey)) {
      return R.error("添加网关子设备,请选择所属网关");
    }
    downRequest.setGwProductKey(gwProductKey);
    R<Void> error = buildGwDeviceId(data, downRequest);
    if (error != null) {
      return error;
    }
    // 强制子设备的deviceId：父设备deviceId+slaveAddress
    if (StrUtil.isNotBlank(downRequest.getSlaveAddress())) {
      String subDeviceId =
          subDeviceIdGeneratorStrategy.generateSubDeviceId(
              gwProductKey,
              downRequest.getGwDeviceId(),
              SubDevice.builder().slaveAddress(downRequest.getSlaveAddress()).build());
      downRequest.setDeviceId(subDeviceId);
    }
    return null;
  }

  private R<Void> buildGwDeviceId(Object data, DownRequest downRequest) {
    Map<String, Object> downData = buildMap(data);
    String gwDeviceId = downRequest.getGwDeviceId();
    if (StrUtil.isBlank(gwDeviceId)) {
      String gwDeviceIdFromData = (String) downData.get("gwDeviceId");
      String extDeviceId = downRequest.getExtDeviceId();
      String extDeviceIdFromData = (String) downData.get("extDeviceId");
      // 依次从不同来源获取gwDeviceId
      if (StrUtil.isNotBlank(gwDeviceIdFromData)) {
        downRequest.setGwDeviceId(gwDeviceIdFromData);
      } else if (StrUtil.isNotBlank(extDeviceId)) {
        downRequest.setGwDeviceId(extDeviceId);
      } else if (StrUtil.isNotBlank(extDeviceIdFromData)) {
        downRequest.setGwDeviceId(extDeviceIdFromData);
      } else {
        return R.error("添加子设备，必须携带网关设备gwDeviceId");
      }
    }
    if (StrUtil.isBlank(downRequest.getSlaveAddress())) {
      downRequest.setSlaveAddress(StrUtil.utf8Str(downData.get("slaveAddress")));
    }
    return null;
  }

  private Map<String, Object> buildMap(Object data) {
    return DownRequestDataHelper.toMap(data);
  }
}
