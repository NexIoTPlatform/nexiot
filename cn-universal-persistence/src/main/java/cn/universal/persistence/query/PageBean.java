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

package cn.universal.persistence.query;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 分页工具类 @Title: @Description: @Author:刘利海 @Since:2018年9月26日 */
@Schema(description = "分页数据封装类")
public class PageBean<T> extends DataTransferObject {

  private static final long serialVersionUID = 1L;

  // 总记录数
  @Schema(description = "总记录数")
  private Long totalCount;

  // 每页记录数
  @Schema(description = "每页记录数")
  private int pageSize;

  // 总页数
  @Schema(description = "总页数")
  private int totalPage;

  // 当前页数
  @Schema(description = "当前页数")
  private int currPage;

  // 列表数据
  @Schema(description = "数据列表")
  private List<T> list;

  private boolean hasMore;
  private String nextCursor;

  public boolean isHasMore() {
    return hasMore;
  }

  public void setHasMore(boolean value) {
    hasMore = value;
  }

  public String getNextCursor() {
    return nextCursor;
  }

  public void setNextCursor(String value) {
    nextCursor = value;
  }

  public PageBean() {}

  /**
   * 分页
   *
   * @param list 列表数据
   * @param totalCount 总记录数
   * @param pageSize 每页记录数
   * @param currPage 当前页数
   */
  public PageBean(List<T> list, Long totalCount, int pageSize, int currPage) {
    this.list = list;
    this.totalCount = totalCount;
    this.pageSize = pageSize;
    this.currPage = currPage;
    this.totalPage = (int) Math.ceil((double) totalCount / pageSize);
  }

  public Long getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(Long totalCount) {
    this.totalCount = totalCount;
  }

  public int getPageSize() {
    return pageSize;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  public int getTotalPage() {
    return totalPage;
  }

  public void setTotalPage(int totalPage) {
    this.totalPage = totalPage;
  }

  public int getCurrPage() {
    return currPage;
  }

  public void setCurrPage(int currPage) {
    this.currPage = currPage;
  }

  public List<T> getList() {
    return list;
  }

  public void setList(List<T> list) {
    this.list = list;
  }
}
