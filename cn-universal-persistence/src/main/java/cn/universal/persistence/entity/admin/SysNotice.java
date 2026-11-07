package cn.universal.persistence.entity.admin;

import cn.universal.common.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 系统通知对象 sys_notice
 *
 * @author universal
 * @date 2023-01-01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@Table(name = "sys_notice")
@Schema(description = "系统通知实体")
public class SysNotice extends BaseEntity {
  private static final long serialVersionUID = 1L;

  /** 公告ID */
  @Schema(description = "公告ID")
  private Long noticeId;

  /** 公告标题 */
  @Schema(description = "公告标题")
  private String noticeTitle;

  /** 公告类型（1通知 2公告） */
  @Schema(description = "公告类型（1通知 2公告）")
  private String noticeType;

  /** 公告内容 */
  @Schema(description = "公告内容")
  private String noticeContent;

  /** 公告状态（0正常 1关闭） */
  @Schema(description = "公告状态（0正常 1关闭）")
  private String status;

  public void setNoticeId(Long noticeId) {
    this.noticeId = noticeId;
  }

  public Long getNoticeId() {
    return noticeId;
  }

  public void setNoticeTitle(String noticeTitle) {
    this.noticeTitle = noticeTitle;
  }

  public String getNoticeTitle() {
    return noticeTitle;
  }

  public void setNoticeType(String noticeType) {
    this.noticeType = noticeType;
  }

  public String getNoticeType() {
    return noticeType;
  }

  public void setNoticeContent(String noticeContent) {
    this.noticeContent = noticeContent;
  }

  public String getNoticeContent() {
    return noticeContent;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getStatus() {
    return status;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
        .append("noticeId", getNoticeId())
        .append("noticeTitle", getNoticeTitle())
        .append("noticeType", getNoticeType())
        .append("noticeContent", getNoticeContent())
        .append("status", getStatus())
        .append("createBy", getCreateBy())
        .append("createTime", getCreateTime())
        .append("updateBy", getUpdateBy())
        .append("updateTime", getUpdateTime())
        .append("remark", getRemark())
        .toString();
  }
}
