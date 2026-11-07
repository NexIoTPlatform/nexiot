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

package cn.universal.admin.platform.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.common.service.BaseServiceImpl;
import cn.universal.admin.network.service.INetworkService;
import cn.universal.admin.platform.service.IIoTProductService;
import cn.universal.admin.platform.support.ProductMetadataNormalizer;
import cn.universal.admin.system.service.ISysDeptService;
import cn.universal.admin.system.service.ISysDictTypeService;
import cn.universal.admin.system.service.IoTDeviceProtocolService;
import cn.universal.cache.annotation.MultiLevelCacheable;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.constant.IoTConstant.DeviceNode;
import cn.universal.common.constant.IoTConstant.ProductFlushType;
import cn.universal.common.constant.IoTConstant.ProtocolModule;
import cn.universal.common.constant.IoTConstant.TcpFlushType;
import cn.universal.common.event.EventTopics;
import cn.universal.common.event.processer.EventPublisher;
import cn.universal.common.exception.IoTException;
import cn.universal.common.utils.JsonDesensitizationUtil;
import cn.universal.core.product.metadata.ProductMetadataProvider;
import cn.universal.dm.cache.IoTCacheInvalidationPublisher;
import cn.universal.dm.device.service.impl.IoTCacheRemoveService;
import cn.universal.dm.device.service.log.IIoTDeviceDataService;
import cn.universal.dm.device.service.protocol.ProtocolClusterService;
import cn.universal.persistence.base.IoTProductAction;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.IoTDeviceFunction;
import cn.universal.persistence.entity.IoTDeviceProperties;
import cn.universal.persistence.entity.IoTDeviceProtocol;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTProductCategory;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.Network;
import cn.universal.persistence.entity.SceneLinkage;
import cn.universal.persistence.entity.admin.SysDictData;
import cn.universal.persistence.entity.bo.IoTProductBO;
import cn.universal.persistence.entity.bo.IoTProductKeyUpdateBO;
import cn.universal.persistence.entity.dto.ProductExportPackageDTO;
import cn.universal.persistence.entity.dto.ProductExportPackageDTO.DeviceProtocolDTO;
import cn.universal.persistence.entity.dto.ProductExportPackageDTO.NetworkConfigDTO;
import cn.universal.persistence.entity.dto.ProductExportPackageDTO.ProductInfoDTO;
import cn.universal.persistence.entity.vo.IoTDeviceModelVO;
import cn.universal.persistence.entity.vo.IoTProductExportVO;
import cn.universal.persistence.entity.vo.IoTProductOptionVO;
import cn.universal.persistence.entity.vo.IoTProductVO;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTDeviceProtocolMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.mapper.IoTProductSortMapper;
import cn.universal.persistence.mapper.NetworkMapper;
import cn.universal.persistence.mapper.SceneLinkageMapper;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.persistence.query.IoTProductQuery;
import cn.universal.security.utils.SecurityUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tk.mybatis.mapper.entity.Example;

/**
 * 设备产品Service业务层处理
 *
 * @since 2025-12-24
 */
@Slf4j
@Service
public class IoTProductServiceImpl extends BaseServiceImpl implements IIoTProductService {

  @Resource private IoTProductMapper ioTProductMapper;

  @Resource private IoTDeviceMapper ioTDeviceMapper;

  @Resource private NetworkMapper networkMapper;
  @Resource private IoTDeviceProtocolMapper ioTDeviceProtocolMapper;
  @Resource private cn.universal.persistence.mapper.IoTProductMetadataVersionMapper metadataVersionMapper;
  @Resource private IoTDeviceProtocolService devProtocolService;
  @Resource private SceneLinkageMapper sceneLinkageMapper;
  @Resource private cn.universal.rule.scene.service.SceneRuleCache sceneRuleCache;

  @Resource private IoTProductSortMapper ioTProductSortMapper;
  @Resource private cn.universal.admin.platform.service.IIoTProductCategoryService productCategoryService;

  @Resource private ISysDictTypeService dictTypeService;
  @Resource private ISysDeptService sysDeptService;

  @Resource(name = "ioTProductActionService")
  private IoTProductAction ioTProductAction;

  @Resource private IoTCacheInvalidationPublisher cacheInvalidationPublisher;
  @Resource private IoTCacheRemoveService iotCacheRemoveService;

  @Resource private EventPublisher eventPublisher;
  @Resource private JdbcTemplate jdbcTemplate;
  @Resource private INetworkService networkService;
  @Resource private IIoTDeviceDataService iIoTDeviceDataService;

  @Autowired(required = false)
  private List<ProductMetadataProvider> productMetadataProviders = Collections.emptyList();

  @Resource private cn.universal.admin.platform.service.customfield.SystemFieldInjectorManager systemFieldInjectorManager;

  private String defaultMetadata = IoTConstant.PRODUCT_METADATA;
  private String thirdConfig = "{\"customField\":[]}";

  private static final String STORE_POLICY_NONE = "none";
  private static final String PLATFORM_INDUSTRIAL = "industrial";
  private static final String PROTOCOL_MODBUS = "modbus";

  private static final Set<String> PRODUCT_KEY_JSON_FIELDS_WITH_ID =
      Collections.unmodifiableSet(
          new LinkedHashSet<>(
              Arrays.asList(
                  "productkey",
                  "productkeys",
                  "provider",
                  "providers",
                  "id",
                  "unionid",
                  "networkunionid",
                  "gwproductkey",
                  "gwproductkeys",
                  "gatewayproductkey",
                  "gatewayproductkeys")));

  private static final Set<String> PRODUCT_KEY_JSON_FIELDS =
      Collections.unmodifiableSet(
          new LinkedHashSet<>(
              Arrays.asList(
                  "productkey",
                  "productkeys",
                  "provider",
                  "providers",
                  "unionid",
                  "networkunionid",
                  "gwproductkey",
                  "gwproductkeys",
                  "gatewayproductkey",
                  "gatewayproductkeys")));

  @Override
  public Set<String> getEnabledStorePolicies() {
    return iIoTDeviceDataService.getEnabledStorePolicies();
  }

  private void validateStorePolicyEnabled(String storePolicy) {
    if (StrUtil.isBlank(storePolicy)) {
      return;
    }
    String normalizedStorePolicy = normalizeStorePolicy(storePolicy);
    if (!iIoTDeviceDataService.isStorePolicyEnabled(normalizedStorePolicy)) {
      Set<String> enabledPolicies = iIoTDeviceDataService.getEnabledStorePolicies();
      throw new IoTException(
          String.format(
              "日志存储策略[%s]未启用，当前可用策略：%s",
              normalizedStorePolicy, CollectionUtil.isEmpty(enabledPolicies) ? "无" : enabledPolicies));
    }
  }

  private String normalizeStorePolicy(String storePolicy) {
    return StrUtil.blankToDefault(storePolicy, STORE_POLICY_NONE).trim().toLowerCase(Locale.ROOT);
  }

  private static final List<TableColumnRef> PRODUCT_KEY_REFERENCE_COLUMNS =
      Collections.unmodifiableList(
          Arrays.asList(
              new TableColumnRef("iot_product", "gw_product_key"),
              new TableColumnRef("iot_product", "network_union_id"),
              new TableColumnRef("iot_device", "product_key"),
              new TableColumnRef("iot_device", "gw_product_key"),
              new TableColumnRef("iot_ota_task", "product_key"),
              new TableColumnRef("iot_ota_task_device", "product_key"),
              new TableColumnRef("iot_ota_package", "product_key"),
              new TableColumnRef("iot_ota_device_version", "product_key"),
              new TableColumnRef("iot_product_metadata_version", "product_key"),
              new TableColumnRef("iot_device_subscribe", "product_key"),
              new TableColumnRef("rule_model", "product_key"),
              new TableColumnRef("iot_dashboard_statistics", "product_key"),
              new TableColumnRef("iot_push_transform_rule", "product_key"),
              new TableColumnRef("iot_device_tags", "product_key"),
              new TableColumnRef("iot_device_shadow", "product_key"),
              new TableColumnRef("iot_device_log", "product_key"),
              new TableColumnRef("iot_device_log_metadata", "product_key"),
              new TableColumnRef("iot_algorithm_config", "product_key"),
              new TableColumnRef("iot_algorithm_result", "product_key"),
              new TableColumnRef("iot_task_center", "product_key"),
              new TableColumnRef("iot_task_definition", "product_key"),
              new TableColumnRef("iot_gateway_polling_config", "product_key"),
              new TableColumnRef("iot_gateway_polling_command", "gateway_product_key"),
              new TableColumnRef("mp_detail_layout", "product_key")));

  @Override
  public int updateNetworkUnionId(Long id, String networkUnionId) {
    IoTProduct product = ioTProductMapper.selectByPrimaryKey(id);
    if (product == null) {
      throw new IoTException("产品不存在");
    }
    assertProductWritable(product);
    if (StrUtil.isBlank(product.getTenantId())) {
      throw new IoTException("产品租户ID缺失，请先执行产品租户数据补偿");
    }
    validateExclusiveNetworkUnionChange(product, networkUnionId);

    IoTProduct ioTProduct = new IoTProduct();
    ioTProduct.setNetworkUnionId(networkUnionId);
    Example example = new Example(IoTProduct.class);
    example.createCriteria().andEqualTo("id", id);
    int updateCount = ioTProductMapper.updateByExampleSelective(ioTProduct, example);
    if (updateCount > 0) {
      ioTProductAction.update(product.getProductKey());
    }
    return updateCount;
  }

  private void validateExclusiveNetworkUnionChange(IoTProduct product, String networkUnionId) {
    boolean tcpProduct = ProtocolModule.tcp.name().equals(product.getThirdPlatform());
    boolean udpProduct = ProtocolModule.udp.name().equals(product.getThirdPlatform());
    if (!tcpProduct && !udpProduct) {
      return;
    }
    String oldNetworkUnionId = StrUtil.trimToEmpty(product.getNetworkUnionId());
    String newNetworkUnionId = StrUtil.trimToEmpty(networkUnionId);
    if (!StrUtil.equals(oldNetworkUnionId, newNetworkUnionId)
        && ioTDeviceMapper.countByProductKey(product.getProductKey()) > 0) {
      throw new IoTException("产品下已有设备，无法变更TCP/UDP网络组件");
    }
    if (StrUtil.isBlank(newNetworkUnionId)) {
      return;
    }
    if (!StrUtil.equals(product.getProductKey(), newNetworkUnionId)) {
      throw new IoTException("TCP/UDP产品只能绑定本产品对应的网络组件");
    }
    Example example = new Example(Network.class);
    Example.Criteria criteria =
        example.createCriteria().andEqualTo("productKey", product.getProductKey());
    criteria.andEqualTo("type", tcpProduct ? "TCP_SERVER" : "UDP");
    if (networkMapper.selectCountByExample(example) <= 0) {
      throw new IoTException("请先创建本产品对应的TCP/UDP网络组件");
    }
  }

  /**
   * 查询设备产品
   *
   * @param id 设备产品主键
   * @return 设备产品
   */
  @Override
  public IoTProductVO selectDevProductById(String id) {
    IoTProduct product = ioTProductMapper.selectDevProductById(id);
    if (product == null) {
      throw new IoTException("产品不存在");
    }
    assertProductAccess(product);
    IoTProductVO productVO = BeanUtil.toBean(product, IoTProductVO.class);
    int deviceCount =
        StrUtil.isBlank(product.getProductKey())
            ? 0
            : ioTDeviceMapper.countByProductKey(product.getProductKey());
    productVO.setDevNum(deviceCount);
    if (ProtocolModule.tcp.name().equals(product.getThirdPlatform())) {
      Network network =
          networkMapper.selectOne(Network.builder().productKey(product.getProductKey()).build());
      if (Objects.nonNull(network)) {
        JSONObject obj = JSONUtil.parseObj(network.getConfiguration());
        JSONObject parserConfiguration = obj.getJSONObject("parserConfiguration");
        if (MapUtil.isNotEmpty(parserConfiguration)) {
          for (Map.Entry<String, Object> q : parserConfiguration.entrySet()) {
            obj.set(q.getKey(), q.getValue());
          }
        }
        JSONObject configuration = JSONUtil.parseObj(product.getConfiguration());
        for (Map.Entry<String, Object> q : obj.entrySet()) {
          configuration.set(q.getKey(), q.getValue());
        }
        configuration.set("enabled", network.isStateEnabled());
        productVO.setConfiguration(configuration.toString());
      }
    }
    // 网关子产品处理。gwProductKey 现在是可选的推荐/兼容网关产品。
    if (DeviceNode.GATEWAY_SUB_DEVICE.name().equals(product.getDeviceNode())) {
      productVO.setGwProductKey(product.getGwProductKey());
      IoTProduct gwProduct =
          StrUtil.isBlank(product.getGwProductKey())
              ? null
              : ioTProductMapper.getProductByProductKey(product.getGwProductKey());
      if (Objects.nonNull(gwProduct)) {
        productVO.setGwName(gwProduct.getName());
        productVO.setGwPhotoUrl(gwProduct.getPhotoUrl());
      }
    }
    return productVO;
  }

  /**
   * 查询设备产品列表
   *
   * @param ioTProduct 设备产品
   * @return 设备产品
   */
  @Override
  //  @Cacheable(cacheNames = "iot_dev_product_list", unless = "#result == null", keyGenerator =
  // "redisKeyGenerate")
  public List<IoTProduct> selectDevProductList(IoTProduct ioTProduct) {
    applyProductTenantScope(ioTProduct);
    return ioTProductMapper.selectDevProductList(ioTProduct);
  }

  @Override
  //  @Cacheable(cacheNames = "iot_dev_product_list", unless = "#result == null", keyGenerator =
  // "redisKeyGenerate")
  public List<IoTProduct> selectDevProductV2List(IoTProductQuery ioTProductQuery) {
    applyProductDataScope(ioTProductQuery);
    return ioTProductMapper.selectDevProductV2List(ioTProductQuery);
  }

  @Override
  //  @Cacheable(cacheNames = "iot_dev_product_list", unless = "#result == null", keyGenerator =
  // "redisKeyGenerate")
  public List<IoTProductVO> selectDevProductV3List(IoTProductQuery ioTProductQuery) {
    applyProductDataScope(ioTProductQuery);
    Page<IoTProductVO> page =
        PageHelper.startPage(ioTProductQuery.getPageNum(), ioTProductQuery.getPageSize());
    ioTProductMapper.selectDevProductV3List(ioTProductQuery);
    return page;
  }

  @Override
  public List<IoTProductOptionVO> selectProductOptions(IoTProductQuery query) {
    query.setName(StrUtil.trimToNull(query.getName()));
    query.setProductKey(StrUtil.trimToNull(query.getProductKey()));
    query.setPageNum(query.getPageNum() == null ? 1 : Math.max(1, query.getPageNum()));
    query.setPageSize(
        query.getPageSize() == null || query.getPageSize() < 1
            ? 20
            : Math.min(50, query.getPageSize()));
    applyProductDataScope(query);
    PageHelper.startPage(query.getPageNum(), query.getPageSize());
    return ioTProductMapper.selectProductOptions(query);
  }

