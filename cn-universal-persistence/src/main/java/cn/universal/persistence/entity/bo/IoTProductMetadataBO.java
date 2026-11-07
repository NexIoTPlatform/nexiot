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
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "IoT产品元数据业务对象")
public class IoTProductMetadataBO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "元数据ID")
  private String id;

  @Schema(description = "元数据名称")
  private String name;

  @Schema(description = "数据源")
  private String source;

  @Schema(description = "是否为配置")
  private boolean config;

  @Schema(description = "扩展信息")
  private Map<String, Object> expands;

  @Schema(description = "值类型")
  private Map<String, Object> valueType;

  @Schema(description = "输入参数列表")
  private List<Map<String, Object>> inputs;

  @Schema(description = "输出参数")
  private Map<String, Object> output;

  @Schema(description = "模式")
  private String mode;

  @Schema(description = "描述信息")
  private String description;
}
