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

import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "IoT设备业务对象")
public class IoTDeviceBO implements Serializable {

  private static final long serialVersionUID = 1L;
  public static final int STATE_OFFLINE = 0;
  public static final int STATE_ONLINE = 1;

  @Schema(description = "设备ID")
  private Long id;

  @Schema(description = "第三方平台")
  private String thirdPlatform;

  /** 对外设备唯一标识符 */
  @Schema(description = "IoT ID")
  private String iotId;

  /** Lora密钥 */
  @Schema(description = "Lora密钥")
  private String secretKey;

  /** 归属应用 */
  @Schema(description = "归属应用")
  private String application;

  /** 激活时间 */
  @Schema(description = "注册时间")
  private Integer registryTime;

  /** 最后上线时间 */
  @Schema(description = "在线时间")
  private Long onlineTime;

  /** 设备自身序号 */
  @Schema(description = "设备ID")
  private String deviceId;

  /** 第三方设备ID唯一标识符 */
  @Schema(description = "扩展设备ID")
  private String extDeviceId;

  /** 别名 */
  @Schema(description = "设备昵称")
  private String nickName;

  /** 设备名称 */
  @Schema(description = "产品名称")
  private String productName;

  @Schema(description = "设备特性")
  private Long features;

  /** 网关产品ProductKey */
  @Schema(description = "网关产品Key")
  private String gwProductKey;

  @Schema(description = "网关设备ID")
  private String gwDeviceId;

  /** 设备密钥 */
  @Schema(description = "设备密钥")
  private String deviceSecret;

  /** 产品key */
  @Schema(description = "产品Key")
  private String productKey;

  /** 设备实例名称 */
  @Schema(description = "设备名称")
  private String deviceName;

  @Schema(description = "创建人ID")
  private String creatorId;

  @Schema(description = "租户ID")
  private String tenantId;

  @Schema(description = "部门ID")
  private Long deptId;

  @Schema(description = "创建人姓名")
  private String creatorName;

  /** 0-离线，1-在线 */
  @Schema(description = "在线状态")
  private Integer state;

  /** 说明 */
  @Schema(description = "详细信息")
  private String detail;

  @Schema(description = "创建时间")
  private Long createTime;

  /** 派生元数据,有的设备的属性，功能，事件可能会动态的添加 */
  @Schema(description = "派生元数据")
  private String deriveMetadata;

  /** 其他配置 */
  @Schema(description = "配置信息")
  @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = cn.universal.persistence.common.serialization.PictureSecretConfigurationSerializer.class)
  private String configuration;

  /** 设备坐标 */
  @Schema(description = "设备坐标")
  private String coordinate;

  /** 区域ID */
  @Schema(description = "区域ID")
  private String areasId;

  /** 纬度 */
  @Schema(description = "纬度")
  private String latitude;

  /** 经度 */
  @Schema(description = "经度")
  private String longitude;

  private String deviceAddress;

  private String fenceId;

  @Schema(description = "在线标识")
  private String online;

  @Schema(description = "设备组ID")
  private Long devGroupId;

  @Schema(description = "传输协议")
  private String transportProtocol;

  /** 请求参数 */
  @Builder.Default
  @Schema(description = "请求参数")
  private Map<String, Object> params = new HashMap<>();

  @Schema(description = "是否有协议")
  private Boolean hasProtocol;

  /** 接收额外参数 */
  @Builder.Default
  @Schema(description = "其他参数")
  private Map<String, Object> otherParams = new HashMap<>();

  @Schema(description = "设备组名称列表")
  private List<String> devGroupName;

  /** 所属父设备信息 */
  @Schema(description = "网关设备信息")
  private String gwDeviceInfo;

  /** 子设备地址 */
  @Schema(description = "子设备地址")
  private String slaveAddress;

  @Schema(description = "设备影子期望值")
  private String deviceTag;

  @Schema(description = "是否待分配归属账号")
  private Boolean pendingOwner;

  @JsonSetter("state")
  public void setStateFromBoolean(Boolean state) {
    this.state = toStateValue(state);
  }

  public static Integer toStateValue(Boolean state) {
    if (state == null) {
      return null;
    }
    return state ? STATE_ONLINE : STATE_OFFLINE;
  }
}
