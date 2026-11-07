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

package cn.universal.web.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.annotation.Log;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.domain.R;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.enums.OperatorType;
import cn.universal.common.exception.IoTErrorCode;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.core.metadata.AbstractFunctionMetadata;
import cn.universal.core.metadata.DeviceMetadata;
import cn.universal.core.service.IoTDownlFactory;
import cn.universal.dm.device.entity.IoTDevicePropertiesBO;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.impl.IoTDeviceShadowService;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.vo.IoTDeviceVO;
import cn.universal.persistence.entity.vo.IoTProductVO;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.query.IoTAPIQuery;
import cn.universal.persistence.query.PageRet;
import cn.universal.web.context.IoTCustomAuthContext;
import cn.universal.web.context.TtlAuthContextHolder;
import cn.universal.web.controller.common.BaseApiController;
import com.github.pagehelper.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 谨慎使用，没有进拦截
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/10/15 19:11
 */
@RestController
@RequestMapping("/api/iot")
@Tag(name = "IoT设备管理", description = "IoT设备管理接口")
@Slf4j(topic = "api_log")
public class IoTController extends BaseApiController {

  @Resource private IoTProductDeviceService iotProductDeviceService;
  @Resource private IoTDeviceShadowService iotDeviceShadowService;
  @Resource private IoTCustomAuthContext ioTCustomAuthContext;
  @Resource private IoTDeviceMapper ioTDeviceMapper;

  @Resource private IoTDeviceService iotDeviceService;

  /** 产品列表查询 */
  @GetMapping(value = "/product/list")
  public PageRet queryProduct(IoTAPIQuery iotAPIQuery) {
    iotAPIQuery.setIotUnionId(iotUnionId());
    Page<IoTProductVO> deviceList = iotProductDeviceService.apiProductList(iotAPIQuery);
    return PageRet.ok(deviceList);
  }

