/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import cn.universal.persistence.common.typehandler.PostgreSqlJsonbStringTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
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
 * 装修组件库实体
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Table(name = "mp_component_library")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MpComponentLibrary implements Serializable {

  private static final long serialVersionUID = 1L;
  @KeySql(genId = SQenGenId.class)
  @Id private Long id;

  /** 组件类型：text/gauge/chart/switch/slider/image/video */
  @Column(name = "component_type")
  private String componentType;

  /** 组件名称 */
  @Column(name = "component_name")
  private String componentName;

  /** 组件图标 */
  @Column(name = "component_icon")
  private String componentIcon;

  /** 配置项schema定义 */
  @Column(name = "config_schema")
  @ColumnType(jdbcType = JdbcType.OTHER, typeHandler = PostgreSqlJsonbStringTypeHandler.class)
  private String configSchema;

  /** 预览图URL */
  @Column(name = "preview_image")
  private String previewImage;

  /** 组件描述 */
  private String description;

  /** 排序序号 */
  @Column(name = "sort_order")
  private Integer sortOrder;

  /** 状态：0-禁用 1-启用 */
  private Integer status;

  /** 创建时间 */
  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date createTime;

  /** 组件类型常量 */
  public static class ComponentType {
    public static final String TEXT = "text";
    public static final String GAUGE = "gauge";
    public static final String CHART = "chart";
    public static final String SWITCH = "switch";
    public static final String SLIDER = "slider";
    public static final String IMAGE = "image";
    public static final String VIDEO = "video";
  }
}
