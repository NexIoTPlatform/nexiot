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

package cn.universal.core.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.common.exception.CodecException;
import cn.universal.core.message.UPRequest;
import cn.universal.core.protocol.jar.ProtocolCodecJar;
import cn.universal.core.protocol.jscrtipt.ProtocolCodecJscript;
import cn.universal.core.protocol.magic.ProtocolCodecMagic;
import cn.universal.core.protocol.request.ProtocolDecodeRequest;
import cn.universal.core.protocol.request.ProtocolEncodeRequest;
import cn.universal.core.protocol.support.ProtocolCodecSupport;
import cn.universal.core.protocol.support.ProtocolCodecSupport.CodecMethod;
import cn.universal.core.protocol.support.ProtocolSupportDefinition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象编解码服务基类
 *
 * <p>提供公共的编解码实现，子类可以重写特定方法
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/01/20
 */
@Slf4j
public abstract class AbstractCodecService implements ICodecService {

  /**
   * 获取协议定义，确保脚本已加载
   *
   * @param productKey 产品Key
   * @param codecMethod 编解码方法
   * @return 协议支持定义
   */
  protected ProtocolSupportDefinition getProtocolDefinitionWithScriptIfNeeded(
      String productKey, CodecMethod codecMethod) {
    ProtocolSupportDefinition protocolDef = getProtocolDefinitionNoScript(productKey);

    if (protocolDef != null && protocolDef.supportMethod(codecMethod)) {
      ProtocolCodecSupport protocolCodecSupport = getProtocolCodecProvider(protocolDef.getType());
      if (!protocolCodecSupport.isLoaded(protocolDef.getProvider(), codecMethod)) {
        synchronized (this) {
          protocolDef = getProtocolDefinitionWithScript(productKey);
        }
      }
    }

    return protocolDef;
  }

  /** 解码 - 带上下文 */
  @Override
  public <R> List<R> decode(
      String productKey, String payload, Object context, Class<R> elementType) {
    ProtocolSupportDefinition protocolDef =
        getProtocolDefinitionWithScriptIfNeeded(productKey, CodecMethod.decode);
    List<R> rs = null;
    long t1 = System.currentTimeMillis();

    if (protocolDef != null && protocolDef.supportMethod(CodecMethod.decode)) {
      ProtocolCodecSupport protocolCodecSupport = getProtocolCodecProvider(protocolDef.getType());
      try {
        String result =
            protocolCodecSupport.decode(new ProtocolDecodeRequest(protocolDef, payload, context));
        if (StrUtil.isNotBlank(result)) {
          if (JSONUtil.isTypeJSONObject(result)) {
            rs = Stream.of(JSONUtil.toBean(result, elementType)).collect(Collectors.toList());
          } else if (JSONUtil.isTypeJSONArray(result)) {
            rs = JSONUtil.toList(result, elementType);
          } else {
            rs = emptyProtocol(result, elementType);
          }
        }
        log.debug("产品ProductKey={} 原始报文={} 解码报文={}", productKey, payload, result);
      } catch (Exception e) {
        log.error("产品编号={} 原始报文={} 解码报错", productKey, payload, e);
      }
    } else {
      rs = emptyProtocol(payload, elementType);
    }

    long t2 = System.currentTimeMillis();
    log.info(
        "产品编号={} 原始报文={} 解码={} 耗时={}ms", productKey, payload, JSONUtil.toJsonStr(rs), (t2 - t1));
    return rs;
  }

  /** 解码 - 简化版本 */
  @Override
  public <R> List<R> decode(String productKey, String payload, Class<R> elementType) {
    return decode(productKey, payload, null, elementType);
  }

  /** 解码为UPRequest列表 */
  @Override
  public List<UPRequest> decode(String productKey, String payload) {
    return decode(productKey, payload, null, UPRequest.class);
  }

  /** 编码 */
  @Override
  public String encode(String productKey, String payload) {
    return encode(productKey, payload, null);
  }

