package cn.universal.http.access.model;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

@Data
public class HttpRequestSpec {
  private String method = "GET";
  private String url;
  private Map<String, String> headers = new LinkedHashMap<>();
  private Map<String, Object> query = new LinkedHashMap<>();
  private String body;
  private Integer timeoutMs;
}
