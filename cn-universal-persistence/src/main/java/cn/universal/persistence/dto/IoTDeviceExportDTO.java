/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权,未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.persistence.dto;

import cn.universal.common.annotation.Excel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** IoT设备导出DTO 包含产品和设备的关键字段 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "IoT设备导出数据传输对象")
public class IoTDeviceExportDTO {

  /** 产品名称 */
  @Excel(name = "产品名称", sort = 1)
  @Schema(description = "产品名称")
  private String productName;

  /** 所属产品/ProductKey */
  @Excel(name = "所属产品/ProductKey", sort = 2)
  @Schema(description = "产品Key")
  private String productKey;

  /** 接入方式 */
  @Excel(
      name = "接入方式",
      sort = 3,
      readConverterExp =
          "mqtt=MQTT,http=HTTP,tcp=TCP直连,wvp=WVP国标28181,imoulife=乐橙云,ctaiot=天翼物联,udp=UDP,icc=大华智慧物联综合管理平台(ICC),isc=海康威视")
  @Schema(description = "第三方平台/接入方式")
  private String thirdPlatform;

  /** 设备类型 */
  @Excel(
      name = "设备类型",
      sort = 4,
      readConverterExp = "GATEWAY_SUB_DEVICE=子设备,GATEWAY=网关设备,DEVICE=直连设备,VIDEO_DEVICE=视频监控")
  @Schema(description = "设备节点类型")
  private String deviceNode;

  /** 存储策略 */
  @Excel(
      name = "存储策略",
      sort = 5,
      readConverterExp =
          "none=不存,mysql=MySQL,tdengine=TDengine,influxdb=InfluxDB,iotdb=IoTDB,clickhouse=clickhouse")
  @Schema(description = "存储策略")
  private String storePolicy;

  /** 设备名称 */
  @Excel(name = "设备名称", sort = 6)
  @Schema(description = "设备名称")
  private String deviceName;

  /** 设备序列号 */
  @Excel(name = "设备序列号", sort = 7)
  @Schema(description = "设备ID")
  private String deviceId;

  /** IoT ID */
  @Excel(name = "IoT ID", sort = 8)
  @Schema(description = "IoT唯一标识")
  private String iotId;

  /** 在线状态 */
  @Excel(name = "在线状态", sort = 9, readConverterExp = "true=在线,false=离线")
  @Schema(description = "在线状态")
  private Boolean state;

  /** 设备地址 */
  @Excel(name = "设备地址", sort = 10)
  @Schema(description = "设备地址")
  private String deviceAddress;

  /** 经度 */
  @Excel(name = "经度", sort = 11)
  @Schema(description = "经度")
  private String longitude;

  /** 纬度 */
  @Excel(name = "纬度", sort = 12)
  @Schema(description = "纬度")
  private String latitude;

  /** 信号强度 */
  @Excel(name = "信号强度", sort = 13)
  @Schema(description = "CSQ信号强度")
  private String signalStrength;

  /** 设备标签 */
  @Excel(name = "设备标签", sort = 14)
  @Schema(description = "设备标签")
  private String deviceTag;

  /** 创建时间 */
  @Excel(name = "创建时间", sort = 15, dateFormat = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private LocalDateTime createTime;

  /** 注册时间 */
  @Excel(name = "注册时间", sort = 16, dateFormat = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "注册时间")
  private LocalDateTime registryTime;

  /** 最后通信时间 */
  @Excel(name = "最后通信时间", sort = 17, dateFormat = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "最后通信时间")
  private LocalDateTime onlineTime;

  /** 备注 */
  @Excel(name = "备注", sort = 18)
  @Schema(description = "详细说明")
  private String detail;
}
