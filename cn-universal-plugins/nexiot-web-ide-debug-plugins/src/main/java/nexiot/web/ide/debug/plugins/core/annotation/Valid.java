package nexiot.web.ide.debug.plugins.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import nexiot.web.ide.debug.plugins.core.interceptor.Authorization;

/**
 * 接口验证信息
 *
 * @author mxd
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Valid {

  /** 验证是否有该权限 */
  Authorization authorization() default Authorization.NONE;

  /** 验证是否是只读模式 */
  boolean readonly() default true;

  /** 验证是否需要登录 */
  boolean requireLogin() default true;
}
