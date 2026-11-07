package nexiot.web.ide.debug.plugins.core.config;

import nexiot.web.ide.debug.plugins.core.model.Plugin;
import nexiot.web.ide.debug.plugins.core.web.MagicControllerRegister;

public interface MagicPluginConfiguration {

  Plugin plugin();

  /** 注册Controller */
  default MagicControllerRegister controllerRegister() {
    return (mapping, configuration) -> {};
  }
}
