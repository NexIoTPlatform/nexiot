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
import lombok.Data;

/**
 * @Author gitee.com/NexIoT
 *
 * @since 2018年12月17日 上午11:38
 */
@Data
@Schema(description = "返回统一对象")
public class AjaxResult<T> {

  private static final long serialVersionUID = 1L;

  @Schema(description = "错误编码 正常返回0")
  private Integer code;

  @Schema(description = "处理结果信息")
  private String msg;

  @Schema(description = "返回结果")
  private T data;

  public static final Integer SUCCESS = 0;
  public static final Integer ERROR = 500;
  public static final String ERROR_MSG = "error";
  public static final String SUCCESS_MSG = "success";

  private AjaxResult(int code, String msg) {
    this.code = code;
    this.msg = msg;
  }

  private AjaxResult(int code, String msg, T data) {
    this.code = code;
    this.msg = msg;
    this.data = data;
  }

  public static AjaxResult<Void> success() {
    return success(SUCCESS_MSG);
  }

  public static <T> AjaxResult<T> success(T data) {
    return success(SUCCESS_MSG, data);
  }

  public static AjaxResult<Void> success(String msg) {
    return new AjaxResult<>(SUCCESS, msg, null);
  }

  public static <T> AjaxResult<T> success(String msg, T data) {
    return new AjaxResult<>(SUCCESS, msg, data);
  }

  public AjaxResult() {}

  public static AjaxResult<Void> error() {
    return error(ERROR_MSG);
  }

  public static AjaxResult<Void> error(String msg) {
    return new AjaxResult<>(ERROR, msg, null);
  }

  public static <T> AjaxResult<T> error(String msg, T data) {
    return new AjaxResult<>(ERROR, msg, data);
  }

  public static AjaxResult<Void> error(int code, String msg) {
    return new AjaxResult<>(code, msg, null);
  }

  public static AjaxResult<Void> toAjax(int rows) {
    if (rows > 0) {
      return success();
    } else {
      return error();
    }
  }
}
