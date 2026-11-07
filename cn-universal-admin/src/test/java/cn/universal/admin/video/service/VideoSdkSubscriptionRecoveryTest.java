package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.dm.video.VideoPlatformAdapter;
import cn.universal.dm.video.VideoPlatformAdapterRegistry;
import cn.universal.dm.video.VideoSdkEventCatalog;
import cn.universal.persistence.entity.VideoPlatformInstance;
import cn.universal.persistence.mapper.VideoPlatformInstanceMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tk.mybatis.mapper.mapperhelper.EntityHelper;
import tk.mybatis.mapper.entity.Config;

class VideoSdkSubscriptionRecoveryTest {
  private final VideoPlatformInstanceMapper mapper = mock(VideoPlatformInstanceMapper.class);
  private final VideoPlatformAdapterRegistry registry = mock(VideoPlatformAdapterRegistry.class);
  private final VideoPlatformAdapter adapter = mock(VideoPlatformAdapter.class);
  private final ObjectMapper json = new ObjectMapper();
  private final VideoSdkEventCatalog catalog = mock(VideoSdkEventCatalog.class);
  private final VideoSdkSubscriptionRecovery recovery = new VideoSdkSubscriptionRecovery(mapper, registry, json, catalog);
  private final VideoPlatformInstance instance = new VideoPlatformInstance();
  private final Map<String, Object> camera = Map.of("deviceId", "camera-one", "host", "192.0.2.1", "port", 37777);
  private final List<Map<String, Object>> runtime = new ArrayList<>();
  private final List<Map<String, Object>> desired = new ArrayList<>();

  @BeforeEach void setup() {
    EntityHelper.initEntityNameMap(VideoPlatformInstance.class, new Config());
    instance.setId(1L);
    instance.setInstanceKey("dahua-test");
    instance.setPlatformType("dahua_sdk");
    instance.setEnabled(1);
    instance.setOptions("{\"subscription\":{\"enabled\":true,\"topics\":[\"motion\"],\"callback\":\"http://localhost/api/video/sdk-events/callback\"}}");
    when(mapper.selectByExample(any())).thenReturn(List.of(instance));
    when(mapper.selectByPrimaryKey(1L)).thenReturn(instance);
    when(registry.getAdapter("dahua_sdk")).thenReturn(adapter);
    desired.add(camera);
    when(adapter.resolveSubscriptionDevices(any())).thenReturn(desired);
    when(adapter.executeOperation(any(), eq("subscriptionStatus"), anyMap())).thenAnswer(call ->
        Map.of("accepted", true, "attributes", Map.of("subscriptions", new ArrayList<>(runtime))));
    doAnswer(call -> { runtime.clear(); runtime.addAll(desired); return null; })
        .when(adapter).subscribe(any(), anyList(), anyString());
    doAnswer(call -> { runtime.clear(); return null; }).when(adapter).unsubscribe(any(), anyList());
  }

  @AfterEach void close() { recovery.close(); }

  @Test void gatewayRestartRecreatesListenersWithoutRepeatedSubscribeWhileHealthy() {
    assertEquals(1, recovery.recoverNow().get("success"));
    assertEquals(0, recovery.recoverNow().get("success"));
    verify(adapter, times(1)).subscribe(any(), eq(List.of("motion")), anyString());
    runtime.clear(); // 模拟 SDK 网关进程重启；数据库配置和 NexIoT 进程保持运行。
    assertEquals(1, recovery.recoverNow().get("success"));
    verify(adapter, times(2)).subscribe(any(), eq(List.of("motion")), anyString());
  }

  @Test void staleDeviceAddressAndOtherDeviceListenersDoNotCountAsRecovered() {
    runtime.add(Map.of("deviceId", "camera-one", "host", "192.0.2.9", "port", 37777));
    runtime.add(Map.of("deviceId", "other-camera", "host", "192.0.2.1", "port", 37777));
    assertEquals(1, recovery.recoverNow().get("success"));
    assertEquals(List.of(camera), runtime);
  }

  @Test void disabledSubscriptionIsNeverReenabledByOldInventorySnapshot() {
    instance.setOptions("{\"subscription\":{\"enabled\":false}}");
    recovery.recoverNow();
    verify(adapter, never()).subscribe(any(), anyList(), anyString());
    clearInvocations(adapter);
    instance.setOptions("{\"subscription\":{\"enabled\":true}}");
    instance.setEnabled(0);
    recovery.recoverNow();
    verify(adapter, never()).subscribe(any(), anyList(), anyString());
  }

