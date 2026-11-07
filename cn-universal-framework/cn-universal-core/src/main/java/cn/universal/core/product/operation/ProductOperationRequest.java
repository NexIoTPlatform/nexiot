package cn.universal.core.product.operation;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 产品级操作执行请求。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOperationRequest {

  private String operationCode;
  private Boolean dryRun;
  private String operatorId;

  @Builder.Default private Map<String, Object> params = new LinkedHashMap<>();
}
