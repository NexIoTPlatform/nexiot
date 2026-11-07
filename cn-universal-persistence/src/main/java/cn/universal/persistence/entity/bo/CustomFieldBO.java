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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 产品自定义扩展字段定义
 * 用于动态配置设备添加表单的扩展字段
 *
 * @author NexIoT
 * @since 2025-02-05
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "产品自定义扩展字段定义")
public class CustomFieldBO implements Serializable {

  private static final long serialVersionUID = 1L;

  // ========== 基础信息 ==========

  /** 字段标识（唯一），如: slaveAddress, appKey */
  @Schema(description = "字段标识", example = "slaveAddress")
  private String id;

  /** 字段名称（显示用），如: 从站地址, 安全码 */
  @Schema(description = "字段名称", example = "从站地址")
  private String name;

  /** 字段描述/帮助文本 */
  @Schema(description = "字段描述", example = "Modbus从站地址，范围1-247")
  private String description;

  // ========== 类型与验证 ==========

  /** 字段类型: string, number, select, password, textarea */
  @Schema(description = "字段类型", example = "number", allowableValues = {"string", "number", "select", "password", "textarea"})
  @Builder.Default
  private String type = "string";

  /** 是否必填 */
  @Schema(description = "是否必填", example = "true")
  @Builder.Default
  private Boolean required = false;

  // ========== 数值类型扩展 ==========

  /** 最小值（type=number时有效） */
  @Schema(description = "最小值", example = "1")
  private Integer min;

  /** 最大值（type=number时有效） */
  @Schema(description = "最大值", example = "247")
  private Integer max;

  // ========== 字符串类型扩展 ==========

  /** 最小长度 */
  @Schema(description = "最小长度", example = "1")
  private Integer minLength;

  /** 最大长度 */
  @Schema(description = "最大长度", example = "64")
  private Integer maxLength;

  /** 正则表达式验证 */
  @Schema(description = "正则表达式", example = "^[a-zA-Z0-9]+$")
  private String pattern;

  // ========== 选择类型扩展 ==========

  /** 下拉选项（type=select时有效） */
  @Schema(description = "下拉选项列表")
  private List<FieldOption> options;

  // ========== UI控制 ==========

  /** 占位提示文本 */
  @Schema(description = "占位提示", example = "请输入从站地址")
  private String placeholder;

  /** 是否禁用编辑（编辑模式下） */
  @Schema(description = "是否禁用", example = "false")
  @Builder.Default
  private Boolean disabled = false;

  /** 是否隐藏字段 */
  @Schema(description = "是否隐藏", example = "false")
  @Builder.Default
  private Boolean hidden = false;

  /** 帮助文本（表单项下方显示） */
  @Schema(description = "帮助文本", example = "设备ID将根据此值自动生成")
  private String helpText;

  // ========== 系统标记 ==========

  /** 是否系统内置字段（如modbus的slaveAddress），系统字段不可删除 */
  @Schema(description = "是否系统字段", example = "true")
  @Builder.Default
  private Boolean system = false;

  /** 值是否自动生成（如设备ID根据从站地址生成） */
  @Schema(description = "是否自动生成", example = "false")
  @Builder.Default
  private Boolean autoGenerate = false;

  /**
   * 下拉选项定义
   */
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  @Schema(description = "字段选项")
  public static class FieldOption implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 选项显示文本 */
    @Schema(description = "选项标签", example = "选项一")
    private String label;

    /** 选项值 */
    @Schema(description = "选项值", example = "1")
    private String value;
  }
}
