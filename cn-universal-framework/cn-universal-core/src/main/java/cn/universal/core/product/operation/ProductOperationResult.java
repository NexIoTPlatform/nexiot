package cn.universal.core.product.operation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 产品级操作执行结果。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOperationResult {

  private boolean success;
  private String code;
  private String message;
  private Object data;

  public static ProductOperationResult success(String message, Object data) {
    return ProductOperationResult.builder()
        .success(true)
        .code("0")
        .message(message)
        .data(data)
        .build();
  }

  public static ProductOperationResult failure(String code, String message, Object data) {
    return ProductOperationResult.builder()
        .success(false)
        .code(code)
        .message(message)
        .data(data)
        .build();
  }
}
