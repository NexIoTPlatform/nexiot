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

package cn.universal.common.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.extra.servlet.JakartaServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 客户端工具类 @Author ruoyi */
public class ServletUtils extends JakartaServletUtil {

  /** 获取String参数 */
  public static String getParameter(String name) {
    return getRequest().getParameter(name);
  }

  /** 获取String参数 */
  public static String getParameter(String name, String defaultValue) {
    return Convert.toStr(getRequest().getParameter(name), defaultValue);
  }

  /** 获取Integer参数 */
  public static Integer getParameterToInt(String name) {
    return Convert.toInt(getRequest().getParameter(name));
  }

  /** 获取Integer参数 */
  public static Integer getParameterToInt(String name, Integer defaultValue) {
    return Convert.toInt(getRequest().getParameter(name), defaultValue);
  }

  /** 获取Boolean参数 */
  public static Boolean getParameterToBool(String name) {
    return Convert.toBool(getRequest().getParameter(name));
  }

  /** 获取Boolean参数 */
  public static Boolean getParameterToBool(String name, Boolean defaultValue) {
    return Convert.toBool(getRequest().getParameter(name), defaultValue);
  }

  /** 获取request */
  public static HttpServletRequest getRequest() {
    return getRequestAttributes().getRequest();
  }

  /** 获取response */
  public static HttpServletResponse getResponse() {
    return getRequestAttributes().getResponse();
  }

  /** 获取session */
  public static HttpSession getSession() {
    return getRequest().getSession();
  }

  public static ServletRequestAttributes getRequestAttributes() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    return (ServletRequestAttributes) attributes;
  }

  /**
   * 将字符串渲染到客户端
   *
   * @param response 渲染对象
   * @param string 待渲染的字符串
   * @return null
   */
  public static String renderString(HttpServletResponse response, String string) {
    try {
      response.setStatus(200);
      response.setContentType("application/json");
      response.setCharacterEncoding("utf-8");
      response.getWriter().print(string);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  /** 是否是Ajax异步请求 */
  public static boolean isAjaxRequest(HttpServletRequest request) {
    String accept = request.getHeader("accept");
    if (accept != null && accept.indexOf("application/json") != -1) {
      return true;
    }

    String xRequestedWith = request.getHeader("X-Requested-With");
    if (xRequestedWith != null && xRequestedWith.indexOf("XMLHttpRequest") != -1) {
      return true;
    }

    String uri = request.getRequestURI();
    if (StringUtils.inStringIgnoreCase(uri, ".json", ".xml")) {
      return true;
    }

    String ajax = request.getParameter("__ajax");
    if (StringUtils.inStringIgnoreCase(ajax, "json", "xml")) {
      return true;
    }
    return false;
  }

  public static String getClientIP() {
    return IPUtils.getIpAddr(getRequest());
  }
}
