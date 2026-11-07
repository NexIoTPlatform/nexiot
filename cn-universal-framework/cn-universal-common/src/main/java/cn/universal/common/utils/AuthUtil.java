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

import cn.hutool.crypto.digest.MD5;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.log4j.Log4j2;

/**
 * @Author 🐤 gitee.com/NexIoT
 *
 * @email ✉ gitee.com/NexIoT@outlook.com
 * @since ⏰ 2020/1/7
 */
@Log4j2
public class AuthUtil {

  private static DateTimeFormatter z8Formatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).withZone(ZoneId.of("+8"));

  private static DateTimeFormatter secretDateFormatter =
      DateTimeFormatter.ofPattern("E, dd MMM yyyy HH:mm:ss O", Locale.ENGLISH)
          .withZone(ZoneId.of("+0"));

  public static String formatZ8DateTime(ZonedDateTime zonedDateTime) {
    return z8Formatter.format(zonedDateTime);
  }

  public static TemporalAccessor parseZ8DateTime(String z8DateTime) {
    return z8Formatter.parse(z8DateTime);
  }

  public static String hash(String z8DateTime, String appSecret, String msg) {
    String utcTime = secretDateFormatter.format(z8Formatter.parse(z8DateTime));
    String secret = MD5.create().digestHex(utcTime + appSecret);
    byte[] sha256 = hmacSha256(msg, secret);
    return Base64.getEncoder().encodeToString(sha256);
  }

  public static byte[] hmacSha256(String message, String secret) {
    byte[] hash = new byte[0];
    try {
      Mac sha256Hmac = Mac.getInstance("HmacSHA256");
      SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
      sha256Hmac.init(secret_key);
      hash = sha256Hmac.doFinal(message.getBytes());
    } catch (Exception e) {
      log.info("encrypt hmac sha256 error:{}", e.getMessage());
    }
    return hash;
  }
}
