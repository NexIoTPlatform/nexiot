package cn.universal.web.controller.common;

import cn.universal.persistence.entity.WechatLoginConfig;
import cn.universal.security.wechat.WechatAccountConfigService;
import cn.universal.security.wechat.WechatAuthService;
import cn.universal.security.wechat.WechatCallbackCodec;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wechat/callback")
public class WechatCallbackController {
  private static final Logger log = LoggerFactory.getLogger(WechatCallbackController.class);
  private final WechatAccountConfigService configs;
  private final WechatCallbackCodec codec;
  private final WechatAuthService auth;

  public WechatCallbackController(WechatAccountConfigService configs, WechatCallbackCodec codec,
      WechatAuthService auth) {
    this.configs = configs;
    this.codec = codec;
    this.auth = auth;
  }

  @GetMapping("/{tenantCode}")
  public ResponseEntity<String> verify(@PathVariable String tenantCode,
      @RequestParam(required = false) String signature, @RequestParam(required = false) String timestamp,
      @RequestParam(required = false) String nonce, @RequestParam(required = false) String echostr,
      @RequestParam(required = false) String msg_signature, @RequestParam(required = false) String encrypt_type) {
    long started = System.nanoTime();
    String phase = "config_lookup";
    String mode = "UNDETERMINED";
    logCallback(tenantCode, "GET", "received", phase, "unchecked", mode, 0, started, 0, null);
    try {
      WechatLoginConfig config = configs.findByTenantCode(tenantCode);
      if (config == null) {
        logCallback(tenantCode, "GET", "rejected", phase, "unchecked", mode, 0, started, 404, null);
        return text(404, "not configured");
      }
      phase = "verify_url";
      String echo = codec.verifyUrl(config.getCallbackToken(), config.getEncodingAESKey(), config.getAppId(),
          signature, msg_signature, timestamp, nonce, encrypt_type, echostr);
      mode = encrypt_type != null || msg_signature != null ? "ENCRYPTED" : "PLAINTEXT";
      logCallback(tenantCode, "GET", "processed", phase, "verified", mode, 0, started, 200, null);
      return text(200, echo);
    } catch (IllegalArgumentException e) {
      logCallback(tenantCode, "GET", "rejected", phase,
          "verify_url".equals(phase) ? "failed" : "unchecked", mode, 0, started, 403, e);
      return text(403, "invalid signature");
    } catch (RuntimeException e) {
      logCallback(tenantCode, "GET", "error", phase, "unchecked", mode, 0, started, 503, e);
      return text(503, "temporarily unavailable");
    }
  }

  @PostMapping("/{tenantCode}")
  public ResponseEntity<String> receive(@PathVariable String tenantCode,
      @RequestParam(required = false) String signature, @RequestParam(required = false) String msg_signature,
      @RequestParam(required = false) String timestamp, @RequestParam(required = false) String nonce,
      @RequestParam(required = false) String encrypt_type,
      HttpServletRequest request) {
    long started = System.nanoTime();
    long bytes = Math.max(-1, request.getContentLengthLong());
    String phase = "config_lookup";
    String security = "unchecked";
    String mode = "UNDETERMINED";
    logCallback(tenantCode, "POST", "received", phase, security, mode, bytes, started, 0, null);
    try {
      WechatLoginConfig config = configs.findByTenantCode(tenantCode);
      if (config == null) {
        logCallback(tenantCode, "POST", "rejected", phase, security, mode, bytes, started, 404, null);
        return text(404, "not configured");
      }
      phase = "read_body";
      byte[] raw = request.getInputStream().readNBytes(65537);
      bytes = raw.length;
      if (raw.length > 65536) {
        logCallback(tenantCode, "POST", "rejected", phase, security, mode, bytes, started, 413, null);
        return text(413, "message too large");
      }
      phase = "decode_message";
      WechatCallbackCodec.DecodedMessage decoded = codec.decodeMessage(config.getCallbackToken(),
          config.getEncodingAESKey(), config.getAppId(), signature, msg_signature, timestamp, nonce,
          encrypt_type, new String(raw, StandardCharsets.UTF_8));
      Map<String, String> message = decoded.message();
      security = "verified";
      mode = decoded.encrypted() ? "ENCRYPTED" : "PLAINTEXT";
      logCallback(tenantCode, "POST", "decoded", phase, security, mode, bytes, started, 0, null);
      phase = "process_message";
      String reply = auth.handleMessage(config, message);
      if (reply == null) {
        logCallback(tenantCode, "POST", "processed", phase, security, mode, bytes, started, 200, null);
        return text(200, "success");
      }
      phase = "build_reply";
      ResponseEntity<String> response = ResponseEntity.ok().header("Cache-Control", "no-store")
          .contentType(new MediaType("application", "xml", StandardCharsets.UTF_8))
          .body(codec.textReply(decoded, reply, config.getCallbackToken(), config.getEncodingAESKey(), config.getAppId()));
      logCallback(tenantCode, "POST", "processed", phase, security, mode, bytes, started, 200, null);
      return response;
    } catch (IllegalArgumentException e) {
      logCallback(tenantCode, "POST", "rejected", phase,
          "decode_message".equals(phase) ? "failed" : security, mode, bytes, started, 403, e);
      return text(403, "invalid message");
    } catch (Exception e) {
      // 状态未可靠保存时返回失败供微信重试，不记录含身份信息的原文/签名。
      logCallback(tenantCode, "POST", "error", phase, security, mode, bytes, started, 503, e);
      return text(503, "temporarily unavailable");
    }
  }

  private void logCallback(String tenantCode, String method, String stage, String phase,
      String security, String mode, long bytes, long started, int httpStatus, Exception exception) {
    // Only fixed diagnostic fields are logged. Never pass message content or a Throwable to SLF4J.
    String tenant = tenantCode != null && tenantCode.matches("[A-Za-z0-9_-]{1,64}")
        ? tenantCode : "invalid";
    long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    String exceptionType = exception == null ? "none" : exception.getClass().getSimpleName();
    String reason = exception instanceof WechatCallbackCodec.ValidationException validation
        ? validation.getReason().name() : "OTHER";
    String format = "wechat_callback tenant={} method={} stage={} phase={} security={} mode={} bytes={} elapsedMs={} httpStatus={} exception={} reason={}";
    if ("rejected".equals(stage) || "error".equals(stage)) {
      log.warn(format, tenant, method, stage, phase, security, mode, bytes, elapsedMs, httpStatus, exceptionType, reason);
    } else {
      log.info(format, tenant, method, stage, phase, security, mode, bytes, elapsedMs, httpStatus, exceptionType, reason);
    }
  }

  private ResponseEntity<String> text(int status, String body) {
    return ResponseEntity.status(status).header("Cache-Control", "no-store")
        .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8)).body(body);
  }
}
