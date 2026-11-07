package cn.universal.web.service.openapi.v2;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cn.universal.common.exception.IoTException;
import cn.universal.dm.device.service.log.HistoryPageSupport;
import cn.universal.dm.device.service.log.IIoTDeviceDataService;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.entity.vo.IoTDeviceLogMetadataVO;
import cn.universal.persistence.entity.vo.IoTDeviceLogVO;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PageBean;
import cn.universal.web.dto.openapi.v2.OpenApiV2LogModels.CursorQuery;
import cn.universal.web.dto.openapi.v2.OpenApiV2LogModels.LogCursorQuery;
import cn.universal.web.dto.openapi.v2.OpenApiV2LogModels.LogPageQuery;
import cn.universal.web.dto.openapi.v2.OpenApiV2LogModels.PageQuery;
import cn.universal.web.dto.openapi.v2.OpenApiV2LogModels.TimeRange;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OpenApiV2LogServiceTest {
  private final OpenApiV2Access access = mock(OpenApiV2Access.class);
  private final IIoTDeviceDataService history = mock(IIoTDeviceDataService.class);
  private final OpenApiV2LogService service = new OpenApiV2LogService(access, history);
  private final IoTUserApplication app = IoTUserApplication.builder()
      .appId("client-one").appUniqueId("app-one").tenantId("tenant-one")
      .appSecret("test-only-application-secret-at-least-32-bytes").build();
  private final LocalDateTime time = LocalDateTime.of(2026, 10, 17, 12, 0, 0, 123000000);

  @BeforeEach
  void setUp() {
    when(access.application()).thenReturn(app);
    when(access.tenantId(any())).thenAnswer(invocation ->
        ((IoTUserApplication) invocation.getArgument(0)).getTenantId());
    when(access.device("p", "d")).thenReturn(IoTDeviceDTO.builder()
        .productKey("trusted-p").deviceId("trusted-d").iotId("trusted-iot").tenantId("tenant-one").build());
    when(history.pageList(any())).thenAnswer(invocation -> {
      LogQuery query = invocation.getArgument(0);
      HistoryPageSupport.prepare(query, false);
      if (query.isCursorMode()) {
        List<IoTDeviceLogVO> rows = query.getBeforeId() == null
            ? List.of(log(9007199254740994L), log(9007199254740993L), log(9007199254740992L))
            : List.of(log(9007199254740992L));
        return HistoryPageSupport.page(rows, query, IoTDeviceLogVO::getCreateTime, IoTDeviceLogVO::getId);
      }
      return new PageBean<>(List.of(log(9007199254740994L)), 3L, query.getPageSize(), query.getPageNum());
    });
    when(history.queryLogMeta(any())).thenAnswer(invocation -> {
      LogQuery query = invocation.getArgument(0);
      HistoryPageSupport.prepare(query, true);
      IoTDeviceLogMetadataVO row = new IoTDeviceLogMetadataVO();
      row.setId(1L);
      row.setProperty(query.getProperty());
      row.setContent("23.5");
      row.setExt1("温度");
      row.setExt3("℃");
      row.setCreateTime(time);
      if (query.isCursorMode()) {
        return HistoryPageSupport.page(
            List.of(row, row), query, IoTDeviceLogMetadataVO::getCreateTime, IoTDeviceLogMetadataVO::getId);
      }
      return new PageBean<>(List.of(row), 1L, query.getPageSize(), query.getPageNum());
    });
  }

  @Test
  void ordinaryPagesHaveExactTotalsAndUseOnlyAuthorizedDeviceIdentity() {
    LogPageQuery query = range(new LogPageQuery());
    query.setPageSize(2);
    var page = service.logs("p", "d", query);
    assertEquals(3L, page.getTotal());
    assertEquals(1, page.getPage());
    assertTrue(page.isHasMore());
    assertNull(page.getNextCursor());
    assertEquals("9007199254740994", page.getItems().getFirst().getId());
    ArgumentCaptor<LogQuery> captured = ArgumentCaptor.forClass(LogQuery.class);
    verify(history).pageList(captured.capture());
    assertEquals("trusted-p", captured.getValue().getProductKey());
    assertEquals("trusted-d", captured.getValue().getDeviceId());
    assertEquals("trusted-iot", captured.getValue().getIotId());
    verify(access).device("p", "d");
  }

  @Test
  void cursorPreservesTimestampTiesAndTerminatesWithoutFakeTotals() {
    LogCursorQuery query = cursor();
    var first = service.logCursor("p", "d", query);
    assertTrue(first.isHasMore());
    assertNull(first.getTotal());
    assertNull(first.getPage());
    assertTrue(first.getNextCursor().startsWith("2."));
    query.setCursor(first.getNextCursor());
    var last = service.logCursor("p", "d", query);
    assertFalse(last.isHasMore());
    assertNull(last.getNextCursor());
    assertEquals("9007199254740992", last.getItems().getFirst().getId());
    ArgumentCaptor<LogQuery> captured = ArgumentCaptor.forClass(LogQuery.class);
    verify(history, times(2)).pageList(captured.capture());
    assertEquals(time, captured.getValue().getBeforeTime());
    assertEquals(9007199254740993L, captured.getValue().getBeforeId());
    verify(access, times(2)).device("p", "d");
  }

  @Test
  void rejectsTamperingAndCrossApplicationTenantOrKeyRotationBeforeReadingHistory() {
    String token = service.logCursor("p", "d", cursor()).getNextCursor();
    clearInvocations(history);
    LogCursorQuery query = cursor();
    query.setCursor(token.substring(0, token.length() - 4) + "AAAA");
    assertThrows(IoTException.class, () -> service.logCursor("p", "d", query));
    query.setCursor(token);
    app.setAppUniqueId("other-app");
    assertThrows(IoTException.class, () -> service.logCursor("p", "d", query));
    app.setAppUniqueId("app-one");
    app.setTenantId("other-tenant");
    assertThrows(IoTException.class, () -> service.logCursor("p", "d", query));
    app.setTenantId("tenant-one");
    app.setAppSecret("new-test-only-secret-at-least-32-bytes");
    assertThrows(IoTException.class, () -> service.logCursor("p", "d", query));
    verifyNoInteractions(history);
  }

  @Test
  void rejectsChangedWindowLimitFilterAndCrossEndpointCursorBeforeReadingHistory() {
    String token = service.logCursor("p", "d", cursor()).getNextCursor();
    clearInvocations(history);
    for (String change : List.of("window", "limit", "message")) {
      LogCursorQuery query = cursor();
      query.setCursor(token);
      if (change.equals("window")) query.setEndTime(query.getEndTime() + 1);
      if (change.equals("limit")) query.setLimit(3);
      if (change.equals("message")) query.setMessageType("EVENT");
      assertThrows(IoTException.class, () -> service.logCursor("p", "d", query));
    }
    CursorQuery propertyQuery = range(new CursorQuery());
    propertyQuery.setLimit(2);
    propertyQuery.setCursor(token);
    assertThrows(IoTException.class, () -> service.propertyCursor("p", "d", "temperature", propertyQuery));
    verifyNoInteractions(history);
  }

  @Test
  void propertyResultsExposeReadableFieldsAndKeepValuesAsText() {
    var page = service.properties("p", "d", "temperature", range(new PageQuery()));
    var row = page.getItems().getFirst();
    assertEquals("temperature", row.getProperty());
    assertEquals("23.5", row.getValue());
    assertEquals("温度", row.getName());
    assertEquals("℃", row.getUnit());
    assertEquals(1L, page.getTotal());
    assertFalse(page.isHasMore());
  }

  @Test
  void propertyCursorIsBoundToTheSingleProperty() {
    CursorQuery query = range(new CursorQuery());
    query.setLimit(1);
    var first = service.propertyCursor("p", "d", "temperature", query);
    assertNull(first.getTotal());
    assertTrue(first.isHasMore());
    assertNotNull(first.getNextCursor());
    query.setCursor(first.getNextCursor());
    clearInvocations(history);
    assertThrows(IoTException.class, () -> service.propertyCursor("p", "d", "humidity", query));
    verifyNoInteractions(history);
    assertEquals("temperature", service.propertyCursor("p", "d", "temperature", query)
        .getItems().getFirst().getProperty());
  }

  @Test
  void rejectsBadRangesDeepPagesMalformedTokensAndUnknownMessageTypes() {
    LogPageQuery query = range(new LogPageQuery());
    query.setEndTime(query.getStartTime() + 31 * 86400L + 1);
    assertThrows(IoTException.class, () -> service.logs("p", "d", query));
    range(query);
    query.setPage(502);
    assertThrows(IoTException.class, () -> service.logs("p", "d", query));
    query.setPage(1);
    query.setPageSize(201);
    assertThrows(IoTException.class, () -> service.logs("p", "d", query));
    query.setPageSize(20);
    query.setMessageType("something-else");
    assertThrows(IoTException.class, () -> service.logs("p", "d", query));
    for (String token : List.of("", " ", "not-a-token", "2.!!!.!!!")) {
      LogCursorQuery cursor = cursor();
      cursor.setCursor(token);
      assertThrows(IoTException.class, () -> service.logCursor("p", "d", cursor));
    }
    verifyNoInteractions(history);
  }

  @Test
  void revokedDeviceAccessPreventsEveryReadIncludingCursorContinuation() {
    String token = service.logCursor("p", "d", cursor()).getNextCursor();
    clearInvocations(history);
    when(access.device("p", "d")).thenThrow(new IoTException("denied", 403));
    LogCursorQuery query = cursor();
    query.setCursor(token);
    assertThrows(IoTException.class, () -> service.logCursor("p", "d", query));
    assertThrows(IoTException.class, () -> service.logs("p", "d", range(new LogPageQuery())));
    assertThrows(IoTException.class, () -> service.properties("p", "d", "temperature", range(new PageQuery())));
    assertThrows(IoTException.class, () -> service.propertyCursor("p", "d", "temperature", range(new CursorQuery())));
    verifyNoInteractions(history);
  }

  private LogCursorQuery cursor() {
    LogCursorQuery query = range(new LogCursorQuery());
    query.setLimit(2);
    return query;
  }

  private <T extends TimeRange> T range(T query) {
    query.setStartTime(1792108800L);
    query.setEndTime(1792195200L);
    return query;
  }

  private IoTDeviceLogVO log(long id) {
    IoTDeviceLogVO row = new IoTDeviceLogVO();
    row.setId(id);
    row.setIotId("trusted-iot");
    row.setCreateTime(time);
    return row;
  }
}
