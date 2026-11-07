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

package cn.universal.common.utils;

public class BleLockCryptoNet {

  public static int ENCRPYT_TYPE_NO = 0;
  public static int ENCRPYT_TYPE_DEFAULT = 1;
  public static int ENCRPYT_TYPE_DYNAMIC = 2;
  public static int ENCRPYT_TYPE_REPLAY = 3;

  private static final byte[] staticKey = {
    48, 101, 54, 100, 100, 54, 99, 98, 98, 57, 56, 57, 97, 98, 54, 48
  };
  private static final byte[] rePlayKey = {
    101, 99, 101, 53, 97, 100, 98, 53, 100, 100, 100, 49, 101, 53, 102, 57
  };

  private int encryptType = ENCRPYT_TYPE_REPLAY + 1;
  private String sn;
  private String verify;
  private int utc = 0;

  private final int SN_LEN = 20;
  private final int ID_CODE_LEN = 8;

  private static int gindex = 1;
  private static int gdayOld = 0;

  private static final int Nk = 4;
  private static final int Nb = 4;
  private static final int KEYLEN = 16;
  private static final int Nr = 10;
  private byte[] RoundKey = new byte[176];
  private byte[] Key = new byte[16];
  private byte[][] state = new byte[4][4];

  private final byte[] tmpeKeyStaticCode = {
    48, 101, 54, 100, 100, 54, 99, 98, 98, 57, 56, 57, 97, 98, 54, 48
  };

  private static short crc_ta[] = {
    0, 4129, 8258, 12387, 16516, 20645, 24774, 28903, -32504, -28375, -24246, -20117, -15988,
    -11859, -7730, -3601, 4657, 528, 12915, 8786, 21173, 17044, 29431, 25302, -27847, -31976,
    -19589, -23718, -11331, -15460, -3073, -7202, 9314, 13379, 1056, 5121, 25830, 29895, 17572,
    21637, -23190, -19125, -31448, -27383, -6674, -2609, -14932, -10867, 13907, 9842, 5649, 1584,
    30423, 26358, 22165, 18100, -18597, -22662, -26855, -30920, -2081, -6146, -10339, -14404, 18628,
    22757, 26758, 30887, 2112, 6241, 10242, 14371, -13876, -9747, -5746, -1617, -30392, -26263,
    -22262, -18133, 23285, 19156, 31415, 27286, 6769, 2640, 14899, 10770, -9219, -13348, -1089,
    -5218, -25735, -29864, -17605, -21734, 27814, 31879, 19684, 23749, 11298, 15363, 3168, 7233,
    -4690, -625, -12820, -8755, -21206, -17141, -29336, -25271, 32407, 28342, 24277, 20212, 15891,
    11826, 7761, 3696, -97, -4162, -8227, -12292, -16613, -20678, -24743, -28808, -28280, -32343,
    -20022, -24085, -12020, -16083, -3762, -7825, 4224, 161, 12482, 8419, 20484, 16421, 28742,
    24679, -31815, -27752, -23557, -19494, -15555, -11492, -7297, -3234, 689, 4752, 8947, 13010,
    16949, 21012, 25207, 29270, -18966, -23093, -27224, -31351, -2706, -6833, -10964, -15091, 13538,
    9411, 5280, 1153, 29798, 25671, 21540, 17413, -22565, -18438, -30823, -26696, -6305, -2178,
    -14563, -10436, 9939, 14066, 1681, 5808, 26199, 30326, 17941, 22068, -9908, -13971, -1778,
    -5841, -26168, -30231, -18038, -22101, 22596, 18533, 30726, 26663, 6336, 2273, 14466, 10403,
    -13443, -9380, -5313, -1250, -29703, -25640, -21573, -17510, 19061, 23124, 27191, 31254, 2801,
    6864, 10931, 14994, -722, -4849, -8852, -12979, -16982, -21109, -25112, -29239, 31782, 27655,
    23652, 19525, 15522, 11395, 7392, 3265, -4321, -194, -12451, -8324, -20581, -16454, -28711,
    -24584, 28183, 32310, 20053, 24180, 11923, 16050, 3793, 7920,
  };

