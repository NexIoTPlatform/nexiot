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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tk.mybatis.mapper.annotation.KeySql;

@Table(name = "iot_device_group")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "IoT设备分组实体")
public class IoTDeviceGroup implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 分组ID，非自增 */
  @Id
  @KeySql(genId = SQenGenId.class)
  @Schema(description = "分组ID")
  private Long id;

  /** 分组名称 */
  @Column(name = "group_name")
  @Schema(description = "分组名称")
  private String groupName;

  /** 分组标识 */
  @Column(name = "group_code")
  @Schema(description = "分组编码")
  private String groupCode;

  /** 群组描述 */
  @Column(name = "group_describe")
  @Schema(description = "分组描述")
  private String groupDescribe;

  /** 父id */
  @Column(name = "parent_id")
  @Schema(description = "父分组ID")
  private Long parentId;

  /** 是否有子分组 */
  @Column(name = "has_child")
  @Schema(description = "是否有子分组")
  private Integer hasChild;

  /** 分组级别 */
  @Column(name = "group_level")
  @Schema(description = "分组级别")
  private Integer groupLevel;

  /** 激活设备数 */
  @Column(name = "relat_dev_count")
  @Schema(description = "关联设备数")
  private Integer relatDevCount;

  /** 关联设备树 */
  @Column(name = "active_dev_count")
  @Schema(description = "激活设备数")
  private Integer activeDevCount;

  /** 创建人 */
  @Column(name = "creator_id")
  @Schema(description = "创建人ID")
  private String creatorId;

  /** 租户ID */
  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

  /** 实例编号 */
  @Column(name = "instance")
  @Schema(description = "实例名称")
  private String instance;

  /** 标签 */
  @Column(name = "tag")
  @Schema(description = "分组标签")
  private String tag;
}
