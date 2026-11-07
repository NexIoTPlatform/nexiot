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

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/9/22
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "IoT设备元数据业务对象")
public class IoTDeviceMetadataBO implements Serializable {

  @Schema(description = "公司编号")
  private String companyNo;

  @Schema(description = "创建者ID")
  private String creatorId;

  @Schema(description = "组织ID")
  private String orgId;

  @Schema(description = "分类ID")
  private String classifiedId;

  @Schema(description = "传输协议")
  private String transportProtocol;

  @Schema(description = "点位信息")
  private String point;
}
