package cn.universal.admin.platform.web.transform;

import cn.universal.admin.platform.service.transform.IPushTransformRuleService;
import cn.universal.admin.system.service.IIoTUserApplicationService;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTPushTransformRule;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.entity.bo.PushTransformPreviewBO;
import cn.universal.persistence.entity.bo.UPPushBO;
import cn.universal.persistence.entity.vo.PushTransformOverviewVO;
import cn.universal.persistence.entity.vo.PushTransformPreviewVO;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/v1/apply/transform")
@Tag(name = "应用数据转换", description = "应用北向推送数据转换配置")
public class PushTransformController extends BaseController {

  @Resource private IPushTransformRuleService pushTransformRuleService;

  @Resource private IIoTUserApplicationService iotUserApplicationService;

  @Resource private IoTUserService ioTUserService;

  @GetMapping("/{appUniqueId}")
  @PreAuthorize("@ss.hasPermi('application:application:transform:query')")
  @Operation(summary = "获取应用数据转换概览")
  public AjaxResult<PushTransformOverviewVO> getOverview(@PathVariable String appUniqueId) {
    checkAppPermission(appUniqueId);
    return AjaxResult.success(pushTransformRuleService.getOverview(appUniqueId));
  }

  @PutMapping("/{appUniqueId}/config")
  @PreAuthorize("@ss.hasPermi('application:application:transform:config')")
  @Operation(summary = "更新应用数据转换开关配置")
  public AjaxResult<UPPushBO.TransformConfig> updateConfig(
      @PathVariable String appUniqueId, @RequestBody UPPushBO.TransformConfig transformConfig) {
    checkAppPermission(appUniqueId);
    return AjaxResult.success(
        pushTransformRuleService.updateConfig(appUniqueId, transformConfig, SecurityUtils.getUnionId()));
  }

  @PostMapping("/rule")
  @PreAuthorize("@ss.hasPermi('application:application:transform:add')")
  @Operation(summary = "新增数据转换规则")
  public AjaxResult<IoTPushTransformRule> addRule(@RequestBody IoTPushTransformRule rule) {
    checkAppPermission(rule.getAppUniqueId());
    return AjaxResult.success(
        pushTransformRuleService.saveRule(rule.getAppUniqueId(), rule, SecurityUtils.getUnionId()));
  }

  @PutMapping("/{appUniqueId}/rule")
  @PreAuthorize("@ss.hasPermi('application:application:transform:edit')")
  @Operation(summary = "保存并发布应用唯一的数据转换规则")
  public AjaxResult<IoTPushTransformRule> saveAndPublishRule(
      @PathVariable String appUniqueId, @RequestBody IoTPushTransformRule rule) {
    checkAppPermission(appUniqueId);
    return AjaxResult.success(
        pushTransformRuleService.saveAndPublishRule(
            appUniqueId, rule, SecurityUtils.getUnionId()));
  }

  @PutMapping("/rule")
  @PreAuthorize("@ss.hasPermi('application:application:transform:edit')")
  @Operation(summary = "更新数据转换规则")
  public AjaxResult<IoTPushTransformRule> updateRule(@RequestBody IoTPushTransformRule rule) {
    checkAppPermission(rule.getAppUniqueId());
    return AjaxResult.success(
        pushTransformRuleService.saveRule(rule.getAppUniqueId(), rule, SecurityUtils.getUnionId()));
  }

  @GetMapping("/rule/{id}")
  @PreAuthorize("@ss.hasPermi('application:application:transform:query')")
  @Operation(summary = "获取规则详情")
  public AjaxResult<IoTPushTransformRule> getRule(@PathVariable Long id) {
    IoTPushTransformRule rule = pushTransformRuleService.getRule(id);
    checkAppPermission(rule.getAppUniqueId());
    return AjaxResult.success(rule);
  }

  @DeleteMapping("/rule/{id}")
  @PreAuthorize("@ss.hasPermi('application:application:transform:remove')")
  @Operation(summary = "删除规则")
  public AjaxResult<Boolean> deleteRule(@PathVariable Long id) {
    IoTPushTransformRule rule = pushTransformRuleService.getRule(id);
    checkAppPermission(rule.getAppUniqueId());
    return AjaxResult.success(
        pushTransformRuleService.deleteRule(rule.getAppUniqueId(), id, SecurityUtils.getUnionId()));
  }

  @PostMapping("/rule/{id}/publish")
  @PreAuthorize("@ss.hasPermi('application:application:transform:publish')")
  @Operation(summary = "发布规则")
  public AjaxResult<IoTPushTransformRule> publishRule(@PathVariable Long id) {
    IoTPushTransformRule rule = pushTransformRuleService.getRule(id);
    checkAppPermission(rule.getAppUniqueId());
    return AjaxResult.success(
        pushTransformRuleService.publishRule(rule.getAppUniqueId(), id, SecurityUtils.getUnionId()));
  }

  @PostMapping("/preview")
  @PreAuthorize("@ss.hasPermi('application:application:transform:query')")
  @Operation(summary = "预览数据转换结果")
  public AjaxResult<PushTransformPreviewVO> preview(@RequestBody PushTransformPreviewBO previewBO) {
    checkAppPermission(previewBO.getAppUniqueId());
    return AjaxResult.success(pushTransformRuleService.preview(previewBO));
  }

  private void checkAppPermission(String appUniqueId) {
    IoTUserApplication app = iotUserApplicationService.selectIotUserApplicationById(appUniqueId);
    IoTUser user = ioTUserService.selectUserByUnionId(SecurityUtils.getUnionId());
    if (app != null && user != null && !user.isAdmin() && !app.getUnionId().equalsIgnoreCase(user.getUnionId())) {
      throw new IoTException("无权限操作");
    }
  }
}
