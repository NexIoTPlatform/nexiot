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

package cn.universal.persistence.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "IoT设备事件实体")
public class IoTDeviceEvents implements Serializable {

  @Schema(description = "事件标识")
  private String id;

  @Schema(description = "事件名称")
  private String name;

  @Schema(description = "事件级别")
  private String level;

  @Schema(description = "完整值类型定义")
  private Map<String, Object> valueType;

  @Schema(description = "扩展配置")
  private Map<String, Object> expands;

  @Schema(description = "描述")
  private String description;

  @Schema(description = "事件展示次数，最多统计100条，达到100条返回99+")
  private String qty;

  @Schema(description = "最新事件上报时间")
  private String time;

  // 是否设置存储策略
  @Schema(description = "是否设置存储策略")
  private boolean storagePolicy;
}
