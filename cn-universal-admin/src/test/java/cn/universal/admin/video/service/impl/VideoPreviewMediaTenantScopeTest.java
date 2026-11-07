package cn.universal.admin.video.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.admin.video.service.VideoDahuaPreviewService;
import cn.universal.admin.video.service.OpenApiVideoScope;
import cn.universal.common.exception.BaseException;
import cn.universal.dm.video.VideoPlatformAdapter;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.dm.video.zlm.VideoZlmStreamProxyService;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.VideoPlatformChannel;
import cn.universal.persistence.entity.VideoPlatformDevice;
import cn.universal.persistence.entity.VideoPlatformDeviceExt;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformChannelMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceExtMapper;
import cn.universal.persistence.mapper.VideoPlatformDeviceMapper;
import cn.universal.security.service.IoTUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

class VideoPreviewMediaTenantScopeTest {
  private static final String INSTANCE = "tiandy-test";
  private static final String NODE = "cross-tenant-node";
  private static final String SOURCE = "rtsp://camera.invalid/live";

  private VideoPlatformInstanceServiceImpl service;
  private VideoZlmStreamProxyService proxy;
  private IoTUserService users;
  private VideoPlatformInstance instance;
  private VideoDahuaPreviewService dahuaPreview;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    SecurityContextHolder.clearContext();
    service = mock(VideoPlatformInstanceServiceImpl.class, CALLS_REAL_METHODS);
    proxy = mock(VideoZlmStreamProxyService.class);
    users = mock(IoTUserService.class);
    ObjectProvider<VideoZlmStreamProxyService> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(proxy);
    ReflectionTestUtils.setField(service, "zlmStreamProxyServiceProvider", provider);
    ReflectionTestUtils.setField(service, "ioTUserService", users);
    ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
    instance = new VideoPlatformInstance();
    instance.setInstanceKey(INSTANCE);
    instance.setPlatformType("tiandy_sdk");
    instance.setTenantId("0");
    doReturn(instance).when(service).getByInstanceKey(INSTANCE);
    doReturn(Map.of()).when(service).getDeviceDetail(INSTANCE, "camera");
    when(proxy.ensureStreamProxy(nullable(String.class), nullable(String.class), anyString(), anyString(), anyString()))
        .thenReturn(Map.of("ws_flv", "ws://media.invalid/live.flv"));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @ParameterizedTest
  @ValueSource(strings = {"admin", "tenant-zero-user", "background"})
  void previewUsesAuthenticatedUserIdentityAndPreservesBackgroundScope(String actor) {
    authenticate(actor);
    Map<String, String> result = preview(Map.of("mediaServerId", NODE));

    assertEquals("READY", result.get("zlmProxyState"));
    assertEquals(NODE, result.get("requestedMediaServerId"));
    verify(proxy).ensureStreamProxy(eq(scope(actor)), eq(NODE), eq(SOURCE), anyString(), anyString());
    assertEquals("0", instance.getTenantId());
    if ("background".equals(actor)) verifyNoInteractions(users);
  }

  @Test
  void tenantZeroAndAdminLookingOptionsOrJwtClaimsCannotGrantAdminScope() {
    authenticate("tenant-zero-user");
    Map<String, String> options = new LinkedHashMap<>();
    options.put("mediaServerId", NODE);
    options.put("tenantId", "");
    options.put("scheduleTenantId", "");
    options.put("isAdmin", "true");
    options.put("identity", "0");
    options.put("playback", "true");
    options.put("capture", "true");
    preview(options);

    verify(proxy).ensureStreamProxy(eq("0"), eq(NODE), eq(SOURCE), anyString(), anyString());
  }

  @Test
  void unauthenticatedAdminPrincipalCannotGrantAdminScope() {
    JwtAuthenticationToken authentication = token("admin");
    authentication.setAuthenticated(false);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    preview(Map.of("mediaServerId", NODE));

    verify(proxy).ensureStreamProxy(eq("0"), eq(NODE), eq(SOURCE), anyString(), anyString());
    verifyNoInteractions(users);
  }

