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

package cn.universal.web.controller.openapi;

import static cn.universal.common.constant.IoTConstant.DownCmd.DEV_FUNCTION;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.platform.service.IIoTDeviceService;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.constant.IoTConstant.MessageType;
import cn.universal.common.domain.R;
import cn.universal.common.exception.IoTErrorCode;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.core.service.IoTDownlFactory;
import cn.universal.dm.device.entity.IoTDevicePropertiesBO;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.impl.IoTDeviceShadowService;
import cn.universal.dm.device.service.impl.IoTDeviceSubscribeService;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.dm.device.service.log.IIoTDeviceDataService;
import cn.universal.http.access.pipeline.HttpUpstreamService;
import cn.universal.persistence.base.IoTDeviceLifeCycle;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.dto.IoTDeviceSubscribeBO;
import cn.universal.persistence.entity.IoTDeviceEvents;
import cn.universal.persistence.entity.IoTDeviceShadow;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.bo.IoTDeviceBO;
import cn.universal.persistence.entity.dto.IoTFunctionCallDTO;
import cn.universal.persistence.entity.vo.IoTDeviceDetailVO;
import cn.universal.persistence.entity.vo.IoTDeviceLogMetadataVO;
import cn.universal.persistence.entity.vo.IoTDeviceVO;
import cn.universal.persistence.entity.vo.IoTProductVO;
import cn.universal.persistence.query.IoTAPIQuery;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PageBean;
import cn.universal.persistence.query.PageRet;
import cn.universal.web.config.annotation.CodeKey;
import cn.universal.web.config.annotation.Codec;
import cn.universal.web.controller.common.BaseApiController;
import com.github.pagehelper.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/3/25
 */
@RestController
@RequestMapping("/api/openapi/v1/iot")
@Tag(name = "开放API接口", description = "提供设备管理、数据上报等开放API接口")
@Slf4j(topic = "api")
public class ApiControllerV1 extends BaseApiController {

  @Resource private IoTDeviceService iotDeviceService;

  @Resource(name = "ioTDeviceActionAfterService")
  private IoTDeviceLifeCycle ioTDeviceLifeCycle;

  @Resource private IoTDeviceShadowService iotDeviceShadowService;
  @Resource private IoTProductDeviceService iotProductDeviceService;

  @Resource private IoTDeviceSubscribeService iotDeviceSubscribeService;

  @Resource private IIoTDeviceDataService iIoTDeviceDataService;

  @Resource private IIoTDeviceService iIotDeviceService;

  @Resource private HttpUpstreamService httpUpstreamService;

  /**
   * 设备增加
   *
   * @param productKey 产品Key
   * @param downRequest 下行请求参数
   * @return 添加结果
   */
  @Operation(summary = "设备增加", description = "为指定产品添加新设备")
  @ApiResponse(
      responseCode = "200",
      description = "成功添加设备",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "下行请求参数",
      required = true,
      content = @Content(mediaType = "application/json", schema = @Schema(type = "string")))
  @PostMapping("/device/{productKey}/add")
  public R newIoTDevice(
      @PathVariable("productKey") String productKey, @RequestBody String downRequest) {
    log.info("openapi添加设备，用户={},productKey={}", iotUnionId(), productKey);
    String unionId = iotUnionId();
    JSONObject obj = JSONUtil.parseObj(downRequest);
    if (obj.isEmpty()) {
      return R.error(IoTErrorCode.DATA_CAN_NOT_NULL.getCode(), "参数为空");
    }
    if (!DownCmd.DEV_ADD.getValue().equalsIgnoreCase(obj.getStr("cmd", ""))) {
      return R.error(
          IoTErrorCode.DEV_DOWN_ADD_ERROR.getCode(), IoTErrorCode.DEV_DOWN_ADD_ERROR.getName());
    }
    if (StrUtil.isBlank(obj.getStr("deviceId"))) {
      return R.error(
          IoTErrorCode.DEV_UPDATE_DEVICE_NO_ID_EXIST.getCode(),
          IoTErrorCode.DEV_UPDATE_DEVICE_NO_ID_EXIST.getName());
    }
    // 防止参数呗篡改
    obj.set("appUnionId", unionId);
    obj.set("productKey", productKey);
    obj.set("applicationId", iotApplicationId());
    fillDeviceOwnerScope(obj);
    IoTProduct ioTProduct = iotProductDeviceService.getProduct(productKey);
    // 转换为UnifiedDownlinkCommand（保持所有参数）
    UnifiedDownlinkCommand command = UnifiedDownlinkCommand.fromJson(obj);
    return IoTDownlFactory.getIDown(ioTProduct.getThirdPlatform()).doAction(command);
  }

