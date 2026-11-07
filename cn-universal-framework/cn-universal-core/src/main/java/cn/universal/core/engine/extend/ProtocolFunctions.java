/*
 * Protocol common helpers (CS checksum etc.) for Magic-API
 */
package cn.universal.core.engine.extend;

import cn.hutool.core.util.StrUtil;
import cn.universal.core.engine.annotation.Comment;
import cn.universal.core.engine.annotation.Function;
import org.springframework.stereotype.Component;

@Component
public class ProtocolFunctions implements IdeMagicFunction {

  // =====================
  // CS 校验（和校验）
  // 计算：对数据按字节求和后，对给定模数取余；常见为 256。
  // 返回两位大写HEX（低8位）。
  // =====================

  @Function
  @Comment("CS求和校验，按字节求和后取余mod（默认256），返回两位HEX")
  public String cs_check(
      @Comment(name = "hex", value = "十六进制字符串(不含CS)") String hex,
      @Comment(name = "mod", value = "模数(默认256)") Integer mod) {
    if (StrUtil.isBlank(hex) || hex.length() % 2 != 0) {
      return null;
    }
    int modulus = (mod == null || mod <= 0) ? 256 : mod;
    long sum = 0L;
    for (int i = 0; i < hex.length(); i += 2) {
      sum += Integer.parseInt(hex.substring(i, i + 2), 16);
    }
    int cs = (int) (sum % modulus);
    // 只返回低8位，两位大写HEX
    return String.format("%02X", cs & 0xFF);
  }

  @Function
  @Comment("CS校验验证，按字节求和后取余mod，与期望CS比较")
  public Boolean cs_verify(
      @Comment(name = "hex", value = "十六进制字符串(不含CS)") String hex,
      @Comment(name = "cs", value = "期望CS，两位HEX") String expectedCs,
      @Comment(name = "mod", value = "模数(默认256)") Integer mod) {
    if (StrUtil.isBlank(hex) || StrUtil.isBlank(expectedCs)) {
      return false;
    }
    String calc = cs_check(hex, mod);
    return StrUtil.equalsIgnoreCase(calc, expectedCs);
  }
}
