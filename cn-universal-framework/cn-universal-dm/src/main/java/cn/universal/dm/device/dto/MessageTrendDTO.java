package cn.universal.dm.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息趋势数据DTO
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2025/1/30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "消息趋势数据")
public class MessageTrendDTO {

  @Schema(description = "时间标签列表")
  private List<String> labels;

  @Schema(description = "消息总数列表（所有渠道总计）")
  private List<Long> total;

  @Schema(description = "成功消息数列表（所有渠道总计）")
  private List<Long> success;

  @Schema(description = "按渠道分组的消息数据，key为渠道名称(如MQTT、TCP等)，value为该渠道每天的消息总数列表")
  private Map<String, List<Long>> channelData;
}


