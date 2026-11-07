package cn.universal.persistence.entity;

import cn.universal.persistence.common.typehandler.PostgreSqlJsonbStringTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.JdbcType;
import tk.mybatis.mapper.annotation.ColumnType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "iot_tenant")
@Schema(description = "IoT租户实体")
public class IoTTenant implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

  @Column(name = "tenant_code")
  @Schema(description = "租户编码")
  private String tenantCode;

  @Column(name = "tenant_name")
  @Schema(description = "租户名称")
  private String tenantName;

  @Column(name = "package_id")
  @Schema(description = "租户套餐ID")
  private Long packageId;

  @Schema(description = "租户状态：0-正常 1-停用")
  private Integer status;

  @Column(name = "expire_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "到期时间")
  private Date expireTime;

  @Column(name = "account_count")
  @Schema(description = "账号数量上限")
  private Integer accountCount;

  @Column(name = "contact_name")
  @Schema(description = "联系人")
  private String contactName;

  @Column(name = "contact_mobile")
  @Schema(description = "联系电话")
  private String contactMobile;

  @Schema(description = "租户域名")
  private String domain;

  @Schema(description = "租户官网")
  private String website;

  @ColumnType(jdbcType = JdbcType.OTHER, typeHandler = PostgreSqlJsonbStringTypeHandler.class)
  @Schema(description = "扩展配置")
  private String cfg;

  @Schema(description = "备注")
  private String remark;

  @Column(name = "create_by")
  @Schema(description = "创建人")
  private String createBy;

  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createTime;

  @Column(name = "update_by")
  @Schema(description = "更新人")
  private String updateBy;

  @Column(name = "update_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "更新时间")
  private Date updateTime;

  @Schema(description = "删除标记：0-正常 1-删除")
  private Integer deleted;

  @Transient
  @Schema(description = "租户套餐名称")
  private String packageName;

  @Transient
  @Schema(description = "租户菜单数量")
  private Integer menuCount;

  @Transient
  @Schema(description = "租户当前账号数")
  private Long userCount;

  @Transient
  @Schema(description = "租户管理员unionId")
  private String adminUnionId;

  @Transient
  @Schema(description = "租户管理员用户名")
  private String adminUsername;

  @Transient
  @Schema(description = "租户管理员昵称")
  private String adminAlias;

  @Transient
  @Schema(description = "租户管理员手机号")
  private String adminMobile;

  @Transient
  @Schema(description = "租户管理员密码")
  private String adminPassword;

  @Transient
  @Schema(description = "当前域名生效的网站名称")
  private String siteName;

  @Transient
  @Schema(description = "当前域名生效的登录标题")
  private String loginTitle;

  @Transient
  @Schema(description = "当前域名生效的网站Logo")
  private String siteLogo;

  @Transient
  @Schema(description = "当前域名生效的浏览器图标")
  private String siteIcon;

  @Transient
  @Schema(description = "当前域名生效的登录模板")
  private String loginTemplate;
}
