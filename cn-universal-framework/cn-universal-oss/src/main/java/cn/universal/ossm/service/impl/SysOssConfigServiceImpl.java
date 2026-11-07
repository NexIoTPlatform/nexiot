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

package cn.universal.ossm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.common.exception.IoTException;
import cn.universal.ossm.entity.SysOssConfig;
import cn.universal.ossm.event.SysOssConfigChangedEvent;
import cn.universal.ossm.mapper.SysOssConfigMapper;
import cn.universal.ossm.oss.context.OssTenantContext;
import cn.universal.ossm.oss.context.OssTenantResolver;
import cn.universal.ossm.oss.enumd.CloudServiceEnumd;
import cn.universal.ossm.oss.factory.OssFactory;
import cn.universal.ossm.oss.properties.CloudStorageProperties;
import cn.universal.ossm.oss.service.ICloudStorageService;
import cn.universal.ossm.oss.util.OssConfigCrypto;
import cn.universal.ossm.service.ISysOssConfigService;
import cn.universal.persistence.common.inteceptor.SQenGenId;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** OSS存储配置服务实现 */
@Service
public class SysOssConfigServiceImpl implements ISysOssConfigService {

  @Resource private SysOssConfigMapper sysOssConfigMapper;

  @Resource private CloudStorageProperties cloudStorageProperties;

  @Resource private ApplicationEventPublisher eventPublisher;

  @Value("${oss.config.crypto-key:}")
  private String cryptoKey;

  private final Cache<String, SysOssConfig> runtimeConfigCache =
      Caffeine.newBuilder().maximumSize(2048).expireAfterWrite(10, TimeUnit.MINUTES).build();

  private final Cache<String, String> activeProviderCache =
      Caffeine.newBuilder().maximumSize(512).expireAfterWrite(10, TimeUnit.MINUTES).build();

  private final ConcurrentHashMap<String, Object> runtimeConfigLoadLocks = new ConcurrentHashMap<>();

  private final ConcurrentHashMap<String, Object> activeProviderLoadLocks = new ConcurrentHashMap<>();

  @Override
  public List<SysOssConfig> listMaskedConfigs() {
    return listMaskedConfigs(OssTenantContext.getTenantId());
  }

  @Override
  public List<SysOssConfig> listMaskedConfigs(String tenantId) {
    String resolvedTenantId = normalizeTenantId(tenantId);
    ensureTenantDefaultConfigs(resolvedTenantId, null);
    List<SysOssConfig> configs = sysOssConfigMapper.listAll(resolvedTenantId);
    Arrays.stream(CloudServiceEnumd.values())
        .map(item -> item.getValue())
        .filter(provider -> configs.stream().noneMatch(item -> provider.equals(item.getProvider())))
        .map(provider -> defaultConfig(provider, resolvedTenantId, null))
        .filter(Objects::nonNull)
        .forEach(configs::add);
    return configs.stream()
        .sorted(Comparator.comparing(SysOssConfig::getProvider))
        .map(config -> mergeFallback(config, resolvedTenantId))
        .map(this::mask)
        .toList();
  }

  @Override
  public SysOssConfig getMaskedConfig(String provider) {
    return getMaskedConfig(provider, OssTenantContext.getTenantId());
  }

  @Override
  public SysOssConfig getMaskedConfig(String provider, String tenantId) {
    return mask(getRuntimeConfig(provider, tenantId));
  }

  @Override
  public SysOssConfig getRuntimeConfig(String provider) {
    return getRuntimeConfig(provider, OssTenantContext.getTenantId());
  }

  @Override
  public SysOssConfig getRuntimeConfig(String provider, String tenantId) {
    provider = normalizeProvider(provider);
    tenantId = normalizeTenantId(tenantId);
    validateProvider(provider);
    String cacheKey = configCacheKey(provider, tenantId);
    SysOssConfig cached = runtimeConfigCache.getIfPresent(cacheKey);
    if (cached != null) {
      return copyConfig(cached);
    }
    Object lock = runtimeConfigLoadLocks.computeIfAbsent(cacheKey, key -> new Object());
    try {
      synchronized (lock) {
        cached = runtimeConfigCache.getIfPresent(cacheKey);
        if (cached != null) {
          return copyConfig(cached);
        }
        SysOssConfig config = loadRuntimeConfig(provider, tenantId);
        runtimeConfigCache.put(cacheKey, copyConfig(config));
        return config;
      }
    } finally {
      runtimeConfigLoadLocks.remove(cacheKey, lock);
    }
  }

