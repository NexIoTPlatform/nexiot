package cn.universal.core.product.operation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/** 产品级操作注册器。 */
@Component
@Slf4j
public class ProductOperationRegistry implements ApplicationContextAware {

  private static final List<ProductOperationProvider> PROVIDERS = new CopyOnWriteArrayList<>();

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    Map<String, ProductOperationProvider> providerBeans =
        applicationContext.getBeansOfType(ProductOperationProvider.class);
    List<ProductOperationProvider> providers =
        providerBeans.values().stream()
            .sorted(Comparator.comparingInt(ProductOperationProvider::getOrder))
            .toList();

    PROVIDERS.clear();
    PROVIDERS.addAll(providers);

    log.info(
        "[产品级操作注册] 完成注册，共 {} 个提供者: {}",
        PROVIDERS.size(),
        PROVIDERS.stream().map(ProductOperationProvider::getProtocolCode).distinct().toList());
  }

  public static List<ProductOperationProvider> getProviders(ProductOperationContext context) {
    if (context == null) {
      return Collections.emptyList();
    }
    return PROVIDERS.stream().filter(provider -> provider.supports(context)).toList();
  }

  public static List<ProductOperationDefinition> getOperations(ProductOperationContext context) {
    List<ProductOperationDefinition> definitions = new ArrayList<>();
    for (ProductOperationProvider provider : getProviders(context)) {
      List<ProductOperationDefinition> current = provider.getOperations(context);
      if (current != null && !current.isEmpty()) {
        definitions.addAll(current.stream().filter(Objects::nonNull).toList());
      }
    }
    return definitions;
  }

  public static ProductOperationProvider getProvider(
      ProductOperationContext context, String operationCode) {
    if (StringUtils.isBlank(operationCode)) {
      return null;
    }
    for (ProductOperationProvider provider : getProviders(context)) {
      List<ProductOperationDefinition> definitions = provider.getOperations(context);
      if (definitions == null || definitions.isEmpty()) {
        continue;
      }
      boolean matched =
          definitions.stream()
              .filter(Objects::nonNull)
              .anyMatch(
                  definition ->
                      StringUtils.equalsIgnoreCase(definition.getCode(), operationCode));
      if (matched) {
        return provider;
      }
    }
    return null;
  }
}
