package nexiot.web.ide.debug.plugins.utils;

import nexiot.web.ide.debug.plugins.core.exception.MagicAPIException;
import org.apache.commons.lang3.StringUtils;

/**
 * 断言辅助类
 *
 * @author mxd
 */
public class Assert {

  /** 断言值不能为空 */
  public static void isNotNull(Object value, String message) {
    if (value == null) {
      throw new MagicAPIException(message);
    }
  }

  /** 验证值必须是true */
  public static void isTrue(boolean value, String message) {
    if (!value) {
      throw new MagicAPIException(message);
    }
  }

  /** 断言值不能为空字符串 */
  public static void isNotBlank(String value, String message) {
    if (StringUtils.isBlank(value)) {
      throw new MagicAPIException(message);
    }
  }

  /** 断言值不能为空字符串 */
  public static void isNotBlanks(String message, String... values) {
    if (values != null) {
      for (String value : values) {
        isNotBlank(value, message);
      }
    }
  }
}
