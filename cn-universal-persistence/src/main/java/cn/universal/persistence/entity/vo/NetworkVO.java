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

package cn.universal.persistence.entity.vo;

import cn.universal.persistence.entity.Network;
import cn.universal.persistence.entity.bo.IoTProductBO;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 网络组件视图对象
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NetworkVO extends Network {

  /** 网络类型名称 */
  private String typeName;

  /** 状态名称 */
  private String stateName;

  /** 创建时间格式化 */
  private String createDateStr;

  /** 配置对象（解析后的JSON） */
  private Object configObject;

  /** 启用/停用状态名称 */
  private String enableName;

  /** 是否正在运行 */
  private boolean running;

  /** 绑定tcp产品数量 */
  private int bindTcpServerProductCount;

  /** 绑定的tcp产品信息 */
  private IoTProductBO bindTcpServerProducts;

  /** 共享网络组件绑定的产品数量 */
  private int bindProductCount;

  /** 共享网络组件绑定的产品列表 */
  private List<IoTProductBO> bindProducts;
}
