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
import java.math.BigDecimal;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.JdbcType;
import tk.mybatis.mapper.annotation.ColumnType;
import tk.mybatis.mapper.annotation.KeySql;

/**
 * 算法结果实体
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/2/27
 */
@Table(name = "iot_algorithm_result")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "算法结果实体")
public class IoTAlgorithmResult implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 主键ID */
  @Id
  @Schema(description = "主键ID")
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 设备ID */
  @Column(name = "device_id")
  @Schema(description = "设备ID")
  private String deviceId;

  /** 产品Key */
  @Column(name = "product_key")
  @Schema(description = "产品Key")
  private String productKey;

  /** 算法类型 */
  @Column(name = "algorithm_type")
  @Schema(description = "算法类型")
  private String algorithmType;

  /** 算法名称 */
  @Column(name = "algorithm_name")
  @Schema(description = "算法名称")
  private String algorithmName;

  /** 结果JSON */
  @Column(name = "result_json")
  @Schema(description = "结果JSON")
  private String resultJson;

  /** 健康分数 */
  @Column(name = "health_score")
  @Schema(description = "健康分数")
  private BigDecimal healthScore;

  /** 健康等级 */
  @Column(name = "health_level")
  @Schema(description = "健康等级")
  private String healthLevel;

  /** 是否异常 */
  @Column(name = "is_anomaly")
  // PG/达梦保留 numeric smallint 标记位，避免直接把 Boolean 绑定成数据库 boolean。
  @ColumnType(
      jdbcType = JdbcType.SMALLINT,
      typeHandler = PostgreSqlNumericBooleanTypeHandler.class)
  @Schema(description = "是否异常")
  private Boolean isAnomaly;

  /** 异常分数 */
  @Column(name = "anomaly_score")
  @Schema(description = "异常分数")
  private BigDecimal anomalyScore;

  /** 是否成功 */
  @Column(name = "success")
  // PG/达梦保留 numeric smallint 标记位，避免直接把 Boolean 绑定成数据库 boolean。
  @ColumnType(
      jdbcType = JdbcType.SMALLINT,
      typeHandler = PostgreSqlNumericBooleanTypeHandler.class)
  @Schema(description = "是否成功")
  private Boolean success;

  /** 错误信息 */
  @Column(name = "error_message")
  @Schema(description = "错误信息")
  private String errorMessage;

  /** 创建时间 */
  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createTime;
}
