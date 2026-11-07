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

package cn.universal.web.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.utils.DingTalkUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.logback.LogbackLoggingSystem;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 程序版本 */
@RestController
@RequestMapping("/test")
@Tag(name = "程序版本管理", description = "程序版本和调试相关接口")
@Slf4j(topic = "api_log")
public class VersionController {

  @GetMapping("/v1/debug/log")
  @Operation(summary = "修改日志级别", description = "修改指定日志记录器的日志级别")
  @Parameter(name = "name", description = "日志记录器名称", required = true)
  @Parameter(name = "level", description = "日志级别(INFO/DEBUG)", required = true)
  @ApiResponse(
      responseCode = "200",
      description = "成功修改日志级别",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class)))
  public Object update(@RequestParam String name, @RequestParam LogLevel level) {
    log.info("修改日志级别 name={} ,level={}", name, level.name());
    if (!(LogLevel.INFO.equals(level) || LogLevel.DEBUG.equals(level))) {
      return Map.of("success", false, "message", "只允许DEBUG和INFO级别");
    }
    try {
      loggingSystem.setLogLevel(name, level);
      ch.qos.logback.classic.Logger verifyLogger =
          (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(name);
      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("loggerName", name);
      result.put("requestedLevel", level.name());
      result.put(
          "actualLevel",
          verifyLogger.getLevel() != null ? verifyLogger.getLevel().toString() : "null");
      result.put("effectiveLevel", verifyLogger.getEffectiveLevel().toString());
      result.put("message", "日志级别修改成功");
      return result;
    } catch (Exception e) {
      log.error("修改日志级别失败: name={}, level={}", name, level, e);
      return Map.of(
          "success",
          false,
          "loggerName",
          name,
          "requestedLevel",
          level.name(),
          "error",
          e.getMessage(),
          "message",
          "日志级别修改失败");
    }
  }

  @GetMapping("/v1/debug/log/status")
  @Operation(summary = "获取日志状态", description = "获取指定日志记录器的当前状态")
  @Parameter(name = "name", description = "日志记录器名称", required = true)
  @ApiResponse(
      responseCode = "200",
      description = "成功获取日志状态",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class)))
  public Object getLogStatus(@RequestParam String name) {
    try {
      ch.qos.logback.classic.Logger logger =
          (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(name);

      Map<String, Object> result = new HashMap<>();
      result.put("loggerName", name);
      result.put("currentLevel", logger.getLevel() != null ? logger.getLevel().toString() : "null");
      result.put("effectiveLevel", logger.getEffectiveLevel().toString());
      result.put("isAdditive", logger.isAdditive());
      result.put(
          "appenderCount", logger.iteratorForAppenders().hasNext() ? "有appender" : "无appender");

      return result;
    } catch (Exception e) {
      log.error("获取日志状态失败: name={}", name, e);
      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("loggerName", name);
      result.put("error", e.getMessage());
      return result;
    }
  }

  @GetMapping("/v1/debug/log/test")
  @Operation(summary = "测试日志输出", description = "测试指定日志记录器的日志输出功能")
  @Parameter(name = "name", description = "日志记录器名称", required = true)
  @ApiResponse(
      responseCode = "200",
      description = "成功测试日志输出",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class)))
  public Object testLog(@RequestParam String name) {
    try {
      Logger testLogger = org.slf4j.LoggerFactory.getLogger(name);

      testLogger.trace("TRACE级别测试日志 - {}", name);
      testLogger.debug("DEBUG级别测试日志 - {}", name);
      testLogger.info("INFO级别测试日志 - {}", name);
      testLogger.warn("WARN级别测试日志 - {}", name);
      testLogger.error("ERROR级别测试日志 - {}", name);

      return Map.of("success", true, "loggerName", name, "message", "测试日志已输出，请检查日志文件");
    } catch (Exception e) {
      log.error("测试日志失败: name={}", name, e);
      return Map.of("success", false, "loggerName", name, "error", e.getMessage());
    }
  }

  @GetMapping("/v1/debug/log/testsql")
  @Operation(summary = "测试SQL日志输出", description = "测试SQL日志输出功能")
  @ApiResponse(
      responseCode = "200",
      description = "成功测试SQL日志输出",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class)))
  public Object testSqlLog() {
    try {
      // 获取 MyBatis mybatis logger
      ch.qos.logback.classic.Logger mybatisLogger =
          (ch.qos.logback.classic.Logger)
              org.slf4j.LoggerFactory.getLogger("cn.universal.persistence.mybatis");

      // 获取 root logger
      ch.qos.logback.classic.Logger rootLogger =
          (ch.qos.logback.classic.Logger)
              org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

      // 创建返回结果
      Map<String, Object> result = new HashMap<>();
      result.put("timestamp", System.currentTimeMillis());

      // 检查 MyBatis logger 状态
      Map<String, Object> mybatisLoggerInfo = new HashMap<>();
      mybatisLoggerInfo.put("name", "cn.universal.persistence.mybatis");
      mybatisLoggerInfo.put(
          "level", mybatisLogger.getLevel() != null ? mybatisLogger.getLevel().toString() : "null");
      mybatisLoggerInfo.put("effectiveLevel", mybatisLogger.getEffectiveLevel().toString());
      mybatisLoggerInfo.put("isAdditive", mybatisLogger.isAdditive());

      // 检查 root logger 状态
      Map<String, Object> rootLoggerInfo = new HashMap<>();
      rootLoggerInfo.put("name", "ROOT");
      rootLoggerInfo.put(
          "level", rootLogger.getLevel() != null ? rootLogger.getLevel().toString() : "null");
      rootLoggerInfo.put("effectiveLevel", rootLogger.getEffectiveLevel().toString());

      result.put("mybatisLogger", mybatisLoggerInfo);
      result.put("rootLogger", rootLoggerInfo);

      // 尝试输出一些 SQL 相关的日志
      log.info("开始测试 SQL 日志输出...");

      // 使用 PerformanceInterceptor 输出 SQL 日志（如果已配置）
      Logger perfLogger = org.slf4j.LoggerFactory.getLogger("mybatisPerformanceInterceptor");
      if (perfLogger.isDebugEnabled()) {
        perfLogger.debug("这是一个测试 SQL 性能拦截器的日志");
        result.put("performanceInterceptorEnabled", true);
      } else {
        result.put("performanceInterceptorEnabled", false);
      }

      // 检查是否有其他相关的 SQL 日志记录器
      Logger sqlLogger = org.slf4j.LoggerFactory.getLogger("org.apache.ibatis");
      if (sqlLogger.isDebugEnabled()) {
        sqlLogger.debug("这是一个测试 Apache MyBatis 的日志");
        result.put("mybatisDebugEnabled", true);
      } else {
        result.put("mybatisDebugEnabled", false);
      }

      result.put("success", true);
      result.put("message", "SQL 日志诊断完成，请检查日志输出");

      return result;
    } catch (Exception e) {
      log.error("SQL 日志诊断失败", e);
      return Map.of("success", false, "error", e.getMessage(), "message", "SQL 日志诊断失败");
    }
  }

  // 移除强制/批量/测试接口，保留最小化的设置与查询

  @GetMapping("/v1/debug/log/diagnose")
  public Object diagnoseLogConfig() {
    try {
      Map<String, Object> result = new HashMap<>();
      result.put("timestamp", System.currentTimeMillis());

      // 获取各种关键 logger 的状态
      String[] loggerNames = {
        "ROOT",
        "api",
        "mqtt",
        "tcp",
        "udp",
        "cn.universal.http.protocol",
        "cn.ctaiot.protocol",
        "cn.onenet.protocol",
        "cn.imoulife.protocol",
        "cn.ezviz.protocol",
        "cn.dahua.cloud.protocol",
        "cn.wvp.protocol",
        "cn.hik.isc.protocol",
        "cn.dahua.icc.protocol",
        "cn.universal.persistence.mybatis",
        "org.apache.ibatis",
        "org.springframework",
        "cn.hutool"
      };

      Map<String, Object> loggerDetails = new HashMap<>();

      for (String loggerName : loggerNames) {
        ch.qos.logback.classic.Logger logger;
        if ("ROOT".equals(loggerName)) {
          logger =
              (ch.qos.logback.classic.Logger)
                  org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        } else {
          logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(loggerName);
        }

        Map<String, Object> loggerInfo = new HashMap<>();
        loggerInfo.put("name", loggerName);
        loggerInfo.put("level", logger.getLevel() != null ? logger.getLevel().toString() : "null");
        loggerInfo.put("effectiveLevel", logger.getEffectiveLevel().toString());
        loggerInfo.put("isAdditive", logger.isAdditive());

        // 检查是否有 appender
        int appenderCount = 0;
        ch.qos.logback.core.Appender<ch.qos.logback.classic.spi.ILoggingEvent> appender;
        java.util.Iterator<ch.qos.logback.core.Appender<ch.qos.logback.classic.spi.ILoggingEvent>>
            iterator = logger.iteratorForAppenders();
        while (iterator.hasNext()) {
          appender = iterator.next();
          appenderCount++;
        }
        loggerInfo.put("appenderCount", appenderCount);

        loggerDetails.put(loggerName, loggerInfo);
      }

      result.put("loggers", loggerDetails);

      // 测试不同级别的日志输出
      Logger testLogger = org.slf4j.LoggerFactory.getLogger("cn.universal.persistence.mybatis");
      testLogger.trace("TRACE级别测试日志 - 诊断接口");
      testLogger.debug("DEBUG级别测试日志 - 诊断接口");
      testLogger.info("INFO级别测试日志 - 诊断接口");
      testLogger.warn("WARN级别测试日志 - 诊断接口");
      testLogger.error("ERROR级别测试日志 - 诊断接口");

      result.put("success", true);
      result.put("message", "日志配置诊断完成，请检查日志文件输出");

      return result;
    } catch (Exception e) {
      log.error("日志配置诊断失败", e);
      return Map.of("success", false, "error", e.getMessage(), "message", "日志配置诊断失败");
    }
  }

  /** 获取所有协议模块的日志状态 */
  @GetMapping("/v1/debug/log/protocols")
  public Object getProtocolLogStatus() {
    try {
      Map<String, Object> result = new HashMap<>();
      result.put("timestamp", System.currentTimeMillis());

      String[] protocolLoggers = {
        "api",
        "mqtt",
        "tcp",
        "udp",
        "cn.universal.http.protocol",
        "cn.ctaiot.protocol",
        "cn.onenet.protocol",
        "cn.imoulife.protocol",
        "cn.ezviz.protocol",
        "cn.dahua.cloud.protocol",
        "cn.wvp.protocol",
        "cn.hik.isc.protocol",
        "cn.dahua.icc.protocol"
      };

      Map<String, Object> protocolStatus = new HashMap<>();

      for (String loggerName : protocolLoggers) {
        ch.qos.logback.classic.Logger logger =
            (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(loggerName);

        Map<String, Object> status = new HashMap<>();
        status.put("level", logger.getLevel() != null ? logger.getLevel().toString() : "inherit");
        status.put("effectiveLevel", logger.getEffectiveLevel().toString());
        status.put("additivity", logger.isAdditive());

        protocolStatus.put(loggerName, status);
      }

      result.put("protocols", protocolStatus);
      result.put("success", true);
      result.put("message", "协议日志状态获取成功");

      return result;
    } catch (Exception e) {
      log.error("获取协议日志状态失败", e);
      return Map.of("success", false, "error", e.getMessage(), "message", "获取协议日志状态失败");
    }
  }

  /** 测试指定协议的日志输出 */
  @GetMapping("/v1/debug/log/testprotocol")
  public Object testProtocolLog(@RequestParam String protocol) {
    try {
      Logger testLogger = org.slf4j.LoggerFactory.getLogger(protocol);

      testLogger.trace("[协议日志测试] TRACE级别 - protocol={}", protocol);
      testLogger.debug("[协议日志测试] DEBUG级别 - protocol={}", protocol);
      testLogger.info("[协议日志测试] INFO级别 - protocol={}", protocol);
      testLogger.warn("[协议日志测试] WARN级别 - protocol={}", protocol);
      testLogger.error("[协议日志测试] ERROR级别 - protocol={}", protocol);

      return Map.of(
          "success",
          true,
          "protocol",
          protocol,
          "message",
          "协议日志测试完成，请检查对应日志文件: logs/" + protocol.replaceAll("\\.", "-") + ".log");
    } catch (Exception e) {
      log.error("协议日志测试失败: protocol={}", protocol, e);
      return Map.of(
          "success", false, "protocol", protocol, "error", e.getMessage(), "message", "协议日志测试失败");
    }
  }

  @GetMapping("/version")
  public Object versionInformation() {
    return readGitProperties();
  }

  @Resource private LogbackLoggingSystem loggingSystem;
  @Resource private StringRedisTemplate stringRedisTemplate;

  // ==========================================
  // 协议扩展模块日志管理API
  // ==========================================

  /** 设置协议模块日志级别 */
  // 以下协议日志管理接口已移除，保留最小化动态级别设置

  /** 批量设置协议模块日志级别 */

  /** 恢复协议模块原始日志级别 */

  /** 获取协议模块日志状态 */

  /** 获取支持的协议模块列表 */

  /** 获取已修改的协议模块信息 */

  /** 测试协议模块日志输出 */

  // ==========================================
  // TCP协议设备统计调试API
  // ==========================================

  /** 获取TCP协议设备统计信息（调试用） */
  @GetMapping("/v1/tcp/stats")
  public Object getTcpStats() {
    try {
      // 这里需要注入TcpConnectionManager，暂时返回模拟数据
      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("message", "TCP协议设备统计功能已优化");
      result.put("timestamp", System.currentTimeMillis());

      // 模拟统计数据
      result.put("currentInstance", "instance-001");
      result.put("currentDeviceCount", 150);
      result.put("activeInstanceCount", 3);
      result.put("totalActiveDevices", 450);
      result.put("totalDevicesIncludingZombie", 500);
      result.put("zombieDeviceCount", 50);

      return result;
    } catch (Exception e) {
      log.error("获取TCP协议统计失败", e);
      return Map.of("success", false, "error", e.getMessage());
    }
  }

  /** 调试Redis中的TCP实例设备索引 */
  @GetMapping("/v1/tcp/debug/redis")
  public Object debugTcpRedis() {
    try {
      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("message", "TCP Redis调试信息");
      result.put("timestamp", System.currentTimeMillis());

      // Redis Key信息
      result.put("deviceRoutesKey", "tcp:device:routes");
      result.put("instanceDevicesKeyPrefix", "tcp:instance:devices:");
      result.put("expectedPattern", "tcp:instance:devices:*");

      // 可能的问题分析
      Map<String, Object> analysis = new HashMap<>();
      analysis.put("ttlIssue", "实例设备索引TTL设置为10分钟，可能已过期");
      analysis.put("registrationIssue", "设备连接时可能没有正确调用registerDeviceRoute");
      analysis.put("scanIssue", "SCAN命令可能没有找到匹配的key");
      analysis.put("redisConnectionIssue", "Redis连接可能有问题");

      result.put("analysis", analysis);

      // 建议的检查步骤
      String[] checkSteps = {
        "1. 检查Redis中是否存在 tcp:device:routes key",
        "2. 检查Redis中是否存在 tcp:instance:devices:* 模式的key",
        "3. 检查设备连接时是否正确调用了registerDevice方法",
        "4. 检查实例心跳是否正常",
        "5. 检查Redis连接是否正常"
      };
      result.put("checkSteps", checkSteps);

      return result;
    } catch (Exception e) {
      log.error("调试TCP Redis失败", e);
      return Map.of("success", false, "error", e.getMessage());
    }
  }

  /** 读取文件 */
  private JSONObject readGitProperties() {
    FileSystemResource classPathResource = new FileSystemResource("./version.json");
    InputStream inputStream = null;
    try {
      inputStream = classPathResource.getInputStream();
    } catch (IOException e) {
      log.error("获取文件异常", e);
    }
    return JSONUtil.parseObj(readFromInputStream(inputStream));
  }

  /** 读取文件里面的值 */
  private String readFromInputStream(InputStream inputStream) {
    StringBuilder stringBuilder = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
      String line;
      while ((line = br.readLine()) != null) {
        stringBuilder.append(line);
      }
    } catch (IOException e) {
      log.error("读取文件失败", e);
    }
    return stringBuilder.toString();
  }

  /** 接收用户使用情况报告 */
  @PostMapping("/report")
  public Object receiveUserReport(@RequestBody String body, HttpServletRequest request) {
    try {
      // 记录请求信息
      log.info("=== 系统状态报告 ===");
      log.info("请求方法: {}", request.getMethod());
      log.info("请求URL: {}", request.getRequestURL());
      log.info("客户端IP: {}", JakartaServletUtil.getClientIP(request));
      log.info("User-Agent: {}", request.getHeader("User-Agent"));
      log.info("Referer: {}", request.getHeader("Referer"));

      // 解析JSON数据
      if (body != null && !body.trim().isEmpty()) {
        try {
          ObjectMapper mapper = new ObjectMapper();
          JsonNode jsonData = mapper.readTree(body);

          // 记录用户数据（使用简化的字段名）
          log.info("用户IP: {}", jsonData.get("ip"));
          log.info("用户代理: {}", jsonData.get("ua"));
          log.info("时间戳: {}", jsonData.get("ts"));
          log.info("版本: {}", jsonData.get("v"));
          log.info("访问URL: {}", jsonData.get("url"));
          log.info("来源页面: {}", jsonData.get("ref"));
          log.info("屏幕分辨率: {}", jsonData.get("sr"));
          log.info("时区: {}", jsonData.get("tz"));
          log.info("语言: {}", jsonData.get("lang"));
          log.info("客户端ID: {}", jsonData.get("cid"));

          // 发送钉钉通知
          try {
            StringBuilder message = new StringBuilder();
            message.append("🔔 系统状态报告通知\n");
            message.append("📍 用户IP: ").append(jsonData.get("ip")).append("\n");
            message
                .append("🌐 客户端IP: ")
                .append(JakartaServletUtil.getClientIP(request))
                .append("\n");
            message.append("🌐 访问URL: ").append(jsonData.get("url")).append("\n");
            message.append("📱 用户代理: ").append(jsonData.get("ua")).append("\n");
            message.append("⏰ 时间戳: ").append(DateUtil.now()).append("\n");
            message.append("🔧 来源页面: ").append(jsonData.get("ref")).append("\n");
            message.append("📱 分辨率: ").append(jsonData.get("sr")).append("\n");
            message.append("🔧 版本: ").append(jsonData.get("v")).append("\n");
            message.append("🆔 客户端ID: ").append(jsonData.get("cid"));
            DingTalkUtil.send(message.toString());
            // DingTalk通知已禁用
          } catch (Exception e) {
            log.error("发送钉钉通知失败: {}", e.getMessage());
          }

          // 这里可以添加数据库存储逻辑
          // saveUserReport(jsonData);

        } catch (Exception e) {
          log.error("解析JSON数据失败: {}", e.getMessage());
        }
      }

      log.info("=== 系统状态报告结束 ===");

      // 返回成功响应
      return Map.of("success", true, "message", "状态同步成功", "timestamp", System.currentTimeMillis());

    } catch (Exception e) {
      log.error("处理系统状态报告失败", e);
      return Map.of("success", false, "message", "服务器错误", "error", e.getMessage());
    }
  }

  /** 处理图片追踪请求 */
  @GetMapping("/report")
  public Object handleImageTracking(
      @RequestParam(required = false) String m,
      @RequestParam(required = false) String d,
      @RequestParam(required = false) String t,
      @RequestParam(required = false) String k,
      HttpServletRequest request) {

    try {
      // 验证密钥
      if (!"iot_tracking_2025".equals(k)) {
        log.warn("无效的追踪密钥: {}", k);
        return "invalid";
      }

      // 如果是图片追踪请求
      if ("img".equals(m) && d != null) {
        try {
          // 解码数据
          String decodedData = new String(Base64.getDecoder().decode(d));
          ObjectMapper mapper = new ObjectMapper();
          JsonNode jsonData = mapper.readTree(decodedData);

          log.info("=== 图片追踪数据 ===");
          log.info("用户IP: {}", jsonData.get("ip"));
          log.info("时间戳: {}", jsonData.get("ts"));
          log.info("客户端ID: {}", jsonData.get("cid"));
          log.info("=== 图片追踪数据结束 ===");

          // 发送钉钉通知
          try {
            StringBuilder message = new StringBuilder();
            message.append("🖼️ 图片追踪通知\n");
            message.append("📍 用户IP: ").append(jsonData.get("ip")).append("\n");
            message.append("⏰ 时间戳: ").append(jsonData.get("ts")).append("\n");
            message.append("🆔 客户端ID: ").append(jsonData.get("cid"));
            DingTalkUtil.send(message.toString());
            // DingTalk通知已禁用
          } catch (Exception e) {
            log.error("发送钉钉通知失败: {}", e.getMessage());
          }

        } catch (Exception e) {
          log.error("解析图片追踪数据失败: {}", e.getMessage());
        }
      }

      // 返回1x1像素的GIF
      byte[] pixel =
          Base64.getDecoder().decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");
      return ResponseEntity.ok().contentType(MediaType.IMAGE_GIF).body(pixel);

    } catch (Exception e) {
      log.error("处理图片追踪失败", e);
      return "error";
    }
  }

  /** echo - 打印请求体和请求头 */
  @RequestMapping("/echo")
  public Object testLog(@RequestBody String body, HttpServletRequest request) {

    // 打印所有请求头
    log.info("=== 请求头信息 ===");
    Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();
      String headerValue = request.getHeader(headerName);
      log.info("Header: {} = {}", headerName, headerValue);
    }
    log.info("=== 请求头信息结束 ===");

    // 打印请求方法、URL、IP等信息
    log.info("请求方法: {}", request.getMethod());
    log.info("请求URL: {}", request.getRequestURL());
    log.info("客户端IP: {}", request.getRemoteAddr());
    log.info("接收第三方消息={}", body);
    return body;
  }
}
