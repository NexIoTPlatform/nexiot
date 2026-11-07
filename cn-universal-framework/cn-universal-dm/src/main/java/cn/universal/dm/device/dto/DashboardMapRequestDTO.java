package cn.universal.dm.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仪表盘地图查询请求 DTO
 *
 * @author gitee.com/NexIoT
 * @since 2026/03/28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "仪表盘地图查询请求")
public class DashboardMapRequestDTO {

  private static final int DEFAULT_LIMIT = 12;
  private static final int MAX_LIMIT = 180;

  @Schema(description = "是否使用预览模式")
  private Boolean preview;

  @Schema(description = "关键词")
  private String keyword;

  @Schema(description = "产品Key")
  private String productKey;

  @Schema(description = "在线状态过滤 0-离线 1-在线")
  private Integer state;

  @Schema(description = "地图缩放等级")
  private Integer zoom;

  @Schema(description = "左下经度")
  private Double westLng;

  @Schema(description = "左下纬度")
  private Double southLat;

  @Schema(description = "右上经度")
  private Double eastLng;

  @Schema(description = "右上纬度")
  private Double northLat;

  @Schema(description = "返回数量限制")
  private Integer limit;

  public int resolveLimit() {
    if (limit == null || limit <= 0) {
      return DEFAULT_LIMIT;
    }
    return Math.min(limit, MAX_LIMIT);
  }

  public boolean hasBounds() {
    return westLng != null && southLat != null && eastLng != null && northLat != null;
  }
}
