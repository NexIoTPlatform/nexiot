package cn.universal.admin.video.service;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

@Data
public class VideoCatalogChannel {
  private String channelId;
  private String channelName;
  private String channelStatus = "UNKNOWN";
  private String streamType = "main";
  private Integer channelNo;
  private Map<String, String> capabilities = new LinkedHashMap<>();
}
