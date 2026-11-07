package cn.universal.web.tcp;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.core.engine.extend.HexFunctions;
import cn.universal.core.engine.extend.ProtocolFunctions;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/9/8 13:34
 */
@Slf4j
public class DT645Test {

  @Test
  public void test_bcd_count() {
    System.out.println(DateUtil.year(new Date()));

    //    HexFunctions hex = new HexFunctions();
    //   Integer count=5;
    //   System.out.println(count.toString());
    //    StrUtil.toString()
    //    String cs = hex.hex_bcdToDec(data, 256);
    //    System.out.println(data + cs + "16");
  }

  @Test
  public void test_len_count() {
    ProtocolFunctions protocol = new ProtocolFunctions();
    HexFunctions hex = new HexFunctions();
    String data =
        "68240707ea8dda68a0413335c433353333433333cc3c33575533333b33333333333333333333333333264d08665600883568418986112521508343912843465830315330303138333333d5";
    String cs = protocol.cs_check(data, 256);
    System.out.println(data + cs + "16");
  }

  @Test
  public void test_hex_decToBcd() {
    HexFunctions hex = new HexFunctions();
    ProtocolFunctions protocol = new ProtocolFunctions();
    System.out.println(hex.hex_decToBcd("0.96", 4, 1));
    System.out.println(hex.hex_decToBcd("0.96", 4, 2));
    System.out.println(hex.hex_decToBcd("0.96", 4, 3));
  }

  @Test
  public void setRemote4GServerAddr() {
    //    1.15 更改 IP
    //    1)功能：更改 4G 连接的 IP 地址和端口。
    //    2)指令：18 16 02 22 + IP + 0xFF + 端口,长度需要自己计算
    //    3)格式如下：
    //    68 A0 ... A5 68 A0 NN 4B 49 35 55 IP FF PORT CS 16
    String prefix = "68250707EA8DDA" + "68A0";
    // 指令编号
    String cmdSn = "4B493555";
    HexFunctions hex = new HexFunctions();
    ProtocolFunctions protocol = new ProtocolFunctions();
    //    String dot="32";
    String host = hex.hex_add33(hex.hex_fromAscii("tcp.nexiot.cc"));
    String port = hex.hex_add33(hex.hex_fromAscii("19999"));
    String dot = hex.hex_add33("FF");
    // 数据区
    String dataHex = host + dot + port;
    // CS校验码
    // 数据区长度
    String len = hex.hex_645MakeLen(cmdSn + host + dot + port);
    String concat = prefix + len + cmdSn + host + dot + port;
    String cs = protocol.cs_check(concat, 256);
    String last = prefix + len + cmdSn + dataHex + cs + "16";
    System.out.println(last);
  }

  /** 事件-心跳 */
  @Test
  public void hex_padLeft() {
    HexFunctions hex = new HexFunctions();
    int num = 1;
    System.out.println(hex.hex_add33(hex.hex_fromDecScaledLE(num + "", 2, 4)));

    System.out.println(hex.hex_fromIntLE(num, 4));
    System.out.println(hex.hex_add33(hex.hex_fromIntLE(num, 4)));
  }

  /** 事件-心跳 */
  @Test
  public void hex_fromDecScaledLE() {
    HexFunctions hex = new HexFunctions();
    int num = 1;
    System.out.println(hex.hex_fromDecScaledLE(num + "", 2, 4));
    System.out.println(hex.hex_add33(hex.hex_fromDecScaledLE(num + "", 2, 4)));

    System.out.println(hex.hex_fromIntLE(num, 4));
    System.out.println(hex.hex_add33(hex.hex_fromIntLE(num, 4)));
  }

  /** 事件-心跳 */
  @Test
  public void test() {
    String hex = "68250707ea8dda68aa04aaaaaa020216";
    Assert.assertTrue(check645CS(hex));
  }

  /** 事件-开启http心跳 */
  @Test
  public void calCs() {
    String hex = "68250707ea8dda 68a0043335c433";
    String s = calcCS(hex);
    System.out.println(s);
    Assert.assertNotNull(s);
  }

