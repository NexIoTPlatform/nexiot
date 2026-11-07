package cn.universal.dm.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 渠道统计数据DTO
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2025/1/30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "渠道统计数据")
public class ChannelStatsDTO {

  @Schema(description = "各渠道消息数量，key为渠道名称，value为消息数量")
  private Map<String, Long> channels;
}

