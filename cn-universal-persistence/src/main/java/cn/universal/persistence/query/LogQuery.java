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

package cn.universal.persistence.query;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/11/15
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
public class LogQuery extends BasePage {

  /** 日志ID，非自增 */
  private String id;

  /** 设备编码 */
  private String iotId;

  /** 设备序列号 */
  private String deviceId;

  /** 第三方设备ID唯一标识符 */
  private String extDeviceId;

  /** 产品ID */
  private String productKey;

  /** 设备名称 */
  private String deviceName;

  /** 消息类型 */
  private String messageType;

  /** 事件名称 */
  private String event;

  /** 请求参数 */
  private Map<String, Object> params;

  private Long endCreateTime;

  private Long beginCreateTime;

  /** 使用无总数的游标查询，避免 COUNT 和深 OFFSET。 */
  private boolean cursorMode;

  private String cursor;
  private LocalDateTime beforeTime;
  private Long beforeId;

  public int getFetchSize() {
    return getPageSize() + 1;
  }

  /** Match the LocalDateTime used by writers, independent of the database session timezone. */
  public LocalDateTime getBeginTime() {
    return beginCreateTime == null
        ? null
        : LocalDateTime.ofInstant(
            java.time.Instant.ofEpochSecond(beginCreateTime), java.time.ZoneId.systemDefault());
  }

  public LocalDateTime getEndTime() {
    return endCreateTime == null
        ? null
        : LocalDateTime.ofInstant(
            java.time.Instant.ofEpochSecond(endCreateTime), java.time.ZoneId.systemDefault());
  }

  /** 属性 */
  private String property;
}
