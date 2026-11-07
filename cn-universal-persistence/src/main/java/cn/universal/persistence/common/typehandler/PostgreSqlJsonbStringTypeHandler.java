package cn.universal.persistence.common.typehandler;

import java.lang.reflect.InvocationTargetException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * PostgreSQL 使用 jsonb 保存 JSON 字符串时的兼容处理。
 *
 * <p>在 PostgreSQL 下将字符串包装成 {@code jsonb} 参数，其他数据库仍按普通字符串写入，避免影响
 * MySQL/达梦的现有行为。
 */
public class PostgreSqlJsonbStringTypeHandler extends BaseTypeHandler<String> {

  private static final String POSTGRESQL = "PostgreSQL";
  private static final String PG_OBJECT_CLASS = "org.postgresql.util.PGobject";

  @Override
  public void setNonNullParameter(
      PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
    if (isPostgreSql(ps)) {
      if (parameter.isBlank()) {
        ps.setNull(i, Types.OTHER);
        return;
      }
      ps.setObject(i, buildJsonbObject(parameter));
      return;
    }

    ps.setString(i, parameter);
  }

  @Override
  public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return rs.getString(columnName);
  }

  @Override
  public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return rs.getString(columnIndex);
  }

  @Override
  public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return cs.getString(columnIndex);
  }

  private boolean isPostgreSql(PreparedStatement ps) throws SQLException {
    return POSTGRESQL.equalsIgnoreCase(ps.getConnection().getMetaData().getDatabaseProductName());
  }

  private Object buildJsonbObject(String value) throws SQLException {
    try {
      Class<?> pgObjectClass = Class.forName(PG_OBJECT_CLASS);
      Object pgObject = pgObjectClass.getDeclaredConstructor().newInstance();
      pgObjectClass.getMethod("setType", String.class).invoke(pgObject, "jsonb");
      pgObjectClass.getMethod("setValue", String.class).invoke(pgObject, value);
      return pgObject;
    } catch (ClassNotFoundException e) {
      throw new SQLException("PostgreSQL driver PGobject class not found", e);
    } catch (NoSuchMethodException
        | InstantiationException
        | IllegalAccessException
        | InvocationTargetException e) {
      throw new SQLException("Failed to build PostgreSQL jsonb parameter", e);
    }
  }
}
