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
import cn.universal.ossm.oss.entity.UploadResult;
import cn.universal.ossm.oss.enumd.CloudServiceEnumd;
import cn.universal.ossm.oss.exception.OssException;
import cn.universal.ossm.oss.factory.OssFactory;
import cn.universal.ossm.oss.service.abstractd.AbstractCloudStorageService;
import cn.universal.ossm.service.ISysOssConfigService;
import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.CreateBucketRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/** 阿里云存储 @Author Lion Li */
@Lazy
@Service
public class AliyunCloudStorageServiceImpl extends AbstractCloudStorageService
    implements InitializingBean {

  private final ISysOssConfigService ossConfigService;

  public AliyunCloudStorageServiceImpl(ISysOssConfigService ossConfigService) {
    this.ossConfigService = ossConfigService;
  }

  @Override
  public void createBucket() {
    try {
      SysOssConfig properties = config();
      OSSClient client = createClient(properties);
      String bucketName = properties.getBucketName();
      if (client.doesBucketExist(bucketName)) {
        return;
      }
      CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
      createBucketRequest.setCannedACL(CannedAccessControlList.PublicRead);
      client.createBucket(createBucketRequest);
    } catch (Exception e) {
      throw new OssException("创建Bucket失败, 请核对阿里云配置信息");
    }
  }

  @Override
  public void testConnection() {
    SysOssConfig properties = config();
    validateRequired(properties);
    OSSClient client = null;
    try {
      client = createClient(properties);
      if (!client.doesBucketExist(properties.getBucketName())) {
        throw new OssException("阿里云OSS连接成功，但Bucket不存在或无权限访问");
      }
    } catch (OssException e) {
      throw e;
    } catch (Exception e) {
      throw new OssException("阿里云OSS连接测试失败，请检查Endpoint、AK/SK、Bucket或权限");
    } finally {
      if (client != null) {
        client.shutdown();
      }
    }
  }

  @Override
  public String getServiceType() {
    return CloudServiceEnumd.ALIYUN.getValue();
  }

  @Override
  public UploadResult upload(byte[] data, String path, String contentType) {
    return upload(new ByteArrayInputStream(data), path, contentType);
  }

  @Override
  public UploadResult upload(InputStream inputStream, String path, String contentType) {
    SysOssConfig properties = config();
    try {
      OSSClient client = createClient(properties);
      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentType(contentType);
      client.putObject(
          new PutObjectRequest(properties.getBucketName(), path, inputStream, metadata));
    } catch (Exception e) {
      throw new OssException("上传文件失败，请检查阿里云配置信息");
    }
    return new UploadResult().setUrl(getEndpointLink(properties) + "/" + path).setFilename(path);
  }

  @Override
  public void delete(String path) {
    try {
      SysOssConfig properties = config();
      OSSClient client = createClient(properties);
      path = path.replace(getEndpointLink(properties) + "/", "");
      client.deleteObject(properties.getBucketName(), path);
    } catch (Exception e) {
      throw new OssException("上传文件失败，请检查阿里云配置信息");
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
  public void afterPropertiesSet() throws Exception {
    OssFactory.register(getServiceType(), this);
  }

  @Override
  public String signedReadUrl(String objectKey, int seconds) {
    var properties = config();
    OSSClient client = createClient(properties);
    try { return client.generatePresignedUrl(properties.getBucketName(), objectKey,
        new java.util.Date(System.currentTimeMillis() + Math.max(1, Math.min(seconds, 3600)) * 1000L)).toString(); }
    finally { client.shutdown(); }
  }

  @Override
  public String getEndpointLink() {
    return getEndpointLink(config());
  }

  private OSSClient createClient(SysOssConfig properties) {
    try {
      ClientConfiguration configuration = new ClientConfiguration();
      DefaultCredentialProvider credentialProvider =
          new DefaultCredentialProvider(properties.getAccessKey(), properties.getSecretKey());
      return new OSSClient(properties.getEndpoint(), credentialProvider, configuration);
    } catch (Exception e) {
      throw new OssException("阿里云存储配置错误! 请检查系统配置!");
    }
  }

  private SysOssConfig config() {
    return ossConfigService.getRuntimeConfig(getServiceType());
  }

  private void validateRequired(SysOssConfig properties) {
    if (StrUtil.isBlank(properties.getEndpoint())) {
      throw new OssException("阿里云OSSEndpoint不能为空");
    }
    if (StrUtil.isBlank(properties.getAccessKey())) {
      throw new OssException("阿里云OSSAccessKey不能为空");
    }
    if (StrUtil.isBlank(properties.getSecretKey())) {
      throw new OssException("阿里云OSSSecretKey不能为空");
    }
    if (StrUtil.isBlank(properties.getBucketName())) {
      throw new OssException("阿里云OSSBucket不能为空");
    }
  }

  private String getEndpointLink(SysOssConfig properties) {
    if (StrUtil.isNotBlank(properties.getPublicEndpoint())) {
      return StrUtil.removeSuffix(properties.getPublicEndpoint(), "/");
    }
    String endpoint = properties.getEndpoint();
    StringBuilder sb = new StringBuilder(endpoint);
    if (StrUtil.containsAnyIgnoreCase(endpoint, "http://")) {
      sb.insert(7, properties.getBucketName() + ".");
    } else if (StrUtil.containsAnyIgnoreCase(endpoint, "https://")) {
      sb.insert(8, properties.getBucketName() + ".");
    } else {
      throw new OssException("Endpoint配置错误");
    }
    return sb.toString();
  }
}