  private SysOssConfig loadRuntimeConfig(String provider, String tenantId) {
    if (StrUtil.isNotBlank(tenantId)) {
      ensureTenantDefaultConfigs(tenantId, null);
    }
    SysOssConfig config = sysOssConfigMapper.selectByProvider(provider, tenantId);
    if (config == null) {
      config = fallbackConfig(provider, tenantId);
    }
    if (config == null) {
      throw new IoTException("未配置存储类型: " + provider);
    }
    config = mergeFallback(config, tenantId);
    config.setSecretKey(OssConfigCrypto.decrypt(config.getSecretKey(), cryptoKey));
    return config;
  }

  @Override
  public String getActiveProvider() {
    return getActiveProvider(OssTenantContext.getTenantId());
  }

  @Override
  public String getActiveProvider(String tenantId) {
    tenantId = normalizeTenantId(tenantId);
    String cacheKey = activeProviderCacheKey(tenantId);
    String cached = activeProviderCache.getIfPresent(cacheKey);
    if (StrUtil.isNotBlank(cached)) {
      return cached;
    }
    Object lock = activeProviderLoadLocks.computeIfAbsent(cacheKey, key -> new Object());
    try {
      synchronized (lock) {
        cached = activeProviderCache.getIfPresent(cacheKey);
        if (StrUtil.isNotBlank(cached)) {
          return cached;
        }
        String activeProvider = loadActiveProvider(tenantId);
        activeProviderCache.put(cacheKey, activeProvider);
        return activeProvider;
      }
    } finally {
      activeProviderLoadLocks.remove(cacheKey, lock);
    }
  }

  private String loadActiveProvider(String tenantId) {
    if (StrUtil.isNotBlank(tenantId)) {
      ensureTenantDefaultConfigs(tenantId, null);
    }
    SysOssConfig active = sysOssConfigMapper.selectActive(tenantId);
    if (active != null && StrUtil.isNotBlank(active.getProvider())) {
      if (!isPlaceholderConfig(active)) {
        return active.getProvider();
      }
      String inheritedProvider = inheritedActiveProvider(tenantId);
      if (StrUtil.isNotBlank(inheritedProvider)) {
        return inheritedProvider;
      }
    }
    return defaultActiveProvider();
  }

