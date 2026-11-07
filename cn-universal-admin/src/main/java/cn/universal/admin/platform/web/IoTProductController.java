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

package cn.universal.admin.platform.web;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.network.service.INetworkService;
import cn.universal.admin.platform.dto.ConnectionInfoDTO;
import cn.universal.admin.platform.service.ConnectionInfoService;
import cn.universal.admin.platform.service.IIoTDeviceService;
import cn.universal.admin.platform.service.IIoTProductService;
import cn.universal.admin.platform.service.impl.IoTProductServiceImpl;
import cn.universal.admin.system.service.ISysDictTypeService;
import cn.universal.admin.system.service.IoTDeviceProtocolService;
import cn.universal.common.annotation.Log;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.constant.IoTConstant.ProtocolModule;
import cn.universal.common.constant.IoTConstant.TcpFlushType;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.enums.OperatorType;
import cn.universal.common.event.EventTopics;
import cn.universal.common.event.processer.EventPublisher;
import cn.universal.common.exception.IoTException;
import cn.universal.common.utils.JsonDesensitizationUtil;
import cn.universal.core.metadata.PropertyMode;
import cn.universal.core.service.IoTDownlFactory;
import cn.universal.ossm.service.ISysOssService;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.IoTDeviceFunction;
import cn.universal.persistence.entity.IoTDeviceProperties;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.admin.SysDictData;
import cn.universal.persistence.entity.bo.IoTProductBO;
import cn.universal.persistence.entity.bo.IoTProductKeyUpdateBO;
import cn.universal.persistence.entity.vo.IoTDeviceModelVO;
import cn.universal.persistence.entity.vo.IoTProductOptionVO;
import cn.universal.persistence.entity.vo.IoTProductVO;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.persistence.query.IoTProductQuery;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 设备产品管理控制器
 *
 * @since 2025-12-24
 */
@Slf4j
@RestController
@Tag(name = "设备产品管理", description = "设备产品管理接口，包括产品的增删改查、物模型管理等")
@RequestMapping("/admin/v1/product")
public class IoTProductController extends BaseController {

  @Autowired private IIoTProductService devProductService;
  @Autowired private IIoTDeviceService iIotDeviceService;
  @Autowired private ISysDictTypeService dictTypeService;
  @Resource private INetworkService iNetworkService;
  @Resource private IoTDeviceProtocolService ioTDeviceProtocolService;
  @Resource private IoTProductMapper ioTProductMapper;
  @Autowired private IoTProductServiceImpl ioTProductServiceImpl;
  @Autowired private EventPublisher eventPublisher;
  @Autowired private ISysOssService iSysOssService;
  @Autowired private ConnectionInfoService connectionInfoService;

  /** 查询设备产品列表 */
  @Operation(summary = "查询设备产品列表", description = "根据查询条件获取设备产品列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备产品列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/list")
  public TableDataInfo list(IoTProductQuery query) {
    startPage();
    List<IoTProduct> list = devProductService.selectDevProductV2List(query);
    return getDataTable(list);
  }

  /** 获取产品详情通过 ProductKey */
  @Operation(summary = "获取产品详情通过 ProductKey", description = "根据ProductKey获取产品详情")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取产品详情",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "key", description = "产品Key", required = true)
  @GetMapping("/get/pro/{key}")
  public AjaxResult<IoTProduct> getProByKey(@PathVariable("key") String key) {
    return devProductService.selectIoTProductByKey(key);
  }

