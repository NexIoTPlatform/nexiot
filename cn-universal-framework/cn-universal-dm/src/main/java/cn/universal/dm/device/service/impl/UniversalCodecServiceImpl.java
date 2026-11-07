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

package cn.universal.dm.device.service.impl;

import cn.universal.core.protocol.support.ProtocolSupportDefinition;
import cn.universal.core.service.AbstractCodecService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 统一编解码服务实现类
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/01/20
 */
@Slf4j
@Service
public class UniversalCodecServiceImpl extends AbstractCodecService {

  @Autowired private IoTProductDeviceService iotProductDeviceService;

  @Override
  protected ProtocolSupportDefinition getProtocolDefinition(String productKey) {
    return iotProductDeviceService.selectProtocolDef(productKey);
  }

  @Override
  protected ProtocolSupportDefinition getProtocolDefinitionNoScript(String productKey) {
    return iotProductDeviceService.selectProtocolDefNoScript(productKey);
  }

  @Override
  protected ProtocolSupportDefinition getProtocolDefinitionWithScript(String productKey) {
    return iotProductDeviceService.selectProtocolDef(productKey);
  }
}
