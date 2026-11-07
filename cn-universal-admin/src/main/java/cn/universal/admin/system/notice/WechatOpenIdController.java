package cn.universal.admin.system.notice;

import cn.universal.manager.notice.model.NoticeChannel;
import cn.universal.manager.notice.service.NoticeChannelService;
import cn.universal.manager.notice.service.WechatOpenIdService;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/v1/notice/wechat")
public class WechatOpenIdController extends NoticeScopedController {
  private final NoticeChannelService channels;
  private final WechatOpenIdService service;

  public WechatOpenIdController(NoticeChannelService channels, WechatOpenIdService service) {
    this.channels = channels;
    this.service = service;
  }

  @Operation(summary = "生成微信 OpenID 授权地址")
  @GetMapping("/openid/url")
  public AjaxResult url(
      @RequestParam Long channelId,
      @RequestParam String redirectUri,
      @RequestParam(required = false) String state) {
    requirePermission("notice:channel:edit", "notice:template:edit");
    NoticeChannel channel = channels.getById(channelId);
    if (channel == null || !"wechat".equalsIgnoreCase(channel.getChannelType()))
      return AjaxResult.error("微信渠道不存在");
    if (!canAccessNotice(currentTenantUser(), channel, channel.getCreator()))
      return AjaxResult.error("无权限访问该渠道");
    return AjaxResult.success(Map.of("url", service.authorizationUrl(channel, redirectUri, state)));
  }

  @Operation(summary = "使用授权 code 获取用户 OpenID")
  @GetMapping("/openid/exchange")
  public AjaxResult exchange(
      @RequestParam Long channelId,
      @RequestParam String code,
      @RequestParam(required = false) String userKey) {
    requirePermission("notice:channel:edit", "notice:template:edit");
    NoticeChannel channel = channels.getById(channelId);
    if (channel == null) return AjaxResult.error("渠道不存在");
    if (!canAccessNotice(currentTenantUser(), channel, channel.getCreator()))
      return AjaxResult.error("无权限访问该渠道");
    String openid = service.exchange(channel, code);
    if (userKey != null) service.bind(channelId, userKey, openid);
    return AjaxResult.success(Map.of("openid", openid));
  }
}
