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

package cn.universal.persistence.page;

import cn.universal.common.utils.StringUtils;

/** 分页数据 @Author ruoyi */
public class PageDomain {

  /** 当前记录起始索引 */
  private Integer pageNum;

  /** 每页显示记录数 */
  private Integer pageSize;

  /** 排序列 */
  private String orderByColumn;

  /** 排序的方向desc或者asc */
  private String isAsc = "asc";

  /** 分页参数合理化 */
  private Boolean reasonable = true;

  public String getOrderBy() {
    if (StringUtils.isEmpty(orderByColumn)) {
      return "";
    }
    return StringUtils.toUnderScoreCase(orderByColumn) + " " + isAsc;
  }

  public Integer getPageNum() {
    return pageNum;
  }

  public void setPageNum(Integer pageNum) {
    this.pageNum = pageNum;
  }

  public Integer getPageSize() {
    return pageSize;
  }

  public void setPageSize(Integer pageSize) {
    this.pageSize = pageSize;
  }

  public String getOrderByColumn() {
    return orderByColumn;
  }

  public void setOrderByColumn(String orderByColumn) {
    this.orderByColumn = orderByColumn;
  }

  public String getIsAsc() {
    return isAsc;
  }

  public void setIsAsc(String isAsc) {
    if (StringUtils.isNotEmpty(isAsc)) {
      // 兼容前端排序类型
      if ("ascending".equals(isAsc)) {
        isAsc = "asc";
      } else if ("descending".equals(isAsc)) {
        isAsc = "desc";
      }
      this.isAsc = isAsc;
    }
  }

  public Boolean getReasonable() {
    if (StringUtils.isNull(reasonable)) {
      return Boolean.TRUE;
    }
    return reasonable;
  }

  public void setReasonable(Boolean reasonable) {
    this.reasonable = reasonable;
  }
}
