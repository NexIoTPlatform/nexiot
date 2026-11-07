package nexiot.web.ide.debug.plugins.modules.db.inteceptor;

import nexiot.web.ide.debug.plugins.modules.db.model.SqlMode;
import nexiot.web.ide.debug.plugins.modules.db.table.NamedTable;

/**
 * 单表模块拦截器
 *
 * @since 1.5.3
 */
public interface NamedTableInterceptor {

  /** 执行之前 */
  void preHandle(SqlMode sqlMode, NamedTable namedTable);
}
