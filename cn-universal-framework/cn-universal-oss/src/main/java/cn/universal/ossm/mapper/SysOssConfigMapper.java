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

package cn.universal.ossm.mapper;

import cn.universal.ossm.entity.SysOssConfig;
import cn.universal.persistence.common.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** OSS存储配置 数据层 */
public interface SysOssConfigMapper extends BaseMapper<SysOssConfig> {

  List<SysOssConfig> listAll(@Param("tenantId") String tenantId);

  SysOssConfig selectByProvider(
      @Param("provider") String provider, @Param("tenantId") String tenantId);

  SysOssConfig selectActive(@Param("tenantId") String tenantId);

  int insertConfig(SysOssConfig config);

  int updateConfig(SysOssConfig config);

  int clearActive(@Param("tenantId") String tenantId);

  int activateProvider(@Param("provider") String provider, @Param("tenantId") String tenantId);
}
