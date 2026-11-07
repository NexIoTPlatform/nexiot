package cn.universal.core.product.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 产品级操作字段选项。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOperationOption {

  private String label;
  private String value;
}
