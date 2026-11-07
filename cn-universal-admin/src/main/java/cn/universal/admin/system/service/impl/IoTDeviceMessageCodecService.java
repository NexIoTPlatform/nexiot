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

package cn.universal.admin.system.service.impl;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.exception.CodecException;
import cn.universal.persistence.entity.bo.IoTDeviceProtocolBO;
import cn.universal.core.protocol.jscrtipt.JsMethodExtractor;
import cn.universal.core.protocol.jar.ProtocolCodecJar;
import cn.universal.core.protocol.request.ProtocolDecodeRequest;
import cn.universal.core.protocol.request.ProtocolEncodeRequest;
import cn.universal.core.protocol.jscrtipt.ProtocolCodecJscript;
import cn.universal.core.protocol.magic.MagicScriptMethodExtractor;
import cn.universal.core.protocol.magic.ProtocolCodecMagic;
import cn.universal.core.protocol.support.ProtocolCodecSupport;
import cn.universal.core.protocol.support.ProtocolCodecSupport.CodecMethod;
import cn.universal.core.protocol.support.ProtocolSupportDefinition;
import cn.universal.core.protocol.support.ProtocolCodecWrapper;
import cn.universal.core.service.ICodecService;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 在线编解码转换测试 */
@Service("messageCodecService")
@Slf4j
public class IoTDeviceMessageCodecService {

  @Resource private IoTProductDeviceService iotProductDeviceService;

  @Autowired private ICodecService codecService;

  // 预解析
  public Object preDecode(String productKey, Object payload) {
    if (productKey == null || payload == null) {
      return null;
    }
    try {
      Object result = codecService.preDecode(productKey, JSONUtil.toJsonStr(payload), null);
      log.info("产品编号={} 原始报文={} , preDecode解码={}", productKey, payload, JSONUtil.toJsonStr(result));
      return result;
    } catch (Exception e) {
      log.info(
          "产品编号={} 原始报文={} , preDecode解码异常", productKey, payload, ExceptionUtil.getRootCause(e));
      return payload;
    }
  }

  // 上行解码
  public Object decode(String productKey, String payload) throws CodecException {
    if (StrUtil.isBlank(payload)) {
      return null;
    }
    try {
      Object result = codecService.decode(productKey, payload);
      log.info("产品编号={} 原始报文={} , 解码={}", productKey, payload, JSONUtil.toJsonStr(result));
      return result;
    } catch (Exception e) {
      log.error("产品编号={} 原始报文={} , 解码异常", productKey, payload, e);
      return null;
    }
  }

  // 下行编码
  public String encode(String productKey, String payload) throws CodecException {
    try {
      String result = codecService.encode(productKey, payload);
      log.info("产品编号={} 原始报文={} , 编码={}", productKey, payload, JSONUtil.toJsonStr(result));
      return result;
    } catch (Exception e) {
      log.error("产品编号={} 原始报文={} , 编码异常", productKey, payload, e);
      return null;
    }
  }

  /** 临时脚本预览，仅用于未保存协议的在线调试。 */
  public Object preview(IoTDeviceProtocolBO protocolBO) throws CodecException {
    ProtocolSupportDefinition definition = buildPreviewDefinition(protocolBO);
    ProtocolCodecSupport provider = getProtocolCodecProvider(definition.getType());
    if (provider == null) {
      throw new CodecException("仅支持magic或jscript类型的预览调试");
    }
    String previewMethod = StrUtil.blankToDefault(protocolBO.getPreviewMethod(), "");
    if (StrUtil.isBlank(previewMethod)) {
      previewMethod = ObjectUtil.defaultIfNull(protocolBO.getDecode(), true) ? "decode" : "encode";
    }
    Object context = protocolBO.getContext();
    try {
      if ("preDecode".equalsIgnoreCase(previewMethod)) {
        return provider.preDecode(new ProtocolDecodeRequest(definition, protocolBO.getPayload(), context));
      }
      if ("decode".equalsIgnoreCase(previewMethod)) {
        return provider.decode(new ProtocolDecodeRequest(definition, protocolBO.getPayload(), context));
      }
      return provider.encode(new ProtocolEncodeRequest(definition, protocolBO.getPayload(), context));
    } finally {
      if (provider instanceof ProtocolCodecWrapper) {
        ((ProtocolCodecWrapper) provider).remove(definition.getProvider());
      }
    }
  }

