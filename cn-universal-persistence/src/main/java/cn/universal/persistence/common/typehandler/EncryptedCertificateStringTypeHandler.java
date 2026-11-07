package cn.universal.persistence.common.typehandler;

import cn.universal.common.security.CertificateFieldCrypto;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/** Transparently encrypts certificate secrets on write and decrypts them on read. */
public class EncryptedCertificateStringTypeHandler extends BaseTypeHandler<String> {

  @Override
  public void setNonNullParameter(
      PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
    ps.setString(i, CertificateFieldCrypto.encrypt(parameter));
  }

  @Override
  public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return CertificateFieldCrypto.decrypt(rs.getString(columnName));
  }

  @Override
  public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return CertificateFieldCrypto.decrypt(rs.getString(columnIndex));
  }

  @Override
  public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return CertificateFieldCrypto.decrypt(cs.getString(columnIndex));
  }
}
