package nexiot.web.ide.debug.plugins.modules.db.dialect;

import nexiot.web.ide.debug.plugins.modules.db.BoundSql;

/**
 * 人大金仓kingbase方言
 *
 * @author is_lixy@163.com
 */
public class KingbaseSQLDialect implements Dialect {

  @Override
  public boolean match(String jdbcUrl) {
    return jdbcUrl.contains(":kingbase8:");
  }

  @Override
  public String getPageSql(String sql, BoundSql boundSql, long offset, long limit) {
    boundSql.addParameter(limit);
    boundSql.addParameter(offset);
    return sql + "\n limit ? offset ?";
  }
}
