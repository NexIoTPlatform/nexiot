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

/** 操作日志记录表 oper_log @Author ruoyi */
@Data
@NoArgsConstructor
@Accessors(chain = true)
@Schema(description = "系统操作日志实体")
public class SysOperLog implements Serializable {

  private static final long serialVersionUID = 1L;

  /** 日志主键 */
  @Excel(name = "操作序号", cellType = ColumnType.NUMERIC)
  @Id
  @KeySql(genId = SQenGenId.class)
  @Schema(description = "操作ID")
  private Long operId;

  /** 操作模块 */
  @Excel(name = "操作模块")
  @Schema(description = "操作模块")
  private String title;

  /** 业务类型（0其它 1新增 2修改 3删除） */
  @Excel(name = "业务类型", readConverterExp = "0=其它,1=新增,2=修改,3=删除,4=授权,5=导出,6=导入,7=强退,8=生成代码,9=清空数据")
  @Schema(description = "业务类型 0其它 1新增 2修改 3删除")
  private Integer businessType;

  /** 业务类型数组 */
  @Transient
  @Schema(description = "业务类型数组")
  private Integer[] businessTypes;

  /** 请求方法 */
  @Excel(name = "请求方法")
  @Schema(description = "请求方法")
  private String method;

  /** 请求方式 */
  @Excel(name = "请求方式")
  @Schema(description = "请求方式")
  private String requestMethod;

  /** 操作类别（0其它 1后台用户 2手机端用户） */
  @Excel(name = "操作类别", readConverterExp = "0=其它,1=后台用户,2=手机端用户")
  @Schema(description = "操作类别 0其它 1后台用户 2手机端用户")
  private Integer operatorType;

  /** 操作人员 */
  @Excel(name = "操作人员")
  @Schema(description = "操作人员")
  private String operName;

  /** 部门名称 */
  @Excel(name = "部门名称")
  @Schema(description = "部门名称")
  private String deptName;

  /** 请求url */
  @Excel(name = "请求地址")
  @Schema(description = "操作URL")
  private String operUrl;

  /** 操作地址 */
  @Excel(name = "操作地址")
  @Schema(description = "操作IP")
  private String operIp;

  /** 操作地点 */
  @Excel(name = "操作地点")
  @Schema(description = "操作地点")
  private String operLocation;

  /** 请求参数 */
  @Excel(name = "请求参数")
  @Schema(description = "操作参数")
  private String operParam;

  /** 返回参数 */
  @Excel(name = "返回参数")
  @Schema(description = "返回结果")
  private String jsonResult;

  /** 操作状态（0正常 1异常） */
  @Excel(name = "状态", readConverterExp = "0=正常,1=异常")
  @Schema(description = "操作状态 0正常 1异常")
  private Integer status;

  /** 错误消息 */
  @Excel(name = "错误消息")
  @Schema(description = "错误消息")
  private String errorMsg;

  /** 操作时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Excel(name = "操作时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "操作时间")
  private Date operTime;

  /** 请求参数 */
  @Transient
  @Schema(description = "请求参数")
  private Map<String, Object> params = new HashMap<>();
}
