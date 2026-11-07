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

package cn.universal.ossm.service;

import cn.universal.ossm.entity.SysOssConfig;
import java.util.List;

/** OSS存储配置服务 */
public interface ISysOssConfigService {

  List<SysOssConfig> listMaskedConfigs();

  List<SysOssConfig> listMaskedConfigs(String tenantId);

  SysOssConfig getMaskedConfig(String provider);

  SysOssConfig getMaskedConfig(String provider, String tenantId);

  SysOssConfig getRuntimeConfig(String provider);

  SysOssConfig getRuntimeConfig(String provider, String tenantId);

  String getActiveProvider();

  String getActiveProvider(String tenantId);

  SysOssConfig saveConfig(SysOssConfig config, String operator);

  SysOssConfig saveConfig(SysOssConfig config, String operator, String tenantId);

  void activate(String provider, String operator);

  void activate(String provider, String operator, String tenantId);

  void test(String provider);

  void test(String provider, String tenantId);

  void ensureTenantDefaultConfigs(String tenantId, String operator);

  void clearRuntimeCache();
}
