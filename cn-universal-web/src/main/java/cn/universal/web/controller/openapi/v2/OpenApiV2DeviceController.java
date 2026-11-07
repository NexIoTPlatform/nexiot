package cn.universal.web.controller.openapi.v2;

import static cn.universal.web.dto.openapi.v2.OpenApiV2DeviceModels.*;

import cn.universal.common.domain.R;
import cn.universal.dm.device.entity.IoTDevicePropertiesBO;
import cn.universal.web.service.openapi.v2.OpenApiV2DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 面向其他子系统的 V2 设备资源接口。独立于 ApiControllerV1，复用现有设备生命周期和协议适配器。
 *
 * <p>通用规则：Authorization: Bearer 应用令牌；code=0 成功；HTTP 400 参数错误，403 越权，409 业务冲突。
 */
@RestController
@RequestMapping(value = "/api/openapi/v2", produces = "application/json")
@RequiredArgsConstructor
@Tag(name = "V2 设备", description = "设备创建、查询、修改、删除、最新属性、物模型和功能调用；所有请求限定当前应用与租户。")
@SecurityRequirement(name = "Authorization")
public class OpenApiV2DeviceController {
  private final OpenApiV2DeviceService service;

  @GetMapping("/devices")
  @Operation(
      operationId = "v2ListDevices",
      summary = "分页查询应用设备",
      description = "只查询当前应用已绑定设备。按创建时间、主键降序；page从1开始，pageSize最大200，最大偏移量10000。查询结果不含设备密钥及原始配置。")
  public R<DevicePage> list(
      @Parameter(description = "可选，精确筛选产品Key") @RequestParam(required = false) String productKey,
      @Parameter(description = "可选，精确筛选设备编号") @RequestParam(required = false) String deviceId,
      @Parameter(description = "页码，从1开始", example = "1") @RequestParam(defaultValue = "1") int page,
      @Parameter(description = "每页1..200条", example = "20") @RequestParam(defaultValue = "20")
          int pageSize) {
    return R.ok(service.list(productKey, deviceId, page, pageSize));
  }

  @PostMapping("/products/{productKey}/devices")
  @Operation(
      operationId = "v2CreateDevice",
      summary = "创建设备并绑定当前应用",
      description =
          "应用所有者必须拥有目标产品。支持mqtt/tcp/udp/sip/industrial/ezviz，其他协议返回409，请通过管理端建档后显式绑定；TCP新增时不可传deviceAddress。已存在设备返回409，不隐式抢占或复用。协议自定义参数放data；子设备父网关也必须已绑定当前应用且节点类型为网关。回执accepted不代表设备已在线；device为同步建档后的实际身份，子设备编号可能按网关与从站地址生成，后续接口应使用返回的device.deviceId。")
  public R<CreationReceipt> create(
      @Parameter(description = "目标产品Key", required = true) @PathVariable String productKey,
      @Valid @RequestBody CreateDevice body) {
    return R.ok(service.create(productKey, body));
  }

  @GetMapping("/devices/{productKey}/{deviceId}")
  @Operation(
      operationId = "v2GetDevice",
      summary = "查询设备信息",
      description = "按产品Key与设备编号查询；仅当前应用绑定设备可访问，不返回密钥和内部配置。不存在或未授权统一403。")
  public R<Device> detail(
      @Parameter(description = "产品Key", required = true) @PathVariable String productKey,
      @Parameter(description = "设备编号，不是iotId", required = true) @PathVariable String deviceId) {
    return R.ok(service.detail(productKey, deviceId));
  }

