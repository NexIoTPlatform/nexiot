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

import cn.hutool.core.codec.Base64;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;

public class RSAUtils {

  /** 密钥长度 于原文长度对应 以及越长速度越慢 */
  private static final int KEY_SIZE = 1024;

  /** 用于封装随机产生的公钥与私钥 */
  public static Map<Integer, String> keyMap = new HashMap<Integer, String>();

  /** 随机生成密钥对 */
  public static void genKeyPair() throws Exception {
    // KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象
    KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
    // 初始化密钥对生成器
    keyPairGen.initialize(KEY_SIZE, new SecureRandom());
    // 生成一个密钥对，保存在keyPair中
    KeyPair keyPair = keyPairGen.generateKeyPair();
    // 得到私钥
    RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
    // 得到公钥
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    String publicKeyString = encryptBASE64(publicKey.getEncoded());
    // 得到私钥字符串
    String privateKeyString = encryptBASE64(privateKey.getEncoded());
    // 将公钥和私钥保存到Map
    // 0表示公钥
    keyMap.put(0, publicKeyString);
    // 1表示私钥
    keyMap.put(1, privateKeyString);
  }

  // 编码返回字符串
  public static String encryptBASE64(byte[] key) throws Exception {
    return Base64.encode(key);
  }

  // 解码返回byte
  public static byte[] decryptBASE64(String key) throws Exception {
    return Base64.decode(key);
  }

  /**
   * RSA公钥加密
   *
   * @param str 加密字符串
   * @param publicKey 公钥
   * @return 密文
   * @throws Exception 加密过程中的异常信息
   */
  public static String encrypt(String str, String publicKey) throws Exception {
    // base64编码的公钥
    byte[] decoded = decryptBASE64(publicKey);
    RSAPublicKey pubKey =
        (RSAPublicKey)
            KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    // RSA加密
    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.ENCRYPT_MODE, pubKey);
    String outStr = encryptBASE64(cipher.doFinal(str.getBytes("UTF-8")));
    return outStr;
  }

  /**
   * RSA私钥解密
   *
   * @param str 加密字符串
   * @param privateKey 私钥
   * @return 明文
   * @throws Exception 解密过程中的异常信息
   */
  public static String decrypt(String str, String privateKey) throws Exception {
    // 64位解码加密后的字符串
    byte[] inputByte = decryptBASE64(str);
    // base64编码的私钥
    byte[] decoded = decryptBASE64(privateKey);
    RSAPrivateKey priKey =
        (RSAPrivateKey)
            KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    // RSA解密
    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.DECRYPT_MODE, priKey);
    String outStr = new String(cipher.doFinal(inputByte));
    return outStr;
  }
}
