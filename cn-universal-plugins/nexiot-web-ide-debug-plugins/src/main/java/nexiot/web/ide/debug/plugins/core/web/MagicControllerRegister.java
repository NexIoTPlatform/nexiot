package nexiot.web.ide.debug.plugins.core.web;

import nexiot.web.ide.debug.plugins.core.config.MagicConfiguration;
import nexiot.web.ide.debug.plugins.utils.Mapping;

public interface MagicControllerRegister {

  void register(Mapping mapping, MagicConfiguration configuration);
}