  @Override
  // 分页查询不使用缓存，因为分页参数会导致缓存键不同，效率低下
  public List<IoTProductVO> selectDevProductV4List(IoTProductQuery ioTProductQuery) {
    applyProductDataScope(ioTProductQuery);
    // 设置分页参数
    PageHelper.startPage(ioTProductQuery.getPageNum(), ioTProductQuery.getPageSize());

    // 优化：一次SQL查询获取产品列表和设备数量，避免N+1查询
    List<IoTProductVO> devProductVOS = ioTProductMapper.selectDevProductV3List(ioTProductQuery);

    // 处理图片URL
    if (CollUtil.isNotEmpty(devProductVOS)) {
      for (IoTProductVO devProduct : devProductVOS) {
        if (JSONUtil.isTypeJSON(devProduct.getPhotoUrl())) {
          JSONObject photo = JSONUtil.parseObj(devProduct.getPhotoUrl());
          devProduct.setImage(photo.getStr("img", ""));
        }
      }
    }

    return devProductVOS;
  }

  /** 获取不分页的产品列表（用于缓存） 缓存基础产品数据，不包含分页信息 */
  @MultiLevelCacheable(
      cacheNames = "selectDevProductV4ListNoPage",
      keyGenerator = "redisKeyGenerate",
      unless = "#result == null",
      l1Expire = 30)
  public List<IoTProductVO> selectDevProductV4ListNoPage(IoTProductQuery ioTProductQuery) {
    // 创建不分页的查询条件
    IoTProductQuery noPageQuery = new IoTProductQuery();
    BeanUtil.copyProperties(ioTProductQuery, noPageQuery);
    noPageQuery.setPageNum(1);
    noPageQuery.setPageSize(Integer.MAX_VALUE); // 获取所有数据
    applyProductDataScope(noPageQuery);

    // 优化：一次SQL查询获取产品列表和设备数量，避免N+1查询
    List<IoTProductVO> devProductVOS = ioTProductMapper.selectDevProductV3List(noPageQuery);

    // 处理图片URL
    if (CollUtil.isNotEmpty(devProductVOS)) {
      for (IoTProductVO devProduct : devProductVOS) {
        if (JSONUtil.isTypeJSON(devProduct.getPhotoUrl())) {
          JSONObject photo = JSONUtil.parseObj(devProduct.getPhotoUrl());
          devProduct.setImage(photo.getStr("img", ""));
        }
      }
    }

    return devProductVOS;
  }

  @Override
  //  @Cacheable(cacheNames = "iot_all_dev_product_list", unless = "#result == null", keyGenerator =
  // "redisKeyGenerate")
  public List<IoTProductExportVO> selectAllDevProductV2List(IoTProductQuery ioTProductQuery) {
    applyProductDataScope(ioTProductQuery);
    return ioTProductMapper.selectAllDevProductV2List(ioTProductQuery);
  }

  /**
   * 新增设备产品
   *
   * @param ioTProduct 设备产品
   * @return 结果
   */
  @Override
  public int insertDevProduct(IoTProduct ioTProduct) {
    Date date = new Date();
    Long time = date.getTime();
    ioTProduct.setCreateTime(new Date());
    // productKey
    String productKey = RandomUtil.randomString(12);
    if (Objects.nonNull(ioTProduct.getClassifiedId())) {
      IoTProductCategory ioTProductCategory =
          ioTProductSortMapper.selectDevProductSortById(ioTProduct.getClassifiedId());
      ioTProduct.setClassifiedName(ioTProductCategory.getClassifiedName());
    }
    if (StrUtil.isBlank(ioTProduct.getProductId())) {
      ioTProduct.setProductId(productKey);
    }
    if (StrUtil.isBlank(ioTProduct.getProductSecret())) {
      ioTProduct.setProductSecret(IdUtil.fastSimpleUUID());
    }
    if (StrUtil.isBlank(ioTProduct.getConfiguration())) {
      ioTProduct.setConfiguration(buildProductCfg(ioTProduct.getThirdPlatform()));
    }
    if (StrUtil.isBlank(ioTProduct.getTransportProtocol())) {
      ioTProduct.setTransportProtocol(ioTProduct.getThirdPlatform());
    }
    prepareIndustrialProductConfig(ioTProduct, null);
    validateGatewayTransportProtocol(ioTProduct, null);
    if ("dahua_cloud".equals(ioTProduct.getThirdPlatform())) ioTProduct.setDeviceNode("VIDEO_DEVICE");
    ioTProduct.setProductKey(productKey);
    ioTProduct.setMetadata(normalizeMetadataJson(metadataFor(ioTProduct.getThirdPlatform())));
    ioTProduct.setThirdConfiguration(thirdConfig);
    ioTProduct.setStorePolicy(IoTConstant.MYSQL);
    validateStorePolicyEnabled(ioTProduct.getStorePolicy());
    fillProductTenant(ioTProduct);
    // 执行系统字段注入（根据产品类型自动添加扩展字段，如Modbus的slaveAddress）
    prepareProductSystemFieldsForWrite(ioTProduct);
    int i = ioTProductMapper.insertDevProduct(ioTProduct);
    if (i > 0) {
      ioTProductAction.create(productKey, ioTProduct.getCreatorId());
    }
    return i;
  }

  /** 默认创建支持自动注册 */
  private String buildProductCfg(String thirdPlatform) {
    JSONObject cfg = new JSONObject();
    if (thirdPlatform == null) {
      return JSONUtil.toJsonStr(cfg);
    }
    if (ProtocolModule.tcp.name().equalsIgnoreCase(thirdPlatform)
        || ProtocolModule.mqtt.name().equalsIgnoreCase(thirdPlatform)
        || ProtocolModule.udp.name().equalsIgnoreCase(thirdPlatform)
        || ProtocolModule.modbus.name().equalsIgnoreCase(thirdPlatform)
        || "industrial".equalsIgnoreCase(thirdPlatform)) {
      cfg.set(IoTConstant.ALLOW_INSERT, true);
    }
    return JSONUtil.toJsonStr(cfg);
  }

  private String metadataFor(String thirdPlatform) {
    if (StrUtil.isNotBlank(thirdPlatform) && productMetadataProviders != null) {
      for (ProductMetadataProvider provider : productMetadataProviders) {
        if (provider != null
            && thirdPlatform.equalsIgnoreCase(provider.getProtocolCode())
            && StrUtil.isNotBlank(provider.getMetadata())) {
          return provider.getMetadata();
        }
      }
    }
    return defaultMetadata;
  }

  private void prepareIndustrialProductConfig(IoTProduct product, IoTProduct currentProduct) {
    if (product == null) {
      return;
    }
    String effectiveThirdPlatform = firstText(product.getThirdPlatform(), currentProduct == null ? null : currentProduct.getThirdPlatform());
    if (!PLATFORM_INDUSTRIAL.equalsIgnoreCase(effectiveThirdPlatform)) {
      return;
    }
    if (StrUtil.isBlank(product.getThirdPlatform())) {
      product.setThirdPlatform(effectiveThirdPlatform);
    }
    if (StrUtil.isBlank(product.getThirdConfiguration()) && currentProduct != null) {
      product.setThirdConfiguration(currentProduct.getThirdConfiguration());
    }

    JSONObject config = parseJsonObject(firstText(product.getConfiguration(), currentProduct == null ? null : currentProduct.getConfiguration()));
    JSONObject industrial = parseNestedJsonObject(config.get("industrial"));
    String protocol =
        normalizeIndustrialProtocol(
            firstText(
                product.getTransportProtocol(),
                config.getStr("industrialProtocol"),
                config.getStr("protocol"),
                industrial.getStr("protocol")));
    protocol = StrUtil.blankToDefault(protocol, "MODBUS");

    industrial.set("protocol", protocol);
    boolean requestResponseProtocol =
        "GENERIC_TCP".equals(protocol) || "GENERIC_UDP".equals(protocol);
    if (requestResponseProtocol || StrUtil.isBlank(industrial.getStr("driverType"))) {
      industrial.set("driverType", defaultIndustrialDriver(protocol));
    }
    if (requestResponseProtocol || StrUtil.isBlank(industrial.getStr("connectionMode"))) {
      industrial.set("connectionMode", defaultIndustrialConnectionMode(protocol));
    }
    config.set(IoTConstant.ALLOW_INSERT, true);
    config.set("industrialProtocol", protocol);
    config.set("industrial", industrial);
    product.setTransportProtocol(protocol);
    product.setConfiguration(config.toString());
  }

  private JSONObject parseJsonObject(String value) {
    if (StrUtil.isBlank(value) || !JSONUtil.isTypeJSON(value)) {
      return new JSONObject();
    }
    try {
      return JSONUtil.parseObj(value);
    } catch (Exception e) {
      return new JSONObject();
    }
  }

  private JSONObject parseNestedJsonObject(Object value) {
    if (value instanceof JSONObject object) {
      return object;
    }
    if (value instanceof Map<?, ?> map) {
      JSONObject object = new JSONObject();
      map.forEach((key, item) -> {
        if (key != null) {
          object.set(String.valueOf(key), item);
        }
      });
      return object;
    }
    if (value instanceof String text && JSONUtil.isTypeJSON(text)) {
      return JSONUtil.parseObj(text);
    }
    return new JSONObject();
  }

  private String normalizeIndustrialProtocol(String value) {
    if (StrUtil.isBlank(value) || PLATFORM_INDUSTRIAL.equalsIgnoreCase(value)) {
      return null;
    }
    String normalized =
        value.trim().toLowerCase(Locale.ROOT).replace("-", "").replace("_", "").replace("/", "").replace(" ", "");
    return switch (normalized) {
      case "s7", "siemenss7" -> "S7";
      case "opcua", "opc" -> "OPCUA";
      case "modbus", "modbustcp", "modbusrtu", "rtu", "modbusserial", "modbusrtuserial",
              "modbusrtuoverserial", "rtuserial", "rtuoverserial", "serialrtu",
              "modbusascii", "ascii", "modbusasciiserial", "modbusasciioverserial",
              "asciiserial", "asciioverserial", "serialascii", "modbusrtutcp",
              "modbusrtuovertcp", "rtutcp", "rtuovertcp", "modbustcpclient" ->
          "MODBUS";
      case "eip", "ethernetip" -> "EIP";
      case "bacnet", "bacnetip" -> "BACNET";
      case "ads", "twincat" -> "ADS";
      case "mc", "mitsubishimc", "mitsubishi" -> "MC";
      case "fins", "omronfins", "omron" -> "FINS";
      case "iec104", "iec60870", "iec608705104", "iec60870104" -> "IEC104";
      case "generictcp", "tcpclient" -> "GENERIC_TCP";
      case "genericudp", "udpclient" -> "GENERIC_UDP";
      default -> null;
    };
  }

  void validateGatewayTransportProtocol(IoTProduct product, IoTProduct currentProduct) {
    if (product == null) {
      return;
    }
    String deviceNode =
        firstText(
            product.getDeviceNode(),
            currentProduct == null ? null : currentProduct.getDeviceNode());
    if (!DeviceNode.GATEWAY.name().equals(deviceNode)
        && !DeviceNode.GATEWAY_SUB_DEVICE.name().equals(deviceNode)) {
      return;
    }

    String thirdPlatform =
        firstText(
            product.getThirdPlatform(),
            currentProduct == null ? null : currentProduct.getThirdPlatform());
    if (isIndustrialCollectionAccess(thirdPlatform)) {
      return;
    }

    String transportProtocol =
        firstText(
            product.getTransportProtocol(),
            currentProduct == null ? null : currentProduct.getTransportProtocol());
    if (PROTOCOL_MODBUS.equalsIgnoreCase(transportProtocol)) {
      return;
    }
    if (currentProduct != null
        && sameGatewayProtocolState(deviceNode, thirdPlatform, transportProtocol, currentProduct)) {
      return;
    }
    throw new IoTException(
        "非工业接入的网关和网关子设备仅支持Modbus数据协议（当前接入方式："
            + thirdPlatform
            + "，数据协议："
            + transportProtocol
            + "），请使用Modbus或工业采集接入");
  }

  private boolean isIndustrialCollectionAccess(String thirdPlatform) {
    if (PLATFORM_INDUSTRIAL.equalsIgnoreCase(thirdPlatform)
        || PROTOCOL_MODBUS.equalsIgnoreCase(thirdPlatform)) {
      return true;
    }
    String protocol = normalizeIndustrialProtocol(thirdPlatform);
    return protocol != null && !"MODBUS".equals(protocol);
  }

  private boolean sameGatewayProtocolState(
      String deviceNode,
      String thirdPlatform,
      String transportProtocol,
      IoTProduct currentProduct) {
    return StrUtil.equalsIgnoreCase(deviceNode, currentProduct.getDeviceNode())
        && StrUtil.equalsIgnoreCase(thirdPlatform, currentProduct.getThirdPlatform())
        && StrUtil.equalsIgnoreCase(transportProtocol, currentProduct.getTransportProtocol());
  }

  private String defaultIndustrialDriver(String protocol) {
    return switch (protocol) {
      case "BACNET", "MC", "FINS", "IEC104", "GENERIC_TCP", "GENERIC_UDP" -> "NATIVE";
      default -> "PLC4X";
    };
  }

  private String defaultIndustrialConnectionMode(String protocol) {
    return "BACNET".equals(protocol) || "GENERIC_UDP".equals(protocol)
        ? "UDP"
        : "TCP_CLIENT";
  }

