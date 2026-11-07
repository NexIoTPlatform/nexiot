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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Schema(description = "IoT API查询条件")
public class IoTAPIQuery extends BasePage {

  @Schema(name = "extDeviceId", description = "网关设备deviceId、第三方平台iotId")
  private String extDeviceId;

  @Schema(name = "applicationId", description = "应用主键Id")
  private String applicationId;

  @Schema(name = "iotUnionId", description = "设备归属者，一般是createId")
  private String iotUnionId;

  @Schema(name = "iotId", description = "productKey+deviceId")
  private String iotId;

  @Schema(name = "productName", description = "产品名称")
  private String productName;

  @Schema(description = "产品Key")
  private String productKey;

  @Schema(description = "设备名称")
  private String deviceName;

  @Schema(description = "设备ID")
  private String deviceId;

  /** 传输协议 */
  @Schema(description = "传输协议")
  private String transportProtocol;

  @Schema(description = "设备节点")
  private String deviceNode;

  @Schema(description = "公司编号")
  private String companyNo;

  /** 产品型号；例如 WS101-470M */
  @Schema(description = "产品型号")
  private String productId;

  /** 经度 */
  @Schema(description = "经度")
  private String longitude;

  /** 维度 */
  @Schema(description = "纬度")
  private String latitude;

  @Schema(description = "网关产品Key")
  private String gwProductKey;

  /** 备注说明 */
  @Schema(description = "备注说明")
  private String detail;
}
