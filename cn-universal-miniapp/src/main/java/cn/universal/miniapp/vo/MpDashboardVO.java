/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小程序仪表盘数据视图对象
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "小程序仪表盘数据")
public class MpDashboardVO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "设备总数")
  private Integer totalDevices;

  @Schema(description = "在线设备数")
  private Integer onlineDevices;

  @Schema(description = "离线设备数")
  private Integer offlineDevices;

  @Schema(description = "告警数")
  private Integer alarmCount;

  @Schema(description = "用户总数")
  private Integer totalUsers;

  @Schema(description = "今日活跃用户数")
  private Integer todayActiveUsers;
}
