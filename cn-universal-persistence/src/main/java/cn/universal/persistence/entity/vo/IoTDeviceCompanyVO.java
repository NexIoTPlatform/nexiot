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
import java.util.List;
import lombok.Data;

/**
 * 公司视图对象 @Author gitee.com/NexIoT
 *
 * @since 2023/1/5 11:35
 */
@Data
@Schema(description = "公司视图对象")
public class IoTDeviceCompanyVO {

  private static final long serialVersionUID = 1L;

  /** 厂家编号 */
  @Schema(description = "厂家编号")
  private String companyNo;

  /** 厂家名称 */
  @Schema(description = "厂家名称")
  private String companyName;

  @Schema(description = "设备类型")
  private List<IoTDeviceTypeVO> devices;
}
