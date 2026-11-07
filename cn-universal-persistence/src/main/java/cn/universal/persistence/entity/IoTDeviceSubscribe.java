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

import cn.universal.persistence.common.inteceptor.SQenGenId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSetter;
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
import tk.mybatis.mapper.annotation.KeySql;

@Table(name = "iot_device_subscribe")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "IoT设备订阅实体")
public class IoTDeviceSubscribe implements Serializable {

  public static final int ENABLED_NO = 0;
  public static final int ENABLED_YES = 1;

  @Schema(description = "主键ID")
  @Id
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 消息类别：属性（PROPERTIES），指令（REPLY），事件（EVENT），上下线（EVENT：online,offline），所有 */
  @Column(name = "msg_type")
  @Schema(description = "消息类型")
  private String msgType;

  /** 订阅级别：设备级，产品级 */
  @Column(name = "sub_type")
  @Schema(description = "订阅类型")
  private String subType;

  /** 设备deviceId */
  @Column(name = "device_id")
  @Schema(description = "设备ID")
  private String deviceId;

  /** 产品ID或者设备唯一标识 */
  @Column(name = "product_key")
  @Schema(description = "产品Key")
  private String productKey;

  /** 产品ID或者设备唯一标识 */
  @Column(name = "iot_id")
  @Schema(description = "IoT ID")
  private String iotId;

  /** 订阅地址 */
  @Schema(description = "订阅URL")
  private String url;

  /** 主题 */
  @Schema(description = "订阅主题")
  private String topic;

  /** 创建时间 */
  @Column(name = "create_date")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Schema(description = "创建时间")
  private Date createDate;

  /** 创建人 */
  @Schema(description = "创建人")
  private String creater;

  /** 实例编号(应用标识) */
  @Column(name = "subscribe_instance")
  @Schema(description = "实例名称")
  private String instance;

  /** 是否启用 */
  @Schema(description = "是否启用")
  private Integer enabled;

  private static final long serialVersionUID = 1L;

  @JsonSetter("enabled")
  public void setEnabledFromBoolean(Boolean enabled) {
    this.enabled = toEnabledValue(enabled);
  }

  public static Integer toEnabledValue(Boolean enabled) {
    if (enabled == null) {
      return null;
    }
    return enabled ? ENABLED_YES : ENABLED_NO;
  }

  public boolean isEnabledFlag() {
    return enabled != null && enabled == ENABLED_YES;
  }

  public static class IoTDeviceSubscribeBuilder {
    public IoTDeviceSubscribeBuilder enabled(Integer enabled) {
      this.enabled = enabled;
      return this;
    }

    public IoTDeviceSubscribeBuilder enabled(Boolean enabled) {
      this.enabled = IoTDeviceSubscribe.toEnabledValue(enabled);
      return this;
    }
  }
}
