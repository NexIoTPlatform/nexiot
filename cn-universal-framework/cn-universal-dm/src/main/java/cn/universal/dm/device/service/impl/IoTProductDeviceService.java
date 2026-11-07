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

package cn.universal.dm.device.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.exception.IoTException;
import cn.universal.core.metadata.DeviceMetadata;
import cn.universal.core.protocol.support.ProtocolSupportDefinition;
import cn.universal.persistence.dto.IoTDeviceOfflineThesholdBO;
import cn.universal.persistence.dto.LogStorePolicyDTO;
import cn.universal.persistence.dto.LogStorePolicyDTO.StorePolicy;
import cn.universal.persistence.entity.IoTDeviceProtocol;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.bo.IoTProductBO;
import cn.universal.persistence.entity.vo.IoTProductVO;
import cn.universal.persistence.mapper.IoTDeviceProtocolMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.query.IoTAPIQuery;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * 设备和产品鉴权
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/8/12 16:10
 */
@Component
@Slf4j
public class IoTProductDeviceService {

  @Resource private IoTProductMapper ioTProductMapper;
  @Resource private IoTDeviceProtocolMapper ioTDeviceProtocolMapper;

  public Page<IoTProductVO> apiProductList(IoTAPIQuery iotAPIQuery) {
    Page<IoTProductVO> page = PageHelper.startPage(iotAPIQuery.getPage(), iotAPIQuery.getSize());
    // 传递查询条件到 Mapper
    List<IoTProductVO> devProducts = ioTProductMapper.openAPIProductList(iotAPIQuery);
    return page;
  }

  public IoTProductVO apiProductDetail(String productKey) {
    IoTProductVO productVO = ioTProductMapper.apiProductDetail(productKey);
    return productVO;
  }

  @Cacheable(cacheNames = "iot_product_device", key = "#productKey", unless = "#result == null")
  public IoTProduct getProduct(String productKey) {
    if (StrUtil.isBlank(productKey)) {
      throw new IoTException("product [" + productKey + "] can not be null");
    }
    // status-0 正常
    IoTProduct ioTProduct =
        IoTProduct.builder().productKey(productKey).state(IoTConstant.NORMAL.byteValue()).build();
    ioTProduct = ioTProductMapper.selectOne(ioTProduct);
    if (ioTProduct == null) {
      throw new IoTException("product [" + productKey + "] not exist");
    }
    return ioTProduct;
  }

  @Cacheable(cacheNames = "getDeviceMetadata", key = "#productKey", unless = "#result == null")
  public DeviceMetadata getDeviceMetadata(String productKey) {
    // status-0 正常
    IoTProduct product =
        IoTProduct.builder().productKey(productKey).state(IoTConstant.NORMAL.byteValue()).build();
    product = ioTProductMapper.selectOne(product);
    if (product == null || StrUtil.isBlank(product.getMetadata())) {
      return new DeviceMetadata(new JSONObject());
    }
    return new DeviceMetadata(new JSONObject(product.getMetadata()));
  }

  @Cacheable(
      cacheNames = "iot_product_log_store_policy",
      key = "#productKey",
      unless = "#result == null")
  public LogStorePolicyDTO getProductLogStorePolicy(String productKey) {
    IoTProduct product =
        ((IoTProductDeviceService) AopContext.currentProxy()).getProduct(productKey);
    String storePolicy = product.getStorePolicyConfiguration();
    Map<String, StorePolicy> propertiesMap = new HashMap<>();
    // 处理属性
    Map<String, StorePolicy> eventMap = new HashMap<>();
    if (StrUtil.isBlank(storePolicy)) {
      return LogStorePolicyDTO.builder().properties(propertiesMap).event(eventMap).build();
    }
    JSONObject storePolicyObj = JSONUtil.parseObj(storePolicy);
    storePolicyObj.getJSONArray("event").stream()
        .forEach(
            s -> {
              JSONObject ev = (JSONObject) s;
              eventMap.put(
                  ev.getStr("id"),
                  new StorePolicy(
                      ev.getStr("id"), ev.getInt("maxStorage", IoTConstant.maxStorage)));
            });
    storePolicyObj.getJSONArray("properties").stream()
        .forEach(
            s -> {
              JSONObject ev = (JSONObject) s;
              propertiesMap.put(
                  ev.getStr("id"),
                  new StorePolicy(
                      ev.getStr("id"), ev.getInt("maxStorage", IoTConstant.maxStorage)));
            });
    return LogStorePolicyDTO.builder().properties(propertiesMap).event(eventMap).build();
  }

