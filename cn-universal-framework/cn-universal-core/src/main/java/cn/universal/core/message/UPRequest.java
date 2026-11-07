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

import cn.hutool.json.JSONObject;
import cn.universal.common.constant.IoTConstant.DeviceNode;
import cn.universal.common.constant.IoTConstant.MessageType;
import java.io.Serializable;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 上行请求类
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/8/9 15:51
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
public class UPRequest extends Request implements Serializable {

  // true：无论设备在线离线设备指令总是缓存
  private transient boolean onlyCache;
  // true: 设备不存在时直接新增该设备
  private transient boolean allowInsert;
  private transient boolean standardTcp;
  private transient boolean preStore;
  private String userUnionId;
  private String extDeviceId;

  /** 设备节点类型 */
  private DeviceNode deviceNode;

  private MessageType messageType;
  private String event;
  private String eventName;
  /** 标准化后的data，供规则、影子等链路按Map结构消费 */
  private Map<String, Object> data;
  /** 原始上报中的data，兼容数组、字符串、数字等非对象结构 */
  private Object rawData;
  private Map<String, Object> properties;
  private Map<String, Object> tags;
  private String function;
  private String functionName;
  private JSONObject shadow;
  private String childDeviceId;
  // 子设备信息
  private SubDevice subDevice;

  /** 独立订阅地址 */
  private transient Object devSubscribe;

  /** 时间 */
  private Long time;

  /** 时间戳字符串 */
  private String ts;

  /** 是否是debug上报 */
  private transient boolean debug;

  /** 空的编解码 */
  private transient boolean emptyProtocol;

  /** 下行topic */
  private String downTopic;
}
