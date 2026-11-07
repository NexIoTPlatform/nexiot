package nexiot.web.ide.debug.plugins.core.service;

import java.util.Collections;
import java.util.List;
import nexiot.web.ide.debug.plugins.core.model.MagicEntity;

public interface MagicDynamicRegistry<T extends MagicEntity> {

  /** 注册 */
  boolean register(T entity);

  /** 取消注册 */
  boolean unregister(T entity);

  T getMapping(String mappingKey);

  /** 资源存储器 */
  MagicResourceStorage<T> getMagicResourceStorage();

  default List<T> defaultMappings() {
    return Collections.emptyList();
  }
}
