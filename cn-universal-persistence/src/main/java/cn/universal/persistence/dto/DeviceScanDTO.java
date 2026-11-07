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
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/7/14
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "设备扫描DTO")
public class DeviceScanDTO {

  @Schema(description = "产品Key")
  private String productKey;

  @Schema(description = "二维码")
  private String qrcode;

  public boolean isEmpty() {
    return StrUtil.isEmpty(getProductKey()) || StrUtil.isEmpty(getQrcode());
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @Schema(description = "扫描结果DTO")
  public static class ResultScanDTO {

    @Schema(description = "IMEI")
    private String imei;

    @Schema(description = "设备ID")
    private String deviceId;
  }
}
