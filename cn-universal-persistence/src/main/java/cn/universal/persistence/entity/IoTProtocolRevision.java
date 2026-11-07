package cn.universal.persistence.entity;

import java.util.Date;
import lombok.Data;

@Data
public class IoTProtocolRevision {
  private String id;
  private String protocolId;
  private String source;
  private String sourceHash;
  private String version;
  private String savedBy;
  private Date createdAt;
}
