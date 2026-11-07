package cn.universal.http.access.web;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public record HttpIngressRequest(
    String method,
    String rawUri,
    String routePath,
    String remoteAddress,
    Map<String, List<String>> headers,
    Map<String, List<String>> query,
    byte[] body) {

  public String bodyText() {
    return body == null ? "" : new String(body, StandardCharsets.UTF_8);
  }

  public String firstHeader(String name) {
    if (name == null || headers == null) {
      return null;
    }
    return headers.entrySet().stream()
        .filter(entry -> entry.getKey().equalsIgnoreCase(name))
        .map(Map.Entry::getValue)
        .filter(values -> values != null && !values.isEmpty())
        .map(values -> values.getFirst())
        .findFirst()
        .orElse(null);
  }

  public String firstQuery(String name) {
    List<String> values = query == null ? null : query.get(name);
    return values == null || values.isEmpty() ? null : values.getFirst();
  }
}
