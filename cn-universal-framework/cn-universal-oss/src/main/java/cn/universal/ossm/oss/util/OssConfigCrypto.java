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

package cn.universal.ossm.oss.util;

import cn.hutool.core.util.StrUtil;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** OSS敏感配置加解密 */
public final class OssConfigCrypto {

  public static final String MASK = "******";
  private static final String PREFIX = "enc:";
  private static final int IV_LENGTH = 12;
  private static final int TAG_LENGTH = 128;
  private static final String DEFAULT_KEY = "nexiot-oss-config-key-2026";
  private static final SecureRandom RANDOM = new SecureRandom();

  private OssConfigCrypto() {}

  public static String encrypt(String plainText, String keySeed) {
    if (StrUtil.isBlank(plainText) || MASK.equals(plainText)) {
      return plainText;
    }
    try {
      byte[] iv = new byte[IV_LENGTH];
      RANDOM.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, buildKey(keySeed), new GCMParameterSpec(TAG_LENGTH, iv));
      byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
      ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
      buffer.put(iv);
      buffer.put(cipherText);
      return PREFIX + Base64.getEncoder().encodeToString(buffer.array());
    } catch (Exception e) {
      throw new IllegalStateException("OSS配置加密失败", e);
    }
  }

  public static String decrypt(String encrypted, String keySeed) {
    if (StrUtil.isBlank(encrypted) || !encrypted.startsWith(PREFIX)) {
      return encrypted;
    }
    try {
      byte[] data = Base64.getDecoder().decode(encrypted.substring(PREFIX.length()));
      ByteBuffer buffer = ByteBuffer.wrap(data);
      byte[] iv = new byte[IV_LENGTH];
      buffer.get(iv);
      byte[] cipherText = new byte[buffer.remaining()];
      buffer.get(cipherText);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, buildKey(keySeed), new GCMParameterSpec(TAG_LENGTH, iv));
      return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("OSS配置解密失败", e);
    }
  }

  public static boolean isMasked(String value) {
    return MASK.equals(value);
  }

  private static SecretKeySpec buildKey(String keySeed) throws Exception {
    String seed = StrUtil.blankToDefault(keySeed, DEFAULT_KEY);
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    return new SecretKeySpec(digest.digest(seed.getBytes(StandardCharsets.UTF_8)), "AES");
  }
}
