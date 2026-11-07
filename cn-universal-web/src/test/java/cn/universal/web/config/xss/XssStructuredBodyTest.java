package cn.universal.web.config.xss;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class XssStructuredBodyTest {
  private final ObjectMapper json = new ObjectMapper();

  JsonNode filter(String method, String path, Object body) throws Exception {
    var request = new MockHttpServletRequest(method, path);
    request.setServletPath(path);
    request.setContentType("application/json");
    request.setContent(json.writeValueAsBytes(body));
    var wrapper = new XssHttpServletRequestWrapper(request);
    return json.readTree(wrapper.getInputStream().readAllBytes());
  }

  @Test
  void picturePasswordIsProtectedOnlyOnItsDedicatedSaveAndVerifyEndpoints() throws Exception {
    String password = "a&b<&amp;<script>exact</script>\n字";
    var body = Map.of("pictureSecret", password, "name", "a&b<script>removed</script>");
    var actual = filter("PUT", "/api/video/platforms/p/evidence/picture-secret", body);
    assertEquals(password, actual.path("pictureSecret").asText());
    assertEquals("a&amp;bremoved", actual.path("name").asText());
    var verified = filter("POST", "/api/video/platforms/p/evidence/picture-secret/verify", body);
    assertEquals(password, verified.path("pictureSecret").asText());
    assertEquals("a&amp;bremoved", verified.path("name").asText());
    assertNotEquals(password, filter("POST", "/api/video/platforms/p/evidence/picture-secret", body)
        .path("pictureSecret").asText());
    assertNotEquals(password, filter("PUT", "/api/video/platforms/p/other", body)
        .path("pictureSecret").asText());
  }

}
