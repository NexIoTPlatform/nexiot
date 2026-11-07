package cn.universal.core.protocol.magic;

import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

class MagicScriptMethodExtractorTest {

  @Test
  void shouldDetectDecodeMethodFromVisualScript() {
    String script =
        """
        var __pickSegment = (payload, corePayload, dataArea, source) => {
          if (source === "payload") {
            return payload;
          }
          if (source === "dataArea") {
            return dataArea;
          }
          return corePayload;
        };

        var decode = (payload, context) => {
          var rawPayload = payload == null ? "" : payload + "";
          var normalizedPayload = rawPayload.replaceAll("\\\\s+", "");
          var corePayload = normalizedPayload.substring(0);
          var dataArea = corePayload;
          var propertiesObj = {
            "messageType": "PROPERTIES",
            "properties": {
              "sourcePayload": rawPayload
            }
          };
          return propertiesObj;
        };
        """;

    Set<String> result =
        MagicScriptMethodExtractor.extractSupportMethods(
            script, Set.of("decode", "encode", "preDecode"));

    Assert.assertEquals(Set.of("decode"), result);
  }

  @Test
  void shouldThrowClearExceptionWhenMagicScriptIsInvalid() {
    String invalidScript = "var decode = (payload, context) => { return payload; ";

    try {
      MagicScriptMethodExtractor.extractSupportMethods(invalidScript, Set.of("decode", "encode"));
      Assert.fail("Expected IllegalStateException to be thrown");
    } catch (IllegalStateException error) {
      Assert.assertTrue(error.getMessage().contains("MagicScript预编译失败"));
    }
  }
}