  @Test void disabledOrEmptySelectionCleansStaleListenersInsteadOfRestoringAllEvents() {
    for (String subscription : List.of("{\"enabled\":false}", "{\"enabled\":true,\"topics\":[]}", "{\"enabled\":true}")) {
      instance.setOptions("{\"subscription\":" + subscription + "}");
      runtime.add(camera);
      assertEquals(1, recovery.recoverNow().get("success"));
      assertTrue(runtime.isEmpty());
    }
    verify(adapter, times(3)).unsubscribe(any(), eq(List.of()));
    verify(adapter, never()).subscribe(any(), anyList(), anyString());
  }

  @Test void changedTopicsCallbackOrCatalogAreReappliedEvenWhenDevicesMatch() {
    for (Map<String,Object> stale : List.of(
        Map.<String,Object>of("topics",List.of("intrusion")),
        Map.<String,Object>of("callback","http://old/api/video/sdk-events/callback"),
        Map.<String,Object>of("eventCatalog",List.of(Map.of("type","stale"))))) {
      var row = new java.util.LinkedHashMap<>(camera); row.putAll(stale); runtime.clear(); runtime.add(row);
      assertEquals(1, recovery.recoverNow().get("success"));
    }
    verify(adapter, times(3)).subscribe(any(), eq(List.of("motion")), anyString());
    var row = new java.util.LinkedHashMap<>(camera); row.put("topics",List.of("motion","__sdk_ivs")); runtime.clear(); runtime.add(row);
    assertEquals(0,recovery.recoverNow().get("success"));
  }

  @Test void newGatewayMissingCatalogIsRepairedWithoutRejectingLegacyStatus() {
    var trustedCatalog=List.<Map<String,Object>>of(Map.of("type","motion","eventId","motionDetection","sdkCodes",List.of("0x00002102")));
    when(catalog.subscriptionCatalog("dahua_sdk")).thenReturn(trustedCatalog);
    runtime.add(camera);
    assertEquals(0,recovery.recoverNow().get("success")); // 旧版没有新状态字段，仍可兼容。
    var withoutCatalog=new java.util.LinkedHashMap<>(camera);
    withoutCatalog.put("forwardingActive",true);
    runtime.clear();runtime.add(withoutCatalog);
    doAnswer(call->{
      var restored=new java.util.LinkedHashMap<>(withoutCatalog);restored.put("eventCatalog",trustedCatalog);
      runtime.clear();runtime.add(restored);return null;
    }).when(adapter).subscribe(any(),anyList(),anyString());
    assertEquals(1,recovery.recoverNow().get("success"));
    assertEquals(trustedCatalog,runtime.getFirst().get("eventCatalog"));
    assertEquals(0,recovery.recoverNow().get("success"));
    verify(adapter,times(1)).subscribe(any(),eq(List.of("motion")),anyString());
  }

  @Test void unknownCatalogCannotAppearReadyAsAnAuthoritativeEmptyCatalog() {
    var row=new java.util.LinkedHashMap<>(camera);
    row.put("eventCatalog",List.of());row.put("eventCatalogReady",false);
    runtime.add(row);
    assertEquals(1,recovery.recoverNow().get("success"));
    verify(adapter).subscribe(any(),eq(List.of("motion")),anyString());
    row.put("eventCatalogReady",true);runtime.clear();runtime.add(row);
    assertEquals(0,recovery.recoverNow().get("success"));
  }

  @Test void unavailableGatewayUsesBackoffAndManualRetryCanResetIt() {
    when(adapter.executeOperation(any(), anyString(), anyMap())).thenThrow(new IllegalStateException("connection refused"));
    assertEquals(1, recovery.recoverNow().get("failed"));
    recovery.recoverNow();
    verify(adapter, times(1)).executeOperation(any(), anyString(), anyMap());
    assertTrue((long) recovery.metadata(instance.getInstanceKey()).get("nextRecoveryAt") > System.currentTimeMillis());
    recovery.resetRetry(instance.getInstanceKey());
    recovery.recoverNow();
    verify(adapter, times(2)).executeOperation(any(), anyString(), anyMap());
    verify(adapter, never()).subscribe(any(), anyList(), anyString());
  }

  @Test void acceptedSubscribeStillRequiresConfirmedRuntimeListener() {
    doNothing().when(adapter).subscribe(any(), anyList(), anyString());
    assertEquals(1, recovery.recoverNow().get("failed"));
    assertNotNull(recovery.metadata(instance.getInstanceKey()).get("recoveryError"));
  }

