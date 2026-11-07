package cn.universal.core.api.crypto.annotation.decrypt;

import cn.universal.core.api.crypto.enums.CryptoType;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 解密带有 {@code @RequestBody} 的请求体，或解密参数 {@code data} 中的密文。 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ApiDecrypt {

  /** 解密类型。 */
  CryptoType value() default CryptoType.AES;

  /** 单接口密钥；为空时读取全局配置。 */
  String secretKey() default "";
}
