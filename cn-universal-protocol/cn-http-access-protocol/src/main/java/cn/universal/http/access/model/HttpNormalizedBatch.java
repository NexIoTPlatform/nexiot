package cn.universal.http.access.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class HttpNormalizedBatch {
  private List<HttpNormalizedRecord> records = new ArrayList<>();
  private String nextCursor;
  private String externalRequestId;
  private Boolean success;
  private String message;
  private Map<String, Object> state = new LinkedHashMap<>();

  @Data
  public static class HttpNormalizedRecord {
    private String productKey;
    private String deviceId;
    private String payload;
    private String state;
    private String commandId;
    private String externalRequestId;
    private Map<String, Object> data = new LinkedHashMap<>();
  }
}
