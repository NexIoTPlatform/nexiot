package cn.universal.manager.notice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Date;

public class NoticeTemplateDTO extends cn.universal.manager.notice.model.NoticeOwnedResource {

  private Long id;
  private String name;

  @JsonProperty("channelType")
  private String channelType;

  @JsonProperty("channelId")
  private Long channelId;

  private cn.universal.manager.notice.model.WechatTemplateConfig wechat;
  public cn.universal.manager.notice.model.WechatTemplateConfig getWechat() { return wechat; }
  public void setWechat(cn.universal.manager.notice.model.WechatTemplateConfig wechat) { this.wechat = wechat; }
  private String content;
  private String receivers;
  private String status;
  private String remark;
  private String creator;

  @JsonProperty("createTime")
  private Date createTime;

  @JsonProperty("updateTime")
  private Date updateTime;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getChannelType() {
    return channelType;
  }

  public void setChannelType(String channelType) {
    this.channelType = channelType;
  }

  public Long getChannelId() {
    return channelId;
  }

  public void setChannelId(Long channelId) {
    this.channelId = channelId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getReceivers() {
    return receivers;
  }

  public void setReceivers(String receivers) {
    this.receivers = receivers;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public Date getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(Date updateTime) {
    this.updateTime = updateTime;
  }
}
