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

import cn.universal.common.annotation.Excel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Schema(description = "设备属性视图对象")
public class IoTDeviceLogMetadataVO {

  private static final long serialVersionUID = 1L;

  /*-----------------日志信息---------------------*/

  /** 日志ID，非自增 */
  @com.fasterxml.jackson.databind.annotation.JsonSerialize(
      using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
  private Long id;

  /** 设备编码 */
  private String iotId;

  /** 设备序列号 */
  @Excel(name = "设备序列号")
  private String deviceId;

  /** 产品ID */
  @Excel(name = "产品Key")
  private String productKey;

  /** 设备名称 */
  @Excel(name = "设备名称")
  private String deviceName;

  /** 消息类型 */
  @Excel(name = "消息类型")
  private String messageType;

  private String event;
  private String property;

  @Excel(name = "属性值/事件内容")
  private String content;

  @Excel(name = "属性名称")
  private String ext1;

  private String ext2;

  @Excel(name = "单位")
  private String ext3;

  @Excel(name = "创建时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime createTime;
}
