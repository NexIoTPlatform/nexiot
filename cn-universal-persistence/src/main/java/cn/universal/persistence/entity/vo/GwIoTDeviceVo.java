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

package cn.universal.persistence.entity.vo;

import lombok.Data;

@Data
public class GwIoTDeviceVo {
  private String iotId;
  private String productKey;
  private String deviceId;
  private String deviceName;
  private String productName;
  private String creatorId;
  private Long deptId;
  private String tenantId;
  private String thirdPlatform;
  private String transportProtocol;
  private Boolean state;
  private Long onlineTime;
  private String configuration;
  private String ext1; // 从站地址
  private String signalStrength; // CSQ信号强度
  private String deviceTag; // 设备标签
  private String deviceAddress; // 设备地址
  private String photoUrl;
}
