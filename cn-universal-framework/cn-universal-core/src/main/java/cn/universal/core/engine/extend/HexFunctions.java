/*
 * Hex and DL/T645 helpers for Magic-API
 */
package cn.universal.core.engine.extend;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.core.engine.annotation.Comment;
import cn.universal.core.engine.annotation.Function;
import java.nio.charset.Charset;
import org.springframework.stereotype.Component;

@Component
public class HexFunctions implements IdeMagicFunction {

  // =====================
  // 基础十六进制操作（前缀：hex_）
  // =====================
  @Function
  @Comment("hex按字节加0x33,取低8位（DL/T645编码）")
  public String hex_add33(@Comment(name = "target", value = "十六进制字符串") String hex) {
    if (StrUtil.isBlank(hex)) {
      return hex;
    }
    if (hex.length() % 2 != 0) {
      return null;
    }
    StringBuilder sb = new StringBuilder(hex.length());
    for (int i = 0; i < hex.length(); i += 2) {
      int b = Integer.parseInt(hex.substring(i, i + 2), 16);
      int v = (b + 0x33) & 0xFF;
      sb.append(String.format("%02X", v));
    }
    return sb.toString();
  }

  @Function
  @Comment("hex按字节减0x33（DL/T645解码）")
  public String hex_sub33(@Comment(name = "target", value = "十六进制字符串") String hex) {
    if (StrUtil.isBlank(hex)) {
      return hex;
    }
    if (hex.length() % 2 != 0) {
      return null;
    }
    StringBuilder sb = new StringBuilder(hex.length());
    for (int i = 0; i < hex.length(); i += 2) {
      int b = Integer.parseInt(hex.substring(i, i + 2), 16);
      int v = (b - 0x33) & 0xFF;
      sb.append(String.format("%02X", v));
    }
    return sb.toString();
  }

  @Function
  @Comment("hex按字节反转顺序")
  public String hex_reverse(@Comment(name = "target", value = "十六进制字符串") String hex) {
    if (StrUtil.isBlank(hex)) {
      return hex;
    }
    if (hex.length() % 2 != 0) {
      return null;
    }
    String newInput = hex.replaceAll("(.{2})", "$1,");
    String[] inputList = newInput.split(",");
    StringBuilder sb = new StringBuilder(hex.length());
    for (int i = inputList.length - 1; i >= 0; i--) {
      if (StrUtil.isNotEmpty(inputList[i])) {
        sb.append(inputList[i]);
      }
    }
    return sb.toString();
  }

  @Function
  @Comment("hex左侧补齐到指定字节数，填充字节值")
  public String hex_padLeft(
      @Comment(name = "hex", value = "十六进制字符串") String hex,
      @Comment(name = "bytes", value = "目标字节数") Integer bytes,
      @Comment(name = "pad", value = "填充值 十六进制两位") String padHex) {
    if (hex == null || bytes == null || bytes < 0) {
      return null;
    }
    if (StrUtil.isBlank(hex)) {
      hex = "";
    }
    if (hex.length() % 2 != 0) {
      return null;
    }
    if (StrUtil.isBlank(padHex) || padHex.length() != 2) {
      padHex = "00";
    }
    int needBytes = Math.max(0, bytes - hex.length() / 2);
    StringBuilder sb = new StringBuilder(bytes * 2);
    for (int i = 0; i < needBytes; i++) {
      sb.append(padHex.toUpperCase());
    }
    sb.append(hex.toUpperCase());
    return sb.toString();
  }

  // =====================
  // 645 地址/数据标识处理
  // =====================

  @Function
  @Comment("645地址编码：12位数字(BCD)->倒序十六进制")
  public String hex_645AddrEncode(@Comment(name = "addr", value = "地址12位数字") String addr) {
    if (StrUtil.isBlank(addr)) {
      return null;
    }
    String digits = addr.trim();
    if (digits.length() > 12) {
      return null;
    }
    digits = StrUtil.padPre(digits, 12, '0');
    StringBuilder bcd = new StringBuilder(12);
    for (int i = 0; i < digits.length(); i += 2) {
      bcd.append(digits.charAt(i + 1)).append(digits.charAt(i));
    }
    String reversed = hex_reverse(bcd.toString());
    return reversed.toUpperCase();
  }

