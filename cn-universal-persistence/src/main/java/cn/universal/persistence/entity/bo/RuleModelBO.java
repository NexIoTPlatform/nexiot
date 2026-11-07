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

package cn.universal.persistence.entity.bo;

import cn.universal.persistence.entity.IoTProductCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规则模型查询 @Author gitee.com/NexIoT
 *
 * @since 2023/1/13 15:06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema
public class RuleModelBO {

  @Schema(description = "id")
  private Long id;

  @Schema(description = "规则名称")
  private String ruleName;

  @Schema(description = "状态 run.运行中 stop.已停止")
  private String status;

  private String productKey;
  private String creatorId;
  private String tenantId;
  private String iotId;
  private List<String> groupIds;

  /**
   * 产品分类业务对象
   *
   * @since 2025-12-29
   */
  @Data
  public static class IoTProductSortBO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分类ID（新增时为空） */
    private String id;

    /** 父分类ID */
    @NotNull(message = "父分类ID不能为空")
    private String parentId;

    /** 分类名称 */
    @NotBlank(message = "分类名称不能为空")
    private String classifiedName;

    /** 标识 */
    private String identification;

    /** 说明 */
    private String description;

    /**
     * 转换为实体对象
     *
     * @return DevProductSort实体
     */
    public IoTProductCategory toEntity() {
      IoTProductCategory entity = new IoTProductCategory();
      entity.setId(this.id);
      entity.setParentId(this.parentId);
      entity.setClassifiedName(this.classifiedName);
      entity.setIdentification(this.identification);
      entity.setDescription(this.description);
      return entity;
    }

    /**
     * 从实体对象转换
     *
     * @param entity 实体对象
     * @return 当前BO对象
     */
    public IoTProductSortBO fromEntity(IoTProductCategory entity) {
      if (entity != null) {
        this.id = entity.getId();
        this.parentId = entity.getParentId();
        this.classifiedName = entity.getClassifiedName();
        this.identification = entity.getIdentification();
        this.description = entity.getDescription();
      }
      return this;
    }
  }
}
