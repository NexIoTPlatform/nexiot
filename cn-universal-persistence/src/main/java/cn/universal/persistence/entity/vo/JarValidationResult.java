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

/**
 * Jar包方法校验结果VO
 *
 * @author gitee.com/NexIoT
 * @since 2026-01-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Jar包方法校验结果")
public class JarValidationResult implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "是否校验成功")
  private Boolean success;

  @Schema(description = "支持的方法列表")
  private List<String> supportMethods;

  @Schema(description = "错误信息")
  private String errorMessage;

  @Schema(description = "类全名")
  private String className;
}
