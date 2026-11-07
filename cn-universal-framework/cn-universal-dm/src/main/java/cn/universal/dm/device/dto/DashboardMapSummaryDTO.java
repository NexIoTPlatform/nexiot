package cn.universal.dm.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仪表盘地图摘要 DTO
 *
 * @author gitee.com/NexIoT
 * @since 2026/03/28
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "仪表盘地图摘要")
public class DashboardMapSummaryDTO {

  @Schema(description = "设备总数")
  private Long totalDevices;

  @Schema(description = "在线设备数")
  private Long onlineDevices;

  @Schema(description = "已定位设备数")
  private Long locatedDevices;

  @Schema(description = "在线且已定位设备数")
  private Long onlineLocatedDevices;

  @Schema(description = "未定位设备数")
  private Long unlocatedDevices;

  @Schema(description = "当前查询命中设备数")
  private Long matchedDevices;
}
