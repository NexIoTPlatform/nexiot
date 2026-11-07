package cn.universal.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/** 经过已登录账号确认的微信身份关系，与通知收件人映射分开。 */
@Data
@Table(name = "iot_user_wechat_binding")
public class WechatUserBinding implements Serializable {
  @Id private String id;
  @Column(name = "tenant_id") private String tenantId;
  @Column(name = "app_id") private String appId;
  @Column(name = "openid") private String openId;
  @Column(name = "user_union_id") private String userUnionId;
  @Column(name = "create_time") private Date createTime;
  @Column(name = "update_time") private Date updateTime;
}
