package cn.universal.admin.platform.service.impl;

import cn.universal.admin.platform.service.IIoTProductService;
import cn.universal.admin.platform.service.IProductOperationService;
import cn.universal.common.exception.IoTException;
import cn.universal.core.product.operation.ProductOperationContext;
import cn.universal.core.product.operation.ProductOperationDefinition;
import cn.universal.core.product.operation.ProductOperationProvider;
import cn.universal.core.product.operation.ProductOperationRegistry;
import cn.universal.core.product.operation.ProductOperationRequest;
import cn.universal.core.product.operation.ProductOperationResult;
import cn.universal.persistence.entity.vo.IoTProductVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/** 产品级操作服务实现。 */
@Service
@RequiredArgsConstructor
public class ProductOperationServiceImpl implements IProductOperationService {

  private final IIoTProductService productService;

  @Override
  public List<ProductOperationDefinition> listOperations(
      Long productId, String operatorId, boolean admin) {
    ProductOperationContext context = loadContext(productId, operatorId, admin);
    return ProductOperationRegistry.getOperations(context);
  }

  @Override
  public ProductOperationResult execute(
      Long productId, ProductOperationRequest request, String operatorId, boolean admin) {
    if (request == null || StringUtils.isBlank(request.getOperationCode())) {
      throw new IoTException("产品级操作编码不能为空");
    }

    ProductOperationContext context = loadContext(productId, operatorId, admin);
    ProductOperationProvider provider =
        ProductOperationRegistry.getProvider(context, request.getOperationCode());
    if (provider == null) {
      throw new IoTException("未找到对应的产品级操作: " + request.getOperationCode());
    }

    if (StringUtils.isBlank(request.getOperatorId())) {
      request.setOperatorId(operatorId);
    }
    return provider.execute(context, request);
  }

  private ProductOperationContext loadContext(Long productId, String operatorId, boolean admin) {
    if (productId == null) {
      throw new IoTException("产品ID不能为空");
    }

    IoTProductVO product = productService.selectDevProductById(String.valueOf(productId));
    if (product == null || product.getId() == null) {
      throw new IoTException("产品不存在: " + productId);
    }

    return ProductOperationContext.builder()
        .productId(product.getId())
        .productKey(product.getProductKey())
        .name(product.getName())
        .thirdPlatform(product.getThirdPlatform())
        .configuration(product.getConfiguration())
        .creatorId(product.getCreatorId())
        .deviceNode(product.getDeviceNode())
        .build();
  }
}
