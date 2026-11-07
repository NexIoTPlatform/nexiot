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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "IoT设备VO")
public class IoTDeviceVO {

  private static final long serialVersionUID = 1L;

  /** 对外设备唯一标识符 */
  @Schema(description = "IoT ID")
  private String iotId;

  @Schema(description = "网关产品Key")
  private String gwProductKey;

  /** 设备自身序号 */
  @Schema(description = "设备ID")
  private String deviceId;

  /** 派生元数据,有的设备的属性，功能，事件可能会动态的添加 */
  @Schema(description = "元数据")
  private String metadata;

  /** 产品key */
  @Schema(description = "产品Key")
  private String productKey;

  /** 设备名称 */
  @Schema(description = "产品名称")
  private String productName;

  /** 设备实例名称 */
  @Schema(description = "设备名称")
  private String deviceName;

  /** 别名 */
  @Schema(description = "设备昵称")
  private String nickName;

  @Schema(description = "经纬度坐标")
  private String latlng;

  @Schema(description = "设备节点")
  private String deviceNode;

  @Schema(description = "订阅主题")
  private String topic;

  @Schema(description = "订阅消息类型")
  private String subMsgType;

  @Schema(description = "订阅类型")
  private String subType;

  @Schema(description = "订阅URL")
  private String subUrl;

  @Schema(description = "设备状态")
  private String state;

  @Schema(description = "设备类型名称")
  private String deviceTypeName;

  @Schema(description = "设备型号")
  private String deviceModel;

  @Schema(description = "分类名称")
  private String classifiedName;

  @Schema(description = "名称")
  private String name;
}
