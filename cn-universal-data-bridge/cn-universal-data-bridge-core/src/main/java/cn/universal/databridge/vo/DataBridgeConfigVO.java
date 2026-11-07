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

package cn.universal.databridge.vo;

import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.persistence.entity.bo.IoTProductBO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据桥接配置VO
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据桥接配置VO")
public class DataBridgeConfigVO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "主键ID")
  private Long id;

  /** 配置名称 */
  @Schema(description = "配置名称")
  private String name;

  /** 源范围：ALL_PRODUCTS-所有产品，SPECIFIC_PRODUCTS-指定产品，APPLICATION-应用 */
  @Schema(description = "源范围")
  private DataBridgeConfig.SourceScope sourceScope;

  /** 源产品KEY列表JSON（当source_scope=SPECIFIC_PRODUCTS时使用） */
  @Schema(description = "源产品KEY列表JSON")
  private String sourceProductKeys;

  /** 源产品名称列表（用于前端显示） */
  @Schema(description = "源产品名称列表")
  private List<IoTProductBO> sourceProductNames;

  /** 源应用ID（当source_scope=APPLICATION时使用） */
  @Schema(description = "源应用ID")
  private Long sourceApplicationId;

  /** 源应用名称（用于前端显示） */
  @Schema(description = "源应用名称")
  private String sourceApplicationName;

  /** 目标资源ID */
  @Schema(description = "目标资源ID")
  private Long targetResourceId;

  /** 目标资源名称（用于前端显示） */
  @Schema(description = "目标资源名称")
  private String targetResourceName;

  /** 桥接类型(JDBC,KAFKA,MQTT,HTTP,IOTDB,INFLUXDB等) */
  @Schema(description = "桥接类型")
  private DataBridgeConfig.BridgeType bridgeType;

  /** 模板内容（SQL、JSON等） */
  @Schema(description = "模板内容")
  private String template;

  /** Magic脚本内容（用户自定义处理逻辑） */
  @Schema(description = "Magic脚本内容")
  private String magicScript;

  /** 统一配置JSON */
  @Schema(description = "统一配置JSON")
  private String config;

  /** 状态：0禁用，1启用 */
  @Schema(description = "状态")
  private Integer status;

  /** 描述 */
  @Schema(description = "描述")
  private String description;

  /** 创建者 */
  @Schema(description = "创建者")
  private String createBy;

  /** 创建时间 */
  @Schema(description = "创建时间")
  private LocalDateTime createTime;

  /** 更新者 */
  @Schema(description = "更新者")
  private String updateBy;

  /** 更新时间 */
  @Schema(description = "更新时间")
  private LocalDateTime updateTime;
}
