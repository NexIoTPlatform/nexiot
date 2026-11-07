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

package cn.universal.common.exception;

/** 基础异常 */
public class BaseException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** 所属模块 */
  private String module;

  /** 错误码 */
  private String code;

  /** 错误码对应的参数 */
  private Object[] args;

  /** 错误消息 */
  private String defaultMessage;

  public BaseException(String module, String code, Object[] args, String defaultMessage) {
    this.module = module;
    this.code = code;
    this.args = args;
    this.defaultMessage = defaultMessage;
  }

  public BaseException(String module, String code, Object[] args) {
    this(module, code, args, null);
  }

  public BaseException(String module, String defaultMessage) {
    this(module, null, null, defaultMessage);
  }

  public BaseException(String code, Object[] args) {
    this(null, code, args, null);
  }

  public BaseException(String defaultMessage) {
    this(null, null, null, defaultMessage);
  }

  @Override
  public String getMessage() {
    return defaultMessage;
  }

  public String getModule() {
    return module;
  }

  public String getCode() {
    return code;
  }

  public Object[] getArgs() {
    return args;
  }

  public String getDefaultMessage() {
    return defaultMessage;
  }
}
