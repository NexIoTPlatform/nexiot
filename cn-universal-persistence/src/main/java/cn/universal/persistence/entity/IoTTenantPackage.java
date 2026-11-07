package cn.universal.persistence.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
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
import tk.mybatis.mapper.annotation.KeySql;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "iot_tenant_package")
@Schema(description = "IoT租户套餐实体")
public class IoTTenantPackage implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @KeySql(genId = SQenGenId.class)
  @Column(name = "package_id")
  @Schema(description = "套餐ID")
  private Long packageId;

  @Column(name = "package_name")
  @Schema(description = "套餐名称")
  private String packageName;

  @Schema(description = "套餐状态：0-正常 1-停用")
  private Integer status;

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
  @Schema(description = "菜单ID数组")
  private Long[] menuIds;

  @Transient
  @Schema(description = "菜单数量")
  private Integer menuCount;

  @Transient
  @Schema(description = "已关联租户数量")
  private Long tenantCount;
}
