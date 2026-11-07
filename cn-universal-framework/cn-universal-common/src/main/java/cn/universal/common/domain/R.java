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

package cn.universal.common.domain;

import cn.universal.common.constant.IoTConstant;
import org.slf4j.MDC;

/**
 * @Author gitee.com/NexIoT
 */
public class R<T> {

  private static final long serialVersionUID = 1L;
  private Integer code;
  private String msg;
  private T data;
  private String requestId;

  public static final Integer SUCCESS = 0;
  public static final Integer ERROR = 500;
  public static final String ERROR_MSG = "error";
  public static final String SUCCESS_MSG = "success";

  private R(int code, String msg) {
    this.requestId = MDC.get(IoTConstant.TRACE_ID);
    this.code = code;
    this.msg = msg;
  }

  public R() {
    this.requestId = MDC.get(IoTConstant.TRACE_ID);
  }

  private R(int code, String msg, T data) {
    this.requestId = MDC.get(IoTConstant.TRACE_ID);
    this.code = code;
    this.msg = msg;
    this.data = data;
  }

  public static R<Void> ok() {
    return ok(SUCCESS_MSG);
  }

  public static <T> R<T> ok(T data) {
    return ok(SUCCESS_MSG, data);
  }

  public static R<Void> ok(String msg) {
    return new R<>(SUCCESS, msg, null);
  }

  public static <T> R<T> ok(String msg, T data) {
    return new R<>(SUCCESS, msg, data);
  }

  public static <T> R<T> ok(int code, String msg, T data) {
    return new R<>(code, msg, data);
  }

  public static R<Void> error() {
    return error(ERROR_MSG);
  }

  public static R<Void> error(String msg) {
    return new R<>(ERROR, msg, null);
  }

  public static <T> R<T> error(String msg, T data) {
    return new R<>(ERROR, msg, data);
  }

//  public static <T> R<T> error(String msg) {
//    return new R<>(ERROR, msg, null);
//  }

  public static <T> R<T> error(int code, String msg, T data) {
    return new R<>(code, msg, data);
  }

  public static R<Void> error(int code, String msg) {
    return new R<>(code, msg, null);
  }

  public static R<Void> toAjax(int rows) {
    if (rows > 0) {
      return ok();
    } else {
      return error();
    }
  }

  /**
   * 判断当前状态是否为成功
   *
   * @return true表示成功，false表示失败
   */
  public boolean isSuccess() {
    return SUCCESS.equals(this.code);
  }

  /**
   * 判断当前状态是否为失败
   *
   * @return true表示失败，false表示成功
   */
  public boolean isError() {
    return !isSuccess();
  }

  /**
   * 判断当前状态是否为失败（与isError()方法相同，提供更直观的命名）
   *
   * @return true表示失败，false表示成功
   */
  public boolean isFailed() {
    return isError();
  }

  public Integer getCode() {
    return code;
  }

  public String getMsg() {
    return msg;
  }

  public T getData() {
    return data;
  }

  public void setCode(Integer code) {
    this.code = code;
  }

  public void setMsg(String msg) {
    this.msg = msg;
  }

  public void setData(T data) {
    this.data = data;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }
}