  @Test void allScopeAutomaticallyAddsNewDevicesAndRemovesDisabledListeners() {
    runtime.add(camera);
    var added = Map.<String, Object>of("deviceId", "new-camera", "host", "192.0.2.2", "port", 37777);
    desired.add(added);
    assertEquals(1, recovery.recoverNow().get("success"));
    assertEquals(List.of(camera, added), runtime);
    desired.remove(camera); // 停用旧设备，只保留新增设备。
    assertEquals(1, recovery.recoverNow().get("success"));
    assertEquals(List.of(added), runtime);
    assertEquals(0, recovery.recoverNow().get("success"));
    verify(adapter, times(2)).subscribe(any(), anyList(), anyString());
    verify(adapter, atLeastOnce()).executeOperation(any(), eq("subscriptionStatus"), eq(Map.of()));
  }

  @Test void emptyAllScopeRemovesLastListenerAndWaitsForFutureDevice() {
    runtime.add(camera);
    desired.clear();
    assertEquals(1, recovery.recoverNow().get("success"));
    assertTrue(runtime.isEmpty());
    assertEquals(0, recovery.recoverNow().get("success"));
    verify(adapter, times(1)).unsubscribe(any(), eq(List.of()));
    verify(adapter, never()).subscribe(any(), anyList(), anyString());
    desired.add(camera);
    assertEquals(1, recovery.recoverNow().get("success"));
    assertEquals(List.of(camera), runtime);
  }

  @Test void selectedScopeDoesNotKeepListenersOutsideItsCurrentRange() {
    instance.setOptions("{\"subscription\":{\"enabled\":true,\"scope\":\"selected\",\"deviceIds\":[\"camera-one\"],\"topics\":[\"motion\"],\"callback\":\"http://localhost/api/video/sdk-events/callback\"}}");
    runtime.add(camera);
    runtime.add(Map.of("deviceId", "excluded", "host", "192.0.2.3", "port", 37777));
    assertEquals(1, recovery.recoverNow().get("success"));
    assertEquals(List.of(camera), runtime);
    verify(adapter).subscribe(any(), anyList(), anyString());
  }

  @Test void failedCleanupIsRetriedRatherThanReportedAsHealthy() {
    runtime.add(camera);
    desired.clear();
    doNothing().when(adapter).unsubscribe(any(), anyList());
    assertEquals(1, recovery.recoverNow().get("failed"));
    assertNotNull(recovery.metadata(instance.getInstanceKey()).get("recoveryError"));
  }

  @Test void revokedListenerIsRecreatedEvenWhenNativeHandleAndConfigurationRemain() {
    var revoked = new java.util.LinkedHashMap<>(camera);
    revoked.put("success",true);
    revoked.put("forwardingActive",false);
    revoked.put("topics",List.of("motion"));
    revoked.put("callback","http://localhost/api/video/sdk-events/callback");
    runtime.add(revoked);
    assertFalse(VideoSdkSubscriptionRecovery.matchesDevice(camera,revoked));
    assertEquals(1,recovery.recoverNow().get("success"));
    assertEquals(List.of(camera),runtime);
    verify(adapter).subscribe(any(),eq(List.of("motion")),anyString());
  }

  @Test void malformedStatusAndFailedRuntimeEntriesAreNotHealthy() {
    assertThrows(IllegalStateException.class, () -> VideoSdkSubscriptionRecovery.runtimeSubscriptions(Map.of()));
    assertThrows(IllegalStateException.class, () -> VideoSdkSubscriptionRecovery.runtimeSubscriptions(Map.of("accepted", false)));
    assertFalse(VideoSdkSubscriptionRecovery.matchesDevice(camera,
        Map.of("deviceId", "camera-one", "host", "192.0.2.1", "port", "37777", "success", false)));
    assertTrue(VideoSdkSubscriptionRecovery.matchesDevice(camera,
        Map.of("deviceId", "camera-one", "host", "192.0.2.1", "port", "37777", "success", true)));
  }

