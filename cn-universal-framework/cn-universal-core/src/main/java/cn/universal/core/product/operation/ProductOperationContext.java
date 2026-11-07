package cn.universal.core.product.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 产品级操作上下文。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOperationContext {

  private Long productId;
  private String productKey;
  private String name;
  private String thirdPlatform;
  private String configuration;
  private String creatorId;
  private String deviceNode;
}
