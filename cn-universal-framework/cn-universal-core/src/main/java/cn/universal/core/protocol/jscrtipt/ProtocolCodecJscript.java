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

package cn.universal.core.protocol.jscrtipt;

import cn.hutool.core.codec.Base64Decoder;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.universal.common.exception.CodecException;
import cn.universal.common.exception.IoTException;
import cn.universal.core.protocol.request.ProtocolDecodeRequest;
import cn.universal.core.protocol.request.ProtocolEncodeRequest;
import cn.universal.core.protocol.support.ProtocolCodecLoader;
import cn.universal.core.protocol.support.ProtocolCodecSupport;
import cn.universal.core.protocol.support.ProtocolCodecSupportWrapper;
import cn.universal.core.protocol.support.ProtocolCodecWrapper;
import cn.universal.core.protocol.support.ProtocolSupportDefinition;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 * jar加解密插件支持，动态加载jar包
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/8/9 19:28
 */
@Slf4j
public class ProtocolCodecJscript extends ProtocolCodecSupportWrapper
    implements ProtocolCodecLoader, ProtocolCodecSupport, ProtocolCodecWrapper {

  /** provider -> 已加载脚本的沙箱 Context（持有脚本全局绑定） */
  private final Map<String, Context> codecJscriptProvider = new ConcurrentHashMap<>();

  private final Map<String, Set<String>> methodCache = new ConcurrentHashMap<>();

  private ProtocolCodecJscript() {}

  private static class ProtocolCodecJscriptProviderHoler {

    private static ProtocolCodecJscript INSTANCE = new ProtocolCodecJscript();
  }

  public static ProtocolCodecJscript getInstance() {
    return ProtocolCodecJscriptProviderHoler.INSTANCE;
  }

  @Override
  public String getProviderType() {
    return "jscript";
  }

  @Override
  public void load(ProtocolSupportDefinition definition) throws CodecException {
    try {
      Map<String, Object> config = definition.getConfiguration();
      // JS 原始代码（字符串内容或 HTTP URL）
      String location =
          Optional.ofNullable(config.get("location"))
              .map(String::valueOf)
              .orElseThrow(
                  () ->
                      new IoTException(
                          "javascript source code not exist, can not do encode or decode"));
      // 获取提供者，产品唯一
      String provider =
          Optional.ofNullable(config.get("provider"))
              .map(String::valueOf)
              .map(String::trim)
              .orElseThrow(() -> new IllegalArgumentException("provider"));

      // 加载脚本源码（支持 HTTP 远程拉取）
      String scriptSource;
      if (location.startsWith("http://") || location.startsWith("https://")) {
        scriptSource = HttpUtil.downloadString(location, Charset.defaultCharset());
      } else {
        scriptSource = location;
      }

      // 使用沙箱 Context 执行脚本（禁止访问环境变量、文件系统、宿主类等）
      Context context = JsMethodExtractor.buildSandboxContext();
      context.eval("js", scriptSource);

      // 缓存方法名
      evalMethodCache(definition, context);
      codecJscriptProvider.put(provider, context);
    } catch (Exception e) {
      String error = ExceptionUtil.getMessage(e);
      log.error("加载jscript编解码出错={}", error);
      throw new CodecException(error);
    }
  }

  private void evalMethodCache(ProtocolSupportDefinition definition, Context context) {
    Set<String> methods = new HashSet<>();
    Value global = context.getBindings("js");
    for (String key : global.getMemberKeys()) {
      methods.add(key);
    }
    methodCache.put(definition.getId(), methods);
  }

  /**
   * 在已加载的沙箱 Context 中调用顶级 JS 函数。
   * 仅传递基本类型或 String，避免将敏感对象注入 JS 沙箱。
   */
  private Value invokeJs(Context context, String methodName, Object... args) {
    Value fn = context.getBindings("js").getMember(methodName);
    if (fn == null || !fn.canExecute()) {
      throw new IllegalStateException("JS 函数不存在或不可调用: " + methodName);
    }
    return fn.execute(args);
  }

  @Override
  public String decode(ProtocolDecodeRequest decodeRequest) throws CodecException {
    if (!codecJscriptProvider.containsKey(decodeRequest.getDefinition().getProvider())) {
      load(decodeRequest.getDefinition());
    }
    // 如果编解码内部不包含 decode 方法，则直接返回原串
    if (methodCache.get(decodeRequest.getDefinition().getId()) == null
        || !methodCache
            .get(decodeRequest.getDefinition().getId())
            .contains(CodecMethod.decode.name())) {
      return decodeRequest.getPayload();
    }
    Context context = codecJscriptProvider.get(decodeRequest.getDefinition().getProvider());
    try {
      Boolean needBs4Decode =
          (Boolean)
              decodeRequest.getDefinition().getConfiguration().getOrDefault("needBs4Decode", false);
      String payload = decodeRequest.getPayload();
      // 发现是 Base64，进行解码且做 16 进制转换
      if (needBs4Decode) {
        payload = HexUtil.encodeHexStr(Base64Decoder.decode(payload));
      }
      Value result = invokeJs(context, CodecMethod.decode.name(), payload, decodeRequest.getContext());
      return str(result);
    } catch (Exception e) {
      String error = ExceptionUtil.getMessage(e);
      log.info("设备 js 解码 payload = {}", decodeRequest.getPayload());
      log.error(
          "产品型号={} 提供者={} Jscript解码失败={}",
          decodeRequest.getDefinition().getId(),
          decodeRequest.getDefinition().getProvider(),
          e);
      throw new CodecException(error);
    }
  }

  @Override
  public String encode(ProtocolEncodeRequest encodeRequest) throws CodecException {
    if (!codecJscriptProvider.containsKey(encodeRequest.getDefinition().getProvider())) {
      load(encodeRequest.getDefinition());
    }
    // 如果编解码内部不包含 encode 方法，则直接返回原串
    if (methodCache.get(encodeRequest.getDefinition().getId()) == null
        || !methodCache
            .get(encodeRequest.getDefinition().getId())
            .contains(CodecMethod.encode.name())) {
      return encodeRequest.getPayload();
    }
    Context context = codecJscriptProvider.get(encodeRequest.getDefinition().getProvider());
    try {
      Value result =
          invokeJs(
              context,
              CodecMethod.encode.name(),
              encodeRequest.getPayload(),
              encodeRequest.getContext());
      return str(result);
    } catch (Exception e) {
      String error = ExceptionUtil.getMessage(e);
      log.error(
          "产品型号={} 提供者={} Jscript编码失败={}",
          encodeRequest.getDefinition().getId(),
          encodeRequest.getDefinition().getProvider(),
          e);
      throw new CodecException(error);
    }
  }

  @Override
  public void remove(String provider) {
    if (StrUtil.isNotBlank(provider)) {
      Context ctx = codecJscriptProvider.remove(provider);
      if (ctx != null) {
        try {
          ctx.close();
        } catch (Exception ignored) {
        }
      }
    }
  }

  @Override
  public void load(String provider, Object providerImpl) {}

  @Override
  public String preDecode(ProtocolDecodeRequest protocolDecodeRequest) throws CodecException {
    if (!codecJscriptProvider.containsKey(protocolDecodeRequest.getDefinition().getProvider())) {
      load(protocolDecodeRequest.getDefinition());
    }
    // 如果编解码内部不包含 preDecode 方法，则直接返回原串
    if (methodCache.get(protocolDecodeRequest.getDefinition().getId()) == null
        || !methodCache
            .get(protocolDecodeRequest.getDefinition().getId())
            .contains(CodecMethod.preDecode.name())) {
      return protocolDecodeRequest.getPayload();
    }
    Context context =
        codecJscriptProvider.get(protocolDecodeRequest.getDefinition().getProvider());
    try {
      Value result =
          invokeJs(
              context,
              CodecMethod.preDecode.name(),
              protocolDecodeRequest.getPayload(),
              protocolDecodeRequest.getContext());
      return str(result);
    } catch (Exception e) {
      String error = ExceptionUtil.getMessage(e);
      log.error(
          "产品型号={} 提供者={} Jscript预编码失败={}",
          protocolDecodeRequest.getDefinition().getId(),
          protocolDecodeRequest.getDefinition().getProvider(),
          e);
      throw new CodecException(error);
    }
  }

  // 在ProtocolCodecJscript类中实现isLoaded方法
  @Override
  public boolean isLoaded(String provider, CodecMethod codecMethod) {
    // 检查provider是否是否已加载到缓存中
    if (!codecJscriptProvider.containsKey(provider)) {
      return false;
    }
    // 检查是否是否包含目标方法（可选，根据实际需求决定
    Set<String> methods = methodCache.get(provider);
    if (methods == null) {
      return false;
    }
    return methods.contains(codecMethod.name());
  }

  @Override
  public String iotToYour(ProtocolEncodeRequest encodeRequest) throws CodecException {
    if (!codecJscriptProvider.containsKey(encodeRequest.getDefinition().getProvider())) {
      load(encodeRequest.getDefinition());
    }
    // 如果编解码内部不包含 iotToYour 方法，则使用encode方法
    if (methodCache.get(encodeRequest.getDefinition().getId()) == null
        || !methodCache
            .get(encodeRequest.getDefinition().getId())
            .contains(CodecMethod.iotToYour.name())) {
      return encode(encodeRequest);
    }
    Context context = codecJscriptProvider.get(encodeRequest.getDefinition().getProvider());
    try {
      Value result =
          invokeJs(
              context,
              CodecMethod.iotToYour.name(),
              encodeRequest.getPayload(),
              encodeRequest.getContext());
      return str(result);
    } catch (Exception e) {
      String error = ExceptionUtil.getMessage(e);
      log.error(
          "产品型号={} 提供者={} Jscript iotToYour失败={}",
          encodeRequest.getDefinition().getId(),
          encodeRequest.getDefinition().getProvider(),
          e);
      throw new CodecException(error);
    }
  }

  @Override
  public String yourToIot(ProtocolDecodeRequest decodeRequest) throws CodecException {
    if (!codecJscriptProvider.containsKey(decodeRequest.getDefinition().getProvider())) {
      load(decodeRequest.getDefinition());
    }
    // 如果编解码内部不包含 yourToIot 方法，则使用decode方法
    if (methodCache.get(decodeRequest.getDefinition().getId()) == null
        || !methodCache
            .get(decodeRequest.getDefinition().getId())
            .contains(CodecMethod.yourToIot.name())) {
      return decode(decodeRequest);
    }
    Context context = codecJscriptProvider.get(decodeRequest.getDefinition().getProvider());
    try {
      Value result =
          invokeJs(
              context,
              CodecMethod.yourToIot.name(),
              decodeRequest.getPayload(),
              decodeRequest.getContext());
      return str(result);
    } catch (Exception e) {
      String error = ExceptionUtil.getMessage(e);
      log.error(
          "产品型号={} 提供者={} Jscript yourToIot失败={}",
          decodeRequest.getDefinition().getId(),
          decodeRequest.getDefinition().getProvider(),
          e);
      throw new CodecException(error);
    }
  }
}