  private static BleLockCryptoNet bleLockCryptoNet = new BleLockCryptoNet();

  public static BleLockCryptoNet getInstance() {
    bleLockCryptoNet.setSn(null);
    bleLockCryptoNet.setUtc(0);
    bleLockCryptoNet.setVerify(null);
    return bleLockCryptoNet;
  }

  private byte[] aesEncrypt(byte[] data, byte[] key) {
    return aes128EcbEncrypt(data, key);
  }

  private byte[] aesDecrypt(byte[] data, byte[] key) {
    return aes128EcbDecrypt(data, key);
  }

  private static final byte sbox[] = {
    99, 124, 119, 123, -14, 107, 111, -59, 48, 1, 103, 43, -2, -41, -85, 118,
    -54, -126, -55, 125, -6, 89, 71, -16, -83, -44, -94, -81, -100, -92, 114, -64,
    -73, -3, -109, 38, 54, 63, -9, -52, 52, -91, -27, -15, 113, -40, 49, 21,
    4, -57, 35, -61, 24, -106, 5, -102, 7, 18, -128, -30, -21, 39, -78, 117,
    9, -125, 44, 26, 27, 110, 90, -96, 82, 59, -42, -77, 41, -29, 47, -124,
    83, -47, 0, -19, 32, -4, -79, 91, 106, -53, -66, 57, 74, 76, 88, -49,
    -48, -17, -86, -5, 67, 77, 51, -123, 69, -7, 2, 127, 80, 60, -97, -88,
    81, -93, 64, -113, -110, -99, 56, -11, -68, -74, -38, 33, 16, -1, -13, -46,
    -51, 12, 19, -20, 95, -105, 68, 23, -60, -89, 126, 61, 100, 93, 25, 115,
    96, -127, 79, -36, 34, 42, -112, -120, 70, -18, -72, 20, -34, 94, 11, -37,
    -32, 50, 58, 10, 73, 6, 36, 92, -62, -45, -84, 98, -111, -107, -28, 121,
    -25, -56, 55, 109, -115, -43, 78, -87, 108, 86, -12, -22, 101, 122, -82, 8,
    -70, 120, 37, 46, 28, -90, -76, -58, -24, -35, 116, 31, 75, -67, -117, -118,
    112, 62, -75, 102, 72, 3, -10, 14, 97, 53, 87, -71, -122, -63, 29, -98,
    -31, -8, -104, 17, 105, -39, -114, -108, -101, 30, -121, -23, -50, 85, 40, -33,
    -116, -95, -119, 13, -65, -26, 66, 104, 65, -103, 45, 15, -80, 84, -69, 22
  };

  private static final byte rsbox[] = {
    82, 9, 106, -43, 48, 54, -91, 56, -65, 64, -93, -98, -127, -13, -41, -5,
    124, -29, 57, -126, -101, 47, -1, -121, 52, -114, 67, 68, -60, -34, -23, -53,
    84, 123, -108, 50, -90, -62, 35, 61, -18, 76, -107, 11, 66, -6, -61, 78,
    8, 46, -95, 102, 40, -39, 36, -78, 118, 91, -94, 73, 109, -117, -47, 37,
    114, -8, -10, 100, -122, 104, -104, 22, -44, -92, 92, -52, 93, 101, -74, -110,
    108, 112, 72, 80, -3, -19, -71, -38, 94, 21, 70, 87, -89, -115, -99, -124,
    -112, -40, -85, 0, -116, -68, -45, 10, -9, -28, 88, 5, -72, -77, 69, 6,
    -48, 44, 30, -113, -54, 63, 15, 2, -63, -81, -67, 3, 1, 19, -118, 107,
    58, -111, 17, 65, 79, 103, -36, -22, -105, -14, -49, -50, -16, -76, -26, 115,
    -106, -84, 116, 34, -25, -83, 53, -123, -30, -7, 55, -24, 28, 117, -33, 110,
    71, -15, 26, 113, 29, 41, -59, -119, 111, -73, 98, 14, -86, 24, -66, 27,
    -4, 86, 62, 75, -58, -46, 121, 32, -102, -37, -64, -2, 120, -51, 90, -12,
    31, -35, -88, 51, -120, 7, -57, 49, -79, 18, 16, 89, 39, -128, -20, 95,
    96, 81, 127, -87, 25, -75, 74, 13, 45, -27, 122, -97, -109, -55, -100, -17,
    -96, -32, 59, 77, -82, 42, -11, -80, -56, -21, -69, 60, -125, 83, -103, 97,
    23, 43, 4, 126, -70, 119, -42, 38, -31, 105, 20, 99, 85, 33, 12, 125
  };