  @Override
  public String encode(String productKey, String payload, Object context) {
    ProtocolSupportDefinition protocolDef =
        getProtocolDefinitionWithScriptIfNeeded(productKey, CodecMethod.encode);
    String result = null;
    long t1 = System.currentTimeMillis();

    if (protocolDef != null && protocolDef.supportMethod(CodecMethod.encode)) {
      ProtocolCodecSupport protocolCodecSupport = getProtocolCodecProvider(protocolDef.getType());
      try {
        result =
            protocolCodecSupport.encode(new ProtocolEncodeRequest(protocolDef, payload, context));
      } catch (CodecException e) {
        log.error("产品编号={} 原始报文={} 编码报错", productKey, payload, e);
      }
    }

    long t2 = System.currentTimeMillis();
    log.info("产品编号={} 原始报文={}  编码={} 耗时={}ms", productKey, payload, result, (t2 - t1));
    return result;
  }

  /** 预解码 */
  @Override
  public UPRequest preDecode(String productKey, String payload, Object context) {
    ProtocolSupportDefinition protocolDef =
        getProtocolDefinitionWithScriptIfNeeded(productKey, CodecMethod.preDecode);
    UPRequest result = null;
    long t1 = System.currentTimeMillis();

    if (protocolDef != null && protocolDef.supportMethod(CodecMethod.preDecode)) {
      ProtocolCodecSupport protocolCodecSupport = getProtocolCodecProvider(protocolDef.getType());
      try {
        String preDecodeResult =
            protocolCodecSupport.preDecode(
                new ProtocolDecodeRequest(protocolDef, payload, context));
        if (StrUtil.isNotBlank(preDecodeResult)) {
          result = JSONUtil.toBean(preDecodeResult, UPRequest.class);
        }
      } catch (Exception e) {
        log.error("产品编号={} 原始报文={} 预解码报错", productKey, payload, e);
      }
    }

    long t2 = System.currentTimeMillis();
    log.info("产品编号={} 原始报文={} 预解码={} 耗时={}ms", productKey, payload, result, (t2 - t1));
    return result;
  }

  /** 通用编解码方法 - 支持所有CodecMethod类型 */
  @Override
  public String codec(String productKey, String payload, CodecMethod codecMethod) {
    ProtocolSupportDefinition protocolDef =
        getProtocolDefinitionWithScriptIfNeeded(productKey, codecMethod);
    String result = null;
    long t1 = System.currentTimeMillis();

    if (protocolDef != null && protocolDef.supportMethod(codecMethod)) {
      ProtocolCodecSupport protocolCodecSupport = getProtocolCodecProvider(protocolDef.getType());
      try {
        switch (codecMethod) {
          case decode:
            result = protocolCodecSupport.decode(new ProtocolDecodeRequest(protocolDef, payload));
            break;
          case encode:
            result = protocolCodecSupport.encode(new ProtocolEncodeRequest(protocolDef, payload));
            break;
          case preDecode:
            result =
                protocolCodecSupport.preDecode(new ProtocolDecodeRequest(protocolDef, payload));
            break;
          case codecAdd:
            result = protocolCodecSupport.encode(new ProtocolEncodeRequest(protocolDef, payload));
            break;
          case codecDelete:
            result = protocolCodecSupport.encode(new ProtocolEncodeRequest(protocolDef, payload));
            break;
          case codecUpdate:
            result = protocolCodecSupport.encode(new ProtocolEncodeRequest(protocolDef, payload));
            break;
          case codecQuery:
            result = protocolCodecSupport.decode(new ProtocolDecodeRequest(protocolDef, payload));
            break;
          case iotToYour:
            // 使用基础执行引擎的iotToYour方法
            result =
                protocolCodecSupport.iotToYour(new ProtocolEncodeRequest(protocolDef, payload));
            break;
          case yourToIot:
            // 使用基础执行引擎的yourToIot方法
            result =
                protocolCodecSupport.yourToIot(new ProtocolDecodeRequest(protocolDef, payload));
            break;
          case codecFunction:
            // 映射到iotToYour
            result =
                protocolCodecSupport.iotToYour(new ProtocolEncodeRequest(protocolDef, payload));
            break;
          case codecOther:
            // 映射到yourToIot
            result =
                protocolCodecSupport.yourToIot(new ProtocolDecodeRequest(protocolDef, payload));
            break;
          default:
            log.warn("不支持的编解码方法: {}", codecMethod);
            break;
        }
      } catch (Exception e) {
        log.error("产品编号={} 原始报文={} {}报错", productKey, payload, codecMethod, e);
      }
    }

    long t2 = System.currentTimeMillis();
    log.info("产品编号={} 原始报文={} {}={} 耗时={}ms", productKey, payload, codecMethod, result, (t2 - t1));
    return result;
  }

