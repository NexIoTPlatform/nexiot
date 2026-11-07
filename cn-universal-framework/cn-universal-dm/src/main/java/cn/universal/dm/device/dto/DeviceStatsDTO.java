package cn.universal.dm.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备统计数据DTO
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2025/1/30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "设备统计数据")
public class DeviceStatsDTO {

  @Schema(description = "设备总数")
  private Long totalDevices;

  @Schema(description = "在线设备数")
  private Long onlineDevices;

  @Schema(description = "离线设备数")
  private Long offlineDevices;

  @Schema(description = "在线率(%)")
  private Double onlineRate;
}

