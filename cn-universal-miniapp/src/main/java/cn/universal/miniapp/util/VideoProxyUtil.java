/*
*
• Copyright (c) 2025, NexIoT. All Rights Reserved.

*
• @Description: 视频流地址代理转换工具

• @Author: gitee.com/NexIoT

*
*/

package cn.universal.miniapp.util;

import cn.hutool.core.util.StrUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * • 视频流地址代理转换工具类
 *
 * <p>• 提供视频流地址的代理转换功能，支持复杂嵌套结构
 *
 * <p>• 支持多种协议：HTTP/HTTPS/WS/WSS/RTMP/RTMPS/RTSP/RTSPS
 *
 * <p>• 自动识别URL并转换为代理地址
 */
@Slf4j
public class VideoProxyUtil {

  /**
   * 转换视频流数据为代理地址（支持复杂嵌套结构）
   *
   * @param data 原始数据对象（支持嵌套Map和List）
   * @param proxyDomain 代理域名
   * @param enabled 是否启用代理转换
   * @return 转换后的数据对象
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> convertStreamData(
      Map<String, Object> data, String proxyDomain, boolean enabled) {
    if (!enabled || StrUtil.isBlank(proxyDomain)) {
      return data;
    }

    if (data == null || data.isEmpty()) {
      return data;
    }

    Map<String, Object> result = new HashMap<>();

    for (Map.Entry<String, Object> entry : data.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      if (value == null) {
        result.put(key, null);
      } else if (value instanceof String) {
        // 字符串类型：判断是否为URL并转换
        String strValue = (String) value;
        if (isUrl(strValue)) {
          try {
            result.put(key, convertSingleUrl(strValue, proxyDomain));
            log.debug("转换视频地址: {} -> {}", strValue, result.get(key));
          } catch (Exception e) {
            log.warn("转换视频流地址失败: {}", strValue, e);
            result.put(key, strValue);
          }
        } else {
          result.put(key, strValue);
        }
      } else if (value instanceof Map) {
        // 嵌套Map：递归转换
        result.put(key, convertStreamData((Map<String, Object>) value, proxyDomain, enabled));
      } else if (value instanceof List) {
        // List类型：递归处理
        result.put(key, convertList((List<?>) value, proxyDomain, enabled));
      } else {
        // 其他类型（Number, Boolean等）：直接保留
        result.put(key, value);
      }
    }

    return result;
  }

  /**
   * 转换List中的URL地址
   *
   * @param list 原始列表
   * @param proxyDomain 代理域名
   * @param enabled 是否启用代理转换
   * @return 转换后的列表
   */
  @SuppressWarnings("unchecked")
  private static List<?> convertList(List<?> list, String proxyDomain, boolean enabled) {
    if (list == null || list.isEmpty()) {
      return list;
    }

    List<Object> result = new ArrayList<>();
    for (Object item : list) {
      if (item instanceof String) {
        String strValue = (String) item;
        if (isUrl(strValue)) {
          try {
            result.add(convertSingleUrl(strValue, proxyDomain));
          } catch (Exception e) {
            log.warn("转换视频流地址失败: {}", strValue, e);
            result.add(strValue);
          }
        } else {
          result.add(strValue);
        }
      } else if (item instanceof Map) {
        result.add(convertStreamData((Map<String, Object>) item, proxyDomain, enabled));
      } else if (item instanceof List) {
        result.add(convertList((List<?>) item, proxyDomain, enabled));
      } else {
        result.add(item);
      }
    }

    return result;
  }

  /**
   * 转换视频流地址为代理地址（简单Map版本）
   *
   * @param streamUrls 原始流地址映射
   * @param proxyDomain 代理域名
   * @param enabled 是否启用代理转换
   * @return 转换后的流地址映射
   */
  public static Map<String, String> convertStreamUrls(
      Map<String, String> streamUrls, String proxyDomain, boolean enabled) {
    if (!enabled || StrUtil.isBlank(proxyDomain)) {
      return streamUrls;
    }

    Map<String, String> convertedUrls = new HashMap<>();
    for (Map.Entry<String, String> entry : streamUrls.entrySet()) {
      String key = entry.getKey();
      String url = entry.getValue();

      if (StrUtil.isNotBlank(url)) {
        try {
          convertedUrls.put(key, convertSingleUrl(url, proxyDomain));
          log.debug("转换视频地址: {} -> {}", url, convertedUrls.get(key));
        } catch (Exception e) {
          log.warn("转换视频流地址失败: {}", url, e);
          // 转换失败时保留原始地址
          convertedUrls.put(key, url);
        }
      } else {
        convertedUrls.put(key, url);
      }
    }

    return convertedUrls;
  }

