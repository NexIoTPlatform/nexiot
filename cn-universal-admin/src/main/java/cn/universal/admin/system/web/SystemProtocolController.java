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

package cn.universal.admin.system.web;

import cn.universal.core.protocol.ProtocolModuleInfo;
import cn.universal.core.protocol.ProtocolModuleRegistry;
import cn.universal.core.protocol.ProtocolModuleRuntimeRegistry;
import cn.universal.core.service.IoTDownlFactory;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统协议模块状态控制器
 *
 * <p>提供系统中各个协议模块的启用状态查询接口，帮助前端了解当前可用的协议服务
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/2
 */
@RestController
@Tag(name = "系统协议模块", description = "系统协议模块状态管理")
@RequestMapping("/admin/protocol")
@Slf4j
public class SystemProtocolController extends BaseController {

  @Resource private ProtocolModuleRegistry protocolModuleRegistry;

  @Resource private ProtocolModuleRuntimeRegistry protocolModuleRuntimeRegistry;

  /**
   * 获取系统协议模块状态
   *
   * <p>查询当前系统中所有协议模块的启用状态，包括：
   *
   * <ul>
   *   <li>CT-AIoT (电信物联网平台)
   *   <li>MQTT (消息队列遥测传输)
   *   <li>TCP (传输控制协议)
   *   <li>HTTP (超文本传输协议)
   *   <li>OneNet (中国移动物联网平台)
   *   <li>ImouLife (乐橙云)
   * </ul>
   *
   * @return 协议模块状态信息
   */
  @Operation(summary = "获取协议模块状态", description = "查询当前系统中所有协议模块的启用状态")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取协议模块状态",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping("/status")
  public AjaxResult getProtocolModuleStatus() {
    log.info("查询协议模块状态");

    try {
      // 使用运行时注册表获取实际运行的协议模块
      Map<String, Map<String, Object>> runtimeProtocols = new HashMap<>();
      ProtocolModuleRuntimeRegistry.getAllRuntimeProtocols()
          .forEach(
              protocol -> {
                Map<String, Object> details = new HashMap<>();
                details.put("code", protocol.getCode());
                details.put("name", protocol.getName());
                details.put("description", protocol.getDescription());
                details.put("version", protocol.getVersion());
                details.put("vendor", protocol.getVendor());
                details.put("isCore", protocol.isCore());
                details.put("category", protocol.getCategory().name());
                details.put("available", true); // 运行时注册表中的都是可用的
                details.put("status", "已启用");
                // 添加是否支持手动创建产品的信息
                details.put("isManualCreatable", protocol.isManualCreatable());
                details.put("notCreatableReason", protocol.getNotCreatableReason());
                runtimeProtocols.put(protocol.getCode(), details);
              });

      // 获取统计信息
      ProtocolModuleRuntimeRegistry.RuntimeStatistics stats =
          ProtocolModuleRuntimeRegistry.getStatistics();
      int totalRegistered = ProtocolModuleRegistry.getAllProtocolCodes().size();

      Map<String, Object> response = new HashMap<>();
      response.put("protocols", runtimeProtocols);
      response.put("totalEnabled", stats.getTotalRunning());
      response.put("totalRegistered", totalRegistered);
      response.put("coreModulesCount", stats.getCoreCount());
      response.put("optionalModulesCount", stats.getOptionalCount());
      response.put("categoryStats", stats.getCategoryStats());

      return AjaxResult.success("查询成功", response);

    } catch (Exception e) {
      log.error("查询协议模块状态失败", e);
      return AjaxResult.error("查询协议模块状态失败: " + e.getMessage());
    }
  }

