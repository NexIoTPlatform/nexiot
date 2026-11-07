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

package cn.universal.persistence.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "IoT设备分组VO")
public class IoTDeviceGroupVO implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 分组ID，非自增 */
  @Schema(description = "分组ID")
  private Long id;

  /** 分组名称 */
  @Schema(description = "分组名称")
  private String groupName;

  /** 分组标识 */
  @Schema(description = "分组编码")
  private String groupCode;

  /** 群组描述 */
  @Schema(description = "分组描述")
  private String groupDescribe;

  /** 父id */
  @Schema(description = "父分组ID")
  private Long parentId;

  /** 是否有子分组 */
  @Schema(description = "是否有子分组")
  private Integer hasChild;

  /** 分组级别 */
  @Schema(description = "分组级别")
  private Integer groupLevel;

  /** 设备列表 */
  @Schema(description = "设备列表")
  private List<IoTDeviceVO> instanceVOS;

  /** 分组子集 */
  @Schema(description = "子分组列表")
  private List<IoTDeviceGroupVO> children;
}
