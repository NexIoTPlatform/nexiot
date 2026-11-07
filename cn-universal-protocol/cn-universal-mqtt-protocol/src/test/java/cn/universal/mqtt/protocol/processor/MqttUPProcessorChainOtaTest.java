package cn.universal.mqtt.protocol.processor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.dm.device.service.ota.api.IoTOtaRuntimeService;
import cn.universal.dm.device.service.ota.api.OtaService.OtaServiceResult;
import cn.universal.dm.device.service.ota.support.OtaConstants;
import cn.universal.dm.device.service.ota.support.OtaReportExtractor;
import cn.universal.dm.device.service.plugin.ProcessorExecutor;
import cn.universal.mqtt.protocol.entity.MQTTUPRequest;
import cn.universal.mqtt.protocol.processor.MqttMessageProcessor.ProcessorResult;
import cn.universal.mqtt.protocol.processor.topic.OtaProcessor;
import cn.universal.mqtt.protocol.processor.up.AutoInsertUPProcessorUP_TWO;
import cn.universal.mqtt.protocol.processor.up.passthrough.PassthroughDeviceInfoProcessor;
import cn.universal.mqtt.protocol.processor.up.passthrough.ThirdMQTTDeviceInfoProcessor;
import cn.universal.mqtt.protocol.processor.up.thingmodel.ThingModelDeviceInfoProcessor;
import cn.universal.mqtt.protocol.topic.MQTTTopicManager;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.query.IoTDeviceQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

/** Exercises the production chain and ordering, not only the OTA processor entry point. */
class MqttUPProcessorChainOtaTest {

  private final IoTOtaRuntimeService runtime = mock(IoTOtaRuntimeService.class);
  private final IoTProductDeviceService products = mock(IoTProductDeviceService.class);
  private final OtaReportExtractor extractor = new OtaReportExtractor();
  private final AtomicReference<Map<String, Object>> decoded = new AtomicReference<>();
  private final ExistingDeviceAutoInsert autoInsert = new ExistingDeviceAutoInsert();
  private final OtaProcessor ota = new OtaProcessor();
  private final ThingModelDeviceInfoProcessor thing = new ThingModelDeviceInfoProcessor();
  private final PassthroughDeviceInfoProcessor passthrough = new PassthroughDeviceInfoProcessor();
  private final ThirdMQTTDeviceInfoProcessor third = new ThirdMQTTDeviceInfoProcessor();
  private final TailProcessor tail = new TailProcessor();
  private final MqttUPProcessorChain chain = new MqttUPProcessorChain();
  private final IoTProduct product = IoTProduct.builder()
      .productKey("product").configuration("{\"allowInsert\":true}").build();

  @BeforeEach
  void initialize() {
    ReflectionTestUtils.setField(autoInsert, "iotProductDeviceService", products);
    when(products.getProduct("product")).thenReturn(product);
    ReflectionTestUtils.setField(ota, "topicManager", new MQTTTopicManager());
    ReflectionTestUtils.setField(ota, "otaRuntimeService", runtime);
    ReflectionTestUtils.setField(ota, "objectMapper", new ObjectMapper());
    when(runtime.handleFirmwareReport(anyMap())).thenReturn(OtaServiceResult.SUCCESS);
    when(runtime.handleUpgradeProgress(anyMap())).thenReturn(OtaServiceResult.SUCCESS);
    when(runtime.handleUpgradeResult(anyMap())).thenReturn(OtaServiceResult.SUCCESS);
    when(runtime.handleDecodedReport(any(), eq("product"), eq("device"), eq("mqtt")))
        .thenAnswer(invocation -> {
          BaseUPRequest report = invocation.getArgument(0);
          decoded.set(extractor.extract(report, "product", "device", "mqtt"));
          return decoded.get() != null;
        });
    ReflectionTestUtils.setField(chain, "processorExecutor", new ProcessorExecutor());
    // Intentionally unsorted. The real executor must put auto-register before OTA, as in production.
    ReflectionTestUtils.setField(chain, "processors",
        List.of(tail, ota, autoInsert, thing, passthrough, third));
  }

  @Test
  void nativeFirmwareInfoReachesRuntimeWithoutDeviceInfoOrAutoRegistration() {
    MQTTUPRequest request = nativeReport("{\"reportType\":\"FIRMWARE_INFO\",\"version\":\"1.0\"}");
    assertProcessed(request);
    verify(runtime).handleFirmwareReport(argThat(report ->
        "1.0".equals(report.get("currentVersion")) && identityMatches(report)));
    verifyNoInteractions(products);
    assertEquals(0, autoInsert.lifecycleCalls);
  }

  @Test
  void nativeUrlProgressAndResultReachRuntimeWithAutoRegistrationEnabled() {
    assertProcessed(nativeReport("""
        {"reportType":"UPGRADE_PROGRESS","taskDeviceId":33,"status":"INSTALLING","progress":95}
        """));
    verify(runtime).handleUpgradeProgress(argThat(report ->
        Integer.valueOf(95).equals(report.get("progress"))
            && "INSTALLING".equals(report.get("upgradeStatus")) && identityMatches(report)));

    assertProcessed(nativeReport("""
        {"reportType":"UPGRADE_RESULT","taskDeviceId":33,"status":"success","version":"2.0"}
        """));
    verify(runtime).handleUpgradeResult(argThat(report ->
        "success".equals(report.get("upgradeStatus"))
            && "2.0".equals(report.get("currentVersion")) && identityMatches(report)));
    verifyNoInteractions(products);
  }

