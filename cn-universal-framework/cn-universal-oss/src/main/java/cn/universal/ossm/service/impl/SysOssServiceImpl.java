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

import cn.hutool.core.util.StrUtil;
import cn.universal.common.exception.IoTException;
import cn.universal.ossm.entity.SysOss;
import cn.universal.ossm.entity.bo.SysOssBo;
import cn.universal.ossm.mapper.SysOssMapper;
import cn.universal.ossm.oss.context.OssTenantContext;
import cn.universal.ossm.oss.context.OssTenantResolver;
import cn.universal.ossm.oss.entity.UploadResult;
import cn.universal.ossm.oss.factory.OssFactory;
import cn.universal.ossm.oss.service.ICloudStorageService;
import cn.universal.ossm.service.ISysOssService;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tk.mybatis.mapper.entity.Example;

/** 文件上传 服务层实现 @Author Lion Li */
@Service
public class SysOssServiceImpl implements ISysOssService {

  @Resource private SysOssMapper sysOssMapper;

  @Value("${codec.path:nexiot/}")
  private String prePath;

  @Override
  public SysOss getById(Long ossId) {
    return sysOssMapper.selectByPrimaryKey(ossId);
  }

  @Override
  public List<SysOss> queryPageList(SysOssBo bo) {
    Example example = new Example(SysOss.class);
    Example.Criteria criteria = example.createCriteria();
    if (bo != null) {
      if (StrUtil.isNotBlank(bo.getFileName())) {
        criteria.andEqualTo("fileName", bo.getFileName());
      }
      if (StrUtil.isNotBlank(bo.getOriginalName())) {
        criteria.andEqualTo("originalName", bo.getOriginalName());
      }
      if (StrUtil.isNotBlank(bo.getFileSuffix())) {
        criteria.andEqualTo("fileSuffix", bo.getFileSuffix());
      }
      if (StrUtil.isNotBlank(bo.getUrl())) {
        criteria.andEqualTo("url", bo.getUrl());
      }
      if (StrUtil.isNotBlank(bo.getTenantId())) {
        criteria.andEqualTo("tenantId", bo.getTenantId());
      }
      if (StrUtil.isNotBlank(bo.getService())) {
        criteria.andEqualTo("service", bo.getService());
      }
      if (StrUtil.isNotBlank(bo.getCreateBy())) {
        criteria.andEqualTo("createBy", bo.getCreateBy());
      }
    }
    example.setOrderByClause("create_time desc, oss_id desc");
    return sysOssMapper.selectByExample(example);
  }

  @Override
  public SysOss upload(MultipartFile file, String unionId) {
    return upload(file, unionId, null);
  }

  @Override
  public SysOss upload(MultipartFile file, String unionId, String tenantId) {
    if (file.getOriginalFilename() == null) {
      throw new IoTException("文件名为空！");
    }
    // 获取文件扩展名
    String originalFilename = file.getOriginalFilename();
    String suffix = "";
    if (originalFilename != null && originalFilename.contains(".")) {
      suffix =
          StrUtil.sub(
              originalFilename, originalFilename.lastIndexOf("."), originalFilename.length());
    }
    // 生成基于 Unix 时间戳的文件名
    String originalfileName = prePath + System.currentTimeMillis() + suffix;
    String configTenantId = OssTenantResolver.normalizeConfigTenantId(tenantId);
    UploadResultHolder resultHolder =
        OssTenantContext.run(
            configTenantId,
            () -> {
              ICloudStorageService storage = OssFactory.instance();
              try (InputStream inputStream = file.getInputStream()) {
                return new UploadResultHolder(
                    storage, storage.upload(inputStream, originalfileName, file.getContentType()));
              } catch (IOException e) {
                throw new IoTException("文件读取异常!!!", e);
              }
            });
    ICloudStorageService storage = resultHolder.storage();
    UploadResult uploadResult = resultHolder.uploadResult();
    // 保存文件信息
    SysOss oss =
        new SysOss()
            .setUrl(uploadResult.getUrl())
            .setFileSuffix(suffix)
            .setFileName(uploadResult.getFilename())
            .setOriginalName(originalfileName)
            .setService(storage.getServiceType())
            .setTenantId(tenantId)
            .setCreateBy(unionId)
            .setCreateTime(new Date());
    sysOssMapper.insert(oss);
    return oss;
  }

