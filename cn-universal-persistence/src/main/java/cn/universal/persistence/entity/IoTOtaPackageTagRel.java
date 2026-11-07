/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: OTA升级包标签关系实体
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
 * OTA升级包标签关系实体 iot_ota_package_tag_rel
 *
 * @since 2026-03-21
 */
@Table(name = "iot_ota_package_tag_rel")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "OTA升级包标签关系实体")
public class IoTOtaPackageTagRel implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @Schema(description = "主键ID")
  @KeySql(genId = SQenGenId.class)
  private Long id;

  @Column(name = "package_id")
  @Schema(description = "升级包ID")
  private Long packageId;

  @Column(name = "tag_id")
  @Schema(description = "标签ID")
  private Long tagId;

  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createTime;
}
