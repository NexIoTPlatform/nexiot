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

package cn.universal.ossm.oss.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.universal.ossm.entity.SysOssConfig;
import cn.universal.ossm.event.SysOssConfigChangedEvent;
import cn.universal.ossm.oss.entity.UploadResult;
import cn.universal.ossm.oss.enumd.CloudServiceEnumd;
import cn.universal.ossm.oss.exception.OssException;
import cn.universal.ossm.oss.factory.OssFactory;
import cn.universal.ossm.oss.service.abstractd.AbstractCloudStorageService;
import cn.universal.ossm.service.ISysOssConfigService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/** MinIO存储 */
@Lazy
@Service
public class MinioCloudStorageServiceImpl extends AbstractCloudStorageService
    implements InitializingBean {

  private static final long PART_SIZE = 10L * 1024 * 1024;

  private final ISysOssConfigService ossConfigService;

  private final Cache<String, MinioClient> minioClientCache =
      Caffeine.newBuilder().maximumSize(128).expireAfterWrite(30, TimeUnit.MINUTES).build();

  private final Cache<String, Boolean> bucketReadyCache =
      Caffeine.newBuilder().maximumSize(1024).expireAfterWrite(30, TimeUnit.MINUTES).build();

  private final ConcurrentHashMap<String, Object> bucketReadyLocks = new ConcurrentHashMap<>();

  public MinioCloudStorageServiceImpl(ISysOssConfigService ossConfigService) {
    this.ossConfigService = ossConfigService;
  }

  @Override
  public void createBucket() {
    try {
      ensureBucket(config());
    } catch (Exception e) {
      throw new OssException("创建MinIO Bucket失败, 请核对MinIO配置信息");
    }
  }

  @Override
  public void testConnection() {
    SysOssConfig properties = config();
    validateRequired(properties);
    try {
      MinioClient client = createClient(properties);
      boolean exists =
          client.bucketExists(
              BucketExistsArgs.builder().bucket(properties.getBucketName()).build());
      if (!exists) {
        throw new OssException("MinIO连接成功，但Bucket不存在或无权限访问");
      }
    } catch (OssException e) {
      throw e;
    } catch (Exception e) {
      throw new OssException("MinIO连接测试失败，请检查Endpoint、AK/SK、Bucket或权限");
    }
  }

  @Override
  public String getServiceType() {
    return CloudServiceEnumd.MINIO.getValue();
  }

  @Override
  public UploadResult upload(byte[] data, String path, String contentType) {
    return upload(new ByteArrayInputStream(data), path, contentType);
  }

  @Override
  public UploadResult upload(InputStream inputStream, String path, String contentType) {
    SysOssConfig properties = config();
    try {
      MinioClient client = createClient(properties);
      client.putObject(
          PutObjectArgs.builder()
              .bucket(properties.getBucketName())
              .object(normalizeObjectName(path))
              .stream(inputStream, -1, PART_SIZE)
              .contentType(normalizeContentType(contentType))
              .build());
      String objectName = normalizeObjectName(path);
      return new UploadResult().setUrl(getEndpointLink(properties) + "/" + objectName).setFilename(objectName);
    } catch (Exception e) {
      throw new OssException("上传文件失败，请检查MinIO配置信息");
    }
  }

  @Override
  public void delete(String path) {
    try {
      SysOssConfig properties = config();
      MinioClient client = createClient(properties);
      client.removeObject(
          RemoveObjectArgs.builder()
              .bucket(properties.getBucketName())
              .object(resolveObjectName(path))
              .build());
    } catch (Exception e) {
      throw new OssException("删除文件失败，请检查MinIO配置信息");
    }
  }

  @Override
  public UploadResult uploadSuffix(byte[] data, String suffix, String contentType) {
    return upload(data, getPath(config().getPrefix(), suffix), contentType);
  }

  @Override
  public UploadResult uploadSuffix(InputStream inputStream, String suffix, String contentType) {
    return upload(inputStream, getPath(config().getPrefix(), suffix), contentType);
  }

  @Override
  public void afterPropertiesSet() {
    OssFactory.register(getServiceType(), this);
  }

  @Override
  public String signedReadUrl(String objectKey, int seconds) {
    try {
      var properties = config();
      return createClient(properties).getPresignedObjectUrl(io.minio.GetPresignedObjectUrlArgs.builder()
          .method(io.minio.http.Method.GET).bucket(properties.getBucketName())
          .object(normalizeObjectName(objectKey)).expiry(Math.max(1, Math.min(seconds, 3600))).build());
    } catch (Exception e) { throw new OssException("生成证据读取链接失败"); }
  }

  @Override
  public String getEndpointLink() {
    return getEndpointLink(config());
  }

  private MinioClient createClient(SysOssConfig properties) {
    return minioClientCache.get(
        minioClientCacheKey(properties),
        key ->
            MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build());
  }

  private SysOssConfig config() {
    return ossConfigService.getRuntimeConfig(getServiceType());
  }

  private void validateRequired(SysOssConfig properties) {
    if (StrUtil.isBlank(properties.getEndpoint())) {
      throw new OssException("MinIO Endpoint不能为空");
    }
    if (StrUtil.isBlank(properties.getAccessKey())) {
      throw new OssException("MinIO AccessKey不能为空");
    }
    if (StrUtil.isBlank(properties.getSecretKey())) {
      throw new OssException("MinIO SecretKey不能为空");
    }
    if (StrUtil.isBlank(properties.getBucketName())) {
      throw new OssException("MinIO Bucket不能为空");
    }
  }

  @EventListener
  public void onOssConfigChanged(SysOssConfigChangedEvent event) {
    if (event == null
        || StrUtil.isBlank(event.getProvider())
        || getServiceType().equals(event.getProvider())) {
      minioClientCache.invalidateAll();
      bucketReadyCache.invalidateAll();
      bucketReadyLocks.clear();
    }
  }

  private void ensureBucket(SysOssConfig properties) {
    validateRequired(properties);
    String cacheKey = bucketReadyCacheKey(properties);
    if (Boolean.TRUE.equals(bucketReadyCache.getIfPresent(cacheKey))) {
      return;
    }
    Object lock = bucketReadyLocks.computeIfAbsent(cacheKey, key -> new Object());
    try {
      synchronized (lock) {
        if (Boolean.TRUE.equals(bucketReadyCache.getIfPresent(cacheKey))) {
          return;
        }
        MinioClient client = createClient(properties);
        boolean exists =
            client.bucketExists(
                BucketExistsArgs.builder().bucket(properties.getBucketName()).build());
        if (!exists) {
          client.makeBucket(MakeBucketArgs.builder().bucket(properties.getBucketName()).build());
        }
        bucketReadyCache.put(cacheKey, Boolean.TRUE);
      }
    } catch (Exception e) {
      throw new OssException("创建MinIO Bucket失败, 请核对MinIO配置信息");
    } finally {
      bucketReadyLocks.remove(cacheKey, lock);
    }
  }

  private String minioClientCacheKey(SysOssConfig properties) {
    String secretKey = properties.getSecretKey();
    String secretFingerprint =
        secretKey == null
            ? ""
            : secretKey.length() + ":" + Integer.toHexString(Objects.hashCode(secretKey));
    return StrUtil.nullToEmpty(properties.getEndpoint())
        + "|"
        + StrUtil.nullToEmpty(properties.getAccessKey())
        + "|"
        + secretFingerprint
        + "|"
        + Boolean.TRUE.equals(properties.getIsHttps());
  }

  private String bucketReadyCacheKey(SysOssConfig properties) {
    return minioClientCacheKey(properties) + "|" + StrUtil.nullToEmpty(properties.getBucketName());
  }

  private String getEndpointLink(SysOssConfig properties) {
    String publicEndpoint = StrUtil.blankToDefault(properties.getPublicEndpoint(), null);
    if (StrUtil.isNotBlank(publicEndpoint)) {
      return normalizePublicEndpoint(publicEndpoint, properties.getBucketName());
    }
    return StrUtil.removeSuffix(properties.getEndpoint(), "/") + "/" + properties.getBucketName();
  }

  private String normalizePublicEndpoint(String endpoint, String bucketName) {
    String value = StrUtil.removeSuffix(endpoint, "/");
    if (StrUtil.isBlank(bucketName) || hasPath(value)) {
      return value;
    }
    return value + "/" + bucketName;
  }

  private boolean hasPath(String endpoint) {
    try {
      String path = new URI(endpoint).getRawPath();
      return StrUtil.isNotBlank(path) && !"/".equals(path);
    } catch (Exception e) {
      String value = endpoint;
      int schemeIndex = value.indexOf("://");
      if (schemeIndex >= 0) {
        value = value.substring(schemeIndex + 3);
      }
      int pathIndex = value.indexOf('/');
      return pathIndex >= 0 && pathIndex < value.length() - 1;
    }
  }

  private String normalizeObjectName(String path) {
    return StrUtil.removePrefix(path, "/");
  }

  private String resolveObjectName(String path) {
    SysOssConfig properties = config();
    String objectName = StrUtil.removePrefix(path, getEndpointLink(properties) + "/");
    objectName =
        StrUtil.removePrefix(objectName, StrUtil.removeSuffix(properties.getEndpoint(), "/") + "/");
    objectName = StrUtil.removePrefix(objectName, properties.getBucketName() + "/");
    return normalizeObjectName(objectName);
  }

  private String normalizeContentType(String contentType) {
    if (StrUtil.isBlank(contentType) || !contentType.contains("/")) {
      return "application/octet-stream";
    }
    return contentType;
  }
}
