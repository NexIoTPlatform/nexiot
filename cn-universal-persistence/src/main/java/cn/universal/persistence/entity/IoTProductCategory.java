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

import cn.universal.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Data;

/**
 * 产品分类对象 iot_product_category @Author gitee.com/NexIoT
 *
 * @since 2025-12-29
 */
@Data
@Schema(description = "IoT产品分类实体")
public class IoTProductCategory implements Serializable {

  private static final long serialVersionUID = 1L;

  /** id */
  @Schema(description = "主键ID")
  private String id;

  /** 父id */
  @Excel(name = "父id")
  @Schema(description = "父分类ID")
  private String parentId;

  /** 是否有子节点 */
  @Excel(name = "是否有子节点")
  @Schema(description = "是否有子分类")
  private Integer hasChild;

  /** 标识 */
  @Excel(name = "标识")
  @Schema(description = "分类标识")
  private String identification;

  /** 分类名称 */
  @Excel(name = "分类名称")
  @Schema(description = "分类名称")
  private String classifiedName;

  /** 说明 */
  @Excel(name = "说明")
  @Schema(description = "分类描述")
  private String description;

  /** 创建时间 */
  @Excel(name = "创建时间")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createTime;

  /** 创建者 */
  @Excel(name = "创建者")
  @Schema(description = "创建者")
  private String createBy;

  /** 租户ID */
  @Column(name = "tenant_id")
  @Schema(description = "租户ID")
  private String tenantId;

  @Transient
  @Schema(description = "数据权限可见用户")
  private List<String> scopeUnionIds = new ArrayList<>();

  @Transient
  @Schema(description = "是否无权限")
  private boolean scopeDenied;

  /** 子分类 */
  @Schema(description = "子分类列表")
  private List<IoTProductCategory> children = new ArrayList<IoTProductCategory>();

  public List<IoTProductCategory> getChildren() {
    return children;
  }

  public void setChildren(List<IoTProductCategory> children) {
    this.children = children;
  }

  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public Integer getHasChild() {
    return hasChild;
  }

  public void setHasChild(Integer hasChild) {
    this.hasChild = hasChild;
  }

  public String getIdentification() {
    return identification;
  }

  public void setIdentification(String identification) {
    this.identification = identification;
  }

  public String getClassifiedName() {
    return classifiedName;
  }

  public void setClassifiedName(String classifiedName) {
    this.classifiedName = classifiedName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public String getCreateBy() {
    return createBy;
  }

  public void setCreateBy(String createBy) {
    this.createBy = createBy;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }
}