  /** 指令-查询数据 */
  @Test
  public void fun_query_data() {
    String hex = "68 250707ea8dda 68 a0 04 33 35 c4 33";
    String s = calcCS(hex);
    System.out.println(s);
    Assert.assertNotNull(s);
  }

  /**
   * 校验 645 报文 CS 是否正确
   *
   * @param hex 完整报文（带 68...16），空格或无空格均可
   * @return true-校验通过 false-失败
   */
  public static boolean check645CS(String hex) {
    hex = hex.replaceAll("\\s+", ""); // 去空格
    if (hex.length() < 4 || !hex.startsWith("68") || !hex.endsWith("16")) {
      return false; // 长度或帧头尾非法
    }
    byte[] frame = hexStringToBytes(hex); // 转字节数组
    int sum = 0;
    // 累加 [0, 倒数第2字节]
    int len = frame.length;
    for (int i = 0; i < frame.length - 2; i++) {
      sum += frame[i] & 0xFF;
      System.out.println(sum);
    }
    int csCalc = sum & 0xFF;
    int csRecv = frame[frame.length - 2] & 0xFF; // 倒数第二字节是CS
    return csCalc == csRecv;
  }

  /* ------ 工具方法 ------ */
  private static byte[] hexStringToBytes(String hex) {
    int len = hex.length();
    byte[] out = new byte[len >> 1];
    for (int i = 0; i < len; i += 2) {
      out[i >> 1] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
    }
    return out;
  }

  /**
   * 计算CS校验位（协议1-9：前序所有数据之和对256取余）
   *
   * @param hexStr 16进制字符串（不含结束符16）
   * @return 2位16进制校验位（如0A、FF）
   */
  /**
   * 计算CS校验位（协议1-9：前序所有数据之和对256取余）
   *
   * @param hexStr 16进制字符串（不含结束符16，可含前后/中间空格）
   * @return 2位16进制校验位（如0A、FF）
   */
  public static String calcCS(String hexStr) {
    // 新增：全量去除字符串中所有空格（前后、中间的空格均清除）
    String cleanHexStr = StrUtil.isEmpty(hexStr) ? "" : StrUtil.removeAll(hexStr, " ");

    // 1. 基础校验：处理后字符串不能为空
    if (StrUtil.isEmpty(cleanHexStr)) {
      throw new IllegalArgumentException("CS计算输入字符串不能为空（含空格时已自动清除，仍为空）");
    }

    // 2. 基础校验：处理后字符串长度必须为偶数（16进制每2位代表1字节）
    if (cleanHexStr.length() % 2 != 0) {
      throw new IllegalArgumentException(
          StrUtil.format("CS计算输入字符串处理后长度不是偶数：原输入={}，处理后={}", hexStr, cleanHexStr));
    }

    // 3. 按协议1-9计算：累加所有字节值
    long sum = 0;
    for (int i = 0; i < cleanHexStr.length(); i += 2) {
      // 截取每2位16进制，转成10进制数值累加
      String twoDigit = cleanHexStr.substring(i, i + 2);
      sum += Integer.parseInt(twoDigit, 16);
    }

    // 4. 取低8位（对256取余），补前导零为2位16进制（符合协议格式要求）
    return String.format("%02X", sum & 0xFF);
  }

  /**
   * 构建帧头（协议1-3：68 + 电表地址 + 68 + 控制码）
   *
   * @param meterNo 电表表号（12位16进制，物模型input必传）
   * @param controlCode 控制码（如A0、A2，协议1-10/1-107定义）
   * @return 帧头16进制字符串（如68250707EA8DDA68A0）
   */
  public static String buildFrameHeader(String meterNo, String controlCode) {
    // 校验电表表号格式（12位16进制）
    if (StrUtil.isEmpty(meterNo) || meterNo.length() != 12) {
      log.error("帧头构建失败：电表表号（meterNo）必须为12位16进制，当前：{}", meterNo);
      throw new IllegalArgumentException("电表表号格式错误：必须12位16进制");
    }
    // 校验控制码（2位16进制）
    if (StrUtil.isEmpty(controlCode) || controlCode.length() != 2) {
      log.error("帧头构建失败：控制码（controlCode）必须为2位16进制，当前：{}", controlCode);
      throw new IllegalArgumentException("控制码格式错误：必须2位16进制");
    }
    // 拼接帧头：68 + 电表地址 + 68 + 控制码
    return StrUtil.format("68{}68{}", meterNo, controlCode);
  }

