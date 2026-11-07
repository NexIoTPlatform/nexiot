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

package cn.universal.persistence.dto;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.annotation.Excel;
import cn.universal.common.constant.IoTConstant.DeviceNode;
import cn.universal.core.metadata.DeviceMetadata;
import cn.universal.persistence.entity.IoTDevice;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "IoT设备DTO")
public class IoTDeviceDTO {

  @Schema(description = "主键ID")
  private Long id;

  @Schema(description = "归属应用ID")
  private String applicationId;

  @Schema(description = "应用ID")
  private String appId;

  @Schema(description = "用户ID")
  private String userId;

  @Schema(description = "用户唯一标志")
  private String userUnionId;

  @Schema(description = "租户ID")
  private String tenantId;

  @Schema(description = "上行主题")
  private String upTopic;

  @Schema(description = "下行主题")
  private String downTopic;

  @Schema(description = "作用域")
  private String scope;

  @Schema(description = "IoT ID")
  private String iotId;

  @Schema(description = "设备ID")
  private String deviceId;

  @Schema(description = "扩展设备ID")
  private String extDeviceId;

  @Schema(description = "设备名称")
  private String deviceName;

  @Schema(description = "设备节点")
  private DeviceNode deviceNode;

  @Schema(description = "产品Key")
  private String productKey;

  @Schema(description = "产品名称")
  private String productName;

  @Schema(description = "产品创建人")
  private String productCreatorId;

  @Schema(description = "第三方平台")
  private String thirdPlatform;

  @Schema(description = "网关产品Key")
  private String gwProductKey;

  @Schema(description = "设备密钥")
  private String deviceSecret;

  @Schema(description = "元数据")
  private String metadata;

  @Schema(description = "配置")
  private String configuration;

  // 在线和离线
  /** 0-离线，1-在线 */
  @Excel(name = "在线状态")
  @Schema(description = "在线状态")
  private Integer state;

  /** 应用状态,true表示禁用 */
  @Schema(description = "应用禁用状态")
  private Integer appDisable;

  /** 首次注册时间 */
  @Schema(description = "注册时间")
  private Integer registryTime;

  @Schema(description = "在线时间")
  private Integer onlineTime;

  @Schema(description = "设备组ID列表")
  private List<String> devGroupId;

  @Schema(description = "产品配置")
  private JSONObject productConfig;

  /** 是否有影子 */
  @Schema(description = "是否有影子")
  private boolean shadow;

  /** 设备配置字段 */
  @Schema(description = "设备配置")
  private String devConfiguration;

  @Schema(description = "区域ID")
  private String areasId;

  @Schema(description = "坐标")
  private String coordinate;

  @Schema(description = "详情")
  private String detail;

  @Schema(description = "存储策略配置")
  private String storePolicyCfg;

  @Schema(description = "存储策略")
  private String storePolicy;

  /**
   * 原串
   *
   * @return
   */
  @Schema(description = "负载数据")
  private String payload;

  @JsonIgnore
  public DeviceMetadata getDeviceMetadata() {
    if (StrUtil.isNotBlank(metadata)) {
      return new DeviceMetadata(new JSONObject(metadata));
    }
    return new DeviceMetadata(new JSONObject());
  }

  public JSONObject getProductConfig() {
    if (StrUtil.isNotBlank(configuration)) {
      productConfig = JSONUtil.parseObj(configuration);
    } else {
      productConfig = new JSONObject();
    }
    return productConfig;
  }

  public void clearMetadata() {
    setMetadata(null);
  }

  public boolean isOnline() {
    return isOnlineState(state);
  }

  public static Integer toStateValue(Boolean state) {
    if (state == null) {
      return null;
    }
    return state ? IoTDevice.STATE_ONLINE : IoTDevice.STATE_OFFLINE;
  }

  public static boolean isOnlineState(Integer state) {
    return state != null && state == IoTDevice.STATE_ONLINE;
  }
}
