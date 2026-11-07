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

package cn.universal.admin.platform.service.customfield;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.universal.common.constant.IoTConstant.DeviceNode;
import cn.universal.persistence.entity.IoTProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Modbus协议系统字段注入器
 *
 * <p>当产品为 Modbus 直连设备或接入网关时，自动注入现场连接参数字段；当产品为 Modbus
 * 直连设备或网关子设备时，自动注入从站地址字段。
 *
 * @author NexIoT
 * @since 2025-02-05
 */
@Slf4j
@Component
public class ModbusSystemFieldInjector implements SystemFieldInjector {

  /** 从站地址字段ID */
  public static final String FIELD_SLAVE_ADDRESS = "slaveAddress";
  private static final String PROTOCOL_MODBUS = "modbus";

  @Override
  public boolean supports(IoTProduct product) {
    if (product == null) {
      return false;
    }
    if (DeviceNode.GATEWAY.name().equals(product.getDeviceNode())
        || DeviceNode.DEVICE.name().equals(product.getDeviceNode())) {
      return isModbusAccessProduct(product);
    }
    return DeviceNode.GATEWAY_SUB_DEVICE.name().equals(product.getDeviceNode())
        && isModbusTransportProduct(product);
  }

  @Override
  public void inject(IoTProduct product, JSONArray customFields) {
    if (DeviceNode.GATEWAY.name().equals(product.getDeviceNode()) && isModbusAccessProduct(product)) {
      injectGatewayConnectionFields(customFields);
      log.info("已为Modbus网关产品[{}]注入连接参数字段", product.getProductKey());
      return;
    }

    if (DeviceNode.DEVICE.name().equals(product.getDeviceNode()) && isModbusAccessProduct(product)) {
      injectGatewayConnectionFields(customFields);
      if (addFieldIfAbsent(customFields, buildDirectSlaveAddressField())) {
        log.info("已为Modbus直连产品[{}]注入连接参数和从站地址字段", product.getProductKey());
      } else {
        log.info("已为Modbus直连产品[{}]注入连接参数字段", product.getProductKey());
      }
      return;
    }

    if (DeviceNode.GATEWAY_SUB_DEVICE.name().equals(product.getDeviceNode())
        && isModbusTransportProduct(product)
        && addFieldIfAbsent(customFields, buildSlaveAddressField())) {
      log.info("已为Modbus子设备产品[{}]注入从站地址字段", product.getProductKey());
    }
  }

  @Override
  public int getOrder() {
    return 10; // 较高优先级
  }

  private boolean isModbusAccessProduct(IoTProduct product) {
    return PROTOCOL_MODBUS.equalsIgnoreCase(product.getThirdPlatform());
  }

  private boolean isModbusTransportProduct(IoTProduct product) {
    return PROTOCOL_MODBUS.equalsIgnoreCase(product.getTransportProtocol())
        || isModbusAccessProduct(product);
  }

  private void injectGatewayConnectionFields(JSONArray customFields) {
    addFieldIfAbsent(
        customFields,
        selectField(
            "transportType",
            "传输方式",
            "Modbus 现场连接方式",
            "modbus_transport_type",
            "tcp-client",
            true));
    addFieldIfAbsent(
        customFields,
        textField("host", "设备IP", "Modbus TCP/RTU over TCP 设备地址", "例如 192.168.1.20", false));
    addFieldIfAbsent(customFields, numberField("port", "端口", "Modbus TCP端口", 502, 1, 65535, false));
    addFieldIfAbsent(
        customFields,
        numberField("connectTimeoutMs", "连接超时(ms)", "建立连接等待时间", 3000, 100, 60000, false));
    addFieldIfAbsent(
        customFields,
        numberField("timeoutMs", "请求超时(ms)", "单次读写请求等待时间", 3000, 100, 60000, false));
    addFieldIfAbsent(customFields, textField("serialPort", "串口", "Modbus RTU 串口名称", "例如 COM3 或 /dev/ttyUSB0", false));
    addFieldIfAbsent(customFields, numberField("baudRate", "波特率", "串口通信波特率", 9600, 300, 921600, false));
    addFieldIfAbsent(
        customFields,
        selectField("parity", "校验位", "串口校验位", "modbus_parity", "NONE", false));
    addFieldIfAbsent(customFields, numberField("dataBits", "数据位", "串口数据位", 8, 5, 8, false));
    addFieldIfAbsent(customFields, numberField("stopBits", "停止位", "串口停止位", 1, 1, 2, false));
    addFieldIfAbsent(customFields, numberField("idleTimeoutMs", "空闲回收(ms)", "连接空闲多久后关闭", 120000, 10000, 3600000, false));
  }

