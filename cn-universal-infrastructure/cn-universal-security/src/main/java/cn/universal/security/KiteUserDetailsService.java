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

package cn.universal.security;

import cn.hutool.core.util.StrUtil;
import cn.universal.common.constant.Constants;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.utils.ServletUtils;
import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.mapper.IoTTenantMapper;
import cn.universal.persistence.mapper.IoTUserMapper;
import cn.universal.security.service.AdminLogService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Slf4j
@Component(value = "kiteUserDetailsService")
public class KiteUserDetailsService implements UserDetailsService {

  @Resource private IoTUserMapper iotUserMapper;
  @Resource private IoTTenantMapper ioTTenantMapper;

  @Resource private AdminLogService adminLogService;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    log.info("usernameis:" + username);

    // 首先尝试通过 username 查找用户
    IoTUser iotUser = IoTUser.builder().username(username).status(IoTConstant.NORMAL).build();
    try {
      iotUser = iotUserMapper.selectOne(iotUser);

      // 如果通过 username 没找到，尝试通过 unionId 查找
      if (iotUser == null) {
        iotUser = IoTUser.builder().unionId(username).status(IoTConstant.NORMAL).build();
        iotUser = iotUserMapper.selectOne(iotUser);
      }

      if (iotUser == null) {
        HttpServletRequest request = ServletUtils.getRequest();
        adminLogService.recordLogininfor(username, Constants.LOGIN_FAIL, "用户名或密码错误", request);
        throw new UsernameNotFoundException("用户名或密码错误");
      } else if (IoTConstant.UN_NORMAL.equals(iotUser.getStatus())) {
        HttpServletRequest request = ServletUtils.getRequest();
        adminLogService.recordLogininfor(
            iotUser.getUnionId(), Constants.LOGIN_FAIL, "用户已冻结", request);
        throw new UsernameNotFoundException("用户名或密码错误");
      }
      validateTenant(iotUser);
      // 返回自定义的 KiteUserDetails
      List<SimpleGrantedAuthority> Authorities = new ArrayList<>();
      Authorities.add(new SimpleGrantedAuthority("WEB"));

      // 处理密码编码格式，避免 BCrypt 警告
      String password = iotUser.getPassword();
      // 如果密码不是以 BCrypt 格式开头，添加 {noop} 前缀
      if (password != null
          && !password.startsWith("$2a$")
          && !password.startsWith("$2b$")
          && !password.startsWith("$2y$")
          && !password.startsWith("{")) {
        password = "{noop}" + password;
      }

      // 使用 unionId 作为 User 的 username，这样在 JWT 的 sub 字段中就是 unionId
      User user = new User(iotUser.getUnionId(), password, Authorities);
      // 更新登录时间
      iotUserMapper.updateLoginDate(iotUser.getId());
      return user;
    } catch (Exception e) {
      log.error("加载用户详情失败: " + username, e);
      throw new UsernameNotFoundException("用户名或密码错误");
    }
  }

  private void validateTenant(IoTUser iotUser) {
    if (iotUser == null || iotUser.isAdmin()) {
      return;
    }
    String tenantId = StrUtil.blankToDefault(iotUser.getTenantId(), iotUser.getUnionId());
    if (StrUtil.isBlank(tenantId)) {
      return;
    }
    IoTTenant tenant = ioTTenantMapper.selectByPrimaryKey(tenantId);
    HttpServletRequest request = ServletUtils.getRequest();
    if (tenant == null || Integer.valueOf(1).equals(tenant.getDeleted())) {
      adminLogService.recordLogininfor(iotUser.getUnionId(), Constants.LOGIN_FAIL, "租户不存在", request);
      throw new UsernameNotFoundException("租户不存在或已删除");
    }
    if (Integer.valueOf(1).equals(tenant.getStatus())) {
      adminLogService.recordLogininfor(iotUser.getUnionId(), Constants.LOGIN_FAIL, "租户已停用", request);
      throw new UsernameNotFoundException("当前租户已停用");
    }
    if (tenant.getExpireTime() != null && tenant.getExpireTime().before(new Date())) {
      adminLogService.recordLogininfor(iotUser.getUnionId(), Constants.LOGIN_FAIL, "租户已过期", request);
      throw new UsernameNotFoundException("当前租户已过期");
    }
  }
}
