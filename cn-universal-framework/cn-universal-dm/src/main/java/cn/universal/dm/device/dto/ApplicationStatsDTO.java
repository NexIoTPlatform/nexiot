package cn.universal.dm.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 应用统计数据DTO
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2025/1/30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "应用统计数据")
public class ApplicationStatsDTO {

  @Schema(description = "应用总数（管理员查全部，普通用户查自己的）")
  private Long totalApplications;
}
