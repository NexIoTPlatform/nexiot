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

import cn.universal.common.constant.IoTConstant;
import com.github.pagehelper.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;
import lombok.Data;
import org.slf4j.MDC;

/**
 * @Author gitee.com/NexIoT
 *
 * @since 2018年12月17日 上午11:38
 */
@Data
@Schema(description = "分页统一对象")
public class PageRet<T> implements Serializable {

  @Schema(description = "错误编码 正常返回0")
  private Integer code;

  @Schema(description = "处理结果信息")
  private String msg;

  @Schema(description = "返回结果")
  private List<T> data;

  @Schema(description = "页码，从1开始")
  private int page;

  @Schema(description = "页面大小")
  private int size;

  @Schema(description = "总数")
  private long total;

  @Schema(description = "总页数")
  private int pages;

  @Schema(description = "请求Id")
  private String requestId;

  public static final Integer SUCCESS = 0;
  public static final Integer ERROR = 500;
  public static final String ERROR_MSG = "error";
  public static final String SUCCESS_MSG = "success";

  private PageRet(Page<T> page) {
    this.code = SUCCESS;
    this.msg = SUCCESS_MSG;
    this.page = page.getPageNum();
    this.size = page.getPageSize();
    this.total = page.getTotal();
    this.pages = page.getPages();
    this.data = page.getResult();
    this.requestId = MDC.get(IoTConstant.TRACE_ID);
  }

  private PageRet(List<T> data, String msg, Integer code) {
    this.code = code;
    this.msg = msg;
    this.data = data;
    this.requestId = MDC.get(IoTConstant.TRACE_ID);
  }

  public PageRet(List<T> data, int page, int size, long total, int pages) {
    this.code = SUCCESS;
    this.msg = SUCCESS_MSG;
    this.data = data;
    this.page = page;
    this.size = size;
    this.total = total;
    this.pages = pages;
    this.requestId = MDC.get(IoTConstant.TRACE_ID);
  }

  public static <T> PageRet<T> ok(Page<T> page) {
    return new PageRet<>(page);
  }

  public static <T> PageRet<T> ok(PageBean<T> page) {
    return new PageRet<>(
        page.getList(),
        page.getCurrPage(),
        page.getPageSize(),
        page.getTotalCount(),
        page.getTotalPage());
  }

  public PageRet(PageBean<T> page) {
    this.code = SUCCESS;
    this.msg = SUCCESS_MSG;
    this.page = page.getCurrPage();
    this.size = page.getPageSize();
    this.total = page.getTotalCount();
    this.pages = page.getTotalPage();
    this.data = page.getList();
    this.requestId = MDC.get(IoTConstant.TRACE_ID);
  }

  //  public static <T> PageRet<T> error(List<T> data){
  //    return new PageRet<>(data,ERROR_MSG,ERROR);
  //  }
  //  public static <T> PageRet<T> error(){
  //    return new PageRet<>(null,ERROR_MSG,ERROR);
  //  }
  //  public static <T> PageRet<T> success(List<T> data){
  //    return new PageRet<>(data,SUCCESS_MSG,SUCCESS);
  //  }
  //  public static <T> PageRet<T> success(){
  //    return new PageRet<>(null,SUCCESS_MSG,SUCCESS);
  //  }
  //  public static <T> PageRet<T> toAjax(int rows){
  //    if(rows>0)return success();
  //    else return error();
  //  }

}