  private static final byte Rcon[] = {
    -115, 1, 2, 4, 8, 16, 32, 64, -128, 27, 54, 108, -40, -85, 77, -102,
    47, 94, -68, 99, -58, -105, 53, 106, -44, -77, 125, -6, -17, -59, -111, 57,
    114, -28, -45, -67, 97, -62, -97, 37, 74, -108, 51, 102, -52, -125, 29, 58,
    116, -24, -53, -115, 1, 2, 4, 8, 16, 32, 64, -128, 27, 54, 108, -40,
    -85, 77, -102, 47, 94, -68, 99, -58, -105, 53, 106, -44, -77, 125, -6, -17,
    -59, -111, 57, 114, -28, -45, -67, 97, -62, -97, 37, 74, -108, 51, 102, -52,
    -125, 29, 58, 116, -24, -53, -115, 1, 2, 4, 8, 16, 32, 64, -128, 27,
    54, 108, -40, -85, 77, -102, 47, 94, -68, 99, -58, -105, 53, 106, -44, -77,
    125, -6, -17, -59, -111, 57, 114, -28, -45, -67, 97, -62, -97, 37, 74, -108,
    51, 102, -52, -125, 29, 58, 116, -24, -53, -115, 1, 2, 4, 8, 16, 32,
    64, -128, 27, 54, 108, -40, -85, 77, -102, 47, 94, -68, 99, -58, -105, 53,
    106, -44, -77, 125, -6, -17, -59, -111, 57, 114, -28, -45, -67, 97, -62, -97,
    37, 74, -108, 51, 102, -52, -125, 29, 58, 116, -24, -53, -115, 1, 2, 4,
    8, 16, 32, 64, -128, 27, 54, 108, -40, -85, 77, -102, 47, 94, -68, 99,
    -58, -105, 53, 106, -44, -77, 125, -6, -17, -59, -111, 57, 114, -28, -45, -67,
    97, -62, -97, 37, 74, -108, 51, 102, -52, -125, 29, 58, 116, -24, -53
  };

  private byte getSBoxValue(char num) {
    return this.sbox[num];
  }

  private byte getSBoxInvert(char num) {
    return this.rsbox[num];
  }

  private void KeyExpansion() {
    int i, j, k;
    char[] tempa = new char[4];

    for (i = 0; i < Nk; ++i) {
      RoundKey[(i * 4) + 0] = Key[(i * 4) + 0];
      RoundKey[(i * 4) + 1] = Key[(i * 4) + 1];
      RoundKey[(i * 4) + 2] = Key[(i * 4) + 2];
      RoundKey[(i * 4) + 3] = Key[(i * 4) + 3];
    }

    for (; (i < (Nb * (Nr + 1))); ++i) {
      for (j = 0; j < 4; ++j) {
        tempa[j] = (char) (RoundKey[(i - 1) * 4 + j] & 0x00ff);
      }
      if (i % Nk == 0) {

        {
          k = tempa[0];
          tempa[0] = tempa[1];
          tempa[1] = tempa[2];
          tempa[2] = tempa[3];
          tempa[3] = (char) (k & 0x00ff);
        }

        {
          tempa[0] = (char) getSBoxValue(tempa[0]);
          tempa[1] = (char) getSBoxValue(tempa[1]);
          tempa[2] = (char) getSBoxValue(tempa[2]);
          tempa[3] = (char) getSBoxValue(tempa[3]);
        }

        tempa[0] = (char) ((tempa[0] ^ Rcon[i / Nk]) & 0x00ff);
      } else if (Nk > 6 && i % Nk == 4) {
        {
          tempa[0] = (char) (getSBoxValue(tempa[0]) & 0x00ff);
          tempa[1] = (char) (getSBoxValue(tempa[1]) & 0x00ff);
          tempa[2] = (char) (getSBoxValue(tempa[2]) & 0x00ff);
          tempa[3] = (char) (getSBoxValue(tempa[3]) & 0x00ff);
        }
      }
      RoundKey[i * 4 + 0] = (byte) (RoundKey[(i - Nk) * 4 + 0] ^ tempa[0]);
      RoundKey[i * 4 + 1] = (byte) (RoundKey[(i - Nk) * 4 + 1] ^ tempa[1]);
      RoundKey[i * 4 + 2] = (byte) (RoundKey[(i - Nk) * 4 + 2] ^ tempa[2]);
      RoundKey[i * 4 + 3] = (byte) (RoundKey[(i - Nk) * 4 + 3] ^ tempa[3]);
    }
  }