  /** 产品列表查询 */
  @GetMapping(value = "/product/list/v2")
  @Operation(summary = "产品列表查询", description = "查询产品列表")
  @Parameter(name = "iotAPIQuery", description = "产品查询条件")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取产品列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = PageRet.class)))
  public PageRet queryProductV2(IoTAPIQuery iotAPIQuery) {
    Page<IoTProductVO> deviceList = iotProductDeviceService.apiProductList(iotAPIQuery);
    return PageRet.ok(deviceList);
  }

  /** 产品详情 */
  @GetMapping(value = "/product/{productKey}")
  @Operation(summary = "产品详情", description = "根据产品标识获取产品详情")
  @Parameter(name = "productKey", description = "产品标识", required = true)
  @ApiResponse(
      responseCode = "200",
      description = "成功获取产品详情",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  public R queryProduct(@PathVariable("productKey") String productKey) {
    IoTProductVO devProduct = iotProductDeviceService.apiProductDetail(productKey);
    return R.ok(devProduct);
  }

  /** 设备列表查询 */
  @GetMapping(value = "/device/list")
  @Operation(summary = "设备列表查询", description = "查询设备列表")
  @Parameter(name = "iotAPIQuery", description = "设备查询条件")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = PageRet.class)))
  public PageRet deviceList(IoTAPIQuery iotAPIQuery) {
    String iotUnionId = iotUnionId();
    iotAPIQuery.setIotUnionId(iotUnionId);
    iotAPIQuery.setApplicationId(iotApplicationId());
    Page<IoTDeviceVO> deviceList = iotDeviceService.apiDeviceList(iotAPIQuery);
    return PageRet.ok(deviceList);
  }

  /** 设备影子查询，设备状态数据查询 */
  @GetMapping(value = "/device/shadow/{iotId}")
  @Operation(summary = "设备影子查询", description = "查询设备影子，设备状态数据")
  @Parameter(name = "iotId", description = "设备ID", required = true)
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备影子信息",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  public R shadow(@PathVariable("iotId") String iotId) {
    log.info("当前用户={}", TtlAuthContextHolder.getInstance().getContext());
    IoTDeviceDTO ioTDeviceDTO = iotDeviceService.selectDevInstanceBO(iotId);
    if (ioTDeviceDTO == null) {
      return R.error(IoTErrorCode.DEV_NOT_FIND.getCode(), IoTErrorCode.DEV_NOT_FIND.getName());
    }
    checkDevSelf(iotId);
    List<IoTDevicePropertiesBO> propertiesBOS = iotDeviceShadowService.getDevState(iotId);
    return R.ok(propertiesBOS);
  }

  /** 设备增加 */
  @PostMapping("/device/{productKey}/add")
  @Operation(summary = "设备增加", description = "为指定产品添加新设备")
  @Parameter(name = "productKey", description = "产品标识", required = true)
  @ApiResponse(
      responseCode = "200",
      description = "成功添加设备",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Log(
      title = "新增设备",
      businessType = BusinessType.INSERT,
      operatorType = OperatorType.MOBILE,
      isSaveRequestData = true)
  public R newIoTDevice(
      @PathVariable("productKey") String productKey, @RequestBody String downRequest) {
    try {
      // 使用统一命令对象
      UnifiedDownlinkCommand command =
          UnifiedDownlinkCommand.fromString(downRequest)
              .withProductKey(productKey)
              .withAppUnionId(iotUnionId())
              .withApplicationId(iotApplicationId())
              .withCmd(DownCmd.DEV_ADD); // 确保指令类型正确

      // 参数验证
      if (command.getDeviceId() != null && command.getDeviceId().matches(".*[\\^&*~@#$%()/].*")) {
        return R.error(
            IoTErrorCode.DEV_ADD_DEVICE_ERROR.getCode(),
            IoTErrorCode.DEV_ADD_DEVICE_ERROR.getName());
      }

      // 验证并获取产品
      IoTProduct ioTProduct = iotProductDeviceService.getProduct(productKey);
      if (ioTProduct == null) {
        return R.error("产品不存在: " + productKey);
      }

      // 调用统一接口
      return IoTDownlFactory.getIDown(ioTProduct.getThirdPlatform()).doAction(command);

    } catch (IllegalArgumentException e) {
      return R.error(IoTErrorCode.DATA_CAN_NOT_NULL.getCode(), e.getMessage());
    } catch (Exception e) {
      log.error("设备增加失败", e);
      return R.error("设备增加失败: " + e.getMessage());
    }
  }

  /**
   * 设备修改（只修改名称，经纬度）
   *
   * <p>{ "deviceId": "24E124535B176069", "deviceName": "对外开放接口测试_改", "longitude":
   * "40.44801283677155", "latitude": "120.29184397901454" }
   */
  @PostMapping("/device/update/{iotId}")
  @Operation(summary = "设备修改", description = "修改设备信息（只修改名称，经纬度）")
  @Parameter(name = "iotId", description = "设备ID", required = true)
  @ApiResponse(
      responseCode = "200",
      description = "成功修改设备",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Log(
      title = "修改设备",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MOBILE,
      isSaveRequestData = true)
  public R updateIoTDevice(@PathVariable("iotId") String iotId, @RequestBody String downRequest) {
    log.info("修改设备信息,用户标识={},iotId={}", iotUnionId(), iotId);
    IoTDeviceDTO ioTDeviceDTO = iotDeviceService.selectDevInstanceBO(iotId);
    if (ioTDeviceDTO == null) {
      return R.error(IoTErrorCode.DEV_NOT_FIND.getCode(), IoTErrorCode.DEV_NOT_FIND.getName());
    }
    checkDevSelf(iotId);
    IoTAPIQuery apiQuery = JSONUtil.toBean(downRequest, IoTAPIQuery.class);
    apiQuery.setIotUnionId(iotUnionId());
    apiQuery.setApplicationId(iotApplicationId());
    apiQuery.setIotId(iotId);
    Map<String, Object> objectMap = iotDeviceService.apiUpdateDevInfo(apiQuery);
    return R.ok(objectMap);
  }

  /** 设备删除 */
  @DeleteMapping("/device/del/{iotId}")
  @Operation(summary = "设备删除", description = "删除指定设备")
  @Parameter(name = "iotId", description = "设备ID", required = true)
  @ApiResponse(
      responseCode = "200",
      description = "成功删除设备",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Log(
      title = "删除设备",
      businessType = BusinessType.DELETE,
      operatorType = OperatorType.MOBILE,
      isSaveRequestData = true)
  public R deleteIoTDevice(@PathVariable("iotId") String iotId) {
    try {
      log.info("删除设备,用户标识={},iotId={}", iotUnionId(), iotId);

      IoTDeviceDTO ioTDeviceDTO = iotDeviceService.selectDevInstanceBO(iotId);
      if (ioTDeviceDTO == null) {
        return R.error(IoTErrorCode.DEV_NOT_FIND.getCode(), IoTErrorCode.DEV_NOT_FIND.getName());
      }
      checkDevSelf(iotId);

      // 使用统一命令对象
      UnifiedDownlinkCommand command =
          UnifiedDownlinkCommand.builder()
              .productKey(ioTDeviceDTO.getProductKey())
              .deviceId(ioTDeviceDTO.getDeviceId())
              .iotId(iotId)
              .cmd(DownCmd.DEV_DEL)
              .appUnionId(iotUnionId())
              .applicationId(iotApplicationId())
              .tenantId(ioTDeviceDTO.getTenantId())
              .build()
              .validate();

      IoTProduct ioTProduct = iotProductDeviceService.getProduct(ioTDeviceDTO.getProductKey());
      return IoTDownlFactory.getIDown(ioTProduct.getThirdPlatform()).doAction(command);

    } catch (Exception e) {
      log.error("设备删除失败", e);
      return R.error("设备删除失败: " + e.getMessage());
    }
  }

  /** 设备查询 */
  @GetMapping("/device/info/{iotId}")
  @Operation(summary = "设备查询", description = "根据设备ID查询设备信息")
  @Parameter(name = "iotId", description = "设备ID", required = true)
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备信息",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  public R deviceInfo(@PathVariable("iotId") String iotId) {
    log.info("查询设备,用户标识={},iotId={}", iotUnionId(), iotId);
    IoTDeviceVO ioTDeviceVO =
        ioTDeviceMapper.apiDeviceInfo(IoTAPIQuery.builder().iotId(iotId).build());
    if (ioTDeviceVO == null) {
      return R.error(IoTErrorCode.DEV_NOT_FIND.getCode(), IoTErrorCode.DEV_NOT_FIND.getName());
    }
    checkDevSelf(iotId);
    return R.ok(ioTDeviceVO);
  }

  /** 设备查询,通过deviceId */
  @GetMapping("/device/info/{productKey}/{deviceId}")
  @Operation(summary = "设备查询", description = "通过产品标识和设备ID查询设备信息")
  @Parameter(name = "productKey", description = "产品标识", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备信息",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  public R deviceInfo(
      @PathVariable("productKey") String productKey, @PathVariable("deviceId") String deviceId) {
    log.info("查询设备,用户标识={},productKey={},deviceId={}", iotUnionId(), productKey, deviceId);
    IoTAPIQuery query = IoTAPIQuery.builder().deviceId(deviceId).productKey(productKey).build();
    IoTDeviceVO ioTDeviceVO = ioTDeviceMapper.apiDeviceInfo(query);
    if (ioTDeviceVO == null) {
      return R.error(IoTErrorCode.DEV_NOT_FIND.getCode(), IoTErrorCode.DEV_NOT_FIND.getName());
    }
    checkDevSelf(query);
    return R.ok(ioTDeviceVO);
  }

  /** 设备绑定 */
  @PutMapping("/device/app/bind/{appid}/{iotId}")
  @Operation(summary = "设备绑定应用", description = "将设备绑定到指定应用")
  @Parameter(name = "appid", description = "应用ID", required = true)
  @Parameter(name = "iotId", description = "设备ID", required = true)
  @ApiResponse(
      responseCode = "200",
      description = "成功绑定设备",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Log(
      title = "设备绑定应用",
      businessType = BusinessType.OTHER,
      operatorType = OperatorType.MOBILE,
      isSaveRequestData = true)
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

  /** 设备解绑 */
  @PutMapping("/device/app/unbind/{iotId}")
  @Operation(summary = "设备解绑应用", description = "解除设备与应用的绑定关系")
  @Parameter(name = "iotId", description = "设备ID", required = true)
  @ApiResponse(
      responseCode = "200",
      description = "成功解绑设备",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Log(
      title = "设备解绑应用",
      businessType = BusinessType.OTHER,
      operatorType = OperatorType.MOBILE,
      isSaveRequestData = true)
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

  /** 设备功能配置下发 */
  @PostMapping("/device/function/{iotId}")
  @Operation(summary = "设备功能配置下发", description = "向设备下发功能配置")
  @Parameter(name = "iotId", description = "设备ID", required = true)
  @ApiResponse(
      responseCode = "200",
      description = "成功下发功能配置",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Log(
      title = "设备功能配置下发",
      businessType = BusinessType.OTHER,
      operatorType = OperatorType.MOBILE,
      isSaveRequestData = true)
  public R devFunction(@PathVariable("iotId") String iotId, @RequestBody String function) {
    try {
      log.info("设备功能下发，用户={},iotId={} data={} ", iotUnionId(), iotId, function);
      checkDevSelf(iotId);

      // 查询设备信息
      Map<String, Object> map = new HashMap<>();
      map.put("iotId", iotId);
      IoTDeviceDTO ioTDeviceDTO = iotDeviceService.selectDevInstanceBO(map);
      if (ioTDeviceDTO == null) {
        return R.error(IoTErrorCode.DEV_NOT_FIND.getCode(), IoTErrorCode.DEV_NOT_FIND.getName());
      }

      // 验证物模型
      DeviceMetadata metadata = ioTDeviceDTO.getDeviceMetadata();
      if (metadata == null) {
        return R.error(
            IoTErrorCode.DEV_METADATA_NOT_FIND.getCode(),
            IoTErrorCode.DEV_METADATA_NOT_FIND.getName());
      }

      JSONObject jsonObject = JSONUtil.parseObj(function);
      AbstractFunctionMetadata ft = metadata.getFunctionOrNull(jsonObject.getStr("function"));
      if (ft == null) {
        return R.error(
            IoTErrorCode.DEV_METADATA_FUNCTION_NOT_FIND.getCode(),
            "物模型定义的功能不存在,请检查function['" + jsonObject.getStr("function") + "']是否存在");
      }

      // 使用统一命令对象
      UnifiedDownlinkCommand command =
          UnifiedDownlinkCommand.builder()
              .productKey(ioTDeviceDTO.getProductKey())
              .deviceId(ioTDeviceDTO.getDeviceId())
              .iotId(iotId)
              .cmd(DownCmd.DEV_FUNCTION)
              .function(jsonObject) // 设置功能参数
              .appUnionId(iotUnionId())
              .applicationId(iotApplicationId())
              .tenantId(ioTDeviceDTO.getTenantId())
              .build()
              .withSource("web-api") // 标记来源
              .validate();

      IoTProduct ioTProduct = iotProductDeviceService.getProduct(ioTDeviceDTO.getProductKey());
      return IoTDownlFactory.getIDown(ioTProduct.getThirdPlatform()).doAction(command);

    } catch (Exception e) {
      log.error("功能下发失败", e);
      return R.error("功能下发失败: " + e.getMessage());
    }
  }
}
