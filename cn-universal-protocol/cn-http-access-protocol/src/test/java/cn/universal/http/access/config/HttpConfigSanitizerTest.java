package cn.universal.http.access.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class HttpConfigSanitizerTest {
  private final HttpConfigParser parser = new HttpConfigParser();
  private final HttpConfigSanitizer sanitizer = new HttpConfigSanitizer(parser);

  @Test
  void masksInboundCredentialsAndPreservesNetworkPolicy() {
    String source = """
        {"accessCode":"factory-east","inbound":{"auth":{"type":"API_KEY",
        "headerName":"X-API-Key","value":"key-secret","secret":"hmac-secret"}},
        "outbound":{"allowPrivateNetwork":false,"requestTimeoutMs":10000}}
        """;
    JsonNode sanitized = parser.readTree(sanitizer.sanitize(source));
    assertEquals(HttpConfigSanitizer.MASK, sanitized.at("/inbound/auth/value").asText());
    assertEquals(HttpConfigSanitizer.MASK, sanitized.at("/inbound/auth/secret").asText());
    assertEquals("X-API-Key", sanitized.at("/inbound/auth/headerName").asText());
    assertEquals(10000, sanitized.at("/outbound/requestTimeoutMs").asInt());
    assertFalse(sanitized.toString().contains("key-secret"));
    assertFalse(sanitized.toString().contains("hmac-secret"));
  }

  @Test
  void maskedEditsRetainCredentialsWhileApplyingPolicyChanges() {
    String existing = """
        {"inbound":{"auth":{"type":"API_KEY","value":"key-secret","secret":"hmac-secret"}},
        "outbound":{"requestTimeoutMs":10000}}
        """;
    String incoming = """
        {"inbound":{"auth":{"type":"API_KEY","value":"******","secret":"******"}},
        "outbound":{"requestTimeoutMs":20000}}
        """;
    JsonNode merged = parser.readTree(sanitizer.mergeMasked(existing, incoming));
    assertEquals("key-secret", merged.at("/inbound/auth/value").asText());
    assertEquals("hmac-secret", merged.at("/inbound/auth/secret").asText());
    assertEquals(20000, merged.at("/outbound/requestTimeoutMs").asInt());
  }

  @Test
  void explicitCredentialRotationReplacesOldSecret() {
    JsonNode merged = parser.readTree(sanitizer.mergeMasked(
        "{\"inbound\":{\"auth\":{\"value\":\"old-key\"}}}",
        "{\"inbound\":{\"auth\":{\"value\":\"new-key\"}}}"));
    assertEquals("new-key", merged.at("/inbound/auth/value").asText());
  }
}
