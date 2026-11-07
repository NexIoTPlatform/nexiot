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

package cn.universal.core.protocol.jar.downloader;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpUtil;
import cn.universal.common.exception.CodecException;
import java.io.File;
import lombok.extern.slf4j.Slf4j;

/**
 * 远程 JAR 下载器 负责下载远程 JAR 文件到本地缓存
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/11/02
 */
@Slf4j
public class JarDownloader {

  private static final String JAR_CACHE_DIR =
      System.getProperty("java.io.tmpdir") + File.separator + "nexiot-jar-cache";

  /**
   * 下载远程 JAR 文件
   *
   * @param remoteUrl 远程 URL
   * @return 本地文件路径
   * @throws CodecException 下载失败时抛出异常
   */
  public String download(String remoteUrl) throws CodecException {
    try {
      // 1. 创建缓存目录
      File cacheDir = new File(JAR_CACHE_DIR);
      if (!cacheDir.exists()) {
        boolean created = cacheDir.mkdirs();
        if (!created) {
          throw new CodecException("无法创建 JAR 缓存目录: " + JAR_CACHE_DIR);
        }
        log.info("创建 JAR 缓存目录: {}", JAR_CACHE_DIR);
      }

      // 2. 生成本地文件名（使用 URL 的 MD5 作为文件名，避免重复下载）
      String fileName = generateFileName(remoteUrl);
      File localFile = new File(cacheDir, fileName);

      // 3. 检查文件是否已存在
      if (localFile.exists() && localFile.length() > 0) {
        log.info("使用缓存的 JAR 文件: {}", localFile.getAbsolutePath());
        return localFile.getAbsolutePath();
      }

      // 4. 下载文件
      log.info("开始下载远程 JAR: {}", remoteUrl);
      long size = HttpUtil.downloadFile(remoteUrl, localFile);
      if (size <= 0) {
        throw new CodecException("下载的 JAR 文件大小为 0: " + remoteUrl);
      }
      log.info("JAR 下载完成: {}, 大小: {} bytes", localFile.getAbsolutePath(), size);

      return localFile.getAbsolutePath();
    } catch (Exception e) {
      if (e instanceof CodecException) {
        throw e;
      }
      throw new CodecException("下载远程 JAR 失败: " + remoteUrl + ", error: " + e.getMessage(), e);
    }
  }

  /**
   * 生成文件名（使用 URL 的 MD5 值）
   *
   * @param url 远程 URL
   * @return 文件名
   */
  private String generateFileName(String url) {
    String md5 = DigestUtil.md5Hex(url);
    // 尝试从 URL 中提取原始文件名
    String originalName = extractFileName(url);
    if (StrUtil.isNotBlank(originalName) && originalName.endsWith(".jar")) {
      return md5 + "_" + originalName;
    }
    return md5 + ".jar";
  }

  /**
   * 从 URL 中提取文件名
   *
   * @param url URL
   * @return 文件名
   */
  private String extractFileName(String url) {
    try {
      int lastSlash = url.lastIndexOf('/');
      if (lastSlash >= 0 && lastSlash < url.length() - 1) {
        String fileName = url.substring(lastSlash + 1);
        // 移除查询参数
        int queryIndex = fileName.indexOf('?');
        if (queryIndex > 0) {
          fileName = fileName.substring(0, queryIndex);
        }
        return fileName;
      }
    } catch (Exception e) {
      log.debug("提取文件名失败: url={}, error={}", url, e.getMessage());
    }
    return null;
  }
}