  @Test
  void unknownAuthenticatedUserCannotGrantAdminScope() {
    SecurityContextHolder.getContext().setAuthentication(token("unknown"));
    preview(Map.of("mediaServerId", NODE));

    verify(proxy).ensureStreamProxy(eq("0"), eq(NODE), eq(SOURCE), anyString(), anyString());
  }

  @Test
  void adminAutomaticSchedulingKeepsPlatformTenant() {
    authenticate("admin");
    preview(Map.of());

    verify(proxy).ensureStreamProxy(eq("0"), isNull(), eq(SOURCE), anyString(), anyString());
    verifyNoInteractions(users);
  }

  @Test
  void configuredNodeUsesAdminScopeOnlyForCurrentRequestWithoutChangingConfiguration() {
    authenticate("admin");
    String options = "{\"mediaServerId\":\"" + NODE + "\"}";
    instance.setOptions(options);
    preview(Map.of());
    verify(proxy).ensureStreamProxy(isNull(), eq(NODE), eq(SOURCE), anyString(), anyString());

    SecurityContextHolder.clearContext();
    clearInvocations(proxy);
    preview(Map.of());
    verify(proxy).ensureStreamProxy(eq("0"), eq(NODE), eq(SOURCE), anyString(), anyString());
    assertEquals(options, instance.getOptions());
    assertEquals("0", instance.getTenantId());
  }

  @Test
  void openApiPreviewsOfTheSameSdkChannelOwnSeparateProxiesWhileUiKeepsItsSharedName() {
    authenticate("tenant-zero-user");
    String sharedName = INSTANCE + "_camera_1_main";
    assertEquals(sharedName, preview(Map.of("mediaServerId", NODE)).get("zlmStream"));

    SecurityContextHolder.getContext().setAuthentication(token("openapi-app"));
    var authorized = new OpenApiVideoScope.Scope("openapi-app", "0", "application", INSTANCE,
        "camera", "1", Set.of("preview"));
    Map<String, String> first = OpenApiVideoScope.call(authorized, () -> preview(Map.of("mediaServerId", NODE)));
    Map<String, String> second = OpenApiVideoScope.call(authorized, () -> preview(Map.of("mediaServerId", NODE)));
    assertTrue(first.get("zlmStream").matches(java.util.regex.Pattern.quote(sharedName) + "_openapi_[a-f0-9]{32}"));
    assertTrue(second.get("zlmStream").matches(java.util.regex.Pattern.quote(sharedName) + "_openapi_[a-f0-9]{32}"));
    org.junit.jupiter.api.Assertions.assertNotEquals(first.get("zlmStream"), second.get("zlmStream"));
    verify(proxy).ensureStreamProxy(eq("0"), eq(NODE), eq(SOURCE), eq("nexiot-tiandy_sdk"), eq(first.get("zlmStream")));
    verify(proxy).ensureStreamProxy(eq("0"), eq(NODE), eq(SOURCE), eq("nexiot-tiandy_sdk"), eq(second.get("zlmStream")));

    authenticate("tenant-zero-user");
    assertEquals(sharedName, preview(Map.of("mediaServerId", NODE)).get("zlmStream"));
    verify(proxy, times(2)).ensureStreamProxy(eq("0"), eq(NODE), eq(SOURCE), eq("nexiot-tiandy_sdk"), eq(sharedName));
  }