  /** 检查是否支持指定的编解码方法 */
  @Override
  public boolean isSupported(String productKey, CodecMethod codecMethod) {
    ProtocolSupportDefinition protocolDef = getProtocolDefinitionNoScript(productKey);
    return protocolDef != null && protocolDef.supportMethod(codecMethod);
  }

  /** IoT到第三方数据转换编解码 */
  @Override
  public String iotToYour(String productKey, String payload) {
    return iotToYour(productKey, payload, null);
  }

  @Override
  public String iotToYour(String productKey, String payload, Object context) {
    return transform(productKey, payload, context, CodecMethod.iotToYour);
  }

  /** 第三方到IoT数据转换编解码 */
  @Override
  public String yourToIot(String productKey, String payload) {
    return yourToIot(productKey, payload, null);
  }

  @Override
  public String yourToIot(String productKey, String payload, Object context) {
    return transform(productKey, payload, context, CodecMethod.yourToIot);
  }

  private String transform(
      String productKey, String payload, Object context, CodecMethod codecMethod) {
    ProtocolSupportDefinition protocolDef =
        getProtocolDefinitionWithScriptIfNeeded(productKey, codecMethod);
    if (protocolDef == null || !protocolDef.supportMethod(codecMethod)) {
      return payload;
    }
    ProtocolCodecSupport support = getProtocolCodecProvider(protocolDef.getType());
    if (support == null) {
      return payload;
    }
    try {
      if (CodecMethod.iotToYour.equals(codecMethod)) {
        return support.iotToYour(new ProtocolEncodeRequest(protocolDef, payload, context));
      }
      return support.yourToIot(new ProtocolDecodeRequest(protocolDef, payload, context));
    } catch (Exception e) {
      log.error("产品编号={} 原始报文={} {}报错", productKey, payload, codecMethod, e);
      return null;
    }
  }

  /**
   * 获取协议定义 - 需要子类实现
   *
   * @param productKey 产品Key
   * @return 协议支持定义
   */
  protected abstract ProtocolSupportDefinition getProtocolDefinition(String productKey);

  /**
   * 获取协议定义（不包含脚本内容）- 需要子类实现
   *
   * <p>用于性能优化，避免每次加载包含脚本内容的完整协议定义
   *
   * @param productKey 产品Key
   * @return 协议支持定义（不包含脚本内容）
   */
  protected abstract ProtocolSupportDefinition getProtocolDefinitionNoScript(String productKey);

  /**
   * 获取协议定义（带脚本内容）- 需要子类实现
   *
   * @param productKey 产品Key
   * @return 协议支持定义（包含脚本内容）
   */
  protected abstract ProtocolSupportDefinition getProtocolDefinitionWithScript(String productKey);

  /**
   * 获取编解码提供者
   *
   * @param supportType 支持类型
   * @return 协议编解码支持实例
   */
  protected ProtocolCodecSupport getProtocolCodecProvider(String supportType) {
    if (supportType == null) {
      return null;
    }
    if (supportType.equalsIgnoreCase("jar") || supportType.equalsIgnoreCase("spring")) {
      return ProtocolCodecJar.getInstance();
    } else if (supportType.equalsIgnoreCase("jscript")) {
      return ProtocolCodecJscript.getInstance();
    } else if (supportType.equalsIgnoreCase("magic")) {
      return ProtocolCodecMagic.getInstance();
    }
    return null;
  }

