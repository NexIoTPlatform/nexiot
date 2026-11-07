/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 视频平台设备查询条件
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.persistence.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 视频平台设备查询条件 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
@Schema(description = "视频平台设备查询条件")
public class VideoPlatformDeviceQuery extends BasePage {

  @Schema(description = "平台实例Key")
  private String instanceKey;

  /**
   * 设备所属租户。由服务端根据可访问的平台实例设置，禁止直接使用客户端传入值。
   */
  @Schema(description = "设备所属租户", hidden = true)
  private String tenantId;

  @Schema(description = "关键词（设备名称或设备ID）")
  private String keyword;

  @Schema(description = "设备状态")
  private String status;

  @Schema(description = "组织ID")
  private String orgId;

  @Schema(description = "是否强制同步")
  private Boolean forceSync;

  @Schema(description = "是否启用")
  private Integer enabled;
}
