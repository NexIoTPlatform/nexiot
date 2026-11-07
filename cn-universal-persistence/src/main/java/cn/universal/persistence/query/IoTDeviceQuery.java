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

import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/1/20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "IoT设备查询条件")
public class IoTDeviceQuery {

  @Schema(description = "IoT ID")
  private String iotId;

  @Schema(description = "扩展设备ID")
  private String extDeviceId;

  @Schema(description = "设备ID")
  private String deviceId;

  @Schema(description = "网关产品Key")
  private String gwProductKey;

  @Schema(description = "产品Key")
  private String productKey;

  @Schema(description = "第三方平台")
  private String thirdPlatform;

  public boolean emptyParams() {
    return StrUtil.isBlank(getIotId())
        && StrUtil.isBlank(getExtDeviceId())
        && StrUtil.isBlank(getDeviceId())
        && StrUtil.isBlank(getGwProductKey())
        && StrUtil.isBlank(productKey)
        && StrUtil.isBlank(thirdPlatform);
  }
}
