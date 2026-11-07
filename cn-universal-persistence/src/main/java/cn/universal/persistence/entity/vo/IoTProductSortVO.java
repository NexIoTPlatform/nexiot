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

package cn.universal.persistence.entity.vo;

import cn.universal.common.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 产品分类对象 iot_product_category @Author gitee.com/NexIoT
 *
 * @since 2025-12-29
 */
public class IoTProductSortVO implements Serializable {

  private static final long serialVersionUID = 1L;

  /** id */
  private String id;

  /** 父id */
  @Excel(name = "父id")
  private String parentId;

  /** 是否有子节点 */
  @Excel(name = "是否有子节点")
  private Integer hasChild;

  /** 标识 */
  @Excel(name = "标识")
  private String identification;

  /** 分类名称 */
  @Excel(name = "分类名称")
  private String label;

  /** 说明 */
  @Excel(name = "说明")
  private String description;

  /** 创建时间 */
  @Excel(name = "创建时间")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date createTime;

  /** 创建者 */
  @Excel(name = "创建者")
  private String createBy;

  /** 子分类 */
  private List<IoTProductSortVO> children = new ArrayList<IoTProductSortVO>();

  public List<IoTProductSortVO> getChildren() {
    return children;
  }

  public void setChildren(List<IoTProductSortVO> children) {
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

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
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
}
