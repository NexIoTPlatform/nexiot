package cn.universal.admin.video.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import cn.hutool.json.JSONUtil;
import cn.universal.dm.device.service.log.IIoTDeviceDataService;
import cn.universal.dm.device.service.log.HistoryPageSupport;
import cn.universal.ossm.service.ISysOssService;
import cn.universal.ossm.util.VideoCloudEvidenceIndex;
import cn.universal.ossm.util.VideoCloudEvidenceService;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.vo.IoTDeviceLogVO;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PageBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VideoCloudEvidenceTimelineServiceTest {
  @Test void cursorReadsBeyondTwoHundredAndKeepsFilteredBatchBoundariesAndSameTimeRows() {
    var rows = new java.util.ArrayList<IoTDeviceLogVO>();
    var time = java.time.LocalDateTime.of(2026, 10, 10, 10, 0);
    for (long id = 1000; id > 0; id--) {
      var row = new IoTDeviceLogVO();
      row.setId(id); row.setCreateTime(time); row.setEvent(id % 2 == 0 ? "motion" : "line");
      row.setContent(JSONUtil.toJsonStr(Map.of("channelId", id % 3 == 0 ? "2" : "1", "cameraEventType", row.getEvent())));
      rows.add(row);
    }
    var logs = cursorLogs(rows);
    var service = new VideoCloudEvidenceTimelineService(logs, mock(ISysOssService.class),
        mock(VideoCloudEvidenceIndex.class), mock(VideoCloudEvidenceService.class), List.of());
    var device = IoTDevice.builder().productKey("p").iotId("pcam").deviceId("cam").tenantId("t").build();
    var body = new java.util.HashMap<String, Object>();
    long epoch = time.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    body.put("startTime", epoch - 1000); body.put("endTime", epoch + 1000);
    body.put("pageSize", 50); body.put("channelId", "1"); body.put("eventType", "motion");
    var found = new java.util.ArrayList<String>();
    for (int page = 0; ; page++) {
      assertTrue(page < 30, "游标必须前进并最终结束");
      var response = service.timeline(device, body);
      var events = (List<Map<String, Object>>) response.get("events");
      assertTrue(events.size() <= 50);
      found.addAll(events.stream().map(e -> (String) e.get("evidenceId")).toList());
      if (!(boolean) response.get("hasMore")) {
        assertEquals("", response.get("nextCursor"));
        break;
      }
      body.put("cursor", response.get("nextCursor"));
    }
    assertEquals(rows.stream().filter(r -> r.getId() % 2 == 0 && r.getId() % 3 != 0)
        .map(r -> r.getId().toString()).toList(), found);
    assertTrue(found.size() > 200);
    String cursor = (String) body.get("cursor");
    body.put("deviceId", "other");
    var other = IoTDevice.builder().productKey("p").iotId("pother").deviceId("other").tenantId("t").build();
    assertThrows(RuntimeException.class, () -> service.timeline(other, body));
    assertFalse(cursor.isBlank());
  }

  @Test void sparseChannelCanContinueAfterThePerRequestScanBudget() {
    var rows = new java.util.ArrayList<IoTDeviceLogVO>();
    var time = java.time.LocalDateTime.of(2026, 10, 10, 10, 0);
    for (long id = 230; id > 0; id--) {
      var row = new IoTDeviceLogVO(); row.setId(id); row.setCreateTime(time);
      row.setContent(JSONUtil.toJsonStr(Map.of("channelId", id == 1 ? "target" : "other")));
      rows.add(row);
    }
    var service = new VideoCloudEvidenceTimelineService(cursorLogs(rows), mock(ISysOssService.class),
        mock(VideoCloudEvidenceIndex.class), mock(VideoCloudEvidenceService.class), List.of());
    var device = IoTDevice.builder().productKey("p").iotId("pcam").deviceId("cam").tenantId("t").build();
    long epoch = time.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    var body = new java.util.HashMap<String, Object>(Map.of("startTime", epoch - 1000, "endTime", epoch + 1000,
        "channelId", "target", "pageSize", 10));
    for (int page = 0; page < 2; page++) {
      var result = service.timeline(device, body);
      assertEquals(List.of(), result.get("events"));
      assertEquals(true, result.get("hasMore"));
      body.put("cursor", result.get("nextCursor"));
    }
    var result = service.timeline(device, body);
    assertEquals(false, result.get("hasMore"));
    assertEquals("1", ((List<Map<String, Object>>) result.get("events")).getFirst().get("evidenceId"));
  }

  private IIoTDeviceDataService cursorLogs(List<IoTDeviceLogVO> rows) {
    var logs = mock(IIoTDeviceDataService.class);
    when(logs.pageList(any(LogQuery.class))).thenAnswer(call -> {
      LogQuery query = call.getArgument(0);
      HistoryPageSupport.prepare(query, false);
      var fetched = rows.stream().filter(row -> query.getBeforeId() == null || row.getId() < query.getBeforeId())
          .limit(query.getFetchSize()).toList();
      return HistoryPageSupport.page(fetched, query, IoTDeviceLogVO::getCreateTime, IoTDeviceLogVO::getId);
    });
    return logs;
  }

  @Test void historicalVendorEventDisplaysTheCurrentAuthoritativeModelName() {
    var logs = mock(IIoTDeviceDataService.class);
    var row = new IoTDeviceLogVO(); row.setId(3L); row.setEvent("vendor_336500");
    row.setContent("{\"cameraEventType\":\"vendor_336500\",\"cameraEventName\":\"厂家事件（336500）\"}");
    when(logs.pageList(any(LogQuery.class))).thenReturn(new PageBean<>(List.of(row), 1L, 200, 1));
    var service = new VideoCloudEvidenceTimelineService(logs, mock(ISysOssService.class),
        mock(VideoCloudEvidenceIndex.class), mock(VideoCloudEvidenceService.class), List.of());
    var device = IoTDevice.builder().productKey("product").iotId("productcamera")
        .deviceId("camera").tenantId("tenant").build();
    var product = cn.universal.persistence.entity.IoTProduct.builder()
        .metadata("{\"events\":[{\"id\":\"vendor_336500\",\"name\":\"区域入侵检测报警\"}]}").build();
    var entries = (List<Map<String, Object>>) service.timeline(device, product, Map.of()).get("events");
    assertEquals("区域入侵检测报警", entries.getFirst().get("cameraEventName"));
    assertEquals("vendor_336500", entries.getFirst().get("cameraEventType"));
  }

  @Test void standardEventEnvelopeReadsTheSameEvidenceAndNestedVendorFieldsAsLegacyData() {
    var logs = mock(IIoTDeviceDataService.class);
    var row = new IoTDeviceLogVO(); row.setId(2L); row.setEvent("motionDetection");
    row.setContent(JSONUtil.toJsonStr(Map.of("messageType", "EVENT", "event", "motionDetection",
        "eventName", "移动侦测", "data", Map.of("cameraEventType", "motionDetection",
            "cameraEventName", "移动侦测", "channelId", "0", "instanceKey", "product",
            "data", Map.of("confidence", 92), "occurredAt", 1791633414000L,
            "evidence", List.of(Map.of("status", "encrypted", "message", "密码错误"))))));
    when(logs.pageList(any(LogQuery.class))).thenReturn(new PageBean<>(List.of(row), 1L, 200, 1));
    var service = new VideoCloudEvidenceTimelineService(logs, mock(ISysOssService.class),
        mock(VideoCloudEvidenceIndex.class), mock(VideoCloudEvidenceService.class), List.of());
    var response = service.timeline(IoTDevice.builder().productKey("product").iotId("productcamera")
        .deviceId("camera").tenantId("tenant").build(), Map.of("channelId", "0"));
    var entries = (List<Map<String, Object>>) response.get("events");
    assertEquals(1, entries.size());
    assertEquals("motionDetection", entries.getFirst().get("cameraEventType"));
    assertEquals("移动侦测", entries.getFirst().get("cameraEventName"));
    assertEquals("密码错误", entries.getFirst().get("imageMessage"));
    var data = (Map<?, ?>) entries.getFirst().get("data");
    assertEquals(92, ((Map<?, ?>) data.get("data")).get("confidence"));
    assertFalse(data.containsKey("messageType"));
  }

  @Test void vendorDataDoesNotDiscardTopLevelEvidenceAndSourceUrlsStayPrivate() {
    var logs = mock(IIoTDeviceDataService.class);
    var row = new IoTDeviceLogVO(); row.setId(1L); row.setEvent("vendor_336500");
    row.setContent(JSONUtil.toJsonStr(Map.of("cameraEventType", "vendor_336500", "channelId", "0",
        "instanceKey", "product", "data", Map.of("confidence", 90), "occurredAt", 1791633414000L,
        "picUrlArr", List.of("https://vendor.example/private?signature=secret"),
        "evidence", List.of(Map.of("status", "encrypted", "message", "密码错误")))));
    when(logs.pageList(any(LogQuery.class))).thenReturn(new PageBean<>(List.of(row), 1L, 200, 1));
    var service = new VideoCloudEvidenceTimelineService(logs, mock(ISysOssService.class),
        mock(VideoCloudEvidenceIndex.class), mock(VideoCloudEvidenceService.class), List.of());
    var response = service.timeline(IoTDevice.builder().productKey("product").iotId("productcamera")
        .deviceId("camera").tenantId("tenant").build(), Map.of("channelId", "0"));
    var entries = (List<Map<String, Object>>) response.get("events");
    assertEquals(1, entries.size());
    assertEquals("vendor_336500", entries.getFirst().get("cameraEventType"));
    assertEquals("密码错误", entries.getFirst().get("imageMessage"));
    assertFalse(response.toString().contains("signature=secret"));
    assertEquals(90, ((Map<?, ?>) ((Map<?, ?>) entries.getFirst().get("data")).get("data")).get("confidence"));
  }

  @Test void cameraPicturePasswordIsMaskedInGenericDeviceHttpSerialization() throws Exception {
    var device = IoTDevice.builder().configuration("{\"pictureSecret\":\"real-password\",\"channelList\":[0]}").build();
    String json = new ObjectMapper().writeValueAsString(device);
    assertFalse(json.contains("real-password"));
    assertTrue(json.contains("***"));
    assertTrue(json.contains("channelList"));
    assertTrue(device.getConfiguration().contains("real-password"));
    var detail = new cn.universal.persistence.entity.vo.IoTDeviceDetailVO();
    detail.setConfiguration(device.getConfiguration());
    assertFalse(new ObjectMapper().writeValueAsString(detail).contains("real-password"));
  }

  @Test void retryIsBoundedToAuthorizedEventRowsAndPersistsWithoutReplayingThem() {
    var logs = mock(IIoTDeviceDataService.class);
    var indexes = mock(VideoCloudEvidenceIndex.class);
    var evidence = mock(VideoCloudEvidenceService.class);
    var service = new VideoCloudEvidenceTimelineService(logs, mock(ISysOssService.class), indexes, evidence, List.of());
    var device = IoTDevice.builder().productKey("p").deviceId("cam").iotId("pcam").tenantId("t").build();
    var product = cn.universal.persistence.entity.IoTProduct.builder().productKey("p").thirdPlatform("imoulife").build();
    var row = new IoTDeviceLogVO(); row.setId(12L); row.setProductKey("p"); row.setDeviceId("cam"); row.setMessageType("EVENT");
    row.setContent("{\"cameraEventType\":\"motion\",\"picUrlArr\":[\"https://camera.example/signed\"]}");
    when(logs.queryById(any(LogQuery.class))).thenReturn(row);
    when(evidence.retry(eq("t"), eq("p"), eq("cam"), anyMap(), isNull(), eq(20))).thenAnswer(call -> {
      Map<String, Object> data = call.getArgument(3);
      data.put("evidence", List.of(Map.of("status", "encrypted", "ossId", "1")));
      data.put("imageStatus", "encrypted");
      return 2;
    });
    var result = service.retry(device, product, Map.of("evidenceIds", List.of("12")));
    assertEquals(1, result.get("processed")); assertEquals(2, result.get("resourceAttempts"));
    verify(indexes).save(eq("t"), eq("p"), eq("cam"), eq("12"), anyMap());
    verify(logs, never()).saveDeviceLog(any(cn.universal.persistence.base.BaseUPRequest.class), any(), any());
    row.setDeviceId("other");
    assertThrows(cn.universal.common.exception.BaseException.class,
        () -> service.retry(device, product, Map.of("evidenceIds", List.of("12"))));
    assertThrows(cn.universal.common.exception.BaseException.class,
        () -> service.retry(device, product, Map.of("evidenceIds", java.util.Collections.nCopies(21, "12"))));
  }

  @Test void unchangedRetryDoesNotUploadAnIndexEvenWithoutAPreviousIndex() {
    var fixture = retryFixture(Map.of("cameraEventType", "motion", "imageStatus", "encrypted",
        "evidence", List.of(Map.of("status", "encrypted", "ossId", "1", "message", "密码错误"))));
    when(fixture.evidence.retry(eq("t"), eq("p"), eq("cam"), anyMap(), isNull(), eq(20)))
        .thenReturn(1);

    var result = fixture.service.retry(fixture.device, fixture.product, Map.of("evidenceIds", List.of("12")));

    assertEquals(1, result.get("resourceAttempts"));
    assertEquals("密码错误", ((List<Map<String, Object>>) result.get("events")).getFirst().get("imageMessage"));
    verify(fixture.indexes, never()).save(any(), any(), any(), any(), anyMap());
  }

  @Test void equivalentArchivedRetryIgnoresMapOrderAndNumericRepresentations() {
    var fixture = retryFixture(Map.of("cameraEventType", "motion", "imageStatus", "failed"));
    when(fixture.indexes.read(eq("t"), eq("p"), eq("cam"), eq(List.of("12"))))
        .thenReturn(Map.of("12", Map.of("imageStatus", "encrypted", "evidence", List.of(Map.of(
            "status", "encrypted", "ossId", "1", "size", 42L,
            "original", Map.of("size", new BigDecimal("42.0"), "ossId", "1"))))));
    when(fixture.evidence.retry(eq("t"), eq("p"), eq("cam"), anyMap(), isNull(), eq(20)))
        .thenAnswer(call -> {
          Map<String, Object> data = call.getArgument(3);
          Map<String, Object> replacement = new LinkedHashMap<>();
          replacement.put("original", Map.of("ossId", "1", "size", 42));
          replacement.put("size", new BigDecimal("42.00"));
          replacement.put("ossId", "1"); replacement.put("status", "encrypted");
          data.put("evidence", List.of(replacement));
          return 1;
        });

    var result = fixture.service.retry(fixture.device, fixture.product, Map.of("evidenceIds", List.of("12")));

    assertEquals(1, result.get("resourceAttempts"));
    assertEquals("encrypted", ((List<Map<String, Object>>) result.get("events")).getFirst().get("imageStatus"));
    verify(fixture.indexes, never()).save(any(), any(), any(), any(), anyMap());
  }

  @Test void changedFailureIsSavedEvenWhenRetryMutatesANestedEntryInPlaceAndSurvivesRefresh() {
    var fixture = retryFixture(Map.of("cameraEventType", "motion", "imageStatus", "encrypted",
        "evidence", List.of(Map.of("status", "encrypted", "ossId", "1", "message", "等待重试"))));
    Map<String, Map<String, Object>> persisted = new LinkedHashMap<>();
    when(fixture.indexes.read(eq("t"), eq("p"), eq("cam"), anyList()))
        .thenAnswer(call -> persisted);
    doAnswer(call -> {
      Map<String, Object> data = call.getArgument(4);
      persisted.put("12", new LinkedHashMap<>(JSONUtil.parseObj(JSONUtil.toJsonStr(data))));
      return null;
    }).when(fixture.indexes).save(eq("t"), eq("p"), eq("cam"), eq("12"), anyMap());
    when(fixture.evidence.retry(eq("t"), eq("p"), eq("cam"), anyMap(), isNull(), eq(20)))
        .thenAnswer(call -> {
          Map<String, Object> data = call.getArgument(3);
          var entry = (Map<String, Object>) ((List<?>) data.get("evidence")).getFirst();
          entry.put("message", "相机图片解密密码错误");
          entry.put("decodeError", "IllegalStateException");
          return 1;
        });

    var result = fixture.service.retry(fixture.device, fixture.product, Map.of("evidenceIds", List.of("12")));
    assertEquals("相机图片解密密码错误", ((List<Map<String, Object>>) result.get("events")).getFirst().get("imageMessage"));
    verify(fixture.indexes).save(eq("t"), eq("p"), eq("cam"), eq("12"), anyMap());

    // A fresh page reads the saved result even though the original event remains unchanged.
    var refreshed = (List<Map<String, Object>>) fixture.service.timeline(fixture.device, Map.of()).get("events");
    assertEquals("相机图片解密密码错误", refreshed.getFirst().get("imageMessage"));
    assertTrue(fixture.row.getContent().contains("等待重试"));
    fixture.service.retry(fixture.device, fixture.product, Map.of("evidenceIds", List.of("12")));
    verify(fixture.indexes, times(1)).save(eq("t"), eq("p"), eq("cam"), eq("12"), anyMap());
  }

  @Test void successfulRetryAndStatusOnlyChangesArePersisted() {
    var fixture = retryFixture(Map.of("cameraEventType", "motion", "imageStatus", "encrypted",
        "evidence", List.of(Map.of("status", "encrypted", "ossId", "1"))));
    when(fixture.evidence.retry(eq("t"), eq("p"), eq("cam"), anyMap(), isNull(), eq(20)))
        .thenAnswer(call -> {
          Map<String, Object> data = call.getArgument(3);
          data.put("evidence", List.of(Map.of("status", "ready", "ossId", "2")));
          data.put("imageStatus", "ready");
          return 1;
        }).thenAnswer(call -> {
          Map<String, Object> data = call.getArgument(3);
          data.put("imageStatus", "failed");
          return 1;
        });

    var result = fixture.service.retry(fixture.device, fixture.product, Map.of("evidenceIds", List.of("12")));
    assertEquals(1, result.get("ready"));
    fixture.service.retry(fixture.device, fixture.product, Map.of("evidenceIds", List.of("12")));
    verify(fixture.indexes, times(2)).save(eq("t"), eq("p"), eq("cam"), eq("12"), anyMap());
  }

  private RetryFixture retryFixture(Map<String, Object> data) {
    var logs = mock(IIoTDeviceDataService.class);
    var indexes = mock(VideoCloudEvidenceIndex.class);
    var evidence = mock(VideoCloudEvidenceService.class);
    var service = new VideoCloudEvidenceTimelineService(logs, mock(ISysOssService.class), indexes, evidence, List.of());
    var device = IoTDevice.builder().productKey("p").deviceId("cam").iotId("pcam").tenantId("t").build();
    var product = IoTProduct.builder().productKey("p").thirdPlatform("imoulife").build();
    var row = new IoTDeviceLogVO(); row.setId(12L); row.setProductKey("p"); row.setDeviceId("cam"); row.setMessageType("EVENT");
    row.setContent(JSONUtil.toJsonStr(data));
    when(logs.queryById(any(LogQuery.class))).thenReturn(row);
    when(logs.pageList(any(LogQuery.class))).thenReturn(new PageBean<>(List.of(row), 1L, 200, 1));
    return new RetryFixture(service, logs, indexes, evidence, device, product, row);
  }

  private record RetryFixture(VideoCloudEvidenceTimelineService service, IIoTDeviceDataService logs,
      VideoCloudEvidenceIndex indexes, VideoCloudEvidenceService evidence, IoTDevice device,
      IoTProduct product, IoTDeviceLogVO row) {}
}
