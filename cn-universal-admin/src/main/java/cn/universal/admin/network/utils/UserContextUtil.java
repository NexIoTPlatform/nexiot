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

package cn.universal.admin.network.utils;

import cn.universal.persistence.entity.IoTUser;
import cn.universal.security.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 用户上下文工具类 用于获取当前登录用户信息 @Author gitee.com/NexIoT */
public class UserContextUtil {

  private static final Logger log = LoggerFactory.getLogger(UserContextUtil.class);

  /**
   * 获取当前登录用户的 unionId 如果无法获取用户信息，返回默认值 "system"
   *
   * @return 当前用户的 unionId，如果获取失败则返回 "system"
   */
  public static String getCurrentUserUnionId() {
    try {
      IoTUser currentUser = SecurityUtils.getIoTUnionUser();
      if (currentUser != null && currentUser.getUnionId() != null) {
        return currentUser.getUnionId();
      }
    } catch (Exception e) {
      log.warn("获取当前用户信息失败，使用默认用户: {}", e.getMessage());
    }
    return "system";
  }

  /**
   * 获取当前登录用户信息 如果无法获取用户信息，返回 null
   *
   * @return 当前用户信息，如果获取失败则返回 null
   */
  public static IoTUser getCurrentUser() {
    try {
      return SecurityUtils.getIoTUnionUser();
    } catch (Exception e) {
      log.warn("获取当前用户信息失败: {}", e.getMessage());
      return null;
    }
  }
}