  public ProtocolSupportDefinition buildPreviewDefinition(IoTDeviceProtocolBO protocolBO)
      throws CodecException {
    if (protocolBO == null || StrUtil.isBlank(protocolBO.getType())) {
      throw new CodecException("预览类型不能为空");
    }
    String type = protocolBO.getType();
    JSONObject configuration = parseConfiguration(protocolBO.getConfiguration());
    String provider = "preview_" + type + "_" + System.nanoTime();
    configuration.set("provider", provider);

    Set<String> targetMethods = new HashSet<>();
    Arrays.stream(CodecMethod.values()).forEach(method -> targetMethods.add(method.name()));

    Set<String> supportMethods;
    if ("magic".equals(type)) {
      if (StrUtil.isBlank(protocolBO.getJscript())) {
        throw new CodecException("Magic脚本不能为空");
      }
      configuration.set("needBs4Decode", ObjectUtil.defaultIfNull(protocolBO.getNeedBs4Decode(), false));
      configuration.set("location", protocolBO.getJscript());
      try {
        supportMethods =
            MagicScriptMethodExtractor.extractSupportMethods(protocolBO.getJscript(), targetMethods);
      } catch (RuntimeException e) {
        String message =
            StrUtil.blankToDefault(e.getMessage(), ExceptionUtil.getRootCauseMessage(e));
        throw new CodecException(message);
      }
    } else if ("jscript".equals(type)) {
      if (StrUtil.isBlank(protocolBO.getJscript())) {
        throw new CodecException("JavaScript脚本不能为空");
      }
      configuration.set("needBs4Decode", ObjectUtil.defaultIfNull(protocolBO.getNeedBs4Decode(), false));
      configuration.set("location", protocolBO.getJscript());
      supportMethods = new HashSet<>();
      for (String method : JsMethodExtractor.extractMethodNames(protocolBO.getJscript())) {
        if (targetMethods.contains(method)) {
          supportMethods.add(method);
        }
      }
    } else {
      throw new CodecException("仅支持magic或jscript类型的预览调试");
    }

    if (supportMethods.isEmpty()) {
      throw new CodecException("预编译失败,没有包含任何一个有效方法");
    }
    configuration.set("supportMethods", supportMethods);

    ProtocolSupportDefinition definition = new ProtocolSupportDefinition();
    definition.setId(provider);
    definition.setName(
        StrUtil.blankToDefault(protocolBO.getName(), provider));
    definition.setDescription(protocolBO.getDescription());
    definition.setProvider(provider);
    definition.setType(type);
    definition.setConfiguration(configuration);
    definition.setSupportMethods(supportMethods);
    definition.setState((byte) 1);
    return definition;
  }

  private JSONObject parseConfiguration(String configuration) {
    if (StrUtil.isBlank(configuration)) {
      return new JSONObject();
    }
    try {
      return JSONUtil.parseObj(configuration);
    } catch (Exception e) {
      log.warn("预览脚本配置解析失败，将使用空配置: {}", configuration, e);
      return new JSONObject();
    }
  }

  /**
   * 查询产品的解码
   *
   * @param productKey
   * @return
   */
  protected ProtocolSupportDefinition selectDevProtocolDef(String productKey) {
    return iotProductDeviceService.selectProtocolDef(productKey);
  }

  /**
   * 获取编解码插件，工程模式
   *
   * @param supportType
   * @return
   */
  protected ProtocolCodecSupport getProtocolCodecProvider(String supportType) {
    if (supportType == null) {
      return null;
    }
    if (supportType.equalsIgnoreCase("jar")) {
      return ProtocolCodecJar.getInstance();
    } else if (supportType.equalsIgnoreCase("jscript")) {
      return ProtocolCodecJscript.getInstance();
    } else if (supportType.equalsIgnoreCase("magic")) {
      return ProtocolCodecMagic.getInstance();
    }
    return null;
  }
}
