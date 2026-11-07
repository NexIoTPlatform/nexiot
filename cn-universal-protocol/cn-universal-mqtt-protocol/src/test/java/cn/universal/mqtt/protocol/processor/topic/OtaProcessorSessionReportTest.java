package cn.universal.mqtt.protocol.processor.topic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.dm.device.service.ota.api.IoTOtaRuntimeService;
import cn.universal.dm.device.service.ota.api.OtaService.OtaServiceResult;
import cn.universal.dm.device.service.ota.support.OtaConstants;
import cn.universal.dm.device.service.ota.support.OtaReportExtractor;
import cn.universal.mqtt.protocol.entity.MQTTUPRequest;
import cn.universal.mqtt.protocol.processor.MqttMessageProcessor.ProcessorResult;
import cn.universal.mqtt.protocol.topic.MQTTTopicManager;
import cn.universal.persistence.base.BaseUPRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

/** Exercises the actual SYSTEM_LEVEL Topic entry, which does not run the product codec. */
class OtaProcessorSessionReportTest {

  private final IoTOtaRuntimeService runtime = mock(IoTOtaRuntimeService.class);
  private final OtaReportExtractor extractor = new OtaReportExtractor();
  private final OtaProcessor processor = new OtaProcessor();

  @BeforeEach
  void initialize() {
    ReflectionTestUtils.setField(processor, "topicManager", new MQTTTopicManager());
    ReflectionTestUtils.setField(processor, "otaRuntimeService", runtime);
    ReflectionTestUtils.setField(processor, "objectMapper", new ObjectMapper());
    when(runtime.handleDecodedReport(any(), eq("product"), eq("device"), eq("mqtt")))
        .thenAnswer(invocation -> extractor.supports(invocation.getArgument(0)));
  }

  @Test
  void standardTopicPreservesChunkAckAndUsesTopicIdentity() {
    MQTTUPRequest request = process("""
        {"reportType":"TRANSFER_ACK","transferAction":"chunk","sessionId":"session-1",
         "taskId":"123","taskDeviceId":456,"chunkIndex":3,"success":false,
         "productKey":"spoofed-product","deviceId":"spoofed-device","errorMessage":"checksum"}
        """);

    Map<String, Object> report = decodedReport();
    assertEquals(OtaConstants.ReportType.TRANSFER_ACK, report.get("reportType"));
    assertEquals("chunk", report.get("transferAction"));
    assertEquals("session-1", report.get("sessionId"));
    assertEquals("123", report.get("taskId"));
    assertEquals(456, report.get("taskDeviceId"));
    assertEquals(3, report.get("chunkIndex"));
    assertEquals(false, report.get("success"));
    assertEquals("product", report.get("productKey"));
    assertEquals("device", report.get("deviceId"));
    assertEquals("checksum", report.get("errorMessage"));
    assertEquals(Boolean.TRUE, request.getContextValue("otaHandled"));
    verify(runtime, never()).handleFirmwareReport(anyMap());
  }

  @Test
  void standardTopicAcceptsNestedPullRequest() {
    process("""
        {"data":{"reportType":"transfer_request","transferAction":"chunk",
          "sessionId":"session-2","taskDeviceId":"456","chunkIndex":1}}
        """);
    Map<String, Object> report = decodedReport();
    assertEquals(OtaConstants.ReportType.TRANSFER_REQUEST, report.get("reportType"));
    assertEquals("session-2", report.get("sessionId"));
    assertEquals(1, report.get("chunkIndex"));
  }

  @Test
  void installationResultRetainsSessionUntilRuntimeValidation() {
    process("""
        {"reportType":"UPGRADE_RESULT","sessionId":"session-3","taskDeviceId":"456",
         "status":"success","version":"2.0"}
        """);
    Map<String, Object> report = decodedReport();
    assertEquals(OtaConstants.ReportType.UPGRADE_RESULT, report.get("reportType"));
    assertEquals("session-3", report.get("sessionId"));
    assertEquals("success", report.get("upgradeStatus"));
    assertEquals("2.0", report.get("currentVersion"));
    verify(runtime, never()).handleUpgradeResult(anyMap());
  }

  @Test
  void malformedTransferReportCannotFallBackToVersionReporting() {
    MQTTUPRequest request = process("""
        {"reportType":"TRANSFER_ACK","transferAction":"ready","version":"2.0"}
        """);
    assertEquals(Boolean.FALSE, request.getContextValue("otaHandled"));
    assertNotNull(request.getContextValue("validationError"));
    verify(runtime, never()).handleFirmwareReport(anyMap());
  }

  @Test
  void legacyWholePackageVersionReportKeepsExistingEntry() {
    when(runtime.handleFirmwareReport(anyMap())).thenReturn(OtaServiceResult.SUCCESS);
    process("""
        {"version":"1.0","module":"APPLICATION"}
        """);
    ArgumentCaptor<Map<String, Object>> capture = ArgumentCaptor.forClass(Map.class);
    verify(runtime).handleFirmwareReport(capture.capture());
    assertEquals("1.0", capture.getValue().get("currentVersion"));
    assertEquals("product", capture.getValue().get("productKey"));
    verify(runtime, never()).handleDecodedReport(any(), anyString(), anyString(), anyString());
  }

  @Test
  void legacyWholePackageResultKeepsExistingEntry() {
    when(runtime.handleUpgradeResult(anyMap())).thenReturn(OtaServiceResult.SUCCESS);
    process("""
        {"reportType":"UPGRADE_RESULT","status":"success","version":"2.0"}
        """);
    verify(runtime).handleUpgradeResult(argThat(report ->
        "success".equals(report.get("upgradeStatus")) && "2.0".equals(report.get("currentVersion"))));
    verify(runtime, never()).handleDecodedReport(any(), anyString(), anyString(), anyString());
  }

  private MQTTUPRequest process(String payload) {
    MQTTUPRequest request = new MQTTUPRequest();
    request.setUpTopic("$ota/report/product/device");
    request.setPayload(payload);
    assertTrue(processor.supports(request));
    assertNull(request.getUpRequestList());
    assertEquals(ProcessorResult.CONTINUE, processor.process(request));
    return request;
  }

  private Map<String, Object> decodedReport() {
    ArgumentCaptor<BaseUPRequest> capture = ArgumentCaptor.forClass(BaseUPRequest.class);
    verify(runtime).handleDecodedReport(capture.capture(), eq("product"), eq("device"), eq("mqtt"));
    Map<String, Object> report = extractor.extract(capture.getValue(), "product", "device", "mqtt");
    assertNotNull(report);
    return report;
  }
}
