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

package cn.universal.persistence.entity.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "IoT设备分组业务对象")
public class IoTDeviceGroupBO {

  private static final long serialVersionUID = 1L;

  /** 分组ID，非自增 */
  @Schema(description = "分组ID")
  private Long id;

  /** 设备id */
  @Schema(description = "设备ID数组")
  private String[] devIds;

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
}
