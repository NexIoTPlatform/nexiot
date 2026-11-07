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

package cn.universal.ossm.oss.service;

import cn.universal.ossm.oss.entity.UploadResult;
import java.io.InputStream;

/** 云存储服务接口 @Author Lion Li */
public interface ICloudStorageService {

  void createBucket();

  /** 测试云存储连接与密钥可用性，不执行创建或写入操作 */
  void testConnection();

  /** 获取服务商类型 */
  String getServiceType();

  /**
   * 文件路径
   *
   * @param prefix 前缀
   * @param suffix 后缀
   * @return 返回上传路径
   */
  String getPath(String prefix, String suffix);

  /**
   * 文件上传
   *
   * @param data 文件字节数组
   * @param path 文件路径，包含文件名
   * @return 返回http地址
   */
  UploadResult upload(byte[] data, String path, String contentType);

  /**
   * 文件删除
   *
   * @param path 文件路径，包含文件名
   */
  void delete(String path);

  /**
   * 文件上传
   *
   * @param data 文件字节数组
   * @param suffix 后缀
   * @return 返回http地址
   */
  UploadResult uploadSuffix(byte[] data, String suffix, String contentType);

  /**
   * 文件上传
   *
   * @param inputStream 字节流
   * @param path 文件路径，包含文件名
   * @return 返回http地址
   */
  UploadResult upload(InputStream inputStream, String path, String contentType);

  /**
   * 文件上传
   *
   * @param inputStream 字节流
   * @param suffix 后缀
   * @return 返回http地址
   */
  UploadResult uploadSuffix(InputStream inputStream, String suffix, String contentType);

  /** Short-lived read URL, generated only after the caller checks business-resource access. */
  default String signedReadUrl(String objectKey, int seconds) {
    throw new UnsupportedOperationException("当前存储提供方不支持临时读取链接");
  }

  String getEndpointLink();
}
