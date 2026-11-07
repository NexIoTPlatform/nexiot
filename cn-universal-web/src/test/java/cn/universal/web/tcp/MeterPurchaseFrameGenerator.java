package cn.universal.web.tcp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;

/**
 * 基于《单相4G蓝牙壁挂645协议_改版.pdf》实现购电帧生成 输入：充值金额（kWh，两位小数）、购电次数、电表地址（12位十六进制）
 * 输出：符合协议的购电16进制报文（如6800112233445568A00C3136343A...16）
 */
public class MeterPurchaseFrameGenerator {
  // 协议固定常量（）
  private static final byte START_FLAG = (byte) 0x68; // 起始符
  private static final byte END_FLAG = (byte) 0x16; // 结束符
  private static final byte CONTROL_CODE = (byte) 0xA0; // 控制码（数据操作）
  private static final byte[] PURCHASE_CMD_RAW = { // 蓝牙购电原始指令（）
    (byte) 0xFE, (byte) 0x03, (byte) 0x01, (byte) 0x07
  };

  /**
   * 生成购电帧
   *
   * @param chargeAmount 充值金额（如1.20 → 1.20kWh）
   * @param purchaseCount 购电次数（如2）
   * @param meterAddr 电表地址（12位十六进制字符串，如"001122334455"）
   * @return 购电帧16进制字符串（大写）
   * @throws IllegalArgumentException 参数非法时抛出
   */
  public static String generatePurchaseFrame(
      double chargeAmount, int purchaseCount, String meterAddr) {
    // 1. 参数校验（）
    if (chargeAmount < 0 || purchaseCount < 0) {
      throw new IllegalArgumentException("充值金额/购电次数不能为负数");
    }
    if (meterAddr == null || meterAddr.length() != 12) {
      throw new IllegalArgumentException("电表地址必须为12位十六进制字符串（如001122334455）");
    }

    // 2. 处理充值金额：金额×100（两位小数）→ 4字节小端数组 → 每个字节+0x33（）
    long amountInt = Math.round(chargeAmount * 100); // 避免浮点精度问题（如1.20→120）
    byte[] amountLittleBytes = intTo4ByteLittleEndian((int) amountInt); // 4字节小端数组
    byte[] amountData = add0x33ToBytes(amountLittleBytes); // 每个字节+0x33

    // 3. 处理购电次数：4字节小端数组 → 每个字节+0x33（）
    byte[] countLittleBytes = intTo4ByteLittleEndian(purchaseCount); // 4字节小端数组
    byte[] countData = add0x33ToBytes(countLittleBytes); // 每个字节+0x33

    // 4. 处理购电指令：原始指令每个字节+0x33（）
    byte[] cmdData = add0x33ToBytes(PURCHASE_CMD_RAW);

    // 5. 构造DATA段：指令(4字节)+金额(4字节)+次数(4字节) → 共12字节（）
    byte[] data = new byte[cmdData.length + amountData.length + countData.length];
    System.arraycopy(cmdData, 0, data, 0, cmdData.length);
    System.arraycopy(amountData, 0, data, cmdData.length, amountData.length);
    System.arraycopy(countData, 0, data, cmdData.length + amountData.length, countData.length);

    // 6. 构造帧头：68 + 电表地址 + 68 + 控制码 + LEN（）
    byte[] meterAddrBytes = HexFormat.of().parseHex(meterAddr); // 电表地址转字节数组（6字节）
    byte len = (byte) data.length; // LEN=DATA实际字节数（12→0x0C）
    byte[] header =
        new byte[1 + meterAddrBytes.length + 1 + 1 + 1]; // 68(1)+地址(6)+68(1)+控制码(1)+LEN(1)
    int headerIdx = 0;
    header[headerIdx++] = START_FLAG;
    System.arraycopy(meterAddrBytes, 0, header, headerIdx, meterAddrBytes.length);
    headerIdx += meterAddrBytes.length;
    header[headerIdx++] = START_FLAG;
    header[headerIdx++] = CONTROL_CODE;
    header[headerIdx++] = len;

    // 7. 计算CS校验位：header+data所有字节之和 mod256（）
    byte cs = calculateCS(header, data);

    // 8. 拼接完整帧：header + data + cs + 结束符（）
    byte[] fullFrame = new byte[header.length + data.length + 1 + 1];
    System.arraycopy(header, 0, fullFrame, 0, header.length);
    System.arraycopy(data, 0, fullFrame, header.length, data.length);
    fullFrame[header.length + data.length] = cs;
    fullFrame[header.length + data.length + 1] = END_FLAG;

    // 9. 转16进制字符串（大写）
    return HexFormat.of().formatHex(fullFrame).toUpperCase();
  }

  /**
   * 整数转4字节小端字节数组（反写规则）
   *
   * @param num 待转换整数（需≤2^32-1）
   * @return 4字节小端数组
   */
  private static byte[] intTo4ByteLittleEndian(int num) {
    return ByteBuffer.allocate(4)
        .order(ByteOrder.LITTLE_ENDIAN) // 小端序（反写）
        .putInt(num)
        .array();
  }

  /**
   * 字节数组每个字节+0x33（发送端规则，）
   *
   * @param bytes 原始字节数组
   * @return 每个字节+0x33后的数组（确保无符号，mod256）
   */
  private static byte[] add0x33ToBytes(byte[] bytes) {
    byte[] result = new byte[bytes.length];
    for (int i = 0; i < bytes.length; i++) {
      result[i] = (byte) ((bytes[i] & 0xFF) + 0x33); // 避免负数，先转无符号
    }
    return result;
  }

  /**
   * 计算CS校验位（）
   *
   * @param header 帧头字节数组
   * @param data DATA段字节数组
   * @return CS校验字节
   */
  private static byte calculateCS(byte[] header, byte[] data) {
    int sum = 0;
    // 累加帧头所有字节
    for (byte b : header) {
      sum += (b & 0xFF); // 无符号累加
    }
    // 累加DATA段所有字节
    for (byte b : data) {
      sum += (b & 0xFF);
    }
    return (byte) (sum % 256); // 对256取余
  }

  // 测试示例
  public static void main(String[] args) {
    // 输入：充值1.20kWh，购电次数2，电表地址001122334455
    String purchaseFrame = generatePurchaseFrame(120, 1, "250707ea8dda");
    System.out.println("购电帧：" + purchaseFrame);
    // 输出示例：6800112233445568A00C3136343A7800000035333333XX16（XX为CS校验位，随参数变化）
  }
}
