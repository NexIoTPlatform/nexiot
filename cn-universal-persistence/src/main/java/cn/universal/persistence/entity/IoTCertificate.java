package cn.universal.persistence.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import cn.universal.persistence.common.typehandler.PostgreSqlJsonbStringTypeHandler;
import cn.universal.persistence.common.typehandler.EncryptedCertificateStringTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.JdbcType;
import tk.mybatis.mapper.annotation.ColumnType;
import tk.mybatis.mapper.annotation.KeySql;

@Table(name = "iot_certificate")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "IoT证书实体")
public class IoTCertificate implements Serializable {

  @Schema(description = "主键ID")
  @Id
  @KeySql(genId = SQenGenId.class)
  private Long id;

  @Column(name = "ssl_key")
  @Schema(description = "SSL密钥")
  private String sslKey;

  @Column(name = "name")
  @Schema(description = "证书名称")
  private String name;

  @Column(name = "cert_content")
  @ColumnType(typeHandler = EncryptedCertificateStringTypeHandler.class)
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  @Schema(description = "证书内容")
  private String certContent; // PEM内容或加密内容

  @Column(name = "key_content")
  @ColumnType(typeHandler = EncryptedCertificateStringTypeHandler.class)
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  @Schema(description = "密钥内容")
  private String keyContent; // PEM内容或加密内容

  @Column(name = "cert_password")
  @ColumnType(typeHandler = EncryptedCertificateStringTypeHandler.class)
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  @Schema(description = "证书密码")
  private String certPassword;

  @Column(name = "key_password")
  @ColumnType(typeHandler = EncryptedCertificateStringTypeHandler.class)
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  @Schema(description = "密钥密码")
  private String keyPassword;

  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

  @Column(name = "certificate_type")
  @Schema(description = "证书类型: GENERIC/CLIENT/CA/SERVER/OPCUA_BUNDLE")
  private String certificateType;

  @Column(name = "trust_mode")
  @Schema(description = "服务端信任模式: PINNED/CA")
  private String trustMode;

  @Column(name = "trust_cert_content")
  @ColumnType(typeHandler = EncryptedCertificateStringTypeHandler.class)
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  @Schema(description = "OPC UA服务端固定证书或CA信任链")
  private String trustCertContent;

  @Column(name = "fingerprint_sha256")
  @Schema(description = "客户端证书SHA-256指纹")
  private String fingerprintSha256;

  @Column(name = "trust_fingerprint_sha256")
  @Schema(description = "服务端信任材料SHA-256指纹")
  private String trustFingerprintSha256;

  @Column(name = "expire_time")
  @Schema(description = "过期时间")
  private Date expireTime;

  @Column(name = "remark")
  @Schema(description = "备注")
  private String remark;

  @Column(name = "create_time")
  @Schema(description = "创建时间")
  private Date createTime;

  @Column(name = "update_time")
  @Schema(description = "更新时间")
  private Date updateTime;

  @Column(name = "create_user")
  @Schema(description = "创建用户")
  private String createUser;

  @Column(name = "cert_info")
  @ColumnType(jdbcType = JdbcType.OTHER, typeHandler = PostgreSqlJsonbStringTypeHandler.class)
  @Schema(description = "证书信息")
  private String certInfo; // 证书详细信息（JSON）
}
