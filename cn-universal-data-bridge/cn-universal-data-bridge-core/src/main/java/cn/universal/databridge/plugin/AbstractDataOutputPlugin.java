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
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.databridge.entity.DataBridgeConfig;
import cn.universal.databridge.entity.ResourceConnection;
import cn.universal.persistence.base.BaseUPRequest;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据输出插件抽象基类 提供输出方向的公共实现
 *
 * @version 2.0 @Author gitee.com/NexIoT
 * @since 2025/1/15
 */
@Slf4j
public abstract class AbstractDataOutputPlugin extends AbstractDataBridgePlugin
    implements DataOutputPlugin {
  // 占位符前缀（双大括号避免冲突）
  public static final String LEFT_PLACEHOLDER_PREFIX = "{{";
  // 占位符后缀
  public static final String RIGHT_PLACEHOLDER_SUFFIX = "}}";

  @Override
  public void batchProcessOutput(
      List<BaseUPRequest> requests, DataBridgeConfig config, ResourceConnection connection) {
    if (CollectionUtil.isEmpty(requests)) {
      return;
    }

    try {
      // 1. 解析配置
      JSONObject configJson = parseConfig(config);

      // 2. 过滤数据
      List<BaseUPRequest> filteredRequests = filterRequests(requests, configJson);
      if (CollectionUtil.isEmpty(filteredRequests)) {
        log.debug("所有数据都被过滤，跳过处理");
        return;
      }

      // 3. 检查是否有Magic脚本
      if (StrUtil.isNotBlank(config.getMagicScript())) {
        // 使用Magic脚本处理
        processWithMagicScript(filteredRequests, config, connection);
      } else {
        // 使用默认模板处理
        processWithDefaultTemplate(filteredRequests, config, connection, configJson);
      }

      log.info("数据输出处理完成，处理数量: {}/{}", filteredRequests.size(), requests.size());

    } catch (Exception e) {
      log.error("数据输出处理失败: {}", e.getMessage(), e);
      throw new RuntimeException("数据输出处理失败: " + e.getMessage(), e);
    }
  }

  /** 使用Magic脚本处理数据 - 子类可重写 */
  protected void processWithMagicScript(
      List<BaseUPRequest> requests, DataBridgeConfig config, ResourceConnection connection) {
    for (BaseUPRequest request : requests) {
      try {
        // 执行Magic脚本 - 输出方向 (IoT -> 外部系统)
        Object processedData =
            executeIotToYourScript(config.getMagicScript(), request, config, connection);

        // 处理Magic脚本返回的数据
        processProcessedData(processedData, request, config, connection);

      } catch (Exception e) {
        String deviceKey =
            request.getIoTDeviceDTO() != null ? request.getIoTDeviceDTO().getDeviceId() : null;
        log.error("Magic脚本处理失败，设备: {}, 错误: {}", deviceKey, e.getMessage(), e);
      }
    }
  }

  /** 使用默认模板处理数据 - 子类可重写 */
  protected void processWithDefaultTemplate(
      List<BaseUPRequest> requests,
      DataBridgeConfig config,
      ResourceConnection connection,
      JSONObject configJson) {
    for (BaseUPRequest request : requests) {
      try {
        // 构建模板变量
        Map<String, Object> variables = buildTemplateVariables(request, configJson);

        // 处理模板
        String template = config.getTemplate();
        String processedTemplate = processTemplate(template, variables);

        // 处理模板结果
        processTemplateResult(processedTemplate, request, config, connection);

      } catch (Exception e) {
        String deviceKey =
            request.getIoTDeviceDTO() != null ? request.getIoTDeviceDTO().getDeviceId() : null;
        log.error("默认模板处理失败，设备: {}, 错误: {}", deviceKey, e.getMessage(), e);
      }
    }
  }

  /** 处理Magic脚本返回的数据 - 子类必须实现 */
  protected abstract void processProcessedData(
      Object processedData,
      BaseUPRequest request,
      DataBridgeConfig config,
      ResourceConnection connection);

  /** 渲染Magic脚本输出，字符串原样输出，结构化对象按JSON输出。 */
  protected String renderProcessedPayload(Object processedData) {
    if (processedData instanceof String payload) {
      return payload;
    }
    return JSONUtil.toJsonStr(processedData);
  }

  /** 处理模板结果 - 子类必须实现 */
  protected abstract void processTemplateResult(
      String templateResult,
      BaseUPRequest request,
      DataBridgeConfig config,
      ResourceConnection connection);
}
