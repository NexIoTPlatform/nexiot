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
import cn.universal.persistence.common.typehandler.PostgreSqlJsonbStringTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.JdbcType;
import tk.mybatis.mapper.annotation.ColumnType;

@Table(name = "iot_user_application")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "IoT用户应用实体")
public class IoTUserApplication implements Serializable {

  private static final long serialVersionUID = 1L;

  //  @Column(name = "uuid")
  //  private Long uuid;

  @Column(name = "union_id")
  @Excel(name = "用户唯一标识")
  @Schema(description = "用户唯一标识")
  private String unionId;

  @Column(name = "tenant_id")
  @Excel(name = "租户ID")
  @Schema(description = "租户ID")
  private String tenantId;

  /** 调用凭证APPID */
  @Column(name = "app_id")
  @Excel(name = "调用凭证APPID")
  @Schema(description = "应用ID")
  private String appId;

  /** MQ上行主题，英文逗号分隔 */
  @Column(name = "up_topic")
  @Excel(name = "MQ上行主题")
  @Schema(description = "上行主题")
  private String upTopic;

  /** MQ下行主题，英文逗号分隔 */
  @Column(name = "down_topic")
  @Excel(name = "MQ下行主题")
  @Schema(description = "下行主题")
  private String downTopic;

  /** 调用密钥 */
  @Column(name = "app_secret")
  @Excel(name = "调用密钥")
  @Schema(description = "应用密钥")
  private String appSecret;

  /** 授权结束时间 */
  @Column(name = "valid_end_date")
  @Excel(name = "授权结束时间")
  @Schema(description = "授权结束时间")
  private Date validEndDate;

  /** 授权范围 */
  @Excel(name = "授权范围")
  @Schema(description = "授权范围")
  private String scope;

  /** 0-正常，1-停用 */
  @Column(name = "app_status")
  @Excel(name = "应用状态 0-正常，1-停用")
  @Schema(description = "应用状态 0-正常，1-停用")
  private Integer appStatus;

  /** 0-正常，1-删除 */
  @Excel(name = "是否删除 0正常 1删除")
  @Schema(description = "删除状态 0-正常，1-删除")
  private Integer deleted;

  @Column(name = "notify_url")
  @Excel(name = "推送地址")
  @Schema(description = "推送地址")
  private String notifyUrl;

  @Column(name = "app_name")
  @Excel(name = "应用名称")
  @Schema(description = "应用名称")
  private String appName;

  @Id
  @Column(name = "app_unique_id")
  @Excel(name = "应用唯一标识")
  @Schema(description = "应用唯一标识")
  private String appUniqueId;

  @Column(name = "instance")
  @Excel(name = "实例名称")
  @Schema(description = "实例名称")
  private String instance;

  @Column(name = "remark")
  @Excel(name = "描述")
  @Schema(description = "备注")
  private String remark;

  @Column(name = "create_date")
  @Excel(name = "创建时间")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createDate;

  @Column(name = "http_enable")
  @Excel(name = "http启用")
  @Schema(description = "HTTP启用状态 0-停用，1-启用")
  private Integer httpEnable;

  @Column(name = "mqtt_enable")
  @Excel(name = "mqtt启用")
  @Schema(description = "MQTT启用状态 0-停用，1-启用")
  private Integer mqttEnable;

  @ColumnType(jdbcType = JdbcType.OTHER, typeHandler = PostgreSqlJsonbStringTypeHandler.class)
  @Schema(description = "配置信息")
  private String cfg;
}
