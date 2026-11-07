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
import cn.universal.persistence.common.typehandler.PostgreSqlNumericBooleanTypeHandler;
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
import org.apache.ibatis.type.JdbcType;
import tk.mybatis.mapper.annotation.ColumnType;
import tk.mybatis.mapper.annotation.KeySql;

/**
 * 算法配置实体
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/2/27
 */
@Table(name = "iot_algorithm_config")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "算法配置实体")
public class IoTAlgorithmConfig implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 主键ID */
  @Id
  @Schema(description = "主键ID")
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 产品Key */
  @Column(name = "product_key")
  @Schema(description = "产品Key")
  private String productKey;

  /** 产品ID（兼容字段，可为空） */
  @Column(name = "product_id")
  @Schema(description = "产品ID")
  private Long productId;

  /** 算法类型 */
  @Column(name = "algorithm_type")
  @Schema(description = "算法类型")
  private String algorithmType;

  /** 算法名称 */
  @Column(name = "algorithm_name")
  @Schema(description = "算法名称")
  private String algorithmName;

  /** 算法描述 */
  @Column(name = "description")
  @Schema(description = "算法描述")
  private String description;

  /** 是否启用 */
  @Column(name = "enable")
  // PG/达梦保留 numeric smallint 标记位，避免直接把 Boolean 绑定成数据库 boolean。
  @ColumnType(
      jdbcType = JdbcType.SMALLINT,
      typeHandler = PostgreSqlNumericBooleanTypeHandler.class)
  @Schema(description = "是否启用")
  private Boolean enable;

  /** 算法配置JSON */
  @Column(name = "config_json")
  @Schema(description = "算法配置JSON")
  private String configJson;

  /** 创建时间 */
  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createTime;

  /** 更新时间 */
  @Column(name = "update_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "更新时间")
  private Date updateTime;

  /** 创建人 */
  @Column(name = "creator_id")
  @Schema(description = "创建人")
  private String creatorId;
}
