package cn.universal.admin.system.service;

import tk.mybatis.mapper.entity.Config;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

final class TkMapperEntityTestSupport {

  private static final Config CONFIG = new Config();

  private TkMapperEntityTestSupport() {}

  static void registerEntityTables(Class<?>... entityClasses) {
    if (entityClasses == null) {
      return;
    }
    for (Class<?> entityClass : entityClasses) {
      if (entityClass != null) {
        EntityHelper.initEntityNameMap(entityClass, CONFIG);
      }
    }
  }
}
