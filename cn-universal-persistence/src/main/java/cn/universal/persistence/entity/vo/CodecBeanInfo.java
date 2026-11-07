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
 * Codec Bean信息VO
 *
 * @author gitee.com/NexIoT
 * @since 2026-01-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Codec Bean信息")
public class CodecBeanInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "Bean名称")
  private String beanName;

  @Schema(description = "产品Key(Bean的productKey()方法返回值)")
  private String productKey;

  @Schema(description = "类全名")
  private String className;

  @Schema(description = "支持的方法列表")
  private List<String> methods;

  @Schema(description = "描述信息")
  private String description;

  @Schema(description = "是否已在数据库中注册")
  private Boolean isRegistered;
}
