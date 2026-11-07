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
import cn.universal.persistence.base.BaseUPRequest;
import java.util.List;

/**
 * 数据输出插件接口 - 输出方向 (IoT -> 外部系统)
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/15
 */
public interface DataOutputPlugin extends DataBridgePlugin {

  /** 批量处理数据输出 - 输出方向 (IoT -> 外部系统) */
  void batchProcessOutput(
      List<BaseUPRequest> requests, DataBridgeConfig config, ResourceConnection connection);
}
