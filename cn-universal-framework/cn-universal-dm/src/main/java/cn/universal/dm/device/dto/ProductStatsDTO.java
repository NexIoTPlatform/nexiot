package cn.universal.dm.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 产品统计数据DTO
 *
 * @author gitee.com/NexIoT
 * @version 2.0
 * @since 2025/1/30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "产品统计数据")
public class ProductStatsDTO {

  @Schema(description = "产品总数")
  private Long totalProducts;

  @Schema(description = "我创建的产品数量")
  private Long myProducts;

  @Schema(description = "公共产品数量（总数 - 我的）")
  private Long publicProducts;
}
