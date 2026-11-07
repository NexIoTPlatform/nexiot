package cn.universal.persistence.entity.vo;

import lombok.Data;

/** Minimal internal projection used by the paginated video catalog. */
@Data
public class VideoCatalogDeviceRow {
  private Long id;
  private String iotId;
  private String deviceId;
  private String deviceName;
  private String productKey;
  private String gwProductKey;
  private String extDeviceId;
  private String instanceKey;
  private String platformType;
  private String tenantId;
  private Integer state;
  /** Read only to derive a channel summary; never serialized in the catalog response. */
  private String configuration;
}
