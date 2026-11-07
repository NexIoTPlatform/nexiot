package cn.universal.admin.system.notice;

import cn.universal.manager.notice.model.NoticeChannel;
import cn.universal.manager.notice.service.NoticeChannelService;
import cn.universal.manager.notice.service.WechatOpenIdService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 使用平台用户/应用 JWT 鉴权的微信授权接口。 */
@RestController
@RequestMapping("/open-api/v1/notice/wechat")
public class WechatOpenApiController extends NoticeApiAccess {
  private final NoticeChannelService channels;
  private final WechatOpenIdService service;

  public WechatOpenApiController(NoticeChannelService channels, WechatOpenIdService service) {
    this.channels = channels;
    this.service = service;
  }

  @GetMapping("/openid/url")
  public Map<String, String> url(
      @RequestParam Long channelId,
      @RequestParam String redirectUri,
      @RequestParam(required = false) String state) {
    return Map.of("url", service.authorizationUrl(channel(channelId), redirectUri, state));
  }

  @GetMapping("/openid/exchange")
  public Map<String, String> exchange(
      @RequestParam Long channelId,
      @RequestParam String code,
      @RequestParam(required = false) String userKey) {
    String openid = service.exchange(channel(channelId), code);
    if (userKey != null) service.bind(channelId, userKey, openid);
    return Map.of("openid", openid);
  }

  private NoticeChannel channel(Long id) {
    requireApiPermission("notice:channel:edit", "notice:template:edit");
    NoticeChannel c = channels.getById(id);
    if (c == null || !"wechat".equalsIgnoreCase(c.getChannelType()))
      throw new IllegalArgumentException("微信渠道不存在");
    requireNoticeAccess(c, c.getCreator());
    return c;
  }
}
