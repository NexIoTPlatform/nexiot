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

/**
 * 双向数据插件接口 - 同时支持输入和输出
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/15
 */
public interface BidirectionalDataPlugin extends DataOutputPlugin, DataInputPlugin {
  // 双向插件同时继承输入和输出接口
}
