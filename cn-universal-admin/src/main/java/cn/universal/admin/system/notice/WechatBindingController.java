package cn.universal.admin.system.notice;

import cn.universal.manager.notice.model.NoticeChannel;
import cn.universal.manager.notice.service.NoticeChannelService;
import cn.universal.manager.notice.service.WechatBindingSessionService;
import cn.universal.manager.notice.service.WechatOpenIdService;
import cn.universal.persistence.query.AjaxResult;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/admin/v1/notice/wechat/bind", "/open-api/v1/notice/wechat/bind"})
public class WechatBindingController extends NoticeApiAccess {
  private final NoticeChannelService channels;
  private final WechatBindingSessionService sessions;
  private final WechatOpenIdService openIds;
  public WechatBindingController(NoticeChannelService channels, WechatBindingSessionService sessions, WechatOpenIdService openIds) {
    this.channels = channels; this.sessions = sessions; this.openIds = openIds;
  }
  public record BindRequest(Long channelId, String userKey) {}
  private NoticeChannel channel(Long id) {
    requireApiPermission("notice:channel:edit", "notice:template:edit");
    NoticeChannel channel = id == null ? null : channels.getById(id);
    if (channel == null || !"wechat".equals(channel.getChannelType())) throw new IllegalArgumentException("微信渠道不存在");
    requireNoticeAccess(channel, channel.getCreator());
    if ("1".equals(channel.getStatus())) throw new IllegalArgumentException("通知渠道已停用");
    return channel;
  }
  @PostMapping("/session") public AjaxResult create(@RequestBody BindRequest request) {
    NoticeChannel channel = channel(request.channelId());
    return AjaxResult.success(sessions.create(channel, request.userKey(), noticeApiUser().getUnionId()));
  }
  @GetMapping("/session/{id}") public AjaxResult status(@PathVariable String id) {
    Map<String, Object> status = sessions.status(id, noticeApiUser().getUnionId());
    channel((Long) status.get("channelId"));
    return AjaxResult.success(status);
  }
  @GetMapping("/openid") public AjaxResult resolve(@RequestParam Long channelId, @RequestParam String userKey) {
    channel(channelId);
    String openid = openIds.resolve(channelId, userKey);
    return AjaxResult.success(Map.of("bound", openid != null, "openid", openid == null ? "" : openid, "receiver", "user:" + userKey));
  }
}