  @Override
  public SysOss uploadStream(InputStream inputStream, String fileName) {
    String resourceTenantId = OssTenantContext.getTenantId();
    UploadResultHolder resultHolder =
        uploadInConfigTenant(
            resourceTenantId,
            inputStream,
            "snapshot/" + fileName,
            fileName.substring(fileName.lastIndexOf(".")));
    ICloudStorageService storage = resultHolder.storage();
    UploadResult uploadResult = resultHolder.uploadResult();
    // 保存文件信息
    SysOss oss =
        new SysOss()
            .setUrl(uploadResult.getUrl())
            .setOriginalName(fileName)
            .setFileSuffix(fileName.substring(fileName.lastIndexOf(".")))
            .setFileName(uploadResult.getFilename())
            .setService(storage.getServiceType())
            .setTenantId(resourceTenantId)
            .setCreateTime(new Date());
    sysOssMapper.insert(oss);
    return oss;
  }

  @Override
  public SysOss uploadVideoStream(InputStream inputStream, String fileName) {
    String resourceTenantId = OssTenantContext.getTenantId();
    UploadResultHolder resultHolder =
        uploadInConfigTenant(
            resourceTenantId,
            inputStream,
            "video/" + fileName,
            fileName.substring(fileName.lastIndexOf(".")));
    ICloudStorageService storage = resultHolder.storage();
    UploadResult uploadResult = resultHolder.uploadResult();
    // 保存文件信息
    SysOss oss =
        new SysOss()
            .setUrl(uploadResult.getUrl())
            .setOriginalName(fileName)
            .setFileSuffix(fileName.substring(fileName.lastIndexOf(".")))
            .setFileName(uploadResult.getFilename())
            .setService(storage.getServiceType())
            .setTenantId(resourceTenantId)
            .setCreateTime(new Date());
    sysOssMapper.insert(oss);
    return oss;
  }

  @Override
  public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
    if (isValid) {
      // 做一些业务上的校验,判断是否需要校验
    }
    List<SysOss> list = sysOssMapper.listByIds(ids);
    for (SysOss sysOss : list) {
      deleteInConfigTenant(sysOss);
    }
    return sysOssMapper.removeByIds(ids) > 0;
  }

  @Override
  public Boolean deleteWithValidByUrls(String[] urls, Boolean isValid) {
    if (urls == null || urls.length <= 0) {
      return false;
    }

    if (isValid) {
      // 做一些业务上的校验,判断是否需要校验
    }
    List<SysOss> list = sysOssMapper.listByUrls(urls);
    for (SysOss sysOss : list) {
      deleteInConfigTenant(sysOss);
    }
    return sysOssMapper.removeByUrls(urls) > 0;
  }

  private void deleteInConfigTenant(SysOss sysOss) {
    if (sysOss == null) {
      return;
    }
    OssTenantContext.run(
        OssTenantResolver.normalizeConfigTenantId(sysOss.getTenantId()),
        () -> {
          ICloudStorageService storage = OssFactory.instance(sysOss.getService());
          storage.delete(sysOss.getUrl());
        });
  }

  private UploadResultHolder uploadInConfigTenant(
      String resourceTenantId, InputStream inputStream, String path, String contentType) {
    String configTenantId = OssTenantResolver.normalizeConfigTenantId(resourceTenantId);
    return OssTenantContext.run(
        configTenantId,
        () -> {
          ICloudStorageService storage = OssFactory.instance();
          try {
            return new UploadResultHolder(storage, storage.upload(inputStream, path, contentType));
          } catch (Exception e) {
            throw new IoTException("文件读取异常!!!", e);
          }
        });
  }

  private record UploadResultHolder(ICloudStorageService storage, UploadResult uploadResult) {}
}
