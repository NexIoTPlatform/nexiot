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

package cn.universal.admin.common.utils;

import cn.universal.common.config.UnivUPloadConfig;
import cn.universal.common.constant.Constants;
import cn.universal.common.utils.StringUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 图片工具类 */
public class ImageUtils {

  private static final Logger log = LoggerFactory.getLogger(ImageUtils.class);

  public static byte[] getImage(String imagePath) {
    InputStream is = getFile(imagePath);
    try {
      return IOUtils.toByteArray(is);
    } catch (Exception e) {
      log.error("加载异常", e);
      return null;
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  public static InputStream getFile(String imagePath) {
    try {
      byte[] result = readFile(imagePath);
      result = Arrays.copyOf(result, result.length);
      return new ByteArrayInputStream(result);
    } catch (Exception e) {
      log.error("图片异常", e);
    }
    return null;
  }

  /**
   * 读取文件为字节数据
   *
   * @param url 地址
   * @return 字节数据
   */
  public static byte[] readFile(String url) {
    InputStream in = null;
    ByteArrayOutputStream baos = null;
    try {
      if (url.startsWith("http")) {
        // 网络地址
        URL urlObj = new URL(url);
        URLConnection urlConnection = urlObj.openConnection();
        urlConnection.setConnectTimeout(30 * 1000);
        urlConnection.setReadTimeout(60 * 1000);
        urlConnection.setDoInput(true);
        in = urlConnection.getInputStream();
      } else {
        // 本机地址
        String localPath = UnivUPloadConfig.getProfile();
        String downloadPath =
            localPath + StringUtils.substringAfter(url, Constants.RESOURCE_PREFIX);
        in = new FileInputStream(downloadPath);
      }
      return IOUtils.toByteArray(in);
    } catch (Exception e) {
      log.error("获取文件路径异常", e);
      return null;
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(baos);
    }
  }
}
