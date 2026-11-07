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

package cn.universal.persistence.entity.bo;

import cn.universal.persistence.entity.Network;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 网络组件业务对象
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NetworkBO extends Network {

  /** 绑定tcp产品数量 */
  private IoTProductBO bindTcpServerProducts;

  /** 绑定的tcp产品信息 */
  private int bindTcpServerProductCount;

  /** 共享网络组件绑定的产品数量 */
  private int bindProductCount;

  /** 共享网络组件绑定的产品列表 */
  private List<IoTProductBO> bindProducts;

  /** 网络类型名称 */
  private String typeName;

  /** 是否正在运行 */
  private boolean running;

  /** 状态名称 */
  private String stateName;

  /** 创建时间格式化 */
  private String createDateStr;

  /** 网络类型列表（多个类型） */
  private List<String> types;

  /** 启用/停用状态名称 */
  private String enableName;

  /** 数据权限部门 */
  private List<Long> scopeDeptIds = new ArrayList<>();

  /** 数据权限可见用户 */
  private List<String> scopeUnionIds = new ArrayList<>();

  /** 是否无权限 */
  private boolean scopeDenied;
}
