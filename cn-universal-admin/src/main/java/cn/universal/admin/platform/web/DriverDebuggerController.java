package cn.universal.admin.platform.web;

import cn.universal.admin.platform.service.debug.DriverDebugService;
import cn.universal.admin.platform.service.debug.DriverRevisionService;
import cn.universal.admin.system.service.IoTDeviceProtocolService;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTDeviceProtocol;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.bo.IoTDeviceProtocolBO;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/v1/protocol/{protocolId}/debugger")
@PreAuthorize("@ss.hasPermi('protocol:protocol:codec')")
public class DriverDebuggerController extends BaseController {
  private final IoTDeviceProtocolService protocols;
  private final DriverDebugService debugger;
  private final DriverRevisionService revisions;

  public DriverDebuggerController(
      IoTDeviceProtocolService protocols,
      DriverDebugService debugger,
      DriverRevisionService revisions) {
    this.protocols = protocols;
    this.debugger = debugger;
    this.revisions = revisions;
  }

  private IoTUser user() {
    return loginIoTUnionUser(SecurityUtils.getUnionId());
  }

  private IoTDeviceProtocol require(String id, IoTUser user) {
    if (user == null) throw new IoTException("请先登录");
    IoTDeviceProtocol protocol =
        protocols.selectDevProtocolById(id, user.isAdmin() ? null : user.getUnionId());
    if (protocol == null) throw new IoTException("该驱动不存在或没有操作权限");
    if (!"magic".equals(protocol.getType())) throw new IoTException("当前调试器支持 MagicScript 驱动");
    return protocol;
  }

  @PostMapping("/runs")
  public AjaxResult<?> run(
      @PathVariable String protocolId, @Valid @RequestBody DriverDebugService.RunRequest request) {
    IoTUser user = user();
    require(protocolId, user);
    return AjaxResult.success(debugger.start(user.getUnionId(), protocolId, request));
  }

  @GetMapping("/runs/{runId}")
  public AjaxResult<?> poll(@PathVariable String protocolId, @PathVariable String runId) {
    IoTUser user = user();
    require(protocolId, user);
    return AjaxResult.success(debugger.poll(runId, user.getUnionId(), protocolId));
  }

  @PostMapping("/runs/{runId}/command")
  public AjaxResult<?> command(
      @PathVariable String protocolId,
      @PathVariable String runId,
      @Valid @RequestBody DriverDebugService.Command command) {
    IoTUser user = user();
    require(protocolId, user);
    return AjaxResult.success(debugger.command(runId, user.getUnionId(), protocolId, command));
  }

  @DeleteMapping("/runs/{runId}")
  public AjaxResult<?> cancel(@PathVariable String protocolId, @PathVariable String runId) {
    IoTUser user = user();
    require(protocolId, user);
    debugger.cancel(runId, user.getUnionId(), protocolId);
    return AjaxResult.success();
  }

  @GetMapping("/history")
  public AjaxResult<?> history(@PathVariable String protocolId) {
    require(protocolId, user());
    return AjaxResult.success(revisions.list(protocolId));
  }

  @GetMapping
  public AjaxResult<?> detail(@PathVariable String protocolId) {
    IoTDeviceProtocol protocol = require(protocolId, user());
    return AjaxResult.success(
        Map.of(
            "source",
            DriverRevisionService.source(protocol),
            "sourceHash",
            DriverRevisionService.hash(DriverRevisionService.source(protocol))));
  }

  public record ApplyRequest(
      @NotBlank @Size(max = 262144) String source,
      @NotBlank @Size(max = 64) String expectedSourceHash) {}

  @PutMapping
  @PreAuthorize("@ss.hasPermi('protocol:protocol:edit')")
  public AjaxResult<?> apply(
      @PathVariable String protocolId, @Valid @RequestBody ApplyRequest request) {
    IoTUser user = user();
    require(protocolId, user);
    IoTDeviceProtocolBO update = new IoTDeviceProtocolBO();
    update.setId(protocolId);
    update.setType("magic");
    update.setJscript(request.source());
    update.setExpectedSourceHash(request.expectedSourceHash());
    protocols.updateDevProtocol(update, user);
    return AjaxResult.success(Map.of("sourceHash", DriverRevisionService.hash(request.source())));
  }
}