  @ParameterizedTest
  @ValueSource(strings = {"admin", "tenant-zero-user", "background"})
  void releaseUsesTheSameAuthenticatedScopeAsPreview(String actor) {
    authenticate(actor);
    when(proxy.releaseStreamProxy(nullable(String.class), eq(NODE), anyString(), anyString()))
        .thenReturn(Map.of("count_closed", "1"));

    Map<String, Object> result = service.stopPreview(INSTANCE, "camera", "1", Map.of("mediaServerId", NODE));

    assertEquals(true, result.get("stopped"));
    verify(proxy).releaseStreamProxy(eq(scope(actor)), eq(NODE), anyString(), anyString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"admin", "tenant-zero-user", "background"})
  void snapshotUsesTheSameAuthenticatedScopeAndIgnoresClaimedOptions(String actor) {
    authenticate(actor);
    doReturn(Map.of("mediaServerPrimaryId", NODE, "originRtsp", SOURCE))
        .when(service).getStreamUrl(eq(INSTANCE), eq("camera"), eq("1"), eq("main"), anyMap());
    when(proxy.captureStreamSnapshot(nullable(String.class), eq(NODE), eq(SOURCE), anyString(), anyString()))
        .thenReturn(Map.of("snapshotUrl", "https://media.invalid/snapshot.jpg"));

    Map<String, Object> result = ReflectionTestUtils.invokeMethod(service, "captureSnapshotViaZlm",
        instance, INSTANCE, "camera", "1", "main", Map.of("mediaServerId", NODE, "tenantId", "", "isAdmin", true));

    assertTrue(result.containsKey("snapshotUrl"));
    verify(proxy).captureStreamSnapshot(eq(scope(actor)), eq(NODE), eq(SOURCE), anyString(), anyString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"source-and-mode", "source-only", "mode-only"})
  void registeredDahuaPreviewPreservesSdkSessionWithoutProxyingItsReturnedRtsp(String markers) {
    VideoPlatformAdapter adapter = dahuaPreviewFixture(true);
    Map<String, String> sdkUrls = new LinkedHashMap<>();
    sdkUrls.put("rtsp", "rtsp://media.invalid/rtp/dahua-preview-1");
    sdkUrls.put("ws_flv", "ws://media.invalid/rtp/dahua-preview-1.live.flv");
    sdkUrls.put("previewId", "native-preview-1");
    sdkUrls.put("ownerNode", "dahua-gateway-1");
    sdkUrls.put("stream", "dahua-preview-1");
    sdkUrls.put("zlmStream", "dahua-preview-1");
    sdkUrls.put("app", "rtp");
    if (!"mode-only".equals(markers)) {
      sdkUrls.put("streamSource", "gateway_dahua_registration");
    }
    if (!"source-only".equals(markers)) {
      sdkUrls.put("streamMode", "sdk_zlm_rtp");
    }
    when(dahuaPreview.start(eq(instance), eq("camera"), eq("1"), eq("main"),
        anyMap(), isNull(), eq(NODE))).thenReturn(sdkUrls);

    Map<String, String> result = service.getStreamUrl(INSTANCE, "camera", "1", "main",
        Map.of("mediaServerId", NODE));

    sdkUrls.forEach((key, value) -> assertEquals(value, result.get(key), key));
    assertEquals(sdkUrls.get("ws_flv"), result.get("preferredUrl"));
    verify(dahuaPreview).start(eq(instance), eq("camera"), eq("1"), eq("main"),
        argThat(options -> !options.containsKey("disableZlmProxy")), isNull(), eq(NODE));
    verifyNoInteractions(proxy, adapter);
  }

  @Test
  void desktopCanPreviewDirectDahuaRtspWithoutAllocatingOrRequiringAMediaNode() {
    VideoPlatformAdapter adapter = dahuaPreviewFixture(false);
    when(adapter.getStreamUrl(eq(instance), eq("camera"), eq("1"), eq("main"), anyMap()))
        .thenReturn(Map.of("rtsp", SOURCE, "streamSource", "origin", "streamMode", "rtsp_first"));

    Map<String, String> result = service.getStreamUrl(INSTANCE, "camera", "1", "main",
        Map.of("playMode", "origin", "preferProtocol", "rtsp", "disableZlmProxy", "true", "proxyRtsp", "false"));

    assertEquals(SOURCE, result.get("preferredUrl"));
    assertEquals("origin", result.get("streamSource"));
    assertEquals("rtsp_first", result.get("streamMode"));
    verifyNoInteractions(proxy, dahuaPreview);
  }

  @Test
  void desktopDirectPreferenceCannotTurnRegisteredDahuaIntoDeviceRtsp() {
    VideoPlatformAdapter adapter = dahuaPreviewFixture(true);
    String managedRtsp = "rtsp://media.invalid/rtp/dahua-preview-1";
    when(dahuaPreview.start(eq(instance), eq("camera"), eq("1"), eq("main"),
        anyMap(), isNull(), eq(NODE))).thenReturn(Map.of("rtsp", managedRtsp,
        "previewId", "native-preview-1", "streamMode", "sdk_zlm_rtp", "streamSource", "gateway_dahua_registration"));

    Map<String, String> result = service.getStreamUrl(INSTANCE, "camera", "1", "main",
        Map.of("mediaServerId", NODE, "preferProtocol", "rtsp", "disableZlmProxy", "true", "proxyRtsp", "false"));

    assertEquals(managedRtsp, result.get("preferredUrl"));
    assertEquals("native-preview-1", result.get("previewId"));
    assertEquals("sdk_zlm_rtp", result.get("streamMode"));
    verifyNoInteractions(proxy, adapter);
  }

