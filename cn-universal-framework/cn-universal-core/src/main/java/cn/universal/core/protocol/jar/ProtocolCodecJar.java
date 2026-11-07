/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 本文件由 gitee.com/NexIoT 开发并拥有版权，未经授权严禁擅自商用、复制或传播。
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 *
 */

package cn.universal.core.protocol.jar;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.common.exception.CodecException;
import cn.universal.core.protocol.jar.downloader.JarDownloader;
import cn.universal.core.protocol.jar.resolver.BeanLocationResolver;
import cn.universal.core.protocol.jar.resolver.LocalJarLocationResolver;
import cn.universal.core.protocol.jar.resolver.LocationResolver;
import cn.universal.core.protocol.jar.resolver.RemoteJarLocationResolver;
import cn.universal.core.protocol.request.ProtocolDecodeRequest;
import cn.universal.core.protocol.request.ProtocolEncodeRequest;
import cn.universal.core.protocol.support.ProtocolCodecLoader;
import cn.universal.core.protocol.support.ProtocolCodecSupport;
import cn.universal.core.protocol.support.ProtocolCodecSupportWrapper;
import cn.universal.core.protocol.support.ProtocolCodecWrapper;
import cn.universal.core.protocol.support.ProtocolSupportDefinition;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * jar加解密插件支持，动态加载jar包
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/8/9 19:28
 */
