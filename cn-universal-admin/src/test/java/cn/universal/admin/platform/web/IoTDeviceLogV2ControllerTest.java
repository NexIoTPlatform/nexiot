package cn.universal.admin.platform.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.common.exception.IoTException;
import cn.universal.dm.device.service.log.IIoTDeviceDataService;
import cn.universal.persistence.entity.vo.IoTDeviceLogMetadataVO;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PageBean;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

class IoTDeviceLogV2ControllerTest {
  private final IIoTDeviceDataService data = mock(IIoTDeviceDataService.class);
  private final DeviceHistoryAccess access = mock(DeviceHistoryAccess.class);
  private final IoTDeviceLogV2Controller controller =
      new IoTDeviceLogV2Controller() {
        @Override
        protected boolean currentHistoryAdmin() {
          return false;
        }

        @Override
        protected String currentHistoryTenant() {
          return "tenant";
        }
      };

  IoTDeviceLogV2ControllerTest() {
    ReflectionTestUtils.setField(controller, "iIoTDeviceDataService", data);
    ReflectionTestUtils.setField(controller, "historyAccess", access);
    ReflectionTestUtils.setField(controller, "maxExportSize", 2);
  }

  private PageBean<IoTDeviceLogMetadataVO> page(boolean more, String... contents) {
    var list =
        java.util.Arrays.stream(contents)
            .map(
                text -> {
                  var row = new IoTDeviceLogMetadataVO();
                  row.setContent(text);
                  return row;
                })
            .toList();
    var result = new PageBean<>(list, -1L, 200, 1);
    result.setHasMore(more);
    result.setNextCursor(more ? "next" : null);
    return result;
  }

  @Test
  void rejectsOversizedExportBeforeWritingAFile() {
    when(data.queryLogMeta(any())).thenReturn(page(true, "1", "2"));
    var response = new MockHttpServletResponse();
    var error =
        assertThrows(
            IoTException.class, () -> controller.exportDeviceMetaLog(response, new LogQuery()));
    assertTrue(error.getMessage().contains("分段导出"));
    assertEquals(0, response.getContentAsByteArray().length);
    verify(data, times(1)).queryLogMeta(any());
  }

  @Test
  void exportsAllPagesRegardlessOfUiCursorAndPageSize() throws Exception {
    when(data.queryLogMeta(any()))
        .thenAnswer(
            invocation -> {
              LogQuery query = invocation.getArgument(0);
              assertTrue(query.isCursorMode());
              assertEquals(200, query.getPageSize());
              return query.getCursor() == null ? page(true, "0") : page(false, "88");
            });
    LogQuery query = LogQuery.builder().pageNum(52).pageSize(50).build();
    query.setCursor("unrelated-ui-cursor");
    var response = new MockHttpServletResponse();
    controller.exportDeviceMetaLog(response, query);
    try (var book =
        new XSSFWorkbook(new java.io.ByteArrayInputStream(response.getContentAsByteArray()))) {
      assertEquals(2, book.getSheetAt(0).getLastRowNum());
    }
    verify(data, times(2)).queryLogMeta(any());
  }

  @Test
  void refusesToReturnPartialFileOnLaterPageFailure() {
    when(data.queryLogMeta(any()))
        .thenReturn(page(true, "1"))
        .thenThrow(new IoTException("timeout"));
    var response = new MockHttpServletResponse();
    assertThrows(
        IoTException.class, () -> controller.exportDeviceMetaLog(response, new LogQuery()));
    assertEquals(0, response.getContentAsByteArray().length);
  }

  @Test
  void checksAccessBeforeCacheOrDatabaseAndBeforeExport() {
    doThrow(new IoTException("forbidden", 403))
        .when(access)
        .requireAccess(eq("tenant"), eq(false), any());
    assertThrows(IoTException.class, () -> controller.metadataSlice(new LogQuery()));
    assertThrows(
        IoTException.class,
        () -> controller.exportDeviceMetaLog(new MockHttpServletResponse(), new LogQuery()));
    verifyNoInteractions(data);
  }
}
