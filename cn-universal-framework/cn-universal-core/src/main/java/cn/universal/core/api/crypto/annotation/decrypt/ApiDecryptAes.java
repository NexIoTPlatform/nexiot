package cn.universal.core.api.crypto.annotation.decrypt;

import cn.universal.core.api.crypto.enums.CryptoType;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;

/** AES 请求报文解密。 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ApiDecrypt(CryptoType.AES)
public @interface ApiDecryptAes {

  /** Alias for {@link ApiDecrypt#secretKey()}. */
  @AliasFor(annotation = ApiDecrypt.class, attribute = "secretKey")
  String secretKey() default "";
}
