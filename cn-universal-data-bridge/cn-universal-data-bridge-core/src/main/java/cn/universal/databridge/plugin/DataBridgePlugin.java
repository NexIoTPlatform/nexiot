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
import cn.universal.databridge.entity.PluginInfo;
import cn.universal.databridge.entity.ResourceConnection;
import java.util.List;

/**
 * 数据桥接插件接口 - 核心接口 只定义最核心的方法，其他功能通过抽象基类实现
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/15
 */
public interface DataBridgePlugin {

  /** 获取插件信息 */
  PluginInfo getPluginInfo();

  /** 测试资源连接 */
  Boolean testConnection(ResourceConnection connection);

  /** 验证配置 */
  Boolean validateConfig(DataBridgeConfig config);

  /** 获取支持的源范围 */
  List<SourceScope> getSupportedSourceScopes();
}