  private JSONObject buildSlaveAddressField() {
    JSONObject field = numberField("slaveAddress", "从站地址", "Modbus从站地址，范围1-247，设备ID将自动生成", null, 1, 247, true);
    field.set("placeholder", "请输入从站地址(1-247)");
    field.set("helpText", "设备ID将根据父设备ID和从站地址自动生成");
    field.set("autoGenerate", true);
    return field;
  }

  private JSONObject buildDirectSlaveAddressField() {
    JSONObject field = numberField("slaveAddress", "从站地址", "Modbus从站地址，范围1-247", 1, 1, 247, true);
    field.set("placeholder", "请输入从站地址(1-247)");
    field.set("helpText", "直连设备读写时使用的 Modbus Unit ID/从站地址");
    field.set("autoGenerate", false);
    return field;
  }

  private JSONObject textField(
      String id, String name, String description, String placeholder, boolean required) {
    JSONObject field = baseField(id, name, description, "text", required);
    field.set("placeholder", placeholder);
    return field;
  }

  private JSONObject numberField(
      String id,
      String name,
      String description,
      Integer defaultValue,
      Integer min,
      Integer max,
      boolean required) {
    JSONObject field = baseField(id, name, description, "number", required);
    if (defaultValue != null) {
      field.set("defaultValue", defaultValue);
    }
    if (min != null) {
      field.set("min", min);
    }
    if (max != null) {
      field.set("max", max);
    }
    return field;
  }

  private JSONObject selectField(
      String id,
      String name,
      String description,
      String dictType,
      String defaultValue,
      boolean required) {
    JSONObject field = baseField(id, name, description, "select", required);
    field.set("dictType", dictType);
    field.set("defaultValue", defaultValue);
    field.set("options", buildOptions(dictType));
    return field;
  }

  private JSONArray buildOptions(String dictType) {
    JSONArray options = new JSONArray();
    if ("modbus_transport_type".equals(dictType)) {
      options.add(option("tcp-client", "Modbus TCP Client"));
      options.add(option("rtu-tcp", "Modbus RTU over TCP"));
      options.add(option("rtu-serial", "Modbus RTU Serial"));
      return options;
    }
    if ("modbus_parity".equals(dictType)) {
      options.add(option("NONE", "None"));
      options.add(option("EVEN", "Even"));
      options.add(option("ODD", "Odd"));
      options.add(option("MARK", "Mark"));
      options.add(option("SPACE", "Space"));
    }
    return options;
  }

  private JSONObject option(String value, String label) {
    JSONObject option = new JSONObject();
    option.set("value", value);
    option.set("label", label);
    return option;
  }

  private JSONObject baseField(
      String id, String name, String description, String type, boolean required) {
    JSONObject field = new JSONObject();
    field.set("id", id);
    field.set("name", name);
    field.set("description", description);
    field.set("type", type);
    field.set("required", required);
    field.set("system", true);
    return field;
  }

  private boolean addFieldIfAbsent(JSONArray customFields, JSONObject field) {
    String id = field.getStr("id");
    for (int i = 0; i < customFields.size(); i++) {
      JSONObject existing = customFields.getJSONObject(i);
      if (existing != null && id.equals(existing.getStr("id"))) {
        return false;
      }
    }
    customFields.add(field);
    return true;
  }
}
