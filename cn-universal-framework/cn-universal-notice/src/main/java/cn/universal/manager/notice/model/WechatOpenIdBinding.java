package cn.universal.manager.notice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;

@Table(name = "notice_wechat_openid")
public class WechatOpenIdBinding extends NoticeOwnedResource {
  @Column(name = "app_id") private String appId;
  public String getAppId() { return appId; }
  public void setAppId(String value) { appId = value; }
  @Id private Long id;
  @Column(name = "channel_id") private Long channelId;
  @Column(name = "user_key") private String userKey;
  private String openid;
  private String unionid;
  private String status;
  @Column(name = "create_time") private Date createTime;
  @Column(name = "update_time") private Date updateTime;
  public Long getId() { return id; } public void setId(Long v) { id = v; }
  public Long getChannelId() { return channelId; } public void setChannelId(Long v) { channelId = v; }
  public String getUserKey() { return userKey; } public void setUserKey(String v) { userKey = v; }
  public String getOpenid() { return openid; } public void setOpenid(String v) { openid = v; }
  public String getUnionid() { return unionid; } public void setUnionid(String v) { unionid = v; }
  public String getStatus() { return status; } public void setStatus(String v) { status = v; }
  public Date getCreateTime() { return createTime; } public void setCreateTime(Date v) { createTime = v; }
  public Date getUpdateTime() { return updateTime; } public void setUpdateTime(Date v) { updateTime = v; }
}
