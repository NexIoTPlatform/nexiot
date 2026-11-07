/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.config;

import cn.hutool.core.util.StrUtil;
import cn.universal.common.exception.IoTException;
import cn.universal.miniapp.entity.MpApp;
import cn.universal.miniapp.mapper.MpAppMapper;
import cn.universal.miniapp.security.MpAppIdContextHolder;
import cn.universal.miniapp.vo.MpUserInfoVO;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.mapper.IoTUserApplicationMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 小程序用户上下文工具类 提供获取当前登录用户、appId、unionId等通用方法
 *
 * @author NexIoT
 * @since 2025/12/18
 */
@Slf4j
@Component
public class MpUserContext {

  private static MpAppMapper mpAppMapper;
  private static IoTUserApplicationMapper ioTUserApplicationMapper;

  @Autowired
  public void setMpAppMapper(MpAppMapper mapper) {
    MpUserContext.mpAppMapper = mapper;
  }

  @Autowired
  public void setIoTUserApplicationMapper(IoTUserApplicationMapper mapper) {
    MpUserContext.ioTUserApplicationMapper = mapper;
  }

  /** 获取当前小程序用户信息 从请求属性中获取（由MpAuthenticationFilter设置） */
  public static MpUserInfoVO getCurrentUserInfo() {
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes == null) {
        return null;
      }
      HttpServletRequest request = attributes.getRequest();
      Object userInfo = request.getAttribute("mpUserInfo");
      if (userInfo instanceof MpUserInfoVO) {
        return (MpUserInfoVO) userInfo;
      }
    } catch (Exception e) {
      log.warn("获取小程序用户信息失败: {}", e.getMessage());
    }
    return null;
  }

  /** 获取当前用户（适配IoTUser接口，用于场景联动等需要IoTUser的场景） 注意：这里构建一个简化的IoTUser对象，主要用于权限检查 */
  public static IoTUser getCurrentUser() {
    MpUserInfoVO userInfo = getCurrentUserInfo();
    if (userInfo == null) {
      return null;
    }

    // 获取unionId
    String unionId = getUnionIdByAppId(userInfo.getAppId());
    // 构建IoTUser对象
    IoTUser user = new IoTUser();
    user.setUnionId(unionId);
    // 小程序用户默认不是管理员
    user.setIdentity(1);
    return user;
  }

  /** 获取当前appId */
  public static String getAppId() {
    // 优先从用户信息获取
    MpUserInfoVO userInfo = getCurrentUserInfo();
    if (userInfo != null && StrUtil.isNotBlank(userInfo.getAppId())) {
      return userInfo.getAppId();
    }
    // 回退到上下文持有者
    return MpAppIdContextHolder.getAppId();
  }

  /** 获取当前unionId 通过 appId -> mpApp.linkedAppId -> IoTUserApplication.unionId 获取 */
  public static String getUnionId() {
    String appId = getAppId();
    if (StrUtil.isBlank(appId)) {
      return null;
    }
    return getUnionIdByAppId(appId);
  }

  /**
   * 根据appId获取unionId
   *
   * <p>获取路径: appId -> mp_app.linked_app_id -> iot_user_application.union_id
   *
   * @param appId 小程序AppId
   * @return 关联的IoT应用unionId
   */
  public static String getUnionIdByAppId(String appId) {
    if (StrUtil.isBlank(appId)) {
      log.warn("appId为空，无法获取unionId");
      return null;
    }

    try {
      // 1. 获取小程序信息
      MpApp mpApp = mpAppMapper.selectByAppId(appId);
      if (mpApp == null) {
        log.warn("小程序不存在: appId={}", appId);
        return null;
      }

      // 2. 获取关联的IoT应用ID
      String linkedAppId = mpApp.getLinkedAppId();
      if (StrUtil.isBlank(linkedAppId)) {
        log.warn("小程序未关联IoT应用: appId={}", appId);
        return null;
      }

      // 3. 获取IoT应用信息
      IoTUserApplication application =
          ioTUserApplicationMapper.selectIotUserApplicationById(linkedAppId);
      if (application == null) {
        log.warn("IoT应用不存在: linkedAppId={}", linkedAppId);
        return null;
      }

      // 4. 返回unionId
      String unionId = application.getUnionId();
      if (StrUtil.isBlank(unionId)) {
        log.warn("IoT应用unionId为空: linkedAppId={}", linkedAppId);
        return null;
      }

      return unionId;
    } catch (Exception e) {
      log.error("获取unionId失败: appId={}", appId, e);
      return null;
    }
  }

  /** 获取unionId（必须存在，否则抛异常） */
  public static String requireUnionId() {
    String unionId = getUnionId();
    if (StrUtil.isBlank(unionId)) {
      throw new IoTException("无法获取应用授权信息，请检查小程序配置");
    }
    return unionId;
  }

  /** 获取关联的IoT应用ID (linkedAppId) */
  public static String getLinkedAppId() {
    String appId = getAppId();
    if (StrUtil.isBlank(appId)) {
      return null;
    }

    try {
      MpApp mpApp = mpAppMapper.selectByAppId(appId);
      if (mpApp != null) {
        return mpApp.getLinkedAppId();
      }
    } catch (Exception e) {
      log.error("获取linkedAppId失败: appId={}", appId, e);
    }
    return null;
  }

  /** 检查当前用户是否已登录 */
  public static boolean isLoggedIn() {
    return getCurrentUserInfo() != null;
  }

  /** 断言用户已登录 */
  public static void assertLogin() {
    if (!isLoggedIn()) {
      throw new IoTException("用户未登录");
    }
  }

  /** 断言用户已登录并返回用户信息 */
  public static MpUserInfoVO requireCurrentUser() {
    MpUserInfoVO userInfo = getCurrentUserInfo();
    if (userInfo == null) {
      throw new IoTException("用户未登录");
    }
    return userInfo;
  }
}
