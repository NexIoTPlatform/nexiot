/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.VideoPlatformOrgCache;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface VideoPlatformOrgCacheMapper extends BaseMapper<VideoPlatformOrgCache> {
  List<VideoPlatformOrgCache> selectByInstanceKey(@Param("instanceKey") String instanceKey);

  VideoPlatformOrgCache selectOneByInstanceAndOrg(
      @Param("instanceKey") String instanceKey, @Param("orgId") String orgId);
}
