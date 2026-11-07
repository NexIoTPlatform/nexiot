package cn.universal.web.config.xss;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Keep camera passwords exact while filtering the surrounding JSON. */
final class XssJsonBodyFilter {
  private static final ObjectMapper JSON = new ObjectMapper();

  private XssJsonBodyFilter() {}

  /** Null means this request uses the ordinary body filter. */
  static String filter(HttpServletRequest request, String body, XssHtmlFilter html) {
    String path = request.getServletPath();
    boolean pictureSecret = ("PUT".equals(request.getMethod())
        && path.matches("/api/video/platforms/[^/]+/evidence/picture-secret"))
        || ("POST".equals(request.getMethod())
        && path.matches("/api/video/platforms/[^/]+/evidence/picture-secret/verify"));
    if (!pictureSecret) return null;
    try {
      JsonNode root = JSON.readTree(body);
      if (!(root instanceof ObjectNode object)) return null;
      Map<ObjectNode, Set<String>> exact = new IdentityHashMap<>();
      if (pictureSecret) protect(exact, object, "pictureSecret");
      return JSON.writeValueAsString(filterValue(root, exact, html));
    } catch (JsonProcessingException e) {
      // Malformed JSON retains the existing filter/parser error behavior.
      return null;
    }
  }

  private static void protect(Map<ObjectNode, Set<String>> exact, ObjectNode parent, String field) {
    if (parent.path(field).isTextual()) exact.computeIfAbsent(parent, ignored -> new HashSet<>()).add(field);
  }

  private static JsonNode filterValue(JsonNode value, Map<ObjectNode, Set<String>> exact,
      XssHtmlFilter html) {
    if (value instanceof ObjectNode object) {
      ObjectNode result = JSON.createObjectNode();
      object.properties().forEach(entry -> result.set(html.filter(entry.getKey()),
          exact.getOrDefault(object, Set.of()).contains(entry.getKey()) ? entry.getValue()
              : filterValue(entry.getValue(), exact, html)));
      return result;
    }
    if (value.isArray()) {
      var result = JSON.createArrayNode();
      value.forEach(item -> result.add(filterValue(item, exact, html)));
      return result;
    }
    return value.isTextual() ? JSON.getNodeFactory().textNode(html.filter(value.asText())) : value;
  }
}
