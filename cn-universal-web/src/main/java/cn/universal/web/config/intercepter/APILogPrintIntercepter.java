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

package cn.universal.web.config.intercepter;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.utils.JsonDesensitizationUtil;
import cn.universal.web.config.log.RequestWrapper;
import cn.universal.web.config.log.WebLog;
import cn.universal.web.config.log.WebLogKits;
import cn.universal.web.context.TtlAuthContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.WebUtils;

@Component
@Order(100)
public class APILogPrintIntercepter implements HandlerInterceptor {

  private static final Logger log = LoggerFactory.getLogger(APILogPrintIntercepter.class);

  // 使用ScopedValue替代ThreadLocal，自动管理生命周期
  private static final ThreadLocal<WebLog> WEB_LOG_SCOPE = ThreadLocal.withInitial(() -> null);

  @Value(value = "${logback.enable:true}")
  private boolean logEnable;

  @Value(value = "${logback.weblog.resp:true}")
  private boolean logRespEnable;

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    long startTime = System.currentTimeMillis();
    String principal = TtlAuthContextHolder.getInstance().getContext();
    WebLog webLog = WebLog.builder().entryTime(startTime).build();
    JSONObject reqParams;
    if (isWechatAuthenticationRequest(request)) {
      // Do not parse authentication bodies: even malformed JSON must never reach raw-body logs.
      reqParams = new JSONObject().set("payload", "[微信认证内容已省略]");
    } else {
      reqParams = WebLogKits.getParameters(request);
      JSONObject reqJsonData = safeJsonData(request);
      reqParams.putAll(reqJsonData);
    }
    webLog.setReqParams(omitSipCredentialText(JsonDesensitizationUtil.desensitizeJsonObject(reqParams)).toString());
    JSONObject headersInfo = WebLogKits.getHeadersInfo(request);
    headersInfo.replaceAll((name, value) -> "authorization".equalsIgnoreCase(name) ? "***" : value);
    headersInfo.set("principal", principal);
    webLog.setHeaderInfo(headersInfo.toString());

    // 使用ScopedValue设置WebLog
    WEB_LOG_SCOPE.set(webLog);

    return true;
  }

  private boolean isWechatAuthenticationRequest(HttpServletRequest request) {
    String path = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
      path = path.substring(contextPath.length());
    }
    if (path.startsWith("/api/")) {
      path = path.substring(4);
    }
    return path.equals("/auth/wechat")
        || path.startsWith("/auth/wechat/")
        || path.startsWith("/wechat/callback/")
        || path.equals("/admin/system/wechat/config")
        || path.equals("/admin/system/profile/wechat")
        || path.startsWith("/admin/system/profile/wechat/")
        || ((path.equals("/oauth/token") || path.equals("/oauth2/token"))
            && "wechat_qr".equals(request.getParameter("grant_type")));
  }

  private JSONObject safeJsonData(HttpServletRequest request) {
    RequestWrapper wrapper = WebUtils.getNativeRequest(request, RequestWrapper.class);
    String body = wrapper == null ? null : wrapper.getBodyString();
    if (containsSipCredential(body)) {
      // Avoid the legacy parser's raw-body error log when credential-bearing JSON is malformed.
      JSONObject safe = JsonDesensitizationUtil.desensitize(body);
      return safe == null ? new JSONObject() : safe;
    }
    return WebLogKits.getJsonData(request);
  }

  private Object omitSipCredentialText(Object value) {
    if (value instanceof JSONObject object) object.replaceAll((name, nested) -> omitSipCredentialText(nested));
    else if (value instanceof JSONArray array) {
      for (int index = 0; index < array.size(); index++) array.set(index, omitSipCredentialText(array.get(index)));
    } else if (value instanceof String text && containsSipCredential(text)) return "[SIP配置已省略]";
    return value;
  }

  private boolean containsSipCredential(String text) {
    return text != null && (text.contains("sipPassword") || text.contains("sipEslPassword"));
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    try {
      WebLog webLog = WEB_LOG_SCOPE.get();
      webLog.setReturnTime(System.currentTimeMillis());
      webLog.setTraceId(MDC.get(IoTConstant.TRACE_ID));
      webLog.log();
    } catch (IllegalStateException e) {
      log.warn("WebLog未设置，跳过日志记录");
    } finally {
      WEB_LOG_SCOPE.remove();
    }
    // ScopedValue会自动清理，无需手动remove
  }
}
