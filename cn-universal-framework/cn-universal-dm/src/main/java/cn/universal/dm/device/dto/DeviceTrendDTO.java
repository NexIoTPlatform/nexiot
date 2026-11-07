package cn.universal.dm.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备趋势数据DTO
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2025/1/30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "设备趋势数据")
public class DeviceTrendDTO {

  @Schema(description = "时间标签列表")
  private List<String> labels;

  @Schema(description = "在线设备数列表")
  private List<Long> online;

  @Schema(description = "离线设备数列表")
  private List<Long> offline;
}

