package cn.universal.dm.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 渠道分布项DTO
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2025/1/30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "渠道分布项")
public class ChannelDistributionDTO {

  @Schema(description = "渠道名称")
  private String channel;

  @Schema(description = "消息数量")
  private Long count;
}

