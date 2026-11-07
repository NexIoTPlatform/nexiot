package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.system.permission.SysPermissionService;
import cn.universal.admin.video.web.VideoCatalogController;
import cn.universal.common.exception.BaseException;
import cn.universal.core.video.hosting.VideoCloudHostingProvider;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.vo.VideoCatalogDeviceRow;
import cn.universal.persistence.mapper.IoTProductMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class VideoCloudSdkTicketServiceTest {
  private final VideoCatalogService catalog = mock(VideoCatalogService.class);
  private final IoTProductMapper products = mock(IoTProductMapper.class);
  private final SysPermissionService permissions = mock(SysPermissionService.class);
  private final VideoCloudHostingProvider provider = mock(VideoCloudHostingProvider.class);

  private VideoCloudSdkTicketService service(String platform) {
    var device = new VideoCatalogDeviceRow();
    device.setProductKey("product"); device.setDeviceId("camera"); device.setPlatformType(platform);
    device.setConfiguration("{}");
    when(catalog.requireVisibleDevice("iot")).thenReturn(device);
    var channel = new VideoCatalogChannel();
    channel.setChannelId("0"); channel.setChannelNo(0);
    channel.setCapabilities(Map.of("preview", "supported"));
    when(catalog.requireVisibleChannel(device, "0")).thenReturn(channel);
    when(products.getProductByProductKey("product")).thenReturn(
        IoTProduct.builder().productKey("product").thirdPlatform(platform).build());
    when(provider.platformType()).thenReturn(platform);
    return new VideoCloudSdkTicketService(catalog, products, List.of(provider), permissions);
  }

  @Test void ticketUsesSelectedIdentityAndNonCacheableResponseWithMinimalCredentials() {
    var service = service("dahua_cloud");
    when(permissions.hasPermission("video:dahua_cloud:live")).thenReturn(true);
    when(provider.desktopPreviewCredentials(any(), any(), eq("0")))
        .thenReturn(Map.of("accessToken", "access", "playInfo", "play"));
    var result = new VideoCatalogController(catalog, service)
        .sdkTicket("iot", Map.of("channelId", "0", "streamType", "sub", "mode", "preview"));
    assertEquals("no-store", result.getHeaders().getCacheControl());
    var ticket = result.getBody();
    assertNotNull(ticket);
    assertEquals("cloud_dahua", ticket.get("vendor"));
    assertEquals("camera", ticket.get("deviceId")); assertEquals("0", ticket.get("channelId"));
    assertEquals(0, ticket.get("nativeChannel")); assertEquals("sub", ticket.get("streamType"));
    assertTrue((long) ticket.get("expiresAt") > System.currentTimeMillis());
    assertEquals(9, ticket.size());
  }

  @Test void lackingPreviewPermissionNeverCallsVendorOrProductLookup() {
    var service = service("imoulife");
    clearInvocations(products, provider);
    assertThrows(AccessDeniedException.class, () -> service.preview("iot", Map.of("channelId", "0")));
    verify(products, never()).getProductByProductKey(anyString());
    verify(provider, never()).desktopPreviewCredentials(any(), any(), any());
  }

  @Test void outOfScopeChannelAndUnsupportedModeNeverIssueCredentials() {
    var service = service("imoulife");
    when(permissions.hasPermission("video:device:play")).thenReturn(true);
    assertThrows(BaseException.class, () -> service.preview("iot", Map.of("channelId", "0", "mode", "playback")));
    when(catalog.requireVisibleChannel(any(VideoCatalogDeviceRow.class), eq("foreign")))
        .thenThrow(new BaseException("denied"));
    assertThrows(BaseException.class, () -> service.preview("iot", Map.of("channelId", "foreign")));
    verify(provider, never()).desktopPreviewCredentials(any(), any(), any());
  }

  @Test void unexpectedVendorCredentialFieldsFailClosed() {
    var service = service("imoulife");
    when(permissions.hasPermission("video:device:play")).thenReturn(true);
    when(provider.desktopPreviewCredentials(any(), any(), any()))
        .thenReturn(Map.of("accessToken", "access", "appSecret", "must-not-escape"));
    assertThrows(BaseException.class, () -> service.preview("iot", Map.of("channelId", "0")));
  }
}
