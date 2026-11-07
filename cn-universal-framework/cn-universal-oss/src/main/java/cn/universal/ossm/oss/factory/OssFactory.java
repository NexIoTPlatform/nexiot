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

package cn.universal.ossm.oss.factory;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.universal.common.utils.SpringUtils;
import cn.universal.ossm.oss.constant.CloudConstant;
import cn.universal.ossm.oss.context.OssTenantContext;
import cn.universal.ossm.oss.enumd.CloudServiceEnumd;
import cn.universal.ossm.oss.exception.OssException;
import cn.universal.ossm.oss.service.ICloudStorageService;
import cn.universal.ossm.service.ISysOssConfigService;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.env.Environment;

/** 文件上传Factory @Author Lion Li */
public class OssFactory {

  private static final Map<String, ICloudStorageService> SERVICES = new ConcurrentHashMap<>();
  private static volatile String currentType;

  public static ICloudStorageService instance() {
    return instance(getCurrentType());
  }

  public static ICloudStorageService instance(String type) {
    if (StrUtil.isBlank(type)) {
      throw new OssException("云存储服务商不能为空");
    }
    ICloudStorageService service = SERVICES.get(type);
    if (service == null) {
      Class<?> serviceClass = CloudServiceEnumd.getServiceClass(type);
      if (serviceClass == null) {
        throw new OssException("不支持的云存储服务商: " + type);
      }
      service = (ICloudStorageService) SpringUtils.getBean(serviceClass);
      SERVICES.put(type, service);
    }
    return service;
  }

  public static String getCurrentType() {
    String tenantId = OssTenantContext.getTenantId();
    if (StrUtil.isNotBlank(tenantId)) {
      String tenantType = getTypeFromSysConfig();
      return StrUtil.blankToDefault(tenantType, CloudServiceEnumd.QINIU.getValue()).trim();
    }
    if (StrUtil.isNotBlank(currentType)) {
      return currentType;
    }
    String type = getTypeFromSysConfig();
    if (StrUtil.isBlank(type)) {
      type = getTypeFromProperties();
    }
    currentType = StrUtil.blankToDefault(type, CloudServiceEnumd.QINIU.getValue()).trim();
    return currentType;
  }

  public static void setCurrentType(String type) {
    if (CloudServiceEnumd.getServiceClass(type) == null) {
      throw new OssException("不支持的云存储服务商: " + type);
    }
    currentType = type;
  }

  public static void register(String type, ICloudStorageService iCloudStorageService) {
    Assert.notNull(type, "type can't be null");
    SERVICES.put(type, iCloudStorageService);
  }

  private static String getTypeFromProperties() {
    try {
      Environment environment = SpringUtils.getBean(Environment.class);
      return environment.getProperty("cloud-storage.type");
    } catch (Exception ignored) {
      return null;
    }
  }

  private static String getTypeFromSysConfig() {
    try {
      ISysOssConfigService ossConfigService = SpringUtils.getBean(ISysOssConfigService.class);
      String provider = ossConfigService.getActiveProvider();
      if (StrUtil.isNotBlank(provider)) {
        return provider;
      }
    } catch (Exception ignored) {
      // fallback to legacy sys_config below
    }
    try {
      Object sysConfigService = SpringUtils.getBean("sysConfigServiceImpl");
      Method method = sysConfigService.getClass().getMethod("selectConfigByKey", String.class);
      Object value = method.invoke(sysConfigService, CloudConstant.CLOUD_STORAGE_CONFIG_KEY);
      return value == null ? null : value.toString();
    } catch (Exception ignored) {
      return null;
    }
  }
}
