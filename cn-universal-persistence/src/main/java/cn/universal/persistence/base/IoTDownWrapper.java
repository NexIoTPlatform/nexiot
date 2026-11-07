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

package cn.universal.persistence.base;

import cn.universal.common.domain.R;
import cn.universal.core.message.DownRequest;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/8/10
 */
public interface IoTDownWrapper {

  /**
   * 用于全局调用的处理
   *
   * @param product
   * @param downRequest
   * @return
   */
  default R beforeDownAction(IoTProduct product, Object data, DownRequest downRequest) {
    return null;
  }

  /**
   * 用于全局调用的处理
   *
   * @param product
   * @param ioTDevice
   * @param downRequest
   * @return
   */
  default R beforeFunctionOrConfigDown(
      IoTProduct product, IoTDevice ioTDevice, DownRequest downRequest) {
    return null;
  }
}
