package nexiot.web.ide.debug.plugins.starter;

import nexiot.web.ide.debug.plugins.core.config.MagicAPIProperties;
import nexiot.web.ide.debug.plugins.core.service.impl.ApiInfoMagicResourceStorage;
import nexiot.web.ide.debug.plugins.core.service.impl.RequestMagicDynamicRegistry;
import nexiot.web.ide.debug.plugins.datasource.model.MagicDynamicDataSource;
import nexiot.web.ide.debug.plugins.datasource.service.DataSourceInfoMagicResourceStorage;
import nexiot.web.ide.debug.plugins.datasource.service.DataSourceMagicDynamicRegistry;
import nexiot.web.ide.debug.plugins.function.service.FunctionInfoMagicResourceStorage;
import nexiot.web.ide.debug.plugins.function.service.FunctionMagicDynamicRegistry;
import nexiot.web.ide.debug.plugins.utils.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
@AutoConfigureAfter(MagicModuleConfiguration.class)
public class MagicDynamicRegistryConfiguration {

  private final MagicAPIProperties properties;

  @Autowired @Lazy private RequestMappingHandlerMapping requestMappingHandlerMapping;

  public MagicDynamicRegistryConfiguration(MagicAPIProperties properties) {
    this.properties = properties;
  }

  @Bean
  @ConditionalOnMissingBean
  public ApiInfoMagicResourceStorage apiInfoMagicResourceStorage() {
    return new ApiInfoMagicResourceStorage(properties.getPrefix());
  }

  @Bean
  @ConditionalOnMissingBean
  public RequestMagicDynamicRegistry magicRequestMagicDynamicRegistry(
      ApiInfoMagicResourceStorage apiInfoMagicResourceStorage) throws NoSuchMethodException {
    return new RequestMagicDynamicRegistry(
        apiInfoMagicResourceStorage,
        Mapping.create(requestMappingHandlerMapping, properties.getWeb()),
        properties.isAllowOverride(),
        properties.getPrefix());
  }

  @Bean
  @ConditionalOnMissingBean
  public FunctionInfoMagicResourceStorage functionInfoMagicResourceStorage() {
    return new FunctionInfoMagicResourceStorage();
  }

  @Bean
  @ConditionalOnMissingBean
  public FunctionMagicDynamicRegistry functionMagicDynamicRegistry(
      FunctionInfoMagicResourceStorage functionInfoMagicResourceStorage) {
    return new FunctionMagicDynamicRegistry(functionInfoMagicResourceStorage);
  }

  @Bean
  @ConditionalOnMissingBean
  public DataSourceInfoMagicResourceStorage dataSourceInfoMagicResourceStorage() {
    return new DataSourceInfoMagicResourceStorage();
  }

  @Bean
  @ConditionalOnMissingBean
  public DataSourceMagicDynamicRegistry dataSourceMagicDynamicRegistry(
      DataSourceInfoMagicResourceStorage dataSourceInfoMagicResourceStorage,
      MagicDynamicDataSource magicDynamicDataSource) {
    return new DataSourceMagicDynamicRegistry(
        dataSourceInfoMagicResourceStorage, magicDynamicDataSource);
  }
}
