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

package cn.universal.persistence.query;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 网络组件查询对象
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
public class NetworkQuery extends BasePage {

  /** 网络类型（单个类型） */
  private String type;

  /** 网络类型列表（多个类型） */
  private List<String> types;

  /** 网络组件名称 */
  private String name;

  /** 产品Key */
  private String productKey;

  /** 状态 */
  private Integer state;

  /** 唯一标识 */
  private String unionId;

  /** 创建用户 */
  private String createUser;

  /** 租户ID */
  private String tenantId;

  /** 数据权限部门 */
  @lombok.Builder.Default
  private List<Long> scopeDeptIds = new ArrayList<>();

  /** 数据权限可见用户 */
  @lombok.Builder.Default
  private List<String> scopeUnionIds = new ArrayList<>();

  /** 是否无权限 */
  private boolean scopeDenied;
}
