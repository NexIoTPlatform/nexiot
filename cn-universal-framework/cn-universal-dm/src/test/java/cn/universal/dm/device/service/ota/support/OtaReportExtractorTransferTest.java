package cn.universal.dm.device.service.ota.support;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.persistence.base.BaseUPRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OtaReportExtractorTransferTest {
  @Test void transferAckPreservesSessionSequenceAndFalseSuccess() {
    BaseUPRequest request = new BaseUPRequest();
    request.setMessageType(MessageType.EVENT);
    request.setData(Map.of("reportType", "TRANSFER_ACK", "sessionId", "session",
        "transferAction", "chunk", "chunkIndex", 0, "success", false));
    OtaReportExtractor extractor = new OtaReportExtractor();
    assertTrue(extractor.supports(request));
    Map<String, Object> report = extractor.extract(request, "product", "device", "tcp");
    assertEquals("TRANSFER_ACK", report.get("reportType"));
    assertEquals("session", report.get("sessionId"));
    assertEquals(0, report.get("chunkIndex"));
    assertEquals(false, report.get("success"));
  }

  @Test void transferRequestIsNotMisclassifiedAsVersionReport() {
    BaseUPRequest request = new BaseUPRequest();
    request.setMessageType(MessageType.EVENT);
    request.setData(Map.of("reportType", "TRANSFER_REQUEST", "sessionId", "session",
        "transferAction", "chunk", "chunkIndex", 1));
    Map<String, Object> report = new OtaReportExtractor().extract(request, "product", "device", "mqtt");
    assertEquals("TRANSFER_REQUEST", report.get("reportType"));
  }
}