  private String firstText(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (StrUtil.isNotBlank(value)) {
        return value.trim();
      }
    }
    return null;
  }

  @Override
  public int insertList(List<IoTProduct> ioTProductList) {
    if (CollectionUtil.isNotEmpty(ioTProductList)) {
      ioTProductList.forEach(
          product -> {
            prepareProductMetadata(product);
            fillProductTenant(product);
            validateGatewayTransportProtocol(product, null);
            prepareProductSystemFieldsForWrite(product);
          });
    }
    int i = ioTProductMapper.insertList(ioTProductList);
    return i;
  }

  private void fillProductTenant(IoTProduct ioTProduct) {
    if (ioTProduct == null
        || StrUtil.isNotBlank(ioTProduct.getTenantId())
        || StrUtil.isBlank(ioTProduct.getCreatorId())) {
      return;
    }
    IoTUser creator = queryIotUser(ioTProduct.getCreatorId());
    if (creator != null) {
      ioTProduct.setTenantId(StrUtil.blankToDefault(creator.getTenantId(), creator.getUnionId()));
    }
  }

  /**
   * 修改设备产品
   *
   * @param ioTProduct 设备产品
   * @return 结果
  */
  @Override
  public int updateDevProduct(IoTProduct ioTProduct) {
    IoTUser iotUser = queryIotUser(SecurityUtils.getUnionId());
    if (iotUser == null) {
      throw new IoTException("获取用户信息异常", HttpStatus.HTTP_UNAUTHORIZED);
    }
    String appUnionId = iotUser.getUnionId();
    IoTProduct pro = ioTProductMapper.selectByPrimaryKey(ioTProduct.getId());
    assertProductWritable(pro);
    if (StrUtil.isNotBlank(ioTProduct.getProductKey())
        && !StrUtil.equals(ioTProduct.getProductKey(), pro.getProductKey())) {
      throw new IoTException("productKey修改请使用专用接口");
    }
    if (Objects.nonNull(ioTProduct.getClassifiedId())) {
      IoTProductCategory ioTProductCategory =
          ioTProductSortMapper.selectDevProductSortById(ioTProduct.getClassifiedId());
      ioTProduct.setClassifiedName(ioTProductCategory.getClassifiedName());
    }
    ioTProduct.setUpdateTime(new Date());
    if (StrUtil.isNotBlank(ioTProduct.getMetadata())) {
      ioTProduct.setMetadata(normalizeMetadataJson(ioTProduct.getMetadata()));
    }
    if (ioTProduct.getStorePolicy() != null) {
      String normalizedStorePolicy = normalizeStorePolicy(ioTProduct.getStorePolicy());
      validateStorePolicyEnabled(normalizedStorePolicy);
      ioTProduct.setStorePolicy(normalizedStorePolicy);
    }
    syncGatewaySubDeviceThirdPlatform(ioTProduct, pro);
    prepareIndustrialProductConfig(ioTProduct, pro);
    validateGatewayTransportProtocol(ioTProduct, pro);
    prepareProductSystemFieldsForWrite(ioTProduct, pro);

    int count = ioTProductMapper.updateByPrimaryKeySelective(ioTProduct);
    log.info("updateDevProduct,操作成功={},userId={}", count, appUnionId);
    // 使用productKey触发更新事件，由Action层统一查询完整对象
    ioTProductAction.update(pro.getProductKey());
    return count;
  }

  private void prepareProductSystemFieldsForWrite(IoTProduct product) {
    prepareProductSystemFieldsForWrite(product, null);
  }

  private void prepareProductSystemFieldsForWrite(IoTProduct product, IoTProduct currentProduct) {
    if (product == null) {
      return;
    }
    fillProductFieldsForSystemInjection(product, currentProduct);
    cn.universal.admin.platform.support.SipProductConfigSupport.preserveCredential(product, currentProduct);
    systemFieldInjectorManager.injectSystemFields(product);
  }

  private void fillProductFieldsForSystemInjection(IoTProduct product, IoTProduct currentProduct) {
    if (product == null || currentProduct == null) {
      return;
    }
    if (StrUtil.isBlank(product.getThirdPlatform())) {
      product.setThirdPlatform(currentProduct.getThirdPlatform());
    }
    if (StrUtil.isBlank(product.getProductKey())) {
      product.setProductKey(currentProduct.getProductKey());
    }
    if (StrUtil.isBlank(product.getDeviceNode())) {
      product.setDeviceNode(currentProduct.getDeviceNode());
    }
    if (StrUtil.isBlank(product.getTransportProtocol())) {
      product.setTransportProtocol(currentProduct.getTransportProtocol());
    }
    if (StrUtil.isBlank(product.getThirdConfiguration())) {
      product.setThirdConfiguration(currentProduct.getThirdConfiguration());
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public int updateProductKey(IoTProductKeyUpdateBO updateBO) {
    if (updateBO == null || updateBO.getId() == null) {
      throw new IoTException("产品ID不能为空");
    }
    String newProductKey = StrUtil.trim(updateBO.getProductKey());
    if (StrUtil.isBlank(newProductKey)) {
      throw new IoTException("productKey不能为空");
    }
    if (newProductKey.length() > 20) {
      throw new IoTException("productKey长度不能超过20个字符");
    }

    IoTUser iotUser = queryIotUser(SecurityUtils.getUnionId());
    if (iotUser == null) {
      throw new IoTException("获取用户信息异常", HttpStatus.HTTP_UNAUTHORIZED);
    }

    IoTProduct currentProduct = ioTProductMapper.selectByPrimaryKey(updateBO.getId());
    assertProductWritable(currentProduct);

    String oldProductKey = currentProduct.getProductKey();
    if (StrUtil.equals(oldProductKey, newProductKey)) {
      throw new IoTException("productKey未发生变化");
    }

    IoTProduct existsProduct = ioTProductMapper.getProductByProductKey(newProductKey);
    if (existsProduct != null && !Objects.equals(existsProduct.getId(), currentProduct.getId())) {
      throw new IoTException("productKey已存在");
    }

    int deviceCount = ioTDeviceMapper.countByProductKey(oldProductKey);
    if (deviceCount > 0) {
      throw new IoTException("当前产品下已有设备，不能修改ProductKey");
    }

    IoTDeviceProtocol protocol = ioTDeviceProtocolMapper.selectByPrimaryKey(oldProductKey);
    if (protocol != null && ioTDeviceProtocolMapper.selectByPrimaryKey(newProductKey) != null) {
      throw new IoTException("新的productKey已被协议标识占用");
    }

    List<NetworkRuntimeContext> networkRuntimeContexts = captureRenameNetworkContexts(oldProductKey);

    currentProduct.setProductKey(newProductKey);
    currentProduct.setConfiguration(
        rewriteProductKeyJson(
            currentProduct.getConfiguration(),
            oldProductKey,
            newProductKey,
            PRODUCT_KEY_JSON_FIELDS_WITH_ID));
    currentProduct.setThirdConfiguration(
        rewriteProductKeyJson(
            currentProduct.getThirdConfiguration(),
            oldProductKey,
            newProductKey,
            PRODUCT_KEY_JSON_FIELDS_WITH_ID));
    currentProduct.setThirdDownRequest(
        rewriteProductKeyJson(
            currentProduct.getThirdDownRequest(),
            oldProductKey,
            newProductKey,
            PRODUCT_KEY_JSON_FIELDS_WITH_ID));
    currentProduct.setUpdateTime(new Date());
    int updated = ioTProductMapper.updateByPrimaryKeySelective(currentProduct);
    if (updated <= 0) {
      throw new IoTException("修改productKey失败");
    }

    updateProtocolProductKey(protocol, oldProductKey, newProductKey);
    updateSimpleProductKeyReferences(oldProductKey, newProductKey);
    updateNetworkProductKeyReferences(oldProductKey, newProductKey);
    updateSceneLinkageProductKeyReferences(oldProductKey, newProductKey);
    registerProductKeyRenameAfterCommit(
        oldProductKey, newProductKey, protocol, networkRuntimeContexts);
    return updated;
  }

  private void syncGatewaySubDeviceThirdPlatform(IoTProduct targetProduct, IoTProduct currentProduct) {
    if (targetProduct == null || currentProduct == null) {
      return;
    }

    String deviceNode =
        StrUtil.isNotBlank(targetProduct.getDeviceNode())
            ? targetProduct.getDeviceNode()
            : currentProduct.getDeviceNode();
    if (!DeviceNode.GATEWAY_SUB_DEVICE.name().equals(deviceNode)) {
      return;
    }

    String gwProductKey =
        StrUtil.isNotBlank(targetProduct.getGwProductKey())
            ? targetProduct.getGwProductKey()
            : currentProduct.getGwProductKey();
    if (targetProduct.getGwProductKey() != null && StrUtil.isBlank(targetProduct.getGwProductKey())) {
      targetProduct.setGwProductKey("");
      return;
    }
    if (StrUtil.isBlank(gwProductKey)) {
      return;
    }

    IoTProduct gatewayProduct = ioTProductMapper.getProductByProductKey(gwProductKey);
    if (gatewayProduct == null) {
      throw new IoTException("绑定的网关产品不存在");
    }
    if (StrUtil.isBlank(gatewayProduct.getThirdPlatform())) {
      throw new IoTException("绑定的网关产品未配置第三方平台");
    }

    String gatewayProtocol = effectiveGatewayProtocol(gatewayProduct);
    targetProduct.setThirdPlatform(gatewayProduct.getThirdPlatform());
    targetProduct.setTransportProtocol(gatewayProtocol);
    log.info(
        "同步网关子设备产品推荐网关协议, productKey={}, gwProductKey={}, thirdPlatform={}, transportProtocol={}",
        currentProduct.getProductKey(),
        gwProductKey,
        gatewayProduct.getThirdPlatform(),
        gatewayProtocol);
  }

  private void updateProtocolProductKey(
      IoTDeviceProtocol protocol, String oldProductKey, String newProductKey) {
    if (protocol == null) {
      return;
    }
    String newConfiguration =
        rewriteProductKeyJson(
            protocol.getConfiguration(),
            oldProductKey,
            newProductKey,
            PRODUCT_KEY_JSON_FIELDS_WITH_ID);
    int rows =
        jdbcTemplate.update(
            "UPDATE iot_device_protocol SET id = ?, configuration = ? WHERE id = ?",
            newProductKey,
            newConfiguration,
            oldProductKey);
    if (rows <= 0) {
      throw new IoTException("更新协议配置失败");
    }
  }

  private void updateSimpleProductKeyReferences(String oldProductKey, String newProductKey) {
    for (TableColumnRef ref : PRODUCT_KEY_REFERENCE_COLUMNS) {
      if (!tableColumnExists(ref.table, ref.column)) {
        continue;
      }
      jdbcTemplate.update(
          "UPDATE " + ref.table + " SET " + ref.column + " = ? WHERE " + ref.column + " = ?",
          newProductKey,
          oldProductKey);
    }
  }

  private void updateNetworkProductKeyReferences(String oldProductKey, String newProductKey) {
    Example example = new Example(Network.class);
    example.createCriteria().andEqualTo("productKey", oldProductKey);
    example.or().andEqualTo("unionId", oldProductKey);
    example.or().andLike("configuration", "%" + oldProductKey + "%");
    List<Network> networks = networkMapper.selectByExample(example);
    if (CollectionUtil.isEmpty(networks)) {
      return;
    }
    for (Network network : networks) {
      boolean changed = false;
      if (StrUtil.equals(network.getProductKey(), oldProductKey)) {
        network.setProductKey(newProductKey);
        changed = true;
      }
      if (StrUtil.equals(network.getUnionId(), oldProductKey)) {
        network.setUnionId(newProductKey);
        changed = true;
      }
      String newConfiguration =
          rewriteProductKeyJson(
              network.getConfiguration(),
              oldProductKey,
              newProductKey,
              PRODUCT_KEY_JSON_FIELDS_WITH_ID);
      if (!StrUtil.equals(newConfiguration, network.getConfiguration())) {
        network.setConfiguration(newConfiguration);
        changed = true;
      }
      if (changed) {
        networkMapper.updateByPrimaryKeySelective(network);
      }
    }
  }

  private void updateSceneLinkageProductKeyReferences(String oldProductKey, String newProductKey) {
    Example example = new Example(SceneLinkage.class);
    example.selectProperties("id", "triggerCondition", "execAction");
    example.createCriteria().andLike("triggerCondition", "%" + oldProductKey + "%");
    example.or().andLike("execAction", "%" + oldProductKey + "%");
    List<SceneLinkage> sceneLinkages = sceneLinkageMapper.selectByExample(example);
    if (CollectionUtil.isEmpty(sceneLinkages)) {
      return;
    }
    for (SceneLinkage sceneLinkage : sceneLinkages) {
      boolean changed = false;
      String newTriggerCondition =
          rewriteProductKeyJson(
              sceneLinkage.getTriggerCondition(),
              oldProductKey,
              newProductKey,
              PRODUCT_KEY_JSON_FIELDS);
      if (!StrUtil.equals(newTriggerCondition, sceneLinkage.getTriggerCondition())) {
        sceneLinkage.setTriggerCondition(newTriggerCondition);
        changed = true;
      }
      String newExecAction =
          rewriteProductKeyJson(
              sceneLinkage.getExecAction(),
              oldProductKey,
              newProductKey,
              PRODUCT_KEY_JSON_FIELDS);
      if (!StrUtil.equals(newExecAction, sceneLinkage.getExecAction())) {
        sceneLinkage.setExecAction(newExecAction);
        changed = true;
      }
      if (changed) {
        sceneLinkageMapper.updateByPrimaryKeySelective(sceneLinkage);
        sceneRuleCache.invalidateAfterCommit();
      }
    }
  }

  private List<NetworkRuntimeContext> captureRenameNetworkContexts(String oldProductKey) {
    Example example = new Example(Network.class);
    example.createCriteria().andEqualTo("productKey", oldProductKey);
    example.or().andEqualTo("unionId", oldProductKey);
    List<Network> networks = networkMapper.selectByExample(example);
    if (CollectionUtil.isEmpty(networks)) {
      return new ArrayList<>();
    }
    return networks.stream()
        .filter(network -> network != null)
        .map(
            network ->
                new NetworkRuntimeContext(
                    network.getId(),
                    network.getType(),
                    network.getProductKey(),
                    network.getUnionId(),
                    network.isStateEnabled()))
        .collect(Collectors.toList());
  }

  private void registerProductKeyRenameAfterCommit(
      String oldProductKey,
      String newProductKey,
      IoTDeviceProtocol protocol,
      List<NetworkRuntimeContext> networkRuntimeContexts) {
    Runnable action =
        () -> {
          cacheInvalidationPublisher.publishProductChange(
              oldProductKey, "product-key-rename-old");
          ioTProductAction.update(newProductKey);
          cacheInvalidationPublisher.publishCustomCacheChange(
              "product-key-rename-protocol",
              Collections.emptyList(),
              Map.of(
                  "iot_protocol_def", List.of(oldProductKey, newProductKey),
                  "selectProtocolDefNoScript", List.of(oldProductKey, newProductKey)));
          if (protocol != null && StrUtil.isNotBlank(protocol.getType())) {
            publishProtocolUpdatedEvent(oldProductKey, protocol.getType());
            publishProtocolUpdatedEvent(newProductKey, protocol.getType());
          }
          refreshRenamedNetworks(networkRuntimeContexts, newProductKey);
        };
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              action.run();
            }
          });
      return;
    }
    action.run();
  }

  private void refreshRenamedNetworks(
      List<NetworkRuntimeContext> networkRuntimeContexts, String newProductKey) {
    if (CollectionUtil.isEmpty(networkRuntimeContexts)) {
      return;
    }
    for (NetworkRuntimeContext context : networkRuntimeContexts) {
      if (context == null) {
        continue;
      }
      String type = StrUtil.blankToDefault(context.type, "");
      if (StrUtil.containsIgnoreCase(type, "tcp") || StrUtil.containsIgnoreCase(type, "udp")) {
        if (StrUtil.isNotBlank(context.oldProductKey)) {
          flushNettyServer(null, context.oldProductKey, TcpFlushType.close);
        }
        if (context.enabled) {
          flushNettyServer(null, newProductKey, TcpFlushType.start);
        }
        continue;
      }
      if (context.enabled && context.id != null && StrUtil.containsIgnoreCase(type, "mqtt")) {
        try {
          networkService.restartNetwork(context.id);
        } catch (Exception e) {
          log.warn("ProductKey修改后刷新MQTT网络失败, networkId={}", context.id, e);
        }
      }
    }
  }

  private void publishProtocolUpdatedEvent(String provider, String protocolType) {
    if (StrUtil.isBlank(provider) || StrUtil.isBlank(protocolType)) {
      return;
    }
    JSONObject object = new JSONObject();
    object.set("type", ProductFlushType.script.name());
    object.set("provider", provider);
    object.set("customType", protocolType);
    eventPublisher.publishEvent(EventTopics.PROTOCOL_UPDATED, object);
  }

  private String rewriteProductKeyJson(
      String json, String oldProductKey, String newProductKey, Set<String> supportedKeys) {
    if (StrUtil.isBlank(json)) {
      return json;
    }
    if (StrUtil.equals(json, oldProductKey)) {
      return newProductKey;
    }
    if (!JSONUtil.isTypeJSON(json)) {
      return json;
    }
    try {
      Object parsed = JSONUtil.parse(json);
      if (rewriteProductKeyJsonValue(parsed, null, oldProductKey, newProductKey, supportedKeys)) {
        return JSONUtil.toJsonStr(parsed);
      }
    } catch (Exception e) {
      log.warn("重写ProductKey关联JSON失败，已跳过该字段", e);
    }
    return json;
  }

  private boolean rewriteProductKeyJsonValue(
      Object current,
      String currentKey,
      String oldProductKey,
      String newProductKey,
      Set<String> supportedKeys) {
    if (current instanceof JSONObject) {
      JSONObject jsonObject = (JSONObject) current;
      boolean changed = false;
      for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
        Object value = entry.getValue();
        String key = entry.getKey();
        if (value instanceof String) {
          String stringValue = (String) value;
          if (shouldRewriteProductKeyJsonField(key, stringValue, oldProductKey, supportedKeys)) {
            jsonObject.set(key, newProductKey);
            changed = true;
          }
        } else if (rewriteProductKeyJsonValue(
            value, key, oldProductKey, newProductKey, supportedKeys)) {
          changed = true;
        }
      }
      return changed;
    }
    if (current instanceof JSONArray) {
      JSONArray jsonArray = (JSONArray) current;
      boolean changed = false;
      for (int i = 0; i < jsonArray.size(); i++) {
        Object item = jsonArray.get(i);
        if (item instanceof String) {
          String stringValue = (String) item;
          if (shouldRewriteProductKeyJsonField(
              currentKey, stringValue, oldProductKey, supportedKeys)) {
            jsonArray.set(i, newProductKey);
            changed = true;
          }
        } else if (rewriteProductKeyJsonValue(
            item, currentKey, oldProductKey, newProductKey, supportedKeys)) {
          changed = true;
        }
      }
      return changed;
    }
    return false;
  }

  private boolean shouldRewriteProductKeyJsonField(
      String fieldName, String fieldValue, String oldProductKey, Set<String> supportedKeys) {
    if (!StrUtil.equals(fieldValue, oldProductKey) || StrUtil.isBlank(fieldName)) {
      return false;
    }
    String normalizedFieldName = fieldName.toLowerCase();
    if (supportedKeys.contains(normalizedFieldName)) {
      return true;
    }
    return StrUtil.contains(normalizedFieldName, "productkey")
        || StrUtil.contains(normalizedFieldName, "provider")
        || StrUtil.contains(normalizedFieldName, "networkunionid")
        || StrUtil.contains(normalizedFieldName, "gatewayproductkey")
        || StrUtil.contains(normalizedFieldName, "gwproductkey");
  }

  private boolean tableColumnExists(String tableName, String columnName) {
    try {
      jdbcTemplate.queryForList("SELECT " + columnName + " FROM " + tableName + " WHERE 1 = 0");
      return true;
    } catch (DataAccessException e) {
      log.debug("表或字段不存在，跳过ProductKey关联更新, table={}, column={}", tableName, columnName);
      return false;
    }
  }

  private static final class TableColumnRef {

    private final String table;
    private final String column;

    private TableColumnRef(String table, String column) {
      this.table = table;
      this.column = column;
    }
  }

  private static final class NetworkRuntimeContext {

    private final Long id;
    private final String type;
    private final String oldProductKey;
    private final String oldUnionId;
    private final boolean enabled;

    private NetworkRuntimeContext(
        Long id, String type, String oldProductKey, String oldUnionId, boolean enabled) {
      this.id = id;
      this.type = type;
      this.oldProductKey = oldProductKey;
      this.oldUnionId = oldUnionId;
      this.enabled = enabled;
    }
  }

  /**
   * 批量删除设备产品
   *
   * @param ids 需要删除的设备产品主键
   * @return 结果
  */
  @Override
  public int deleteDevProductByIds(String[] ids) {
    IoTUser iotUser = queryIotUser(SecurityUtils.getUnionId());
    if (iotUser == null) {
      throw new IoTException("获取用户信息异常", HttpStatus.HTTP_UNAUTHORIZED);
    }
    String appUnionId = iotUser.getUnionId();
    int count = 0;
    for (String id : ids) {
      IoTProduct ioTProduct = ioTProductMapper.selectDevProductById(id);
      assertProductWritable(ioTProduct);
      count = +deleteDevProductById(id);
    }
    return count;
  }

  /**
   * 删除设备产品信息
   *
   * @param id 设备产品主键
   * @return 结果
  */
  @Override
  public int deleteDevProductById(String id) {
    String appUnionId = queryIotUser(SecurityUtils.getUnionId()).getUnionId();
    IoTProduct ioTProduct = selectDevProductById(id);
    assertProductWritable(ioTProduct);
    IoTDevice ioTDevice = IoTDevice.builder().productKey(ioTProduct.getProductKey()).build();
    int count = ioTDeviceMapper.selectCount(ioTDevice);
    if (count > 0) {
      throw new IoTException("" + ioTProduct.getName() + "存在设备，不允许删除");
    }
    int delCount = ioTProductMapper.deleteDevProductById(id);
    if (delCount > 0) {
      ioTProductAction.delete(ioTProduct.getProductKey());
      log.info(
          "删除产品={},删除状态={}，删除人={}",
          ioTProduct.getProductKey() + ioTProduct.getName(),
          delCount,
          appUnionId);
    }
    return delCount;
  }

  /** 修改产品配置信息 */
  @Override
  // @Transactional
  public int updateDevProductConfig(IoTProductVO devProduct) {
    IoTUser iotUser = queryIotUser(SecurityUtils.getUnionId());
    if (iotUser == null || iotUser.getUnionId() == null) {
      throw new IoTException("获取用户信息异常", HttpStatus.HTTP_UNAUTHORIZED);
    }
    // 根据id获取产品信息
    IoTProduct product =
        ioTProductMapper.selectDevProductById(String.valueOf(devProduct.getId()));
    assertProductWritable(product);
    JSONObject mergedConfig = mergeProductConfigByScope(devProduct, product);
    // 判断tcp端口号是否冲突
    if (ProtocolModule.tcp.name().equals(product.getThirdPlatform())
        && "privateConfiguration".equals(devProduct.getType())) {
      Integer port = null;
      try {
        port = mergedConfig.getInt("port");
      } catch (Exception e) {
        throw new IoTException("端口号存在字符");
      }
      // 判断端口号是否冲突
      if (Objects.nonNull(port)) {
        // 判断是否和数据库中其他tcp端口配置冲突
        int count = networkMapper.selectNetWorkByPort(port, product.getProductKey());
        if (count > 0) {
          throw new IoTException(port + "端口" + "已被占用");
        }
      }
    }
    devProduct.setConfiguration(mergedConfig.toString());
    devProduct.setThirdConfiguration(product.getThirdConfiguration());
    prepareIndustrialProductConfig(devProduct, product);
    prepareProductSystemFieldsForWrite(devProduct, product);
    int updateCount = ioTProductMapper.updateDevProduct(devProduct);
    if (updateCount > 0) {
      ioTProductAction.update(product.getProductKey());
      log.info(
          "更新产品={},更新状态={}，更新人={}",
          devProduct.getProductKey() + devProduct.getName(),
          updateCount,
          iotUser.getUnionId());
    }

    return updateCount;
  }

  private JSONObject mergeProductConfigByScope(IoTProductVO devProduct, IoTProduct product) {
    String dictType = resolveConfigDictType(devProduct.getType(), product.getThirdPlatform());
    if (StrUtil.isBlank(dictType)) {
      return mergeProductConfig(devProduct.getConfiguration(), product.getConfiguration());
    }

    List<String> scopedKeys = loadConfigKeysByDictType(dictType);
    String mergedConfigJson =
        JsonDesensitizationUtil.replaceScopedAuthConfig(
            devProduct.getConfiguration(), product.getConfiguration(), scopedKeys);
    return StrUtil.isBlank(mergedConfigJson)
        ? new JSONObject()
        : JSONUtil.parseObj(mergedConfigJson);
  }

  private JSONObject mergeProductConfig(String newConfig, String oldConfig) {
    String mergedConfigJson = JsonDesensitizationUtil.mergeAuthConfig(newConfig, oldConfig);
    return StrUtil.isBlank(mergedConfigJson) ? new JSONObject() : JSONUtil.parseObj(mergedConfigJson);
  }

  private String resolveConfigDictType(String configType, String thirdPlatform) {
    if ("publicConfiguration".equals(configType)) {
      return "product_configuration_public";
    }
    if ("privateConfiguration".equals(configType) && StrUtil.isNotBlank(thirdPlatform)) {
      return "product_configuration_" + thirdPlatform;
    }
    return null;
  }

  private List<String> loadConfigKeysByDictType(String dictType) {
    List<SysDictData> dictDataList = dictTypeService.selectDictDataByType(dictType);
    if (CollUtil.isEmpty(dictDataList)) {
      return new ArrayList<>();
    }
    return dictDataList.stream()
        .map(SysDictData::getDictValue)
        .filter(StrUtil::isNotBlank)
        .distinct()
        .collect(Collectors.toList());
  }

  /**
   * 查询设备产品物模型属性列表
   *
   * @return 设备产品物模型属性集合
   */
  @Override
  public List<IoTDeviceProperties> selectDevProperties(String Id) {
    IoTProduct ioTProduct = ioTProductMapper.selectDevProductById(Id);
    JSONObject metadata = JSONUtil.parseObj(ioTProduct.getMetadata());
    List<IoTDeviceProperties> ioTDevicePropertiesList = new ArrayList<>();
    JSONArray properties = metadata.getJSONArray("properties");
    if (properties != null) {
      for (Object object : properties) {
        JSONObject jsonObject = JSONUtil.parseObj(object);
        JSONObject valueType = getJsonObject(jsonObject, "valueType");
        JSONObject expands = getJsonObject(jsonObject, "expands");
        IoTDeviceProperties ioTDeviceProperties =
            IoTDeviceProperties.builder()
                .id(jsonObject.getStr("id"))
                .name(jsonObject.getStr("name"))
                .type(valueType == null ? null : valueType.getStr("type"))
                .unit(valueType == null ? null : valueType.getStr("unit"))
                .elements(
                    valueType != null && valueType.containsKey("elements")
                        ? JSONUtil.toJsonStr(valueType.get("elements"))
                        : null)
                .valueType(valueType)
                .expands(expands)
                .mode(jsonObject.getStr("mode"))
                .source(jsonObject.getStr("source"))
                .description(jsonObject.getStr("description"))
                .build();
        ioTDevicePropertiesList.add(ioTDeviceProperties);
      }
    }
    return ioTDevicePropertiesList;
  }

  /**
   * 查询设备产品物模型事件列表
   *
   * @return 设备产品物模型事件集合
   */
  @Override
  public List<IoTDeviceEvents> selectDevEvents(String Id) {
    IoTProduct ioTProduct = ioTProductMapper.selectDevProductById(Id);
    JSONObject metadata = JSONUtil.parseObj(ioTProduct.getMetadata());
    List<IoTDeviceEvents> ioTDeviceEventsList = new ArrayList<>();
    JSONArray properties = metadata.getJSONArray("events");
    if (properties != null) {
      for (Object object : properties) {
        JSONObject jsonObject = JSONUtil.parseObj(object);
        JSONObject expands = getJsonObject(jsonObject, "expands");
        JSONObject valueType = getJsonObject(jsonObject, "valueType");
        IoTDeviceEvents ioTDeviceEvents =
            IoTDeviceEvents.builder()
                .id(jsonObject.getStr("id"))
                .name(jsonObject.getStr("name"))
                .description(jsonObject.getStr("description"))
                .level(expands == null ? null : expands.getStr("level"))
                .expands(expands)
                .valueType(valueType)
                .build();
        ioTDeviceEventsList.add(ioTDeviceEvents);
      }
    }
    return ioTDeviceEventsList;
  }

  /**
   * 查询设备产品物模型方法列表
   *
   * @return 设备产品物模型方法集合
   */
  @Override
  public List<IoTDeviceFunction> selectDevFunctions(String Id) {
    IoTProduct ioTProduct = ioTProductMapper.selectDevProductById(Id);
    JSONObject metadata = JSONUtil.parseObj(ioTProduct.getMetadata());
    List<IoTDeviceFunction> ioTDeviceFunctionList = new ArrayList<>();
    JSONArray properties = metadata.getJSONArray("functions");
    if (properties != null) {
      for (Object object : properties) {
        JSONObject jsonObject = JSONUtil.parseObj(object);
        IoTDeviceFunction ioTDeviceFunction =
            IoTDeviceFunction.builder()
                .id(jsonObject.getStr("id"))
                .name(jsonObject.getStr("name"))
                .config(jsonObject.getBool("config") == null ? false : jsonObject.getBool("config"))
                .inputs(
                    jsonObject.containsKey("inputs")
                        ? JSONUtil.toJsonStr(jsonObject.get("inputs"))
                        : null)
                .output(
                    jsonObject.containsKey("output")
                        ? JSONUtil.toJsonStr(jsonObject.get("output"))
                        : null)
                .source(jsonObject.getStr("source"))
                .description(jsonObject.getStr("description"))
                .build();
        ioTDeviceFunctionList.add(ioTDeviceFunction);
      }
    }
    return ioTDeviceFunctionList;
  }

  // @PostConstruct
  // public void s2s(){
  //    JSONObject object=new JSONObject();
  //    object.set("id","testt222");
  //    object.set("name","测试df");
  ////    object.set("expands","ff");
  //  JSONObject k=new JSONObject();
  //    object.set("valueType",k);
  //    k.set("type","string");
  //    k.set("expands",null);
  //   int a= insertMetadata("test","properties", JSONUtil.toJsonStr(object));
  //  System.out.println(a);
  // }
  //  @PostConstruct
  // public void ss(){
  //    JSONObject object=new JSONObject();
  //    object.set("id","testt222");
  //    object.set("name","测试df");
  ////    object.set("expands","ff");
  //  JSONObject k=new JSONObject();
  //    object.set("valueType",k);
  //    k.set("type","string");
  //    k.set("expands",null);
  //   int a= deleteMetadata("test","properties", "testt222");
  //  System.out.println(a);
  // }

  /** 物模型新增字段 注：使用 Java 代码处理，支持覆盖模式，防止 ID 重复错误 */
  @Override
  public int insertMetadata(IoTProductBO ioTProductBO) {
    IoTProduct ioTProduct =
        ioTProductMapper.selectOne(
            IoTProduct.builder().productKey(ioTProductBO.getProductKey()).build());
    if (ioTProduct == null) {
      throw new IoTException("产品不存在");
    }
    assertProductWritable(ioTProduct);

    // 使用 Java 代码处理 JSON，避免 SQL JSON 函数可能导致的重复问题
    ioTProductBO.toJsonObj();
    String type = ioTProductBO.getType();

    // 解析当前物模型
    JSONObject metadata = parseMetadata(ioTProduct.getMetadata());

    // 确定要操作的数组名称
    String arrayName;
    switch (type) {
      case "properties":
        arrayName = "properties";
        break;
      case "events":
        arrayName = "events";
        break;
      case "functions":
        arrayName = "functions";
        break;
      default:
        throw new IoTException("物模型类型不存在!");
    }

    // 获取或初始化对应的数组
    JSONArray array = metadata.getJSONArray(arrayName);
    if (array == null) {
      array = new JSONArray();
    }

    // 解析新的物模型数据
    JSONObject newItem = JSONUtil.parseObj(ioTProductBO.getJsonStr());
    String newId = newItem.getStr("id");

    if (newId == null || newId.trim().isEmpty()) {
      throw new IoTException("物模型 ID 不能为空");
    }

    // 检查是否已存在相同 ID
    for (int i = 0; i < array.size(); i++) {
      JSONObject item = array.getJSONObject(i);
      if (newId.equals(item.getStr("id"))) {
        // ID 已存在，抛出异常
        throw new IoTException("该 ID 已存在：" + newId);
      }
    }

    // ID 不存在，添加到数组
    array.add(newItem);

    // 更新 metadata
    metadata.set(arrayName, array);
    normalizeSystemFunctions(metadata);

    // 保存到数据库
    ioTProduct.setMetadata(metadata.toString());
    int updateCount = ioTProductMapper.updateByPrimaryKeySelective(ioTProduct);

    if (updateCount > 0) {
      ioTProductAction.metadataCreate(ioTProductBO.getProductKey());
      log.info(
          "产品新增物模型={},新增物模型状态={}，修改人={}",
          ioTProduct.getProductKey() + ioTProduct.getName(),
          updateCount,
          ioTProduct.getCreatorId());
    }
    return updateCount;
  }

  @Override
  public int insertMetadataV2(IoTProductBO ioTProductBO) {
    IoTProduct ioTProduct =
        ioTProductMapper.selectOne(
            IoTProduct.builder().productKey(ioTProductBO.getProductKey()).build());
    if (ioTProduct == null) {
      throw new IoTException("产品不存在");
    }
    assertProductWritable(ioTProduct);

    // 使用Java代码处理JSON，避免SQL JSON函数可能导致的重复问题
    ioTProductBO.toJsonObj();
    String type = ioTProductBO.getType();

    // 解析当前物模型
    JSONObject metadata = parseMetadata(ioTProduct.getMetadata());

    // 确定要操作的数组名称
    String arrayName;
    switch (type) {
      case "properties":
        arrayName = "properties";
        break;
      case "events":
        arrayName = "events";
        break;
      case "functions":
        arrayName = "functions";
        break;
      default:
        throw new IoTException("物模型类型不存在!");
    }

    // 获取或初始化对应的数组
    JSONArray array = metadata.getJSONArray(arrayName);
    if (array == null) {
      array = new JSONArray();
    }

    // 解析新的物模型数据
    JSONObject newItem = JSONUtil.parseObj(ioTProductBO.getJsonStr());
    String newId = newItem.getStr("id");

    // 查找是否已存在相同ID，如果存在则替换，否则添加
    boolean found = false;
    for (int i = 0; i < array.size(); i++) {
      JSONObject item = array.getJSONObject(i);
      if (newId != null && newId.equals(item.getStr("id"))) {
        // ID已存在，替换
        array.set(i, newItem);
        found = true;
        break;
      }
    }

    if (!found) {
      // ID不存在，添加到数组
      array.add(newItem);
    }

    // 更新metadata
    metadata.set(arrayName, array);
    normalizeSystemFunctions(metadata);

    // 保存到数据库
    ioTProduct.setMetadata(metadata.toString());
    int updateCount = ioTProductMapper.updateByPrimaryKeySelective(ioTProduct);

    if (updateCount > 0) {
      ioTProductAction.metadataCreate(ioTProductBO.getProductKey());
      log.info(
          "产品新增/更新物模型(V2)={},操作状态={}，修改人={}",
          ioTProduct.getProductKey() + ioTProduct.getName(),
          updateCount,
          ioTProduct.getCreatorId());
    }
    return updateCount;
  }

  @Override
  public int deleteMetadata(IoTProductBO ioTProductBO) {
    String appUnionId = queryIotUser(SecurityUtils.getUnionId()).getUnionId();
    IoTProduct product = ioTProductMapper.getProductByProductKey(ioTProductBO.getProductKey());
    assertProductWritable(product);
    JSONObject metadata = parseMetadata(product.getMetadata());
    String arrayName = getMetadataArrayName(ioTProductBO.getType());
    JSONArray array = getOrCreateMetadataArray(metadata, arrayName);
    int targetIndex = findMetadataIndex(array, ioTProductBO.getOldId());
    if (targetIndex < 0) {
      return 0;
    }
    array.remove(targetIndex);
    metadata.set(arrayName, array);
    normalizeSystemFunctions(metadata);
    product.setMetadata(metadata.toString());
    int delCount = ioTProductMapper.updateByPrimaryKeySelective(product);
    if (delCount > 0) {
      ioTProductAction.metadataDelete(ioTProductBO.getProductKey());
      log.info(
          "删除物模型={},删除物模型状态={}，修改人={}",
          ioTProductBO.getProductKey() + ioTProductBO.getName(),
          delCount,
          appUnionId);
    }
    return delCount;
  }

  @Override
  public IoTProductBO getMetadata(IoTProductBO ioTProductBO) {
    IoTProduct product = ioTProductMapper.getProductByProductKey(ioTProductBO.getProductKey());
    if (product == null) {
      throw new IoTException("产品不存在");
    }
    JSONObject metadata = parseMetadata(product.getMetadata());
    String arrayName = getMetadataArrayName(ioTProductBO.getType());
    JSONArray array = getOrCreateMetadataArray(metadata, arrayName);
    int targetIndex = findMetadataIndex(array, ioTProductBO.getOldId());
    if (targetIndex < 0) {
      return null;
    }
    IoTProductBO result = new IoTProductBO();
    result.setJsonStr(array.getJSONObject(targetIndex).toString());
    return result;
  }

  @Override
  public int updateMetadata(IoTProductBO ioTProductBO) {
    String appUnionId = queryIotUser(SecurityUtils.getUnionId()).getUnionId();
    IoTProduct product = ioTProductMapper.getProductByProductKey(ioTProductBO.getProductKey());
    assertProductWritable(product);
    ioTProductBO.toJsonObj();
    JSONObject metadata = parseMetadata(product.getMetadata());
    String arrayName = getMetadataArrayName(ioTProductBO.getType());
    JSONArray array = getOrCreateMetadataArray(metadata, arrayName);
    JSONObject newItem = JSONUtil.parseObj(ioTProductBO.getJsonStr());
    String lookupId = StrUtil.blankToDefault(ioTProductBO.getOldId(), newItem.getStr("id"));
    int targetIndex = findMetadataIndex(array, lookupId);
    if (targetIndex < 0) {
      throw new IoTException("未找到需要更新的物模型: " + lookupId);
    }
    String newId = newItem.getStr("id");
    int duplicateIndex = findMetadataIndex(array, newId);
    if (StrUtil.isNotBlank(newId) && duplicateIndex >= 0 && duplicateIndex != targetIndex) {
      throw new IoTException("该 ID 已存在：" + newId);
    }
    array.set(targetIndex, newItem);
    metadata.set(arrayName, array);
    normalizeSystemFunctions(metadata);
    product.setMetadata(metadata.toString());
    int updateCount = ioTProductMapper.updateByPrimaryKeySelective(product);
    if (updateCount > 0) {
      ioTProductAction.metadataUpdate(ioTProductBO.getProductKey());
      log.info(
          "产品新增物模型={},新增物模型状态={}，修改人={}",
          ioTProductBO.getProductKey() + ioTProductBO.getName(),
          updateCount,
          appUnionId);
    }
    return updateCount;
  }

  public String getPath(IoTProductBO ioTProductBO) {
    switch (ioTProductBO.getType()) {
      case "properties":
        return "$.properties**.id";
      case "events":
        return "$.events**.id";
      case "functions":
        return "$.functions**.id";
      default:
        throw new IoTException("物模型键不存在!");
    }
  }

  @Override
  public IoTDeviceModelVO getModelByProductKey(String productKey) {
    return ioTProductMapper.getModelByProductKey(productKey);
  }

  @Override
  public String getMetadataProductKey(String devId) {
    return ioTProductMapper.getMetadataProductKey(devId);
  }

  @Override
  @MultiLevelCacheable(
      cacheNames = "countDevNumberByProductKey",
      keyGenerator = "redisKeyGenerate",
      unless = "#result == null",
      l1Expire = 30)
  public Map<String, Integer> countDevNumberByProductKey(String unionId) {
    List<IoTProductVO> results = ioTProductMapper.countDevNumberByProductKey(unionId);
    return results.stream()
        .collect(Collectors.toMap(IoTProductVO::getProductKey, IoTProductVO::getDevNum));
  }

  /** 设备数量变化时清除相关缓存 当设备新增、删除、修改时调用此方法 */
  public void evictDeviceCountCache() {
    cacheInvalidationPublisher.publishDeviceCountChange("device-count-manual-evict");
  }

  @Override
  public AjaxResult<IoTProduct> selectIoTProductByKey(String key) {
    IoTProduct ioTProduct = new IoTProduct();
    ioTProduct.setProductKey(key);
    IoTProduct product = ioTProductMapper.selectOne(ioTProduct);
    if (product != null) {
      assertProductAccess(product);
    }
    return AjaxResult.success(product);
  }

  @Override
  public AjaxResult<List<IoTProduct>> selectGatewaySubProductsByKey(String gwProductKey) {
    IoTProduct gatewayProduct =
        ioTProductMapper.selectOne(IoTProduct.builder().productKey(gwProductKey).build());
    if (gatewayProduct != null) {
      assertProductAccess(gatewayProduct);
    }
    IoTProduct ioTProduct = new IoTProduct();
    ioTProduct.setState((byte) 0);
    ioTProduct.setDeviceNode(DeviceNode.GATEWAY_SUB_DEVICE.name());
    List<IoTProduct> list =
        ioTProductMapper.select(ioTProduct).stream()
            .filter(
                product ->
                    (StrUtil.isBlank(product.getGwProductKey())
                            || StrUtil.equals(product.getGwProductKey(), gwProductKey))
                        && isGatewaySubProductCompatible(gatewayProduct, product))
            .toList();
    return AjaxResult.success(filterAccessibleProducts(list));
  }

  private boolean isGatewaySubProductCompatible(IoTProduct gatewayProduct, IoTProduct subProduct) {
    if (gatewayProduct == null || subProduct == null) {
      return true;
    }
    String gatewayProtocol = effectiveGatewayProtocol(gatewayProduct);
    String subProtocol = effectiveGatewayProtocol(subProduct);
    if (StrUtil.isNotBlank(gatewayProtocol)
        && StrUtil.isNotBlank(subProtocol)
        && !StrUtil.equalsIgnoreCase(gatewayProtocol, subProtocol)) {
      return false;
    }
    if (StrUtil.isBlank(gatewayProtocol) && StrUtil.isBlank(subProtocol)) {
      String gatewayPlatform = normalizeProductPlatform(gatewayProduct);
      String subPlatform = normalizeProductPlatform(subProduct);
      return StrUtil.isBlank(gatewayPlatform)
          || StrUtil.isBlank(subPlatform)
          || StrUtil.equals(gatewayPlatform, subPlatform);
    }
    return true;
  }

  private String effectiveGatewayProtocol(IoTProduct product) {
    if (product == null) {
      return null;
    }
    JSONObject config = parseJsonObject(product.getConfiguration());
    JSONObject industrial = parseNestedJsonObject(config.get("industrial"));
    return normalizeIndustrialProtocol(
        firstText(
            product.getTransportProtocol(),
            config.getStr("industrialProtocol"),
            config.getStr("protocol"),
            industrial.getStr("protocol"),
            product.getThirdPlatform(),
            product.getMessageProtocol()));
  }

  private String normalizeProductPlatform(IoTProduct product) {
    return product == null || StrUtil.isBlank(product.getThirdPlatform())
        ? null
        : product.getThirdPlatform().trim().toLowerCase();
  }

  /** 修改产品其他配置信息 */
  @Override
  public int updateDevProductOtherConfig(String otherConfig) {
    // 解析新的配置
    JSONObject newConfig = JSONUtil.parseObj(otherConfig);
    // 获取产品信息
    IoTProduct product = ioTProductMapper.selectDevProductById(newConfig.getStr("productId"));
    assertProductWritable(product);
    // 获取第三方配置
    String thirdConfiguration = product.getThirdConfiguration();
    JSONObject oldConfig;
    if (StrUtil.isBlank(thirdConfiguration)) {
      oldConfig = new JSONObject();
    } else {
      oldConfig = JSONUtil.parseObj(thirdConfiguration);
    }

    // 获取旧的customField中的系统字段（需要保留）
    JSONArray oldCustomFields = oldConfig.getJSONArray("customField");
    JSONArray systemFields = new JSONArray();
    if (oldCustomFields != null) {
      for (int i = 0; i < oldCustomFields.size(); i++) {
        JSONObject field = oldCustomFields.getJSONObject(i);
        if (Boolean.TRUE.equals(field.getBool("system"))) {
          systemFields.add(field);
        }
      }
    }

    // 构建新的customField数组（新格式）
    JSONArray newCustomFields = newConfig.getJSONArray("customField");
    JSONArray finalCustomFields = new JSONArray();

    // 先添加系统字段
    for (int i = 0; i < systemFields.size(); i++) {
      finalCustomFields.add(systemFields.get(i));
    }

    // 添加用户自定义字段（过滤掉与系统字段id重复的）
    if (newCustomFields != null) {
      for (int i = 0; i < newCustomFields.size(); i++) {
        JSONObject field = newCustomFields.getJSONObject(i);
        String fieldId = field.getStr("id");
        // 检查是否与系统字段重复
        boolean isDuplicate = false;
        for (int j = 0; j < systemFields.size(); j++) {
          if (fieldId != null && fieldId.equals(systemFields.getJSONObject(j).getStr("id"))) {
            isDuplicate = true;
            break;
          }
        }
        if (!isDuplicate && StrUtil.isNotBlank(fieldId)) {
          // 确保用户字段有必要的属性
          if (!field.containsKey("type")) {
            field.set("type", "string");
          }
          if (!field.containsKey("required")) {
            field.set("required", false);
          }
          field.set("system", false);
          finalCustomFields.add(field);
        }
      }
    }

    // 只保留customField字段，移除旧的ext兼容字段
    JSONObject cleanConfig = new JSONObject();
    cleanConfig.set("customField", finalCustomFields);
    // 保留其他非ext开头的配置项
    for (String key : oldConfig.keySet()) {
      if (!key.startsWith("ext") && !"customField".equals(key)) {
        cleanConfig.set(key, oldConfig.get(key));
      }
    }

    product.setThirdConfiguration(cleanConfig.toString());
    prepareProductSystemFieldsForWrite(product);

    int updateCount = ioTProductMapper.updateDevProduct(product);
    if (updateCount > 0) {
      ioTProductAction.update(product.getProductKey());
      log.info(
          "产品其他配置更改={},产品其他配置更改状态={}，修改人={}",
          product.getProductKey() + product.getName(),
          updateCount,
          product.getCreatorId());
    }
    return updateCount;
  }

  @Override
  public int updateDevProductStoreConfig(IoTProductVO storeConfig) {
    // 获取产品信息
    IoTProduct product = ioTProductMapper.selectDevProductById(String.valueOf(storeConfig.getId()));
    assertProductWritable(product);
    product.setStorePolicyConfiguration(storeConfig.getStorePolicyConfiguration());
    int updateCount = ioTProductMapper.updateDevProduct(product);
    if (updateCount > 0) {
      cacheInvalidationPublisher.publishProductStorePolicyChange(
          product.getProductKey(), "product-store-policy-update");
    }
    return updateCount;
  }

  @Override
  public void flushNettyServer(String config, String productKey, TcpFlushType type) {
    JSONObject object = new JSONObject();
    object.set("type", ProductFlushType.server.name());
    object.set("productKey", productKey);
    object.set("customType", type.name());
    eventPublisher.publishEvent(EventTopics.PRODUCT_CONFIG_UPDATED, object);
    // 使用ProtocolClusterService进行集群操作
    Map<String, ProtocolClusterService> beansOfType =
        SpringUtil.getBeansOfType(ProtocolClusterService.class);
    if (MapUtil.isNotEmpty(beansOfType)) {
      beansOfType.forEach(
          (key, value) -> {
            if (key != null) {
              switch (type) {
                case start -> value.start(productKey);
                case reload -> value.restart(productKey);
                case close -> value.stop(productKey);
              }
            }
          });
    }
  }

  @Override
  public List<IoTProductVO> selectDevProductAllList() {
    return filterAccessibleProductVOs(ioTProductMapper.selectDevProductAllList());
  }

  @Override
  public List<IoTProductVO> getGatewayList() {
    List<IoTProductVO> devProductVOS = filterAccessibleProductVOs(ioTProductMapper.selectDevProductAllList());
    devProductVOS =
        devProductVOS.stream()
            .filter(devProductVO -> DeviceNode.GATEWAY.name().equals(devProductVO.getDeviceNode()))
            .collect(Collectors.toList());
    return devProductVOS;
  }

  @Override
  public List<IoTProductVO> getGatewaySubDeviceList(String productKey) {
    IoTProduct parentProduct =
        ioTProductMapper.selectOne(IoTProduct.builder().productKey(productKey).build());
    if (parentProduct != null) {
      assertProductAccess(parentProduct);
    }
    List<IoTProductVO> devProductVOS = ioTProductMapper.getGatewaySubDeviceList(productKey);
    return filterAccessibleProductVOs(devProductVOS);
  }

  private void assertProductAccess(IoTProduct product) {
    if (product == null) {
      throw new IoTException("产品不存在");
    }
    if (!canAccessProduct(product)) {
      throw new IoTException("无产品权限");
    }
  }

  private void assertProductWritable(IoTProduct product) {
    assertProductAccess(product);
    IoTUser currentUser = currentTenantUser();
    if (currentUser == null) {
      throw new IoTException("获取用户信息异常", HttpStatus.HTTP_UNAUTHORIZED);
    }
    if (currentUser.isAdmin()) {
      return;
    }
    if (!StrUtil.equals(currentUser.getUnionId(), product.getCreatorId())) {
      throw new IoTException("无权操作非本人创建的产品");
    }
  }

  private List<IoTProduct> filterAccessibleProducts(List<IoTProduct> products) {
    if (CollectionUtil.isEmpty(products)) {
      return products;
    }
    return products.stream().filter(this::canAccessProduct).collect(Collectors.toList());
  }

  private List<IoTProductVO> filterAccessibleProductVOs(List<IoTProductVO> products) {
    if (CollectionUtil.isEmpty(products)) {
      return products;
    }
    return products.stream().filter(this::canAccessProduct).collect(Collectors.toList());
  }

  private void applyProductDataScope(IoTProductQuery query) {
    if (query == null) {
      return;
    }
    applyDataPermissionScope(query, currentTenantUser());
    IoTUser currentUser = currentTenantUser();
    if (!query.isSelf() || currentUser == null || StrUtil.isBlank(currentUser.getUnionId())) {
      return;
    }
    if (StrUtil.isBlank(query.getCreatorId())) {
      query.setCreatorId(currentUser.getUnionId());
    }
  }

  private boolean canAccessProduct(IoTProduct product) {
    if (product == null) {
      return false;
    }
    return canAccessTenantResource(product.getTenantId(), product.getCreatorId());
  }

  private boolean canAccessProduct(IoTProductVO product) {
    if (product == null) {
      return false;
    }
    IoTProduct entity =
        IoTProduct.builder()
            .tenantId(product.getTenantId())
            .creatorId(product.getCreatorId())
            .build();
    return canAccessProduct(entity);
  }

  private void applyProductTenantScope(IoTProduct product) {
    if (product == null) {
      return;
    }
    IoTUser currentUser = currentTenantUser();
    if (currentUser == null || currentUser.isAdmin()) {
      return;
    }
    if (StrUtil.isBlank(product.getTenantId())) {
      product.setTenantId(resolveTenantId(currentUser));
    }
  }

  @Override
  public int bindCertificate(String productKey, String sslKey) {
    IoTProduct product =
        ioTProductMapper.selectOne(IoTProduct.builder().productKey(productKey).build());
    assertProductWritable(product);
    JSONObject config = null;
    try {
      config = JSONUtil.parseObj(product.getConfiguration());
    } catch (Exception e) {
      config = new JSONObject();
    }
    config.set("ssl", true);
    config.set("sslKey", sslKey);
    product.setConfiguration(JSONUtil.toJsonStr(config));
    int updateCount = ioTProductMapper.updateByPrimaryKeySelective(product);
    if (updateCount > 0) {
      ioTProductAction.update(productKey);
    }
    return updateCount;
  }

  @Override
  public int unbindCertificate(String productKey) {
    IoTProduct product =
        ioTProductMapper.selectOne(IoTProduct.builder().productKey(productKey).build());
    assertProductWritable(product);
    JSONObject config = null;
    try {
      config = JSONUtil.parseObj(product.getConfiguration());
    } catch (Exception e) {
      config = new JSONObject();
    }
    config.set("ssl", false);
    config.set("sslKey", "");
    product.setConfiguration(JSONUtil.toJsonStr(config));
    int updateCount = ioTProductMapper.updateByPrimaryKeySelective(product);
    if (updateCount > 0) {
      ioTProductAction.update(productKey);
    }
    return updateCount;
  }

  /** 产品配置包：设备和设备工业配置由独立迁移入口处理。 */
  @Override
  @Transactional(readOnly = true)
  public List<ProductExportPackageDTO> exportProductPackages(IoTProductQuery query) {
    if (query == null) query = new IoTProductQuery();
    IoTUser user = currentTenantUser();
    if (user == null) throw new IoTException("获取用户信息异常", HttpStatus.HTTP_UNAUTHORIZED);
    applyProductDataScope(query);
    if (!user.isAdmin()) query.setCreatorId(user.getUnionId());
    Map<String, IoTProduct> products = new java.util.LinkedHashMap<>();
    for (IoTProductExportVO row : ioTProductMapper.selectAllDevProductV2List(query)) {
      IoTProduct product =
          ioTProductMapper.selectOne(IoTProduct.builder().productKey(row.getProductKey()).build());
      assertProductAccess(product);
      products.put(product.getProductKey(), product);
    }
    // 单独导出子产品时带上其网关依赖，避免导入后丢失绑定。
    for (IoTProduct product : new ArrayList<>(products.values())) {
      if (StrUtil.isNotBlank(product.getGwProductKey())
          && !products.containsKey(product.getGwProductKey())) {
        IoTProduct gateway =
            ioTProductMapper.selectOne(
                IoTProduct.builder().productKey(product.getGwProductKey()).build());
        assertProductAccess(gateway);
        products.put(gateway.getProductKey(), gateway);
      }
    }
    List<ProductExportPackageDTO> result = new ArrayList<>();
    for (IoTProduct product : products.values()) {
      ProductExportPackageDTO dto = new ProductExportPackageDTO();
      dto.setExportVersion("2.0");
      dto.setScope("PRODUCT_CONFIGURATION");
      dto.setExportTime(System.currentTimeMillis());
      dto.setExportUserId(user.getUnionId());
      // 从产品实体读取完整字段，避免导出列表SQL遗漏tags、网关关联和密钥。
      dto.setProduct(BeanUtil.copyProperties(product, ProductInfoDTO.class));
      IoTDeviceProtocol protocol =
          ioTDeviceProtocolMapper.selectByPrimaryKey(product.getProductKey());
      if (protocol != null) {
        assertTransferTenant(product.getTenantId(), protocol.getTenantId(), "协议");
        dto.setProtocol(BeanUtil.copyProperties(protocol, DeviceProtocolDTO.class));
      }
      Example networks = new Example(Network.class);
      networks.createCriteria().andEqualTo("productKey", product.getProductKey());
      networks.or().andEqualTo("unionId", product.getProductKey());
      if (StrUtil.isNotBlank(product.getNetworkUnionId()))
        networks.or().andEqualTo("unionId", product.getNetworkUnionId());
      List<NetworkConfigDTO> configs = new ArrayList<>();
      for (Network network : networkMapper.selectByExample(networks)) {
        assertTransferTenant(product.getTenantId(), network.getTenantId(), "网络");
        NetworkConfigDTO config = BeanUtil.copyProperties(network, NetworkConfigDTO.class);
        config.setState(network.isStateEnabled());
        configs.add(config);
      }
      if (StrUtil.isNotBlank(product.getNetworkUnionId())
          && configs.stream().noneMatch(n -> product.getNetworkUnionId().equals(n.getUnionId()))) {
        throw new IoTException("产品「" + product.getName() + "」关联网络不存在，无法导出完整包");
      }
      dto.setNetworks(configs);
      dto.setNetwork(configs.isEmpty() ? null : configs.getFirst());
      dto.setMetadataVersions(
          metadataVersionMapper.selectVersionListByProductKey(product.getProductKey()));
      dto.setCategories(exportPackageCategories(product));
      result.add(dto);
    }
    return result;
  }

  private List<IoTProductCategory> exportPackageCategories(IoTProduct product) {
    List<IoTProductCategory> path = new ArrayList<>();
    Set<String> visited = new java.util.HashSet<>();
    String id = product.getClassifiedId();
    while (StrUtil.isNotBlank(id) && !"0".equals(id)) {
      if (!visited.add(id)) throw new IoTException("产品分类存在循环引用，无法导出完整包");
      IoTProductCategory category = ioTProductSortMapper.selectDevProductSortById(id);
      if (category == null) throw new IoTException("产品「" + product.getName() + "」分类不存在，无法导出完整包");
      assertTransferTenant(product.getTenantId(), category.getTenantId(), "分类");
      path.addFirst(category);
      id = category.getParentId();
    }
    return path;
  }

  private void assertTransferTenant(String productTenant, String resourceTenant, String label) {
    if (StrUtil.isNotBlank(resourceTenant) && !StrUtil.equals(productTenant, resourceTenant)) {
      throw new IoTException(label + "与产品租户不一致，无法导出完整包");
    }
  }

  /** 服务事务覆盖整个文件，任何失败都抛出异常；禁止部分成功或吞异常。 */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public cn.universal.persistence.entity.dto.ProductImportResultDTO importProductPackages(
      List<ProductExportPackageDTO> packages, String unionId, String tenantId) {
    if (CollectionUtil.isEmpty(packages)) throw new IoTException("导入包为空");
    String scopedTenantId = requireImportTenantId(tenantId);
    if (StrUtil.isBlank(unionId)) throw new IoTException("无法识别产品导入用户");
    Map<String, String> keys = new java.util.LinkedHashMap<>();
    Map<String, IoTProductCategory> sourceCategories = new java.util.LinkedHashMap<>();
    List<IoTProduct> products = new ArrayList<>();
    Set<String> existing =
        selectDevProductList(IoTProduct.builder().tenantId(scopedTenantId).build()).stream()
            .map(IoTProduct::getProductKey)
            .collect(Collectors.toSet());
    // 完整校验所有产品及子配置，再开始写入。
    for (int i = 0; i < packages.size(); i++) {
      ProductExportPackageDTO dto = packages.get(i);
      try {
        validateProductPackage(dto);
        if (dto.getCategories() != null) {
          for (IoTProductCategory category : dto.getCategories()) {
            IoTProductCategory prior = sourceCategories.putIfAbsent(category.getId(), category);
            if (prior != null
                && (!StrUtil.equals(prior.getParentId(), category.getParentId())
                    || !sameCategoryConfiguration(prior, category)))
              throw new IoTException("文件内分类「" + category.getId() + "」配置不一致，请重新导出");
          }
        }
        ProductInfoDTO source = dto.getProduct();
        if (existing.contains(source.getProductKey()))
          throw new IoTException("产品已存在，请移除重复产品后重新导入，禁止跳过后部分导入");
        if (keys.containsKey(source.getProductKey())) throw new IoTException("文件内ProductKey重复");
        String key;
        do {
          key = RandomUtil.randomString(12);
        } while (keys.containsValue(key)
            || ioTProductMapper.selectCount(IoTProduct.builder().productKey(key).build()) > 0);
        keys.put(source.getProductKey(), key);
        IoTProduct product = BeanUtil.copyProperties(source, IoTProduct.class);
        product.setProductKey(key);
        // 第三方平台只迁移已有产品配置，不调用无法参与本地事务的外部创建接口。
        product.setProductSecret(
            ProtocolModule.ctaiot.name().equalsIgnoreCase(source.getThirdPlatform())
                ? source.getProductSecret()
                : IdUtil.fastSimpleUUID());
        product.setCreatorId(unionId);
        product.setTenantId(scopedTenantId);
        product.setCreateTime(new Date());
        product.setClassifiedId(null); // 在事务内按分类路径重新映射。
        product.setProductId(StrUtil.blankToDefault(source.getProductId(), key));
        product.setThirdConfiguration(
            StrUtil.blankToDefault(source.getThirdConfiguration(), thirdConfig));
        product.setConfiguration(
            StrUtil.blankToDefault(
                source.getConfiguration(), buildProductCfg(source.getThirdPlatform())));
        product.setStorePolicy(StrUtil.blankToDefault(source.getStorePolicy(), "mysql"));
        validateStorePolicyEnabled(product.getStorePolicy());
        product.setTransportProtocol(
            StrUtil.blankToDefault(source.getTransportProtocol(), source.getThirdPlatform()));
        product.setNetworkUnionId(null);
        prepareProductMetadata(product);
        validateGatewayTransportProtocol(product, null);
        prepareProductSystemFieldsForWrite(product);
        products.add(product);
      } catch (Exception e) {
        throw packageImportFailure(i, dto, "校验", e);
      }
    }
    for (int i = 0; i < products.size(); i++) {
      IoTProduct product = products.get(i);
      try {
        String gatewayKey = product.getGwProductKey();
        if (StrUtil.isNotBlank(gatewayKey)) {
          if (keys.containsKey(gatewayKey)) {
            int gatewayIndex = new ArrayList<>(keys.keySet()).indexOf(gatewayKey);
            if (!DeviceNode.GATEWAY.name().equals(products.get(gatewayIndex).getDeviceNode()))
              throw new IoTException("关联产品不是网关");
            product.setGwProductKey(keys.get(gatewayKey));
          } else {
            IoTProduct gateway =
                ioTProductMapper.selectOne(
                    IoTProduct.builder().productKey(gatewayKey).tenantId(scopedTenantId).build());
            if (gateway == null || !DeviceNode.GATEWAY.name().equals(gateway.getDeviceNode()))
              throw new IoTException("缺少网关依赖「" + gatewayKey + "」，请同时导出网关产品");
          }
        }
        product.setConfiguration(remapPackageJson(product.getConfiguration(), keys));
        product.setThirdConfiguration(remapPackageJson(product.getThirdConfiguration(), keys));
        product.setThirdDownRequest(remapPackageJson(product.getThirdDownRequest(), keys));
      } catch (Exception e) {
        throw packageImportFailure(i, packages.get(i), "关联校验", e);
      }
    }
    int protocols = 0, networks = 0, versions = 0;
    Map<Integer, IoTDeviceProtocol> importedProtocols = new java.util.LinkedHashMap<>();
    List<Network> importedNetworks = new ArrayList<>();
    Map<String, Integer> networkOwners = new java.util.LinkedHashMap<>();
    Map<String, String> categoryKeys = new java.util.LinkedHashMap<>();
    Map<String, NetworkConfigDTO> sourceNetworks = new java.util.LinkedHashMap<>();
    Map<String, String> networkKeys = new java.util.LinkedHashMap<>();
    for (int i = 0; i < products.size(); i++) {
      try {
        products
            .get(i)
            .setClassifiedId(
                importPackageCategories(packages.get(i), scopedTenantId, unionId, categoryKeys));
        if (ioTProductMapper.insertDevProduct(products.get(i)) != 1)
          throw new IoTException("产品写入未成功");
      } catch (Exception e) {
        throw packageImportFailure(i, packages.get(i), "产品写入", e);
      }
    }
    for (int i = 0; i < products.size(); i++) {
      ProductExportPackageDTO dto = packages.get(i);
      IoTProduct product = products.get(i);
      String step = "协议写入";
      try {
        if (dto.getProtocol() != null) {
          IoTDeviceProtocol protocol =
              BeanUtil.copyProperties(dto.getProtocol(), IoTDeviceProtocol.class);
          protocol.setId(product.getProductKey());
          protocol.setTenantId(scopedTenantId);
          protocol.setCreateTime(new Date());
          JSONObject configuration =
              JSONUtil.parseObj(remapPackageJson(protocol.getConfiguration(), keys));
          configuration.set("provider", product.getProductKey());
          protocol.setConfiguration(configuration.toString());
          if (ioTDeviceProtocolMapper.insertDevProtocol(protocol) != 1)
            throw new IoTException("协议写入未成功");
          importedProtocols.put(i, protocol);
          protocols++;
        }
        step = "网络写入";
        int index = 0;
        for (NetworkConfigDTO config : packageNetworks(dto)) {
          if (StrUtil.isNotBlank(config.getProductKey())
              && !keys.containsKey(config.getProductKey()))
            throw new IoTException("网络缺少所属产品「" + config.getProductKey() + "」，请同时导出该产品");
          String sourceNetworkKey =
              StrUtil.blankToDefault(
                  config.getUnionId(), dto.getProduct().getProductKey() + "#network" + index++);
          NetworkConfigDTO prior = sourceNetworks.putIfAbsent(sourceNetworkKey, config);
          if (prior != null) {
            if (!prior.equals(config))
              throw new IoTException("共享网络「" + sourceNetworkKey + "」配置不一致");
            continue;
          }
          Network network = BeanUtil.copyProperties(config, Network.class);
          network.setProductKey(
              StrUtil.isNotBlank(config.getProductKey())
                  ? keys.get(config.getProductKey())
                  : (isExclusivePackageNetwork(config.getType()) ? product.getProductKey() : null));
          if (isExclusivePackageNetwork(config.getType())
              && StrUtil.isBlank(network.getProductKey())) throw new IoTException("网络缺少所属产品依赖");
          network.setTenantId(scopedTenantId);
          network.setCreateUser(unionId);
          network.setCreateDate(new Date());
          network.setState(Network.STATE_DISABLED); // 迁移配置后手动启用，事务中不创建监听或远程连接。
          network.setConfiguration(remapPackageJson(config.getConfiguration(), keys));
          if (networkService.insertNetwork(network) != 1) throw new IoTException("网络写入未成功");
          networkKeys.put(sourceNetworkKey, network.getUnionId());
          importedNetworks.add(network);
          networkOwners.put(network.getUnionId(), i);
          networks++;
        }
        step = "物模型版本写入";
        if (dto.getMetadataVersions() != null)
          for (cn.universal.persistence.entity.IoTProductMetadataVersion source :
              dto.getMetadataVersions()) {
            cn.universal.persistence.entity.IoTProductMetadataVersion version =
                BeanUtil.copyProperties(
                    source, cn.universal.persistence.entity.IoTProductMetadataVersion.class);
            version.setId(null);
            version.setProductId(product.getId());
            version.setProductKey(product.getProductKey());
            if (StrUtil.isBlank(version.getOperator())) version.setOperator(unionId);
            if (metadataVersionMapper.insertVersion(version) != 1)
              throw new IoTException("物模型版本写入未成功");
            versions++;
          }
      } catch (Exception e) {
        throw packageImportFailure(i, dto, step, e);
      }
    }
    Map<String, String> references = new java.util.LinkedHashMap<>(keys);
    references.putAll(networkKeys);
    for (Map.Entry<Integer, IoTDeviceProtocol> entry : importedProtocols.entrySet()) {
      try {
        IoTDeviceProtocol protocol = entry.getValue();
        String configuration = remapPackageJson(protocol.getConfiguration(), references);
        if (!StrUtil.equals(configuration, protocol.getConfiguration())) {
          protocol.setConfiguration(configuration);
          if (ioTDeviceProtocolMapper.updateByPrimaryKeySelective(protocol) != 1)
            throw new IoTException("协议引用写入未成功");
        }
      } catch (Exception e) {
        throw packageImportFailure(entry.getKey(), packages.get(entry.getKey()), "协议关联写入", e);
      }
    }
    for (int i = 0; i < products.size(); i++) {
      try {
        IoTProduct product = products.get(i);
        ProductExportPackageDTO dto = packages.get(i);
        String sourceNetwork = dto.getProduct().getNetworkUnionId();
        if (StrUtil.isNotBlank(sourceNetwork)) {
          if (!networkKeys.containsKey(sourceNetwork))
            throw new IoTException("缺少关联网络「" + sourceNetwork + "」，请重新导出完整包");
          product.setNetworkUnionId(networkKeys.get(sourceNetwork));
        } else if (!packageNetworks(dto).isEmpty()) {
          NetworkConfigDTO first = packageNetworks(dto).getFirst();
          product.setNetworkUnionId(
              networkKeys.get(
                  StrUtil.blankToDefault(
                      first.getUnionId(), dto.getProduct().getProductKey() + "#network0")));
        }
        product.setConfiguration(remapPackageJson(product.getConfiguration(), references));
        product.setThirdConfiguration(
            remapPackageJson(product.getThirdConfiguration(), references));
        product.setThirdDownRequest(remapPackageJson(product.getThirdDownRequest(), references));
        product.setStorePolicyConfiguration(
            remapPackageJson(product.getStorePolicyConfiguration(), references));
        if (ioTProductMapper.updateByPrimaryKeySelective(product) != 1)
          throw new IoTException("产品关联写入未成功");
      } catch (Exception e) {
        throw packageImportFailure(i, packages.get(i), "网络关联写入", e);
      }
    }
    for (Network network : importedNetworks) {
      int owner = networkOwners.get(network.getUnionId());
      try {
        network.setConfiguration(remapPackageJson(network.getConfiguration(), references));
        if (networkMapper.updateByPrimaryKeySelective(network) != 1)
          throw new IoTException("网络「" + network.getName() + "」引用写入未成功");
      } catch (Exception e) {
        throw packageImportFailure(owner, packages.get(owner), "网络引用写入", e);
      }
    }
    afterProductImportCommit(products, unionId);
    return new cn.universal.persistence.entity.dto.ProductImportResultDTO(
        products.size(), protocols, networks, versions);
  }

  private boolean isExclusivePackageNetwork(String type) {
    return type != null
        && (type.startsWith("TCP_") || "UDP".equals(type) || "HTTP_SERVER".equals(type));
  }

  private String importPackageCategories(
      ProductExportPackageDTO dto,
      String tenantId,
      String operator,
      Map<String, String> categoryKeys) {
    if (CollectionUtil.isEmpty(dto.getCategories())) {
      if (StrUtil.isBlank(dto.getProduct().getClassifiedId())
          || "0".equals(dto.getProduct().getClassifiedId())) return null;
      if ("2.0".equals(dto.getExportVersion())) throw new IoTException("完整包缺少分类路径，请重新导出");
      // 旧版文件不带分类路径，仅允许引用目标租户或公共分类。
      IoTProductCategory category =
          ioTProductSortMapper.selectDevProductSortById(dto.getProduct().getClassifiedId());
      if (category == null
          || (StrUtil.isNotBlank(category.getTenantId())
              && !tenantId.equals(category.getTenantId()))
          || (StrUtil.isNotBlank(dto.getProduct().getClassifiedName())
              && !dto.getProduct().getClassifiedName().equals(category.getClassifiedName()))) {
        throw new IoTException("旧版包缺少分类依赖，请使用新版完整包重新导出");
      }
      return category.getId();
    }
    String parent = "0";
    for (IoTProductCategory source : dto.getCategories()) {
      String mapped = categoryKeys.get(source.getId());
      if (mapped == null) {
        List<IoTProductCategory> siblings =
            ioTProductSortMapper.getListByParentId(parent, tenantId);
        IoTProductCategory existing =
            siblings == null
                ? null
                : siblings.stream()
                    .filter(c -> source.getClassifiedName().equals(c.getClassifiedName()))
                    .findFirst()
                    .orElse(null);
        if (existing != null) {
          if (!sameCategoryConfiguration(existing, source))
            throw new IoTException("目标分类「" + source.getClassifiedName() + "」配置不同，请先处理分类冲突");
          mapped = existing.getId();
        } else {
          IoTProductCategory category = BeanUtil.copyProperties(source, IoTProductCategory.class);
          category.setId(null);
          category.setParentId(parent);
          category.setTenantId(tenantId);
          category.setCreateBy(operator);
          category.setCreateTime(new Date());
          category.setChildren(new ArrayList<>());
          if (productCategoryService.insertDevProductSort(category) != 1)
            throw new IoTException("分类写入未成功");
          mapped = category.getId();
        }
        categoryKeys.put(source.getId(), mapped);
      }
      parent = mapped;
    }
    return parent;
  }

  private boolean sameCategoryConfiguration(IoTProductCategory left, IoTProductCategory right) {
    return StrUtil.equals(left.getClassifiedName(), right.getClassifiedName())
        && StrUtil.equals(
            StrUtil.blankToDefault(left.getIdentification(), ""),
            StrUtil.blankToDefault(right.getIdentification(), ""))
        && StrUtil.equals(
            StrUtil.blankToDefault(left.getDescription(), ""),
            StrUtil.blankToDefault(right.getDescription(), ""));
  }

  private List<NetworkConfigDTO> packageNetworks(ProductExportPackageDTO dto) {
    if (dto.getNetworks() != null) return dto.getNetworks();
    return dto.getNetwork() == null ? Collections.emptyList() : List.of(dto.getNetwork());
  }

  private void validateProductPackage(ProductExportPackageDTO dto) {
    if (dto == null || dto.getProduct() == null) throw new IoTException("缺少product产品信息");
    if (StrUtil.isNotBlank(dto.getExportVersion())
        && !List.of("1.0", "2.0").contains(dto.getExportVersion()))
      throw new IoTException("不支持的导出版本：" + dto.getExportVersion());
    if (StrUtil.isNotBlank(dto.getScope()) && !"PRODUCT_CONFIGURATION".equals(dto.getScope()))
      throw new IoTException("不支持的导入范围：" + dto.getScope());
    if ("2.0".equals(dto.getExportVersion())
        && (dto.getNetworks() == null || dto.getMetadataVersions() == null))
      throw new IoTException("2.0完整包缺少networks或metadataVersions，请重新导出");
    if (dto.getNetworks() != null
        && dto.getNetwork() != null
        && !dto.getNetworks().contains(dto.getNetwork()))
      throw new IoTException("network与networks配置不一致，请重新导出完整包");
    ProductInfoDTO p = dto.getProduct();
    if (dto.getCategories() != null && !dto.getCategories().isEmpty()) {
      String parent = "0";
      Set<String> categoryIds = new java.util.HashSet<>();
      for (IoTProductCategory category : dto.getCategories()) {
        if (category == null
            || StrUtil.isBlank(category.getId())
            || StrUtil.isBlank(category.getClassifiedName())
            || !parent.equals(StrUtil.blankToDefault(category.getParentId(), "0"))
            || !categoryIds.add(category.getId())) {
          throw new IoTException("分类路径不完整或存在循环引用，请重新导出");
        }
        parent = category.getId();
      }
      if (!parent.equals(p.getClassifiedId())) throw new IoTException("产品分类与分类路径不一致");
    }
    if (StrUtil.isBlank(p.getProductKey())
        || StrUtil.isBlank(p.getName())
        || StrUtil.isBlank(p.getThirdPlatform()))
      throw new IoTException("productKey、产品名称和接入方式不能为空");
    if (StrUtil.isNotBlank(p.getDeviceNode())
        && DeviceNode.find(p.getDeviceNode()) == null)
      throw new IoTException("设备类型不正确：" + p.getDeviceNode());
    cn.universal.admin.platform.support.ProductImportValidator.metadata(p.getMetadata(), false);
    cn.universal.admin.platform.support.ProductImportValidator.object(
        p.getConfiguration(), "产品配置", false);
    cn.universal.admin.platform.support.ProductImportValidator.object(
        p.getThirdConfiguration(), "自定义字段配置", false);
    cn.universal.admin.platform.support.ProductImportValidator.object(
        p.getStorePolicyConfiguration(), "存储策略配置", false);
    cn.universal.admin.platform.support.ProductImportValidator.object(
        p.getThirdDownRequest(), "第三方产品配置", false);
    if (ProtocolModule.ctaiot.name().equalsIgnoreCase(p.getThirdPlatform())
        && StrUtil.isBlank(p.getProductSecret()))
      throw new IoTException("CTWing迁移缺少原产品密钥，请重新导出；完整包仅迁移已有平台产品配置");
    if (dto.getProtocol() != null) {
      if (StrUtil.isBlank(dto.getProtocol().getType())) throw new IoTException("协议类型不能为空");
      cn.universal.admin.platform.support.ProductImportValidator.object(
          dto.getProtocol().getConfiguration(), "协议配置", true);
    }
    for (NetworkConfigDTO network : packageNetworks(dto)) {
      if (network == null || StrUtil.isBlank(network.getType())) throw new IoTException("网络类型不能为空");
      cn.universal.admin.platform.support.ProductImportValidator.object(
          network.getConfiguration(), "网络配置", true);
    }
    Set<Integer> versions = new java.util.HashSet<>();
    if (dto.getMetadataVersions() != null)
      for (cn.universal.persistence.entity.IoTProductMetadataVersion version :
          dto.getMetadataVersions()) {
        if (version == null
            || version.getVersion() == null
            || version.getVersion() <= 0
            || !versions.add(version.getVersion())) throw new IoTException("物模型版本号无效或重复");
        cn.universal.admin.platform.support.ProductImportValidator.metadata(
            version.getMetadataSnapshot(), false);
      }
  }

  private String remapPackageJson(String json, Map<String, String> keys) {
    String result = json;
    for (Map.Entry<String, String> key : keys.entrySet())
      result = rewriteProductKeyJson(result, key.getKey(), key.getValue(), PRODUCT_KEY_JSON_FIELDS);
    return result;
  }

  private IoTException packageImportFailure(
      int index, ProductExportPackageDTO dto, String step, Exception error) {
    ProductInfoDTO product = dto == null ? null : dto.getProduct();
    String label = product == null ? "缺失产品信息" : product.getName() + " / " + product.getProductKey();
    log.error("产品包整批导入失败: index={}, product={}, step={}", index + 1, label, step, error);
    String reason =
        error instanceof DataAccessException
            ? "数据库写入失败（字段、约束或关联不匹配），详情见服务日志"
            : StrUtil.blankToDefault(error.getMessage(), error.getClass().getSimpleName());
    return new IoTException(
        "第" + (index + 1) + "个产品「" + label + "」" + step + "失败：" + reason + "。整批导入已取消，所有写入回滚。");
  }

  private void afterProductImportCommit(List<IoTProduct> products, String unionId) {
    Runnable action =
        () ->
            products.forEach(
                product -> {
                  try {
                    ioTProductAction.create(product.getProductKey(), unionId);
                  } catch (Exception e) {
                    log.error("产品包已提交，产品创建通知失败: productKey={}", product.getProductKey(), e);
                  }
                });
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              action.run();
            }
          });
    } else {
      action.run();
    }
  }

  private String requireImportTenantId(String tenantId) {
    String scopedTenantId = StrUtil.trim(tenantId);
    if (StrUtil.isBlank(scopedTenantId)) {
      throw new IoTException("无法识别产品导入租户");
    }
    return scopedTenantId;
  }

  @Resource
  private cn.universal.persistence.mapper.IoTStandardProductMapper ioTStandardProductMapper;

  /** 批量更新物模型 支持从公共物模型初始化、批量导入等场景 用Java代码处理，不使用SQL的JSON函数 */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public int batchUpdateMetadata(
      cn.universal.persistence.entity.bo.IoTProductMetadataBatchBO batchBO) {
    // 1. 验证目标产品是否存在
    IoTProduct targetProduct =
        ioTProductMapper.selectOne(
            IoTProduct.builder().productKey(batchBO.getProductKey()).build());
    if (targetProduct == null) {
      throw new IoTException("目标产品不存在");
    }
    assertProductWritable(targetProduct);

    if (batchBO.getMetadata() != null) {
      return importRawMetadata(targetProduct, batchBO);
    }

    // 2. 获取要导入的物模型数据
    java.util.List<cn.universal.persistence.entity.bo.IoTProductMetadataBO> newProperties;
    java.util.List<cn.universal.persistence.entity.bo.IoTProductMetadataBO> newEvents;
    java.util.List<cn.universal.persistence.entity.bo.IoTProductMetadataBO> newFunctions;

    // 如果提供了公共物模型ID，从公共物模型表获取数据
    if (batchBO.getStandardProductId() != null) {
      IoTUser currentUser = currentTenantUser();
      String tenantId =
          currentUser == null || currentUser.isAdmin() ? null : resolveTenantId(currentUser);
      cn.universal.persistence.entity.IoTStandardProduct standardProduct =
          ioTStandardProductMapper.selectProductById(batchBO.getStandardProductId(), tenantId);
      if (standardProduct == null) {
        throw new IoTException("公共物模型不存在");
      }
      // 解析公共物模型的metadata
      JSONObject stdMetadata = parseMetadata(standardProduct.getMetadata());
      newProperties = parseMetadataList(stdMetadata.getJSONArray("properties"));
      newEvents = parseMetadataList(stdMetadata.getJSONArray("events"));
      newFunctions = parseMetadataList(stdMetadata.getJSONArray("functions"));

      // 增加公共物模型的使用次数
      ioTStandardProductMapper.incrementUseCount(batchBO.getStandardProductId(), tenantId);
    } else {
      // 使用请求中的数据
      newProperties =
          batchBO.getProperties() != null ? batchBO.getProperties() : new java.util.ArrayList<>();
      newEvents = batchBO.getEvents() != null ? batchBO.getEvents() : new java.util.ArrayList<>();
      newFunctions =
          batchBO.getFunctions() != null ? batchBO.getFunctions() : new java.util.ArrayList<>();
    }

    // 3. 校验ID不能重复（属性、事件、功能各自内部不能重复）
    validateNoDuplicateIds(newProperties, "属性");
    validateNoDuplicateIds(newEvents, "事件");
    validateNoDuplicateIds(newFunctions, "功能");

    // 4. 根据模式处理物模型
    String mode = batchBO.getMode() != null ? batchBO.getMode() : "replace";
    JSONObject finalMetadata;

    if ("replace".equals(mode)) {
      // 完全替换模式：直接使用新数据
      finalMetadata = parseMetadata(targetProduct.getMetadata());
      finalMetadata.set("properties", new JSONArray(newProperties));
      finalMetadata.set("events", new JSONArray(newEvents));
      finalMetadata.set("functions", new JSONArray(newFunctions));
    } else {
      // 合并模式：保留现有物模型，ID相同的会被覆盖
      JSONObject existingMetadata = parseMetadata(targetProduct.getMetadata());

      java.util.List<cn.universal.persistence.entity.bo.IoTProductMetadataBO> existingProperties =
          parseMetadataList(existingMetadata.getJSONArray("properties"));
      java.util.List<cn.universal.persistence.entity.bo.IoTProductMetadataBO> existingEvents =
          parseMetadataList(existingMetadata.getJSONArray("events"));
      java.util.List<cn.universal.persistence.entity.bo.IoTProductMetadataBO> existingFunctions =
          parseMetadataList(existingMetadata.getJSONArray("functions"));

      // 合并数据（新数据覆盖旧数据）
      java.util.List<cn.universal.persistence.entity.bo.IoTProductMetadataBO> mergedProperties =
          mergeMetadataList(existingProperties, newProperties);
      java.util.List<cn.universal.persistence.entity.bo.IoTProductMetadataBO> mergedEvents =
          mergeMetadataList(existingEvents, newEvents);
      java.util.List<cn.universal.persistence.entity.bo.IoTProductMetadataBO> mergedFunctions =
          mergeMetadataList(existingFunctions, newFunctions);

      finalMetadata = existingMetadata;
      finalMetadata.set("properties", new JSONArray(mergedProperties));
      finalMetadata.set("events", new JSONArray(mergedEvents));
      finalMetadata.set("functions", new JSONArray(mergedFunctions));
    }
    normalizeSystemFunctions(finalMetadata);

    // 5. 更新数据库
    targetProduct.setMetadata(finalMetadata.toString());
    int updateCount = ioTProductMapper.updateByPrimaryKeySelective(targetProduct);

    if (updateCount > 0) {
      // 触发物模型创建事件
      ioTProductAction.metadataCreate(batchBO.getProductKey());
      log.info(
          "批量更新物模型成功: productKey={}, mode={}, properties={}, events={}, functions={}",
          batchBO.getProductKey(),
          mode,
          newProperties.size(),
          newEvents.size(),
          newFunctions.size());
    }

    return updateCount;
  }

  private int importRawMetadata(
      IoTProduct product, cn.universal.persistence.entity.bo.IoTProductMetadataBatchBO request) {
    cn.universal.admin.platform.support.ProductImportValidator.metadata(
        request.getMetadata(), true);
    if (!"replace".equals(request.getMode())) throw new IoTException("物模型JSON导入仅支持完整替换");
    JSONObject result = JSONUtil.parseObj(request.getMetadata());
    product.setMetadata(normalizeMetadataObject(result).toString());
    if (ioTProductMapper.updateByPrimaryKeySelective(product) != 1)
      throw new IoTException("物模型写入失败，原物模型已保留");
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            try {
              ioTProductAction.metadataCreate(product.getProductKey());
            } catch (Exception e) {
              log.error("物模型已提交，更新通知失败: productKey={}", product.getProductKey(), e);
            }
          }
        });
    return 1;
  }

  /** 解析物模型JSON */
  private JSONObject parseMetadata(String metadata) {
    try {
      return ProductMetadataNormalizer.parseMetadata(metadata);
    } catch (Exception e) {
      log.warn("解析物模型JSON失败: {}", e.getMessage());
      return ProductMetadataNormalizer.parseMetadata(null);
    }
  }

  private void prepareProductMetadata(IoTProduct product) {
    if (product == null) {
      return;
    }
    String metadata = StrUtil.blankToDefault(product.getMetadata(), defaultMetadata);
    product.setMetadata(normalizeMetadataJson(metadata));
  }

  private String normalizeMetadataJson(String metadata) {
    return ProductMetadataNormalizer.normalizeMetadataJson(metadata);
  }

  private JSONObject normalizeMetadataObject(JSONObject metadata) {
    return ProductMetadataNormalizer.normalizeMetadataObject(metadata);
  }

  private void normalizeSystemFunctions(JSONObject metadata) {
    ProductMetadataNormalizer.normalizeMetadataObject(metadata);
  }

  private JSONObject getJsonObject(JSONObject source, String key) {
    if (source == null || !source.containsKey(key) || source.get(key) == null) {
      return null;
    }
    Object value = source.get(key);
    if (value instanceof JSONObject) {
      return (JSONObject) value;
    }
    try {
      return JSONUtil.parseObj(value);
    } catch (Exception e) {
      return null;
    }
  }

  private String getMetadataArrayName(String type) {
    switch (type) {
      case "properties":
        return "properties";
      case "events":
        return "events";
      case "functions":
        return "functions";
      default:
        throw new IoTException("物模型键不存在!");
    }
  }

  private JSONArray getOrCreateMetadataArray(JSONObject metadata, String arrayName) {
    JSONArray array = metadata.getJSONArray(arrayName);
    if (array == null) {
      array = new JSONArray();
      metadata.set(arrayName, array);
    }
    return array;
  }

  private int findMetadataIndex(JSONArray array, String targetId) {
    if (array == null || StrUtil.isBlank(targetId)) {
      return -1;
    }
    for (int i = 0; i < array.size(); i++) {
      JSONObject item = array.getJSONObject(i);
      if (item != null && targetId.equals(item.getStr("id"))) {
        return i;
      }
    }
    return -1;
  }

  /** 解析物模型列表 */
  private java.util.List<cn.universal.persistence.entity.bo.IoTProductMetadataBO> parseMetadataList(
      JSONArray array) {
    java.util.List<cn.universal.persistence.entity.bo.IoTProductMetadataBO> list =
        new java.util.ArrayList<>();
    if (array == null || array.isEmpty()) {
      return list;
    }
    for (int i = 0; i < array.size(); i++) {
      JSONObject obj = array.getJSONObject(i);
      cn.universal.persistence.entity.bo.IoTProductMetadataBO bo =
          JSONUtil.toBean(obj, cn.universal.persistence.entity.bo.IoTProductMetadataBO.class);
      list.add(bo);
    }
    return list;
  }

  /** 校验ID不能重复 */
  private void validateNoDuplicateIds(
      java.util.List<cn.universal.persistence.entity.bo.IoTProductMetadataBO> list,
      String typeName) {
    if (list == null || list.isEmpty()) {
      return;
    }
    java.util.Set<String> ids = new java.util.HashSet<>();
    for (cn.universal.persistence.entity.bo.IoTProductMetadataBO bo : list) {
      if (bo.getId() != null && !ids.add(bo.getId())) {
        throw new IoTException(typeName + "ID重复: " + bo.getId());
      }
    }
  }

  /** 合并物模型列表（新数据覆盖旧数据） */
  private java.util.List<cn.universal.persistence.entity.bo.IoTProductMetadataBO> mergeMetadataList(
      java.util.List<cn.universal.persistence.entity.bo.IoTProductMetadataBO> existing,
      java.util.List<cn.universal.persistence.entity.bo.IoTProductMetadataBO> newList) {
    // 用Map存储，ID为key
    java.util.Map<String, cn.universal.persistence.entity.bo.IoTProductMetadataBO> map =
        new java.util.LinkedHashMap<>();

    // 先放入现有数据
    if (existing != null) {
      for (cn.universal.persistence.entity.bo.IoTProductMetadataBO bo : existing) {
        if (bo.getId() != null) {
          map.put(bo.getId(), bo);
        }
      }
    }

    // 再放入新数据（覆盖同名ID）
    if (newList != null) {
      for (cn.universal.persistence.entity.bo.IoTProductMetadataBO bo : newList) {
        if (bo.getId() != null) {
          map.put(bo.getId(), bo);
        }
      }
    }

    return new java.util.ArrayList<>(map.values());
  }
}
