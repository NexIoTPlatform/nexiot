package nexiot.web.ide.debug.plugins.modules.spring;

import cn.universal.core.engine.annotation.Comment;
import nexiot.web.ide.debug.plugins.core.annotation.MagicModule;
import org.springframework.core.env.Environment;

/**
 * env模块
 *
 * @author mxd
 */
@MagicModule("env")
public class EnvModule {

  private final Environment environment;

  public EnvModule(Environment environment) {
    this.environment = environment;
  }

  @Comment("获取配置")
  public String get(@Comment(name = "key", value = "配置项") String key) {
    return environment.getProperty(key);
  }

  @Comment("获取配置")
  public String get(
      @Comment(name = "key", value = "配置项") String key,
      @Comment(name = "defaultValue", value = "未配置时的默认值") String defaultValue) {
    return environment.getProperty(key, defaultValue);
  }
}
