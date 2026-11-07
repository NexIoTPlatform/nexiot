package cn.universal.persistence.mapper;

import cn.universal.persistence.common.BaseMapper;
import cn.universal.persistence.entity.IoTTenantPackageMenu;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface IoTTenantPackageMenuMapper extends BaseMapper<IoTTenantPackageMenu> {

  int batchInsert(@Param("menus") List<IoTTenantPackageMenu> menus);
}
