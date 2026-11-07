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

package cn.universal.core.message;

import cn.universal.common.constant.IoTConstant.DownCmd;
import java.io.Serializable;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/8/13 9:34
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
public class DownRequest extends Request implements Serializable {

  /* 网关绑定设备 ProductKey */
  private String gwProductKey;
  /* 网关绑定设备deviceId */
  private String gwDeviceId;

  /** 为了兼容gwDeviceId */
  private String extDeviceId;

  /** 设备密钥 */
  private String deviceKey;

  /** 从站地址 */
  private String slaveAddress;

  /** 命令名称 */
  private DownCmd cmd;

  /** 消息标识符 */
  private String msgId;

  /** 备注 */
  private String detail;

  /** 资源归属部门 */
  private Long deptId;

  /** payload 来源(codec/raw-fallback/shadow-only) */
  private String payloadSource;

  private String deviceAddress;

  private Map<String, Object> function;

  private Long storeTime;

  /** 设备复用 */
  private boolean deviceReuse;

  /** 是否超管 */
  private boolean isAdmin;

  /** 是否自动注册 */
  private boolean allowInsert;

  // ===== 网关子设备支持（网关代理模式）=====

  /** 子设备ID（网关代理模式时使用，记录真实的子设备ID） */
  private String subDeviceId;

  /** 子设备ProductKey（网关代理模式时使用，记录真实的子设备ProductKey） */
  private String subProductKey;

  /** 是否为网关代理模式（true表示使用网关设备通信，但编解码用子设备） */
  private Boolean isGatewayProxy = false;
}