  private void fillDeviceOwnerScope(JSONObject obj) {
    String tenantId = currentTenantId();
    if (StrUtil.isNotBlank(tenantId)) {
      obj.set("tenantId", tenantId);
    } else {
      obj.remove("tenantId");
    }
    Long deptId = currentDeptId();
    if (deptId != null) {
      obj.set("deptId", deptId);
    } else {
      obj.remove("deptId");
    }
  }

  /**
   * 设备批量增加
   *
   * @param productKey 产品Key
   * @param downRequest 下行请求参数
   * @return 批量添加结果
   */
  @Operation(summary = "设备批量增加", description = "为指定产品批量添加新设备")
  @ApiResponse(
      responseCode = "200",
      description = "成功批量添加设备",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "下行请求参数",
      required = true,
      content = @Content(mediaType = "application/json", schema = @Schema(type = "string")))
  @PostMapping("/device/{productKey}/batch/add")
  public R newIoTDevices(
      @PathVariable("productKey") String productKey, @RequestBody String downRequest) {
    log.info("openapi批量添加设备，用户={},productKey={}", iotUnionId(), productKey);
    JSONArray obj = JSONUtil.parseArray(downRequest);
    if (obj == null || obj.isEmpty()) {
      return R.error(IoTErrorCode.DATA_CAN_NOT_NULL.getCode(), "参数为空");
    }
    List<JSONObject> reasons = new ArrayList<>();
    for (Object arr : obj) {
      try {
        JSONObject data = (JSONObject) arr;
        R r = newIoTDevice(productKey, JSONUtil.toJsonStr(data));
        if (!R.SUCCESS.equals(r.getCode())) {
          JSONObject reason = new JSONObject();
          reason.set("deviceId", data.getStr("deviceId"));
          reason.set("reason", r.getMsg());
          reason.set("res", data);
          reasons.add(reason);
        }

      } catch (Exception e) {
        log.warn("设备添加失败={}", JSONUtil.toJsonStr(arr));
      }
    }
    return R.ok(reasons);
  }

