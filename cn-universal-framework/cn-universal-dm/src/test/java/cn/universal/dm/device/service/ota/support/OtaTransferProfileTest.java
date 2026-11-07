package cn.universal.dm.device.service.ota.support;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class OtaTransferProfileTest {
  @Test void missingConfigUsesLegacyUrlModeAndDefaults() {
    OtaTransferProfile profile = OtaTransferProfile.from(null);
    assertEquals("URL", profile.getMode());
    assertEquals(1024, profile.getChunkSize());
    assertEquals(10, profile.getAckTimeoutSeconds());
    assertFalse(profile.isChunked());
  }

  @Test void rejectsUnknownModeEncodingBoundsAndNonIntegerNumbers() {
    for (Map<String, Object> value : java.util.List.<Map<String, Object>>of(
        Map.of("mode", "anything"), Map.of("dataEncoding", "UTF8"),
        Map.of("chunkSize", 127), Map.of("chunkSize", 32769),
        Map.of("ackTimeoutSeconds", 1), Map.of("ackTimeoutSeconds", 301),
        Map.of("chunkMaxRetries", -1), Map.of("chunkMaxRetries", 11),
        Map.of("indexBase", 2), Map.of("chunkSize", 1024.5), Map.of("mode", true))) {
      assertThrows(IllegalArgumentException.class, () -> OtaTransferProfile.from(value), value.toString());
    }
  }

  @Test void snapshotRoundTripsWithoutSharingMutableProfile() {
    OtaTransferProfile profile = OtaTransferProfile.from(Map.of("mode", "PULL", "indexBase", 1, "dataEncoding", "HEX"));
    OtaTransferProfile snapshot = OtaTransferProfile.from(profile);
    profile.setChunkSize(2048);
    assertEquals(1024, snapshot.getChunkSize());
    assertTrue(snapshot.isChunked());
    assertEquals("PULL", snapshot.toMap().get("mode"));
  }

  @Test void optionalTemplateNameSurvivesPersistedJsonNull() {
    OtaTransferProfile profile = OtaTransferProfile.from(Map.of("mode", "PUSH"));
    String json = cn.hutool.json.JSONUtil.toJsonStr(profile.toMap());
    OtaTransferProfile snapshot = OtaTransferProfile.from(json);
    assertEquals("PUSH", snapshot.getMode());
    assertNull(snapshot.getTemplateName());
    assertNull(OtaTransferProfile.from("{\"templateName\":null}").getTemplateName());
  }
}
