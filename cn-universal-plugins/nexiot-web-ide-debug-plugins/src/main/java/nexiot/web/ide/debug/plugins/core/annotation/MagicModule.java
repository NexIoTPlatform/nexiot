package nexiot.web.ide.debug.plugins.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 模块，主要用于import指令，import时根据模块名获取当前类如：<code>import assert</code>;
 *
 * @author mxd
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MagicModule {

  /** 模块名 */
  String value();
}
