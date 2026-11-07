/*
 * 请求签名校验拦截器
 */
package cn.universal.web.config.intercepter;

import cn.hutool.json.JSONUtil;
import cn.universal.common.domain.R;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 前后端请求签名校验拦截器
 *
 * <p>前端在 /api/** 和 /admin/** 请求头中携带： X-Sign-Timestamp, X-Sign-Nonce, X-Signature
 *
 * <p>服务端按相同规则使用 HMAC-SHA256 验签，并做时间窗口与 nonce 防重放校验。
 */
@Component
@Order(50)
@Slf4j
public class RequestSignatureInterceptor implements HandlerInterceptor {

  /** enable 开启 */
  @Value("${security.request-sign.enable:true}")
  private boolean enable;

  /** 签名密钥，从配置读取，需与前端 VUE_APP_SIGN_SECRET 一致 */
  @Value("${security.request-sign.secret:nexiot}")
  private String signSecret;

  /** 允许的过去时间偏移（秒）- 防止请求延迟 */
  @Value("${security.request-sign.past-skew-seconds:3600}")
  private long ALLOWED_PAST_SKEW_SECONDS;

  /** 允许的未来时间偏移（秒）- 防止客户端时间超前/重放未来请求 */
  @Value("${security.request-sign.future-skew-seconds:7200}")
  private long ALLOWED_FUTURE_SKEW_SECONDS;

  /** 签名排除路径配置（支持配置文件注入） */
  private final SignatureExcludePathConfig signatureExcludePathConfig;

  public RequestSignatureInterceptor(SignatureExcludePathConfig signatureExcludePathConfig) {
    this.signatureExcludePathConfig = signatureExcludePathConfig;
  }

  /** Nonce缓存：使用Caffeine替代Map，自动管控内存和过期 */
  private final Cache<String, Long> nonceCache =
      Caffeine.newBuilder()
          .maximumSize(100_000) // 页面并发请求较多，避免 5 分钟窗口内过早淘汰 nonce。
          .expireAfterWrite(300, TimeUnit.SECONDS) // 与时间窗口一致，5分钟过期
          .build();

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    String uri = request.getRequestURI();
    String clientIp = request.getRemoteAddr(); // 记录客户端IP，便于日志排查

    if (!Boolean.TRUE.equals(enable)) {
      return true;
    }
    // 仅拦截 /api/** 和 /admin/**
    if (!(uri.startsWith("/api/") || uri.startsWith("/admin/"))) {
      return true;
    }
    // 使用统一的排除路径配置来检查是否需要跳过签名校验
    // 包括：文件上传、数据导出、其他白名单接口等
    if (signatureExcludePathConfig.isExcludePath(uri)) {
      log.debug("[签名校验] 路径已在白名单中，跳过签名校验: uri={}", uri);
      return true;
    }

    // 未配置签名密钥时，不做校验
    if (signSecret == null || signSecret.isEmpty()) {
      return true;
    }

    String tsHeader = request.getHeader("X-Sign-Timestamp");
    String nonce = request.getHeader("X-Sign-Nonce");
    String signature = request.getHeader("X-Signature");

    // ========== 1. 基础参数校验（错误信息模糊化） ==========
    if (isBlank(tsHeader) || isBlank(nonce) || isBlank(signature)) {
      log.warn(
          "[签名校验-{}] 缺少签名头: uri={}, ip={}, ts={}, nonce={}",
          clientIp,
          uri,
          clientIp,
          tsHeader,
          nonce);
      writeError(response, 403, "请求验证失败，请重试"); // 修改：401 → 403
      return false;
    }

    long nowSeconds = Instant.now().getEpochSecond();
    long ts;
    try {
      ts = Long.parseLong(tsHeader);
    } catch (NumberFormatException e) {
      log.warn("[签名校验-{}] 时间戳格式错误: uri={}, ts={}, 异常={}", clientIp, uri, tsHeader, e.getMessage());
      writeError(response, 403, "请求验证失败，请重试"); // 修改：401 → 403
      return false;
    }

    // ========== 2. 强化时间戳校验（错误信息模糊化） ==========
    // 禁止未来时间请求
    if (ts > nowSeconds + ALLOWED_FUTURE_SKEW_SECONDS) {
      log.warn(
          "[签名校验-{}] 时间戳超出未来窗口: uri={}, ts={}, now={}, 允许未来偏移={}秒",
          clientIp,
          uri,
          ts,
          nowSeconds,
          ALLOWED_FUTURE_SKEW_SECONDS);
      writeError(response, 403, "请求验证失败，请重试"); // 修改：401 → 403
      return false;
    }

    // 禁止过期时间请求
    if (ts < nowSeconds - ALLOWED_PAST_SKEW_SECONDS) {
      log.warn(
          "[签名校验-{}] 时间戳超出过去窗口: uri={}, ts={}, now={}, 允许过去偏移={}秒",
          clientIp,
          uri,
          ts,
          nowSeconds,
          ALLOWED_PAST_SKEW_SECONDS);
      writeError(response, 403, "请求验证失败，请重试"); // 修改：401 → 403
      return false;
    }

