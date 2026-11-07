/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.ossm.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table(name = "dev_camera_video")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DevCameraVideoVo implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 主键ID */
  @Id private Long id;

  /** 设备序列号 */
  @Column(name = "device_id")
  private String deviceId;

  /** 录像地址 */
  @Column(name = "url")
  private String url;

  /** 录像标识 */
  @Column(name = "video_index")
  private int videoIndex;

  /** 录像状态 */
  @Column(name = "status")
  private String status;

  /** 创建人 */
  @Column(name = "create_by")
  private String createBy;

  /** 总数 */
  private String total;

  /** 创建时间 */
  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date createTime;

  @Column(name = "begin_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date beginTime;

  @Column(name = "end_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date endTime;
}