  @Function
  @Comment("645地址解码：倒序十六进制->12位数字(去前导0)")
  public String hex_645AddrDecode(@Comment(name = "hex", value = "十六进制") String hex) {
    if (StrUtil.isBlank(hex) || hex.length() != 12) {
      return null;
    }
    String rev = hex_reverse(hex);
    StringBuilder digits = new StringBuilder(12);
    for (int i = 0; i < rev.length(); i += 2) {
      digits.append(rev.charAt(i + 1)).append(rev.charAt(i));
    }
    String s = digits.toString().replaceFirst("^0+", "");
    return StrUtil.isEmpty(s) ? "0" : s;
  }

  @Function
  @Comment("数据标识(DI)按字节倒序")
  public String hex_diReverse(@Comment(name = "di", value = "数据标识 4字节/2字节等") String di) {
    if (StrUtil.isBlank(di) || di.length() % 2 != 0) {
      return null;
    }
    return hex_reverse(di);
  }

  @Function
  @Comment("十六进制转GBK字符串")
  public String hex_toGBK(@Comment(name = "hex", value = "十六进制") String hex) {
    if (StrUtil.isBlank(hex)) {
      return null;
    }
    byte[] a = HexUtil.decodeHex(hex);
    return new String(a, Charset.forName("GBK")).trim();
  }

  @Function
  @Comment("GBK字符串转十六进制")
  public String hex_fromGBK(@Comment(name = "str", value = "字符串") String str) {
    if (StrUtil.isBlank(str)) {
      return null;
    }
    byte[] bytes = str.getBytes(Charset.forName("GBK"));
    return HexUtil.encodeHexStr(bytes).toUpperCase();
  }

  // =====================
  // 校验（前缀：crc_）
  // =====================

  @Function
  @Comment("CRC-CS校验值（逐字节求和低8位，两位大写HEX）")
  public String crc_cs(@Comment(name = "hex", value = "十六进制字符串") String hex) {
    if (StrUtil.isBlank(hex) || hex.length() % 2 != 0) {
      return null;
    }
    int sum = 0;
    for (int i = 0; i < hex.length(); i += 2) {
      sum += Integer.parseInt(hex.substring(i, i + 2), 16);
    }
    return String.format("%02X", sum & 0xFF);
  }

  @Function
  @Comment("CRC-CS校验验证（忽略大小写）")
  public Boolean crc_csVerify(
      @Comment(name = "data", value = "十六进制字符串") String hex,
      @Comment(name = "cs", value = "期望CS，两位HEX") String expectedCs) {
    if (StrUtil.isBlank(hex) || StrUtil.isBlank(expectedCs)) {
      return false;
    }
    String calc = crc_cs(hex);
    return StrUtil.equalsIgnoreCase(calc, expectedCs);
  }

  @Function
  @Comment("DL/T645 简易帧解析：返回json{head,addr,ctrl,len,data,cs,tail}")
  public String hex_645SimpleParse(@Comment(name = "frame", value = "完整帧 十六进制") String frame) {
    JSONObject obj = new JSONObject();
    if (StrUtil.isBlank(frame)) {
      return JSONUtil.toJsonStr(obj);
    }
    String f = frame.toUpperCase();
    // 粗略切分：68 AA AA AA AA AA AA 68 C L .... CS 16
    try {
      int head1 = f.indexOf("68");
      int head2 = f.indexOf("68", head1 + 2);
      int tail = f.lastIndexOf("16");
      if (head1 < 0 || head2 < 0 || tail < 0 || tail <= head2) {
        return JSONUtil.toJsonStr(obj);
      }
      String addr = f.substring(head1 + 2, head2);
      String ctrl = f.substring(head2 + 2, head2 + 4);
      String len = f.substring(head2 + 4, head2 + 6);
      int dataLen = Integer.parseInt(len, 16);
      int dataStart = head2 + 6;
      int dataEnd = dataStart + dataLen * 2;
      if (dataEnd + 2 > tail) {
        return JSONUtil.toJsonStr(obj);
      }
      String data = f.substring(dataStart, dataEnd);
      String cs = f.substring(dataEnd, dataEnd + 2);
      obj.set("head", "68");
      obj.set("addr", addr);
      obj.set("ctrl", ctrl);
      obj.set("len", len);
      obj.set("data", data);
      obj.set("cs", cs);
      obj.set("tail", "16");
      return JSONUtil.toJsonStr(obj);
    } catch (Exception e) {
      return JSONUtil.toJsonStr(obj);
    }
  }

