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

import cn.universal.core.message.DownRequest;
import cn.universal.persistence.base.IoTDeviceLifeCycle;
import cn.universal.persistence.dto.IoTDeviceDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 生命周期前置处理
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/5/30
 */
@Service("ioTDeviceActionBeforeService")
@Slf4j
public class IoTDeviceActionBeforeService implements IoTDeviceLifeCycle {

  @Override
  public void create(String productKey, String deviceId, DownRequest downRequest) {}

  @Override
  public void online(String productKey, String deviceId) {}

  @Override
  public void offline(String productKey, String deviceId) {}

  @Override
  public void update(String productKey, String deviceId, DownRequest downRequest) {}

  @Override
  public void enable(String iotId) {}

  @Override
  public void disable(String iotId) {}

  @Override
  public void delete(IoTDeviceDTO ioTDeviceDTO, DownRequest downRequest) {}
}
