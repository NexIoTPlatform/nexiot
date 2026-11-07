package cn.universal.manager.notice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date;

@Table(name = "notice_template")
public class NoticeTemplate extends NoticeOwnedResource {

  @Id private Long id;
  private String name;

  @Column(name = "channel_type")
  private String channelType;

  @Column(name = "channel_id")
  private Long channelId;

  @Column(name = "wechat_config")
  @com.fasterxml.jackson.annotation.JsonIgnore
  private String wechatConfig;

  public String getWechatConfig() { return wechatConfig; }
  public void setWechatConfig(String value) { this.wechatConfig = value; }
  public WechatTemplateConfig getWechat() {
    return wechatConfig == null || wechatConfig.isBlank() ? null
        : cn.hutool.json.JSONUtil.toBean(wechatConfig, WechatTemplateConfig.class);
  }
  public void setWechat(WechatTemplateConfig value) {
    this.wechatConfig = value == null ? "" : cn.hutool.json.JSONUtil.toJsonStr(value);
  }

  private String content;
  private String receivers;
  private String status;
  private String remark;
  private String creator;

  @Column(name = "create_time")
  private Date createTime;

  @Column(name = "update_time")
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
