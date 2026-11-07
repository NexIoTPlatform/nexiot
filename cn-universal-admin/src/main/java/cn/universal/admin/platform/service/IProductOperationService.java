package cn.universal.admin.platform.service;

import cn.universal.core.product.operation.ProductOperationDefinition;
import cn.universal.core.product.operation.ProductOperationRequest;
import cn.universal.core.product.operation.ProductOperationResult;
import java.util.List;

/** 产品级操作服务。 */
public interface IProductOperationService {

  List<ProductOperationDefinition> listOperations(Long productId, String operatorId, boolean admin);

  ProductOperationResult execute(
      Long productId, ProductOperationRequest request, String operatorId, boolean admin);
}