  // =====================
  // ASCII/可见字符
  // =====================

  @Function
  @Comment("十六进制转ASCII字符串（直接按字节转换）")
  public String hex_toAscii(@Comment(name = "hex", value = "十六进制") String hex) {
    if (!hex_isValid(hex)) {
      return null;
    }
    String s = hex_stripSpaces(hex);
    byte[] bytes = HexUtil.decodeHex(s);
    return new String(bytes, java.nio.charset.StandardCharsets.US_ASCII);
  }

  @Function
  @Comment("ASCII字符串转十六进制（大写）")
  public String hex_fromAscii(@Comment(name = "str", value = "ASCII字符串") String str) {
    if (str == null) {
      return null;
    }
    byte[] bytes = str.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    return HexUtil.encodeHexStr(bytes).toUpperCase();
  }

  @Function
  @Comment("十六进制转ASCII（不可见字符转为点号.）")
  public String hex_toAsciiSafe(@Comment(name = "hex", value = "十六进制") String hex) {
    if (!hex_isValid(hex)) {
      return null;
    }
    String s = hex_stripSpaces(hex);
    StringBuilder out = new StringBuilder(s.length() / 2);
    for (int i = 0; i < s.length(); i += 2) {
      int b = Integer.parseInt(s.substring(i, i + 2), 16);
      if (b >= 32 && b <= 126) {
        out.append((char) b);
      } else {
        out.append('.');
      }
    }
    return out.toString();
  }

  @Function
  @Comment("是否全部为ASCII可见字符(0x20-0x7E)")
  public Boolean hex_isAsciiPrintable(@Comment(name = "hex", value = "十六进制") String hex) {
    if (!hex_isValid(hex)) {
      return false;
    }
    String s = hex_stripSpaces(hex);
    for (int i = 0; i < s.length(); i += 2) {
      int b = Integer.parseInt(s.substring(i, i + 2), 16);
      if (b < 32 || b > 126) {
        return false;
      }
    }
    return true;
  }

  // =====================
  // HEX常用工具
  // =====================

  @Function
  @Comment("清理HEX中空格/逗号/换行等分隔符")
  public String hex_stripSpaces(@Comment(name = "hex", value = "字符串") String hex) {
    if (hex == null) {
      return null;
    }
    return hex.replaceAll("[^0-9A-Fa-f]", "").toUpperCase();
  }

  @Function
  @Comment("校验是否为有效十六进制且长度为偶数")
  public Boolean hex_isValid(@Comment(name = "hex", value = "字符串") String hex) {
    if (StrUtil.isBlank(hex)) {
      return false;
    }
    String cleaned = hex_stripSpaces(hex);
    return cleaned.length() % 2 == 0;
  }

  @Function
  @Comment("指定位宽大小端转换（每word长度2/4/8字节）")
  public String hex_swapEndian(
      @Comment(name = "hex", value = "十六进制") String hex,
      @Comment(name = "wordBytes", value = "位宽字节：2/4/8") Integer wordBytes) {
    if (!hex_isValid(hex) || wordBytes == null || wordBytes <= 0) {
      return null;
    }
    String cleaned = hex_stripSpaces(hex);
    if (cleaned.length() % (wordBytes * 2) != 0) {
      return null;
    }
    StringBuilder out = new StringBuilder(cleaned.length());
    for (int i = 0; i < cleaned.length(); i += wordBytes * 2) {
      String word = cleaned.substring(i, i + wordBytes * 2);
      out.append(hex_reverse(word));
    }
    return out.toString();
  }

  // =====================
  // 645 DATA 编码/解码与长度/CS
  // =====================

  @Function
  @Comment("645数据区编码：每字节+0x33")
  public String hex_645DataEncode(@Comment(name = "hex", value = "十六进制") String hex) {
    return hex_add33(hex);
  }

