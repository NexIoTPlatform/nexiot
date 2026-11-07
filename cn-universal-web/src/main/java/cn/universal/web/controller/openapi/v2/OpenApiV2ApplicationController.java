package cn.universal.web.controller.openapi.v2;

import cn.universal.common.domain.R;
import cn.universal.web.dto.openapi.v2.OpenApiV2ApplicationModels.ApplicationInfo;
import cn.universal.web.dto.openapi.v2.OpenApiV2ApplicationModels.DeviceBinding;
import cn.universal.web.dto.openapi.v2.OpenApiV2ApplicationModels.HttpPushInfo;
import cn.universal.web.dto.openapi.v2.OpenApiV2ApplicationModels.HttpPushUpdate;
import cn.universal.web.service.openapi.v2.OpenApiV2ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 子系统集成应用接口。应用身份完全由 Token 决定，请求不允许指定或切换 AppId。 */
@RestController
@RequestMapping(value = "/api/openapi/v2/applications/current", produces = "application/json")
@Tag(
    name = "开放 API V2 · 应用",
    description = "查询当前应用、维护 HTTP 推送和绑定设备。需要应用 client_credentials 令牌；应用之间保持隔离")
@SecurityRequirement(name = "Authorization")
@RequiredArgsConstructor
public class OpenApiV2ApplicationController {

  private final OpenApiV2ApplicationService applications;

  @GetMapping
  @Operation(
      operationId = "openApiV2GetCurrentApplication",
      summary = "查询当前应用信息",
      description =
          "根据 Authorization: Bearer 令牌返回当前应用公开信息。不会返回 AppSecret、推送密钥或其他应用。应用停用、删除或过期后拒绝访问")
  public R<ApplicationInfo> current() {
    return R.ok(applications.current());
  }

  @GetMapping("/push/http")
  @Operation(
      operationId = "openApiV2GetHttpPush",
      summary = "查询 HTTP 推送配置",
      description =
          "返回当前应用真实生效的 cfg.http 配置，secretConfigured 表示是否已保存密钥。密钥不回显。设备上报的应用级推送复用现有推送链路，无需逐台创建设备订阅")
  public R<HttpPushInfo> httpPush() {
    return R.ok(applications.httpPush());
  }

  @PutMapping(value = "/push/http", consumes = "application/json")
  @Operation(
      operationId = "openApiV2UpdateHttpPush",
      summary = "更新 HTTP 推送配置",
      description =
          "enable 必填；url、header 不传保留原值，secret 不传或空白保留原值。首次启用必须有 HTTP/HTTPS URL 和密钥，默认密钥请求头"
              + " X-Nexiot-Secret。仅更新当前应用 HTTP"
              + " 通道并刷新现有推送缓存；其他通道、应用身份和应用密钥不变。停用保留原配置。配置成功不表示回调目标已连通，推送成功由目标返回成功状态码确认")
  public R<HttpPushInfo> updateHttpPush(@Valid @RequestBody HttpPushUpdate request) {
    return R.ok(applications.updateHttpPush(request));
  }

  @PutMapping("/devices/{productKey}/{deviceId}")
  @Operation(
      operationId = "openApiV2BindApplicationDevice",
      summary = "绑定设备到当前应用",
      description =
          "只允许绑定当前应用所有者在同一租户内拥有的未绑定设备。已绑定当前应用时幂等成功；已绑定其他应用时拒绝，不能借此转移其他应用的设备。绑定成功后设备可被当前应用查询、控制及推送")
  public R<DeviceBinding> bind(
      @Parameter(description = "产品 ProductKey", required = true, example = "demoProduct")
          @PathVariable
          String productKey,
      @Parameter(description = "产品内唯一设备编号", required = true, example = "device001") @PathVariable
          String deviceId) {
    return R.ok(applications.bind(productKey, deviceId));
  }

  @DeleteMapping("/devices/{productKey}/{deviceId}")
  @Operation(
      operationId = "openApiV2UnbindApplicationDevice",
      summary = "解除当前应用与设备绑定",
      description =
          "只允许操作当前应用所有者在同一租户内拥有的设备。已解绑时幂等成功；设备属于其他应用时拒绝。此操作仅解除应用归属，不删除设备或历史数据，解绑后当前应用不再拥有设备查询和控制权限")
  public R<DeviceBinding> unbind(
      @Parameter(description = "产品 ProductKey", required = true, example = "demoProduct")
          @PathVariable
          String productKey,
      @Parameter(description = "产品内唯一设备编号", required = true, example = "device001") @PathVariable
          String deviceId) {
    return R.ok(applications.unbind(productKey, deviceId));
  }
}
