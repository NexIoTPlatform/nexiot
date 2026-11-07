package cn.universal.core.product.operation;

import java.util.List;
import org.apache.commons.lang3.StringUtils;

/** 产品级操作提供者接口。 */
public interface ProductOperationProvider {

  /** 当前提供者绑定的协议编码。 */
  String getProtocolCode();

  /** 多个提供者同时存在时的排序。 */
  default int getOrder() {
    return 0;
  }

  /** 是否支持当前产品。 */
  default boolean supports(ProductOperationContext context) {
    return context != null
        && StringUtils.isNotBlank(context.getThirdPlatform())
        && StringUtils.equalsIgnoreCase(getProtocolCode(), context.getThirdPlatform());
  }

  /** 返回当前产品可用的产品级操作定义。 */
  List<ProductOperationDefinition> getOperations(ProductOperationContext context);

  /** 执行产品级操作。 */
  ProductOperationResult execute(
      ProductOperationContext context, ProductOperationRequest request);
}
