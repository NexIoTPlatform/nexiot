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

import cn.universal.core.message.UPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/1/12
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
public class BaseUPRequest extends UPRequest {

  private transient IoTDeviceDTO ioTDeviceDTO;

  private transient IoTProduct ioTProduct;

  /** 指令 */
  private String commandId;

  /** 指令 */
  private Integer commandStatus;

  /** 回复消息 - 需要发送给设备的响应 */
  private Object replyPayload;

  public Object getReplyPayload() {
    return replyPayload;
  }

  public void setReplyPayload(Object replyPayload) {
    this.replyPayload = replyPayload;
  }
}
