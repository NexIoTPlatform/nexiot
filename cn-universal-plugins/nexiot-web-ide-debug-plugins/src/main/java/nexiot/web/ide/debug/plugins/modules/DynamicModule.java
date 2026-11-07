package nexiot.web.ide.debug.plugins.modules;

import cn.universal.core.engine.MagicScriptContext;
import java.beans.Transient;

public interface DynamicModule<T> {

  @Transient
  T getDynamicModule(MagicScriptContext context);
}