  @ParameterizedTest
  @CsvSource({
      "all, false, true",
      "all, true, false",
      "all, false, false",
      "selected, false, true",
      "selected, true, false",
      "selected, false, false"
  })
  void univiewInvalidListenerTriggersSubscribeDespiteMatchingScopeDevicesAndCounts(
      String scope, boolean success, boolean forwardingActive) {
    configureCompleteUniviewRuntime(scope);
    var invalid = runtime.getFirst();
    invalid.put("success", success);
    invalid.put("forwardingActive", forwardingActive);
    invalid.put("nativeInvalidReason", "native session disconnected");
    assertEquals(desired.size(), runtime.size());
    for (int index = 0; index < desired.size(); index++) {
      for (String field : List.of("deviceId", "host", "port")) {
        assertEquals(desired.get(index).get(field), runtime.get(index).get(field));
      }
    }

    var result = recovery.recoverNow();

    assertEquals(1, result.get("success"));
    assertEquals(0, result.get("failed"));
    var sequence = inOrder(adapter);
    sequence.verify(adapter).executeOperation(same(instance), eq("subscriptionStatus"), eq(Map.of()));
    sequence.verify(adapter).subscribe(
        same(instance), eq(List.of("motion")), eq("http://localhost/api/video/sdk-events/callback"));
    sequence.verify(adapter).executeOperation(same(instance), eq("subscriptionStatus"), eq(Map.of()));
    assertTrue(runtime.stream().allMatch(row -> Boolean.TRUE.equals(row.get("success"))
        && Boolean.TRUE.equals(row.get("forwardingActive"))));
    assertEquals(0, recovery.recoverNow().get("success"));
    verify(adapter, times(1)).subscribe(same(instance), eq(List.of("motion")), anyString());
    verify(adapter, never()).unsubscribe(any(), anyList());
    assertFalse(recovery.metadata(instance.getInstanceKey()).containsKey("recoveryError"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"all", "selected"})
  void univiewCompleteHealthyStatusDoesNotRecreateListeners(String scope) {
    configureCompleteUniviewRuntime(scope);

    assertEquals(0, recovery.recoverNow().get("success"));
    assertEquals(0, recovery.recoverNow().get("success"));

    verify(adapter, times(2)).executeOperation(
        same(instance), eq("subscriptionStatus"), eq(Map.of()));
    verify(adapter, never()).subscribe(any(), anyList(), anyString());
    verify(adapter, never()).unsubscribe(any(), anyList());
    assertFalse(recovery.metadata(instance.getInstanceKey()).containsKey("recoveryError"));
  }

  @ParameterizedTest
  @CsvSource({"false, true", "true, false"})
  void univiewAcceptedSubscribeStillFailsRecoveryWhenConfirmedListenerRemainsInvalid(
      boolean success, boolean forwardingActive) {
    configureCompleteUniviewRuntime("all");
    runtime.getFirst().put("success", success);
    runtime.getFirst().put("forwardingActive", forwardingActive);
    doNothing().when(adapter).subscribe(any(), anyList(), anyString());

    var result = recovery.recoverNow();

    assertEquals(0, result.get("success"));
    assertEquals(1, result.get("failed"));
    verify(adapter).subscribe(same(instance), eq(List.of("motion")), anyString());
    verify(adapter, times(2)).executeOperation(
        same(instance), eq("subscriptionStatus"), eq(Map.of()));
    assertNotNull(recovery.metadata(instance.getInstanceKey()).get("recoveryError"));
  }

  private void configureCompleteUniviewRuntime(String scope) {
    instance.setInstanceKey("uniview-test");
    instance.setPlatformType("uniview_sdk");
    instance.setOptions("{\"subscription\":{\"enabled\":true,\"scope\":\"" + scope
        + "\",\"deviceIds\":[\"camera-one\",\"camera-two\"],\"topics\":[\"motion\"],"
        + "\"callback\":\"http://localhost/api/video/sdk-events/callback\"}}");
    when(registry.getAdapter("uniview_sdk")).thenReturn(adapter);
    when(catalog.subscriptionCatalog("uniview_sdk")).thenReturn(univiewCatalog());
    desired.clear();
    desired.add(Map.of("deviceId", "camera-one", "host", "192.0.2.1", "port", 80));
    desired.add(Map.of("deviceId", "camera-two", "host", "192.0.2.2", "port", 80));
    runtime.clear();
    desired.stream().map(this::completeUniviewRuntimeRow).forEach(runtime::add);
    when(adapter.executeOperation(any(), eq("subscriptionStatus"), anyMap())).thenAnswer(call ->
        Map.of("accepted", true, "attributes", Map.of(
            "count", runtime.size(),
            // Aggregate counts alone must not hide an individual failed listener.
            "successCount", runtime.size(),
            "subscriptions", new ArrayList<>(runtime))));
    doAnswer(call -> {
      runtime.clear();
      desired.stream().map(this::completeUniviewRuntimeRow).forEach(runtime::add);
      return null;
    }).when(adapter).subscribe(any(), anyList(), anyString());
  }

  private Map<String, Object> completeUniviewRuntimeRow(Map<String, Object> device) {
    var row = new java.util.LinkedHashMap<>(device);
    row.put("instanceKey", instance.getInstanceKey());
    row.put("success", true);
    row.put("forwardingActive", true);
    row.put("topics", List.of("motion"));
    row.put("callback", "http://localhost/api/video/sdk-events/callback");
    row.put("eventCatalog", univiewCatalog());
    row.put("eventCatalogReady", true);
    return row;
  }

  private List<Map<String, Object>> univiewCatalog() {
    return List.of(Map.of("type", "motion", "eventId", "motionDetection", "sdkCodes", List.of("MOTION")));
  }
}