  private void AddRoundKey(char round) {
    char i, j;
    for (i = 0; i < 4; ++i) {
      for (j = 0; j < 4; ++j) {
        state[i][j] ^= RoundKey[round * Nb * 4 + i * Nb + j];
      }
    }
  }

  private void SubBytes() {
    char i, j;
    for (i = 0; i < 4; ++i) {
      for (j = 0; j < 4; ++j) {
        state[j][i] = getSBoxValue((char) (state[j][i] & 0x00ff));
      }
    }
  }

  private void ShiftRows() {
    byte temp;

    temp = state[0][1];
    state[0][1] = state[1][1];
    state[1][1] = state[2][1];
    state[2][1] = state[3][1];
    state[3][1] = temp;

    temp = state[0][2];
    state[0][2] = state[2][2];
    state[2][2] = temp;

    temp = state[1][2];
    state[1][2] = state[3][2];
    state[3][2] = temp;

    temp = state[0][3];
    state[0][3] = state[3][3];
    state[3][3] = state[2][3];
    state[2][3] = state[1][3];
    state[1][3] = temp;
  }

  private byte xtime(byte x) {
    return (byte) ((x << 1) ^ (((x >> 7) & 1) * 0x1b));
  }

  private void MixColumns() {
    byte i;
    byte Tmp, Tm, t;
    for (i = 0; i < 4; ++i) {
      t = state[i][0];
      Tmp = (byte) (state[i][0] ^ state[i][1] ^ state[i][2] ^ state[i][3]);
      Tm = (byte) (state[i][0] ^ state[i][1]);
      Tm = xtime(Tm);
      state[i][0] ^= Tm ^ Tmp;
      Tm = (byte) (state[i][1] ^ state[i][2]);
      Tm = xtime(Tm);
      state[i][1] ^= Tm ^ Tmp;
      Tm = (byte) (state[i][2] ^ state[i][3]);
      Tm = xtime(Tm);
      state[i][2] ^= Tm ^ Tmp;
      Tm = (byte) (state[i][3] ^ t);
      Tm = xtime(Tm);
      state[i][3] ^= Tm ^ Tmp;
    }
  }

  private byte Multiply(byte x, byte y) {
    return (byte)
        (((y & 1) * x)
            ^ ((y >> 1 & 1) * xtime(x))
            ^ ((y >> 2 & 1) * xtime(xtime(x)))
            ^ ((y >> 3 & 1) * xtime(xtime(xtime(x))))
            ^ ((y >> 4 & 1) * xtime(xtime(xtime(xtime(x))))));
  }

  private void InvMixColumns() {
    byte i;
    byte a, b, c, d;
    for (i = 0; i < 4; ++i) {
      a = state[i][0];
      b = state[i][1];
      c = state[i][2];
      d = state[i][3];

      state[i][0] =
          (byte)
              (Multiply(a, (byte) 0x0e)
                  ^ Multiply(b, (byte) 0x0b)
                  ^ Multiply(c, (byte) 0x0d)
                  ^ Multiply(d, (byte) 0x09));
      state[i][1] =
          (byte)
              (Multiply(a, (byte) 0x09)
                  ^ Multiply(b, (byte) 0x0e)
                  ^ Multiply(c, (byte) 0x0b)
                  ^ Multiply(d, (byte) 0x0d));
      state[i][2] =
          (byte)
              (Multiply(a, (byte) 0x0d)
                  ^ Multiply(b, (byte) 0x09)
                  ^ Multiply(c, (byte) 0x0e)
                  ^ Multiply(d, (byte) 0x0b));
      state[i][3] =
          (byte)
              (Multiply(a, (byte) 0x0b)
                  ^ Multiply(b, (byte) 0x0d)
                  ^ Multiply(c, (byte) 0x09)
                  ^ Multiply(d, (byte) 0x0e));
    }
  }

