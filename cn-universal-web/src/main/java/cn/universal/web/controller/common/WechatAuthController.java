package cn.universal.web.controller.common;

import cn.universal.common.domain.R;
import cn.universal.security.wechat.WechatAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/wechat")
public class WechatAuthController {
  private final WechatAuthService auth;

  public WechatAuthController(WechatAuthService auth) { this.auth = auth; }

  @GetMapping("/options")
  public R<Map<String, Object>> options(@RequestParam(required = false) String tenantCode,
      HttpServletResponse response) {
    noStore(response);
    return R.ok(auth.options(tenantCode));
  }

  @PostMapping("/sessions")
  public R<Map<String, Object>> create(@RequestBody(required = false) LoginRequest body,
      HttpServletRequest request, HttpServletResponse response) {
    noStore(response);
    // 仅使用连接来源，不信任客户端自行传入的 X-Forwarded-For。
    return R.ok(auth.createLogin(body == null ? null : body.tenantCode(), request.getRemoteAddr()));
  }

  @GetMapping("/sessions/{id}")
  public R<Map<String, Object>> status(@PathVariable String id,
      @RequestHeader("X-Wechat-Proof") String proof, HttpServletResponse response) {
    noStore(response);
    return R.ok(auth.status(id, proof));
  }

  @DeleteMapping("/sessions/{id}")
  public R<Void> cancel(@PathVariable String id, @RequestHeader("X-Wechat-Proof") String proof,
      HttpServletResponse response) {
    noStore(response);
    auth.cancel(id, proof);
    return R.ok();
  }

  @PostMapping("/sessions/{id}/ws-ticket")
  public R<Map<String, String>> ticket(@PathVariable String id,
      @RequestHeader("X-Wechat-Proof") String proof, HttpServletResponse response) {
    noStore(response);
    return R.ok(Map.of("ticket", auth.createWebSocketTicket(id, proof)));
  }

  private void noStore(HttpServletResponse response) {
    response.setHeader("Cache-Control", "no-store");
    response.setHeader("Pragma", "no-cache");
  }

  public record LoginRequest(String tenantCode) {}
}
