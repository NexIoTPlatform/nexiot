package cn.universal.core.api.crypto.config;

import cn.universal.core.api.crypto.core.ApiCryptoKeyProvider;
import cn.universal.core.api.crypto.core.ApiDecryptParamResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** API 报文加解密 Web MVC 配置。 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(ApiCryptoProperties.class)
public class ApiCryptoConfiguration implements WebMvcConfigurer {

  private final ApiCryptoProperties properties;
  private final ApiCryptoKeyProvider keyProvider;
  private final ObjectMapper objectMapper;

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new ApiDecryptParamResolver(properties, keyProvider, objectMapper));
  }

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    if (!Boolean.TRUE.equals(properties.getSupportTextPlain())) {
      return;
    }
    for (HttpMessageConverter<?> converter : converters) {
      if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
        List<MediaType> mediaTypes = new ArrayList<>(jacksonConverter.getSupportedMediaTypes());
        if (!mediaTypes.contains(MediaType.TEXT_PLAIN)) {
          mediaTypes.add(MediaType.TEXT_PLAIN);
          jacksonConverter.setSupportedMediaTypes(mediaTypes);
        }
      }
    }
  }
}