  private void InvSubBytes() {
    char i, j;
    for (i = 0; i < 4; ++i) {
      for (j = 0; j < 4; ++j) {
        state[j][i] = getSBoxInvert((char) (state[j][i] & 0x00ff));
      }
    }
  }

  private void InvShiftRows() {
    byte temp;

    temp = state[3][1];
    state[3][1] = state[2][1];
    state[2][1] = state[1][1];
    state[1][1] = state[0][1];
    state[0][1] = temp;

    temp = state[0][2];
    state[0][2] = state[2][2];
    state[2][2] = temp;

    temp = state[1][2];
    state[1][2] = state[3][2];
    state[3][2] = temp;

    temp = state[0][3];
    state[0][3] = state[1][3];
    state[1][3] = state[2][3];
    state[2][3] = state[3][3];
    state[3][3] = temp;
  }

  private void Cipher() {
    char round = 0;

    AddRoundKey((char) 0);

    for (round = 1; round < Nr; ++round) {
      SubBytes();
      ShiftRows();
      MixColumns();
      AddRoundKey(round);
    }

    SubBytes();
    ShiftRows();
    AddRoundKey((char) Nr);
  }

  private void InvCipher() {
    char round = 0;

    AddRoundKey((char) Nr);

    for (round = Nr - 1; round > 0; round--) {
      InvShiftRows();
      InvSubBytes();
      AddRoundKey(round);
      InvMixColumns();
    }

    InvShiftRows();
    InvSubBytes();
    AddRoundKey((char) 0);
  }

  private void BlockCopy(byte[] output, byte[] input) {
    char i;
    for (i = 0; i < KEYLEN; ++i) {
      output[i] = input[i];
    }
  }

