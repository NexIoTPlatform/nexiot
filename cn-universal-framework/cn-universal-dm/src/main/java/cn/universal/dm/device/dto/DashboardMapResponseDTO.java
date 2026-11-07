package cn.universal.dm.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仪表盘地图响应 DTO
 *
 * @author gitee.com/NexIoT
 * @since 2026/03/28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "仪表盘地图响应")
public class DashboardMapResponseDTO {

  @Schema(description = "是否为预览模式")
  private Boolean previewMode;

  @Schema(description = "当前查询是否还有更多数据")
  private Boolean hasMore;

  @Schema(description = "当前返回数量限制")
  private Integer limit;

  @Schema(description = "设备列表")
  private List<DashboardMapDeviceDTO> list;

  @Schema(description = "地图摘要")
  private DashboardMapSummaryDTO summary;

  @Schema(description = "时间戳")
  private Long timestamp;
}