@Slf4j
public class ProtocolCodecJar extends ProtocolCodecSupportWrapper
    implements ProtocolCodecLoader, ProtocolCodecSupport, ProtocolCodecWrapper {

  private final Map<String, ProtocolJarClassLoader> protocolLoaders = new ConcurrentHashMap<>();
  private final Map<String, Object> codecJarProvider = new ConcurrentHashMap<>();
  // 记录每个 provider 的类型：true 表示 Bean 类型，false 表示 JAR 类型
  private final Map<String, Boolean> providerTypeCache = new ConcurrentHashMap<>();

  private ProtocolCodecJar() {
    // 已移除 beforeSecurity()，兼容 JDK 17+
  }

  private static class ProtocolCodecJarProviderHolder {

    private static ProtocolCodecJar INSTANCE = new ProtocolCodecJar();
  }

  public static ProtocolCodecJar getInstance() {
    return ProtocolCodecJarProviderHolder.INSTANCE;
  }

  @Override
  public String getProviderType() {
    return "jar";
  }

  ProtocolJarClassLoader createClassLoader(URL localtion) {
    return new ProtocolJarClassLoader(new URL[] {localtion}, this.getClass().getClassLoader());
  }

  @Override
  public void load(ProtocolSupportDefinition definition) throws CodecException {
    synchronized (this) {
      try {
        Map<String, Object> config = definition.getConfiguration();
        // 获取位置
        String location =
            (String)
                Optional.ofNullable(config.get("location"))
                    .map(String::valueOf)
                    .orElseThrow(
                        () -> {
                          return new IllegalArgumentException("location");
                        });
        // 获取提供者
        String provider =
            (String)
                Optional.ofNullable(config.get("provider"))
                    .map(String::valueOf)
                    .map(String::trim)
                    .orElseThrow(
                        () -> {
                          return new IllegalArgumentException("provider");
                        });
        // 卸载老的加载器
        codecJarProvider.remove(provider);
        protocolLoaders.remove(provider);
        providerTypeCache.remove(provider);

        // 使用策略模式解析 location
        LocationResolver resolver = findResolver(location);
        if (resolver == null) {
          throw new CodecException("无法识别 location 类型: " + location);
        }

        Object codecInstance = resolver.resolve(location, provider);
        if (codecInstance == null) {
          throw new CodecException("无法解析 location: " + location);
        }

        // 如果是 JAR 类型，需要保存 ClassLoader（Bean 类型不需要）
        if (resolver instanceof LocalJarLocationResolver
            || resolver instanceof RemoteJarLocationResolver) {
          try {
            String actualLocation = location;
            // 如果是远程 JAR，需要先下载获取本地路径
            if (resolver instanceof RemoteJarLocationResolver) {
              JarDownloader jarDownloader = new JarDownloader();
              actualLocation = jarDownloader.download(location);
            }
            // 创建 ClassLoader
            URL url;
            if (!actualLocation.contains("://")) {
              url = (new File(actualLocation)).toURI().toURL();
            } else {
              url = new java.net.URI("jar:" + actualLocation + "!/").toURL();
            }
            ProtocolJarClassLoader loader = createClassLoader(url);
            protocolLoaders.put(provider, loader);
          } catch (java.net.URISyntaxException | java.net.MalformedURLException e) {
            log.warn("保存 ClassLoader 失败，但不影响 Bean 类型使用: {}", e.getMessage());
          } catch (Exception e) {
            log.warn("保存 ClassLoader 失败，但不影响 Bean 类型使用: {}", e.getMessage());
          }
        }

        // 校验和设置 supportMethods
        validateAndSetSupportMethods(definition, codecInstance, resolver);

        // 记录 provider 类型（Bean 还是 JAR）
        boolean isBean = resolver instanceof BeanLocationResolver;
        providerTypeCache.put(provider, isBean);

        // 存储编解码器实例
        codecJarProvider.put(provider, codecInstance);
        log.info(
            "成功加载协议编解码器: provider={}, location={}, type={}, isBean={}, supportMethods={}",
            provider,
            location,
            resolver.getClass().getSimpleName(),
            isBean,
            definition.getSupportMethods());
      } catch (Exception e) {
        String error = ExceptionUtil.getRootCauseMessage(e);
        log.error("加载jar编解码出错={}", e);
        throw new CodecException(error);
      }
    }
  }

  @Override
  public String decode(ProtocolDecodeRequest decodeRequest) throws CodecException {
    try {
      if (!codecJarProvider.containsKey(decodeRequest.getDefinition().getProvider())) {
        load(decodeRequest.getDefinition());
      }
      Object codecInstance = codecJarProvider.get(decodeRequest.getDefinition().getProvider());
      String provider = decodeRequest.getDefinition().getProvider();
      String payload = decodeRequest.getPayload();
      Object context = decodeRequest.getContext();

      // 优先级1：如果是 Bean 类型且实现了 JarDriverCodecService 接口，直接类型转换调用（最快）
      if (Boolean.TRUE.equals(providerTypeCache.get(provider))
          && codecInstance instanceof JarDriverCodecService) {
        try {
          String result = ((JarDriverCodecService) codecInstance).decode(payload, context);
          return str(result);
        } catch (Exception e) {
          log.warn(
              "JarDriverCodecService decode 调用失败，回退到反射: provider={}, error={}",
              provider,
              e.getMessage());
        }
      }

      // 优先级2：使用反射调用两个参数的方法（适用于 JAR 包和未实现接口的 Bean）
      try {
        Method method =
            codecInstance
                .getClass()
                .getMethod(CodecMethod.decode.name(), String.class, Object.class);
        Object result = method.invoke(codecInstance, payload, context);
        return str(result);
      } catch (NoSuchMethodException e) {
        // 方法不存在，返回原始数据
        log.error("编解码器未实现 decode 方法（两个参数）: provider={}", provider);
        return decodeRequest.getPayload();
      }
    } catch (Exception e) {
      String error = ExceptionUtil.getRootCauseMessage(e);
      log.error(
          "产品型号={} 提供者={} 解码失败={}",
          decodeRequest.getDefinition().getId(),
          decodeRequest.getDefinition().getProvider(),
          e);
      throw new CodecException(error);
    }
  }

  @Override
  public String encode(ProtocolEncodeRequest encodeRequest) throws CodecException {
    try {
      if (!codecJarProvider.containsKey(encodeRequest.getDefinition().getProvider())) {
        load(encodeRequest.getDefinition());
      }
      Object codecInstance = codecJarProvider.get(encodeRequest.getDefinition().getProvider());
      String provider = encodeRequest.getDefinition().getProvider();
      String payload = encodeRequest.getPayload();
      Object context = encodeRequest.getContext();

      // 优先级1：如果是 Bean 类型且实现了 JarDriverCodecService 接口，直接类型转换调用（最快）
      if (Boolean.TRUE.equals(providerTypeCache.get(provider))
          && codecInstance instanceof JarDriverCodecService) {
        try {
          String result = ((JarDriverCodecService) codecInstance).encode(payload, context);
          return str(result);
        } catch (Exception e) {
          log.warn(
              "JarDriverCodecService encode 调用失败，回退到反射: provider={}, error={}",
              provider,
              e.getMessage());
        }
      }

      // 优先级2：使用反射调用两个参数的方法（适用于 JAR 包和未实现接口的 Bean）
      try {
        Method method =
            codecInstance
                .getClass()
                .getMethod(CodecMethod.encode.name(), String.class, Object.class);
        Object result = method.invoke(codecInstance, payload, context);
        return str(result);
      } catch (NoSuchMethodException e) {
        // 方法不存在，返回原始数据
        log.error("编解码器未实现 encode 方法（两个参数）: provider={}", provider);
        return encodeRequest.getPayload();
      }
    } catch (Exception e) {
      String error = ExceptionUtil.getRootCauseMessage(e);
      log.error(
          "产品型号={} 提供者={} 编码失败={}",
          encodeRequest.getDefinition().getId(),
          encodeRequest.getDefinition().getProvider(),
          error);
      throw new CodecException(error);
    }
  }

  @Override
  public String preDecode(ProtocolDecodeRequest protocolDecodeRequest) throws CodecException {
    try {
      if (!codecJarProvider.containsKey(protocolDecodeRequest.getDefinition().getProvider())) {
        load(protocolDecodeRequest.getDefinition());
      }
      Object codecInstance =
          codecJarProvider.get(protocolDecodeRequest.getDefinition().getProvider());
      String provider = protocolDecodeRequest.getDefinition().getProvider();
      String payload = protocolDecodeRequest.getPayload();
      Object context = protocolDecodeRequest.getContext();

      // 优先级1：如果是 Bean 类型且实现了 JarDriverCodecService 接口，直接类型转换调用（最快）
      if (Boolean.TRUE.equals(providerTypeCache.get(provider))
          && codecInstance instanceof JarDriverCodecService) {
        try {
          String result = ((JarDriverCodecService) codecInstance).preDecode(payload, context);
          return str(result);
        } catch (Exception e) {
          log.warn(
              "JarDriverCodecService preDecode 调用失败，回退到反射: provider={}, error={}",
              provider,
              e.getMessage());
        }
      }

      // 优先级2：使用反射调用两个参数的方法（适用于 JAR 包和未实现接口的 Bean）
      try {
        Method method =
            codecInstance
                .getClass()
                .getMethod(CodecMethod.preDecode.name(), String.class, Object.class);
        Object result = method.invoke(codecInstance, payload, context);
        return str(result);
      } catch (NoSuchMethodException e) {
        // 方法不存在，返回原始数据
        log.error("编解码器未实现 preDecode 方法（两个参数）: provider={}", provider);
        return protocolDecodeRequest.getPayload();
      }
    } catch (Exception e) {
      String error = ExceptionUtil.getRootCauseMessage(e);
      log.error(
          "产品型号={} 提供者={} 预编码失败={}",
          protocolDecodeRequest.getDefinition().getId(),
          protocolDecodeRequest.getDefinition().getProvider(),
          error);
      throw new CodecException(error);
    }
  }

  @Override
  public void load(String provider, Object providerImpl) {
    if (StrUtil.isNotBlank(provider) && providerImpl != null) {
      codecJarProvider.put(provider, providerImpl);
    }
  }

  @Override
  public void remove(String provider) {
    if (StrUtil.isNotBlank(provider)) {
      codecJarProvider.remove(provider);
      protocolLoaders.remove(provider);
      providerTypeCache.remove(provider);
    }
  }

  // 在ProtocolCodecJar类中添加isLoaded方法
  @Override
  public boolean isLoaded(String provider, CodecMethod codecMethod) {
    // 检查provider对应的是否已加载到缓存中
    if (!codecJarProvider.containsKey(provider)) {
      return false;
    }

    // 检查编解码器中是否实现了对应的方法（两个参数）
    try {
      Object codecInstance = codecJarProvider.get(provider);
      Class<?> clazz = codecInstance.getClass();

      Method method = clazz.getMethod(codecMethod.name(), String.class, Object.class);
      if (method != null && method.getReturnType() == String.class) {
        return true;
      }

      return false;
    } catch (NoSuchMethodException e) {
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public String iotToYour(ProtocolEncodeRequest encodeRequest) throws CodecException {
    try {
      if (!codecJarProvider.containsKey(encodeRequest.getDefinition().getProvider())) {
        load(encodeRequest.getDefinition());
      }
      Object codecInstance = codecJarProvider.get(encodeRequest.getDefinition().getProvider());
      String provider = encodeRequest.getDefinition().getProvider();
      String payload = encodeRequest.getPayload();
      Object context = encodeRequest.getContext();

      // 优先级1：使用反射调用两个参数的方法
      try {
        Method method =
            codecInstance
                .getClass()
                .getMethod(CodecMethod.iotToYour.name(), String.class, Object.class);
        Object result = method.invoke(codecInstance, payload, context);
        return str(result);
      } catch (NoSuchMethodException e) {
        // 方法不存在，回退到 encode 方法
        log.error("编解码器未实现 iotToYour 方法（两个参数），回退到 encode: provider={}", provider);
        return encode(encodeRequest);
      }
    } catch (Exception e) {
      String error = ExceptionUtil.getRootCauseMessage(e);
      log.error(
          "产品型号={} 提供者={} iotToYour失败={}",
          encodeRequest.getDefinition().getId(),
          encodeRequest.getDefinition().getProvider(),
          e);
      throw new CodecException(error);
    }
  }

  @Override
  public String yourToIot(ProtocolDecodeRequest decodeRequest) throws CodecException {
    try {
      if (!codecJarProvider.containsKey(decodeRequest.getDefinition().getProvider())) {
        load(decodeRequest.getDefinition());
      }
      Object codecInstance = codecJarProvider.get(decodeRequest.getDefinition().getProvider());
      String provider = decodeRequest.getDefinition().getProvider();
      String payload = decodeRequest.getPayload();
      Object context = decodeRequest.getContext();

      // 优先级1：使用反射调用两个参数的方法
      try {
        Method method =
            codecInstance
                .getClass()
                .getMethod(CodecMethod.yourToIot.name(), String.class, Object.class);
        Object result = method.invoke(codecInstance, payload, context);
        return str(result);
      } catch (NoSuchMethodException e) {
        // 方法不存在，回退到 decode 方法
        log.error("编解码器未实现 yourToIot 方法（两个参数），回退到 decode: provider={}", provider);
        return decode(decodeRequest);
      }
    } catch (Exception e) {
      String error = ExceptionUtil.getRootCauseMessage(e);
      log.error(
          "产品型号={} 提供者={} yourToIot失败={}",
          decodeRequest.getDefinition().getId(),
          decodeRequest.getDefinition().getProvider(),
          e);
      throw new CodecException(error);
    }
  }

  /**
   * 查找合适的 LocationResolver
   *
   * @param location location 值
   * @return LocationResolver 实例
   */
  private LocationResolver findResolver(String location) {
    // 按优先级顺序尝试
    List<LocationResolver> resolvers =
        Arrays.asList(
            new BeanLocationResolver(), // 优先检查 Bean
            new RemoteJarLocationResolver(), // 其次检查远程 JAR
            new LocalJarLocationResolver() // 最后检查本地 JAR
            );

    for (LocationResolver resolver : resolvers) {
      if (resolver.supports(location)) {
        return resolver;
      }
    }
    return null;
  }

  /**
   * 校验和设置 supportMethods 1. 如果配置了 supportMethods，验证编解码器是否实现了这些方法 2. 如果未配置
   * supportMethods，通过反射自动检测并设置
   *
   * @param definition 协议支持定义
   * @param codecInstance 编解码器实例
   * @param resolver LocationResolver 实例
   */
  private void validateAndSetSupportMethods(
      ProtocolSupportDefinition definition, Object codecInstance, LocationResolver resolver) {
    Set<String> configuredMethods = definition.getSupportMethods();
    Set<String> detectedMethods = detectSupportMethods(codecInstance, resolver);

    if (configuredMethods != null && !configuredMethods.isEmpty()) {
      // 配置了 supportMethods，进行校验
      Set<String> missingMethods = new HashSet<>(configuredMethods);
      missingMethods.removeAll(detectedMethods);

      if (!missingMethods.isEmpty()) {
        log.warn(
            "编解码器未实现配置的方法: provider={}, missingMethods={}, detectedMethods={}",
            definition.getProvider(),
            missingMethods,
            detectedMethods);
        // 可以选择抛出异常或仅警告
        // throw new CodecException("编解码器未实现配置的方法: " + missingMethods);
      }

      // 使用配置的方法（取交集，确保只使用实际实现的方法）
      Set<String> validMethods = new HashSet<>(configuredMethods);
      validMethods.retainAll(detectedMethods);
      definition.setSupportMethods(validMethods);

      log.info(
          "supportMethods 校验完成: provider={}, configured={}, valid={}",
          definition.getProvider(),
          configuredMethods,
          validMethods);
    } else {
      // 未配置 supportMethods，使用自动检测的结果
      definition.setSupportMethods(detectedMethods);
      log.info(
          "自动检测 supportMethods: provider={}, methods={}",
          definition.getProvider(),
          detectedMethods);
    }
  }

  /**
   * 检测编解码器支持的方法
   *
   * <p>统一检测两个参数的方法签名：String methodName(String payload, Object context)
   *
   * <p>注意：productKey 不需要作为参数传递，它只用于路由选择使用哪种 codec（jar、magic等）
   *
   * @param codecInstance 编解码器实例
   * @param resolver LocationResolver 实例
   * @return 支持的方法集合
   */
  private Set<String> detectSupportMethods(Object codecInstance, LocationResolver resolver) {
    Set<String> supportMethods = new HashSet<>();
    Class<?> clazz = codecInstance.getClass();

    // 所有可能的 CodecMethod
    CodecMethod[] allMethods = CodecMethod.values();

    for (CodecMethod codecMethod : allMethods) {
      try {
        // 检测两个参数的方法签名：String methodName(String, Object)
        Method method = clazz.getMethod(codecMethod.name(), String.class, Object.class);
        if (method != null && method.getReturnType() == String.class) {
          supportMethods.add(codecMethod.name());
        }
      } catch (NoSuchMethodException e) {
        // 方法不存在，跳过
      } catch (Exception e) {
        log.warn("检测方法时出错: method={}, error={}", codecMethod.name(), e.getMessage());
      }
    }

    return supportMethods;
  }
}