  private byte[] aes128EcbEncrypt(byte[] input, byte[] key) {
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        state[i][j] = input[i * 4 + j];
      }
    }
    byte[] encryptData = new byte[16];

    BlockCopy(this.Key, key);
    KeyExpansion();
    Cipher();

    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        encryptData[i * 4 + j] = state[i][j];
      }
    }

    return encryptData;
  }

  private byte[] aes128EcbDecrypt(byte[] input, byte[] key) {
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        state[i][j] = input[i * 4 + j];
      }
    }
    BlockCopy(Key, key);
    KeyExpansion();
    InvCipher();
    byte[] decryptData = new byte[16];

    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        decryptData[i * 4 + j] = state[i][j];
      }
    }

    return decryptData;
  }

  private byte[] createReplay(int utc) {
    int i;
    byte[] ret = new byte[4];
    for (i = 0; i < 4; i++) {
      ret[i] = (byte) (rePlayKey[i] ^ (byte) (utc >> (i * 8)));
    }
    return ret;
  }

  private int getUtcFromReplay(byte[] rePlayData) {

    return (rePlayData[0] ^ rePlayKey[0]) & 0x000000ff
        | ((rePlayData[1] ^ rePlayKey[1]) << 8) & 0x0000ff00
        | ((rePlayData[2] ^ rePlayKey[2]) << 16) & 0x00ff0000
        | ((rePlayData[3] ^ rePlayKey[3]) << 24) & 0xff000000;
  }

  private byte[] createDynamicKey(int utc, String sn, String idCode) {
    int i;
    byte[] snByte = sn.getBytes();
    byte[] idCodeByte = idCode.getBytes();
    byte[] key = new byte[16];
    for (i = 0; i < this.ID_CODE_LEN; i++) {
      key[i] = (byte) (snByte[i + 8] ^ idCodeByte[i]);
    }
    for (; i < this.ID_CODE_LEN + 4; i++) {
      key[i] = snByte[i + 8];
    }
    for (; i < 16; i++) {
      key[i] = (byte) ((utc >> (((i - 12) * 8))));
    }
    return key;
  }

  private byte[] lockEncrypt(byte[] data) throws Exception {

    byte[] headEncrypt = new byte[5];
    int headEncryptLen;
    byte[] encryptKey;

    headEncrypt[0] = (byte) data.length;
    if (encryptType == ENCRPYT_TYPE_NO) {
      return data;
    } else if (encryptType == ENCRPYT_TYPE_DEFAULT) {
      encryptKey = new byte[16];
      System.arraycopy(staticKey, 0, encryptKey, 0, 16);
      headEncryptLen = 1;
    } else if (encryptType == ENCRPYT_TYPE_DYNAMIC) {
      if (sn.length() != SN_LEN) {
        throw new Exception("error sn length");
      }
      if (verify.length() != ID_CODE_LEN) {
        throw new Exception("error id code length");
      }
      System.arraycopy(createReplay(utc), 0, headEncrypt, 1, 4);
      encryptKey = createDynamicKey(utc, sn, verify);
      headEncryptLen = 5;

    } else if (encryptType == ENCRPYT_TYPE_REPLAY) {
      byte[] ret = new byte[5 + data.length];
      ret[0] = headEncrypt[0];
      System.arraycopy(createReplay(utc), 0, ret, 1, 4);
      System.arraycopy(data, 0, ret, 5, data.length);
      return ret;

    } else {
      throw new Exception("error encrypt type");
    }
    byte[] encryptData = new byte[(data.length + 15) / 16 * 16];
    byte[] ret = new byte[headEncryptLen + encryptData.length];
    byte[] temp = new byte[16];
    System.arraycopy(data, 0, encryptData, 0, data.length);
    System.arraycopy(headEncrypt, 0, ret, 0, headEncryptLen);
    for (int i = 0; i < encryptData.length; ) {
      System.arraycopy(encryptData, i, temp, 0, 16);
      System.arraycopy(aesEncrypt(temp, encryptKey), 0, ret, i + headEncryptLen, 16);
      i += 16;
    }
    return ret;
  }

  private byte[] lockDecrypt(byte[] data) throws Exception {
    int headLen;
    int decryptLen;
    byte[] decryptKey;

    decryptLen = data[0];
    if (encryptType == ENCRPYT_TYPE_NO) {
      return data;
    } else if (encryptType == ENCRPYT_TYPE_DEFAULT) {
      if (1 != data.length % 16) {
        throw new Exception("error decrypt length");
      }
      decryptLen = data.length - 1;

      decryptKey = new byte[16];
      System.arraycopy(staticKey, 0, decryptKey, 0, 16);
      headLen = 1;

    } else if (encryptType == ENCRPYT_TYPE_DYNAMIC) {
      if (5 != data.length % 16) {
        throw new Exception("error decrypt length");
      }
      decryptLen = data.length - 5;

      if (verify.length() != ID_CODE_LEN) {
        throw new Exception("error id code length");
      }
      if (sn.length() != SN_LEN) {
        throw new Exception("error sn length");
      }
      byte[] rePlayData = new byte[4];
      System.arraycopy(data, 1, rePlayData, 0, 4);
      int encryptUtc = getUtcFromReplay(rePlayData);
      if (Math.abs(encryptUtc - utc) > 10) {
        throw new Exception("error utc");
      }
      decryptKey = createDynamicKey(encryptUtc, sn, verify);
      headLen = 5;

    } else if (encryptType == ENCRPYT_TYPE_REPLAY) {
      if (5 + data[0] != data.length) {
        throw new Exception("error decrypt length");
      }

      byte[] rePlayData = new byte[4];
      System.arraycopy(data, 1, rePlayData, 0, 4);
      int encryptUtc = getUtcFromReplay(rePlayData);
      if (Math.abs(encryptUtc - utc) > 10) {
        throw new Exception("error utc");
      }
      byte[] ret = new byte[data.length - 5];
      System.arraycopy(data, 5, ret, 0, data.length - 5);
      return ret;

    } else {
      throw new Exception("error encrypt type");
    }
    byte[] decryptData = new byte[decryptLen];
    byte[] temp = new byte[16];

    for (int i = 0; i < decryptLen; ) {
      System.arraycopy(data, i + headLen, temp, 0, 16);
      System.arraycopy(aesDecrypt(temp, decryptKey), 0, decryptData, i, 16);
      i += 16;
    }
    return decryptData;
  }

  private byte[] lockRfidEncrypt(byte[] id) throws Exception {
    if (id.length > 16) {
      throw new Exception("error id length");
    }
    byte[] tempData = new byte[16];
    System.arraycopy(id, 0, tempData, 0, id.length);
    byte[] encryptData = aesEncrypt(tempData, staticKey);
    byte[] ret = new byte[6];
    System.arraycopy(encryptData, 10, ret, 0, 6);
    return ret;
  }

  private int lockRCR16(byte[] data) {
    short u16crc;
    char u8da;
    int temp;
    u16crc = 0;
    for (int i = 0; i < data.length; i++) {
      temp = u16crc & 0x0000ffff;
      u8da = (char) (temp / 256);
      u16crc <<= 8;
      u16crc ^= crc_ta[(u8da ^ data[i]) & 0x000000ff];
    }

    temp = (~u16crc) & 0x0000ffff;
    return temp;
  }

  private long lockRand(int seed) {
    int next = seed;
    int result;

    next *= 1103515245;
    next += 12345;
    result = (next / 65536) % 2048;

    next *= 1103515245;
    next += 12345;
    result <<= 10;
    result ^= (next / 65536) % 1024;

    next *= 1103515245;
    next += 12345;
    result <<= 10;
    long temp = next & 0x00000000ffffffffL;
    temp = ((temp / 65536) % 1024) ^ result;
    return temp;
  }

  private String lockFillPasswdPrefix(byte[] cryptedStr, int iCryLen) {
    int j = 1;
    int seed = 0;
    byte[] passwdTbl = "0123456789".getBytes();

    for (int i = 0; i < iCryLen; i++) {
      if (100000 == j) {
        j = 1;
      }
      seed += (cryptedStr[i]) * j;
      j = j * 10;
    }
    long randRet = lockRand(seed);

    int passwdTblLen = passwdTbl.length;
    byte[] ret = new byte[2];
    ret[0] = passwdTbl[(int) (randRet % passwdTblLen)];
    randRet /= passwdTblLen;
    ret[1] = passwdTbl[(int) (randRet % passwdTblLen)];
    return String.format("%c%c", ret[0], ret[1]);
  }

  private String lockFillPasswdPostfix(byte[] cryptedStr, int iCryLen) {
    int crc;
    crc = lockRCR16(cryptedStr);

    String str = String.format("%04x", crc);
    byte[] strByte = str.getBytes();
    for (int i = 0; i < 4; i++) {
      if (strByte[i] >= 'a' && strByte[i] <= 'z') {
        strByte[i] = (byte) (strByte[i] - 'a' + '0');
      }
    }

    return String.format("%c%c%c%c", strByte[0], strByte[1], strByte[2], strByte[3]);
  }

  private String lockCreateOneTimePasswdForSlock(int utc, String sn) throws Exception {
    long day = (utc / (24 * 60 * 60));
    long timeCal;

    if (gdayOld != day) {
      gindex = 1;
    }
    timeCal = day * 24 * 60 * 60 + gindex * 60 * 60 + gindex * 29 + gindex;
    String str = String.format("%08x", timeCal) + sn;
    String pre = lockFillPasswdPrefix(str.getBytes(), 28);
    String pos = lockFillPasswdPostfix(str.getBytes(), 28);
    return pre + pos;
  }

  private String lockCreateOneTimePasswd(int index, int utc, String sn) throws Exception {
    long day = (utc / (24 * 60 * 60));
    int tempIndex = index + 1;
    long timeCal = day * 24 * 60 * 60 + tempIndex * 60 * 60 + tempIndex * 29 + tempIndex;
    String str = String.format("%08x", timeCal) + sn;
    String pre = lockFillPasswdPrefix(str.getBytes(), 28);
    String pos = lockFillPasswdPostfix(str.getBytes(), 28);
    return pre + pos;
  }

  private String lockCreateTempKey(int effectiveTime, String sn, int utc, String verify)
      throws Exception {

    byte[] aesKey = new byte[16];
    byte[] snByte = sn.getBytes();
    byte[] idCodeByte = verify.getBytes();
    String retString;
    int utcHours = (utc / 3600); /* utc小时数 */
    if (effectiveTime > 24 || effectiveTime < 1) {
      throw new Exception("error sn effectiveTime");
    }
    byte utcLastSixBit = (byte) (utcHours & 0x0000003f);
    System.out.print("utcLastSixBit=" + utcLastSixBit);
    System.out.print(" \n");

    // effectiveTime +=50;    /* 去掉该条件，java无无符号数据类型，数据太大可能会导致数据溢出出错 */
    int i;
    for (i = 0; i < 8; i++) {
      aesKey[i] = (byte) (snByte[i + 8] ^ idCodeByte[i]);
    }

    for (; i < 8 + 4; i++) {
      aesKey[i] = (byte) (snByte[i + 8] ^ (utcHours >> (24 - ((i - 8) * 8))));
    }

    for (; i < 8 + 4 + 4; i++) {
      aesKey[i] = (byte) (idCodeByte[i - 12] ^ (utcHours >> (24 - ((i - 12) * 8))));
    }

    byte[] staticCode = new byte[16];
    System.arraycopy(tmpeKeyStaticCode, 0, staticCode, 0, 16);
    staticCode[14] += utcLastSixBit;
    staticCode[15] += (byte) effectiveTime;
    System.out.format("staticCode[15] = %x", staticCode[15]);
    System.out.print(" \n");

    byte[] encrypt = aes128EcbEncrypt(staticCode, aesKey);
    for (int ii = 0; ii < encrypt.length; ii++) {
      System.out.format("%x", encrypt[ii]);
      System.out.print(" ");
    }
    System.out.print(" \n");

    long lastEncrypt_12 = (encrypt[12] & 0x00000000000000ff);
    long lastEncrypt_13 = (encrypt[13] & 0x00000000000000ff);
    long lastEncrypt_14 = (encrypt[14] & 0x00000000000000ff);
    long lastEncrypt_15 = (encrypt[15] & 0x00000000000000ff);
    long lastEncrypt =
        lastEncrypt_12 << 24
            | lastEncrypt_13 << 16
            | lastEncrypt_14 << 8
            | lastEncrypt_15; // 先单独与并用long变量保存，否则会把数值变成负数，然后移位进行异或；
    // long  lastEncrypt = ( encrypt[12] & 0x00000000000000ff) <<24 | ( encrypt[13] &
    // 0x00000000000000ff) <<16 | (
    // encrypt[14] & 0x00000000000000ff) <<8 | ( encrypt[15] & 0x00000000000000ff);

    System.out.format("lastEncrypt= %x", lastEncrypt);
    System.out.print(" \n");

    lastEncrypt = lastEncrypt % 100000000; /* 保证转成整数字符串最多只有8位*/

    retString = String.format("%02d%02d%08d", utcLastSixBit, effectiveTime, lastEncrypt);
    return retString;
  }

  public byte[] encrypt(byte[] data) throws Exception {
    return lockEncrypt(data);
  }

  public byte[] decrypt(byte[] data) throws Exception {
    return lockDecrypt(data);
  }

  public byte[] rfidEncrypt(byte[] id) throws Exception {
    return lockRfidEncrypt(id);
  }

  public String createOneTimePasswdForSlock(int utc, String sn) throws Exception {
    return lockCreateOneTimePasswdForSlock(utc, sn);
  }

  public String createOneTimePasswd(int index, int utc, String sn) throws Exception {
    return lockCreateOneTimePasswd(index, utc, sn);
  }

  public String createTempKey(int effectiveTime, String sn, int utc, String verify)
      throws Exception {
    return lockCreateTempKey(effectiveTime, sn, utc, verify);
  }

  public void setSn(String sn) {
    this.sn = sn;
  }

  public void setVerify(String verify) {
    this.verify = verify;
  }

  public void setEncryptType(int encryptType) {
    this.encryptType = encryptType;
  }

  public void setUtc(int utc) {
    this.utc = utc;
  }
}
