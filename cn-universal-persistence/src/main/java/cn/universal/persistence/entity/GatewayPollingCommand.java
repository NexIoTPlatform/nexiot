/*
 *
 * Copyright (c) 2025, IoT-Universal. All Rights Reserved.
 *
 * @Description: 本文件由 Aleo 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: Aleo
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.persistence.entity;

import cn.universal.persistence.common.inteceptor.SQenGenId;
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
import tk.mybatis.mapper.annotation.KeySql;

/**
 * 网关轮询指令实体
 *
 * @author gitee.com/NexIoT
 * @date 2025-10-26
 */
@Data
@Table(name = "iot_gateway_polling_command")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "网关轮询指令实体")
public class GatewayPollingCommand implements Serializable {

  private static final long serialVersionUID = 1L;
  public static final int ENABLED_NO = 0;
  public static final int ENABLED_YES = 1;
  public static final String TASK_TYPE_CLOUD_POLLING = GatewayPollingConfig.TASK_TYPE_CLOUD_POLLING;
  public static final String TASK_TYPE_MODBUS_COLLECT = GatewayPollingConfig.TASK_TYPE_MODBUS_COLLECT;

  /** 主键ID */
  @Id
  @Schema(description = "主键ID")
  @KeySql(genId = SQenGenId.class)
  private Long id;

  /** 网关产品KEY */
  @Column(name = "gateway_product_key")
  @Schema(description = "网关产品Key")
  private String gatewayProductKey;

  /** 网关设备ID */
  @Column(name = "gateway_device_id")
  @Schema(description = "网关设备ID")
  private String gatewayDeviceId;

  /** 任务类型: CLOUD_POLLING-云端轮询透传, MODBUS_COLLECT-Modbus采集 */
  @Column(name = "task_type")
  @Schema(description = "任务类型")
  private String taskType;

  /** 从站设备ID (可选) */
  @Column(name = "slave_device_id")
  @Schema(description = "从站设备ID")
  private String slaveDeviceId;

  /** 指令名称 */
  @Column(name = "command_name")
  @Schema(description = "指令名称")
  private String commandName;

  /** 执行顺序 */
  @Column(name = "execution_order")
  @Schema(description = "执行顺序")
  private Integer executionOrder;

  /** 透传内容或完整的Modbus轮询指令(HEX格式) */
  @Column(name = "command_hex")
  @Schema(description = "透传内容或Modbus指令HEX格式")
  private String commandHex;

  /** 指令类型: MODBUS/S7/OPCUA/CUSTOM */
  @Column(name = "command_type")
  @Schema(description = "指令类型")
  private String commandType;

  /** 协议参数JSON (用于前端回显编辑) */
  @Column(name = "protocol_params")
  @Schema(description = "协议参数JSON")
  private String protocolParams;

  /** 属性映射JSON (寄存器->物模型属性) */
  @Column(name = "property_mapping")
  @Schema(description = "属性映射JSON")
  private String propertyMapping;

  /** 数据解析脚本 (可选) */
  @Column(name = "data_parser_script")
  @Schema(description = "数据解析脚本")
  private String dataParserScript;

  /** 是否启用 */
  @Column(name = "enabled")
  @Schema(description = "是否启用")
  private Integer enabled;

  /** 超时时间(ms) */
  @Column(name = "timeout_ms")
  @Schema(description = "超时时间(毫秒)")
  private Integer timeoutMs;

  /** 描述 */
  @Column(name = "description")
  @Schema(description = "描述")
  private String description;

  /** 创建时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Column(name = "create_time")
  @Schema(description = "创建时间")
  private Date createTime;

  /** 更新时间 */
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @Column(name = "update_time")
  @Schema(description = "更新时间")
  private Date updateTime;

  // 为了信创适配
  @com.fasterxml.jackson.annotation.JsonSetter("enabled")
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

  public static class GatewayPollingCommandBuilder {
    public GatewayPollingCommandBuilder enabled(Integer enabled) {
      this.enabled = enabled;
      return this;
    }

    public GatewayPollingCommandBuilder enabled(Boolean enabled) {
      this.enabled = GatewayPollingCommand.toEnabledValue(enabled);
      return this;
    }
  }
}