  @Function
  @Comment("645数据区解码：每字节-0x33")
  public String hex_645DataDecode(@Comment(name = "hex", value = "十六进制") String hex) {
    return hex_sub33(hex);
  }

  @Function
  @Comment("645长度字段计算：返回两位HEX")
  public String hex_645MakeLen(@Comment(name = "dataHex", value = "数据区HEX") String dataHex) {
    if (!hex_isValid(dataHex)) {
      return null;
    }
    int bytes = hex_stripSpaces(dataHex).length() / 2;
    return String.format("%02X", bytes & 0xFF);
  }

  @Function
  @Comment("645 CS计算：对不含CS的报文求和取低8位，两位HEX")
  public String hex_645MakeCs(@Comment(name = "noCsHex", value = "HEX不含CS") String noCsHex) {
    if (!hex_isValid(noCsHex)) {
      return null;
    }
    int sum = 0;
    String s = hex_stripSpaces(noCsHex);
    for (int i = 0; i < s.length(); i += 2) {
      sum += Integer.parseInt(s.substring(i, i + 2), 16);
    }
    return String.format("%02X", sum & 0xFF);
  }

  // =====================
  // BCD 编解码（电能/电压/电流等数值）
  // =====================

  @Function
  @Comment("BCD(十六进制)转十进制字符串，带小数位scale")
  public String hex_bcdToDec(
      @Comment(name = "hex", value = "BCD编码HEX") String hex,
      @Comment(name = "scale", value = "小数位数") Integer scale) {
    if (!hex_isValid(hex)) {
      return null;
    }
    String s = hex_stripSpaces(hex);
    StringBuilder digits = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      digits.append(c);
    }
    String val = digits.toString().replaceFirst("^0+", "");
    if (StrUtil.isEmpty(val)) {
      val = "0";
    }
    int sc = (scale == null || scale < 0) ? 0 : scale;
    if (sc == 0) {
      return val;
    }
    if (val.length() <= sc) {
      return "0." + StrUtil.padPre(val, sc, '0');
    }
    return val.substring(0, val.length() - sc) + "." + val.substring(val.length() - sc);
  }

  @Function
  @Comment("十进制字符串转BCD(HEX)，指定字节数与小数位scale")
  public String hex_decToBcd(
      @Comment(name = "value", value = "十进制字符串") String value,
      @Comment(name = "bytes", value = "目标字节数") Integer bytes,
      @Comment(name = "scale", value = "小数位数") Integer scale) {
    if (StrUtil.isBlank(value) || bytes == null || bytes <= 0) {
      return null;
    }
    int sc = (scale == null || scale < 0) ? 0 : scale;
    String v = value.trim();
    int dot = v.indexOf(".");
    if (dot >= 0) {
      String intPart = v.substring(0, dot).replaceFirst("^0+", "");
      String fracPart = v.substring(dot + 1);
      if (fracPart.length() > sc) {
        fracPart = fracPart.substring(0, sc);
      } else {
        fracPart = StrUtil.padAfter(fracPart, sc, '0');
      }
      v = (StrUtil.isEmpty(intPart) ? "0" : intPart) + fracPart;
    } else if (sc > 0) {
      v = v + StrUtil.repeat("0", sc);
    }
    v = v.replaceFirst("^0+", "");
    if (StrUtil.isEmpty(v)) {
      v = "0";
    }
    // 需要的BCD字符数 = bytes*2
    String padded = StrUtil.padPre(v, bytes * 2, '0');
    // 每两位组成一个字节的HEX（直接返回数字字符，不再做交换）
    return padded.toUpperCase();
  }

  // =====================
  // 645 组帧/解帧
  // =====================

  @Function
  @Comment("构建645读帧：68 + addr(12) + 68 + ctrl + len + data(可选编码) + cs + 16")
  public String hex_645Build(
      @Comment(name = "addr", value = "地址HEX(12位)") String addrHex,
      @Comment(name = "ctrl", value = "控制码 两位HEX") String ctrl,
      @Comment(name = "data", value = "数据区HEX(未编码)") String dataHex,
      @Comment(name = "encode33", value = "数据区是否+0x33") Boolean encode33) {
    if (StrUtil.isBlank(addrHex) || addrHex.length() != 12 || StrUtil.isBlank(ctrl)) {
      return null;
    }
    String data = StrUtil.nullToEmpty(dataHex);
    if (!StrUtil.isEmpty(data) && !hex_isValid(data)) {
      return null;
    }
    String enc =
        StrUtil.isEmpty(data)
            ? ""
            : (Boolean.TRUE.equals(encode33) ? hex_add33(data) : data.toUpperCase());
    String len = hex_645MakeLen(enc);
    String noCs = "68" + addrHex.toUpperCase() + "68" + ctrl.toUpperCase() + len + enc;
    String cs = hex_645MakeCs(noCs);
    return noCs + cs + "16";
  }

  @Function
  @Comment("645严谨解帧并验CS：返回json{ok,addr,ctrl,len,data,rawData,cs,calcCs}")
  public String hex_645Parse(@Comment(name = "frame", value = "完整帧HEX") String frame) {
    JSONObject obj = new JSONObject();
    if (!hex_isValid(frame)) {
      obj.set("ok", false);
      return JSONUtil.toJsonStr(obj);
    }
    String f = hex_stripSpaces(frame).toUpperCase();
    try {
      if (!f.startsWith("68") || !f.endsWith("16")) {
        obj.set("ok", false);
        return JSONUtil.toJsonStr(obj);
      }
      // 68 + 12(addr) + 68 + 1(ctrl) + 1(len) + data + 1(cs) + 16
      String addr = f.substring(2, 14);
      String head2 = f.substring(14, 16);
      if (!"68".equals(head2)) {
        obj.set("ok", false);
        return JSONUtil.toJsonStr(obj);
      }
      String ctrl = f.substring(16, 18);
      String len = f.substring(18, 20);
      int dataBytes = Integer.parseInt(len, 16);
      int dataStart = 20;
      int dataEnd = dataStart + dataBytes * 2;
      if (dataEnd + 2 > f.length() - 2) {
        obj.set("ok", false);
        return JSONUtil.toJsonStr(obj);
      }
      String data = f.substring(dataStart, dataEnd);
      String cs = f.substring(dataEnd, dataEnd + 2);
      String calc = hex_645MakeCs(f.substring(0, dataEnd));
      obj.set("ok", StrUtil.equalsIgnoreCase(cs, calc));
      obj.set("addr", addr);
      obj.set("ctrl", ctrl);
      obj.set("len", len);
      obj.set("data", data);
      obj.set("rawData", hex_sub33(data));
      obj.set("cs", cs);
      obj.set("calcCs", calc);
      return JSONUtil.toJsonStr(obj);
    } catch (Exception e) {
      obj.set("ok", false);
      return JSONUtil.toJsonStr(obj);
    }
  }

  // =====================
  // 定点数/整数 转 HEX（支持小端）
  // =====================

  @Function
  @Comment("十进制字符串转定点HEX（大端），bytes为输出总字节数，scale为小数位")
  public String hex_fromDecScaled(
      @Comment(name = "decimal", value = "十进制，如1.20") String decimal,
      @Comment(name = "scale", value = "小数位，例如3表示*1000") Integer scale,
      @Comment(name = "bytes", value = "输出字节数，例如4/2") Integer bytes) {
    if (StrUtil.isBlank(decimal) || scale == null || scale < 0 || bytes == null || bytes <= 0) {
      return null;
    }
    java.math.BigDecimal val;
    try {
      val = new java.math.BigDecimal(decimal.trim());
    } catch (Exception e) {
      return null;
    }
    java.math.BigDecimal factor = java.math.BigDecimal.TEN.pow(scale);
    java.math.BigDecimal scaled = val.multiply(factor).setScale(0, java.math.RoundingMode.HALF_UP);
    if (scaled.compareTo(java.math.BigDecimal.ZERO) < 0) {
      return null;
    }
    java.math.BigInteger bi = scaled.toBigInteger();
    String hex = bi.toString(16).toUpperCase();
    // 按字节长度左侧补0
    int targetLen = bytes * 2;
    if (hex.length() > targetLen) {
      // 超出位宽，截断高位（协议通常不允许，此处返回null更安全）
      return null;
    }
    return StrUtil.padPre(hex, targetLen, '0');
  }

  @Function
  @Comment("十进制字符串转定点HEX（小端，按字节反转），bytes为输出总字节数，scale为小数位")
  public String hex_fromDecScaledLE(
      @Comment(name = "decimal", value = "十进制，如1.20") String decimal,
      @Comment(name = "scale", value = "小数位，例如3表示*1000") Integer scale,
      @Comment(name = "bytes", value = "输出字节数，例如4/2") Integer bytes) {
    String be = hex_fromDecScaled(decimal, scale, bytes);
    if (be == null) {
      return null;
    }
    return hex_reverse(be);
  }

  @Function
  @Comment("整数转HEX（大端），bytes为输出总字节数")
  public String hex_fromInt(
      @Comment(name = "value", value = "整数") Integer value,
      @Comment(name = "bytes", value = "输出字节数") Integer bytes) {
    if (value == null || value < 0 || bytes == null || bytes <= 0) {
      return null;
    }
    String hex = Integer.toHexString(value).toUpperCase();
    int targetLen = bytes * 2;
    if (hex.length() > targetLen) {
      return null;
    }
    return StrUtil.padPre(hex, targetLen, '0');
  }

  @Function
  @Comment("整数转HEX（小端，按字节反转），bytes为输出总字节数")
  public String hex_fromIntLE(
      @Comment(name = "value", value = "整数") Integer value,
      @Comment(name = "bytes", value = "输出字节数") Integer bytes) {
    String be = hex_fromInt(value, bytes);
    if (be == null) {
      return null;
    }
    return hex_reverse(be);
  }

  @Function
  @Comment("确保HEX为偶数长度（左侧补0）")
  public String hex_even(@Comment(name = "hex", value = "HEX字符串") String hex) {
    if (hex == null) {
      return null;
    }
    String cleaned = hex.replaceAll("[^0-9A-Fa-f]", "").toUpperCase();
    return (cleaned.length() % 2 == 0) ? cleaned : ("0" + cleaned);
  }

  @Function
  @Comment("十进制字符串(不带小数缩放)转HEX并左侧补0到N字节。例如: '2.45' -> 去点 '245' -> 十进制245 -> HEX 00F5(2字节)")
  public String hex_fromDecNoScalePad(
      @Comment(name = "decimal", value = "十进制字符串，可含小数点，如2.45") String decimal,
      @Comment(name = "bytes", value = "输出总字节数，例如4") Integer bytes) {
    if (StrUtil.isBlank(decimal) || bytes == null || bytes <= 0) {
      return null;
    }
    String cleaned = decimal.trim();
    // 去除小数点，不做缩放，直接把所有数字拼起来作为十进制整数
    cleaned = cleaned.replace(".", "");
    if (!cleaned.matches("^[0-9]+$")) {
      return null;
    }
    java.math.BigInteger bi = new java.math.BigInteger(cleaned);
    String hex = bi.toString(16).toUpperCase();
    int targetLen = bytes * 2;
    if (hex.length() > targetLen) {
      return null;
    }
    return StrUtil.padPre(hex, targetLen, '0');
  }

  @Function
  @Comment("HEX左侧补0到N字节长度（不改变内容）。例如: 'F5' + 2字节 => '00F5'")
  public String hex_padToBytes(
      @Comment(name = "hex", value = "HEX字符串") String hex,
      @Comment(name = "bytes", value = "目标字节数") Integer bytes) {
    if (hex == null || bytes == null || bytes <= 0) {
      return null;
    }
    String cleaned = hex.replaceAll("[^0-9A-Fa-f]", "").toUpperCase();
    if (cleaned.length() % 2 != 0) {
      cleaned = "0" + cleaned;
    }
    int targetLen = bytes * 2;
    if (cleaned.length() > targetLen) {
      return null;
    }
    return StrUtil.padPre(cleaned, targetLen, '0');
  }

  @Function
  @Comment("HEX按字节大小端转换（每2位反转顺序）。等同hex_reverse，单独暴露以便magic-api调用链更直观")
  public String hex_swapEndianBytes(@Comment(name = "hex", value = "HEX字符串") String hex) {
    return hex_reverse(hex);
  }
}
