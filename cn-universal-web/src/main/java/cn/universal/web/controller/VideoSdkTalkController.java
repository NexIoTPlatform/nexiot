package cn.universal.web.controller;

import cn.universal.common.domain.R;
import cn.universal.web.websocket.VideoSdkTalkTickets;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/video/platforms/{instanceKey}/sdk/talk")
public class VideoSdkTalkController {
  private final VideoSdkTalkTickets tickets;
  public VideoSdkTalkController(VideoSdkTalkTickets tickets) { this.tickets = tickets; }
  @PostMapping
  @PreAuthorize("@videoOperationAccess.allowed('talkStart')")
  public R<Map<String, Object>> start(@PathVariable String instanceKey, @RequestBody Map<String, Object> body) {
    return R.ok(tickets.create(instanceKey, body));
  }
  @PostMapping("/cancel")
  @PreAuthorize("@videoOperationAccess.allowed('talkStop')")
  public R<?> cancel(@PathVariable String instanceKey,@RequestBody Map<String, String> body) {
    tickets.cancel(instanceKey,body.get("token"));
    return R.ok();
  }
}
