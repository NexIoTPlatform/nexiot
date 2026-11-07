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

import cn.universal.persistence.entity.IoTProduct;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/11/16
 */
@Data
@Schema(description = "IoT产品VO")
public class IoTProductVO extends IoTProduct {

  @Schema(description = "产品图片")
  private String image;

  @Schema(description = "功耗模式")
  private int powerModel;

  @Schema(description = "LwM2M eDRX时间")
  private String lwm2mEdrxTime;

  @Schema(description = "设备数量")
  private int devNum;

  @Schema(description = "存储策略")
  private String storePolicy;

  @Schema(description = "产品类型")
  private String type;

  @Schema(description = "网关名称")
  private String gwName;

  @Schema(description = "网关图片URL")
  private String gwPhotoUrl;
}
