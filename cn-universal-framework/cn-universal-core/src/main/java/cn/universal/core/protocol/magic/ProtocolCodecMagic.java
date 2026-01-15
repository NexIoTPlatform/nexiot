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

package cn.universal.core.protocol.magic;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.universal.common.exception.CodecException;
import cn.universal.common.exception.IoTException;
import cn.universal.core.engine.MagicScript;
import cn.universal.core.engine.MagicScriptContext;
import cn.universal.core.engine.runtime.MagicScriptRuntime;
import cn.universal.core.protocol.request.ProtocolDecodeRequest;
import cn.universal.core.protocol.request.ProtocolEncodeRequest;
import cn.universal.core.protocol.support.ProtocolCodecLoader;
import cn.universal.core.protocol.support.ProtocolCodecSupport;
import cn.universal.core.protocol.support.ProtocolCodecSupportWrapper;
import cn.universal.core.protocol.support.ProtocolCodecWrapper;
import cn.universal.core.protocol.support.ProtocolSupportDefinition;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * magic 脚本引擎
 *
 * <p>https://gitee.com/ssssssss-team/magic-script
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/05/19 19:28
 */
@Slf4j

public class ProtocolCodecMagic extends ProtocolCodecSupportWrapper
    implements ProtocolCodecLoader, ProtocolCodecSupport, ProtocolCodecWrapper {

  /** 编码 */
  private final Map<String, MagicScript> magicEncoderProvider = new ConcurrentHashMap<>();

  /** 解码 */
  private final Map<String, MagicScript> magicDecoderProvider = new ConcurrentHashMap<>();

  /** 解码前置解析 */
  private final Map<String, MagicScript> magicPreDecoderProvider = new ConcurrentHashMap<>();

  /** 协议方法缓存 */
  private final Map<String, Set<String>> methodCache = new ConcurrentHashMap<>();

  private ProtocolCodecMagic() {}

  private static class ProtocolCodecJscriptProviderHoler {

    private static ProtocolCodecMagic INSTANCE = new ProtocolCodecMagic();
  }

  public static ProtocolCodecMagic getInstance() {
    return ProtocolCodecJscriptProviderHoler.INSTANCE;
  }

  @Override
  public String getProviderType() {
    return "magic";
  }

  @Override
  public void load(ProtocolSupportDefinition definition) {
    return;
  }

  private void load(ProtocolSupportDefinition definition, CodecMethod codecMethod)
      throws CodecException {
    try {
      Map<String, Object> config = definition.getConfiguration();
      // JS原始代码
      String location =
              Optional.ofNullable(config.get("location"))
                  .map(String::valueOf)
                  .orElseThrow(
                      () -> {
                        return new IoTException(
                            "magic engine source code not exist, can not do encode or decode ");
                      });
      // 获取提供者,产品唯一
      String provider =
              Optional.ofNullable(config.get("provider"))
                  .map(String::valueOf)
                  .map(String::trim)
                  .orElseThrow(
                      () -> {
                        return new CodecException("provider 不能为空");
                      });
      // magic 方法调用必须使用return 方法名(参数);
        location = switch (codecMethod) {
            case decode -> location + "  \n return decode(payload,context);";
            case encode -> location + "  \n return encode(payload,context);";
            case preDecode -> location + "  \n return preDecode(payload,context);";
            case codecAdd -> location + "  \n return codecAdd(payload,context);";
            case codecDelete -> location + "  \n return codecDelete(payload,context);";
            case codecUpdate -> location + "  \n return codecUpdate(payload,context);";
            case codecQuery -> location + "  \n return codecQuery(payload,context);";
            case iotToYour -> location + "  \n return iotToYour(payload,context);";
            case yourToIot -> location + "  \n return yourToIot(payload,context);";
            default -> location + "  \n return encode(payload,context);";
        };
      // 初始化
      long t = System.currentTimeMillis();
      MagicScript script = MagicScript.create(location, null);
      MagicScriptRuntime compile = script.compile();
      // 处理内部实现了哪些方法
      evalMethodCache(provider, compile);
      log.info("编译耗时：{}", (System.currentTimeMillis() - t) + "ms");
      t = System.currentTimeMillis();
      if (codecMethod.equals(CodecMethod.decode)) {
        magicDecoderProvider.put(provider, script);
      } else if (codecMethod.equals(CodecMethod.preDecode)) {
        magicPreDecoderProvider.put(provider, script);
      } else {
        magicEncoderProvider.put(provider, script);
      }
    } catch (Exception e) {
      String error = ExceptionUtil.getRootCauseMessage(e);
      log.error("加载magic编解码出错={}", e);
      throw new CodecException(error);
    }
  }

  private void evalMethodCache(String provider, MagicScriptRuntime compile) {
    String[] varNames = compile.getVarNames();
    if (varNames != null && varNames.length > 0) {
      Set<String> methods = new HashSet<>();
      Set<String> collect =
          Stream.of(CodecMethod.values()).map(Enum::name).collect(Collectors.toSet());
      for (String method : varNames) {
        if (collect.contains(method)) {
          methods.add(method);
        }
      }
      methodCache.put(provider, methods);
      log.info("evalMethodCache provider={}, methods={}", provider, methods);
    }
  }

  @Override
  public String decode(ProtocolDecodeRequest decodeRequest) throws CodecException {
    try {
      if (!magicDecoderProvider.containsKey(decodeRequest.getDefinition().getProvider())) {
        // 增加同步锁，防止异常
        synchronized (magicDecoderProvider) {
          log.info(
              "magic decoder not exist,key={}, start reload ",
              decodeRequest.getDefinition().getProvider());
          load(decodeRequest.getDefinition(), CodecMethod.decode);
        }
      }
      // 如果编解码内部不包含decode方法，则直接返回原串
      if (methodCache.get(decodeRequest.getDefinition().getId()) == null
          || !methodCache
              .get(decodeRequest.getDefinition().getId())
              .contains(ProtocolCodecSupport.CodecMethod.decode.name())) {
        return decodeRequest.getPayload();
      }
      MagicScript magicScript =
          magicDecoderProvider.get(decodeRequest.getDefinition().getProvider());
      MagicScriptContext context = new MagicScriptContext();
      context.set("payload", decodeRequest.getPayload());
      context.set("context", decodeRequest.getContext());
      Object result = magicScript.execute(context);
      return str(result);
    } catch (Exception e) {
      String error = ExceptionUtil.getRootCauseMessage(e);
      log.error(
          "产品型号={} 提供者={} magic解码失败",
          decodeRequest.getDefinition().getId(),
          decodeRequest.getDefinition().getProvider(),
          e);
      throw new CodecException(error);
    }
  }

  @Override
  public String encode(ProtocolEncodeRequest encodeRequest) throws CodecException {
    try {
      if (!magicEncoderProvider.containsKey(encodeRequest.getDefinition().getProvider())) {
        log.info(
            "magic encode not exist,key={}, start reload ",
            encodeRequest.getDefinition().getProvider());
        synchronized (magicEncoderProvider) {
          load(encodeRequest.getDefinition(), CodecMethod.encode);
        }
      }
      // 如果编解码内部不包含 encode 方法，则直接返回原串
      if (methodCache.get(encodeRequest.getDefinition().getId()) == null
          || !methodCache
              .get(encodeRequest.getDefinition().getId())
              .contains(ProtocolCodecSupport.CodecMethod.encode.name())) {
        return encodeRequest.getPayload();
      }
      MagicScript magicScript =
          magicEncoderProvider.get(encodeRequest.getDefinition().getProvider());
      MagicScriptContext context = new MagicScriptContext();
      context.set("payload", encodeRequest.getPayload());
      context.set("context", encodeRequest.getContext());
      Object execute = magicScript.execute(context);
      return str(execute);
    } catch (Exception e) {
      String error = ExceptionUtil.getRootCauseMessage(e);
      log.error(
          "产品型号={} 提供者={} magic编码失败",
          encodeRequest.getDefinition().getId(),
          encodeRequest.getDefinition().getProvider(),
          e);
      throw new CodecException(error);
    }
  }

  @Override
  public void remove(String provider) {
    log.info("开始remove magic provider={}", provider);
    if (StrUtil.isNotBlank(provider)) {
      MagicScript encodeV = magicEncoderProvider.remove(provider);
      MagicScript decodeV = magicDecoderProvider.remove(provider);
      MagicScript preDecodeV = magicPreDecoderProvider.remove(provider);
      Set<String> methodV = methodCache.remove(provider);
      log.info(
          "remove encodeV={},decodeV={},preDecodeV={},methodV={}",
          encodeV,
          decodeV,
          preDecodeV,
          methodV);
    }
  }

  @Override
  public void load(String provider, Object providerImpl) {}

  @Override
  public String preDecode(ProtocolDecodeRequest protocolDecodeRequest) throws CodecException {
    try {
      if (!magicPreDecoderProvider.containsKey(
          protocolDecodeRequest.getDefinition().getProvider())) {
        log.info(
            "magic preDecode not exist,key={}, start reload ",
            protocolDecodeRequest.getDefinition().getProvider());
        synchronized (magicPreDecoderProvider) {
          load(protocolDecodeRequest.getDefinition(), CodecMethod.preDecode);
        }
      }
      // 如果编解码内部不包含 preDecode 方法，则直接返回原串
      if (methodCache.get(protocolDecodeRequest.getDefinition().getId()) == null
          || !methodCache
              .get(protocolDecodeRequest.getDefinition().getId())
              .contains(ProtocolCodecSupport.CodecMethod.preDecode.name())) {
        return protocolDecodeRequest.getPayload();
      }
      MagicScript magicScript =
          magicPreDecoderProvider.get(protocolDecodeRequest.getDefinition().getProvider());
      MagicScriptContext context = new MagicScriptContext();
      context.set("payload", protocolDecodeRequest.getPayload());
      context.set("context", protocolDecodeRequest.getContext());
      Object result = magicScript.execute(context);
      return str(result);
    } catch (Exception e) {
      String error = ExceptionUtil.getRootCauseMessage(e);
      log.error(
          "产品型号={} 提供者={} magic前置解码失败{}",
          protocolDecodeRequest.getDefinition().getId(),
          protocolDecodeRequest.getDefinition().getProvider(),
          error);
      throw new CodecException(error);
    }
  }

  // 在ProtocolCodecMagic类中修改isLoaded方法

  @Override
  public boolean isLoaded(String provider, CodecMethod codecMethod) {
      return switch (codecMethod) {
          case decode -> magicDecoderProvider.containsKey(provider);
          case encode -> magicEncoderProvider.containsKey(provider);
          case preDecode -> magicPreDecoderProvider.containsKey(provider);
          case iotToYour -> magicEncoderProvider.containsKey(provider); // 使用encode的provider
          case yourToIot -> magicDecoderProvider.containsKey(provider); // 使用decode的provider
          default -> false;
      };
  }

  @Override
  public String iotToYour(ProtocolEncodeRequest encodeRequest) throws CodecException {
    try {
      if (!magicEncoderProvider.containsKey(encodeRequest.getDefinition().getProvider())) {
        synchronized (magicEncoderProvider) {
          log.info("magic encoder not exist,key={}", encodeRequest.getDefinition().getProvider());
          load(encodeRequest.getDefinition(), CodecMethod.iotToYour);
        }
      }
      // 如果编解码内部不包含 iotToYour 方法，则使用encode方法
      if (methodCache.get(encodeRequest.getDefinition().getId()) == null
          || !methodCache
              .get(encodeRequest.getDefinition().getId())
              .contains(CodecMethod.iotToYour.name())) {
        return encode(encodeRequest);
      }
      MagicScript magicScript =
          magicEncoderProvider.get(encodeRequest.getDefinition().getProvider());
      MagicScriptContext context = new MagicScriptContext();
      context.set("payload", encodeRequest.getPayload());
      context.set("context", encodeRequest.getContext());
      Object execute = magicScript.execute(context);
      log.debug("execute={}", execute.getClass());
      return str(execute);
    } catch (Exception e) {
      String error = ExceptionUtil.getRootCauseMessage(e);
      log.error(
          "产品型号={} 提供者={} magic iotToYour失败",
          encodeRequest.getDefinition().getId(),
          encodeRequest.getDefinition().getProvider(),
          e);
      throw new CodecException(error);
    }
  }

  @Override
  public String yourToIot(ProtocolDecodeRequest decodeRequest) throws CodecException {
    try {
      if (!magicDecoderProvider.containsKey(decodeRequest.getDefinition().getProvider())) {
        log.info("magic decoder not exist,key={}", decodeRequest.getDefinition().getProvider());
        synchronized (magicDecoderProvider) {
          load(decodeRequest.getDefinition(), CodecMethod.yourToIot);
        }
      }
      // 如果编解码内部不包含 yourToIot 方法，则使用decode方法
      if (methodCache.get(decodeRequest.getDefinition().getId()) == null
          || !methodCache
              .get(decodeRequest.getDefinition().getId())
              .contains(CodecMethod.yourToIot.name())) {
        return decode(decodeRequest);
      }
      MagicScript magicScript =
          magicDecoderProvider.get(decodeRequest.getDefinition().getProvider());
      MagicScriptContext context = new MagicScriptContext();
      context.set("payload", decodeRequest.getPayload());
      context.set("context", decodeRequest.getContext());
      Object result = magicScript.execute(context);
      return str(result);
    } catch (Exception e) {
      String error = ExceptionUtil.getRootCauseMessage(e);
      log.error(
          "产品型号={} 提供者={} magic yourToIot失败",
          decodeRequest.getDefinition().getId(),
          decodeRequest.getDefinition().getProvider(),
          e);
      throw new CodecException(error);
    }
  }
}
