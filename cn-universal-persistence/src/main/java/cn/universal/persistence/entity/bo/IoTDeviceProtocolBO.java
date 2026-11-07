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

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table(name = "iot_device_protocol")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "IoT设备协议业务对象")
public class IoTDeviceProtocolBO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "协议名称")
  private String name;

  @Schema(description = "协议描述")
  private String description;

  @Schema(description = "协议状态")
  private Byte state;

  @Schema(description = "协议ID")
  @Id
  private String id;

  @Schema(description = "协议类型")
  private String type;

  @Schema(description = "配置信息")
  private String configuration;

  @Schema(description = "文件名")
  private String fileName;

  @Schema(description = "URL地址")
  private String url;

  @Schema(description = "JavaScript脚本")
  private String jscript;

  /** 打开编辑器时的源码摘要，用于拒绝覆盖别人刚保存的版本。 */
  private String expectedSourceHash;

  @Schema(description = "负载数据")
  private String payload;

  @Schema(description = "上下文数据")
  private String context;

  @Schema(description = "是否解码")
  private Boolean decode;

  @Schema(description = "预览方法: decode / encode / preDecode")
  private String previewMethod;

  @Schema(description = "是否需要BS4解码")
  private Boolean needBs4Decode;

  @Schema(description = "示例数据")
  private String example;

  @Schema(description = "协议版本")
  private String version;

  @Schema(description = "Spring Bean名称(type=spring时使用)")
  private String beanName;

  /** 创建时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createTime;

  //  private String parentUnionId;

}