  @Override
  public SysOssConfig saveConfig(SysOssConfig config, String operator) {
    return saveConfig(config, operator, OssTenantContext.getTenantId());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public SysOssConfig saveConfig(SysOssConfig config, String operator, String tenantId) {
    if (config == null) {
      throw new IoTException("OSS配置不能为空");
    }
    String provider = normalizeProvider(config.getProvider());
    tenantId = normalizeTenantId(tenantId);
    validateProvider(provider);
    SysOssConfig old = sysOssConfigMapper.selectByProvider(provider, tenantId);
    SysOssConfig target = normalize(config);
    target.setProvider(provider);
    target.setTenantId(tenantId);
    if (old != null && OssConfigCrypto.isMasked(target.getSecretKey())) {
      target.setSecretKey(old.getSecretKey());
    } else if (StrUtil.isNotBlank(target.getSecretKey())) {
      target.setSecretKey(encryptIfNeeded(target.getSecretKey()));
    }
    if (old == null) {
      target.setConfigId(SQenGenId.nextId());
      target.setActive(Boolean.FALSE);
      target.setCreateBy(operator);
      target.setCreateTime(new Date());
      sysOssConfigMapper.insertConfig(target);
    } else {
      target.setConfigId(old.getConfigId());
      target.setActive(old.getActive());
      target.setUpdateBy(operator);
      target.setUpdateTime(new Date());
      sysOssConfigMapper.updateConfig(target);
    }
    evictRuntimeCaches(provider, tenantId);
    return mask(loadRuntimeConfig(provider, tenantId));
  }

  @Override
  public void activate(String provider, String operator) {
    activate(provider, operator, OssTenantContext.getTenantId());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void activate(String provider, String operator, String tenantId) {
    provider = normalizeProvider(provider);
    tenantId = normalizeTenantId(tenantId);
    validateProvider(provider);
    getRuntimeConfig(provider, tenantId);
    SysOssConfig old = sysOssConfigMapper.selectByProvider(provider, tenantId);
    if (old == null) {
      SysOssConfig fallback = fallbackConfig(provider, tenantId);
      if (fallback == null) {
        throw new IoTException("未配置存储类型: " + provider);
      }
      fallback.setConfigId(SQenGenId.nextId());
      fallback.setTenantId(tenantId);
      fallback.setSecretKey(encryptIfNeeded(fallback.getSecretKey()));
      fallback.setActive(Boolean.FALSE);
      fallback.setCreateBy(operator);
      fallback.setCreateTime(new Date());
      sysOssConfigMapper.insertConfig(fallback);
    }
    sysOssConfigMapper.clearActive(tenantId);
    sysOssConfigMapper.activateProvider(provider, tenantId);
    if (OssTenantResolver.GLOBAL_TENANT_ID.equals(tenantId)) {
      OssFactory.setCurrentType(provider);
    }
    evictRuntimeCaches(null, tenantId);
  }

  @Override
  public void test(String provider) {
    test(provider, OssTenantContext.getTenantId());
  }

  @Override
  public void test(String provider, String tenantId) {
    provider = normalizeProvider(provider);
    String resolvedTenantId = normalizeTenantId(tenantId);
    validateProvider(provider);
    String resolvedProvider = provider;
    OssTenantContext.run(
        resolvedTenantId,
        () -> {
          ICloudStorageService storage = OssFactory.instance(resolvedProvider);
          storage.testConnection();
        });
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void ensureTenantDefaultConfigs(String tenantId, String operator) {
    tenantId = normalizeTenantId(tenantId);
    String createBy = StrUtil.blankToDefault(operator, "system");
    String activeProvider = defaultActiveProviderForTenant(tenantId);
    for (CloudServiceEnumd provider : CloudServiceEnumd.values()) {
      if (sysOssConfigMapper.selectByProvider(provider.getValue(), tenantId) != null) {
        continue;
      }
      SysOssConfig config =
          defaultConfig(provider.getValue(), tenantId, provider.getValue().equals(activeProvider));
      config.setConfigId(SQenGenId.nextId());
      config.setCreateBy(createBy);
      config.setCreateTime(new Date());
      sysOssConfigMapper.insertConfig(config);
      evictRuntimeCaches(provider.getValue(), tenantId);
    }
  }

  @Override
  public void clearRuntimeCache() {
    evictRuntimeCaches(null, null);
  }

  private void evictRuntimeCaches(String provider, String tenantId) {
    doEvictRuntimeCaches(provider, tenantId);
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              doEvictRuntimeCaches(provider, tenantId);
            }
          });
    }
  }

  private void doEvictRuntimeCaches(String provider, String tenantId) {
    String normalizedProvider = StrUtil.isBlank(provider) ? null : normalizeProvider(provider);
    String normalizedTenantId = tenantId == null ? null : normalizeTenantId(tenantId);
    boolean globalTenantChanged = tenantId != null && StrUtil.isBlank(normalizedTenantId);
    if (globalTenantChanged && StrUtil.isBlank(normalizedProvider)) {
      runtimeConfigCache.invalidateAll();
    } else if (StrUtil.isNotBlank(normalizedProvider) && normalizedTenantId != null && !globalTenantChanged) {
      runtimeConfigCache.invalidate(configCacheKey(normalizedProvider, normalizedTenantId));
    } else if (StrUtil.isNotBlank(normalizedProvider)) {
      runtimeConfigCache
          .asMap()
          .keySet()
          .removeIf(key -> key.startsWith(normalizedProvider + "|"));
    } else if (normalizedTenantId != null) {
      runtimeConfigCache
          .asMap()
          .keySet()
          .removeIf(key -> key.endsWith("|" + normalizedTenantId));
    } else {
      runtimeConfigCache.invalidateAll();
    }
    if (normalizedTenantId != null && !globalTenantChanged) {
      activeProviderCache.invalidate(activeProviderCacheKey(normalizedTenantId));
    } else {
      activeProviderCache.invalidateAll();
    }
    eventPublisher.publishEvent(new SysOssConfigChangedEvent(normalizedProvider, normalizedTenantId));
  }

  private void validateProvider(String provider) {
    if (CloudServiceEnumd.getServiceClass(provider) == null) {
      throw new IoTException(
          StrUtil.isBlank(provider) ? "存储类型不能为空" : "不支持的存储类型: " + provider);
    }
  }

  private String normalizeProvider(String provider) {
    String value = StrUtil.trim(provider);
    return value == null ? null : value.toLowerCase(Locale.ROOT);
  }

  private String normalizeTenantId(String tenantId) {
    return OssTenantResolver.normalizeConfigTenantId(tenantId);
  }

  private String configCacheKey(String provider, String tenantId) {
    return provider + "|" + StrUtil.nullToEmpty(tenantId);
  }

  private String activeProviderCacheKey(String tenantId) {
    return StrUtil.nullToEmpty(tenantId);
  }

  private SysOssConfig copyConfig(SysOssConfig config) {
    return config == null ? null : BeanUtil.copyProperties(config, SysOssConfig.class);
  }

  private SysOssConfig normalize(SysOssConfig config) {
    return BeanUtil.copyProperties(config, SysOssConfig.class)
        .setProvider(StrUtil.trim(config.getProvider()))
        .setEndpoint(StrUtil.trim(config.getEndpoint()))
        .setTenantId(normalizeTenantId(config.getTenantId()))
        .setPublicEndpoint(StrUtil.trim(config.getPublicEndpoint()))
        .setDomain(StrUtil.trim(config.getDomain()))
        .setPrefix(StrUtil.trim(config.getPrefix()))
        .setAccessKey(StrUtil.trim(config.getAccessKey()))
        .setSecretKey(StrUtil.trim(config.getSecretKey()))
        .setBucketName(StrUtil.trim(config.getBucketName()))
        .setRegion(StrUtil.trim(config.getRegion()));
  }

  private SysOssConfig mask(SysOssConfig config) {
    if (config == null) {
      return null;
    }
    SysOssConfig masked = BeanUtil.copyProperties(config, SysOssConfig.class);
    if (StrUtil.isNotBlank(masked.getSecretKey())) {
      masked.setSecretKey(OssConfigCrypto.MASK);
    }
    return masked;
  }

  private SysOssConfig mergeFallback(SysOssConfig config, String tenantId) {
    if (config == null) {
      return null;
    }
    SysOssConfig merged = BeanUtil.copyProperties(config, SysOssConfig.class);
    boolean inheritConnectionOptions = isPlaceholderConfig(merged);
    SysOssConfig fallback = fallbackConfig(config.getProvider(), tenantId);
    if (fallback == null) {
      return merged;
    }
    merged
        .setEndpoint(StrUtil.blankToDefault(merged.getEndpoint(), fallback.getEndpoint()))
        .setPublicEndpoint(
            StrUtil.blankToDefault(merged.getPublicEndpoint(), fallback.getPublicEndpoint()))
        .setDomain(StrUtil.blankToDefault(merged.getDomain(), fallback.getDomain()))
        .setPrefix(merged.getPrefix() == null ? fallback.getPrefix() : merged.getPrefix())
        .setAccessKey(StrUtil.blankToDefault(merged.getAccessKey(), fallback.getAccessKey()))
        .setSecretKey(StrUtil.blankToDefault(merged.getSecretKey(), fallback.getSecretKey()))
        .setBucketName(StrUtil.blankToDefault(merged.getBucketName(), fallback.getBucketName()))
        .setRegion(StrUtil.blankToDefault(merged.getRegion(), fallback.getRegion()));
    if (inheritConnectionOptions || merged.getIsHttps() == null) {
      merged.setIsHttps(fallback.getIsHttps());
    }
    return merged;
  }

  private SysOssConfig fallbackConfig(String provider, String tenantId) {
    if (StrUtil.isNotBlank(tenantId)) {
      if (!OssTenantResolver.GLOBAL_TENANT_ID.equals(tenantId)) {
        SysOssConfig globalConfig =
            sysOssConfigMapper.selectByProvider(provider, OssTenantResolver.GLOBAL_TENANT_ID);
        if (globalConfig != null) {
          return mergeConfig(globalConfig, propertyConfig(provider, OssTenantResolver.GLOBAL_TENANT_ID));
        }
      }
      SysOssConfig propertyConfig = propertyConfig(provider, tenantId);
      return propertyConfig == null ? defaultConfig(provider, tenantId, null) : propertyConfig;
    }
    return propertyConfig(provider, tenantId);
  }

  private SysOssConfig propertyConfig(String provider, String tenantId) {
    if (CloudServiceEnumd.QINIU.getValue().equals(provider) && cloudStorageProperties.getQiniu() != null) {
      CloudStorageProperties.QiniuProperties cfg = cloudStorageProperties.getQiniu();
      return SysOssConfig.builder()
          .provider(provider)
          .tenantId(tenantId)
          .domain(cfg.getDomain())
          .prefix(cfg.getPrefix())
          .accessKey(cfg.getAccessKey())
          .secretKey(cfg.getSecretKey())
          .bucketName(cfg.getBucketName())
          .region(cfg.getRegion())
          .isHttps(cfg.getIsHttps())
          .active(provider.equals(getActiveProviderFromDbOrProperties(tenantId)))
          .build();
    }
    if (CloudServiceEnumd.ALIYUN.getValue().equals(provider) && cloudStorageProperties.getAliyun() != null) {
      CloudStorageProperties.AliyunProperties cfg = cloudStorageProperties.getAliyun();
      return SysOssConfig.builder()
          .provider(provider)
          .tenantId(tenantId)
          .endpoint(cfg.getEndpoint())
          .prefix(cfg.getPrefix())
          .accessKey(cfg.getAccessKeyId())
          .secretKey(cfg.getAccessKeySecret())
          .bucketName(cfg.getBucketName())
          .active(provider.equals(getActiveProviderFromDbOrProperties(tenantId)))
          .build();
    }
    if (CloudServiceEnumd.MINIO.getValue().equals(provider) && cloudStorageProperties.getMinio() != null) {
      CloudStorageProperties.MinioProperties cfg = cloudStorageProperties.getMinio();
      return SysOssConfig.builder()
          .provider(provider)
          .tenantId(tenantId)
          .endpoint(cfg.getEndpoint())
          .publicEndpoint(cfg.getPublicEndpoint())
          .prefix(cfg.getPrefix())
          .accessKey(cfg.getAccessKey())
          .secretKey(cfg.getSecretKey())
          .bucketName(cfg.getBucketName())
          .active(provider.equals(getActiveProviderFromDbOrProperties(tenantId)))
          .build();
    }
    if (CloudServiceEnumd.QCLOUD.getValue().equals(provider) && cloudStorageProperties.getQcloud() != null) {
      CloudStorageProperties.QcloudProperties cfg = cloudStorageProperties.getQcloud();
      return SysOssConfig.builder()
          .provider(provider)
          .tenantId(tenantId)
          .endpoint(cfg.getEndpoint())
          .prefix(cfg.getPrefix())
          .accessKey(cfg.getSecretId())
          .secretKey(cfg.getSecretKey())
          .bucketName(cfg.getBucketName())
          .region(cfg.getRegion())
          .isHttps(cfg.getIsHttps())
          .active(provider.equals(getActiveProviderFromDbOrProperties(tenantId)))
          .build();
    }
    return null;
  }

  private SysOssConfig mergeConfig(SysOssConfig config, SysOssConfig fallback) {
    if (config == null || fallback == null) {
      return config;
    }
    SysOssConfig merged = BeanUtil.copyProperties(config, SysOssConfig.class);
    boolean inheritConnectionOptions = isPlaceholderConfig(merged);
    merged
        .setEndpoint(StrUtil.blankToDefault(merged.getEndpoint(), fallback.getEndpoint()))
        .setPublicEndpoint(
            StrUtil.blankToDefault(merged.getPublicEndpoint(), fallback.getPublicEndpoint()))
        .setDomain(StrUtil.blankToDefault(merged.getDomain(), fallback.getDomain()))
        .setPrefix(merged.getPrefix() == null ? fallback.getPrefix() : merged.getPrefix())
        .setAccessKey(StrUtil.blankToDefault(merged.getAccessKey(), fallback.getAccessKey()))
        .setSecretKey(StrUtil.blankToDefault(merged.getSecretKey(), fallback.getSecretKey()))
        .setBucketName(StrUtil.blankToDefault(merged.getBucketName(), fallback.getBucketName()))
        .setRegion(StrUtil.blankToDefault(merged.getRegion(), fallback.getRegion()));
    if (inheritConnectionOptions || merged.getIsHttps() == null) {
      merged.setIsHttps(fallback.getIsHttps());
    }
    return merged;
  }

  private String encryptIfNeeded(String secretKey) {
    if (StrUtil.isBlank(secretKey) || OssConfigCrypto.isMasked(secretKey) || secretKey.startsWith("enc:")) {
      return secretKey;
    }
    return OssConfigCrypto.encrypt(secretKey, cryptoKey);
  }

  private SysOssConfig defaultConfig(String provider, String tenantId, Boolean active) {
    if (CloudServiceEnumd.getServiceClass(provider) == null) {
      return null;
    }
    return SysOssConfig.builder()
        .provider(provider)
        .tenantId(tenantId)
        .prefix(defaultPrefix(provider))
        .region(defaultRegion(provider))
        .isHttps(Boolean.TRUE)
        .active(active == null ? Boolean.FALSE : active)
        .remark("租户对象存储配置，请在后台对象存储页面补齐")
        .build();
  }

  private String defaultPrefix(String provider) {
    if (CloudServiceEnumd.MINIO.getValue().equals(provider)
        || CloudServiceEnumd.QCLOUD.getValue().equals(provider)) {
      return "nexiot";
    }
    return "";
  }

  private String defaultRegion(String provider) {
    if (CloudServiceEnumd.QINIU.getValue().equals(provider)) {
      return "auto";
    }
    if (CloudServiceEnumd.QCLOUD.getValue().equals(provider)) {
      return "ap-shanghai";
    }
    return "";
  }

  private String getActiveProviderFromDbOrProperties(String tenantId) {
    SysOssConfig active = sysOssConfigMapper.selectActive(tenantId);
    return active == null ? defaultActiveProvider() : active.getProvider();
  }

  private String defaultActiveProviderForTenant(String tenantId) {
    String inheritedProvider = inheritedActiveProvider(tenantId);
    return StrUtil.blankToDefault(inheritedProvider, defaultActiveProvider());
  }

  private String inheritedActiveProvider(String tenantId) {
    if (StrUtil.isBlank(tenantId) || OssTenantResolver.GLOBAL_TENANT_ID.equals(tenantId)) {
      return null;
    }
    SysOssConfig globalActive = sysOssConfigMapper.selectActive(OssTenantResolver.GLOBAL_TENANT_ID);
    return globalActive == null ? null : globalActive.getProvider();
  }

  private String defaultActiveProvider() {
    String provider = StrUtil.blankToDefault(cloudStorageProperties.getType(), CloudServiceEnumd.QINIU.getValue());
    return CloudServiceEnumd.getServiceClass(provider) == null
        ? CloudServiceEnumd.QINIU.getValue()
        : provider;
  }

  private boolean isPlaceholderConfig(SysOssConfig config) {
    return config != null
        && StrUtil.isBlank(config.getEndpoint())
        && StrUtil.isBlank(config.getPublicEndpoint())
        && StrUtil.isBlank(config.getDomain())
        && StrUtil.isBlank(config.getAccessKey())
        && StrUtil.isBlank(config.getSecretKey())
        && StrUtil.isBlank(config.getBucketName());
  }
}
