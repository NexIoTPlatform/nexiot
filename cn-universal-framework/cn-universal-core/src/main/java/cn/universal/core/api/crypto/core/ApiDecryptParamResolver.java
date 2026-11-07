package cn.universal.core.api.crypto.core;

import cn.universal.core.api.crypto.annotation.decrypt.ApiDecrypt;
import cn.universal.core.api.crypto.config.ApiCryptoProperties;
import cn.universal.core.api.crypto.enums.CryptoType;
import cn.universal.core.api.crypto.util.ApiCryptoAesUtil;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** 参数级 API 密文解析，默认读取请求参数 {@code data}。 */
@RequiredArgsConstructor
public class ApiDecryptParamResolver implements HandlerMethodArgumentResolver {

  private final ApiCryptoProperties properties;
  private final ApiCryptoKeyProvider keyProvider;
  private final ObjectMapper objectMapper;

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return keyProvider.isEnabled()
        && !parameter.hasParameterAnnotation(RequestBody.class)
        && AnnotatedElementUtils.hasAnnotation(parameter.getParameter(), ApiDecrypt.class);
  }

  @Nullable
  @Override
  public Object resolveArgument(
      MethodParameter methodParameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory)
      throws Exception {
    ApiDecrypt apiDecrypt =
        AnnotatedElementUtils.findMergedAnnotation(methodParameter.getParameter(), ApiDecrypt.class);
    String text = webRequest.getParameter(properties.getParamName());
    if (apiDecrypt == null || !StringUtils.hasText(text)) {
      return null;
    }
    if (apiDecrypt.value() != CryptoType.AES) {
      throw new IllegalArgumentException("Unsupported API decrypt type");
    }
    String aesKey = keyProvider.resolveAesKey(apiDecrypt.secretKey());
    byte[] decryptData =
        ApiCryptoAesUtil.decryptFromBase64(text.getBytes(StandardCharsets.UTF_8), aesKey);
    JavaType javaType =
        objectMapper.getTypeFactory().constructType(methodParameter.getGenericParameterType());
    return objectMapper.readValue(decryptData, javaType);
  }
}
