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

import cn.universal.common.annotation.Excel;
import cn.universal.common.annotation.Excel.ColumnType;
import cn.universal.persistence.common.inteceptor.SQenGenId;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import tk.mybatis.mapper.annotation.KeySql;

/** 系统访问记录表 sys_logininfor @Author ruoyi */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@Schema(description = "系统登录信息实体")
public class SysLogininfor implements Serializable {

  private static final long serialVersionUID = 1L;

  /** ID */
  @Excel(name = "序号", cellType = ColumnType.NUMERIC)
  @Schema(description = "信息ID")
  @Id
  @KeySql(genId = SQenGenId.class)
  private Long infoId;

  /** 用户账号 */
  @Excel(name = "用户账号")
  @Schema(description = "用户账号")
  private String userName;

  /** 登录状态 0成功 1失败 */
  @Excel(name = "登录状态", readConverterExp = "0=成功,1=失败")
  @Schema(description = "登录状态 0成功 1失败")
  private String status;

  /** 登录IP地址 */
  @Excel(name = "登录地址")
  @Schema(description = "登录IP地址")
  private String ipaddr;

  /** 登录地点 */
  @Excel(name = "登录地点")
  @Schema(description = "登录地点")
  private String loginLocation;

  /** 浏览器类型 */
  @Excel(name = "浏览器")
  @Schema(description = "浏览器类型")
  private String browser;

  /** 操作系统 */
  @Excel(name = "操作系统")
  @Schema(description = "操作系统")
  private String os;

  /** 提示消息 */
  @Excel(name = "提示消息")
  @Schema(description = "提示消息")
  private String msg;

  /** 访问时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Excel(name = "访问时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "登录时间")
  private Date loginTime;

  /** 请求参数 */
  @Transient
  @Schema(description = "请求参数")
  private Map<String, Object> params = new HashMap<>();
}
