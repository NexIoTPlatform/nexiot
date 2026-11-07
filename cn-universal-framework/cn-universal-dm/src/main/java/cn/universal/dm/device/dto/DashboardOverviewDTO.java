package cn.universal.dm.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仪表盘概览数据DTO
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2025/1/30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "仪表盘概览数据")
public class DashboardOverviewDTO {

  @Schema(description = "设备统计")
  private DeviceStatsDTO deviceStats;

  @Schema(description = "应用统计")
  private ApplicationStatsDTO applicationStats;

  @Schema(description = "性能统计")
  private PerformanceStatsDTO performanceStats;

  @Schema(description = "渠道统计")
  private ChannelStatsDTO channelStats;

  @Schema(description = "产品统计")
  private ProductStatsDTO productStats;

  @Schema(description = "时间戳")
  private Long timestamp;
}