    // ========== 3. Caffeine实现Nonce防重放（错误信息模糊化） ==========
    if (isReplayNonce(nonce, nowSeconds, clientIp, uri)) {
      log.warn("[签名校验-{}] 检测到重放请求: uri={}, nonce={}, ts={}", clientIp, uri, nonce, ts);
      writeError(response, 403, "请求验证失败，请重试"); // 修改：401 → 403
      return false;
    }

    // ========== 4. 签名校验（错误信息模糊化） ==========
    String method = request.getMethod().toUpperCase();
    String path = uri;
    String query = request.getQueryString();
    if (query == null) {
      query = "";
    }
    // 解码query，防止编码不一致导致验签失败
    query = URLDecoder.decode(query, StandardCharsets.UTF_8.name());

    String payload = String.join("\n", method, path, query, tsHeader, nonce);
    String expectedSignature = hmacSha256Hex(payload, signSecret);

    // 详细调试日志（仅服务端可见，生产可通过日志级别控制）
    log.debug("[签名调试-{}] 校验详情 ===>", clientIp);
    log.debug("[签名调试-{}] URI: {}", clientIp, uri);
    log.debug("[签名调试-{}] Method: {}", clientIp, method);
    log.debug("[签名调试-{}] Path: {}", clientIp, path);
    log.debug("[签名调试-{}] Query: {}", clientIp, query);
    log.debug("[签名调试-{}] Timestamp: {} (当前时间: {})", clientIp, tsHeader, nowSeconds);
    log.debug("[签名调试-{}] Nonce: {}", clientIp, nonce);
    log.debug("[签名调试-{}] Payload: {}", clientIp, payload.replace("\n", "\\n"));
    log.debug("[签名调试-{}] Expected Signature: {}", clientIp, expectedSignature);
    log.debug("[签名调试-{}] Actual Signature: {}", clientIp, signature);
    // 打印前端调试头（如有）
    String frontendPayload = request.getHeader("X-Debug-Payload");
    if (frontendPayload != null && log.isDebugEnabled()) {
      log.debug(
          "[签名调试-{}] 前端Payload(解码后): {}",
          clientIp,
          URLDecoder.decode(frontendPayload, StandardCharsets.UTF_8.name()));
    }

    if (!expectedSignature.equals(signature)) {
      log.warn(
          "[签名校验-{}] 验签失败: uri={}, expected={}, actual={}",
          clientIp,
          uri,
          expectedSignature,
          signature);
      writeError(response, 403, "请求验证失败，请重试"); // 修改：401 → 403
      return false;
    }

    return true;
  }

  /** Caffeine实现Nonce防重放 */
  private boolean isReplayNonce(String nonce, long nowSeconds, String clientIp, String uri) {
    // 尝试获取Nonce：存在则说明是重放，不存在则存入（过期时间=当前时间+TTL）
    Long existingExpireTime = nonceCache.getIfPresent(nonce);
    if (existingExpireTime != null && existingExpireTime >= nowSeconds) {
      // Nonce已存在且未过期 → 重放请求
      log.debug(
          "[Nonce校验-{}] Nonce已存在且未过期: uri={}, nonce={}, 过期时间={}",
          clientIp,
          uri,
          nonce,
          existingExpireTime);
      return true;
    }

    // Nonce首次使用 → 存入缓存（过期时间=当前时间+5分钟）
    nonceCache.put(nonce, nowSeconds + ALLOWED_PAST_SKEW_SECONDS);
    log.debug(
        "[Nonce校验-{}] Nonce首次使用: uri={}, nonce={}, 过期时间={}",
        clientIp,
        uri,
        nonce,
        nowSeconds + ALLOWED_PAST_SKEW_SECONDS);
    return false;
  }

  /** HMAC-SHA256 加密 */
  private String hmacSha256Hex(String data, String key) {
    try {
      javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
      javax.crypto.spec.SecretKeySpec secretKeySpec =
          new javax.crypto.spec.SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      mac.init(secretKeySpec);
      byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
      return toHexUpper(raw);
    } catch (Exception e) {
      log.error("[签名加密] 计算签名失败: data={}, 异常={}", data, e.getMessage(), e);
      return "";
    }
  }

  /** 字节数组转大写十六进制 */
  private String toHexUpper(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      String hex = Integer.toHexString(b & 0xFF);
      if (hex.length() == 1) {
        sb.append('0');
      }
      sb.append(hex);
    }
    return sb.toString().toUpperCase();
  }

  /** 空值判断 */
  private boolean isBlank(String str) {
    return str == null || str.trim().isEmpty();
  }

  /** 写入模糊化错误响应（仅返回通用提示，不暴露具体原因） */
  private void writeError(HttpServletResponse response, int code, String msg) throws IOException {
    // 修改1：设置HTTP响应状态码为403（Forbidden），符合语义
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setCharacterEncoding("UTF-8");
    response.setContentType("application/json;charset=UTF-8");
    // 修改2：响应体业务码改为403，与HTTP状态码一致，便于前端识别
    String body = JSONUtil.toJsonStr(R.error(403, msg));
    response.getWriter().write(body);
  }
}
