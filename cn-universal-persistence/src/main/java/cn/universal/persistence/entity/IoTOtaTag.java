/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA自定义标签实体
 * @Author: gitee.com/NexIoT
 *
 */

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

/**
 * OTA自定义标签实体 iot_ota_tag
 *
 * @since 2026-03-21
 */
@Table(name = "iot_ota_tag")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "OTA自定义标签实体")
public class IoTOtaTag implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @Schema(description = "主键ID")
  @KeySql(genId = SQenGenId.class)
  private Long id;

  @Column(name = "tag_code")
  @Schema(description = "标签编码")
  private String tagCode;

  @Column(name = "tag_name")
  @Schema(description = "标签名称")
  private String tagName;

  @Schema(description = "标签颜色")
  private String color;

  @Schema(description = "标签描述")
  private String description;

  @Column(name = "creator_id")
  @Schema(description = "创建人")
  private String creatorId;

  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createTime;

  @Column(name = "update_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "更新时间")
  private Date updateTime;
}