  /**
   * 当编解码为空时的处理 - 泛型版本
   *
   * @param payload 原始数据
   * @param elementType 目标类型
   * @param <R> 泛型类型
   * @return 处理后的对象列表
   */
  protected <R> List<R> emptyProtocol(String payload, Class<R> elementType) {
    List<R> list = new ArrayList<>();
    try {
      // 1.消息是JSON
      List<R> list1 = doJson(payload, elementType, list);
      if (list1 != null) return list1;

      // 2.消息是JSONArray
      List<R> list2 = doJsonArray(payload, elementType, list);
      if (list2 != null) return list2;

      // 3.不是JSON也不是JSONArray，创建UPRequest
      UPRequest upRequest = new UPRequest();
      upRequest.setEmptyProtocol(true);
      upRequest.setMessageType(MessageType.PROPERTIES);
      Map<String, Object> properties = new HashMap<>();
      properties.put("sourcePayload", payload);
      properties.put("noCodec", true);
      upRequest.setPayload(payload);
      upRequest.setProperties(properties);
      // 将UPRequest转换为R类型
      R upMsg = JSONUtil.toBean(JSONUtil.toJsonStr(upRequest), elementType);
      list.add(upMsg);
    } catch (Exception e) {
      log.warn("字符串转换失败: {}", e.getMessage());
    }
    return list;
  }

  /**
   * 处理JSON格式的数据
   *
   * @param payload 原始数据
   * @param elementType 目标类型
   * @param list 结果列表
   * @param <R> 泛型类型
   * @return 处理后的对象列表
   */
  private <R> List<R> doJson(String payload, Class<R> elementType, List<R> list) {
    if (JSONUtil.isTypeJSON(payload)) {
      JSONObject payloadJson = JSONUtil.parseObj(payload);

      // 平台的消息结构示例：
      // {
      //   "messageType": "PROPERTIES",
      //   "properties": {
      //     "battery": "93",
      //     "csq": 26,
      //     "ecl": "87",
      //     "sn": "37532",
      //     "switchStatus": 0
      //   }
      // }
      if (payloadJson != null
              && (MessageType.PROPERTIES
                  .name()
                  .equalsIgnoreCase(payloadJson.getStr("messageType", "")))
          || MessageType.EVENT.name().equalsIgnoreCase(payloadJson.getStr("messageType", ""))) {
        R upMsg = JSONUtil.toBean(payloadJson, elementType);
        list.add(upMsg);
        return list;
      }
      // 否则可能是物模型结构示例：
      // {
      //   "battery": "93",
      //   "csq": 26,
      //   "ecl": "87",
      //   "sn": "37532",
      //   "switchStatus": 0
      // }
      else {
        UPRequest things = new UPRequest();
        things.setProperties(payloadJson);
        things.setMessageType(MessageType.PROPERTIES);
        things.setPayload(payload);
        // 将UPRequest转换为R类型
        R upMsg = JSONUtil.toBean(JSONUtil.toJsonStr(things), elementType);
        list.add(upMsg);
        return list;
      }
    }
    return null;
  }

  /**
   * 处理JSONArray格式的数据
   *
   * @param payload 原始数据
   * @param elementType 目标类型
   * @param list 结果列表
   * @param <R> 泛型类型
   * @return 处理后的对象列表
   */
  private <R> List<R> doJsonArray(String payload, Class<R> elementType, List<R> list) {
    if (JSONUtil.isTypeJSONArray(payload)) {
      JSONArray payloadArray = JSONUtil.parseArray(payload);

      // JSONArray结构示例：
      // [
      //   {
      //     "messageType": "PROPERTIES",
      //     "properties": {
      //       "battery": "93",
      //       "csq": 26
      //     }
      //   },
      //   {
      //     "messageType": "EVENT",
      //     "event": "alarm",
      //     "data": {
      //       "level": "high"
      //     }
      //   }
      // ]
      for (int i = 0; i < payloadArray.size(); i++) {
        JSONObject item = payloadArray.getJSONObject(i);
        if (item != null) {
          R upMsg = JSONUtil.toBean(item, elementType);
          list.add(upMsg);
        }
      }
      return list;
    }
    return null;
  }
}
