/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 物模型版本历史实体类
 * @Author: gitee.com/NexIoT
 *
 */

package cn.universal.persistence.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
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
import tk.mybatis.mapper.annotation.KeySql;

/**
 * 物模型版本历史实体 iot_product_metadata_version
 *
 * @since 2026-01-06
 */
@Table(name = "iot_product_metadata_version")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "物模型版本历史实体")
public class IoTProductMetadataVersion implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 主键ID */
  @Id
  @Schema(description = "主键ID")
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 产品ID */
  @Column(name = "product_id")
  @Schema(description = "产品ID（关联iot_product表）")
  private Long productId;

  /** 产品Key */
  @Column(name = "product_key")
  @Schema(description = "产品Key")
  private String productKey;

  /** 版本号 */
  @Schema(description = "版本号（自增）")
  private Integer version;

  /** 物模型快照 */
  @Column(name = "metadata_snapshot")
  @ColumnType(jdbcType = JdbcType.OTHER, typeHandler = PostgreSqlJsonbStringTypeHandler.class)
  @Schema(description = "物模型快照（完整JSON）")
  private String metadataSnapshot;

  /** 变更类型 */
  @Column(name = "change_type")
  @Schema(description = "变更类型（create-创建，update-更新，import-导入，restore-还原）")
  private String changeType;

  /** 变更描述 */
  @Column(name = "change_description")
  @Schema(description = "变更描述")
  private String changeDescription;

  /** 操作人 */
  @Schema(description = "操作人")
  private String operator;

  /** 创建时间 */
  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createTime;

  /** 产品名称（非数据库字段） */
  @Transient
  @Schema(description = "产品名称")
  private String productName;
}
