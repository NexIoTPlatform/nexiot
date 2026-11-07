package cn.universal.persistence.entity.vo;

import java.util.Date;

public class EventCountAndTimeVO {
  private String event;
  // 符合条件的总条数
  private Long totalCount;
  // 最新一条的创建时间
  private Date latestCreateTime;

  // 生成getter/setter
  public String getEvent() {
    return event;
  }

  public void setEvent(String event) {
    this.event = event;
  }

  public Long getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(Long totalCount) {
    this.totalCount = totalCount;
  }

  public Date getLatestCreateTime() {
    return latestCreateTime;
  }

  public void setLatestCreateTime(Date latestCreateTime) {
    this.latestCreateTime = latestCreateTime;
  }
}
