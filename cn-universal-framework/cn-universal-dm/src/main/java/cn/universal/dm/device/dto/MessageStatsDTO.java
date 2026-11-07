package cn.universal.dm.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息统计数据DTO
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2025/1/30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "消息统计数据")
public class MessageStatsDTO {

  @Schema(description = "消息总数")
  private Long totalMessages;

  @Schema(description = "成功消息数")
  private Long successMessages;

  @Schema(description = "失败消息数")
  private Long failedMessages;

  @Schema(description = "推送消息数")
  private Long pushMessages;

  @Schema(description = "重试消息数")
  private Long retryMessages;

  @Schema(description = "成功率(%)")
  private Double successRate;

  @Schema(description = "平均每小时消息数")
  private Double avgPerHour;
}

