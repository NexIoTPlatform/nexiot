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
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @ClassName: AESUtil @Description 参数对称加密 @Author Administrator
 *
 * @since 2025/4/1 20:34
 */
public class AESUtil {

  // 参数分别代表 算法名称/加密模式/数据填充方式
  private static final String ALGORITHMSTR = "AES/ECB/PKCS5Padding";
  private static final String AES = "AES";

  /**
   * AES加密
   *
   * @param data 加密串
   * @param aesKey
   * @return
   * @throws Exception
   */
  public static String encrypt(String data, String aesKey) {
    try {
      KeyGenerator kg = KeyGenerator.getInstance(AES);
      kg.init(128); // 秘钥长度
      Cipher cipher = Cipher.getInstance(ALGORITHMSTR);
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey.getBytes(), AES));
      byte[] b = cipher.doFinal(data.getBytes("UTF-8"));
      // 采用base64算法进行转码,避免出现中文乱码
      return Base64.encode(b);
    } catch (Exception e) {
      throw new RuntimeException("加密失败");
    }
  }

  /**
   * 解密
   *
   * @param data 解密的字符串
   * @param aesKey 解密的key值
   * @return
   * @throws Exception
   */
  public static String decrypt(String data, String aesKey) {
    try {
      KeyGenerator kgen = KeyGenerator.getInstance(AES);
      kgen.init(128);
      Cipher cipher = Cipher.getInstance(ALGORITHMSTR);
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey.getBytes(), AES));
      // 采用base64算法进行转码,避免出现中文乱码
      byte[] encryptBytes = Base64.decode(data);
      byte[] decryptBytes = cipher.doFinal(encryptBytes);
      return new String(decryptBytes);
    } catch (Exception e) {
      throw new RuntimeException("业务参数解密失败");
    }
  }

  /** 空格转+号 */
  public static String SpaceTurnPlus(String str) {
    char[] chars = str.toCharArray();
    StringBuffer returnStr = new StringBuffer();
    for (char s : chars) {
      if (s == ' ') {
        returnStr = returnStr.append("+");
      } else {
        returnStr = returnStr.append(s);
      }
    }
    return returnStr.toString();
  }

  public static void main(String[] args) throws Exception {}

  // 加密
  public static String Encrypt(String content, String key) throws Exception {
    byte[] raw = key.getBytes("utf-8");
    SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding"); // "算法/模式/补码方式"
    // 使用CBC模式，需要一个向量iv，可增加加密算法的强度
    IvParameterSpec ips = new IvParameterSpec(key.substring(16).getBytes());
    cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ips);
    byte[] encrypted = cipher.doFinal(content.getBytes());
    return cn.hutool.core.codec.Base64.encode(encrypted);
  }

  // 解密
  public static String Decrypt(String content, String key) {
    try {
      byte[] raw = key.getBytes("utf-8");
      SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      IvParameterSpec ips = new IvParameterSpec(key.substring(16).getBytes());
      cipher.init(Cipher.DECRYPT_MODE, skeySpec, ips);
      // 采用base64算法进行转码,避免出现中文乱码
      byte[] encryptBytes = Base64.decode(content);
      byte[] decryptBytes = cipher.doFinal(encryptBytes);
      return new String(decryptBytes);
      //			byte[] encrypted1 = new BASE64Decoder().decodeBuffer(content);
      //			try {
      //				byte[] original = cipher.doFinal(encrypted1);
      //				String originalString = new String(original);
      //				return originalString;
      //			} catch (Exception e) {
      //				System.out.println(e.toString());
      //				return null;
      //			}
    } catch (Exception ex) {
      System.out.println(ex.toString());
      return null;
    }
  }
}
