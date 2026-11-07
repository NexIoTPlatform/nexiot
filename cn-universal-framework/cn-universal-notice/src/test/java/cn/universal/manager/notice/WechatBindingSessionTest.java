package cn.universal.manager.notice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.manager.notice.model.NoticeChannel;
import cn.universal.manager.notice.service.*;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class WechatBindingSessionTest {
  @Test void callbackIsOneTimeAndResultsBelongToCreator() {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    ValueOperations<String, String> values = mock(ValueOperations.class);
    Map<String, String> state = new HashMap<>();
    when(redis.opsForValue()).thenReturn(values);
    doAnswer(invocation -> { state.put(invocation.getArgument(0), invocation.getArgument(1)); return null; }).when(values).set(anyString(), anyString(), any(Duration.class));
    when(values.get(anyString())).thenAnswer(invocation -> state.get(invocation.getArgument(0)));
    when(values.getAndDelete(anyString())).thenAnswer(invocation -> state.remove(invocation.getArgument(0)));
    NoticeChannelService channels = mock(NoticeChannelService.class);
    WechatOpenIdService openIds = mock(WechatOpenIdService.class);
    NoticeChannel channel = new NoticeChannel(); channel.setId(8L); channel.setStatus("0");
    channel.setConfig("{\"oauthCallbackUrl\":\"https://example.com/api/open-api/v1/notice/wechat/callback\"}");
    when(channels.getById(8L)).thenReturn(channel);
    when(openIds.authorizationUrl(eq(channel), anyString(), anyString())).thenReturn("https://open.weixin.qq.com/test");
    when(openIds.exchange(channel, "code")).thenReturn("openid_user_bound");
    WechatBindingSessionService sessions = new WechatBindingSessionService(redis, openIds, channels);
    NoticeAccess access = mock(NoticeAccess.class);
    when(access.runAs(anyString(), any())).thenAnswer(invocation -> ((java.util.function.Supplier<?>) invocation.getArgument(1)).get());
    org.springframework.test.util.ReflectionTestUtils.setField(sessions, "access", access);
    when(openIds.bind(anyLong(), anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
    String id = (String) sessions.create(channel, "employee001", "tenant-a").get("sessionId");
    assertEquals("WAITING", sessions.status(id, "tenant-a").get("status"));
    assertThrows(IllegalArgumentException.class, () -> sessions.status(id, "tenant-b"));
    sessions.complete(id, "code");
    assertEquals("openid_user_bound", sessions.status(id, "tenant-a").get("openid"));
    verify(openIds).bind(8L, "employee001", "openid_user_bound");
    assertThrows(IllegalArgumentException.class, () -> sessions.complete(id, "code"));
    verify(openIds, times(1)).exchange(any(), anyString());
    String scanId = (String) sessions.create(channel, "", "tenant-a").get("sessionId");
    String scanKey = (String) sessions.status(scanId, "tenant-a").get("userKey");
    assertTrue(scanKey.startsWith("scan_"));
    String anotherId = (String) sessions.create(channel, null, "tenant-a").get("sessionId");
    assertNotEquals(scanKey, sessions.status(anotherId, "tenant-a").get("userKey"));
    sessions.complete(scanId, "code");
    verify(openIds).bind(8L, scanKey, "openid_user_bound");
  }
  @Test void rejectsUnconfiguredCallbackBeforeCreatingSession() {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    NoticeChannel channel = new NoticeChannel(); channel.setConfig("{}");
    WechatBindingSessionService sessions = new WechatBindingSessionService(redis, mock(WechatOpenIdService.class), mock(NoticeChannelService.class));
    assertThrows(IllegalArgumentException.class, () -> sessions.create(channel, "employee001", "tenant-a"));
    verifyNoInteractions(redis);
  }
}
