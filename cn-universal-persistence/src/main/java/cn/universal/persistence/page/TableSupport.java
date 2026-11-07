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

import cn.universal.common.utils.ServletUtils;

/** 表格数据处理 @Author ruoyi */
public class TableSupport {

  /** 当前记录起始索引 */
  public static final String PAGE_NUM = "pageNum";

  /** 每页显示记录数 */
  public static final String PAGE_SIZE = "pageSize";

  /** 排序列 */
  public static final String ORDER_BY_COLUMN = "orderByColumn";

  /** 排序的方向 "desc" 或者 "asc". */
  public static final String IS_ASC = "isAsc";

  /** 分页参数合理化 */
  public static final String REASONABLE = "reasonable";

  /** 封装分页对象 */
  public static PageDomain getPageDomain() {
    PageDomain pageDomain = new PageDomain();
    pageDomain.setPageNum(ServletUtils.getParameterToInt(PAGE_NUM));
    pageDomain.setPageSize(ServletUtils.getParameterToInt(PAGE_SIZE));
    pageDomain.setOrderByColumn(ServletUtils.getParameter(ORDER_BY_COLUMN));
    pageDomain.setIsAsc(ServletUtils.getParameter(IS_ASC));
    pageDomain.setReasonable(ServletUtils.getParameterToBool(REASONABLE));
    return pageDomain;
  }

  public static PageDomain buildPageRequest() {
    return getPageDomain();
  }
}
