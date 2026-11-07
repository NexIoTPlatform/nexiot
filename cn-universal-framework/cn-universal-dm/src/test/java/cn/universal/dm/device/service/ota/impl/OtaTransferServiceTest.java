package cn.universal.dm.device.service.ota.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.dm.device.service.ota.api.OtaService.OtaServiceResult;
import cn.universal.dm.device.service.ota.support.OtaConstants;
import cn.universal.dm.device.service.ota.support.OtaFirmwareStore;
import cn.universal.dm.device.service.ota.support.OtaTransferProfile;
import cn.universal.persistence.entity.IoTOtaPackage;
import cn.universal.persistence.entity.IoTOtaTask;
import cn.universal.persistence.entity.IoTOtaTaskDevice;
import cn.universal.persistence.mapper.IoTOtaPackageMapper;
import cn.universal.persistence.mapper.IoTOtaTaskDeviceMapper;
import cn.universal.persistence.mapper.IoTOtaTaskMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OtaTransferServiceTest {
  @TempDir Path directory;
  IoTOtaTaskDeviceMapper devices;
  IoTOtaPackageMapper packages;
  OtaFirmwareStore store;
  IoTOtaTaskMapper tasks;
  RecordingService service;
  IoTOtaTaskDevice device;
  IoTOtaPackage firmware;

  @BeforeEach void setup() throws Exception {
    devices = mock(IoTOtaTaskDeviceMapper.class);
    packages = mock(IoTOtaPackageMapper.class);
    store = mock(OtaFirmwareStore.class);
    tasks = mock(IoTOtaTaskMapper.class);
    service = new RecordingService(devices, packages, store, tasks);
    device = IoTOtaTaskDevice.builder().id(1L).taskId(2L).packageId(3L)
        .productKey("product").deviceId("device").protocol("tcp")
        .status(OtaConstants.DeviceStatus.PUSHED).progress(0).build();
    firmware = IoTOtaPackage.builder().id(3L).fileSize(129L).build();
    when(devices.selectTaskDeviceForUpdate(1L)).thenReturn(device);
    when(devices.selectTaskDeviceById(1L)).thenReturn(device);
    when(devices.updateTaskDeviceIfActive(any())).thenReturn(1);
    when(packages.selectPackageById(3L)).thenReturn(firmware);
    Path path = directory.resolve("firmware.bin");
    Files.write(path, new byte[129]);
    when(store.prepare(firmware)).thenReturn(path);
    when(store.readChunk(firmware, 128, 0)).thenReturn(new byte[128]);
    when(store.readChunk(firmware, 128, 1)).thenReturn(new byte[] {42});
  }

  @Test void pushRequiresReadyAndOrderedAcksThenWaitsForInstallation() throws Exception {
    initialize("PUSH", 1);
    assertTrue(service.sent.isEmpty());
    assertEquals(OtaServiceResult.SUCCESS, service.handleReport(report("TRANSFER_ACK", "ready", null)));
    assertEquals("chunk", service.lastData().getStr("action"));
    assertEquals(1, service.lastData().getInt("chunkIndex"));
    assertEquals(128, service.lastData().getInt("chunkLength"));

    assertEquals(OtaServiceResult.PROTOCOL_ERROR, service.handleReport(report("TRANSFER_ACK", "chunk", 2)));
    assertEquals(1, service.sent.size());
    assertEquals(OtaServiceResult.SUCCESS, service.handleReport(report("TRANSFER_ACK", "chunk", 1)));
    assertEquals(2, service.lastData().getInt("chunkIndex"));
    assertEquals(1, service.lastData().getInt("chunkLength"));
    assertEquals(OtaServiceResult.PROTOCOL_ERROR, service.handleReport(report("TRANSFER_ACK", "chunk", 1)));
    assertEquals(OtaServiceResult.SUCCESS, service.handleReport(report("TRANSFER_ACK", "chunk", 2)));
    assertEquals("finish", service.lastData().getStr("action"));
    assertEquals(2, state().getInt("ackedChunks"));
    assertNotEquals(OtaConstants.DeviceStatus.SUCCESS, device.getStatus());
    assertEquals(OtaServiceResult.SUCCESS, service.handleReport(report("TRANSFER_ACK", "finish", null)));
    assertEquals("COMPLETE", state().getStr("phase"));
    assertEquals(OtaConstants.DeviceStatus.VERIFYING, device.getStatus());
  }

  @Test void pullCannotRequestNextChunkUntilPreviousAck() throws Exception {
    initialize("PULL", 0);
    service.handleReport(report("TRANSFER_ACK", "ready", null));
    assertEquals("WAIT_REQUEST", state().getStr("phase"));
    assertTrue(service.sent.isEmpty());
    assertEquals(OtaServiceResult.PROTOCOL_ERROR, service.handleReport(report("TRANSFER_REQUEST", "chunk", 1)));
    service.handleReport(report("TRANSFER_REQUEST", "chunk", 0));
    assertEquals(0, service.lastData().getInt("chunkIndex"));
    assertEquals(OtaServiceResult.PROTOCOL_ERROR, service.handleReport(report("TRANSFER_REQUEST", "chunk", 1)));
    service.handleReport(report("TRANSFER_ACK", "chunk", 0));
    assertEquals("WAIT_REQUEST", state().getStr("phase"));
    service.handleReport(report("TRANSFER_REQUEST", "chunk", 1));
    assertEquals(1, service.lastData().getInt("chunkIndex"));
  }

  @Test void oldSessionAndWrongDeviceCannotAdvanceOrFailCurrentTransfer() throws Exception {
    initialize("PUSH", 0);
    Map<String, Object> old = report("TRANSFER_ACK", "ready", null);
    old.put("sessionId", "previous-session");
    old.put("success", false);
    assertEquals(OtaServiceResult.PROTOCOL_ERROR, service.handleReport(old));
    Map<String, Object> foreign = report("TRANSFER_ACK", "ready", null);
    foreign.put("deviceId", "another-device");
    assertEquals(OtaServiceResult.PROTOCOL_ERROR, service.handleReport(foreign));
    assertEquals("START", state().getStr("phase"));
    assertTrue(service.sent.isEmpty());
  }

  @Test void restartResendsOnlyPersistedInflightChunkAndStopsAtRetryBudget() throws Exception {
    initialize("PUSH", 0);
    service.handleReport(report("TRANSFER_ACK", "ready", null));
    service.handleReport(report("TRANSFER_ACK", "chunk", 0));
    RecordingService restarted = new RecordingService(devices, packages, store, tasks);
    for (int retry = 1; retry <= 2; retry++) {
      expire();
      assertTrue(restarted.resume(1L));
      assertEquals(1, restarted.lastData().getInt("chunkIndex"));
      assertEquals(1, state().getInt("ackedChunks"));
      assertEquals(retry, state().getInt("retries"));
    }
    expire();
    restarted.resume(1L);
    assertEquals(OtaConstants.DeviceStatus.FAILED, device.getStatus());
    assertEquals(2, restarted.sent.size());
    assertFalse(restarted.resume(1L));
  }

  @Test void cancelledTransferNeverSendsAnotherChunk() throws Exception {
    initialize("PUSH", 0);
    service.handleReport(report("TRANSFER_ACK", "ready", null));
    device.setStatus(OtaConstants.DeviceStatus.CANCELED);
    expire();
    assertFalse(service.resume(1L));
    assertEquals(OtaServiceResult.PROTOCOL_ERROR, service.handleReport(report("TRANSFER_ACK", "chunk", 0)));
    assertEquals(1, service.sent.size());
  }

  @Test void freshManualAttemptHasNewSessionAndHexEncodingWorks() throws Exception {
    initialize("PUSH", 0);
    String previous = state().getStr("sessionId");
    OtaTransferProfile profile = OtaTransferProfile.from(state().get("profile"));
    profile.setDataEncoding("HEX");
    initialize(profile);
    assertNotEquals(previous, state().getStr("sessionId"));
    service.handleReport(report("TRANSFER_ACK", "ready", null));
    service.handleReport(report("TRANSFER_ACK", "chunk", 0));
    assertEquals("2a", service.lastData().getStr("chunkData"));
    assertEquals(64, service.lastData().getStr("chunkChecksum").length());
  }

  @Test void missingLegacyProfileRetainsUrlBehavior() throws Exception {
    IoTOtaTask task = IoTOtaTask.builder().id(2L).build();
    Map<String, Object> function = function();
    service.initialize(task, device, firmware, function);
    assertTrue(device.getTransferState().isEmpty());
    verify(store, never()).prepare(any());
    assertTrue(service.canConfirmInstallation(device));
  }

  @Test void pullWaitingForRequestExpiresWithoutInfiniteResends() throws Exception {
    initialize("PULL", 0);
    service.handleReport(report("TRANSFER_ACK", "ready", null));
    JSONObject state = state();
    state.set("sentAt", System.currentTimeMillis() - 100_000);
    device.setTransferState(state.toString());
    service.resume(1L);
    assertEquals(OtaConstants.DeviceStatus.FAILED, device.getStatus());
    assertTrue(service.sent.isEmpty());
  }

  @Test void storeReadFailureStopsInsteadOfSendingTruncatedFirmware() throws Exception {
    initialize("PUSH", 0);
    when(store.readChunk(firmware, 128, 0)).thenReturn(new byte[5]);
    assertEquals(OtaServiceResult.FAILED, service.handleReport(report("TRANSFER_ACK", "ready", null)));
    assertEquals(OtaConstants.DeviceStatus.FAILED, device.getStatus());
    assertTrue(service.sent.isEmpty());
  }

  @Test void transferAlwaysLocksTaskBeforeTaskDevice() throws Exception {
    initialize("PUSH", 0);
    clearInvocations(devices, tasks);
    service.handleReport(report("TRANSFER_ACK", "ready", null));
    var order = inOrder(devices, tasks);
    order.verify(devices).selectTaskDeviceById(1L);
    order.verify(tasks).selectTaskForUpdate(2L);
    order.verify(devices).selectTaskDeviceForUpdate(1L);
  }

  private void initialize(String mode, int indexBase) throws Exception {
    OtaTransferProfile profile = new OtaTransferProfile();
    profile.setMode(mode);
    profile.setChunkSize(128);
    profile.setChunkMaxRetries(2);
    profile.setIndexBase(indexBase);
    initialize(profile);
  }

  private void initialize(OtaTransferProfile profile) throws Exception {
    JSONObject strategy = new JSONObject().set("transferProfile", profile.toMap());
    IoTOtaTask task = IoTOtaTask.builder().id(2L).strategyJson(strategy.toString()).build();
    Map<String, Object> function = function();
    service.initialize(task, device, firmware, function);
    device.setCommandPayload(JSONUtil.toJsonStr(function));
  }

  private Map<String, Object> function() {
    Map<String, Object> function = new LinkedHashMap<>();
    function.put("messageType", "FUNCTIONS");
    function.put("function", "otaUpgrade");
    function.put("data", new LinkedHashMap<>(Map.of("action", "start", "taskId", "2")));
    return function;
  }

  private JSONObject state() { return JSONUtil.parseObj(device.getTransferState()); }
  private void expire() {
    JSONObject state = state();
    state.set("sentAt", System.currentTimeMillis() - 60_000);
    device.setTransferState(state.toString());
  }

  private Map<String, Object> report(String type, String action, Integer index) {
    Map<String, Object> report = new LinkedHashMap<>();
    report.put("reportType", type);
    report.put("transferAction", action);
    report.put("taskDeviceId", 1L);
    report.put("taskId", 2L);
    report.put("productKey", "product");
    report.put("deviceId", "device");
    report.put("sessionId", state().getStr("sessionId"));
    if (index != null) report.put("chunkIndex", index);
    return report;
  }

  static class RecordingService extends OtaTransferService {
    List<JSONObject> sent = new ArrayList<>();
    RecordingService(IoTOtaTaskDeviceMapper devices, IoTOtaPackageMapper packages, OtaFirmwareStore store, IoTOtaTaskMapper tasks) {
      super(devices, packages, store, tasks);
    }
    @Override protected void send(IoTOtaTaskDevice device, Map<String, Object> function) throws IOException {
      sent.add(JSONUtil.parseObj(JSONUtil.toJsonStr(function)));
    }
    JSONObject lastData() { return sent.get(sent.size() - 1).getJSONObject("data"); }
  }
}
