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
package cn.universal.web.dto;

import java.util.List;
import lombok.Data;

/**
 * EMQX 认证响应 DTO 对应 EMQX 期望的认证响应格式
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Data
public class EmqxAuthResponse {

  /** 认证结果：allow-允许，deny-拒绝 */
  private String result;

  /** 是否为超级用户 */
  private Boolean is_superuser;

  /** 访问控制列表 */
  private List<AclRule> acl;

  /** 认证响应构造函数 */
  public EmqxAuthResponse() {}

  /** 认证响应构造函数 */
  public EmqxAuthResponse(String result) {
    this.result = result;
    this.is_superuser = false;
  }

  /** 认证响应构造函数 */
  public EmqxAuthResponse(String result, Boolean is_superuser) {
    this.result = result;
    this.is_superuser = is_superuser;
  }

  /** 认证响应构造函数 */
  public EmqxAuthResponse(String result, Boolean is_superuser, List<AclRule> acl) {
    this.result = result;
    this.is_superuser = is_superuser;
    this.acl = acl;
  }

  /** ACL 规则类 */
  @Data
  public static class AclRule {
    /** 主题 */
    private String topic;

    /** 权限：allow-允许，deny-拒绝 */
    private String permission;

    /** 操作：publish-发布，subscribe-订阅，all-全部 */
    private String action;

    /** ACL 规则构造函数 */
    public AclRule() {}

    /** ACL 规则构造函数 */
    public AclRule(String topic, String permission, String action) {
      this.topic = topic;
      this.permission = permission;
      this.action = action;
    }
  }
}
