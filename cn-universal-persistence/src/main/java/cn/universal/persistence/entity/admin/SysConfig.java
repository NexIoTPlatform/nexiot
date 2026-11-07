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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import tk.mybatis.mapper.annotation.KeySql;

/** 参数配置表 sys_config @Author ruoyi */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@Table(name = "sys_config")
@Schema(description = "系统配置实体")
public class SysConfig {

  private static final long serialVersionUID = 1L;

  /** 参数主键 */
  @Id
  @Schema(description = "配置ID")
  @KeySql(genId = SQenGenId.class)
  private Long configId;

  /** 参数名称 */
  @Column(name = "config_name")
  @Schema(description = "配置名称")
  private String configName;

  /** 参数键名 */
  @Column(name = "config_key")
  @Schema(description = "配置键名")
  private String configKey;

  /** 参数键值 */
  @Column(name = "config_value")
  @Schema(description = "配置键值")
  private String configValue;

  /** 系统内置（Y是 N否） */
  @Column(name = "config_type")
  @Schema(description = "配置类型（Y是 N否）")
  private String configType;

  /** 创建者 */
  @Column(name = "create_by")
  @Schema(description = "创建者")
  private String createBy;

  /** 创建时间 */
  @Column(name = "create_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createTime;

  /** 更新者 */
  @Column(name = "update_by")
  @Schema(description = "更新者")
  private String updateBy;

  /** 更新时间 */
  @Column(name = "update_time")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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
