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

package cn.universal.security.service;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import cn.universal.common.constant.Constants;
import cn.universal.common.utils.IPUtils;
import cn.universal.common.utils.LocationUtils;
import cn.universal.common.utils.ServletUtils;
import cn.universal.persistence.entity.SysLogininfor;
import cn.universal.persistence.entity.SysOperLog;
import cn.universal.persistence.mapper.SysLogininforMapper;
import cn.universal.persistence.mapper.SysOperLogMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 日志记录 */
@Slf4j
@Component
public class AdminLogService {

  @Resource private SysLogininforMapper sysLogininforMapper;

  @Resource private SysOperLogMapper sysOperLogMapper;

  /**
   * 记录登录信息
   *
   * @param username 已识别用户使用 unionId，与登录日志查询一致；未识别用户使用输入账号
   * @param status 状态
   * @param message 消息
   * @param request HTTP请求对象，可以为null
   */
  public void recordLogininfor(
      final String username,
      final String status,
      final String message,
      final HttpServletRequest request) {
    HttpServletRequest httpRequest = request;
    // 如果传入的request为null，尝试从RequestContextHolder获取
    if (httpRequest == null) {
      try {
        httpRequest = ServletUtils.getRequest();
      } catch (Exception e) {
        log.warn("无法获取HttpServletRequest，使用默认值记录登录信息: {}", e.getMessage());
        // 创建默认的登录信息记录
        recordLogininforWithDefaults(username, status, message);
        return;
      }
    }

    // 使用获取到的request参数
    final UserAgent userAgent = UserAgentUtil.parse(httpRequest.getHeader("User-Agent"));
    final String ip = IPUtils.getIpAddr(httpRequest);
    // 操作系统
    String os = userAgent == null ? "unknown" : userAgent.getOs().getName();
    // 浏览器
    String browser = userAgent == null ? "unknown" : userAgent.getBrowser().getName();
    // 根据IP地址获取地理位置
    String location = LocationUtils.getLocationByIp(ip);

    SysLogininfor login = new SysLogininfor();
    login.setUserName(username);
    login.setIpaddr(ip);
    login.setLoginLocation(location);
    login.setBrowser(browser);
    login.setOs(os);
    login.setMsg(message);
    // 日志状态
    if (Constants.LOGIN_SUCCESS.equals(status) || Constants.LOGOUT.equals(status)) {
      login.setStatus(Constants.SUCCESS);
    } else if (Constants.LOGIN_FAIL.equals(status)) {
      login.setStatus(Constants.FAIL);
    }
    login.setLoginTime(new Date());
    // 插入数据
    sysLogininforMapper.insertSelective(login);
  }

  /**
   * 使用默认值记录登录信息（用于无法获取HttpServletRequest的情况）
   *
   * @param username 用户名
   * @param status 状态
   * @param message 消息
   */
  private void recordLogininforWithDefaults(
      final String username, final String status, final String message) {
    // 创建默认的登录信息记录
    SysLogininfor logininfor = new SysLogininfor();
    logininfor.setUserName(username);
    logininfor.setIpaddr("unknown");
    logininfor.setLoginLocation("未知");
    logininfor.setBrowser("unknown");
    logininfor.setOs("unknown");
    logininfor.setMsg(message);
    // 日志状态
    if (Constants.LOGIN_SUCCESS.equals(status) || Constants.LOGOUT.equals(status)) {
      logininfor.setStatus(Constants.SUCCESS);
    } else if (Constants.LOGIN_FAIL.equals(status)) {
      logininfor.setStatus(Constants.FAIL);
    }
    // 插入数据
    logininfor.setLoginTime(new Date());
    sysLogininforMapper.insertSelective(logininfor);
  }

  /**
   * 操作日志记录
   *
   * @param operLog 操作日志信息
   */
  public void recordOper(final SysOperLog operLog) {
    // 远程查询操作地点
    operLog.setOperTime(new Date());
    sysOperLogMapper.insertSelective(operLog);
  }
}
