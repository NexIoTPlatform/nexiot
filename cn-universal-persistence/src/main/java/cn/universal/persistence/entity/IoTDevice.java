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

package cn.universal.persistence.entity;

import cn.universal.common.annotation.Excel;
import cn.universal.persistence.common.inteceptor.SQenGenId;
import cn.universal.persistence.common.typehandler.PostgreSqlJsonbStringTypeHandler;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.JdbcType;
import tk.mybatis.mapper.annotation.ColumnType;
import tk.mybatis.mapper.annotation.KeySql;

@Table(name = "iot_device")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "IoT设备实体")
public class IoTDevice implements Serializable {

  private static final long serialVersionUID = 1L;
  public static final int STATE_OFFLINE = 0;
  public static final int STATE_ONLINE = 1;

  @Schema(description = "主键ID")
  @Id
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 对外设备唯一标识符 */
  @Column(name = "iot_id")
  @Schema(description = "IoT ID")
  private String iotId;

  /** 设备自身序号 */
  @Excel(name = "设备序列号")
  @Column(name = "device_id")
  @Schema(description = "设备ID")
  private String deviceId;

  /** 实例名称 */
  // @Excel(name = "实例名称")
  @Column(name = "instance")
  @Schema(description = "实例名称")
  private String instance;

  /** 归属应用 */
  @Column(name = "application")
  @Schema(description = "归属应用")
  private String application;

  /** 激活时间 */
  @Column(name = "registry_time")
  @Schema(description = "注册时间")
  private Integer registryTime;

  /** 最后上线时间 */
  @Column(name = "online_time")
  @Schema(description = "在线时间")
  private Long onlineTime;

  /** 第三方设备ID唯一标识符 */
  @Column(name = "ext_device_id")
  @Schema(description = "扩展设备ID")
  private String extDeviceId;

  /** 设备名称 */
  @Column(name = "product_name")
  @Schema(description = "产品名称")
  private String productName;

  /** 网关产品ProductKey */
  @Excel(name = "网关产品ProductKey")
  @Column(name = "gw_product_key")
  @Schema(description = "网关产品Key")
  private String gwProductKey;

  /** 设备密钥 */
  @Excel(name = "设备密钥")
  @Column(name = "device_secret")
  @Schema(description = "设备密钥")
  private String deviceSecret;

  /** 产品key */
  @Column(name = "product_key")
  @Schema(description = "产品Key")
  @Excel(name = "所属产品/ProductKey")
  private String productKey;

  /** 设备实例名称 */
  @Excel(name = "设备名称")
  @Column(name = "device_name")
  @Schema(description = "设备名称")
  private String deviceName;

  @Column(name = "creator_id")
  @Schema(description = "创建者ID")
  private String creatorId;

  @Column(name = "dept_id")
  @Schema(description = "部门ID")
  private Long deptId;

  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

  /** 扩展字段1 */
  @Column(name = "ext1")
  @Schema(description = "扩展字段1")
  private String ext1;

  /** 扩展字段2 */
  @Column(name = "ext2")
  @Schema(description = "扩展字段2")
  private String ext2;

  /** 扩展字段3 */
  @Column(name = "ext3")
  @Schema(description = "扩展字段3")
  private String ext3;

  /** 扩展字段4 */
  @Column(name = "ext4")
  @Schema(description = "扩展字段4")
  private String ext4;

  /** CSQ信号强度 */
  @Column(name = "signal_strength")
  @Schema(description = "信号强度")
  private String signalStrength;

  /** 设备标签 */
  @Column(name = "device_tag")
  @ColumnType(jdbcType = JdbcType.OTHER, typeHandler = PostgreSqlJsonbStringTypeHandler.class)
  @Schema(description = "设备标签")
  private String deviceTag;

  /** 设备地址 */
  @Column(name = "device_address")
  @Schema(description = "设备地址")
  private String deviceAddress;

  /** 0-离线，1-在线 */
  @Excel(name = "在线状态")
  @Schema(description = "在线状态")
  private Integer state;

  /** 说明 */
  @Excel(name = "备注")
  @Schema(description = "详情")
  private String detail;

  @Column(name = "create_time")
  @Schema(description = "创建时间")
  private Long createTime;

  /** 派生元数据,有的设备的属性，功能，事件可能会动态的添加 */
  @Column(name = "derive_metadata")
  @Schema(description = "派生元数据")
  private String deriveMetadata;

  /** 其他配置 */
  @Column(name = "configuration")
  @Schema(description = "配置")
  @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = cn.universal.persistence.common.serialization.PictureSecretConfigurationSerializer.class)
  private String configuration;

  /** 区域ID */
  @Schema(description = "区域ID")
  private String areasId;

  /** 坐标 */
  // @Excel(name = "设备坐标")
  @Schema(description = "坐标")
  private String coordinate;

  @Transient
  @Schema(description = "设备节点")
  private String deviceNode;

  @Transient
  @Schema(description = "第三方平台")
  private String thirdPlatform;

  /** 产品配置（来自JOIN查询，包含kitToken等平台级配置） */
  @Transient
  @Schema(description = "产品配置")
  private String productConfiguration;

  @Transient
  @Schema(description = "是否待分配归属账号")
  private Boolean pendingOwner;

  /** 纬度 */
  @Excel(name = "纬度")
  @Schema(description = "纬度")
  private transient String latitude;

  /** 经度 */
  @Excel(name = "经度")
  @Schema(description = "经度")
  private transient String longitude;

  /** 请求参数 */
  @Builder.Default
  @Schema(description = "请求参数")
  private Map<String, Object> params = new HashMap<>();

  /** 接收额外参数 */
  @Excel(name = "其他配置")
  @Builder.Default
  @Schema(description = "其他参数")
  private Map<String, Object> otherParams = new HashMap<>();

  @JsonSetter("state")
  public void setStateFromBoolean(Boolean state) {
    this.state = toStateValue(state);
  }

  public boolean isOnline() {
    return isOnlineState(state);
  }

  public static Integer toStateValue(Boolean state) {
    if (state == null) {
      return null;
    }
    return state ? STATE_ONLINE : STATE_OFFLINE;
  }

  public static boolean isOnlineState(Integer state) {
    return state != null && state == STATE_ONLINE;
  }

  public static class IoTDeviceBuilder {
    public IoTDeviceBuilder state(Boolean state) {
      this.state = IoTDevice.toStateValue(state);
      return this;
    }
  }
}
