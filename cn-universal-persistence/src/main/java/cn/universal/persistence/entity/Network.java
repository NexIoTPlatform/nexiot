/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.persistence.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
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

@Table(name = "iot_network")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "网络配置实体")
public class Network implements Serializable {

  public static final int STATE_DISABLED = 0;
  public static final int STATE_ENABLED = 1;

  @Schema(description = "主键ID")
  @Id
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** TCP_CLIENT,MQTT_CLIENT,HTTP_CLIENT,WEB_SOCKET_CLIENT */
  @Schema(description = "网络类型")
  private String type;

  /** 唯一标识 */
  @Column(name = "product_key")
  @Schema(description = "产品Key")
  private String productKey;

  @Column(name = "union_id")
  @Schema(description = "联合ID")
  private String unionId;

  @Schema(description = "网络名称")
  private String name;

  /** 详细描述 */
  @Schema(description = "描述信息")
  private String description;

  @Column(name = "create_date")
  @Schema(description = "创建时间")
  private Date createDate;

  /** 0-disable,1-enable */
  @Schema(description = "状态: 0-停用, 1-启用")
  private Integer state;

  /** 配置内容 */
  @Schema(description = "配置内容")
  private String configuration;

  /** 创建用户 */
  @Column(name = "create_user")
  @Schema(description = "创建用户")
  private String createUser;

  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

  public boolean isStateEnabled() {
    return Integer.valueOf(STATE_ENABLED).equals(state);
  }

  private static final long serialVersionUID = 1L;
}
