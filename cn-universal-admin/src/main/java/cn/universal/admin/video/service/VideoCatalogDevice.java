package cn.universal.admin.video.service;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

@Data
public class VideoCatalogDevice {
  private String id;
  private String iotId;
  private String deviceId;
  private String deviceName;
  private String productKey;
  private String instanceKey;
  private String platformType;
  private Integer state;
  private int channelCount;
  private boolean channelsTruncated;
  private List<VideoCatalogChannel> channels = new ArrayList<>();
  private Map<String, Object> capabilities = new LinkedHashMap<>();
  private String sdkVendor;
  private boolean sdkRequired;
}
