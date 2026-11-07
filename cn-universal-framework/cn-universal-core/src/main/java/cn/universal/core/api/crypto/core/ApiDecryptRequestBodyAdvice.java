package cn.universal.core.api.crypto.core;

import cn.universal.core.api.crypto.annotation.decrypt.ApiDecrypt;
import cn.universal.core.api.crypto.bean.DecryptHttpInputMessage;
import cn.universal.core.api.crypto.enums.CryptoType;
import cn.universal.core.api.crypto.util.ApiCryptoAesUtil;
import cn.universal.core.api.crypto.util.ApiCryptoAnnotationUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

/** 对带有 {@link ApiDecrypt} 的 {@code @RequestBody} 请求体进行解密。 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class ApiDecryptRequestBodyAdvice implements RequestBodyAdvice {

  private final ApiCryptoKeyProvider keyProvider;

  @Override
  public boolean supports(
      MethodParameter methodParameter,
      @NonNull Type targetType,
      @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
    return keyProvider.isEnabled() && ApiCryptoAnnotationUtils.hasDecryptAnnotation(methodParameter);
  }

  @Override
  public Object handleEmptyBody(
      Object body,
      @NonNull HttpInputMessage inputMessage,
      @NonNull MethodParameter parameter,
      @NonNull Type targetType,
      @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
    return body;
  }

  @NonNull
  @Override
  public HttpInputMessage beforeBodyRead(
      HttpInputMessage inputMessage,
      @NonNull MethodParameter parameter,
      @NonNull Type targetType,
      @NonNull Class<? extends HttpMessageConverter<?>> converterType)
      throws IOException {
    byte[] encryptedBody = StreamUtils.copyToByteArray(inputMessage.getBody());
    if (encryptedBody.length == 0) {
      return inputMessage;
    }
    ApiDecrypt apiDecrypt = ApiCryptoAnnotationUtils.findDecryptAnnotation(parameter);
    if (apiDecrypt == null) {
      return inputMessage;
    }
    if (apiDecrypt.value() != CryptoType.AES) {
      throw new HttpMessageNotReadableException("Unsupported API decrypt type", inputMessage);
    }
    try {
      String aesKey = keyProvider.resolveAesKey(apiDecrypt.secretKey());
      byte[] decryptedBody = ApiCryptoAesUtil.decryptFromBase64(encryptedBody, aesKey);
      return new DecryptHttpInputMessage(
          new ByteArrayInputStream(decryptedBody), inputMessage.getHeaders());
    } catch (Exception e) {
      log.warn("API AES request body decrypt failed: {}", e.getMessage());
      throw new HttpMessageNotReadableException("API AES request body decrypt failed", e, inputMessage);
    }
  }

  @NonNull
  @Override
  public Object afterBodyRead(
      @NonNull Object body,
      @NonNull HttpInputMessage inputMessage,
      @NonNull MethodParameter parameter,
      @NonNull Type targetType,
      @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
    return body;
  }
}
