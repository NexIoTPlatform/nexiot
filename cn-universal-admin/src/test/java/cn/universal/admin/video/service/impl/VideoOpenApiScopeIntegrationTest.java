package cn.universal.admin.video.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.system.permission.SysPermissionService;
import cn.universal.admin.video.service.OpenApiVideoScope;
import cn.universal.admin.video.service.VideoOperationAccess;
import cn.universal.dm.video.VideoPlatformAdapter;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

class VideoOpenApiScopeIntegrationTest {
  private VideoPlatformInstanceServiceImpl service;
  private VideoPlatformAdapter adapter;
  private VideoPlatformDeviceMapper devices;
  private VideoPlatformChannelMapper channels;
  private VideoPlatformInstance instance;

  @BeforeEach
  void setup() {
    EntityHelper.initEntityNameMap(VideoPlatformDevice.class, new Config());
    EntityHelper.initEntityNameMap(VideoPlatformChannel.class, new Config());
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
        Jwt.withTokenValue("test").header("alg", "none").subject("app")
            .claim("tenantId", "tenant").build(), List.of()));
    service = mock(VideoPlatformInstanceServiceImpl.class, CALLS_REAL_METHODS);
    adapter = mock(VideoPlatformAdapter.class);
    devices = mock(VideoPlatformDeviceMapper.class);
    channels = mock(VideoPlatformChannelMapper.class);
    var registry = mock(VideoPlatformAdapterRegistry.class);
    when(registry.getAdapter("wvp")).thenReturn(adapter);
    ReflectionTestUtils.setField(service, "adapterRegistry", registry);
    ReflectionTestUtils.setField(service, "deviceMapper", devices);
    ReflectionTestUtils.setField(service, "channelMapper", channels);
    ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
    ReflectionTestUtils.setField(service, "videoOperationAccess", new VideoOperationAccess(mock(SysPermissionService.class)));
    instance = VideoPlatformInstance.builder().instanceKey("instance").tenantId("tenant").platformType("wvp").build();
    doReturn(instance).when(service).getByInstanceKey(anyString());
    when(devices.selectByExample(any())).thenReturn(List.of(
        VideoPlatformDevice.builder().id(1L).instanceKey("instance").tenantId("tenant").deviceId("device").build()));
    when(channels.selectByExample(any())).thenReturn(List.of(
        VideoPlatformChannel.builder().instanceKey("instance").deviceId(1L).channelId("channel").tenantId("tenant").build()));
    when(adapter.queryRecords(eq(instance), eq("device"), eq("channel"), eq(1000L), eq(2000L), anyMap()))
        .thenReturn(List.of(Map.of("startTime", 1000L)));
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void authorizedAppUsesUnifiedAdapterWithoutChangingAuthentication() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    var result = OpenApiVideoScope.call(scope(), () -> service.queryRecords("instance", "device", "channel", 1000, 2000));
    assertEquals(1, result.size());
    assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());
    verify(adapter).queryRecords(eq(instance), eq("device"), eq("channel"), eq(1000L), eq(2000L), anyMap());
  }

  @Test
  void mismatchedInstanceTenantAndChannelNeverReachAdapter() {
    instance.setInstanceKey("another-instance");
    assertDenied("device", "channel");
    instance.setInstanceKey("instance");
    instance.setTenantId("another-tenant");
    assertDenied("device", "channel");
    instance.setTenantId("tenant");
    assertDenied("another-device", "channel");
    assertDenied("device", "another-channel");
    verifyNoInteractions(adapter);
    verifyNoInteractions(devices, channels);
  }

  @Test
  void staleOrInconsistentDatabaseTenantRowsAreRejected() {
    when(devices.selectByExample(any())).thenReturn(List.of(
        VideoPlatformDevice.builder().id(1L).instanceKey("instance").tenantId("other-tenant").deviceId("device").build()));
    assertDenied("device", "channel");
    verifyNoInteractions(channels);
    when(devices.selectByExample(any())).thenReturn(List.of(
        VideoPlatformDevice.builder().id(1L).instanceKey("instance").tenantId("tenant").deviceId("device").build()));
    when(channels.selectByExample(any())).thenReturn(List.of(
        VideoPlatformChannel.builder().instanceKey("instance").deviceId(1L).channelId("channel").tenantId("other-tenant").build()));
    assertDenied("device", "channel");
    verifyNoInteractions(adapter);
  }

  private void assertDenied(String device, String channel) {
    assertThrows(AccessDeniedException.class, () -> OpenApiVideoScope.call(scope(),
        () -> service.queryRecords("instance", device, channel, 1000, 2000)));
  }

  private OpenApiVideoScope.Scope scope() {
    return new OpenApiVideoScope.Scope("app", "tenant", "application", "instance", "device", "channel", Set.of("recordingSearch"));
  }
}
