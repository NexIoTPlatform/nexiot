package cn.universal.web.websocket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import cn.universal.admin.video.service.VideoPlatformInstanceService;
import cn.universal.persistence.entity.VideoPlatformInstance;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class VideoSdkTalkTicketsTest {
  @Test void stripsCredentialOverridesAndOnlyReturnsRelayTicket() {
    VideoPlatformInstanceService instances = mock(VideoPlatformInstanceService.class);
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    VideoPlatformInstance instance = VideoPlatformInstance.builder().platformType("hik_sdk").endpoint("https://gateway.internal:5102").build();
    when(instances.getByInstanceKey("instance")).thenReturn(instance);
    String upstreamToken = UUID.randomUUID().toString();
    when(instances.executePlatformOperation(eq("instance"), eq("talkStart"), any())).thenReturn(
        Map.of("token", upstreamToken, "sampleRate", 8000, "frameSamples", 160));
    var tickets = new VideoSdkTalkTickets(instances, redis, new ObjectMapper());
    var response = tickets.create("instance", Map.of("deviceId", "camera", "channelId", "1", "host", "attacker", "password", "injected", "options", Map.of("gatewayBaseUrl", "http://attacker")));
    @SuppressWarnings("unchecked") ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
    verify(instances).executePlatformOperation(eq("instance"), eq("talkStart"), params.capture());
    assertEquals(Map.of("deviceId", "camera", "channelId", "1", "transfer", false), params.getValue());
    assertFalse(response.containsKey("url"));
    assertNotEquals(upstreamToken, response.get("token"));
    verify(values).set(eq("video:sdk:talk:" + response.get("token")),contains("wss://gateway.internal:5102/ws/sdk-talk?token=" + upstreamToken),eq(Duration.ofSeconds(20)));
  }
  @Test void consumesTicketAtomicallyAndRejectsReplay() {
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    var tickets = new VideoSdkTalkTickets(mock(VideoPlatformInstanceService.class), redis, new ObjectMapper());
    String token = UUID.randomUUID().toString();
    when(values.getAndDelete("video:sdk:talk:" + token)).thenReturn("ws://internal/ws/sdk-talk?token=private").thenReturn(null);
    assertEquals(URI.create("ws://internal/ws/sdk-talk?token=private"), tickets.consume(token));
    assertThrows(RuntimeException.class, () -> tickets.consume(token));
    assertThrows(RuntimeException.class, () -> tickets.consume("arbitrary-key"));
  }
  @Test void instanceDenialDoesNotIssueTicket() {
    VideoPlatformInstanceService instances = mock(VideoPlatformInstanceService.class);
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    when(instances.executePlatformOperation(any(), any(), any())).thenThrow(new SecurityException("tenant denied"));
    var tickets = new VideoSdkTalkTickets(instances, redis, new ObjectMapper());
    assertThrows(SecurityException.class, () -> tickets.create("foreign", Map.of("deviceId", "camera", "channelId", "1")));
    verifyNoInteractions(redis);
  }
  @Test void isupPreservesAudioModeAndCodecWithoutLegacyTransfer() {
    var instances=mock(VideoPlatformInstanceService.class);var redis=mock(StringRedisTemplate.class);var values=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(values);
    when(instances.getByInstanceKey("isup")).thenReturn(VideoPlatformInstance.builder().platformType("hik_isup").endpoint("http://gateway.internal:5102").build());
    when(instances.executePlatformOperation(eq("isup"),eq("talkStart"),anyMap())).thenReturn(Map.of("token",UUID.randomUUID().toString(),"sampleRate",8000,"frameSamples",160));
    var tickets=new VideoSdkTalkTickets(instances,redis,new ObjectMapper());
    var response=tickets.create("isup",Map.of("deviceId","nvr","channelId","7","voiceChannel",2,"mode","listen","codec","g711a","transfer",true));
    assertEquals("listen",response.get("mode"));verify(instances).executePlatformOperation(eq("isup"),eq("talkStart"),eq(Map.of("deviceId","nvr","channelId","7","voiceChannel",2,"mode","listen","codec","G711A")));
    assertThrows(RuntimeException.class,()->tickets.create("isup",Map.of("deviceId","nvr","channelId","7","mode","shell")));
  }
  @Test void foreignInstanceCannotConsumeCancellationTicket() throws Exception {
    var instances=mock(VideoPlatformInstanceService.class);var redis=mock(StringRedisTemplate.class);ValueOperations<String,String> values=mock(ValueOperations.class);when(redis.opsForValue()).thenReturn(values);
    String token=UUID.randomUUID().toString();when(values.get("video:sdk:talk:"+token)).thenReturn(new ObjectMapper().writeValueAsString(Map.of("instanceKey","private","deviceId","nvr","channelId","7","upstream","ws://internal/ws/sdk-talk?token=secret")));
    var tickets=new VideoSdkTalkTickets(instances,redis,new ObjectMapper());assertThrows(RuntimeException.class,()->tickets.cancel("foreign",token));
    verify(values,never()).getAndDelete(anyString());verifyNoInteractions(instances);
  }
}
