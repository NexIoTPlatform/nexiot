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

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/** 表格分页数据对象 @Author ruoyi */
@Schema(description = "分页响应对象")
@Data
public class TableDataInfo<T> implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 总记录数 */
  @Schema(description = "总记录数")
  private int total;

  /** 列表数据 */
  @Schema(description = "列表数据")
  private List<T> rows;

  /** 消息状态码 */
  @Schema(description = "消息状态码")
  private int code;

  /** 消息内容 */
  @Schema(description = "消息内容")
  private String msg;

  /** 表格数据对象 */
  public TableDataInfo() {}

  /**
   * 分页
   *
   * @param list 列表数据
   * @param total 总记录数
   */
  public TableDataInfo(List<T> list, int total) {
    this.rows = list;
    this.total = total;
  }
}