  @PutMapping("/devices/{productKey}/{deviceId}")
  @Operation(
      operationId = "v2UpdateDevice",
      summary = "更新设备展示信息",
      description =
          "仅允许修改名称、说明、地址和经纬度，省略/null保留原值，传入展示文字不可为空白；通过原协议更新，不能变更所属应用、租户、设备密钥和网关。支持mqtt/tcp/udp/sip/industrial/ezviz，其他协议返回409；协议支持矩阵见docs/openapi/README-v2.md。")
  public R<CommandReceipt> update(
      @Parameter(description = "产品Key", required = true) @PathVariable String productKey,
      @Parameter(description = "设备编号", required = true) @PathVariable String deviceId,
      @Valid @RequestBody UpdateDevice body) {
    return R.ok(service.update(productKey, deviceId, body));
  }

  @DeleteMapping("/devices/{productKey}/{deviceId}")
  @Operation(
      operationId = "v2DeleteDevice",
      summary = "删除应用设备",
      description =
          "通过现有协议及生命周期删除当前应用绑定设备，会执行原有子设备等业务检查。支持协议见docs/openapi/README-v2.md；未知协议及仅支持控制的HTTP协议返回409。该操作删除平台设备，部分云协议还会删除远端关联；仅解除应用关联请使用应用解绑接口。")
  public R<CommandReceipt> delete(
      @Parameter(description = "产品Key", required = true) @PathVariable String productKey,
      @Parameter(description = "设备编号", required = true) @PathVariable String deviceId) {
    return R.ok(service.delete(productKey, deviceId));
  }

  @GetMapping("/devices/{productKey}/{deviceId}/properties")
  @Operation(
      operationId = "v2GetDeviceProperties",
      summary = "查询最新属性与期望值",
      description =
          "读取设备影子和物模型解释后的当前属性值，不主动唤醒或轮询设备。timestamp沿用影子服务单位；离线设备返回最后已知值。历史属性使用properties/{property}/logs接口。")
  public R<List<IoTDevicePropertiesBO>> properties(
      @Parameter(description = "产品Key", required = true) @PathVariable String productKey,
      @Parameter(description = "设备编号", required = true) @PathVariable String deviceId) {
    return R.ok(service.properties(productKey, deviceId));
  }

  @GetMapping("/devices/{productKey}/{deviceId}/model")
  @Operation(
      operationId = "v2GetDeviceModel",
      summary = "查询设备生效物模型",
      description =
          "返回properties/functions/events等物模型定义，优先使用设备派生模型。调用功能前从functions读取功能标识及输入参数；各产品结构由其模型定义。")
  public R<Map<String, Object>> model(
      @Parameter(description = "产品Key", required = true) @PathVariable String productKey,
      @Parameter(description = "设备编号", required = true) @PathVariable String deviceId) {
    return R.ok("success", service.model(productKey, deviceId));
  }

  @PostMapping("/devices/{productKey}/{deviceId}/functions/{function}/invoke")
  @Operation(
      operationId = "v2InvokeDeviceFunction",
      summary = "调用物模型功能",
      description =
          "功能必须存在于当前设备生效物模型；输入参数放data，可传{}调用无参功能。设备目标由路径固定：ezviz的deviceSerial及TTLock的lockId若传入必须一致，缺失时由平台补齐；TTLock仅开放锁级功能白名单。wvp/hik_sdk/dahua_sdk/tiandy_sdk/uniview_sdk/hik_isup统一使用V2视频专用接口，通用功能入口返回409，避免绕过通道和任务隔离。复用原协议下行，不重复包装cmd/messageType。accepted表示平台/协议接受，设备最终执行结果应通过日志、影子或应用推送确认；调用不会自动重试或去重。ezviz/imoulife等云摄像头物模型抓图也可使用此接口。")
  public R<CommandReceipt> invoke(
      @Parameter(description = "产品Key", required = true) @PathVariable String productKey,
      @Parameter(description = "设备编号", required = true) @PathVariable String deviceId,
      @Parameter(description = "物模型功能标识，例如meterRead", required = true) @PathVariable
          String function,
      @Valid @RequestBody FunctionInvocation body) {
    return R.ok(service.invoke(productKey, deviceId, function, body));
  }
}
