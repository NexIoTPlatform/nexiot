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

package cn.universal.web.config;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.json.JSONUtil;
import cn.universal.common.domain.R;
import cn.universal.common.exception.IoTException;
import cn.universal.common.utils.DingTalkUtil;
import cn.universal.common.utils.ErrorUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * @Author 刘利海
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /** 未知异常处理 */
  @ExceptionHandler(value = Exception.class)
  @ResponseBody
  public R exceptionHandler(Exception e) {
    // 把错误信息输入到日志中
    log.error("未知异常: {}", e.getMessage(), e);
    return R.error("未知错误: " + e.getMessage());
  }

  /** 缺失接口和静态资源统一返回 404，避免被客户端误判为空响应成功。 */
  @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public R<Void> handleNotFoundException(HttpServletRequest request) {
    if (!request.getRequestURI().endsWith("favicon.ico")) {
      log.warn("Resource URI NoT Exist | 请求URI: {}", request.getRequestURI());
    }
    return R.error(404, "路径不存在，请检查路径是否正确");
  }

  /**
   * spring-validate处理
   *
   * @param exception 错误
   * @return 返回信息
   */
  @ExceptionHandler(value = {BindException.class, MethodArgumentNotValidException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Object bindExceptionHandle(Exception exception) {
    List<Map<String, String>> validationResultList = new ArrayList<>();
    Iterator iterator;
    if (exception instanceof BindException) {
      iterator = ((BindException) exception).getBindingResult().getFieldErrors().iterator();
    } else {
      iterator =
          ((MethodArgumentNotValidException) exception)
              .getBindingResult()
              .getFieldErrors()
              .iterator();
    }

    while (iterator.hasNext()) {
      FieldError error = (FieldError) iterator.next();
      Map<String, String> validationResult = new HashMap();
      validationResult.put("message", error.getDefaultMessage());
      validationResult.put("param", error.getField());
      validationResultList.add(validationResult);
    }
    log.error(JSONUtil.toJsonStr(validationResultList));
    return R.error(JSONUtil.toJsonStr(validationResultList));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public R handleNoMethodxception(HttpRequestMethodNotSupportedException e) {
    return R.error(e.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public R handleIllegalArgumentxception(IllegalArgumentException e) {
    return R.error(e.getMessage());
  }

  // @ExceptionHandler(DuplicateKeyException.class)
  // public DataRet<String> handleDuplicateKeyException(DuplicateKeyException e) {
  // log.error(e.getMessage(), e);
  // return DataRet.error("数据库中已存在该记录");
  // }

  @ExceptionHandler(RuntimeException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public R handleException(RuntimeException e) {
    log.error("运行异常: {}", e.getMessage(), e);
    return R.error("" + e.getMessage());
  }

  @ExceptionHandler(IoTException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public R handleException(IoTException e) {
    log.warn("biz异常={}", ExceptionUtil.getSimpleMessage(e));
    return R.error(e.getCode(), e.getMessage());
  }

  @ExceptionHandler(value = {DataAccessException.class, CannotGetJdbcConnectionException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public R handleMysql(Exception e) {
    log.error("数据库异常，请及时处理={}", ErrorUtil.errorInfoToString(e));
    DingTalkUtil.send("数据库异常，请及时处理" + ExceptionUtil.getSimpleMessage(e));
    return R.error("数据库异常，请联系管理员");
  }

  /** 处理认证异常 - 返回统一R对象格式 */
  @ExceptionHandler(AuthenticationException.class)
  @ResponseStatus(HttpStatus.OK)
  public R<Void> handleAuthenticationException(
      AuthenticationException exception, HttpServletRequest request, HttpServletResponse response) {
    log.warn("OAuth2 认证失败: {}", exception.getMessage());
    
    // 返回统一的R对象,使用401错误码
    String errorMsg = getErrorMessage(exception);
    return R.error(401, errorMsg);
  }

  /** 处理 OAuth2 认证异常 - 返回统一R对象格式 */
  @ExceptionHandler({OAuth2AuthenticationException.class, UsernameNotFoundException.class})
  @ResponseStatus(HttpStatus.OK)
  public R<Void> handleOAuth2AuthenticationException(
      OAuth2AuthenticationException exception,
      HttpServletRequest request,
      HttpServletResponse response) {

    log.warn("OAuth2 认证异常: {}", exception.getMessage());

    // 返回统一的R对象,使用401错误码
    OAuth2Error error = exception.getError();
    String errorMsg = error.getDescription() != null ? error.getDescription() : error.getErrorCode();
    return R.error(401, errorMsg);
  }

  /** 获取错误信息 */
  private String getErrorMessage(AuthenticationException exception) {
    if (exception instanceof BadCredentialsException) {
      return exception.getMessage();
    }

    // 默认错误信息
    return "认证失败: " + exception.getMessage();
  }
}
