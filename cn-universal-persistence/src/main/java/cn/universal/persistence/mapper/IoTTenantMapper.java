package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.IoTTenant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface IoTTenantMapper extends BaseMapper<IoTTenant> {

  List<Map<String, Object>> selectTenantCountByPackageIds(
      @Param("packageIds") Collection<Long> packageIds);

  @Update(
      "UPDATE iot_tenant SET expire_time = #{expireTime,jdbcType=TIMESTAMP}, update_by = #{updateBy}, update_time = #{updateTime} WHERE tenant_id = #{tenantId}")
  int updateTenantExpireTime(
      @Param("tenantId") String tenantId,
      @Param("expireTime") Date expireTime,
      @Param("updateBy") String updateBy,
      @Param("updateTime") Date updateTime);
}
