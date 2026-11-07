package nexiot.web.ide.debug.plugins.starter;

import nexiot.web.ide.debug.plugins.core.config.MagicAPIProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

/**
 * Magic API 插件启动器
 *
 * @author gitee.com/NexIoTXin
 * @since 2025-01-15
 */
@Component
@ConditionalOnProperty(
    prefix = "nexiot.ide.debug",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false)
@ComponentScan(basePackages = "nexiot.web.ide.debug")
public class MagicAPIPluginStarter implements ApplicationRunner {

  private static final Logger logger = LoggerFactory.getLogger(MagicAPIPluginStarter.class);

  @Autowired private MagicAPIProperties pluginProperties;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    if (pluginProperties.isEnabled()) {
      logger.info("=== Magic API 插件已启用 ===");
      logger.info("Web路径: {}", pluginProperties.getWeb());
      logger.info("前缀: {}", pluginProperties.getPrefix());
      logger.info("资源类型: {}", pluginProperties.getResource().getType());
      logger.info("调试超时: {}秒", pluginProperties.getDebug().getTimeout());
      logger.info("跨域支持: {}", pluginProperties.isSupportCrossDomain());
      logger.info("安全认证: {}", pluginProperties.getSecurity().getUsername() != null);
      logger.info("备份功能: {}", pluginProperties.getBackup().isEnable());
      logger.info("================================");
    } else {
      logger.info("Magic API 插件已禁用");
    }
  }
}
