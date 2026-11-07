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
@Schema(description = "IoT设备属性实体")
public class IoTDeviceProperties implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "属性标识")
  private String id;

  @Schema(description = "属性名称")
  private String name;

  @Schema(description = "数据类型")
  private String type;

  @Schema(description = "属性值来源")
  private String source;

  @Schema(description = "单位")
  private String unit;

  @Schema(description = "枚举键值")
  private String elements;

  @Schema(description = "完整值类型定义")
  private Map<String, Object> valueType;

  @Schema(description = "扩展配置")
  private Map<String, Object> expands;

  @Schema(description = "描述")
  private String description;

  @Schema(description = "读写模式")
  private String mode;
}