  /** 加载产品解析协议 */
  @Cacheable(cacheNames = "iot_protocol_def", key = "#productKey", unless = "#result == null")
  public ProtocolSupportDefinition selectProtocolDef(String productKey) {
    if (StrUtil.isBlank(productKey)) {
      throw new IoTException("product [" + productKey + "] can not be null");
    }
    IoTDeviceProtocol ioTDeviceProtocol =
        ioTDeviceProtocolMapper.selectDevProtocolByProductKey(productKey);
    if (ioTDeviceProtocol != null) {
      return ioTDeviceProtocol.toDefinition();
    }
    return null;
  }

  /** 加载产品解析协议 */
  @Cacheable(
      cacheNames = "selectProtocolDefNoScript",
      key = "#productKey",
      unless = "#result == null")
  public ProtocolSupportDefinition selectProtocolDefNoScript(String productKey) {
    if (StrUtil.isBlank(productKey)) {
      throw new IoTException("product [" + productKey + "] can not be null");
    }
    IoTDeviceProtocol ioTDeviceProtocol =
        ioTDeviceProtocolMapper.selectDevProtocolByProductKey(productKey);
    if (ioTDeviceProtocol != null) {
      return ioTDeviceProtocol.toDefinitionNoScript();
    }
    return null;
  }

  /**
   * 根据产品编号和归属人查询总数
   *
   * @param productKey 产品key
   * @param unionId 用户唯一标识
   * @return int 总数
   */
  @Cacheable(
      cacheNames = "iot_product_count",
      key = "#unionId + ':' + #productKey",
      unless = "#result == null")
  public int selectProductCount(String productKey, String unionId) {
    if (StrUtil.isBlank(productKey) || StrUtil.isBlank(unionId)) {
      return 0;
    }
    return ioTProductMapper.selectCount(
        IoTProduct.builder().productKey(productKey).creatorId(unionId).build());
  }

  /** 获取产品配置列表的离线阈值，缺省值24小时 */
  @Cacheable(
      cacheNames = "iot_product_offline_threshold",
      key = "'all'",
      unless = "#result == null")
  public List<IoTDeviceOfflineThesholdBO> getProductOfflineThresholds() {
    // status-0 正常
    IoTProduct ioTProduct = IoTProduct.builder().state(IoTConstant.NORMAL.byteValue()).build();
    List<IoTProduct> select = ioTProductMapper.select(ioTProduct);
    List<IoTDeviceOfflineThesholdBO> productsMap = new ArrayList<>();
    if (!CollectionUtil.isEmpty(select)) {
      for (IoTProduct s : select) {
        int offlineThreshold = IoTConstant.DEFAULT_OFFLINE_THRESHOLD_VALUE;
        IoTDeviceOfflineThesholdBO ioTDeviceOfflineThesholdBO =
            IoTDeviceOfflineThesholdBO.builder()
                .productKey(s.getProductKey())
                .platform(s.getThirdPlatform())
                .build();
        // 如果没有配置使用默认的
        if (StrUtil.isBlank(s.getConfiguration())) {
          ioTDeviceOfflineThesholdBO.setOfflineThreshold(offlineThreshold);
        } else {
          try {
            JSONObject config = JSONUtil.parseObj(s.getConfiguration());
            offlineThreshold =
                config.getInt(
                    IoTConstant.DEFAULT_OFFLINE_THRESHOLD,
                    IoTConstant.DEFAULT_OFFLINE_THRESHOLD_VALUE);
            ioTDeviceOfflineThesholdBO.setOfflineThreshold(offlineThreshold);
          } catch (Exception e) {
            log.error("执行任务报错,productKey={} msg={}", s.getProductKey(), e.getMessage());
            e.printStackTrace();
          }
        }
        productsMap.add(ioTDeviceOfflineThesholdBO);
      }
    }
    return productsMap;
  }

