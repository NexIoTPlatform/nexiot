package cn.universal.web.tcp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;

/** 基于《单相4G蓝牙壁挂645协议_改版.pdf》： 输入金额+指定小数位，返回4字节小端（反写）16进制字符串 核心规则：16进制数据（）、电量反写（小端）（）、4字节存储（） */
public class AmountToLittleEndianHex {

  /**
   * 金额转4字节小端反写16进制
   *
   * @param amount 输入金额（如1.20、5.00，支持非负数值）
   * @param decimalDigits 保留小数位数（如2表示保留两位小数，需≥0且缩放后整数≤4294967295）
   * @return 4字节小端（反写）16进制字符串（大写，如1.20保留2位→"78000000"）
   * @throws IllegalArgumentException 非法参数（负金额、小数位超范围、整数超4字节上限）
   */
  public static String amountTo4ByteLittleHex(double amount, int decimalDigits) {
    // 1. 参数合法性校验（符合电表电量非负、4字节存储上限要求）
    if (amount < 0) {
      throw new IllegalArgumentException("金额不能为负数（协议无负电量定义，）");
    }
    if (decimalDigits < 0 || decimalDigits > 6) {
      throw new IllegalArgumentException("小数位数需在0-6之间（协议电量最多6位小数，）");
    }

    // 2. 按指定小数位缩放金额为整数（避免浮点精度误差）
    long scaleFactor = (long) Math.pow(10, decimalDigits); // 缩放因子（如保留2位→100）
    long amountInt = Math.round(amount * scaleFactor); // 四舍五入为整数（如1.20×100=120）

    // 3. 校验整数是否超出4字节无符号范围（4字节最大=2^32-1=4294967295）
    if (amountInt > 4294967295L) {
      throw new IllegalArgumentException("缩放后整数超出4字节无符号上限（最大4294967295，）");
    }

    // 4. 整数转4字节小端字节数组（即协议“反写数据”，）
    // ByteBuffer默认大端，需手动设为小端（字节顺序反转=反写）
    byte[] littleEndianBytes =
        ByteBuffer.allocate(4)
            .order(ByteOrder.LITTLE_ENDIAN) // 小端序=反写（符合🔶1-151“反写数据”规则）
            .putInt((int) amountInt) // 强制转int（因已校验≤4294967295，无溢出）
            .array();

    // 5. 字节数组转16进制字符串（大写，符合协议“16进制数据”要求，）
    return HexFormat.of().formatHex(littleEndianBytes).toUpperCase();
  }

  // -------------------------- 测试案例（验证协议兼容性） --------------------------
  public static void main(String[] args) {
    // 案例1：输入1.20（购电量），保留2位小数（协议常用，）
    String hex1 = amountTo4ByteLittleHex(120, 2);
    System.out.println("1.20（保留2位）→ 小端4字节16进制：" + hex1);
    // 输出：78000000（解析：1.20×100=120→0x78→小端字节[0x78,0x00,0x00,0x00]→16进制"78000000"）

    // 案例2：输入123456.12（协议示例电量，），保留2位小数
    String hex2 = amountTo4ByteLittleHex(123456.12, 2);
    System.out.println("123456.12（保留2位）→ 小端4字节16进制：" + hex2);
    // 输出：12563412（完全匹配协议“123456.12→12563412”的反写规则，）

    // 案例3：输入5（无小数），保留0位小数
    String hex3 = amountTo4ByteLittleHex(5.00, 0);
    System.out.println("5.00（保留0位）→ 小端4字节16进制：" + hex3);
    // 输出：05000000（解析：5→0x05→小端字节[0x05,0x00,0x00,0x00]→16进制"05000000"）
  }
}