  @ParameterizedTest
  @CsvSource({"ready,-1", "chunk,2", "finish,-1"})
  void nativeChunkAcknowledgementsReachUnifiedRuntime(String action, int index) {
    String payload = "{\"reportType\":\"TRANSFER_ACK\",\"transferAction\":\"" + action
        + "\",\"sessionId\":\"session-1\",\"taskDeviceId\":33,\"success\":true,\"chunkIndex\":" + index + "}";
    MQTTUPRequest request = nativeReport(payload);
    assertProcessed(request);
    assertEquals(OtaConstants.ReportType.TRANSFER_ACK, decoded.get().get("reportType"));
    assertEquals(action, decoded.get().get("transferAction"));
    assertEquals(index, decoded.get().get("chunkIndex"));
    assertEquals("session-1", decoded.get().get("sessionId"));
    assertTrue(identityMatches(decoded.get()));
    assertEquals(Boolean.TRUE, request.getContextValue("otaHandled"));
    verifyNoInteractions(products);
  }

  @Test
  void nativePullRequestRetainsSessionAndRequestedIndex() {
    assertProcessed(nativeReport("""
        {"data":{"reportType":"transfer_request","transferAction":"chunk",
          "sessionId":"session-pull","taskDeviceId":33,"chunkIndex":1}}
        """));
    assertEquals(OtaConstants.ReportType.TRANSFER_REQUEST, decoded.get().get("reportType"));
    assertEquals("session-pull", decoded.get().get("sessionId"));
    assertEquals(1, decoded.get().get("chunkIndex"));
    assertTrue(identityMatches(decoded.get()));
    verifyNoInteractions(products);
  }

  @Test
  void malformedNativeTransferCannotFallBackToFirmwareInfo() {
    MQTTUPRequest request = nativeReport("""
        {"reportType":"TRANSFER_ACK","transferAction":"ready","version":"2.0"}
        """);
    assertProcessed(request);
    assertEquals(Boolean.FALSE, request.getContextValue("otaHandled"));
    assertNotNull(request.getContextValue("validationError"));
    verify(runtime, never()).handleFirmwareReport(anyMap());
  }

  @ParameterizedTest
  @CsvSource({"true,$thing/up/property/product/device",
      "true,$thing/up/product/device", "false,vendor/device/telemetry",
      "false,$ota/report/product/device"})
  void ordinaryAndThirdPartyRegistrationKeepsExistingBehavior(boolean system, String topic) {
    MQTTUPRequest request = request(system, topic, "{}");
    request.setIoTProduct(product);
    request.setIoTDeviceDTO(autoInsert.device);
    assertTrue(autoInsert.supports(request));
    assertTrue(autoInsert.preCheck(request));
    assertEquals(ProcessorResult.CONTINUE, autoInsert.process(request));
    assertSame(autoInsert.device, request.getIoTDeviceDTO());
    assertEquals(1, autoInsert.lifecycleCalls);
  }

  private MQTTUPRequest nativeReport(String payload) {
    MQTTUPRequest request = request(true, "$ota/report/product/device", payload);
    assertNull(request.getIoTProduct());
    assertNull(request.getIoTDeviceDTO());
    assertFalse(thing.supports(request));
    assertFalse(passthrough.supports(request));
    assertFalse(third.supports(request));
    return request;
  }

  private MQTTUPRequest request(boolean system, String topic, String payload) {
    return MQTTUPRequest.builder().isSysMQTTBroker(system).productKey("product")
        .deviceId("device").upTopic(topic).payload(payload).build();
  }

  private void assertProcessed(MQTTUPRequest request) {
    assertTrue(chain.process(request));
    assertEquals(Boolean.TRUE, request.getContextValue("tailReached"));
  }

  private boolean identityMatches(Map<String, Object> report) {
    return "product".equals(report.get("productKey")) && "device".equals(report.get("deviceId"));
  }

  /** No database lifecycle side effects; supports and process are the actual production methods. */
  private static class ExistingDeviceAutoInsert extends AutoInsertUPProcessorUP_TWO {
    final IoTDeviceDTO device = new IoTDeviceDTO();
    int lifecycleCalls;

    @Override
    protected IoTDeviceDTO lifeCycleDevInstance(IoTDeviceQuery query) {
      lifecycleCalls++;
      return device;
    }
  }

  private static class TailProcessor implements MqttMessageProcessor {
    public String getName() { return "tail"; }
    public int getOrder() { return 9999; }
    public boolean supports(MQTTUPRequest request) { return true; }
    public ProcessorResult process(MQTTUPRequest request) {
      request.setContextValue("tailReached", true);
      return ProcessorResult.CONTINUE;
    }
  }
}
