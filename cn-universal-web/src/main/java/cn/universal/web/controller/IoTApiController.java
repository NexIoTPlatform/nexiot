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
import cn.universal.admin.platform.service.IIoTProductService;
import cn.universal.common.domain.R;
import cn.universal.core.message.UnifiedDownlinkCommand;
import cn.universal.core.service.IoTDownlFactory;
import cn.universal.dm.device.entity.IoTDevicePropertiesBO;
import cn.universal.dm.device.service.impl.IoTDeviceShadowService;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.dm.device.service.log.IIoTDeviceDataService;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.vo.IoTDeviceLogMetadataVO;
import cn.universal.persistence.query.LogQuery;
import cn.universal.persistence.query.PageBean;
import cn.universal.security.BaseController;
import cn.universal.web.context.IoTCustomAuthContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * IoT设备通信控制器 V1版本
 *
 * <p>提供IoT设备与平台之间的通信接口，包括： - 设备上行数据接收（HTTP、TCP等协议） - 设备下行指令发送 - 设备影子查询 - 产品信息查询 -
 * 第三方平台集成（乐橙、移动OneNet等）
 *
 * <p>该控制器支持多种设备协议和第三方平台，采用工厂模式 根据产品配置自动选择对应的处理服务。
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/8/12 19:11
 */
@RestController
@RequestMapping("iot")
@Tag(name = "IoT设备通信管理", description = "提供IoT设备与平台之间的通信接口")
@Slf4j(topic = "api_log")
public class IoTApiController extends BaseController {

  /** IoT产品设备服务 */
  @Resource private IoTProductDeviceService iotProductDeviceService;

  /** IoT设备影子服务 */
  @Resource private IoTDeviceShadowService iotDeviceShadowService;

  /** IoT内部认证上下文 */
  @Resource private IoTCustomAuthContext ioTCustomAuthContext;

  /** 设备产品服务 */
  @Resource private IIoTProductService devProductService;

  /** IoT设备数据服务 */
  @Autowired private IIoTDeviceDataService iIoTDeviceDataService;

  /**
   * 设备下行指令发送（加密版本）
   *
   * <p>向指定产品的设备发送加密的下行指令，支持消息验证和解密
   *
   * @param productKey 产品标识
   * @param downRequest 加密的下行请求内容
   * @param request HTTP请求对象
   * @return 处理结果
   */
  @RequestMapping("/v1/down/{productKey}")
  @Operation(summary = "设备下行指令发送", description = "向指定产品的设备发送加密的下行指令")
  @Parameter(name = "productKey", description = "产品标识", required = true)
  @Parameter(name = "request", description = "HTTP请求对象")
  @ApiResponse(
      responseCode = "200",
      description = "成功发送下行指令",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  public R iotDown(
      @PathVariable("productKey") String productKey,
      @RequestBody String downRequest,
      HttpServletRequest request) {
    IoTProduct ioTProduct = iotProductDeviceService.getProduct(productKey);
    // SIP 只通过具有设备授权和可信租户上下文的管理/OpenAPI入口下发。
    if ("sip".equalsIgnoreCase(ioTProduct.getThirdPlatform())) {
      return R.error("SIP 请使用设备管理或已鉴权 OpenAPI 的功能下发接口");
    }
    JSONObject data = ioTCustomAuthContext.decryptMessage(downRequest, request);
    // 转换为UnifiedDownlinkCommand（保持所有参数）
    UnifiedDownlinkCommand command = UnifiedDownlinkCommand.fromJson(data);
    return IoTDownlFactory.getIDown(ioTProduct.getThirdPlatform()).doAction(command);
  }

  /**
   * 查询产品信息
   *
   * <p>根据产品标识获取产品的详细信息
   *
   * @param productKey 产品标识
   * @return 产品信息
   */
  @RequestMapping(value = "/product/{productKey}")
  @Operation(summary = "查询产品信息", description = "根据产品标识获取产品的详细信息")
  @Parameter(name = "productKey", description = "产品标识", required = true)
  @ApiResponse(
      responseCode = "200",
      description = "成功获取产品信息",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  public R queryProduct(@PathVariable("productKey") String productKey) {
    IoTProduct ioTProduct = iotProductDeviceService.getProduct(productKey);
    return R.ok(ioTProduct);
  }

  /**
   * 查询设备影子
   *
   * <p>获取指定IoT设备的影子状态信息
   *
   * @param iotId IoT设备标识
   * @return 设备影子属性列表
   */
  @RequestMapping(value = "/device/shadow/{iotId}")
  @Operation(summary = "查询设备影子", description = "获取指定IoT设备的影子状态信息")
  @Parameter(name = "iotId", description = "IoT设备标识", required = true)
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备影子信息",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  public R shadow(@PathVariable("iotId") String iotId) {
    List<IoTDevicePropertiesBO> propertiesBOS = iotDeviceShadowService.getDevState(iotId);
    return R.ok(propertiesBOS);
  }

  /**
   * 查询设备事件元数据列表
   *
   * <p>根据查询条件获取设备的事件统计信息和元数据
   *
   * @param logQuery 日志查询条件
   * @return 设备日志元数据分页结果
   */
  @PostMapping("/device/meta/list")
  @Operation(summary = "查询设备事件元数据列表", description = "根据查询条件获取设备的事件统计信息和元数据")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备事件元数据列表",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class)))
  public Object logMeta(@RequestBody LogQuery logQuery) {
    PageBean<IoTDeviceLogMetadataVO> devLogMetaVoPageBean =
        iIoTDeviceDataService.queryLogMeta(logQuery);
    return devLogMetaVoPageBean;
  }

  /**
   * 测试日志记录接口
   *
   * <p>用于测试和调试的简单接口，记录传入的消息内容
   *
   * @param msg 测试消息内容
   * @return 原消息内容
   */
  @RequestMapping("/echo")
  @Operation(summary = "测试日志记录接口", description = "用于测试和调试的简单接口，记录传入的消息内容")
  @Parameter(name = "msg", description = "测试消息内容")
  @ApiResponse(
      responseCode = "200",
      description = "成功返回消息内容",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class)))
  public Object iotEcho(String msg) {
    log.info("测试日志记录: {}", msg);
    return msg;
  }
}
