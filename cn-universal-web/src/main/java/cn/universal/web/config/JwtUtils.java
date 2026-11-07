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

package cn.universal.web.config;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/** JWT 工具类 - 简化版本 使用 Spring Authorization Server 内置的 JWT 功能 */
@Component
public class JwtUtils {

  /** 验证 token 是否有效 注意：实际的 JWT 验证由 Spring Authorization Server 处理 */
  public Boolean validateToken(String token) {
    // 这里可以添加额外的验证逻辑
    // 实际的 JWT 验证由 Spring Authorization Server 的 JWT 解码器处理
    return token != null && !token.isEmpty();
  }

  /** 从 token 中提取用户名 注意：实际的 JWT 解析由 Spring Authorization Server 处理 */
  public String extractUsername(String token) {
    // 这里可以添加额外的解析逻辑
    // 实际的 JWT 解析由 Spring Authorization Server 处理
    return null;
  }

  /** 生成自定义 claims */
  public Map<String, Object> generateClaims(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("username", userDetails.getUsername());
    claims.put("Authorities", userDetails.getAuthorities());
    claims.put("created", new Date());
    return claims;
  }
}