  /**
   * 检查指定协议模块是否可用
   *
   * @param protocolCode 协议代码 (如: ctwing, mqtt, tcp, http, onenet, imoulife)
   * @return 协议模块可用性信息
   */
  @Operation(summary = "检查协议模块可用性", description = "检查指定协议模块是否可用")
  @ApiResponse(
      responseCode = "200",
      description = "成功检查协议模块可用性",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(
      name = "protocolCode",
      description = "协议代码 (如: ctwing, mqtt, tcp, http, onenet, imoulife)",
      required = true)
  @GetMapping("/check")
  public AjaxResult checkProtocolAvailability(String protocolCode) {
    try {
      if (protocolCode == null || protocolCode.trim().isEmpty()) {
        return AjaxResult.error("协议代码不能为空");
      }

      // 使用运行时注册表检查协议是否可用
      boolean available = ProtocolModuleRuntimeRegistry.isProtocolRunning(protocolCode);
      ProtocolModuleInfo protocolInfo = ProtocolModuleRuntimeRegistry.getProtocolInfo(protocolCode);

      Map<String, Object> result = new HashMap<>();
      result.put("protocolCode", protocolCode);
      result.put("available", available);
      result.put("status", available ? "可用" : "不可用");

      // 如果协议可用，添加详细信息
      if (available && protocolInfo != null) {
        result.put("name", protocolInfo.getName());
        result.put("description", protocolInfo.getDescription());
        result.put("version", protocolInfo.getVersion());
        result.put("vendor", protocolInfo.getVendor());
        result.put("isCore", protocolInfo.isCore());
        result.put("category", protocolInfo.getCategory().name());
      }
      result.put("timestamp", System.currentTimeMillis());

      if (available) {
        log.info("[协议模块检查] 协议 {} 可用", protocolCode);
        return AjaxResult.success("协议模块可用", result);
      } else {
        log.warn(
            "[协议模块检查] 协议 {} 不可用，可用协议: {}", protocolCode, IoTDownlFactory.getEnabledDownServices());
        result.put("availableProtocols", IoTDownlFactory.getEnabledDownServices());
        return AjaxResult.success("协议模块不可用", result);
      }

    } catch (Exception e) {
      log.error("[协议模块检查] 检查协议 {} 失败", protocolCode, e);
      return AjaxResult.error("检查协议模块失败: " + e.getMessage());
    }
  }

  /**
   * 获取协议模块分类统计
   *
   * @return 协议模块分类统计信息
   */
  @Operation(summary = "获取协议分类统计", description = "按分类统计协议模块数量")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取协议分类统计",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping("/categories")
  public AjaxResult getProtocolCategories() {
    try {
      Map<String, Object> result = new HashMap<>();

      // 按分类统计
      Map<String, Integer> categoryStats = new HashMap<>();
      Map<String, java.util.List<String>> categoryModules = new HashMap<>();

      for (ProtocolModuleInfo.ProtocolCategory category :
          ProtocolModuleInfo.ProtocolCategory.values()) {
        java.util.List<ProtocolModuleInfo> modules =
            ProtocolModuleRegistry.getModulesByCategory(category);
        categoryStats.put(category.name(), modules.size());

        java.util.List<String> moduleNames =
            modules.stream()
                .map(ProtocolModuleInfo::getName)
                .collect(java.util.stream.Collectors.toList());
        categoryModules.put(category.name(), moduleNames);
      }

      result.put("categoryStats", categoryStats);
      result.put("categoryModules", categoryModules);

      // 核心模块统计
      java.util.List<ProtocolModuleInfo> coreModules = ProtocolModuleRegistry.getCoreModules();
      java.util.List<ProtocolModuleInfo> optionalModules =
          ProtocolModuleRegistry.getOptionalModules();

      result.put(
          "coreModules",
          coreModules.stream()
              .map(
                  m ->
                      Map.of(
                          "code",
                          m.getCode(),
                          "name",
                          m.getName(),
                          "category",
                          m.getCategory().name()))
              .collect(java.util.stream.Collectors.toList()));

      result.put(
          "optionalModules",
          optionalModules.stream()
              .map(
                  m ->
                      Map.of(
                          "code",
                          m.getCode(),
                          "name",
                          m.getName(),
                          "category",
                          m.getCategory().name()))
              .collect(java.util.stream.Collectors.toList()));

      result.put("timestamp", System.currentTimeMillis());

      return AjaxResult.success("查询成功", result);

    } catch (Exception e) {
      log.error("[协议分类统计] 查询失败", e);
      return AjaxResult.error("查询协议分类失败: " + e.getMessage());
    }
  }
}
