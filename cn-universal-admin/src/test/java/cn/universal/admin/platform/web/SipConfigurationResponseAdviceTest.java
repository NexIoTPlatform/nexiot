package cn.universal.admin.platform.web;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.persistence.entity.IoTProduct;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

class SipConfigurationResponseAdviceTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final SipConfigurationResponseAdvice advice = new SipConfigurationResponseAdvice(mapper);

  @Test
  void redactsNestedListAndDetailWithoutChangingStoredObjects() throws Exception {
    String config = "{\"sipHost\":\"127.0.0.1\",\"sipEslPassword\":\"control-secret\"}";
    IoTProduct product = IoTProduct.builder().configuration(config).build();
    Object body =
        Map.of(
            "code",
            200,
            "rows",
            List.of(product),
            "data",
            Map.of(
                "configuration",
                "{\"sipUsername\":\"1001\",\"sipPassword\":\"device-secret\"}",
                "productConfiguration",
                config));
    JsonNode result =
        (JsonNode)
            advice.beforeBodyWrite(
                body,
                null,
                MediaType.APPLICATION_JSON,
                MappingJackson2HttpMessageConverter.class,
                null,
                null);
    assertEquals(200, result.get("code").asInt());
    assertFalse(result.toString().contains("control-secret"));
    assertFalse(result.toString().contains("device-secret"));
    assertEquals(
        "1001",
        mapper.readTree(result.at("/data/configuration").asText()).get("sipUsername").asText());
    assertEquals(config, product.getConfiguration());
  }

  @Test
  void leavesOtherProtocolsAndUnconfiguredCredentialsUnchanged() {
    Map<String, Object> body =
        Map.of("configuration", "{\"port\":6001}", "sipPassword", "", "state", true);
    Object result =
        advice.beforeBodyWrite(
            body,
            null,
            MediaType.APPLICATION_JSON,
            MappingJackson2HttpMessageConverter.class,
            null,
            null);
    assertSame(body, result);
  }

  @Test
  void neverLeaksMalformedCredentialConfiguration() {
    JsonNode result =
        (JsonNode)
            advice.beforeBodyWrite(
                Map.of("configuration", "{\"sipPassword\":\"secret\""),
                null,
                MediaType.APPLICATION_JSON,
                MappingJackson2HttpMessageConverter.class,
                null,
                null);
    assertEquals("{}", result.get("configuration").asText());
  }
}
