package cn.universal.admin.video.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.video.service.VideoOpenApiSessionService;
import cn.universal.admin.video.service.VideoPlaybackTicketService;
import cn.universal.common.exception.BaseException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class VideoPublicPlayerControllerTest {
  private final VideoPlaybackTicketService tickets = mock(VideoPlaybackTicketService.class);
  private final VideoOpenApiSessionService sessions = mock(VideoOpenApiSessionService.class);
  private final VideoPublicPlayerController controller = new VideoPublicPlayerController(tickets, sessions);

  @Test
  void refreshesOnlyTheSessionStoredInTheAuthorizedTicketAndNeverCachesResponse() {
    when(tickets.getSessionId("ticket")).thenReturn("trusted-session");
    when(tickets.context("ticket")).thenReturn(Map.of("stream", Map.of("url", "https://media.example/live.m3u8")));
    var response = controller.context("ticket");
    assertEquals(200, response.getStatusCode().value());
    assertEquals("no-store", response.getHeaders().getCacheControl());
    assertEquals("no-referrer", response.getHeaders().getFirst("Referrer-Policy"));
    verify(sessions).heartbeat("trusted-session");
    assertNotNull(response.getBody().getData().get("stream"));
  }

  @Test
  void invalidTicketAndRevokedSessionReturnGoneWithoutExposingInternalDetails() {
    when(tickets.getSessionId("expired")).thenThrow(new BaseException("internal ownership detail"));
    var expired = controller.context("expired");
    assertEquals(410, expired.getStatusCode().value());
    assertFalse(expired.getBody().getMsg().contains("internal"));
    verifyNoInteractions(sessions);

    when(tickets.getSessionId("revoked")).thenReturn("stopped-session");
    doThrow(new AccessDeniedException("private device address")).when(sessions).heartbeat("stopped-session");
    var revoked = controller.context("revoked");
    assertEquals(410, revoked.getStatusCode().value());
    assertFalse(revoked.getBody().getMsg().contains("private"));
    assertEquals("no-store", revoked.getHeaders().getCacheControl());
  }

  @Test
  void temporaryInfrastructureFailureCanBeRetriedWithoutLeakingBackendError() {
    when(tickets.getSessionId("temporary")).thenThrow(new IllegalStateException("redis://private:secret@internal"));
    var response = controller.context("temporary");
    assertEquals(502, response.getStatusCode().value());
    assertEquals("视频连接暂时不可用", response.getBody().getMsg());
    assertEquals("no-store", response.getHeaders().getCacheControl());
    assertEquals("no-referrer", response.getHeaders().getFirst("Referrer-Policy"));
  }
}
