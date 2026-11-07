package cn.universal.core.api.crypto.util;

import cn.universal.core.api.crypto.annotation.decrypt.ApiDecrypt;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;

/** API 加解密注解解析工具。 */
public final class ApiCryptoAnnotationUtils {

  private ApiCryptoAnnotationUtils() {}

  @Nullable
  public static ApiDecrypt findDecryptAnnotation(MethodParameter parameter) {
    ApiDecrypt annotation =
        AnnotatedElementUtils.findMergedAnnotation(parameter.getParameter(), ApiDecrypt.class);
    if (annotation != null) {
      return annotation;
    }
    if (parameter.getMethod() != null) {
      annotation = AnnotatedElementUtils.findMergedAnnotation(parameter.getMethod(), ApiDecrypt.class);
      if (annotation != null) {
        return annotation;
      }
    }
    return AnnotatedElementUtils.findMergedAnnotation(
        parameter.getContainingClass(), ApiDecrypt.class);
  }

  public static boolean hasDecryptAnnotation(MethodParameter parameter) {
    return findDecryptAnnotation(parameter) != null;
  }
}
