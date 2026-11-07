package cn.universal.core.product.operation;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 产品级操作定义。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOperationDefinition {

  private String code;
  private String name;
  private String description;
  private String group;
  private String groupName;
  private String style;
  private boolean enabled;
  private String disabledReason;
  private String confirmText;

  @Builder.Default private List<ProductOperationField> fields = new ArrayList<>();
}
