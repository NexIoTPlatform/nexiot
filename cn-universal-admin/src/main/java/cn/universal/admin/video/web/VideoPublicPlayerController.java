package cn.universal.admin.video.web;

import cn.universal.admin.video.service.VideoPlaybackTicketService;
import cn.universal.admin.video.service.VideoOpenApiSessionService;
import cn.universal.common.domain.R;
import cn.universal.common.exception.BaseException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only capability endpoint; revocation is available only through the authenticated OpenAPI. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/video/public/player")
public class VideoPublicPlayerController {

  private final VideoPlaybackTicketService tickets;
  private final VideoOpenApiSessionService sessions;

  @GetMapping("/{token}")
  public ResponseEntity<R<Map<String, Object>>> context(@PathVariable("token") String token) {
    try {
      String sessionId = tickets.getSessionId(token);
      if (sessionId != null) sessions.heartbeat(sessionId);
      return response(200, R.ok(tickets.context(token)));
    } catch (BaseException | AccessDeniedException e) {
      return response(410, R.error(410, "播放链接无效、已过期或已撤销", null));
    } catch (RuntimeException e) {
      return response(502, R.error(502, "视频连接暂时不可用", null));
    }
  }

  private ResponseEntity<R<Map<String, Object>>> response(int status, R<Map<String, Object>> body) {
    return ResponseEntity.status(status)
        .cacheControl(CacheControl.noStore())
        .header("Referrer-Policy", "no-referrer")
        .body(body);
  }
}
