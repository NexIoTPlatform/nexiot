package cn.universal.persistence.annotation;

import cn.universal.persistence.serializer.SensitiveFieldSerializer;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 敏感字段注解 用于标记需要加密显示的字段 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveFieldSerializer.class)
public @interface SensitiveField {

  /** 是否完全隐藏（显示为***） */
  boolean hide() default false;

  /** 显示前几位字符 */
  int showPrefix() default 0;

  /** 显示后几位字符 */
  int showSuffix() default 0;
}
