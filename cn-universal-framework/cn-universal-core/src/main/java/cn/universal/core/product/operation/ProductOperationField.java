package cn.universal.core.product.operation;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 产品级操作动态表单字段定义。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOperationField {

  private String key;
  private String label;
  private String type;
  private boolean required;
  private boolean advanced;
  private Object defaultValue;
  private String placeholder;
  private String description;

  @Builder.Default private List<ProductOperationOption> options = new ArrayList<>();
}
