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

package cn.universal.persistence.entity.bo;

import cn.universal.common.domain.BaseEntity;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class IoTDeviceLogBO extends BaseEntity {

  private static final long serialVersionUID = 1L;

  /** 日志ID，非自增 */
  private Long id;

  /** 设备编码 */
  private String iotId;

  /** 设备序列号 */
  private String deviceId;

  /** 第三方设备ID唯一标识符 */
  @Transient private String extDeviceId;

  /** 产品ID */
  private String productKey;

  /** 设备名称 */
  private String deviceName;

  /** 消息类型 */
  private String messageType;

  /** 公司名称 */
  private String companyNo;

  /** 协议名称 */
  private String protocol;

  /** 节点类型 */
  private String deviceNode;

  /** 设备分类编号 */
  private String classifiedId;

  /** 组织ID */
  private String orgId;

  /** 事件名称 */
  private String event;

  /** 创建人 */
  private String createId;

  /** 经纬度 */
  private String point;

  /** 内容 */
  private String content;
}