  /**
   * 转换单个URL地址，支持所有协议类型
   *
   * @param originalUrl 原始URL
   * @param proxyDomain 代理域名
   * @return 转换后的URL
   * @throws Exception 解析失败时抛出异常
   */
  public static String convertSingleUrl(String originalUrl, String proxyDomain) throws Exception {
    if (StrUtil.isBlank(originalUrl) || StrUtil.isBlank(proxyDomain)) {
      return originalUrl;
    }

    // 处理不同协议的URL
    if (originalUrl.startsWith("http://") || originalUrl.startsWith("https://")) {
      // HTTP/HTTPS协议处理
      java.net.URL urlObj = new java.net.URL(originalUrl);
      String path = urlObj.getPath();
      String query = urlObj.getQuery();

      StringBuilder newUrl = new StringBuilder();
      // 统一使用HTTPS协议
      newUrl.append("https://").append(proxyDomain).append(path);
      if (StrUtil.isNotBlank(query)) {
        newUrl.append("?").append(query);
      }
      return newUrl.toString();

    } else if (originalUrl.startsWith("rtmp://")) {
      // RTMP协议处理：只保留路径和查询参数，不带端口号
      String pathAndQuery = originalUrl.substring("rtmp://".length());
      String[] parts = pathAndQuery.split("/", 2);
      String pathWithQuery = parts.length > 1 ? "/" + parts[1] : "";

      return String.format("rtmp://%s%s", proxyDomain, pathWithQuery);

    } else if (originalUrl.startsWith("rtsp://")) {
      // RTSP协议处理：只保留路径和查询参数，不带端口号
      String pathAndQuery = originalUrl.substring("rtsp://".length());
      String[] parts = pathAndQuery.split("/", 2);
      String pathWithQuery = parts.length > 1 ? "/" + parts[1] : "";

      return String.format("rtsp://%s%s", proxyDomain, pathWithQuery);

    } else if (originalUrl.startsWith("ws://")) {
      // WS协议处理：转换为WSS安全协议，只保留路径和查询参数，不带端口号
      String pathAndQuery = originalUrl.substring("ws://".length());
      String[] parts = pathAndQuery.split("/", 2);
      String pathWithQuery = parts.length > 1 ? "/" + parts[1] : "";

      return String.format("wss://%s%s", proxyDomain, pathWithQuery);

    } else if (originalUrl.startsWith("wss://")) {
      // WSS协议处理：只保留路径和查询参数，不带端口号
      String pathAndQuery = originalUrl.substring("wss://".length());
      String[] parts = pathAndQuery.split("/", 2);
      String pathWithQuery = parts.length > 1 ? "/" + parts[1] : "";

      return String.format("wss://%s%s", proxyDomain, pathWithQuery);

    } else if (originalUrl.startsWith("rtmps://")) {
      // RTMPS协议处理
      String pathAndQuery = originalUrl.substring("rtmps://".length());
      String[] parts = pathAndQuery.split("/", 2);
      String pathWithQuery = parts.length > 1 ? "/" + parts[1] : "";

      return String.format("rtmps://%s%s", proxyDomain, pathWithQuery);

    } else if (originalUrl.startsWith("rtsps://")) {
      // RTSPS协议处理
      String pathAndQuery = originalUrl.substring("rtsps://".length());
      String[] parts = pathAndQuery.split("/", 2);
      String pathWithQuery = parts.length > 1 ? "/" + parts[1] : "";

      return String.format("rtsps://%s%s", proxyDomain, pathWithQuery);

    } else {
      // 未知协议，返回原始地址
      return originalUrl;
    }
  }

  /**
   * 判断字符串是否为URL
   *
   * @param str 待判断的字符串
   * @return 是否为URL
   */
  public static boolean isUrl(String str) {
    if (StrUtil.isBlank(str)) {
      return false;
    }
    // 判断是否以常见协议开头
    return str.startsWith("http://")
        || str.startsWith("https://")
        || str.startsWith("ws://")
        || str.startsWith("wss://")
        || str.startsWith("rtmp://")
        || str.startsWith("rtmps://")
        || str.startsWith("rtsp://")
        || str.startsWith("rtsps://");
  }
}
