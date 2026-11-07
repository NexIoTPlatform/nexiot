/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 */
package cn.universal.core.message;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Request implements Serializable {

  /** 应用唯一标识，同数据库（app_unique_id） */
  private String applicationId;

  /** 用户标识 */
  private String appUnionId;

  /** 产品唯一标识 */
  private String productKey;

  /** 设备唯一标识 */
  private String iotId;

  /** 设备ID */
  private String deviceId;

  /** 消息原文 */
  private String payload;

  /** 原始字节 */
  private byte[] payloadRaw;

  /** 设备名称 */
  private String deviceName;

  private String requestId;

  private String tenantId;
}
