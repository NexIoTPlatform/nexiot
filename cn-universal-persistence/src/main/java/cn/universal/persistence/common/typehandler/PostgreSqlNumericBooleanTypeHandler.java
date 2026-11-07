package cn.universal.persistence.common.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * PostgreSQL/达梦兼容处理：将历史 MySQL tinyint(1) 风格迁移后的 numeric flag 字段按 0/1 读写。
 */
public class PostgreSqlNumericBooleanTypeHandler extends BaseTypeHandler<Boolean> {

  @Override
  public void setNonNullParameter(
      PreparedStatement ps, int i, Boolean parameter, JdbcType jdbcType) throws SQLException {
    ps.setShort(i, Boolean.TRUE.equals(parameter) ? (short) 1 : (short) 0);
  }

  @Override
  public Boolean getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return toBoolean(rs.getObject(columnName));
  }

  @Override
  public Boolean getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return toBoolean(rs.getObject(columnIndex));
  }

  @Override
  public Boolean getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return toBoolean(cs.getObject(columnIndex));
  }

  private Boolean toBoolean(Object value) throws SQLException {
    if (value == null) {
      return null;
    }
    if (value instanceof Boolean booleanValue) {
      return booleanValue;
    }
    if (value instanceof Number number) {
      return number.intValue() != 0;
    }

    String normalized = value.toString().trim();
    if (normalized.isEmpty()) {
      return null;
    }
    if (isTrueValue(normalized)) {
      return Boolean.TRUE;
    }
    if (isFalseValue(normalized)) {
      return Boolean.FALSE;
    }

    throw new SQLException("Unsupported numeric boolean flag value: " + value);
  }

  private boolean isTrueValue(String value) {
    return "1".equals(value)
        || "t".equalsIgnoreCase(value)
        || "true".equalsIgnoreCase(value)
        || "y".equalsIgnoreCase(value)
        || "yes".equalsIgnoreCase(value)
        || "on".equalsIgnoreCase(value);
  }

  private boolean isFalseValue(String value) {
    return "0".equals(value)
        || "f".equalsIgnoreCase(value)
        || "false".equalsIgnoreCase(value)
        || "n".equalsIgnoreCase(value)
        || "no".equalsIgnoreCase(value)
        || "off".equalsIgnoreCase(value);
  }
}