  /** 查询网络类型 */
  @Cacheable(cacheNames = "selectNetworkUnionId", key = "#productKey", unless = "#result == null")
  public String selectNetworkUnionId(String productKey) {
    if (StrUtil.isBlank(productKey)) {
      return null;
    }
    return ioTProductMapper.selectNetworkUnionId(productKey);
  }

  /** 查询通信传输协议 */
  @Cacheable(
      cacheNames = "selectTransportProtocol",
      key = "#productKey",
      unless = "#result == null")
  public String selectTransportProtocol(String productKey) {
    if (StrUtil.isBlank(productKey)) {
      return null;
    }
    IoTProductBO ioTProductBO = ioTProductMapper.selectTransportProtocol(productKey);
    return ioTProductBO == null ? null : ioTProductBO.getTransportProtocol();
  }

  /** 查询产品配置信息 */
  @Cacheable(
      cacheNames = "getProductConfiguration",
      key = "#productKey",
      unless = "#result == null")
  public JSONObject getProductConfiguration(String productKey) {
    // status-0 正常
    IoTProduct product =
        IoTProduct.builder().productKey(productKey).state(IoTConstant.NORMAL.byteValue()).build();
    product = ioTProductMapper.selectOne(product);
    if (product == null || StrUtil.isBlank(product.getConfiguration())) {
      return null;
    }
    return JSONUtil.parseObj(product.getConfiguration());
  }

  /**
   * 获取产品的解码类型（从产品配置的 configuration 字段）
   *
   * @param productKey 产品Key
   * @return 解码类型（HEX/STRING），默认 STRING
   */
  @Cacheable(cacheNames = "getProductDecoderType", key = "#productKey", unless = "#result == null")
  public String getProductDecoderType(String productKey) {
    if (StrUtil.isBlank(productKey)) {
      return "STRING";
    }
    try {
      JSONObject config = getProductConfiguration(productKey);
      if (config != null) {
        return config.getStr("decoderType", "STRING");
      }
    } catch (Exception e) {
      log.warn("获取产品decoderType失败: productKey={}", productKey, e);
    }
    return "STRING";
  }

  /**
   * 获取产品的编码类型（从产品配置的 configuration 字段）
   *
   * @param productKey 产品Key
   * @return 编码类型（HEX/STRING），默认 STRING
   */
  @Cacheable(cacheNames = "getProductEncoderType", key = "#productKey", unless = "#result == null")
  public String getProductEncoderType(String productKey) {
    if (StrUtil.isBlank(productKey)) {
      return "STRING";
    }
    try {
      JSONObject config = getProductConfiguration(productKey);
      if (config != null) {
        return config.getStr("encoderType", "STRING");
      }
    } catch (Exception e) {
      log.warn("获取产品encoderType失败: productKey={}", productKey, e);
    }
    return "STRING";
  }

  /** 日志写入指定数据库时，是否默认双写一份到MySQL；布尔类型，默认值false（关闭） */
  @Cacheable(
      cacheNames = IoTConstant.ENABLE_MYSQL_WRITE,
      key = "#productKey",
      unless = "#result == null")
  public Boolean enableMysqlWrite(String productKey) {
    if (StrUtil.isBlank(productKey)) {
      return Boolean.FALSE;
    }
    try {
      JSONObject config = getProductConfiguration(productKey);
      if (config != null) {
        return config.getBool(IoTConstant.ENABLE_MYSQL_WRITE, Boolean.FALSE);
      }
    } catch (Exception e) {
      log.warn("获取产品encoderType失败: productKey={}", productKey, e);
    }
    return Boolean.FALSE;
  }
}