  /**
   * 设备上线
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @return 上线结果
   */
  @Operation(summary = "设备上线", description = "将指定设备标记为在线状态")
  @ApiResponse(
      responseCode = "200",
      description = "成功将设备标记为在线",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @PutMapping("/online/{productKey}/{deviceId}")
  public R online(
      @PathVariable("productKey") String productKey, @PathVariable("deviceId") String deviceId) {
    log.info(
        "openapi设备online，用户={},productKey={} deviceId={} ", iotUnionId(), productKey, deviceId);
    IoTAPIQuery query = IoTAPIQuery.builder().deviceId(deviceId).productKey(productKey).build();
    IoTDeviceDTO ioTDeviceDTO = iotDeviceService.selectDevInstanceBO(BeanUtil.beanToMap(query));
    if (ioTDeviceDTO == null) {
      return R.error(IoTErrorCode.DEV_NOT_FIND.getCode(), IoTErrorCode.DEV_NOT_FIND.getName());
    }
    checkDevOrProductSelf(query);
    ioTDeviceLifeCycle.online(productKey, deviceId);
    return R.ok();
  }

  /**
   * 设备删除
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @return 删除结果
   */
  @Operation(summary = "设备删除", description = "删除指定的设备")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除设备",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @DeleteMapping("/device/del/{productKey}/{deviceId}")
  public R deleteIoTDevice(
      @PathVariable("productKey") String productKey, @PathVariable("deviceId") String deviceId) {
    log.info("openapi设备刪除，用户={},productKey={} deviceId={} ", iotUnionId(), productKey, deviceId);
    IoTAPIQuery query = IoTAPIQuery.builder().deviceId(deviceId).productKey(productKey).build();
    IoTDeviceDTO ioTDeviceDTO = iotDeviceService.selectDevInstanceBO(BeanUtil.beanToMap(query));
    if (ioTDeviceDTO == null) {
      return R.error(IoTErrorCode.DEV_NOT_FIND.getCode(), IoTErrorCode.DEV_NOT_FIND.getName());
    }
    checkDevSelf(query);
    JSONObject param = new JSONObject();
    param.set("appUnionId", iotUnionId());
    param.set("productKey", ioTDeviceDTO.getProductKey());
    param.set("deviceId", ioTDeviceDTO.getDeviceId());
    param.set("tenantId", ioTDeviceDTO.getTenantId());
    param.set("cmd", DownCmd.DEV_DEL.name());
    param.set("data", new JSONObject());
    param.set("applicationId", iotApplicationId());
    IoTProduct ioTProduct = iotProductDeviceService.getProduct(ioTDeviceDTO.getProductKey());
    // 转换为UnifiedDownlinkCommand（保持所有参数）
    UnifiedDownlinkCommand command = UnifiedDownlinkCommand.fromJson(param);
    return IoTDownlFactory.getIDown(ioTProduct.getThirdPlatform()).doAction(command);
  }

  /**
   * 设备修改名称，经纬度
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @param ioTDeviceBO 修改请求参数
   * @return 修改结果
   */
  @Operation(summary = "设备修改名称，经纬度", description = "修改设备的名称、经纬度等信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改设备信息",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "修改请求参数",
      required = true,
      content = @Content(mediaType = "application/json", schema = @Schema(type = "string")))
  @PostMapping("/device/update/{productKey}/{deviceId}")
  public R updateIoTDevice(
      @PathVariable("productKey") String productKey,
      @PathVariable("deviceId") String deviceId,
      @RequestBody IoTDeviceBO ioTDeviceBO) {
    log.info(
        "openapi设备修改，用户={},productKey={} deviceId={}",
        iotUnionId(),
        productKey,
        deviceId);
    IoTAPIQuery query = IoTAPIQuery.builder().deviceId(deviceId).productKey(productKey).build();
    IoTDeviceDTO ioTDeviceDTO = iotDeviceService.selectDevInstanceBO(BeanUtil.beanToMap(query));
    if (ioTDeviceDTO == null) {
      return R.error(IoTErrorCode.DEV_NOT_FIND.getCode(), IoTErrorCode.DEV_NOT_FIND.getName());
    }
    checkDevSelf(query);
    JSONObject updateRequest = new JSONObject();
    updateRequest.set("appUnionId", iotUnionId());
    updateRequest.set("applicationId", iotApplicationId());
    updateRequest.set("productKey", ioTDeviceDTO.getProductKey());
    updateRequest.set("deviceId", ioTDeviceDTO.getDeviceId());
    updateRequest.set("tenantId", ioTDeviceDTO.getTenantId());
    updateRequest.set("detail", ioTDeviceBO.getDetail());
    updateRequest.set("deviceAddress", ioTDeviceBO.getDeviceAddress());
    updateRequest.set("slaveAddress", ioTDeviceBO.getSlaveAddress());
    updateRequest.set("cmd", DownCmd.DEV_UPDATE.name());
    JSONObject ob = new JSONObject();
    ob.putAll(JSONUtil.parseObj(ioTDeviceBO));
    updateRequest.set("data", ob);
    IoTProduct ioTProduct = iotProductDeviceService.getProduct(productKey);
    // 转换为UnifiedDownlinkCommand（保持所有参数）
    UnifiedDownlinkCommand command = UnifiedDownlinkCommand.fromJson(updateRequest);
    return IoTDownlFactory.getIDown(ioTProduct.getThirdPlatform()).doAction(command);
  }

  /**
   * 设备查询,通过deviceId
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @return 设备信息
   */
  @Operation(summary = "设备查询", description = "通过产品Key和设备ID查询设备信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备信息",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @GetMapping("/device/info/{productKey}/{deviceId}")
  public R deviceInfo(
      @PathVariable("productKey") String productKey, @PathVariable("deviceId") String deviceId) {
    log.info("openapi设备查询，用户={},productKey={} deviceId={} ", iotUnionId(), productKey, deviceId);
    IoTAPIQuery query = IoTAPIQuery.builder().deviceId(deviceId).productKey(productKey).build();
    IoTDeviceVO ioTDeviceVO = iotDeviceService.apiIoTDeviceVOInfo(query);
    if (ioTDeviceVO == null) {
      return R.error(IoTErrorCode.DEV_NOT_FIND.getCode(), IoTErrorCode.DEV_NOT_FIND.getName());
    }
    checkDevSelf(query);
    return R.ok(ioTDeviceVO);
  }

  /**
   * 设备查询,通过deviceId
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @return 设备信息
   */
  @Operation(summary = "设备查询V@", description = "通过产品Key和设备ID查询设备信息V@")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备信息V2",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @GetMapping("/device/info/v2/{productKey}/{deviceId}")
  public R deviceInfoV2(
      @PathVariable("productKey") String productKey, @PathVariable("deviceId") String deviceId) {
    log.info("openapi-v2设备查询，用户={},productKey={} deviceId={} ", iotUnionId(), productKey, deviceId);
    IoTAPIQuery query = IoTAPIQuery.builder().deviceId(deviceId).productKey(productKey).build();
    IoTDeviceDetailVO ioTDeviceVO = iotDeviceService.queryDeviceDetail(query);
    if (ioTDeviceVO == null) {
      return R.error(IoTErrorCode.DEV_NOT_FIND.getCode(), IoTErrorCode.DEV_NOT_FIND.getName());
    }
    checkDevSelf(query);
    return R.ok(ioTDeviceVO);
  }

  /**
   * 设备状态数据查询
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @return 设备影子数据
   */
  @Operation(summary = "设备状态数据查询", description = "查询设备的状态数据")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备状态数据查询",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @GetMapping(value = "/device/shadow/{productKey}/{deviceId}")
  public R shadow(
      @PathVariable("productKey") String productKey, @PathVariable("deviceId") String deviceId) {
    log.info("openapi设备影子，用户={},productKey={} deviceId={} ", iotUnionId(), productKey, deviceId);
    IoTAPIQuery query = IoTAPIQuery.builder().deviceId(deviceId).productKey(productKey).build();
    IoTDeviceDTO ioTDeviceDTO = iotDeviceService.selectDevInstanceBO(BeanUtil.beanToMap(query));
    if (ioTDeviceDTO == null) {
      return R.error(IoTErrorCode.DEV_NOT_FIND.getCode(), IoTErrorCode.DEV_NOT_FIND.getName());
    }
    checkDevSelf(ioTDeviceDTO.getIotId());
    List<IoTDevicePropertiesBO> propertiesBOS =
        iotDeviceShadowService.getDevState(ioTDeviceDTO.getIotId());
    return R.ok(propertiesBOS);
  }

  /**
   * 查询影子原值
   *
   * @param iotId IoT设备ID
   * @return 设备影子数据
   */
  @Operation(summary = "查询影子原值", description = "根据IoT设备ID查询查询影子原值")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备影子数据",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "iotId", description = "IoT设备ID", required = true)
  @GetMapping(value = "/device/shadow/data/{iotId}")
  public R getDeviceShadowData(@PathVariable("iotId") String iotId) {
    log.info("openapi设备影子数据，用户={},iotId={} ", iotUnionId(), iotId);
    checkDevSelf(iotId);
    IoTDeviceShadow deviceShadow = iIotDeviceService.getDeviceShadow(iotId);
    return R.ok(deviceShadow);
  }

  /**
   * 属性上报
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @param properties 属性数据
   * @return 上报结果
   */
  @Operation(summary = "属性上报", description = "设备属性数据上报")
  @ApiResponse(
      responseCode = "200",
      description = "成功接收属性数据",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "属性数据",
      required = true,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = JSONObject.class)))
  @PostMapping("/device/report/properties/{productKey}/{deviceId}")
  public R reportProperties(
      @PathVariable("productKey") String productKey,
      @PathVariable("deviceId") String deviceId,
      @RequestBody JSONObject properties) {
    log.info("openapi设备属性上报，用户={},deviceId={} data={} ", iotUnionId(), deviceId, properties);
    IoTAPIQuery query = IoTAPIQuery.builder().deviceId(deviceId).productKey(productKey).build();
    if (properties == null) {
      return R.error(IoTErrorCode.DATA_CAN_NOT_NULL.getCode(), "属性消息为空");
    }
    IoTDeviceDTO ioTDeviceDTO = iotDeviceService.selectDevInstanceBO(BeanUtil.beanToMap(query));
    if (ioTDeviceDTO == null) {
      return R.error(IoTErrorCode.DEV_NOT_FIND.getCode(), IoTErrorCode.DEV_NOT_FIND.getName());
    }
    checkDevOrProductSelf(query);
    properties.set("messageType", MessageType.PROPERTIES);
    properties.set("iotId", ioTDeviceDTO.getIotId());
    httpUpstreamService.processOpenApi(productKey, deviceId, properties.toString());
    return R.ok();
  }

