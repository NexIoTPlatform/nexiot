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

package cn.universal.persistence.entity.admin;

import cn.universal.persistence.common.inteceptor.SQenGenId;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import tk.mybatis.mapper.annotation.KeySql;

/** 字典类型表 sys_dict_type @Author ruoyi */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "sys_dict_type")
@Schema(description = "系统字典类型实体")
public class SysDictType implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 字典主键 */
  //  @Excel(name = "字典主键", cellType = Excel.ColumnType.NUMERIC)
  //  @TableId(value = "dict_id", type = IdType.AUTO)
  @Id
  @Schema(description = "字典主键ID")
  @KeySql(genId = SQenGenId.class)
  private Long dictId;

  /** 字典名称 */
  //  @Excel(name = "字典名称")
  @NotBlank(message = "字典名称不能为空")
  @Size(min = 0, max = 100, message = "字典类型名称长度不能超过100个字符")
  @Column(name = "dict_name")
  @Schema(description = "字典名称")
  private String dictName;

  /** 字典类型 */
  //  @Excel(name = "字典类型")
  @NotBlank(message = "字典类型不能为空")
  @Size(min = 0, max = 100, message = "字典类型类型长度不能超过100个字符")
  @Column(name = "dict_type")
  @Schema(description = "字典类型")
  private String dictType;

  /** 状态（0正常 1停用） */
  //  @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
  @Schema(description = "状态（0正常 1停用）")
  private String status;

  /** 创建者 */
  //  @TableField(fill = FieldFill.INSERT)
  @Column(name = "create_by")
  @Schema(description = "创建者")
  private String createBy;

  /** 创建时间 */
  //  @TableField(fill = FieldFill.INSERT)
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Column(name = "create_time")
  @Schema(description = "创建时间")
  private Date createTime;

  /** 更新者 */
  //  @TableField(fill = FieldFill.INSERT_UPDATE)
  @Column(name = "update_by")
  @Schema(description = "更新者")
  private String updateBy;

  /** 更新时间 */
  //  @TableField(fill = FieldFill.INSERT_UPDATE)
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Column(name = "update_time")
  @Schema(description = "更新时间")
  private Date updateTime;

  /** 备注 */
  @Schema(description = "备注")
  private String remark;

  /** 请求参数 */
  //  @TableField(exist = false)
  @Schema(description = "请求参数")
  private Map<String, Object> params = new HashMap<>();
}
