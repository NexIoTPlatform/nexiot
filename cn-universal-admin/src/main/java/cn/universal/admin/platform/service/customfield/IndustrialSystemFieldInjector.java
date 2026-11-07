package cn.universal.admin.platform.service.customfield;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.universal.persistence.entity.IoTProduct;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class IndustrialSystemFieldInjector implements SystemFieldInjector {

  private static final String PLATFORM_INDUSTRIAL = "industrial";
  private static final String SERIAL_PORT_OPTION_URL = "/admin/v1/industrial/serial-ports/options";
  private static final String[] NETWORK_PROTOCOLS = {
    "S7", "OPCUA", "IEC104", "EIP", "AB_ETH", "BACNET", "ADS", "MC", "FINS", "GENERIC_TCP",
    "GENERIC_UDP"
  };

  @Override
  public boolean supports(IoTProduct product) {
    if (product == null) {
      return false;
    }
    if (PLATFORM_INDUSTRIAL.equalsIgnoreCase(product.getThirdPlatform())) {
      return true;
    }
    if (isIndustrialAccessProtocol(product.getThirdPlatform())) {
      return true;
    }
    return false;
  }

  @Override
  public void inject(IoTProduct product, JSONArray customFields) {
    String protocol = StrUtil.blankToDefault(normalizeProtocol(product), "MODBUS");
    if ("CUSTOM".equals(protocol)) {
      addFieldIfAbsent(
          customFields,
          numberField("timeoutMs", "请求超时(ms)", "单次读写请求等待时间", 3000, 100, 60000, false));
    } else {
      injectCommonConnectionFields(
          customFields,
          true,
          false,
          protocol,
          defaultProtocolPort(protocol));
    }
    injectProtocolFields(
        customFields,
        protocol,
        false,
        defaultModbusTransportType(product));
  }

  @Override
  public int getOrder() {
    return 20;
  }

  private void injectCommonConnectionFields(
      JSONArray customFields, boolean hostRequired, boolean protocolSelectable, String protocol, Integer defaultPort) {
    boolean modbusTransportAware = protocolSelectable || "MODBUS".equals(protocol);
    JSONObject host =
        textField(
            "host",
            "设备IP/主机",
            "工业设备、网关或协议服务地址",
            "例如 192.168.1.10",
            hostRequired);
    JSONObject port =
        numberField(
            "port",
            "端口",
            "工业协议 TCP/UDP 端口",
            defaultPort,
            1,
            65535,
            true);
    if (protocolSelectable) {
      withVisibleWhenAnyOf(
          host,
          visibleWhenIn("industrialProtocol", NETWORK_PROTOCOLS),
          visibleWhenIn("transportType", "tcp-client", "rtu-tcp"));
      withVisibleWhenAnyOf(
          port,
          visibleWhenIn("industrialProtocol", NETWORK_PROTOCOLS),
          visibleWhenIn("transportType", "tcp-client", "rtu-tcp"));
    } else if (modbusTransportAware) {
      withVisibleWhenIn(host, "transportType", "tcp-client", "rtu-tcp");
      withVisibleWhenIn(port, "transportType", "tcp-client", "rtu-tcp");
    }
    addFieldIfAbsent(customFields, host);
    addFieldIfAbsent(customFields, port);
    addFieldIfAbsent(
        customFields,
        numberField("connectTimeoutMs", "连接超时(ms)", "建立连接等待时间", 3000, 100, 60000, false));
    addFieldIfAbsent(
        customFields,
        numberField("timeoutMs", "请求超时(ms)", "单次读写请求等待时间", 3000, 100, 60000, false));
  }

  private void injectProtocolFields(
      JSONArray customFields, String protocol, boolean protocolSelectable, String defaultModbusTransportType) {
    if ("S7".equals(protocol) || protocol == null) {
      addProtocolField(
          customFields,
          numberField("rack", "S7 Rack", "西门子机架号", 0, 0, 8, false),
          protocolSelectable,
          "S7");
      addProtocolField(
          customFields,
          numberField("slot", "S7 Slot", "西门子槽位号", 1, 0, 32, false),
          protocolSelectable,
          "S7");
    }
    if ("MODBUS".equals(protocol) || protocol == null) {
      addProtocolField(
          customFields,
          selectField(
              "transportType",
              "Modbus传输",
              "新工业 Modbus 现场连接方式",
              defaultModbusTransportType,
              true,
              option("tcp-client", "Modbus TCP"),
              option("rtu-tcp", "RTU over TCP"),
              option("rtu-serial", "RTU Serial"),
              option("ascii-serial", "ASCII Serial")),
          protocolSelectable,
          "MODBUS");
      addProtocolField(
          customFields,
          numberField("slaveAddress", "从站地址", "Modbus Unit ID/从站地址，TCP 与串口场景均使用", 1, 1, 247, true),
          protocolSelectable,
          "MODBUS");
      addProtocolField(
          customFields,
          withVisibleWhenIn(
              serialPortField(),
              "transportType",
              "rtu-serial",
              "ascii-serial"),
          protocolSelectable,
          "MODBUS");
      addProtocolField(
          customFields,
          withVisibleWhenIn(
              numberField("baudRate", "波特率", "串口通信波特率", 9600, 300, 921600, false),
              "transportType",
              "rtu-serial",
              "ascii-serial"),
          protocolSelectable,
          "MODBUS");
      addProtocolField(
          customFields,
          withVisibleWhenIn(
              selectField(
                  "parity",
                  "校验位",
                  "串口校验位",
                  "NONE",
                  false,
                  option("NONE", "None"),
                  option("EVEN", "Even"),
                  option("ODD", "Odd"),
                  option("MARK", "Mark"),
                  option("SPACE", "Space")),
              "transportType",
              "rtu-serial",
              "ascii-serial"),
          protocolSelectable,
          "MODBUS");
      addProtocolField(
          customFields,
          withVisibleWhenIn(
              numberField("dataBits", "数据位", "串口通信数据位", 8, 5, 8, false),
              "transportType",
              "rtu-serial",
              "ascii-serial"),
          protocolSelectable,
          "MODBUS");
      addProtocolField(
          customFields,
          withVisibleWhenIn(
              numberField("stopBits", "停止位", "串口通信停止位", 1, 1, 2, false),
              "transportType",
              "rtu-serial",
              "ascii-serial"),
          protocolSelectable,
          "MODBUS");
    }
    if ("OPCUA".equals(protocol) || protocol == null) {
      addProtocolField(
          customFields,
          textField("username", "用户名", "OPC UA 用户名", "", false),
          protocolSelectable,
          "OPCUA");
      addProtocolField(
          customFields,
          passwordField("password", "密码", "OPC UA 密码", false),
          protocolSelectable,
          "OPCUA");
    }
    if ("EIP".equals(protocol) || protocol == null) {
      addProtocolField(
          customFields,
          numberField("backplane", "背板号", "ControlLogix 路由背板", 1, 0, 255, false),
          protocolSelectable,
          "EIP");
      addProtocolField(
          customFields,
          numberField("slot", "槽位", "目标 Logix 控制器所在槽位", 0, 0, 255, false),
          protocolSelectable,
          "EIP");
      addProtocolField(
          customFields,
          selectField(
              "bigEndian",
              "大端传输",
              "PLC4J EIP 数据传输字节序",
              "true",
              false,
              option("true", "启用"),
              option("false", "关闭")),
          protocolSelectable,
          "EIP");
      addProtocolField(
          customFields,
          selectField(
              "forceUnconnectedOperation",
              "强制非连接消息",
              "目标不支持 Class 3 connected messaging 时启用",
              "false",
              false,
              option("false", "关闭"),
              option("true", "启用")),
          protocolSelectable,
          "EIP");
      addProtocolField(
          customFields,
          textField(
              "communicationPath",
              "通信路由",
              "PLC4J 多背板路由路径，填写后优先于背板和槽位",
              "例如 1,4,2,192.168.0.1,1,1",
              false),
          protocolSelectable,
          "EIP");
    }
    if ("AB_ETH".equals(protocol) || protocol == null) {
      addProtocolField(
          customFields,
          numberField("station", "站号", "DF1/PCCC目标站号，直连PLC通常使用0", 0, 0, 255, true),
          protocolSelectable,
          "AB_ETH");
    }
    if ("IEC104".equals(protocol) || protocol == null) {
      addProtocolField(
          customFields,
          numberField("commonAddress", "默认公共地址", "IEC104 默认 CA/ASDU 地址，点位地址可覆盖", 1, 0, 65535, false),
          protocolSelectable,
          "IEC104");
      addProtocolField(
          customFields,
          numberField("originatorAddress", "源发地址", "IEC104 Originator Address，通常为 0", 0, 0, 255, false),
          protocolSelectable,
          "IEC104");
      addProtocolField(
          customFields,
          numberField("commandTimeoutMs", "命令超时(ms)", "遥控/设点命令等待确认时间", 3000, 100, 60000, false),
          protocolSelectable,
          "IEC104");
      addProtocolField(
          customFields,
          numberField("ackEveryNFrames", "确认帧间隔", "收到 I 帧达到该数量后发送 S 帧确认", 8, 1, 32767, false),
          protocolSelectable,
          "IEC104");
      addProtocolField(
          customFields,
          selectField(
              "codecMode",
              "产品编解码",
              "AUTO 时仅在产品绑定驱动协议且支持方法时调用 preDecode/decode/encode",
              "AUTO",
              false,
              option("AUTO", "自动"),
              option("NONE", "禁用")),
          protocolSelectable,
          "IEC104");
      addProtocolField(
          customFields,
          selectField(
              "qualityPushMode",
              "质量上报",
              "是否为 IEC104 遥信/遥测附加质量与源时间属性",
              "NONE",
              false,
              option("NONE", "不附加"),
              option("PROPERTY_SUFFIX", "属性后缀")),
          protocolSelectable,
          "IEC104");
    }
    if ("BACNET".equals(protocol) || protocol == null) {
      addProtocolField(
          customFields,
          numberField("localDeviceId", "BACnet本地设备ID", "本地 BACnet Device ID", 4194000, 1, 4194303, false),
          protocolSelectable,
          "BACNET");
      addProtocolField(
          customFields,
          numberField("deviceInstance", "BACnet目标实例", "目标 BACnet Device Instance", null, 0, 4194303, false),
          protocolSelectable,
          "BACNET");
      addProtocolField(
          customFields,
          numberField("localPort", "BACnet本地端口", "NexIoT 本地 UDP 端口，0 表示自动分配", 0, 0, 65535, false),
          protocolSelectable,
          "BACNET");
      addProtocolField(
          customFields,
          textField("localBindAddress", "BACnet绑定地址", "NexIoT 本地 UDP 绑定地址", "0.0.0.0", false),
          protocolSelectable,
          "BACNET");
      addProtocolField(
          customFields,
          textField("broadcastAddress", "BACnet广播/目标地址", "定向采集建议填写目标设备IP；广播发现可使用网段广播地址", "255.255.255.255", false),
          protocolSelectable,
          "BACNET");
      addProtocolField(
          customFields,
          selectField(
              "directedOnly",
              "BACnet定向模式",
              "有明确目标主机时跳过启动广播，直接向目标设备发起读写",
              "true",
              false,
              option("true", "启用"),
              option("false", "关闭")),
          protocolSelectable,
          "BACNET");
    }
    if ("ADS".equals(protocol) || protocol == null) {
      addProtocolField(
          customFields,
          textField("targetAmsNetId", "ADS目标AMS", "目标 AMS Net ID", "例如 192.168.1.60.1.1", true),
          protocolSelectable,
          "ADS");
      addProtocolField(
          customFields,
          numberField("targetAmsPort", "目标AMS端口", "TwinCAT 3 PLC Runtime 1 通常使用851", 851, 1, 65535, true),
          protocolSelectable,
          "ADS");
      addProtocolField(
          customFields,
          textField("sourceAmsNetId", "ADS源AMS", "采集端 AMS Net ID", "例如 192.168.1.20.1.1", true),
          protocolSelectable,
          "ADS");
      addProtocolField(
          customFields,
          numberField("sourceAmsPort", "源AMS端口", "采集端 AMS 客户端端口", 32905, 1, 65535, true),
          protocolSelectable,
          "ADS");
      addProtocolField(
          customFields,
          selectField(
              "loadSymbolAndDataTypeTables",
              "加载符号与类型表",
              "连接时加载 TwinCAT 符号与数据类型表",
              "true",
              false,
              option("true", "启用"),
              option("false", "关闭")),
          protocolSelectable,
          "ADS");
    }
    if ("MC".equals(protocol) || protocol == null) {
      addProtocolField(
          customFields,
          selectField("frame", "MC帧格式", "三菱 MC 帧格式", "3e-binary", false,
              option("3e-binary", "3E Binary")),
          protocolSelectable,
          "MC");
      addProtocolField(
          customFields,
          numberField("networkNo", "网络号", "三菱 MC 网络号", 0, 0, 255, false),
          protocolSelectable,
          "MC");
      addProtocolField(
          customFields,
          numberField("pcNo", "PC号", "三菱 MC PC号", 255, 0, 255, false),
          protocolSelectable,
          "MC");
      addProtocolField(
          customFields,
          numberField("ioNo", "IO号", "三菱 MC IO号", 1023, 0, 65535, false),
          protocolSelectable,
          "MC");
      addProtocolField(
          customFields,
          numberField("stationNo", "站号", "三菱 MC 站号", 0, 0, 31, false),
          protocolSelectable,
          "MC");
    }
    if ("FINS".equals(protocol) || protocol == null) {
      addProtocolField(
          customFields,
          selectField("transport", "FINS传输", "欧姆龙 FINS 传输方式", "tcp", true,
              option("tcp", "TCP")),
          protocolSelectable,
          "FINS");
      addProtocolField(
          customFields,
          numberField("clientNode", "客户端节点", "FINS客户端节点号", 0, 0, 254, false),
          protocolSelectable,
          "FINS");
      addProtocolField(
          customFields,
          numberField("serverNode", "服务端节点", "FINS服务端节点号", 0, 0, 254, false),
          protocolSelectable,
          "FINS");
      addProtocolField(
          customFields,
          numberField("sourceNetwork", "源网络号", "FINS源网络号", 0, 0, 127, false),
          protocolSelectable,
          "FINS");
      addProtocolField(
          customFields,
          numberField("targetNetwork", "目标网络号", "FINS目标网络号", 0, 0, 127, false),
          protocolSelectable,
          "FINS");
    }
    if ("CUSTOM".equals(protocol) || protocol == null) {
      addProtocolField(
          customFields,
          selectField(
              "simulator",
              "模拟驱动",
              "使用本地动态数据模拟器",
              "true",
              true,
              option("true", "启用")),
          protocolSelectable,
          "CUSTOM");
    }
  }

  private String normalizeProtocol(IoTProduct product) {
    String transportProtocol = normalizeProtocolValue(product.getTransportProtocol());
    if (transportProtocol != null) {
      return transportProtocol;
    }
    String accessProtocol = normalizeProtocolValue(product.getThirdPlatform());
    return "MODBUS".equals(accessProtocol) ? null : accessProtocol;
  }

  private String defaultModbusTransportType(IoTProduct product) {
    String value = product == null ? null : firstText(product.getTransportProtocol(), product.getThirdPlatform());
    if (StrUtil.isBlank(value)) {
      return "tcp-client";
    }
    String normalized = normalizeToken(value);
    return switch (normalized) {
      case "modbusrtutcp", "modbusrtuovertcp", "rtuovertcp", "rtutcp" -> "rtu-tcp";
      case "modbusrtu", "modbusserial", "modbusrtuserial", "modbusrtuoverserial",
              "rtuserial", "rtuoverserial", "serialrtu" ->
          "rtu-serial";
      case "modbusascii", "modbusasciiserial", "modbusasciioverserial",
              "asciiserial", "asciioverserial", "serialascii" ->
          "ascii-serial";
      default -> "tcp-client";
    };
  }

  private Integer defaultProtocolPort(String protocol) {
    return switch (protocol == null ? "MODBUS" : protocol) {
      case "OPCUA" -> 4840;
      case "S7" -> 102;
      case "EIP" -> 44818;
      case "AB_ETH" -> 2222;
      case "BACNET" -> 47808;
      case "ADS" -> 48898;
      case "MC" -> 5000;
      case "FINS" -> 9600;
      case "IEC104" -> 2404;
      case "GENERIC_TCP", "GENERIC_UDP" -> 9000;
      default -> 502;
    };
  }

  private String firstText(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (StrUtil.isNotBlank(value)) {
        return value;
      }
    }
    return null;
  }

  private boolean isIndustrialAccessProtocol(String value) {
    String protocol = normalizeProtocolValue(value);
    return protocol != null && !"MODBUS".equals(protocol);
  }

  private String normalizeProtocolValue(String value) {
    if (StrUtil.isBlank(value) || PLATFORM_INDUSTRIAL.equalsIgnoreCase(value)) {
      return null;
    }
    String normalized = normalizeToken(value);
    return switch (normalized) {
      case "s7" -> "S7";
      case "opcua" -> "OPCUA";
      case "modbus", "modbustcp", "modbusrtu", "rtu", "modbusserial", "modbusrtuserial",
              "modbusrtuoverserial", "rtuserial", "rtuoverserial", "serialrtu",
              "modbusascii", "ascii", "modbusasciiserial", "modbusasciioverserial",
              "asciiserial", "asciioverserial", "serialascii", "modbusrtutcp",
              "modbusrtuovertcp", "rtutcp", "rtuovertcp", "modbustcpclient" ->
          "MODBUS";
      case "eip", "ethernetip" -> "EIP";
      case "logix", "rockwelllogix" -> "EIP";
      case "abeth", "allenbradleyeth", "allenbradleylegacyeth" -> "AB_ETH";
      case "bacnet", "bacnetip" -> "BACNET";
      case "ads", "twincat", "beckhoffads" -> "ADS";
      case "mc", "mitsubishi", "mitsubishimc" -> "MC";
      case "fins", "omron", "omronfins" -> "FINS";
      case "iec104", "iec60870", "iec608705104", "iec60870104" -> "IEC104";
      case "generictcp", "tcpclient" -> "GENERIC_TCP";
      case "genericudp", "udpclient" -> "GENERIC_UDP";
      case "custom", "simulator", "simulation" -> "CUSTOM";
      default -> null;
    };
  }

  private String normalizeToken(String value) {
    return value.trim().toLowerCase(Locale.ROOT).replace("-", "").replace("_", "").replace("/", "").replace(" ", "");
  }

  private JSONObject textField(String id, String name, String description, String placeholder, boolean required) {
    JSONObject field = baseField(id, name, description, "text", required);
    field.set("placeholder", placeholder);
    return field;
  }

  private JSONObject passwordField(String id, String name, String description, boolean required) {
    return baseField(id, name, description, "password", required);
  }

  private JSONObject serialPortField() {
    JSONObject field = selectField(
        "serialPort",
        "采集主机串口",
        "NexIoT 后端采集主机或容器检测到的串口；串口设备需接在后端所在主机。",
        null,
        true);
    field.remove("defaultValue");
    field.set("optionUrl", SERIAL_PORT_OPTION_URL);
    return field;
  }

  private JSONObject numberField(
      String id, String name, String description, Integer defaultValue, Integer min, Integer max, boolean required) {
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
      String id, String name, String description, String defaultValue, boolean required, JSONObject... options) {
    JSONObject field = baseField(id, name, description, "select", required);
    field.set("defaultValue", defaultValue);
    JSONArray values = new JSONArray();
    for (JSONObject option : options) {
      values.add(option);
    }
    field.set("options", values);
    return field;
  }

  private JSONObject baseField(String id, String name, String description, String type, boolean required) {
    JSONObject field = new JSONObject();
    field.set("id", id);
    field.set("name", name);
    field.set("description", description);
    field.set("type", type);
    field.set("required", required);
    field.set("system", true);
    return field;
  }

  private JSONObject option(String value, String label) {
    JSONObject option = new JSONObject();
    option.set("value", value);
    option.set("label", label);
    return option;
  }

  private JSONObject withVisibleWhenIn(JSONObject field, String dependsOn, String... values) {
    addVisibleWhenIn(field, dependsOn, values);
    return field;
  }

  private JSONObject withVisibleWhenAnyOf(JSONObject field, JSONObject... conditions) {
    JSONArray anyOf = new JSONArray();
    for (JSONObject condition : conditions) {
      anyOf.add(condition);
    }
    field.set("visibleWhen", new JSONObject().set("anyOf", anyOf));
    return field;
  }

  private JSONObject visibleWhenIn(String dependsOn, String... values) {
    JSONObject condition = new JSONObject();
    condition.set("field", dependsOn);
    JSONArray acceptedValues = new JSONArray();
    for (String value : values) {
      acceptedValues.add(value);
    }
    condition.set("in", acceptedValues);
    return condition;
  }

  private boolean addProtocolField(
      JSONArray customFields, JSONObject field, boolean protocolSelectable, String protocol) {
    if (protocolSelectable) {
      addVisibleWhenIn(field, "industrialProtocol", protocol);
    }
    return addFieldIfAbsent(customFields, field);
  }

  private void addVisibleWhenIn(JSONObject field, String dependsOn, String... values) {
    JSONObject condition = visibleWhenIn(dependsOn, values);
    Object existing = field.get("visibleWhen");
    if (existing == null) {
      field.set("visibleWhen", condition);
      return;
    }
    JSONArray conditions = new JSONArray();
    if (existing instanceof JSONArray existingConditions) {
      conditions.addAll(existingConditions);
    } else {
      conditions.add(existing);
    }
    conditions.add(condition);
    field.set("visibleWhen", conditions);
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
