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

package cn.universal.admin.system.web;

import cn.hutool.core.util.StrUtil;
import cn.universal.admin.system.permission.SysPermissionService;
import cn.universal.admin.system.service.ISysMenuService;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.domain.R;
import cn.universal.persistence.entity.IoTTenant;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.mapper.IoTTenantMapper;
import cn.universal.persistence.entity.admin.SysMenu;
import cn.universal.persistence.entity.admin.vo.RouterVo;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.utils.SecurityUtils;
import io.netty.util.internal.StringUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 登录验证 @Author ruoyi */
@RestController
@RequestMapping("/admin")
@Tag(name = "系统用户管理", description = "系统用户信息管理接口")
public class SysUserController {

  @Resource private IoTUserService ioTUserService;

  @Resource private ISysMenuService menuService;

  @Resource private SysPermissionService permissionService;
  @Resource private StringRedisTemplate stringRedisTemplate;
  @Resource private IoTTenantMapper ioTTenantMapper;

  /**
   * 获取用户信息
   *
   * @return 用户信息
   */
  @Operation(summary = "获取用户信息", description = "获取当前登录用户的详细信息，包括角色和权限")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取用户信息",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @GetMapping("getInfo")
  public R getInfo(HttpServletRequest request) {
    final String unionId = SecurityUtils.getUnionId();
    IoTUser iotUser = ioTUserService.selectUserByUnionId(unionId);
    // 更新登录时间
    // 角色集合
    Set<String> roles = permissionService.getRolePermission(iotUser);
    // 权限集合
    Set<String> permissions = permissionService.getMenuPermission(iotUser);
    String tenantId = StrUtil.blankToDefault(SecurityUtils.getTenantIdQuietly(), iotUser.getTenantId());
    IoTTenant tenant = StrUtil.isBlank(tenantId) ? null : ioTTenantMapper.selectByPrimaryKey(tenantId);
    String tenantRole =
        StrUtil.blankToDefault(SecurityUtils.getTenantRoleQuietly(), iotUser.getTenantRole());
    String tenantCode =
        StrUtil.blankToDefault(
            SecurityUtils.getTenantCodeQuietly(), tenant != null ? tenant.getTenantCode() : tenantId);
    String tenantName = tenant == null ? null : tenant.getTenantName();
    if (StrUtil.isNotBlank(tenantId)) {
      iotUser.setTenantId(tenantId);
    }
    if (StrUtil.isNotBlank(tenantRole)) {
      iotUser.setTenantRole(tenantRole);
    }
    Map<String, Object> map = new HashMap<>();
    map.put("user", iotUser);
    map.put("roles", roles);
    map.put("permissions", permissions);
    map.put("tenantId", tenantId);
    map.put("tenantCode", tenantCode);
    map.put("tenantRole", tenantRole);
    map.put("tenantName", tenantName);
    return R.ok(map);
  }

  /**
   * 获取路由信息
   *
   * @return 路由信息
   */
  @Operation(summary = "获取路由信息", description = "获取当前用户的菜单路由信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取路由信息",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping("getRouters")
  public AjaxResult<List<RouterVo>> getRouters() {
    List<SysMenu> menus = menuService.selectMenuTreeByUnionId(SecurityUtils.getUnionId());
    return AjaxResult.success(menuService.buildMenus(menus));
  }

  @Operation(summary = "退出登录", description = "用户退出登录，清除会话信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功退出登录",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping("logout")
  public AjaxResult<Void> logout(HttpServletRequest request) {
    String token = StringUtil.substringAfter(request.getHeader("Authorization"), ' ');
    String unionId = SecurityUtils.getUnionId();
    String key = IoTConstant.EXCLUSIVE_LOGIN + ":" + unionId;
    String tokenKey = IoTConstant.EXCLUSIVE_LOGIN_TOKEN + ":" + unionId;
    if (StrUtil.isNotBlank(token) && Boolean.TRUE.equals(stringRedisTemplate.hasKey(tokenKey))) {
      String origin = stringRedisTemplate.opsForValue().get(tokenKey);
      if (token.equals(origin)) {
        stringRedisTemplate.delete(key);
        stringRedisTemplate.delete(tokenKey);
      }
    }
    return AjaxResult.success();
  }
}
