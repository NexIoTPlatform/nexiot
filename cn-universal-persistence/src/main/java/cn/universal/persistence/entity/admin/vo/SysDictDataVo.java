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

package cn.universal.persistence.entity.admin.vo;

import cn.universal.common.constant.IoTUserConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 字典数据表 sys_dict_data @Author ruoyi */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "系统字典数据VO")
public class SysDictDataVo implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 字典编码 */
  @Schema(description = "字典编码")
  private Long dictCode;

  /** 字典排序 */
  @Schema(description = "字典排序")
  private Long dictSort;

  /** 字典标签 */
  @Schema(description = "字典标签")
  private String dictLabel;

  /** 字典键值 */
  @Schema(description = "字典键值")
  private String dictValue;

  /** 字典类型 */
  @Schema(description = "字典类型")
  private String dictType;

  /** 样式属性（其他样式扩展） */
  @Schema(description = "CSS样式类")
  private String cssClass;

  /** 表格字典样式 */
  @Schema(description = "列表样式类")
  private String listClass;

  /** 是否默认（Y是 N否） */
  @Schema(description = "是否默认（Y是 N否）")
  private String isDefault;

  /** 状态（0正常 1停用） */
  @Schema(description = "状态（0正常 1停用）")
  private String status;

  /** 创建者 */
  @Schema(description = "创建者")
  private String createBy;

  /** 创建时间 */
  @Schema(description = "创建时间")
  private Date createTime;

  /** 更新者 */
  @Schema(description = "更新者")
  private String updateBy;

  /** 更新时间 */
  @Schema(description = "更新时间")
  private Date updateTime;

  /** 开始时间 */
  @Schema(description = "开始时间")
  private String startTime;

  /** 结束时间 */
  @Schema(description = "结束时间")
  private String endTime;

  /** 备注 */
  @Schema(description = "备注")
  private String remark;

  /** 请求参数 */
  //  @TableField(exist = false)
  @Schema(description = "请求参数")
  private Map<String, Object> params = new HashMap<>();

  public boolean getDefault() {
    return IoTUserConstants.YES.equals(this.isDefault);
  }
}
