/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 腾讯云COS存储实现
 *
 */

package cn.universal.ossm.oss.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.universal.ossm.entity.SysOssConfig;
import cn.universal.ossm.oss.entity.UploadResult;
import cn.universal.ossm.oss.enumd.CloudServiceEnumd;
import cn.universal.ossm.oss.exception.OssException;
import cn.universal.ossm.oss.factory.OssFactory;
import cn.universal.ossm.oss.service.abstractd.AbstractCloudStorageService;
import cn.universal.ossm.service.ISysOssConfigService;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.CreateBucketRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/** 腾讯云COS存储 */
@Lazy
@Service
public class QcloudCloudStorageServiceImpl extends AbstractCloudStorageService
    implements InitializingBean {

  private final ISysOssConfigService ossConfigService;

  public QcloudCloudStorageServiceImpl(ISysOssConfigService ossConfigService) {
    this.ossConfigService = ossConfigService;
  }

  @Override
  public void createBucket() {
    COSClient client = null;
    try {
      SysOssConfig properties = config();
      client = createClient(properties);
      String bucketName = properties.getBucketName();
      if (client.doesBucketExist(bucketName)) {
        return;
      }
      client.createBucket(new CreateBucketRequest(bucketName));
      client.setBucketAcl(bucketName, CannedAccessControlList.PublicRead);
    } catch (Exception e) {
      throw new OssException("创建腾讯云COS Bucket失败, 请核对腾讯云配置信息");
    } finally {
      shutdown(client);
    }
  }

  @Override
  public void testConnection() {
    SysOssConfig properties = config();
    validateRequired(properties);
    COSClient client = null;
    try {
      client = createClient(properties);
      if (!client.doesBucketExist(properties.getBucketName())) {
        throw new OssException("腾讯云COS连接成功，但Bucket不存在或无权限访问");
      }
    } catch (OssException e) {
      throw e;
    } catch (Exception e) {
      throw new OssException("腾讯云COS连接测试失败，请检查Region、AK/SK、Bucket或权限");
    } finally {
      shutdown(client);
    }
  }

  @Override
  public String getServiceType() {
    return CloudServiceEnumd.QCLOUD.getValue();
  }

  @Override
  public UploadResult upload(byte[] data, String path, String contentType) {
    return upload(new ByteArrayInputStream(data), path, contentType);
  }

  @Override
  public UploadResult upload(InputStream inputStream, String path, String contentType) {
    COSClient client = null;
    SysOssConfig properties = config();
    String objectName = normalizeObjectName(path);
    try {
      client = createClient(properties);
      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentType(normalizeContentType(contentType));
      client.putObject(
          new PutObjectRequest(properties.getBucketName(), objectName, inputStream, metadata));
    } catch (Exception e) {
      throw new OssException("上传文件失败，请检查腾讯云COS配置信息");
    } finally {
      shutdown(client);
    }
    return new UploadResult()
        .setUrl(getEndpointLink(properties) + "/" + objectName)
        .setFilename(objectName);
  }

  @Override
  public void delete(String path) {
    COSClient client = null;
    try {
      SysOssConfig properties = config();
      client = createClient(properties);
      client.deleteObject(properties.getBucketName(), resolveObjectName(properties, path));
    } catch (Exception e) {
      throw new OssException("删除文件失败，请检查腾讯云COS配置信息");
    } finally {
      shutdown(client);
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
    var properties = config();
    COSClient client = createClient(properties);
    try { return client.generatePresignedUrl(properties.getBucketName(), objectKey,
        new java.util.Date(System.currentTimeMillis() + Math.max(1, Math.min(seconds, 3600)) * 1000L)).toString(); }
    finally { shutdown(client); }
  }

  @Override
  public String getEndpointLink() {
    return getEndpointLink(config());
  }

  private COSClient createClient(SysOssConfig properties) {
    if (StrUtil.isBlank(properties.getRegion())) {
      throw new OssException("腾讯云COS region不能为空");
    }
    COSCredentials credentials =
        new BasicCOSCredentials(properties.getAccessKey(), properties.getSecretKey());
    ClientConfig clientConfig = new ClientConfig(new Region(properties.getRegion()));
    clientConfig.setHttpProtocol(Boolean.FALSE.equals(properties.getIsHttps()) ? HttpProtocol.http : HttpProtocol.https);
    return new COSClient(credentials, clientConfig);
  }

  private SysOssConfig config() {
    return ossConfigService.getRuntimeConfig(getServiceType());
  }

  private void validateRequired(SysOssConfig properties) {
    if (StrUtil.isBlank(properties.getRegion())) {
      throw new OssException("腾讯云COS Region不能为空");
    }
    if (StrUtil.isBlank(properties.getAccessKey())) {
      throw new OssException("腾讯云COS SecretId不能为空");
    }
    if (StrUtil.isBlank(properties.getSecretKey())) {
      throw new OssException("腾讯云COS SecretKey不能为空");
    }
    if (StrUtil.isBlank(properties.getBucketName())) {
      throw new OssException("腾讯云COS Bucket不能为空");
    }
  }

  private String getEndpointLink(SysOssConfig properties) {
    String publicEndpoint = StrUtil.blankToDefault(properties.getPublicEndpoint(), null);
    if (StrUtil.isNotBlank(publicEndpoint)) {
      return StrUtil.removeSuffix(publicEndpoint, "/");
    }
    String endpoint = StrUtil.blankToDefault(properties.getEndpoint(), null);
    if (StrUtil.isNotBlank(endpoint)) {
      return normalizeEndpoint(endpoint, properties);
    }
    String protocol = Boolean.FALSE.equals(properties.getIsHttps()) ? "http://" : "https://";
    return protocol
        + properties.getBucketName()
        + ".cos."
        + properties.getRegion()
        + ".myqcloud.com";
  }

  private String normalizeEndpoint(String endpoint, SysOssConfig properties) {
    String value = StrUtil.removeSuffix(endpoint, "/");
    if (StrUtil.startWithIgnoreCase(value, "http://")
        || StrUtil.startWithIgnoreCase(value, "https://")) {
      return value;
    }
    String protocol = Boolean.FALSE.equals(properties.getIsHttps()) ? "http://" : "https://";
    return protocol + value;
  }

  private String normalizeObjectName(String path) {
    return StrUtil.removePrefix(path, "/");
  }

  private String resolveObjectName(SysOssConfig properties, String path) {
    String objectName = StrUtil.removePrefix(path, getEndpointLink(properties) + "/");
    return normalizeObjectName(objectName);
  }

  private String normalizeContentType(String contentType) {
    if (StrUtil.isBlank(contentType) || !contentType.contains("/")) {
      return "application/octet-stream";
    }
    return contentType;
  }

  private void shutdown(COSClient client) {
    if (client != null) {
      client.shutdown();
    }
  }
}