  @Test
  void directDahuaPreviewStillProxiesOriginRtspThroughTheNormalPlatformEntry() {
    VideoPlatformAdapter adapter = dahuaPreviewFixture(false);
    when(adapter.getStreamUrl(eq(instance), eq("camera"), eq("1"), eq("main"), anyMap()))
        .thenReturn(Map.of("rtsp", SOURCE));

    Map<String, String> result = service.getStreamUrl(INSTANCE, "camera", "1", "main",
        Map.of("mediaServerId", NODE));

    assertEquals("zlm_proxy", result.get("streamSource"));
    assertEquals("READY", result.get("zlmProxyState"));
    assertEquals("ZLM代理拉流已就绪", result.get("playHint"));
    assertEquals(SOURCE, result.get("originRtsp"));
    assertEquals("ws://media.invalid/live.flv", result.get("preferredUrl"));
    verify(proxy).ensureStreamProxy(isNull(), eq(NODE), eq(SOURCE), anyString(), anyString());
    verifyNoInteractions(dahuaPreview);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void directOriginPreviewPreservesWaitingStateAndPublicPlaybackUrls(boolean proxyProvidesHint) {
    VideoPlatformAdapter adapter = dahuaPreviewFixture(false);
    when(adapter.getStreamUrl(eq(instance), eq("camera"), eq("1"), eq("main"), anyMap()))
        .thenReturn(Map.of("rtsp", SOURCE, "playHint", "设备RTSP地址已返回"));
    Map<String, String> proxyUrls = new LinkedHashMap<>();
    proxyUrls.put("proxyState", "WAITING");
    proxyUrls.put("ws_flv", "wss://public-media.invalid/live.flv");
    proxyUrls.put("flv", "https://public-media.invalid/live.flv");
    String waitingHint = "源流尚未产生视频帧，正在等待媒体就绪";
    if (proxyProvidesHint) proxyUrls.put("playHint", waitingHint);
    when(proxy.ensureStreamProxy(isNull(), eq(NODE), eq(SOURCE), anyString(), anyString()))
        .thenReturn(proxyUrls);

    Map<String, String> result = service.getStreamUrl(INSTANCE, "camera", "1", "main",
        Map.of("mediaServerId", NODE));

    assertEquals("WAITING", result.get("zlmProxyState"));
    assertEquals("WAITING", result.get("proxyState"));
    assertEquals(proxyUrls.get("ws_flv"), result.get("ws_flv"));
    assertEquals(proxyUrls.get("flv"), result.get("flv"));
    assertEquals(proxyUrls.get("ws_flv"), result.get("preferredUrl"));
    assertEquals(SOURCE, result.get("originRtsp"));
    assertEquals("zlm_proxy", result.get("streamSource"));
    assertEquals(proxyProvidesHint ? waitingHint : "ZLM代理已创建，正在等待源流注册和视频帧，请稍后重试",
        result.get("playHint"));
    verifyNoInteractions(dahuaPreview);
  }

  @ParameterizedTest
  @ValueSource(strings = {"stopPreview", "previewStop", "stopStream"})
  void directDahuaStopAliasesReleaseThePlatformProxyWithoutCallingTheSdkAdapter(String operation) {
    VideoPlatformAdapter adapter = dahuaPreviewFixture(false);
    when(proxy.releaseStreamProxy(isNull(), eq(NODE), eq("nexiot-dahua_sdk"), eq("direct-camera-1-sub")))
        .thenReturn(Map.of("count_hit", "1", "count_closed", "1"));

    Map<String, Object> result = service.executePlatformOperation(INSTANCE, operation, Map.of(
        "deviceId", "camera", "channelId", "1", "streamType", "sub", "mediaServerPrimaryId", NODE,
        "zlmApp", "nexiot-dahua_sdk", "zlmStream", "direct-camera-1-sub"));

    assertEquals(true, result.get("stopped"));
    assertEquals("1", result.get("countClosed"));
    assertEquals("sub", result.get("streamType"));
    assertEquals("nexiot-dahua_sdk", result.get("app"));
    assertEquals("direct-camera-1-sub", result.get("stream"));
    verify(proxy).releaseStreamProxy(isNull(), eq(NODE), eq("nexiot-dahua_sdk"), eq("direct-camera-1-sub"));
    verify((VideoPlatformChannelMapper) ReflectionTestUtils.getField(service, "channelMapper"))
        .selectByExample(any());
    verifyNoInteractions(adapter, dahuaPreview);
  }

  @ParameterizedTest
  @ValueSource(strings = {"stopPreview", "previewStop", "stopStream"})
  void directDahuaStopAliasesRejectMissingChannelsBeforeReleasingAnyStream(String operation) {
    VideoPlatformAdapter adapter = dahuaPreviewFixture(false);
    VideoPlatformChannelMapper channels =
        (VideoPlatformChannelMapper) ReflectionTestUtils.getField(service, "channelMapper");
    when(channels.selectByExample(any())).thenReturn(List.of());

    BaseException error = assertThrows(BaseException.class, () -> service.executePlatformOperation(
        INSTANCE, operation, Map.of("deviceId", "camera", "channelId", "missing", "mediaServerId", NODE)));

    assertTrue(error.getMessage().contains("通道不存在"));
    verify(channels).selectByExample(any());
    verifyNoInteractions(proxy, adapter, dahuaPreview);
  }

  @ParameterizedTest
  @ValueSource(strings = {"hik_sdk", "onvif", "tiandy_sdk", "dahua_sdk"})
  void wallPreviewUsesAViewerLeaseAndPreservesItsDedicatedNamespace(String platformType) {
    authenticate("tenant-zero-user");
    instance.setPlatformType(platformType);
    when(proxy.acquirePreviewLease(eq("0"), anyString(), eq(NODE), eq(SOURCE),
        eq("nexiot-" + platformType), anyString())).thenReturn(Map.of(
            "ws_flv", "ws://media.invalid/wall.flv", "previewLeaseId", "lease-1",
            "zlmApp", "nexiot-wall-" + platformType + "-owner", "zlmStream", "dedicated-wall-main",
            "leaseExpiresIn", "90", "heartbeatInterval", "25", "keepAlive", "true"));
    Map<String, String> result = preview(Map.of("mediaServerId", NODE, "keepAlive", "true",
        "viewerKey", "forged-admin", "tenantId", "forged"));
    assertEquals("lease-1", result.get("previewLeaseId"));
    assertEquals("nexiot-wall-" + platformType + "-owner", result.get("zlmApp"));
    assertEquals("dedicated-wall-main", result.get("zlmStream"));
    verify(proxy).acquirePreviewLease(eq("0"), eq("[\"0\",\"tiandy-test\",\"camera\",\"1\",\"tenant-zero-user\"]"),
        eq(NODE), eq(SOURCE), eq("nexiot-" + platformType), matches("tiandy-test_camera_1_main_[a-f0-9]{16}"));
    verify(proxy, never()).ensureStreamProxy(any(), any(), anyString(), anyString(), anyString());
  }

  @Test
  void wallHeartbeatAndReleaseResolveTrustedViewerIdentityAndIgnoreSuppliedStreamNames() {
    VideoPlatformAdapter adapter = dahuaPreviewFixture(false);
    String identity = "[\"0\",\"tiandy-test\",\"camera\",\"1\",\"admin\"]";
    when(proxy.renewPreviewLease(identity, "lease-1")).thenReturn(Map.of("active", true));
    when(proxy.releasePreviewLease(identity, "lease-1")).thenReturn(Map.of("stopped", true));
    Map<String, Object> params = Map.of("deviceId", "camera", "channelId", "1", "previewLeaseId", "lease-1",
        "zlmApp", "nexiot-dataset", "zlmStream", "foreign-job", "viewerKey", "forged");
    assertEquals(true, service.executePlatformOperation(INSTANCE, "previewHeartbeat", params).get("active"));
    assertEquals(true, service.executePlatformOperation(INSTANCE, "stopPreview", params).get("stopped"));
    verify(proxy).renewPreviewLease(identity, "lease-1");
    verify(proxy).releasePreviewLease(identity, "lease-1");
    verify(proxy, never()).releaseStreamProxy(any(), any(), anyString(), anyString());
    verifyNoInteractions(adapter, dahuaPreview);
  }

  @Test
  void wallLeaseOperationsRejectMissingChannelBeforeDelegating() {
    dahuaPreviewFixture(false);
    var channels = (VideoPlatformChannelMapper) ReflectionTestUtils.getField(service, "channelMapper");
    when(channels.selectByExample(any())).thenReturn(List.of());
    for (String operation : List.of("previewHeartbeat", "stopPreview"))
      assertThrows(BaseException.class, () -> service.executePlatformOperation(INSTANCE, operation,
          Map.of("deviceId", "camera", "channelId", "missing", "previewLeaseId", "lease-1")));
    verifyNoInteractions(proxy);
  }

  private VideoPlatformAdapter dahuaPreviewFixture(boolean registered) {
    authenticate("admin");
    instance.setPlatformType("dahua_sdk");
    for (Class<?> entity : List.of(VideoPlatformDevice.class, VideoPlatformDeviceExt.class,
        VideoPlatformChannel.class)) {
      EntityHelper.initEntityNameMap(entity, new Config());
    }
    VideoPlatformDeviceMapper devices = mock(VideoPlatformDeviceMapper.class);
    VideoPlatformDeviceExtMapper extensions = mock(VideoPlatformDeviceExtMapper.class);
    VideoPlatformChannelMapper channels = mock(VideoPlatformChannelMapper.class);
    VideoPlatformAdapterRegistry registry = mock(VideoPlatformAdapterRegistry.class);
    VideoPlatformAdapter adapter = mock(VideoPlatformAdapter.class);
    dahuaPreview = mock(VideoDahuaPreviewService.class);
    when(devices.selectByExample(any())).thenReturn(List.of(VideoPlatformDevice.builder()
        .id(11L).instanceKey(INSTANCE).deviceId("camera").build()));
    when(channels.selectByExample(any())).thenReturn(List.of(VideoPlatformChannel.builder()
        .id(12L).deviceId(11L).instanceKey(INSTANCE).channelId("1").build()));
    when(extensions.selectByExample(any())).thenReturn(List.of(VideoPlatformDeviceExt.builder()
        .deviceId(11L).extField3(registered
            ? "{\"loginMode\":\"active_registration\",\"accessMode\":\"active_registration\"}"
            : "{\"loginMode\":\"direct\",\"accessMode\":\"direct\"}").build()));
    when(registry.getAdapter("dahua_sdk")).thenReturn(adapter);
    ReflectionTestUtils.setField(service, "deviceMapper", devices);
    ReflectionTestUtils.setField(service, "deviceExtMapper", extensions);
    ReflectionTestUtils.setField(service, "channelMapper", channels);
    ReflectionTestUtils.setField(service, "adapterRegistry", registry);
    ReflectionTestUtils.setField(service, "dahuaPreviewService", dahuaPreview);
    return adapter;
  }

  private Map<String, String> preview(Map<String, String> options) {
    return ReflectionTestUtils.invokeMethod(service, "ensureBrowserPlayableOriginStream",
        instance, "camera", "1", "main", Map.of("rtsp", SOURCE), options);
  }

  private void authenticate(String actor) {
    if ("background".equals(actor)) return;
    SecurityContextHolder.getContext().setAuthentication(token(actor));
    when(users.selectUserByUnionId(actor)).thenReturn(IoTUser.builder()
        .unionId(actor).identity("admin".equals(actor) ? 0 : 1).tenantId("0").tenantRole("ADMIN").build());
  }

  private String scope(String actor) {
    return "admin".equals(actor) ? null : "0";
  }

  private JwtAuthenticationToken token(String subject) {
    Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").subject(subject)
        .claim("tenantId", "0").claim("identity", 0).claim("isAdmin", true)
        .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build();
    return new JwtAuthenticationToken(jwt, List.of());
  }
}
