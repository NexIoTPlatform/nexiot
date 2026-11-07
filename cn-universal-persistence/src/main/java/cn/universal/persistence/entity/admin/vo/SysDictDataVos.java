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

import cn.universal.persistence.entity.admin.SysDictData;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 字典类型group分组 @Author ruoyi */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "系统字典数据集合VO")
public class SysDictDataVos implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "字典ID")
  private String id;

  @Schema(description = "字典名称")
  private String name;

  @Schema(description = "字典数据列表")
  private List<SysDictData> array;
}
