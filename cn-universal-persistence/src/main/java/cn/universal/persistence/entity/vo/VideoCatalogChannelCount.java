package cn.universal.persistence.entity.vo;

import lombok.Data;

/** Aggregated channel totals; no channel/configuration payload is read for a device page. */
@Data
public class VideoCatalogChannelCount {
  private String instanceKey;
  private String deviceId;
  private String tenantId;
  private long channelCount;
}
