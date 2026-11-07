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

package cn.universal.databridge.plugin;

import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.entity.ResourceConnection;
import java.util.List;

/**
 * 数据输入插件接口 - 输入方向 (外部系统 -> IoT)
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/15
 */
public interface DataInputPlugin extends DataBridgePlugin {

  /** 批量处理数据输入 - 输入方向 (外部系统 -> IoT) */
  void batchProcessInput(
      List<Object> externalDataList, DataBridgeConfig config, ResourceConnection connection);
}