  /**
   * 事件上报
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @param events 事件数据
   * @return 上报结果
   */
  @Operation(summary = "事件上报", description = "设备事件数据上报")
  @ApiResponse(
      responseCode = "200",
      description = "成功接收事件数据",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "事件数据",
      required = true,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = JSONObject.class)))
  @PostMapping("/device/report/event/{productKey}/{deviceId}")
  public R reportEvent(
      @PathVariable("productKey") String productKey,
      @PathVariable("deviceId") String deviceId,
      @RequestBody JSONObject events) {
    log.info("openapi设备事件上报，用户={},deviceId={} data={} ", iotUnionId(), deviceId, events);
    IoTAPIQuery query = IoTAPIQuery.builder().deviceId(deviceId).productKey(productKey).build();
    IoTDeviceDTO ioTDeviceDTO = iotDeviceService.selectDevInstanceBO(BeanUtil.beanToMap(query));
    if (ioTDeviceDTO == null) {
      return R.error(IoTErrorCode.DEV_NOT_FIND.getCode(), IoTErrorCode.DEV_NOT_FIND.getName());
    }
    if (events == null) {
      events = new JSONObject();
    }
    checkDevOrProductSelf(query);
    events.set("messageType", MessageType.EVENT);
    events.set("iotId", ioTDeviceDTO.getIotId());
    httpUpstreamService.processOpenApi(productKey, deviceId, events.toString());
    return R.ok();
  }

  /**
   * 功能调用
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @param data 功能调用数据
   * @return 调用结果
   */
  @Operation(summary = "功能调用", description = "向设备发送功能调用指令")
  @ApiResponse(
      responseCode = "200",
      description = "成功发送功能调用指令",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "功能调用数据",
      required = true,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = IoTFunctionCallDTO.class)))
  @PostMapping("/device/function/{productKey}/{deviceId}")
  public R actionFunction(
      @PathVariable("productKey") String productKey,
      @PathVariable("deviceId") String deviceId,
      @RequestBody JSONObject data) {
    log.info("设备功能调用，用户={},deviceId={} data={} ", iotUnionId(), deviceId, data);
    IoTAPIQuery query = IoTAPIQuery.builder().deviceId(deviceId).productKey(productKey).build();
    IoTDeviceDTO ioTDeviceDTO = iotDeviceService.selectDevInstanceBO(BeanUtil.beanToMap(query));
    if (ioTDeviceDTO == null) {
      return R.error(IoTErrorCode.DEV_NOT_FIND.getCode(), IoTErrorCode.DEV_NOT_FIND.getName());
    }
    if (data == null) {
      data = new JSONObject();
    }
    checkDevOrProductSelf(query);
    JSONObject fuction = new JSONObject();
    fuction.set("function", data);
    fuction.set("messageType", MessageType.FUNCTIONS);
    fuction.set("iotId", ioTDeviceDTO.getIotId());
    fuction.put("appUnionId", iotUnionId());
    fuction.put("productKey", ioTDeviceDTO.getProductKey());
    fuction.put("deviceId", ioTDeviceDTO.getDeviceId());
    fuction.put("tenantId", ioTDeviceDTO.getTenantId());
    fuction.put("cmd", DEV_FUNCTION);
    fuction.put("applicationId", iotApplicationId());
    // 转换为UnifiedDownlinkCommand（保持所有参数）
    UnifiedDownlinkCommand command = UnifiedDownlinkCommand.fromJson(fuction);
    return IoTDownlFactory.getIDown(ioTDeviceDTO.getThirdPlatform()).doAction(command);
  }

  /**
   * 通用数据上报
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @param msg 数据消息
   * @return 上报结果
   */
  @Operation(summary = "通用数据上报", description = "通用设备数据上报接口")
  @ApiResponse(
      responseCode = "200",
      description = "成功接收数据",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "数据消息",
      required = true,
      content = @Content(mediaType = "application/json", schema = @Schema(type = "string")))
  @Codec
  @RequestMapping(value = "/device/report/codec/{productKey}/{deviceId}")
  public Object codecCommon(
      @CodeKey @PathVariable("productKey") String productKey,
      @PathVariable("deviceId") String deviceId,
      @RequestBody String msg) {
    log.info("收到来自: HTTP 云云消息 的消息，{}", msg);
    // 接收消息
    if (StrUtil.isNotBlank(msg)) {
      JSONObject json = JSONUtil.parseObj(msg);
      json.set("deviceId", deviceId);
      json.set("productKey", productKey);
      httpUpstreamService.processOpenApi(productKey, deviceId, json.toString());
    }
    return R.ok();
  }

  /**
   * 设备消息订阅
   *
   * @param iotId 设备ID
   * @param sub 订阅信息
   * @return 订阅结果
   */
  @Operation(summary = "设备消息订阅", description = "为设备订阅消息")
  @ApiResponse(
      responseCode = "200",
      description = "成功订阅设备消息",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "iotId", description = "设备ID", required = true)
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "订阅信息",
      required = true,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = IoTDeviceSubscribeBO.class)))
  @PostMapping("/device/subscribe/{iotId}")
  public R subscribe(@PathVariable("iotId") String iotId, @RequestBody IoTDeviceSubscribeBO sub) {
    String iotUnionId = iotUnionId();
    String applicationId = iotApplicationId();
    log.info("openapi设备查询，用户={},iotId={}  ", iotUnionId, iotId);
    IoTAPIQuery query = IoTAPIQuery.builder().iotId(iotId).build();
    IoTDeviceDTO ioTDeviceDTO = iotDeviceService.selectDevInstanceBO(BeanUtil.beanToMap(query));
    if (ioTDeviceDTO == null) {
      return R.error(IoTErrorCode.DEV_NOT_FIND.getCode(), IoTErrorCode.DEV_NOT_FIND.getName());
    }
    checkDevSelf(query);
    return iotDeviceSubscribeService.doSubscribe(
        ioTDeviceDTO.getIotId(), ioTDeviceDTO.getProductKey(), iotUnionId, applicationId, sub);
  }

  /**
   * 设备消息订阅
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @param sub 订阅信息
   * @return 订阅结果
   */
  @Operation(summary = "设备消息订阅", description = "为设备订阅消息")
  @ApiResponse(
      responseCode = "200",
      description = "成功订阅设备消息",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "订阅信息",
      required = true,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = IoTDeviceSubscribeBO.class)))
  @PostMapping("/device/subscribe/{productKey}/{deviceId}")
  public R subscribe(
      @PathVariable("productKey") String productKey,
      @PathVariable("deviceId") String deviceId,
      @RequestBody IoTDeviceSubscribeBO sub) {
    String unionId = iotUnionId();
    String applicationId = iotApplicationId();
    log.info("openapi设备订阅，用户={},productKey={} deviceId={} ", unionId, productKey, deviceId);
    IoTAPIQuery query = IoTAPIQuery.builder().deviceId(deviceId).productKey(productKey).build();
    IoTDeviceDTO ioTDeviceDTO = iotDeviceService.selectDevInstanceBO(BeanUtil.beanToMap(query));
    if (ioTDeviceDTO == null) {
      return R.error(IoTErrorCode.DEV_NOT_FIND.getCode(), IoTErrorCode.DEV_NOT_FIND.getName());
    }
    checkDevSelf(query);
    return iotDeviceSubscribeService.doSubscribe(
        ioTDeviceDTO.getIotId(), productKey, unionId, applicationId, sub);
  }

  /**
   * 属性日志
   *
   * @param productKey 设备productKey
   * @param deviceId 设备deviceId
   * @param messageType 消息类型
   * @param property 属性
   * @param pageNum 页码
   * @param pageSize 每页大小
   * @return 属性消息
   */
  @Operation(summary = "属性日志", description = "获取设备属性日志消息")
  @ApiResponse(
      responseCode = "200",
      description = "属性日志",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "iotId", description = "设备ID", required = true)
  @Parameter(name = "messageType", description = "消息类型", required = true)
  @Parameter(name = "property", description = "属性", required = true)
  @Parameter(name = "pageNum", description = "页码", required = false)
  @Parameter(name = "pageSize", description = "每页大小", required = false)
  @GetMapping("/device/log/meta/{productKey}/{deviceId}/{messageType}/{property}")
  public PageRet logMeta(
      @PathVariable("productKey") String productKey,
      @PathVariable("deviceId") String deviceId,
      @PathVariable("messageType") String messageType,
      @PathVariable("property") String property,
      @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
      @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
    checkDevSelf(productKey + deviceId);
    LogQuery logQuery =
        LogQuery.builder()
            .iotId(productKey + deviceId)
            .messageType(messageType)
            .deviceId(deviceId)
            .productKey(productKey)
            .pageNum(pageNum)
            .pageSize(pageSize)
            .build();
    if (MessageType.PROPERTIES.equals(MessageType.find(messageType))) {
      logQuery.setProperty(property);
    } else if (MessageType.EVENT.equals(MessageType.find(messageType))) {
      logQuery.setEvent(property);
    }
    PageBean<IoTDeviceLogMetadataVO> devLogMetaVoPageBean =
        iIoTDeviceDataService.queryLogMeta(logQuery);
    return PageRet.ok(devLogMetaVoPageBean);
  }

  /**
   * 设备完整日志列表（用于SaaS查询） 支持根据设备ID、产品Key、消息类型等条件查询完整日志记录
   *
   * @param logQuery 日志查询条件
   * @return 日志列表（PageBean结构）
   */
  @Operation(summary = "设备日志列表", description = "获取设备完整日志列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取日志列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = PageBean.class)))
  @GetMapping("/device/log/list")
  public PageBean deviceLogList(LogQuery logQuery) {
    // 1. 校验设备权限
    if (StrUtil.isNotBlank(logQuery.getIotId())) {
      IoTAPIQuery query = IoTAPIQuery.builder().iotId(logQuery.getIotId()).build();
      IoTDeviceDTO ioTDeviceDTO = iotDeviceService.selectDevInstanceBO(BeanUtil.beanToMap(query));
      if (ioTDeviceDTO == null) {
        PageBean<Object> errorBean = new PageBean<>();
        errorBean.setList(new ArrayList<>());
        errorBean.setTotalCount(0L);
        return errorBean;
      }
      checkDevSelf(query);

      // 2. 补充产品信息
      if (StrUtil.isBlank(logQuery.getProductKey())) {
        logQuery.setProductKey(ioTDeviceDTO.getProductKey());
      }
    }

    // 3. 查询日志列表
    PageBean devLogVoPageBean = iIoTDeviceDataService.pageList(logQuery);
    return devLogVoPageBean;
  }

  /**
   * 获取设备事件的统计信息
   *
   * @param productKey 产品key
   * @param iotId 设备id
   * @return 设备事件统计信息
   */
  @Operation(summary = "获取设备事件的统计信息", description = "根据产品key和设备id获取设备事件的统计信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备事件统计信息",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = PageRet.class)))
  @Parameter(name = "productKey", description = "产品key", required = true)
  @Parameter(name = "iotId", description = "设备id", required = true)
  @GetMapping("/device/event/total/{productKey}/{iotId}")
  public PageRet getDeviceEventTotal(
      @PathVariable("productKey") String productKey, @PathVariable("iotId") String iotId) {
    log.info("openapi获取设备事件统计，用户={},productKey={},iotId={}", iotUnionId(), productKey, iotId);
    checkDevSelf(iotId);
    PageBean<IoTDeviceEvents> devEventsPageBean =
        iIoTDeviceDataService.queryEventTotal(productKey, iotId);
    return PageRet.ok(devEventsPageBean);
  }

  /**
   * 设备绑定
   *
   * @param appid 应用ID
   * @param iotId 设备ID
   * @return 绑定结果
   */
  @Operation(summary = "设备绑定", description = "将设备绑定到应用")
  @ApiResponse(
      responseCode = "200",
      description = "成功绑定设备",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "appid", description = "应用ID", required = true)
  @Parameter(name = "iotId", description = "设备ID", required = true)
  @PutMapping("/device/app/bind/{appid}/{iotId}")
  public R appBind(@PathVariable("appid") String appid, @PathVariable("iotId") String iotId) {
    log.info("设备绑定应用,用户标识={},iotId={}", iotUnionId(), iotId);
    checkDevSelf(iotId);
    checkAPPSelf(appid);
    int i = iotDeviceService.apiAppBind(appid, iotId);
    if (i > 0) {
      return R.ok("绑定成功");
    }
    return R.error(
        IoTErrorCode.APPLICATION_BIND_FAILURE.getCode(),
        IoTErrorCode.APPLICATION_BIND_FAILURE.getName());
  }

  /**
   * 设备解绑
   *
   * @param iotId 设备ID
   * @return 解绑结果
   */
  @Operation(summary = "设备解绑", description = "解除设备与应用的绑定")
  @ApiResponse(
      responseCode = "200",
      description = "成功解绑设备",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "iotId", description = "设备ID", required = true)
  @PutMapping("/device/app/unbind/{iotId}")
  public R appBind(@PathVariable("iotId") String iotId) {
    log.info("设备解绑应用，用户={},iotId={} ", iotUnionId(), iotId);
    checkDevSelf(iotId);
    int i = iotDeviceService.apiAppUnBind(iotId);
    if (i > 0) {
      return R.ok("解绑成功");
    }
    return R.error(
        IoTErrorCode.APPLICATION_BIND_FAILURE.getCode(),
        IoTErrorCode.APPLICATION_BIND_FAILURE.getName());
  }

  /**
   * 产品列表查询
   *
   * @param iotAPIQuery 查询条件
   * @return 产品列表
   */
  @Operation(summary = "产品列表查询", description = "查询产品列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取产品列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = PageRet.class)))
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "查询条件",
      required = false,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = IoTAPIQuery.class)))
  @GetMapping(value = "/product/list")
  public PageRet queryProductV2(IoTAPIQuery iotAPIQuery) {
    Page<IoTProductVO> deviceList = iotProductDeviceService.apiProductList(iotAPIQuery);
    return PageRet.ok(deviceList);
  }

  /**
   * 产品详情
   *
   * @param productKey 产品Key
   * @return 产品详情
   */
  @Operation(summary = "产品详情", description = "获取产品详细信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取产品详情",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @GetMapping(value = "/product/{productKey}")
  public R queryProduct(@PathVariable("productKey") String productKey) {
    IoTProductVO devProduct = iotProductDeviceService.apiProductDetail(productKey);
    return R.ok(devProduct);
  }

  /**
   * 设备列表查询
   *
   * @param iotAPIQuery 查询条件
   * @return 设备列表
   */
  @Operation(summary = "设备列表查询", description = "查询设备列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = PageRet.class)))
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "查询条件",
      required = false,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = IoTAPIQuery.class)))
  @GetMapping(value = "/device/list")
  public PageRet deviceList(IoTAPIQuery iotAPIQuery) {
    String iotUnionId = iotUnionId();
    iotAPIQuery.setIotUnionId(iotUnionId);
    iotAPIQuery.setApplicationId(iotApplicationId());
    Page<IoTDeviceVO> deviceList = iotDeviceService.apiDeviceList(iotAPIQuery);
    return PageRet.ok(deviceList);
  }

  /**
   * V2版本设备列表查询 - 返回完整字段 用于SaaS层设备导入获取完整设备信息（包含密钥、配置、标签等）
   *
   * @param iotAPIQuery 查询条件
   * @return 设备列表查询V2
   */
  @Operation(summary = "设备列表查询V2", description = "返回完整字段的设备列表（用于SaaS导入）")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = PageRet.class)))
  @GetMapping(value = "/device/list/v2")
  public PageRet deviceListV2(IoTAPIQuery iotAPIQuery) {
    String iotUnionId = iotUnionId();
    iotAPIQuery.setIotUnionId(iotUnionId);
    iotAPIQuery.setApplicationId(iotApplicationId());
    Page<IoTDeviceDetailVO> deviceList = iotDeviceService.apiDeviceListV2(iotAPIQuery);
    return PageRet.ok(deviceList);
  }

  // ========== 北向应用推送注册 API ==========

  @Operation(
      summary = "注册北向应用推送地址",
      description = "注册或更新北向应用的 HTTP 推送地址、密钥和请求头。SaaS 应用在配置管理时自动调用此接口。")
  @ApiResponse(
      responseCode = "200",
      description = "注册成功",
      content = @Content(mediaType = "application/json"))
  @PostMapping("/application/push/register")
  public R registerPushConfig(@RequestBody JSONObject body) {
    String unionId = iotUnionId();
    String applicationId = iotApplicationId();
    String url = body.getStr("url");
    String secret = body.getStr("secret");
    String header = body.getStr("header", "X-Nexiot-Secret");

    if (StrUtil.isBlank(url)) {
      return R.error(IoTErrorCode.DATA_CAN_NOT_NULL.getCode(), "推送地址不能为空");
    }

    log.info("[registerPushConfig] 注册推送地址, unionId={}, applicationId={}, url={}", unionId, applicationId, url);

    // 构建订阅对象，使用产品级别订阅（ALL 消息类型）
    IoTDeviceSubscribeBO sub = new IoTDeviceSubscribeBO();
    sub.setUrl(url);
    sub.setMsgType("ALL");

    // 通过 subscribe service 注册（复用现有订阅机制）
    // 这里使用 applicationId 作为 iotId 的标识，subType 为 PRODUCT 级别
    return iotDeviceSubscribeService.doSubscribe(
        applicationId, null, unionId, applicationId, sub);
  }

  @Operation(
      summary = "查询北向应用推送配置",
      description = "查询当前应用的推送配置信息")
  @GetMapping("/application/push/config")
  public R getPushConfig() {
    String unionId = iotUnionId();
    String applicationId = iotApplicationId();
    log.info("[getPushConfig] 查询推送配置, unionId={}, applicationId={}", unionId, applicationId);

    JSONObject result = new JSONObject();
    result.set("unionId", unionId);
    result.set("applicationId", applicationId);
    return R.ok(result);
  }
}
