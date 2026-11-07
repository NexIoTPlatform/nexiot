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
 * 资源连接配置实体
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "iot_resource_connection")
@Schema(description = "资源连接配置实体")
public class ResourceConnection implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @KeySql(genId = SQenGenId.class)
  @Schema(description = "主键ID")
  private Long id;

  /** 资源名称 */
  @Column(name = "name")
  @Schema(description = "资源名称")
  private String name;

  /** 资源类型 */
  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  @Schema(description = "资源类型")
  private ResourceType type;

  /** 插件类型 */
  @Column(name = "plugin_type")
  @Schema(description = "插件类型")
  private String pluginType;

  /** 主机地址 */
  @Column(name = "host")
  @Schema(description = "主机地址")
  private String host;

  /** 端口号 */
  @Column(name = "port")
  @Schema(description = "端口号")
  private Integer port;

  /** 用户名 */
  @Column(name = "username")
  @Schema(description = "用户名")
  private String username;

  /** 密码 */
  @Column(name = "password")
  @Schema(description = "密码")
  private String password;

  /** 数据库名 */
  @Column(name = "database_name")
  @Schema(description = "数据库名")
  private String databaseName;

  /** 扩展配置JSON */
  @Column(name = "extra_config")
  @Schema(description = "扩展配置JSON")
  private String extraConfig;

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

  /** 方向：IN-输入，OUT-输出，BOTH-双向 */
  @Column(name = "direction")
  @Enumerated(EnumType.STRING)
  @Schema(description = "方向")
  private Direction direction;

  /** 数据流向：INPUT-输入，OUTPUT-输出，BIDIRECTIONAL-双向 */
  @Column(name = "data_direction")
  @Enumerated(EnumType.STRING)
  @Schema(description = "数据流向")
  private DataDirection dataDirection;

  /** 动态配置JSON - 使用现有的extra_config字段存储 */
  @Transient
  @Schema(description = "动态配置JSON")
  private String dynamicConfig;

  /** 资源类型枚举 */
  public enum ResourceType {
    MYSQL,
    POSTGRESQL,
    H2,
    ORACLE,
    SQLSERVER,
    KAFKA,
    IOTDB,
    INFLUXDB,
    MQTT,
    HTTP,
    REDIS,
    ELASTICSEARCH,
    ALIYUN_IOT,
    TENCENT_IOT,
    HUAWEI_IOT
  }

  /** 方向枚举 */
  public enum Direction {
    IN, // 输入
    OUT, // 输出
    BOTH // 双向
  }

  /** 数据流向枚举 */
  public enum DataDirection {
    INPUT, // 数据输入
    OUTPUT, // 数据输出
    BIDIRECTIONAL // 双向流转
  }
}
