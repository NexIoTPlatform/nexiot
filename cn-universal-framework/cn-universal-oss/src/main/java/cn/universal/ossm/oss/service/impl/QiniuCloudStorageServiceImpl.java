/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 七牛云存储实现
 *
 */

package cn.universal.ossm.oss.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.universal.common.exception.IoTException;
import cn.universal.ossm.entity.SysOssConfig;
import cn.universal.ossm.oss.entity.UploadResult;
import cn.universal.ossm.oss.enumd.CloudServiceEnumd;
import cn.universal.ossm.oss.exception.OssException;
import cn.universal.ossm.oss.factory.OssFactory;
import cn.universal.ossm.oss.service.ICloudStorageService;
import cn.universal.ossm.service.ISysOssConfigService;
import com.qiniu.common.QiniuException;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.springframework.stereotype.Service;

@Service
public class QiniuCloudStorageServiceImpl implements ICloudStorageService {

  @Resource private ISysOssConfigService ossConfigService;

  private UploadManager uploadManager;

  @PostConstruct
  public void init() {
    Region region = Region.autoRegion();
    Configuration configuration = new Configuration(region);
    this.uploadManager = new UploadManager(configuration);
    OssFactory.register(getServiceType(), this);
  }

  @Override
  public void createBucket() {}

  @Override
  public void testConnection() {
    SysOssConfig cfg = config();
    validateRequired(cfg);
    try {
      Auth auth = Auth.create(cfg.getAccessKey(), cfg.getSecretKey());
      BucketManager bucketManager = new BucketManager(auth, new Configuration(Region.autoRegion()));
      bucketManager.getBucketInfo(cfg.getBucketName());
    } catch (QiniuException e) {
      throw new OssException("七牛云连接测试失败，请检查AK/SK、Bucket或权限: " + e.code());
    } catch (Exception e) {
      throw new OssException("七牛云连接测试失败，请检查七牛云配置信息");
    }
  }

  @Override
  public String getServiceType() {
    return CloudServiceEnumd.QINIU.getValue();
  }

  @Override
  public String getPath(String prefix, String suffix) {
    StringBuilder path = new StringBuilder();
    if (StrUtil.isNotBlank(prefix)) {
      path.append(prefix);
    }
    path.append(System.currentTimeMillis());
    if (StrUtil.isNotBlank(suffix)) {
      path.append(suffix);
    }
    return path.toString();
  }

  private String getUpToken() {
    SysOssConfig cfg = config();
    Auth auth = Auth.create(cfg.getAccessKey(), cfg.getSecretKey());
    StringMap putPolicy = new StringMap();
    return auth.uploadToken(cfg.getBucketName(), null, 3600, putPolicy);
  }

  private String buildUrl(String key) {
    return endpointLink(config()) + "/" + key;
  }

  private String endpointLink(SysOssConfig cfg) {
    String domain = StrUtil.trim(StrUtil.blankToDefault(cfg.getPublicEndpoint(), cfg.getDomain()));
    if (Boolean.TRUE.equals(cfg.getIsHttps())) {
      domain = domain.replaceFirst("^http://", "https://");
    }
    if (!domain.startsWith("http")) {
      domain = (Boolean.TRUE.equals(cfg.getIsHttps()) ? "https://" : "http://") + domain;
    }
    return StrUtil.removeSuffix(domain, "/");
  }

  @Override
  public UploadResult upload(byte[] data, String path, String contentType) {
    try {
      String token = getUpToken();
      String key = path;
      com.qiniu.http.Response response =
          uploadManager.put(new ByteArrayInputStream(data), key, token, null, contentType);
      if (!response.isOK()) {
        throw new IoTException("七牛上传失败:" + response.toString());
      }
      DefaultPutRet putRet = JSONUtil.toBean(response.bodyString(), DefaultPutRet.class);
      String url = buildUrl(putRet.key);
      return new UploadResult().setUrl(url).setFilename(putRet.key);
    } catch (Exception e) {
      throw new IoTException("七牛上传异常", e);
    }
  }

  @Override
  public void delete(String path) {
    // 可按需实现：调用 BucketManager 删除
  }

  @Override
  public UploadResult uploadSuffix(byte[] data, String suffix, String contentType) {
    String path = getPath(config().getPrefix(), suffix);
    return upload(data, path, contentType);
  }

  @Override
  public UploadResult upload(InputStream inputStream, String path, String contentType) {
    try {
      String token = getUpToken();
      com.qiniu.http.Response response = uploadManager.put(inputStream, path, token, null, contentType);
      if (!response.isOK()) {
        throw new IoTException("七牛上传失败:" + response);
      }
      DefaultPutRet putRet = JSONUtil.toBean(response.bodyString(), DefaultPutRet.class);
      return new UploadResult().setUrl(buildUrl(putRet.key)).setFilename(putRet.key);
    } catch (Exception e) {
      throw new IoTException("七牛上传异常", e);
    }
  }

  @Override
  public UploadResult uploadSuffix(InputStream inputStream, String suffix, String contentType) {
    String path = getPath(config().getPrefix(), suffix);
    return upload(inputStream, path, contentType);
  }

  @Override
  public String signedReadUrl(String objectKey, int seconds) {
    var properties = config();
    return Auth.create(properties.getAccessKey(), properties.getSecretKey())
        .privateDownloadUrl(buildUrl(objectKey), Math.max(1, Math.min(seconds, 3600)));
  }

  @Override
  public String getEndpointLink() {
    return endpointLink(config());
  }

  private SysOssConfig config() {
    return ossConfigService.getRuntimeConfig(getServiceType());
  }

  private void validateRequired(SysOssConfig cfg) {
    if (StrUtil.isBlank(cfg.getAccessKey())) {
      throw new OssException("七牛云AccessKey不能为空");
    }
    if (StrUtil.isBlank(cfg.getSecretKey())) {
      throw new OssException("七牛云SecretKey不能为空");
    }
    if (StrUtil.isBlank(cfg.getBucketName())) {
      throw new OssException("七牛云Bucket不能为空");
    }
  }
}
