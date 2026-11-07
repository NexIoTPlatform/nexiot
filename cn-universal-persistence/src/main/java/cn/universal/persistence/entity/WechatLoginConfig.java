package cn.universal.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import lombok.ToString;

/** 租户公众号登录设置。秘密字段仅允许在服务端内部解密使用。 */
@Data
@Table(name = "iot_wechat_login_config")
public class WechatLoginConfig implements Serializable {
  @Id @Column(name = "tenant_id") private String tenantId;
  @Column(name = "account_name") private String accountName;
  @Column(name = "app_id") private String appId;
  @JsonIgnore @ToString.Exclude @Column(name = "app_secret") private String appSecret;
  @JsonIgnore @ToString.Exclude @Column(name = "callback_token") private String callbackToken;
  @JsonIgnore @ToString.Exclude @Column(name = "encoding_aes_key") private String encodingAESKey;
  @Column(name = "account_qr_url") private String accountQrUrl;
  @Column(name = "capability_mode") private String capabilityMode;
  @Column(name = "login_mode") private String loginMode;
  private Boolean enabled;
  @Column(name = "binding_enabled") private Boolean bindingEnabled;
  @Column(name = "demo_user_union_id") private String demoUserUnionId;
  @Column(name = "expire_seconds") private Integer expireSeconds;
  @Column(name = "config_version") private Long configVersion;
  @Column(name = "create_time") private Date createTime;
  @Column(name = "update_time") private Date updateTime;
}
