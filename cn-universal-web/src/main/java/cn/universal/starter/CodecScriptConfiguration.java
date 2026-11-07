package cn.universal.starter;

import cn.universal.core.engine.MagicResourceLoader;
import cn.universal.core.engine.exception.MagicScriptRuntimeException;
import cn.universal.core.engine.functions.DynamicModuleImport;
import cn.universal.core.engine.functions.ExtensionMethod;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** nexiot.ide.debug自动配置类 */
@Configuration
@ConditionalOnClass({RequestMappingHandlerMapping.class})
public class CodecScriptConfiguration implements ApplicationContextAware {

  private static final Logger logger = LoggerFactory.getLogger(CodecScriptConfiguration.class);

  private ApplicationContext applicationContext;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
    setupMagicModules(null);
  }

  /** 注册模块、类型扩展 */
  private void setupMagicModules(List<ExtensionMethod> extensionMethods) {
    // 设置脚本import时 class加载策略
    MagicResourceLoader.setClassLoader(
        (className) -> {
          try {
            return applicationContext.getBean(className);
          } catch (Exception e) {
            Class<?> clazz = null;
            try {
              clazz = Class.forName(className);
              return applicationContext.getBean(clazz);
            } catch (Exception ex) {
              if (clazz == null) {
                throw new MagicScriptRuntimeException(new ClassNotFoundException(className));
              }
              return clazz;
            }
          }
        });
    logger.info("注册模块:{} -> {}", "log", Logger.class);
    MagicResourceLoader.addModule(
        "log",
        new DynamicModuleImport(
            Logger.class,
            context ->
                LoggerFactory.getLogger(Objects.toString(context.getScriptName(), "Codec"))));
    List<String> importModules = List.of("cn.hutool.json.*");
    importModules.forEach(
        importPackage -> {
          logger.info("自动导包：{}", importPackage);
          MagicResourceLoader.addPackage(importPackage);
        });
  }
}
