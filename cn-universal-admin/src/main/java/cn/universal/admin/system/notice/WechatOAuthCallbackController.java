package cn.universal.admin.system.notice;

import cn.universal.manager.notice.service.WechatBindingSessionService;
import org.springframework.web.bind.annotation.*;

@RestController
public class WechatOAuthCallbackController {
  private final WechatBindingSessionService sessions;

  public WechatOAuthCallbackController(WechatBindingSessionService sessions) {
    this.sessions = sessions;
  }

  @GetMapping(value = "/open-api/v1/notice/wechat/callback", produces = "text/html;charset=UTF-8")
  public String callback(@RequestParam String state, @RequestParam(required = false) String code) {
    String message;
    try {
      sessions.complete(state, code);
      message = "微信绑定成功，请返回电脑查看 OpenID。接收模板通知前请关注该服务号。";
    } catch (Exception e) {
      message = "绑定未完成或二维码已过期，请返回电脑查看结果并重新生成二维码。";
    }
    return "<!doctype html><html lang='zh-CN'><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>微信绑定</title><body style='font-family:system-ui;padding:48px 24px;line-height:1.8'><h2>微信通知绑定</h2><p>"
        + message
        + "</p></body></html>";
  }
}
