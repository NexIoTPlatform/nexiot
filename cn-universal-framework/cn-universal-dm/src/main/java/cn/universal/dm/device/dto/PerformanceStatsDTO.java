package cn.universal.dm.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 性能统计数据DTO
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2025/1/30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "性能统计数据")
public class PerformanceStatsDTO {

  @Schema(description = "平均响应时间(ms)")
  private Double avgResponseTime;

  @Schema(description = "消息处理速度(消息/秒)")
  private Double messageProcessingSpeed;

  @Schema(description = "系统吞吐量(MB/s)")
  private Double systemThroughput;

  @Schema(description = "连接稳定性(%)")
  private Double connectionStability;
}

