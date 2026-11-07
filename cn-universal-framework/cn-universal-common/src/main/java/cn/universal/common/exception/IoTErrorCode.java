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

package cn.universal.common.exception;

/** IoT设备错误码 */
public enum IoTErrorCode {
  /** 设备 */
  DEV_NOT_FIND(700, "设备不存在"),
  DEV_NOT_FOR_YOU(701, "设备没有操作权限！"),
  DEV_DEL_ERROR(703, "设备删除错误"),
  DEV_UPDATE_ERROR(704, "设备修改错误"),
  DEV_UPDATE_DEVICE_NO_ID_EXIST(705, "deviceId不存在"),
  DEV_CONFIG_DEVICE_NO_ID_EXIST(706, "deviceId不存在"),
  DEV_CONFIG_DEVICE_PARA_NULL(707, "参数为空"),
  DEV_CONFIG_DEVICE_LEVEL_ERROR(708, "level不合法"),
  DEV_CONFIG_DEVICE_STATE_ERROR(709, "设备未激活"),
  DEV_CONFIG_DEVICE_PARA_ERROR(710, "参数校验失败"),
  DEV_CONFIG_DEVICE_MASTERKEY_ERROR(711, "MasterKey不匹配"),
  DEV_CONFIG_DEVICE_MESSAGE_ERROR(712, "指令应为偶数"),
  DEV_CONFIG_DEVICE_NULL(713, "下发指令不能为空"),
  DEV_CONFIG_PAYLOAD_NULL(714, "下发报文(payload)不能为空"),
  DEV_CONFIG_ERROR(715, "设备配置错误"),
  DEV_DOWN_ADD_ERROR(717, "指令 cmd 配置错误(指令 cmd 不能为空)"),
  DEV_PARA_RANGE_ERROR(721, "指令下发内容与服务模型参数范围不匹配"),
  DEV_DOWN_CMD_ERROR(715, "下发指令错误"),
  DEV_CONFIG_DEVICE_PARA_FAIL(716, "参数解析失败"),
  DEV_DOWN_NOT_SUPPORT(721, "设备不支持功能下发"),
  DEV_DOWN_FAILURE(722, "发送设备失败（期望值设置成功）"),
  DEV_DOWN_NOT_ONLINE(723, "设备已断开，指令已暂存"),
  DEV_SUBSCRIBE_REPEAT_ERROR(730, "超过最大允许订阅数"),
  DEV_MASTER_NOT_EXIST_ERROR(731, "主设备不存在"),
  DEV_ADD_DEVICE_ERROR(732, "设备序列号不合法"),

  DEV_METADATA_NOT_FIND(750, "物模型不存在"),
  /** 编解码 */
  CODEC_ERROR(758, "编解码异常"),
  DEV_METADATA_FUNCTION_NOT_FIND(751, "物模型定义功能不存在"),

  DEV_ADD_ERROR(799, "设备添加错误,请检查参数"),

  /** 参数校验 */
  DATA_CAN_NOT_NULL(600, "数据不能为空"),

  /** 应用 */
  APPLICATION_BIND_FAILURE(800, "绑定应用失败"),

  APPLICATION_NOT_FOR_YOU(801, "应用没有操作权限！"),
  /** 产品 */
  PRODUCT_NOT_FOR_YOU(901, "无产品权限"),
  PRODUCT_NOT_EXIST(902, "产品不存在");

  private Integer code;
  private String name;

  private IoTErrorCode(Integer code, String name) {
    this.code = code;
    this.name = name;
  }

  public Integer getCode() {
    return code;
  }

  public String getName() {
    return name;
  }
}
