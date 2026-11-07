package cn.universal.persistence.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "iot_tenant_package_menu")
@Schema(description = "租户套餐菜单关联实体")
public class IoTTenantPackageMenu implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @KeySql(genId = SQenGenId.class)
  @Schema(description = "主键ID")
  private Long id;

  @Column(name = "package_id")
  @Schema(description = "套餐ID")
  private Long packageId;

  @Column(name = "menu_id")
  @Schema(description = "菜单ID")
  private Long menuId;

  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createTime;
}
