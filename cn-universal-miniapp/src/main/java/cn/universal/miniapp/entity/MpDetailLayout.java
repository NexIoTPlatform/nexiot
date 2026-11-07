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
 * 设备详情装修配置实体
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Table(name = "mp_detail_layout")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MpDetailLayout implements Serializable {

  private static final long serialVersionUID = 1L;
  @KeySql(genId = SQenGenId.class)
  @Id private Long id;

  /** 小程序APPID */
  @Column(name = "app_id")
  private String appId;

  /** 产品Key */
  @Column(name = "product_key")
  private String productKey;

  /** 布局名称 */
  @Column(name = "layout_name")
  private String layoutName;

  /** 布局配置JSON */
  @Column(name = "layout_config")
  @ColumnType(jdbcType = JdbcType.OTHER, typeHandler = PostgreSqlJsonbStringTypeHandler.class)
  private String layoutConfig;

  /** 状态：0-禁用 1-启用 */
  private Integer status;

  /** 创建者ID */
  @Column(name = "creator_id")
  private String creatorId;

  /** 创建时间 */
  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date createTime;

  /** 更新时间 */
  @Column(name = "update_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date updateTime;
}
