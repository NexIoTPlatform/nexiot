package cn.universal.persistence.entity.vo;

import lombok.Data;

/** Scalar projection from a configuration channel array paged by PageHelper in the database. */
@Data
public class VideoCatalogConfigurationChannel {
  private String channelId;
  private String channelName;
  private String channelStatus;
  private Integer channelNo;
  private String streamType;
  private String preview;
  private String playback;
  private String ptz;
  private String audio;
}
