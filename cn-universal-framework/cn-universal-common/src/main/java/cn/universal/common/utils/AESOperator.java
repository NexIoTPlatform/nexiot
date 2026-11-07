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

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;

/** AES CBC加密 */
@Slf4j
public class AESOperator {
  /*
   * 加密用的Key 可以用26个字母和数字组成 此处使用AES-128-CBC加密模式，key需要为16位。
   */
  // private String VECTOR = "!WFNZFU_{H%M(S|a";
  // private String KEY = "1234567812345678";
  // private String VECTOR = "1234567812345678";

  private AESOperator() {}

  public static AESOperator getInstance() {
    return Nested.instance;
  }

  // 于内部静态类只会被加载一次，故该实现方式时线程安全的！
  static class Nested {

    private static AESOperator instance = new AESOperator();
  }

  /**
   * 加密
   *
   * @param content 加密消息体
   * @param appId APPID
   * @param appSecret APPSECRET
   */
  public String encrypt(String content, String appId, String appSecret) {
    try {
      SecretKeySpec skeySpec = new SecretKeySpec(appSecret.getBytes("UTF-8"), "AES");
      IvParameterSpec iv = new IvParameterSpec(appId.getBytes()); // 使用CBC模式，需要一个向量iv，可增加加密算法的强度
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
      byte[] encrypted = cipher.doFinal(content.getBytes("UTF-8"));
      return Base64.getEncoder().encodeToString(encrypted); // 此处使用BASE64做转码。
    } catch (Exception e) {
      log.error("aes加密失败");
    }
    return "";
  }

  /**
   * 解密
   *
   * @param content 解密消息体
   * @param appId APPID
   * @param appSecret APPSECRET
   */
  public String decrypt(String content, String appId, String appSecret) {
    try {
      SecretKeySpec skeySpec = new SecretKeySpec(appSecret.getBytes("UTF-8"), "AES");
      IvParameterSpec iv = new IvParameterSpec(appId.getBytes());
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
      byte[] encrypted1 = Base64.getDecoder().decode(content); // 先用base64解密
      byte[] original = cipher.doFinal(encrypted1);
      String originalString = new String(original, "UTF-8");
      return originalString;
    } catch (Exception ex) {
      log.error("aes加密失败", ex);
      return null;
    }
  }
}
