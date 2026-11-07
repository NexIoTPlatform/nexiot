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

package cn.universal.databridge.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

/**
 * 数据桥接配置实体
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "iot_data_bridge_config")
@Schema(description = "数据桥接配置实体")
public class DataBridgeConfig implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @KeySql(genId = SQenGenId.class)
  @Schema(description = "主键ID")
  private Long id;

  /** 配置名称 */
  @Column(name = "name")
  @Schema(description = "配置名称")
  private String name;

  /** 源范围：ALL_PRODUCTS-所有产品，SPECIFIC_PRODUCTS-指定产品，APPLICATION-应用 */
  @Column(name = "source_scope")
  @Enumerated(EnumType.STRING)
  @Schema(description = "源范围")
  private SourceScope sourceScope;

  /** 源产品KEY列表JSON（当source_scope=SPECIFIC_PRODUCTS时使用） */
  @Column(name = "source_product_keys")
  @Schema(description = "源产品KEY列表JSON")
  private String sourceProductKeys;

  /** 源应用ID（当source_scope=APPLICATION时使用） */
  @Column(name = "source_application_id")
  @Schema(description = "源应用ID")
  private Long sourceApplicationId;

  /** 目标资源ID */
  @Column(name = "target_resource_id")
  @Schema(description = "目标资源ID")
  private Long targetResourceId;

  /** 桥接类型(JDBC,KAFKA,MQTT,HTTP,IOTDB,INFLUXDB等) */
  @Column(name = "bridge_type")
  @Enumerated(EnumType.STRING)
  @Schema(description = "桥接类型")
  private BridgeType bridgeType;

  /** 模板内容（SQL、JSON等） */
  @Column(name = "template")
  @Schema(description = "模板内容")
  private String template;

  /** Magic脚本内容（用户自定义处理逻辑） */
  @Column(name = "magic_script")
  @Schema(description = "Magic脚本内容")
  private String magicScript;

  /** 统一配置JSON */
  @Column(name = "config")
  @Schema(description = "统一配置JSON")
  private String config;

  /** 状态：0禁用，1启用 */
  @Column(name = "status")
  @Schema(description = "状态")
  private Integer status;

  /** 描述 */
  @Column(name = "description")
  @Schema(description = "描述")
  private String description;

  /** 创建者 */
  @Column(name = "create_by")
  @Schema(description = "创建者")
  private String createBy;

  /** 创建时间 */
  @Column(name = "create_time")
  @Schema(description = "创建时间")
  private LocalDateTime createTime;

  /** 更新者 */
  @Column(name = "update_by")
  @Schema(description = "更新者")
  private String updateBy;

  /** 更新时间 */
  @Column(name = "update_time")
  @Schema(description = "更新时间")
  private LocalDateTime updateTime;

  /** 源范围枚举 */
  public enum SourceScope {
    ALL_PRODUCTS, // 所有产品
    SPECIFIC_PRODUCTS, // 指定产品
    APPLICATION // 应用级别
  }

  /** 桥接类型枚举 */
  public enum BridgeType {
    JDBC,
    KAFKA,
    IOTDB,
    INFLUXDB,
    MQTT,
    HTTP,
    REDIS,
    ELASTICSEARCH
  }
}
