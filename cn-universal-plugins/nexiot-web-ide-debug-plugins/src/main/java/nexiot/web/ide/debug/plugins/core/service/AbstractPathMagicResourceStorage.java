package nexiot.web.ide.debug.plugins.core.service;

import java.util.Objects;
import nexiot.web.ide.debug.plugins.core.config.JsonCodeConstants;
import nexiot.web.ide.debug.plugins.core.model.PathMagicEntity;
import nexiot.web.ide.debug.plugins.utils.PathUtils;

public abstract class AbstractPathMagicResourceStorage<T extends PathMagicEntity>
    implements MagicResourceStorage<T>, JsonCodeConstants {

  protected MagicResourceService magicResourceService;

  @Override
  public String suffix() {
    return ".ms";
  }

  @Override
  public boolean requirePath() {
    return true;
  }

  @Override
  public void setMagicResourceService(MagicResourceService magicResourceService) {
    this.magicResourceService = magicResourceService;
  }

  public String buildMappingKey(T entity, String path) {
    return PathUtils.replaceSlash(
        "/" + Objects.toString(path, "") + "/" + Objects.toString(entity.getPath(), ""));
  }

  @Override
  public void validate(T entity) {
    notBlank(entity.getPath(), REQUEST_PATH_REQUIRED);
    notBlank(entity.getScript(), SCRIPT_REQUIRED);
  }
}