  /** 获取网关绑定的子产品列表 */
  @Operation(summary = "获取网关绑定的子产品列表", description = "根据网关产品Key获取绑定的子产品列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取子产品列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "gwProductKey", description = "网关产品Key", required = true)
  @GetMapping("/gateway/subProducts/{key}")
  public AjaxResult<List<IoTProduct>> gatewaySubProducts(@PathVariable("key") String gwProductKey) {
    return devProductService.selectGatewaySubProductsByKey(gwProductKey);
  }

  /** 查询设备产品列表 */
  @Operation(summary = "查询设备产品列表(v2)", description = "根据查询条件获取设备产品列表(v2版本)")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备产品列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/v2/list")
  public TableDataInfo v2List(IoTProductQuery query) {
    startPage();
    List<IoTProduct> list = devProductService.selectDevProductV2List(query);
    return getDataTable(list);
  }

  /** 查询设备产品列表 */
  @GetMapping("/v3/list")
  public TableDataInfo v3List(IoTProductQuery query) {
    query.setState(0);
    List<IoTProductVO> list = devProductService.selectDevProductV3List(query);

    return getDataTable(list);
  }

  /** 查询设备产品列表 */
  @GetMapping("/v4/list")
  public TableDataInfo v4List(IoTProductQuery query) {
    query.setState(0);
    List<IoTProductVO> list = devProductService.selectDevProductV4List(query);

    return getDataTable(list);
  }

  /** 产品下拉选择：名称或 ProductKey 搜索，ProductKey 精确回显。 */
  @Operation(summary = "搜索产品选项", description = "分页返回当前用户可见产品的ID、名称和ProductKey")
  @GetMapping("/options")
  public TableDataInfo options(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String productKey,
      @RequestParam(defaultValue = "1") Integer pageNum,
      @RequestParam(defaultValue = "20") Integer pageSize) {
    IoTProductQuery query = new IoTProductQuery();
    query.setName(name);
    query.setProductKey(productKey);
    query.setPageNum(pageNum);
    query.setPageSize(pageSize);
    List<IoTProductOptionVO> list = devProductService.selectProductOptions(query);
    return getDataTable(list);
  }

  /** 查询所有设备产品列表 */
  @GetMapping("/all/list")
  public TableDataInfo v3List() {
    List<IoTProductVO> list = devProductService.selectDevProductAllList();
    return getDataTable(list);
  }

  /** 查询当前已启用的日志存储策略 */
  @GetMapping("/storePolicies/enabled")
  public AjaxResult<Set<String>> getEnabledStorePolicies() {
    return AjaxResult.success(devProductService.getEnabledStorePolicies());
  }

  /** 物模型新增内容 */
  @PostMapping("/metadata/add")
  @Log(
      title = "物模型新增内容",
      businessType = BusinessType.INSERT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> metadataAdd(@RequestBody IoTProductBO ioTProductBO) {
    return toAjax(devProductService.insertMetadata(ioTProductBO));
  }

  /** 物模型新增内容 V2 - 支持空物模型初始化和覆盖模式 */
  @PostMapping("/metadata/add/v2")
  @Log(
      title = "物模型新增内容V2",
      businessType = BusinessType.INSERT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> metadataAddV2(@RequestBody IoTProductBO ioTProductBO) {
    return toAjax(devProductService.insertMetadataV2(ioTProductBO));
  }

  /** 物模型修改内容 */
  @PostMapping("/metadata/update")
  @Log(
      title = "物模型修改内容",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> metadataUpdate(@RequestBody IoTProductBO ioTProductBO) {
    return toAjax(devProductService.updateMetadata(ioTProductBO));
  }

  /** 
   * 物模型批量更新
   * 支持从公共物模型初始化、批量导入等场景
   * 用Java代码处理，不使用SQL的JSON函数
   * 性能更好，一次请求完成所有操作
   */
  @PostMapping("/metadata/batch")
  @Log(
      title = "物模型批量更新",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> metadataBatch(
      @RequestBody cn.universal.persistence.entity.bo.IoTProductMetadataBatchBO batchBO) {
    return toAjax(devProductService.batchUpdateMetadata(batchBO));
  }

  /** 物模型删除内容 */
  @PostMapping("/metadata/delete")
  @Log(
      title = "物模型删除内容",
      businessType = BusinessType.DELETE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Void> metadataDelete(@RequestBody IoTProductBO ioTProductBO) {
    return toAjax(devProductService.deleteMetadata(ioTProductBO));
  }

  /** 物模型查询内容 */
  @PostMapping("/metadata/get")
  public AjaxResult<IoTProductBO> metadataGet(@RequestBody IoTProductBO ioTProductBO) {
    return AjaxResult.success(devProductService.getMetadata(ioTProductBO));
  }

  /** 获取设备产品详细信息 */
  @GetMapping(value = "/{id}")
  public AjaxResult getInfo(
      @PathVariable("id") String id,
      @RequestParam(value = "desensitizeConfig", required = false, defaultValue = "false")
          boolean desensitizeConfig) {
    IoTProductVO product = devProductService.selectDevProductById(id);
    if (desensitizeConfig
        && product != null
        && product.getConfiguration() != null
        && !product.getConfiguration().isEmpty()) {
      JSONObject desensitizedConfig =
          JsonDesensitizationUtil.desensitize(product.getConfiguration());
      if (desensitizedConfig != null) {
        product.setConfiguration(desensitizedConfig.toString());
      }
    }
    return AjaxResult.success(product);
  }

  /** 查询可绑定网关列表 */
  @Operation(summary = "查询可绑定网关产品列表")
  @GetMapping("/gateway/list")
  public AjaxResult<List<IoTProductVO>> getGatewayList() {
    List<IoTProductVO> list = devProductService.getGatewayList();
    return AjaxResult.success(list);
  }

  /** 查询可绑定网关子设备产品列表 */
  @Operation(summary = "查询可绑定网关子设备产品列表")
  @GetMapping("/sub/device/{productKey}")
  public AjaxResult<List<IoTProductVO>> getGatewaySubDeviceList(
      @PathVariable("productKey") String productKey) {
    List<IoTProductVO> list = devProductService.getGatewaySubDeviceList(productKey);
    return AjaxResult.success(list);
  }

  /** 查询电信公共产品列表 */
  @PostMapping(value = "/ctwing/pubpro")
  public AjaxResult getCtwingPubPro(@RequestBody String downRequest) {
    String creatorId = loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId();
    JSONObject jsonObject = JSONUtil.parseObj(downRequest);
    JSONObject downProRequest = new JSONObject();
    Map<String, Object> data = new HashMap<>();
    data.put("searchValue", jsonObject.getStr("searchValue"));
    data.put("pageNow", jsonObject.getInt("pageNum"));
    data.put("pageSize", jsonObject.getInt("pageSize"));
    downProRequest.set("creatorId", creatorId);
    downProRequest.set("cmd", "PUBPRO_GET");
    downProRequest.set("data", data);
    return AjaxResult.success(
        IoTDownlFactory.safeInvokeDown(
            ProtocolModule.ctaiot.name(),
            IoTConstant.DOWN_TO_THIRD_PLATFORM,
            downProRequest.toString()));
  }

  /** 新增设备产品 */
  @Operation(summary = "新增设备产品", description = "新增设备产品信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功新增设备产品",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping
  @Log(
      title = "新增产品",
      businessType = BusinessType.INSERT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult add(@RequestBody IoTProduct ioTProduct) {
    rejectSuperAdminCreate("产品");
    String unionId = loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId();
    ioTProduct.setCreatorId(unionId);
    // 发布产品配置更新事件
    Map<String, Object> eventData = new HashMap<>();
    eventData.put("type", "product");
    eventData.put("productKey", ioTProduct.getProductKey());
    eventData.put("operation", "ADD");
    eventPublisher.publishEvent(EventTopics.PRODUCT_CONFIG_UPDATED, eventData);
    int rows = devProductService.insertDevProduct(ioTProduct);
    return rows > 0
        ? AjaxResult.success(Map.of("id", String.valueOf(ioTProduct.getId()), "productKey", ioTProduct.getProductKey()))
        : toAjax(rows);
  }

  /** 新增设备产品NB */
  @Operation(summary = "新增设备产品NB", description = "新增NB-IoT设备产品")
  @ApiResponse(
      responseCode = "200",
      description = "成功新增NB-IoT设备产品",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping(value = "/nb")
  @Log(
      title = "新增产品NB",
      businessType = BusinessType.INSERT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult addNb(@RequestBody String downRequest) {
    rejectSuperAdminCreate("产品");
    String creatorId = loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId();
    JSONObject jsonObject = JSONUtil.parseObj(downRequest);
    IoTProduct ioTProduct = IoTProduct.builder().name(jsonObject.getStr("name")).build();
    List<IoTProduct> ioTProductList = devProductService.selectDevProductList(ioTProduct);
    if (!ioTProductList.isEmpty()) {
      throw new IoTException("产品名称已存在");
    }
    JSONObject downProRequest = new JSONObject();
    Map<String, Object> data = new HashMap<>();
    data.put("productName", jsonObject.getStr("name"));
    data.put("tupDeviceModel", jsonObject.getStr("productId"));
    data.put("tupIsThrough", jsonObject.getInt("tupIsThrough"));
    data.put("payloadFormat", jsonObject.getInt("payloadFormat"));
    data.put("powerModel", jsonObject.getInt("powerModel"));
    data.put("lwm2mEdrxTime", jsonObject.getStr("lwm2mEdrxTime"));

    downProRequest.set("creatorId", creatorId);
    downProRequest.set("cmd", "pro_add");
    downProRequest.set("companyNo", jsonObject.getStr("companyNo"));
    downProRequest.set("classifiedId", jsonObject.getStr("classifiedId"));
    downProRequest.set("classifiedName", jsonObject.getStr("classifiedName"));
    downProRequest.set("data", data);
    return AjaxResult.success(
        IoTDownlFactory.safeInvokeDown(
            ProtocolModule.ctaiot.name(),
            IoTConstant.DOWN_TO_THIRD_PLATFORM,
            downProRequest.toString()));
  }

  /** 新增电信公共产品 */
  @PostMapping(value = "/ctaiot/public/product")
  @Log(
      title = "新增电信公共产品",
      businessType = BusinessType.INSERT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult addCtAIotPublicProduct(@RequestBody String downRequest) {
    rejectSuperAdminCreate("产品");
    String creatorId = loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId();
    JSONObject jsonObject = JSONUtil.parseObj(downRequest);
    JSONObject downProRequest = buildCtwingPublicProductDownRequest(jsonObject, creatorId);
    return AjaxResult.success(
        IoTDownlFactory.safeInvokeDown(
            ProtocolModule.ctaiot.name(),
            IoTConstant.DOWN_TO_THIRD_PLATFORM,
            downProRequest.toString()));
  }

  static JSONObject buildCtwingPublicProductDownRequest(JSONObject productRequest, String creatorId) {
    JSONObject downProRequest = new JSONObject();
    Map<String, Object> data = new HashMap<>();
    Integer publicProductID = Integer.parseInt(productRequest.getStr("publicProductID"));
    data.put("publicProductId", publicProductID);
    data.put("productId", productRequest.getStr("productId"));
    data.put("productName", productRequest.getStr("name"));
    downProRequest.set("creatorId", creatorId);
    downProRequest.set("classifiedId", productRequest.getStr("classifiedId"));
    downProRequest.set("classifiedName", productRequest.getStr("classifiedName"));
    downProRequest.set("companyNo", productRequest.getStr("companyNo"));
    downProRequest.set("cmd", DownCmd.PUBPRO_ADD.name());
    downProRequest.set("data", data);
    return downProRequest;
  }

  /** 修改设备产品 */
  @PostMapping(value = "/updateNetworkUnionId")
  @Log(
      title = "修改网络组件",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult editNetwork(@RequestBody IoTProductBO ioTProductBO) {
    if (ioTProductBO.getId() == null) {
      throw new IoTException("id不能为空");
    }
    //    if (StrUtil.isBlank(ioTProductBO.getNetworkUnionId())) {
    //      throw new IoTException("networkUnionId不能为空");
    //    }
    return toAjax(
        ioTProductServiceImpl.updateNetworkUnionId(
            ioTProductBO.getId(), ioTProductBO.getNetworkUnionId()));
  }

  /** 子设备绑定网关 */
  @Operation(summary = "子设备绑定网关")
  @PutMapping("/gateway")
  @Log(
      title = "子设备绑定网关",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult editGateway(@RequestBody IoTProductBO ioTProductBO) {
    Long id = ioTProductBO.getId();
    String gwProductKey = ioTProductBO.getGwProductKey();
    logger.info("子设备产品绑定网关,id={},gwProductKey={}", id, gwProductKey);
    IoTProductVO productVO =
        devProductService.selectDevProductById(String.valueOf(ioTProductBO.getId()));
    if (productVO == null) {
      return AjaxResult.error("网关产品不存在");
    }
    productVO.setGwProductKey(gwProductKey);

    return toAjax(devProductService.updateDevProduct(productVO));
  }

  /** 修改产品 ProductKey */
  @PutMapping("/productKey")
  @Log(
      title = "修改产品ProductKey",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult updateProductKey(@RequestBody IoTProductKeyUpdateBO updateBO) {
    return toAjax(devProductService.updateProductKey(updateBO));
  }

  /** 修改设备产品 */
  @Operation(summary = "修改设备产品", description = "修改设备产品信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改设备产品",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping
  @Log(
      title = "修改设备产品",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult edit(@RequestBody IoTProductBO ioTProductBO) {
    if (ProtocolModule.ctaiot.name().equals(ioTProductBO.getThirdPlatform())) {
      IoTProduct pro = devProductService.selectDevProductById(ioTProductBO.getId().toString());
      if (pro == null) {
        throw new IoTException("没有该产品");
      }
      Map<String, Object> data = new HashMap<>();
      JSONObject downProRequest = new JSONObject();
      JSONObject jsonObject = JSONUtil.parseObj(pro.getConfiguration());
      Integer powerModel = ioTProductBO.getPowerModel();
      data.put("powerModel", ioTProductBO.getPowerModel());
      data.put("productName", ioTProductBO.getName());
      data.put("productId", jsonObject.getStr("productId"));
      data.put("classifiedId", ioTProductBO.getClassifiedId());
      if (powerModel == 3) {
        data.put("lwm2mEdrxTime", ioTProductBO.getLwm2mEdrxTime());
      }
      downProRequest.set("cmd", "pro_update");
      downProRequest.set("data", data);
      downProRequest.set("productKey", ioTProductBO.getProductKey());
      return AjaxResult.success(
          IoTDownlFactory.safeInvokeDown(
              ProtocolModule.ctaiot.name(),
              IoTConstant.DOWN_TO_THIRD_PLATFORM,
              downProRequest.toString()));
    }
    IoTProduct ioTProduct = BeanUtil.toBean(ioTProductBO, IoTProduct.class);
    int result = devProductService.updateDevProduct(ioTProduct);
    if (result > 0) {
      // 发布产品配置更新事件
      Map<String, Object> eventData = new HashMap<>();
      eventData.put("type", "product");
      eventData.put("productKey", ioTProduct.getProductKey());
      eventData.put("operation", "UPDATE");
      eventPublisher.publishEvent(EventTopics.PRODUCT_CONFIG_UPDATED, eventData);
    }
    return toAjax(result);
  }

  /** 修改产品配置信息 */
  @PutMapping("/config")
  @Log(
      title = "修改产品配置信息",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult editConfig(@RequestBody IoTProductVO devProduct) {
    return toAjax(devProductService.updateDevProductConfig(devProduct));
  }

  /** 修改产品其他配置信息 */
  @PutMapping("/otherConfig")
  @Log(
      title = "修改产品其他配置信息",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult editOtherConfig(@RequestBody String otherConfig) {
    return toAjax(devProductService.updateDevProductOtherConfig(otherConfig));
  }

  /** 修改产品存储策略 */
  @PutMapping("/storeConfig")
  @Log(
      title = "修改产品存储策略",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult editOtherConfig(@RequestBody IoTProductVO devProduct) {
    return toAjax(devProductService.updateDevProductStoreConfig(devProduct));
  }

  /** 删除设备产品 */
  @Operation(summary = "删除设备产品", description = "根据产品ID数组删除设备产品")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除设备产品",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "ids", description = "产品ID数组", required = true)
  @DeleteMapping("/{ids}")
  @Log(
      title = "删除设备产品",
      businessType = BusinessType.DELETE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult remove(@PathVariable String[] ids) {
    for (String id : ids) {
      IoTProduct ioTProduct = devProductService.selectDevProductById(id);
      if (ioTProduct == null) {
        throw new IoTException("没有该产品");
      }
      IoTDevice ioTDevice = IoTDevice.builder().productKey(ioTProduct.getProductKey()).build();
      List<IoTDevice> ioTDeviceList =
          iIotDeviceService.selectDevInstanceList(
              ioTDevice, loginIoTUnionUser(SecurityUtils.getUnionId()));
      if (!ioTDeviceList.isEmpty()) {
        throw new IoTException("该产品下还有设备");
      }
      if (ProtocolModule.ctaiot.name().equals(ioTProduct.getThirdPlatform())) {
        Map<String, Object> data = new HashMap<>();
        JSONObject downProRequest = new JSONObject();
        JSONObject object = JSONUtil.parseObj(ioTProduct.getConfiguration());
        data.put("productId", object.getStr("productId"));
        data.put("masterKey", object.getStr("masterKey"));
        downProRequest.set("cmd", "pro_del");
        downProRequest.set("data", data);
        downProRequest.set("productKey", ioTProduct.getProductKey());

        return AjaxResult.success(
            IoTDownlFactory.safeInvokeDown(
                ProtocolModule.ctaiot.name(),
                IoTConstant.DOWN_TO_THIRD_PLATFORM,
                downProRequest.toString()));
      }
      // tcp产品删除时同时删除network表
      if (ProtocolModule.tcp.name().equals(ioTProduct.getThirdPlatform())) {
        // 停止监听
        devProductService.flushNettyServer(
            ioTProduct.getConfiguration(), ioTProduct.getProductKey(), TcpFlushType.close);
        iNetworkService.del(ioTProduct.getProductKey());
      }
      // 删除协议
      ioTDeviceProtocolService.deleteDevProtocolById(ioTProduct.getProductKey());

      // 发布产品删除事件
      Map<String, Object> eventData = new HashMap<>();
      eventData.put("type", "product");
      eventData.put("productKey", ioTProduct.getProductKey());
      eventData.put("operation", "DELETE");
      eventPublisher.publishEvent(EventTopics.PRODUCT_CONFIG_UPDATED, eventData);
    }
    return toAjax(devProductService.deleteDevProductByIds(ids));
  }

  /** 查询设备产品物模型属性列表 */
  @Operation(summary = "查询设备产品物模型属性列表", description = "根据产品ID获取设备产品物模型属性列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取物模型属性列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "产品ID", required = true)
  @GetMapping("/properties/{id}")
  public AjaxResult getPropertiesList(@PathVariable("id") String id) {
    List<IoTDeviceProperties> list = devProductService.selectDevProperties(id);
    return AjaxResult.success(list);
  }

  /** 查询设备产品物模型属性列表(功能下发属性) */
  @Operation(summary = "查询设备产品物模型属性列表(功能下发属性)", description = "根据产品ID获取设备产品物模型属性列表(仅包含可写属性)")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取功能下发属性列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "产品ID", required = true)
  @GetMapping("/functionProperties/{id}")
  public AjaxResult getFunctionPropertiesList(@PathVariable("id") String id) {
    List<IoTDeviceProperties> list = devProductService.selectDevProperties(id);
    // 过滤可读写属性
    List<IoTDeviceProperties> dataRw =
        list.stream()
            .filter((item) -> PropertyMode.rw.name().equals(item.getMode()))
            .collect(Collectors.toList());
    // 获取设备公共字段
    List<SysDictData> data =
        dictTypeService.selectDictDataByType(IoTConstant.DEVICE_SHADOW_CUSTOMIZED_PROPERTY);
    // 根据公共字段过滤
    if (Validator.isNotNull(data)) {
      for (SysDictData datum : data) {
        for (int i = 0; i < dataRw.size(); i++) {
          if (dataRw.get(i).getId().equals(datum.getDictValue())) {
            dataRw.remove(i);
            break;
          }
        }
        dataRw.add(
            IoTDeviceProperties.builder()
                .description(datum.getRemark() + "(公共字段)")
                .id(datum.getDictValue())
                .type("string")
                .mode("r")
                .name(datum.getDictLabel())
                .build());
      }
    }
    return AjaxResult.success(dataRw);
  }

  /** 查询设备产品物模型事件列表 */
  @GetMapping("/events/{id}")
  public AjaxResult getEventsList(@PathVariable("id") String id) {
    List<IoTDeviceEvents> list = devProductService.selectDevEvents(id);
    return AjaxResult.success(list);
  }

  /** 查询设备产品物模型方法列表 */
  @GetMapping("/functions/{id}")
  public AjaxResult getFunctionsList(@PathVariable("id") String id) {
    List<IoTDeviceFunction> list = devProductService.selectDevFunctions(id);
    return AjaxResult.success(list);
  }

  /** 根据产品key查model配置 */
  @Operation(summary = "根据产品key查model配置", description = "根据产品ID获取model配置信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取model配置",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "产品ID", required = true)
  @GetMapping("/model/{id}")
  public AjaxResult getmodel(@PathVariable("id") String id) {
    IoTDeviceModelVO ioTDeviceModelVO = devProductService.getModelByProductKey(id);
    return AjaxResult.success(ioTDeviceModelVO);
  }

  @Operation(summary = "根据产品key获取元数据", description = "根据产品Key获取产品元数据信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取产品元数据",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @GetMapping("/metadata/{productKey}")
  public AjaxResult getMetadataProductKey(@PathVariable("productKey") String devId) {
    return AjaxResult.success(devProductService.getMetadataProductKey(devId));
  }

  @Operation(summary = "统计设备数量", description = "统计各产品下的设备数量")
  @ApiResponse(
      responseCode = "200",
      description = "成功统计设备数量",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping("/device/count")
  public AjaxResult countDevNumberByProductKey() {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    Map<String, Integer> products =
        devProductService.countDevNumberByProductKey(
            iotUser.isAdmin() ? null : iotUser.getUnionId());
    return AjaxResult.success(products);
  }

  /**
   * 导出设备产品列表
   *
   * @deprecated 请使用 ProductImportExportController.exportProductPackages() 代替 该接口仅保留用于向后兼容，建议使用
   *     /admin/v1/product/transfer/export
   */
  @Deprecated
  @Operation(summary = "导出设备产品列表(旧版)", description = "导出设备产品列表(旧版，已废弃，请使用新版接口)")
  @ApiResponse(
      responseCode = "200",
      description = "成功导出设备产品列表",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Void.class)))
  @PostMapping("/export")
  @Log(
      title = "产品导出",
      businessType = BusinessType.EXPORT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public void export(HttpServletResponse response, IoTProductQuery query) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    query.setSelf(!iotUser.isAdmin());
    if (query.isSelf()) {
      query.setCreatorId(loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId());
    }
    if (query.isHasDevice()) {
      query.setCreatorId(loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId());
    }

    // 使用新的导出包方法
    List<cn.universal.persistence.entity.dto.ProductExportPackageDTO> packageList =
        devProductService.exportProductPackages(query);

    // 设置响应头为文件下载格式
    response.setContentType("application/json");
    response.setCharacterEncoding("utf-8");

    // 生成文件名：产品导出_时间戳.json
    String fileName = String.format("products_export_%d.json", System.currentTimeMillis());
    response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

    // 添加缓存控制头，防止浏览器缓存
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setHeader("Expires", "0");
    // 强制下载，防止浏览器直接显示
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("X-Download-Options", "noopen");
    // 确保响应不会被压缩
    response.setHeader("Content-Encoding", "identity");

    try {
      // 将数据转换为JSON格式并写入响应
      String jsonData = JSONUtil.toJsonPrettyStr(packageList);
      byte[] bytes = jsonData.getBytes("UTF-8");
      response.setContentLength(bytes.length);

      // 使用缓冲输出流确保数据完整写入
      try (java.io.OutputStream out = response.getOutputStream()) {
        out.write(bytes);
        out.flush();
      }
    } catch (Exception e) {
      throw new IoTException("导出失败：" + e.getMessage());
    }
  }

  /** 上传产品图片并保存到产品信息 */
  @Operation(summary = "上传产品图片", description = "上传产品图片并保存到产品信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功上传产品图片",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "产品ID", required = true)
  @Parameter(name = "file", description = "上传的图片文件", required = true)
  @PostMapping("/uploadImage")
  @Log(
      title = "上传产品图片",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<Map<String, String>> uploadProductImage(
      @RequestParam("id") String id, @RequestParam("file") MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IoTException("上传文件不能为空");
    }
    IoTProductVO product = devProductService.selectDevProductById(id);
    if (product == null) {
      throw new IoTException("产品不存在");
    }
    var user = loginIoTUnionUser(SecurityUtils.getUnionId());
    String unionId = user.getUnionId();
    // 上传到 OSS，沿用系统统一上传逻辑
    var oss = iSysOssService.upload(file, unionId, user.isAdmin() ? "system" : resolveTenantId(user));
    String url = oss.getUrl();

    // 更新产品 photoUrl 字段（保持前端期望结构）
    IoTProduct update = new IoTProduct();
    update.setId(product.getId());
    JSONObject photo = new JSONObject();
    photo.set("img", url);
    photo.set("detail", "");
    update.setPhotoUrl(photo.toString());
    devProductService.updateDevProduct(update);

    Map<String, String> data = new HashMap<>(2);
    data.put("url", url);
    String name = oss.getOriginalName();
    data.put("fileName", name.substring(name.lastIndexOf('/') + 1, name.lastIndexOf(".")));
    return AjaxResult.success(data);
  }

  /**
   * 产品模糊搜索接口
   *
   * @param query 搜索关键字
   * @return productKey和name列表
   */
  @Operation(summary = "产品模糊搜索", description = "根据搜索关键字模糊搜索产品，返回productKey和name列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功搜索产品",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/search")
  public TableDataInfo searchProductKeyAndName(IoTProductQuery query) {
    List<Map<String, String>> maps = ioTProductMapper.searchProductKeyAndName(query);
    return getDataTable(maps);
  }

  /** 绑定证书到产品 */
  @Operation(summary = "绑定证书到产品", description = "将证书绑定到指定产品")
  @ApiResponse(
      responseCode = "200",
      description = "成功绑定证书",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @Parameter(name = "sslKey", description = "SSL证书Key", required = true)
  @PostMapping("/bindCertificate")
  @Log(
      title = "绑定证书",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult bindCertificate(@RequestParam String productKey, @RequestParam String sslKey) {
    int result = devProductService.bindCertificate(productKey, sslKey);
    return toAjax(result);
  }

  /** 解绑证书 */
  @Operation(summary = "解绑证书", description = "解除产品与证书的绑定关系")
  @ApiResponse(
      responseCode = "200",
      description = "成功解绑证书",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @PostMapping("/unbindCertificate")
  @Log(
      title = "解绑证书",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult unbindCertificate(@RequestParam String productKey) {
    int result = devProductService.unbindCertificate(productKey);
    return toAjax(result);
  }

  /** 查看连接信息 */
  @PostMapping("/connect/info")
  public AjaxResult connectInfo(@RequestParam String productKey) {
    try {
      ConnectionInfoDTO connectionInfo = connectionInfoService.getConnectionInfo(productKey);
      return AjaxResult.success(connectionInfo);
    } catch (Exception e) {
      return AjaxResult.error("获取连接信息失败：" + e.getMessage());
    }
  }

  /** 查看MQTT密码 */
  @PostMapping("/connect/mqtt/password")
  @Log(
      title = "查看连接信息密码",
      businessType = BusinessType.GRANT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult connectInfoPassword(@RequestParam String productKey) {
    try {
      ConnectionInfoDTO passwordInfo = connectionInfoService.getMqttPasswordInfo(productKey);
      return AjaxResult.success(passwordInfo);
    } catch (Exception e) {
      return AjaxResult.error("获取密码信息失败：" + e.getMessage());
    }
  }
}
