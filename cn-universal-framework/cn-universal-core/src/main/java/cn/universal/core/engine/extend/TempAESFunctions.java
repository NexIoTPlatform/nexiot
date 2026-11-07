package cn.universal.core.engine.extend;

import cn.universal.core.engine.annotation.Comment;
import cn.universal.core.engine.annotation.Function;
import java.io.ByteArrayOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2026/8/3 15:38
 */
@Component
public class TempAESFunctions implements IdeMagicFunction {

  private static byte[] msgcnt = new byte[] {0, 0, 0, 0};

  private class DIR_TYPE {
    public static final int DIR_UP = 0;
    public static final int DIR_DOWN = 1;
  }


  @Function
  @Comment("客户定制：checkMic")
  public byte[] aes_checkMic(byte[] app_key, byte[] payload, byte[] devaddr, byte[] msgid) {
    return checkMic(app_key, payload, DIR_TYPE.DIR_UP, devaddr, msgid, payload.length);
  }

  @Function
  @Comment("客户定制：decryptPayLoad")
  public byte[] aes_decryptPayLoad(byte[] nwk_skey, byte[] payload, byte[] devaddr, byte[] msgid) {
    return decrypt(nwk_skey, payload, devaddr, msgid);
  }

  @Function
  @Comment("客户定制：bytesToHex")
  public String aes_bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02X", b));
    }
    return sb.toString();
  }

  @Function
  @Comment("客户定制：encryptAndAddMic")
  public byte[] aes_encryptAndAddMic(
      String cmd, byte[] app_skey, byte[] app_key, byte[] devaddr, byte msgtp) {
    byte[] cmdPayload = cmd.getBytes();
    int cnt =
        (cmdPayload.length % 16 == 0 ? cmdPayload.length / 16 : (cmdPayload.length + 16) / 16);

    byte[] s = encryptFrmPayload(app_skey, DIR_TYPE.DIR_DOWN, devaddr, msgcnt, cnt);
    byte[] pld = new byte[cnt * 16];
    System.arraycopy(cmdPayload, 0, pld, 0, cmdPayload.length);
    byte[] encrypted = new byte[cmdPayload.length];
    for (int i = 0; i < cmdPayload.length; i++) {
      encrypted[i] = (byte) (pld[i] ^ s[i]);
    }

    byte[] mic = checkMic(app_key, encrypted, DIR_TYPE.DIR_DOWN, devaddr, msgcnt, encrypted.length);

    byte[] dest = new byte[12 + encrypted.length + mic.length];
    byte[] head = "JOYG".getBytes();
    System.arraycopy(head, 0, dest, 0, head.length);
    dest[4] = 0x00;
    System.arraycopy(msgcnt, 0, dest, 5, msgcnt.length);
    msgcnt = byteChange(msgcnt);
    int i = byteArr2Int(msgcnt);
    i++;
    msgcnt = int2ByteArr(i);
    msgcnt = byteChange(msgcnt);

    dest[9] = msgtp;
    byte[] en_len = new byte[2];
    en_len[0] = (byte) (encrypted.length & 0x00FF);
    en_len[1] = (byte) (encrypted.length & 0xFF00);
    System.arraycopy(en_len, 0, dest, 10, en_len.length);
    System.arraycopy(encrypted, 0, dest, 12, encrypted.length);
    System.arraycopy(mic, 0, dest, 12 + encrypted.length, mic.length);

    return dest;
  }

  @Function
  @Comment("客户定制：hexToBytes")
  public byte[] aes_hexToBytes(String hexStr) {
    return hexToBytes(hexStr);
  }

  public static byte[] hexToBytes(String hexStr) {
    try {
      if (hexStr == null || hexStr.isEmpty()) {
        return null;
      }
      if (hexStr.length() % 2 == 1) {
        hexStr = "0" + hexStr;
      }
      final String hex = hexStr.toUpperCase();
      int length = hex.length() / 2;
      char[] hexChars = hex.toCharArray();
      byte[] d = new byte[length];
      for (int i = 0; i < length; i++) {
        int pos = i * 2;
        d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
      }
      return d;
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
    return null;
  }

  private static byte[] encryptFrmPayload(
      final byte[] appsKey, int dir, final byte[] devAddr, final byte[] fCnt, int cnt) {
    byte[] result = new byte[16 * cnt];
    for (int i = 0; i < cnt; i++) {
      byte[] dest = new byte[16];
      dest[0] = (byte) 0x01;
      int pos = 1;
      byte[] src = hexToBytes(String.format("%08X", 0));
      System.arraycopy(src, 0, dest, pos, src.length);
      src = intToBytes(dir, 1);
      pos = 1 + 4;
      System.arraycopy(src, 0, dest, pos, src.length);
      pos = 1 + 4 + 1;
      System.arraycopy(devAddr, 0, dest, pos, devAddr.length);
      pos = 1 + 4 + 1 + 4;
      System.arraycopy(fCnt, 0, dest, pos, fCnt.length);
      src = intToBytes(0, 1);
      pos = 1 + 4 + 1 + 4 + 4;
      System.arraycopy(src, 0, dest, pos, src.length);
      src = intToBytes(i + 1, 1);
      pos = 1 + 4 + 1 + 4 + 4 + 1;
      System.arraycopy(src, 0, dest, pos, src.length);
      src = encryptAes(dest, appsKey);
      System.arraycopy(src, 0, result, i * 16, src.length);
    }
    return result;
  }

  private static byte[] encryptAes(byte[] input, byte[] key) {
    if (input == null || key == null) {
      return null;
    }
    return AESFunc.encryptAes128(input, key);
  }

  private static byte[] checkMic(
      final byte[] nwksKey,
      final byte[] msg,
      int dir,
      final byte[] devAddr,
      final byte[] fCnt,
      int len) {
    byte[] dest = new byte[16 + len];
    dest[0] = (byte) 0x49;
    int pos = 1;
    byte[] src = hexToBytes(String.format("%08X", 0));
    System.arraycopy(src, 0, dest, pos, src.length);
    src = intToBytes(dir, 1);
    pos = 1 + 4;
    System.arraycopy(src, 0, dest, pos, src.length);
    pos = 1 + 4 + 1;
    System.arraycopy(devAddr, 0, dest, pos, devAddr.length);
    pos = 1 + 4 + 1 + 4;
    System.arraycopy(fCnt, 0, dest, pos, fCnt.length);
    src = intToBytes(0, 1);
    pos = 1 + 4 + 1 + 4 + 4;
    System.arraycopy(src, 0, dest, pos, src.length);
    src = intToBytes(len, 1);
    pos = 1 + 4 + 1 + 4 + 4 + 1;
    System.arraycopy(src, 0, dest, pos, src.length);
    System.arraycopy(msg, 0, dest, 16, msg.length);
    return checkMic(nwksKey, dest);
  }

  private static byte[] checkMic(final byte[] key, final byte[] payload) {
    if (payload == null || payload.length <= 0) {
      return null;
    }
    if (key == null || key.length <= 0 || key.length % 16 != 0) {
      return null;
    }
    AESCMac.getInstance().doInit(key);
    AESCMac.getInstance().doUpdate(payload, 0, payload.length);
    byte[] b = AESCMac.getInstance().doFinal();
    if (b == null || b.length < 4) {
      return null;
    }
    byte[] result = new byte[4];
    System.arraycopy(b, 0, result, 0, result.length);
    return result;
  }

  private static byte[] decrypt(
      final byte[] appsKey, byte[] frmPayload, final byte[] devAddr, final byte[] fCnt) {
    return decrypt(appsKey, frmPayload, devAddr, fCnt, DIR_TYPE.DIR_UP);
  }

  private static byte[] decrypt(
      final byte[] appsKey, byte[] frmPayload, final byte[] devAddr, final byte[] fCnt, int dir) {
    int len = frmPayload.length;
    int cnt = (len % 16 == 0 ? len / 16 : (len + 16) / 16);
    byte[] s = encryptFrmPayload(appsKey, dir, devAddr, fCnt, cnt);
    byte[] pld = new byte[cnt * 16];
    System.arraycopy(frmPayload, 0, pld, 0, frmPayload.length);
    byte[] dest = new byte[frmPayload.length];
    for (int i = 0; i < frmPayload.length; i++) {
      dest[i] = (byte) (pld[i] ^ s[i]);
    }
    return dest;
  }

  private static class AESCMac {

    private static final byte[] CONST_ZERO = hexToBytes("00000000000000000000000000000000");
    private static final byte[] CONST_RB = hexToBytes("00000000000000000000000000000087");

    private byte[] contentKey;
    private ByteArrayOutputStream barros;

    private static AESCMac aesCmac = null;

    public AESCMac() {}

    public static AESCMac getInstance() {
      if (AESCMac.aesCmac == null) {
        AESCMac.aesCmac = new AESCMac();
      }
      return AESCMac.aesCmac;
    }

    public void doInit(byte[] contentKey) {
      this.contentKey = contentKey;
      barros = new ByteArrayOutputStream();
    }

    public void doUpdate(byte[] input, int offset, int len) {
      barros.write(input, offset, len);
    }

    public byte[] doFinal() {
      Object[] keys = generateSubKey(contentKey);
      byte[] K1 = (byte[]) keys[0];
      byte[] K2 = (byte[]) keys[1];

      byte[] input = barros.toByteArray();

      int numberOfRounds = (input.length + 15) / 16;
      boolean lastBlockComplete;

      if (numberOfRounds == 0) {
        numberOfRounds = 1;
        lastBlockComplete = false;
      } else {
        lastBlockComplete = input.length % 16 == 0;
      }

      byte[] M_last;
      int srcPos = 16 * (numberOfRounds - 1);

      if (lastBlockComplete) {
        byte[] partInput = new byte[16];
        System.arraycopy(input, srcPos, partInput, 0, 16);
        M_last = xor128(partInput, K1);
      } else {
        byte[] partInput = new byte[input.length % 16];
        System.arraycopy(input, srcPos, partInput, 0, input.length % 16);
        byte[] padded = doPadding(partInput);
        M_last = xor128(padded, K2);
      }

      byte[] X = CONST_ZERO.clone();
      byte[] partInput = new byte[16];
      byte[] Y;

      for (int i = 0; i < numberOfRounds - 1; i++) {
        srcPos = 16 * i;
        System.arraycopy(input, srcPos, partInput, 0, 16);
        Y = xor128(partInput, X);
        X = AESFunc.encryptAes128(Y, contentKey);
      }

      Y = xor128(X, M_last);
      X = AESFunc.encryptAes128(Y, contentKey);

      return X;
    }

    private byte[] doPadding(byte[] input) {
      byte[] padded = new byte[16];
      for (int j = 0; j < 16; j++) {
        if (j < input.length) {
          padded[j] = input[j];
        } else if (j == input.length) {
          padded[j] = (byte) 0x80;
        } else {
          padded[j] = (byte) 0x00;
        }
      }
      return padded;
    }

    private static Object[] generateSubKey(byte[] key) {
      byte[] L = AESFunc.encryptAes128(CONST_ZERO, key);

      byte[] K1 = null;
      if ((L[0] & 0x80) == 0) {
        K1 = doLeftShiftOneBit(L);
      } else {
        byte[] tmp = doLeftShiftOneBit(L);
        K1 = xor128(tmp, CONST_RB);
      }

      byte[] K2 = null;
      if ((K1[0] & 0x80) == 0) {
        K2 = doLeftShiftOneBit(K1);
      } else {
        byte[] tmp = doLeftShiftOneBit(K1);
        K2 = xor128(tmp, CONST_RB);
      }

      Object[] result = new Object[2];
      result[0] = K1;
      result[1] = K2;
      return result;
    }

    private static byte[] xor128(byte[] input1, byte[] input2) {
      byte[] output = new byte[input1.length];
      for (int i = 0; i < input1.length; i++) {
        output[i] = (byte) (((int) input1[i] ^ (int) input2[i]) & 0xFF);
      }
      return output;
    }

    private static byte[] doLeftShiftOneBit(byte[] input) {
      byte[] output = new byte[input.length];
      byte overflow = 0;
      for (int i = (input.length - 1); i >= 0; i--) {
        output[i] = (byte) ((int) input[i] << 1 & 0xFF);
        output[i] |= overflow;
        overflow = ((input[i] & 0x80) != 0) ? (byte) 1 : (byte) 0;
      }
      return output;
    }
  }

  private static class AESFunc {

    static final byte[] AESCBC_CONSTANT_IV = hexToBytes("0BA0F8DDFEA61FB3D8DF9F566A050F78");
    static final byte[] AES_H_CONSTANT = hexToBytes("2DC2DF39420321D0CEF1FE2374029D95");

    static final IvParameterSpec IV = new IvParameterSpec(AESCBC_CONSTANT_IV);

    static Cipher AESCBC = null;
    static Cipher AESECB = null;

    static {
      try {
        AESCBC = Cipher.getInstance("AES/CBC/NOPADDING");
        AESECB = Cipher.getInstance("AES/ECB/NOPADDING");
      } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
        e.printStackTrace();
      }
    }

    public static byte[] encryptAes128(byte[] input, byte[] key) {
      SecretKey AESKey = new SecretKeySpec(key, 0, 16, "AES");
      try {
        synchronized (AESECB) {
          AESECB.init(Cipher.ENCRYPT_MODE, AESKey);
          byte[] result = AESECB.doFinal(input);
          return result;
        }
      } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
        e.printStackTrace();
      }
      return null;
    }
  }

  private static int byteArr2Int(byte[] arr) {
    int m = (arr[0] & 0xff) << 24;
    int n = (arr[1] & 0xff) << 16;
    int k = (arr[2] & 0xff) << 8;
    int d = (arr[3] & 0xff) << 0;
    int i = m + n + k + d;
    return i;
  }

  private static byte[] int2ByteArr(int i) {
    byte[] arr = new byte[4];
    int m = (i >> 24) & 0xff;
    int n = (i >> 16) & 0xff;
    int k = (i >> 8) & 0xff;
    int d = i & 0xff;
    arr[0] = (byte) m;
    arr[1] = (byte) n;
    arr[2] = (byte) k;
    arr[3] = (byte) d;
    return arr;
  }

  private static byte[] byteChange(byte[] arr) {
    for (int i = 0; i < arr.length / 2; i++) {
      byte temp = arr[i];
      arr[i] = arr[arr.length - i - 1];
      arr[arr.length - i - 1] = temp;
    }
    return arr;
  }

  private static byte charToByte(char c) {
    return (byte) "0123456789ABCDEF".indexOf(c);
  }

  private static byte[] intToBytes(int value, int len) {
    byte[] bytes = new byte[len];
    int srcValue = value;
    for (int i = 0; i < len; i++) {
      int res = value >> (i * 8);
      if (i > 0 && (res == srcValue)) {
        break;
      }
      bytes[i] = (byte) (res & 0xff);
    }
    return bytes;
  }
}
