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

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.entity.ResourceConnection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据输入插件抽象基类 提供输入方向的公共实现
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/15
 */
@Slf4j
public abstract class AbstractDataInputPlugin extends AbstractDataBridgePlugin
    implements DataInputPlugin {

  @Override
  public void batchProcessInput(
      List<Object> externalDataList, DataBridgeConfig config, ResourceConnection connection) {
    if (CollectionUtil.isEmpty(externalDataList)) {
      return;
    }

    try {
      log.info("批量处理外部数据输入，数量: {}", externalDataList.size());

      for (Object externalData : externalDataList) {
        try {
          processExternalDataInput(externalData, config, connection);
        } catch (Exception e) {
          log.error("处理外部数据输入失败，数据: {}, 错误: {}", externalData, e.getMessage(), e);
        }
      }

      log.info("批量处理外部数据输入完成，处理数量: {}", externalDataList.size());
    } catch (Exception e) {
      log.error("批量处理外部数据输入失败: {}", e.getMessage(), e);
      throw new RuntimeException("批量处理外部数据输入失败: " + e.getMessage(), e);
    }
  }

  /** 处理单个外部数据输入 */
  protected void processExternalDataInput(
      Object externalData, DataBridgeConfig config, ResourceConnection connection) {
    try {
      // 检查是否有Magic脚本
      if (StrUtil.isNotBlank(config.getMagicScript())) {
        // 使用Magic脚本处理
        Object processedData =
            executeYourToIotScript(config.getMagicScript(), externalData, config, connection);
        // 发送到IoT平台
        sendToIoTPlatform(processedData, config);
      } else {
        // 使用默认处理逻辑
        processExternalDataWithDefaultLogic(externalData, config, connection);
      }
    } catch (Exception e) {
      log.error("处理外部数据输入失败: {}", e.getMessage(), e);
      throw new RuntimeException("处理外部数据输入失败: " + e.getMessage(), e);
    }
  }

  /** 发送数据到IoT平台 - 子类必须实现 */
  protected abstract void sendToIoTPlatform(Object processedData, DataBridgeConfig config);

  /** 使用默认逻辑处理外部数据 - 子类可重写 */
  protected void processExternalDataWithDefaultLogic(
      Object externalData, DataBridgeConfig config, ResourceConnection connection) {
    try {
      // 默认的外部数据处理逻辑
      log.debug("使用默认逻辑处理外部数据: {}", externalData);

      // 这里可以实现默认的数据转换逻辑
      // 例如：将外部数据转换为IoT设备数据格式

    } catch (Exception e) {
      log.error("默认外部数据处理失败: {}", e.getMessage(), e);
      throw new RuntimeException("默认外部数据处理失败: " + e.getMessage(), e);
    }
  }
}
