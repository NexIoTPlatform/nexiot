package cn.universal.web.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.universal.security.wechat.WechatAuthService;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

class WechatAuthWebSocketConfigTest {
  private static final String HOST = "test.iot.192886.xyz:81";
  private static final String ORIGIN = "http://" + HOST;

  private WechatAuthService auth;
  private WechatAuthWebSocketConfig.TicketInterceptor interceptor;
  private WebSocketHandler handler;

  @BeforeEach
  void setup() {
    auth = mock(WechatAuthService.class);
    interceptor = new WechatAuthWebSocketConfig.TicketInterceptor(auth);
    handler = mock(WebSocketHandler.class);
  }

  @Test
  void acceptsConfiguredPort81AndStoresOnlyConsumedSessionIdentity() {
    when(auth.consumeWebSocketTicket("ticket-fixture")).thenReturn("session-fixture");
    Map<String, Object> attributes = new LinkedHashMap<>();
    ServerHttpResponse response = mock(ServerHttpResponse.class);

    assertTrue(
        interceptor.beforeHandshake(
            request(ORIGIN, HOST, "ticket=ticket-fixture"), response, handler, attributes));

    assertEquals(Map.of("wechatSessionId", "session-fixture"), attributes);
    verify(auth).consumeWebSocketTicket("ticket-fixture");
    verifyNoInteractions(response);
  }

  @Test
  void rejectsAReplayedOrExpiredTicketWithoutAttachingSessionIdentity() {
    when(auth.consumeWebSocketTicket("ticket-fixture"))
        .thenReturn("session-fixture")
        .thenThrow(new IllegalArgumentException("expired or consumed"));
    assertTrue(
        interceptor.beforeHandshake(
            request(ORIGIN, HOST, "ticket=ticket-fixture"),
            mock(ServerHttpResponse.class),
            handler,
            new LinkedHashMap<>()));

    Map<String, Object> attributes = new LinkedHashMap<>();
    ServerHttpResponse response = mock(ServerHttpResponse.class);
    assertFalse(
        interceptor.beforeHandshake(
            request(ORIGIN, HOST, "ticket=ticket-fixture"), response, handler, attributes));
    assertTrue(attributes.isEmpty());
    verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    verify(auth, times(2)).consumeWebSocketTicket("ticket-fixture");
  }

  @Test
  void rejectsMissingAndRepeatedTicketParametersBeforeConsumingAnyTicket() {
    for (String query : new String[] {"", "sessionId=forged", "ticket=one&ticket=two"}) {
      ServerHttpResponse response = mock(ServerHttpResponse.class);
      Map<String, Object> attributes = new LinkedHashMap<>();
      assertFalse(
          interceptor.beforeHandshake(request(ORIGIN, HOST, query), response, handler, attributes));
      assertTrue(attributes.isEmpty());
      verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }
    verifyNoInteractions(auth);
  }

  @Test
  void rejectsForeignAndMismatchedPortOriginsBeforeTicketConsumption() {
    for (String origin :
        new String[] {
          "http://test.iot.192886.xyz",
          "http://test.iot.192886.xyz:80",
          "http://attacker.example:81",
          "null",
          "http://user@test.iot.192886.xyz:81",
          "http://test.iot.192886.xyz:81/path",
          "http://test.iot.192886.xyz:81?query=1",
          "http://test.iot.192886.xyz:81#fragment"
        }) {
      ServerHttpResponse response = mock(ServerHttpResponse.class);
      Map<String, Object> attributes = new LinkedHashMap<>();
      assertFalse(
          interceptor.beforeHandshake(
              request(origin, HOST, "ticket=ticket-fixture"), response, handler, attributes),
          origin);
      assertTrue(attributes.isEmpty());
      verify(response).setStatusCode(HttpStatus.FORBIDDEN);
    }
    verifyNoInteractions(auth);
  }

  @Test
  void normalizesDefaultPortsAndPreservesExplicitPortBoundaries() {
    assertTrue(sameOrigin("http://example.test", "example.test:80"));
    assertTrue(sameOrigin("https://example.test:443", "example.test"));
    assertTrue(sameOrigin("https://EXAMPLE.test:81", "example.test:81"));
    assertTrue(sameOrigin("http://[2001:db8::1]:81", "[2001:db8::1]:81"));
    assertFalse(sameOrigin("https://example.test:81", "example.test"));
    assertFalse(sameOrigin("http://example.test:81", "example.test:80"));
    assertFalse(sameOrigin(null, "example.test"));
    assertFalse(sameOrigin("http://example.test", null));
  }

  @Test
  void rejectsMalformedHostAuthoritiesInsteadOfIgnoringTheirExtraComponents() {
    for (String host :
        new String[] {
          "user@example.test:81",
          "example.test:81/path",
          "example.test:81?query=1",
          "example.test:81#fragment",
          "example.test:",
          "example.test:70000"
        }) {
      assertFalse(sameOrigin("http://example.test:81", host), host);
    }
    assertFalse(sameOrigin("http://example.test:70000", "example.test:70000"));
  }

  private static boolean sameOrigin(String origin, String host) {
    return WechatAuthWebSocketConfig.TicketInterceptor.sameOrigin(origin, host);
  }

  private static ServerHttpRequest request(String origin, String host, String query) {
    ServerHttpRequest request = mock(ServerHttpRequest.class);
    HttpHeaders headers = new HttpHeaders();
    headers.setOrigin(origin);
    headers.set(HttpHeaders.HOST, host);
    when(request.getHeaders()).thenReturn(headers);
    when(request.getURI())
        .thenReturn(
            URI.create(
                "http://" + host + "/ws/wechat-auth" + (query.isEmpty() ? "" : "?" + query)));
    return request;
  }
}
