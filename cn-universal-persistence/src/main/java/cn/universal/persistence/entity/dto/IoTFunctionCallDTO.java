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

package cn.universal.persistence.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Map;
import lombok.Data;

/**
 * IoT设备功能调用DTO 用于API文档展示功能调用的数据结构
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/12/24
 */
@Data
@Schema(description = "IoT设备功能")
public class IoTFunctionCallDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "功能标识", example = "meterRead", requiredMode = Schema.RequiredMode.REQUIRED)
  private String function;

  @Schema(description = "消息类型", example = "FUNCTIONS", requiredMode = Schema.RequiredMode.REQUIRED)
  private String messageType;

  @Schema(description = "输入参数", example = "{\"switch\": \"open\"}")
  private Map<String, Object> data;
}