  /**
   * 数值转协议反写16进制（协议1-151：XX.XXXXXX格式，需反写字节）
   *
   * @param value 数值（如购电量10.0、电压240.0）
   * @param digit 小数位数（6=电量，1=电压，3=电流，2=余额阈值）
   * @return 8位反写16进制字符串（4字节，如00002710反写后为10270000）
   */
  public static String valueToReverseHex(double value, int digit) {
    if (digit < 0 || digit > 10) {
      throw new IllegalArgumentException("小数位数（digit）必须在0-10之间：" + digit);
    }
    // 计算倍数（避免Math.pow精度丢失，用long类型）
    long multiplier = (long) Math.pow(10, digit);
    // 转整数（保留指定小数位，四舍五入）
    long intValue = Math.round(value * multiplier);
    // 转8位16进制字符串（不足补前导零）
    String hexStr = String.format("%08X", intValue);
    // 反写字节（调用reverseStr方法）
    return reverseStr(hexStr);
  }

  /**
   * 大小端转换（每2位16进制反转，如12345678→78563412）
   *
   * @param data 16进制字符串（长度需为偶数）
   * @return 反转后的16进制字符串
   */
  public static String reverseStr(String data) {
    if (StrUtil.isEmpty(data)) {
      return "";
    }
    if (data.length() % 2 != 0) {
      log.error("大小端转换失败：输入字符串长度不是偶数，当前：{}", data);
      throw new IllegalArgumentException("大小端转换输入必须为偶数长度的16进制字符串");
    }
    StringBuilder rData = new StringBuilder();
    int seq = data.length();
    // 从末尾开始，每2位截取拼接
    while (seq > 0) {
      rData.append(data.substring(seq - 2, seq));
      seq -= 2;
    }
    return rData.toString();
  }

  /**
   * 数据转换：倒转+减0x33（适配A0控制码数据段，接收端处理）
   *
   * @param str 16进制字符串（A0控制码数据段）
   * @return 处理后的16进制字符串（反写+减33后）
   */
  public static String transform(String str) {
    if (StrUtil.isEmpty(str)) {
      return "";
    }
    if (str.length() % 2 != 0) {
      log.error("数据转换（transform）失败：输入长度不是偶数，当前：{}", str);
      throw new IllegalArgumentException("transform输入必须为偶数长度的16进制字符串");
    }
    // 1. 先反写字节
    String reversedStr = reverseStr(str);
    StringBuilder result = new StringBuilder();
    int i = 0;
    // 2. 每2位处理：33→00，其他减0x33（补码处理）
    while (i < reversedStr.length()) {
      String twoDigit = reversedStr.substring(i, i + 2);
      if ("33".equals(twoDigit)) {
        result.append("00");
      } else {
        int value = Integer.parseInt(twoDigit, 16) - 0x33;
        // 补码处理：确保结果为正（小于0则加256）
        if (value < 0) {
          value += 256;
        }
        result.append(String.format("%02X", value));
      }
      i += 2;
    }
    return result.toString();
  }

  /**
   * 数据转换：加0x33（适配A0/A2控制码数据段，发送端处理）
   *
   * @param strData 16进制字符串（原始数据段）
   * @return 加33后的16进制字符串
   */
  public static String dataplus(String strData) {
    if (StrUtil.isEmpty(strData)) {
      return "";
    }
    if (strData.length() % 2 != 0) {
      log.error("数据转换（dataplus）失败：输入长度不是偶数，当前：{}", strData);
      throw new IllegalArgumentException("dataplus输入必须为偶数长度的16进制字符串");
    }
    StringBuilder sData = new StringBuilder();
    int i = 0;
    // 每2位加0x33，与0xFF按位与确保为字节范围
    while (i < strData.length()) {
      String twoDigit = strData.substring(i, i + 2);
      int pData = (Integer.parseInt(twoDigit, 16) + 0x33) & 0xFF;
      sData.append(String.format("%02X", pData));
      i += 2;
    }
    return sData.toString();
  }
}
