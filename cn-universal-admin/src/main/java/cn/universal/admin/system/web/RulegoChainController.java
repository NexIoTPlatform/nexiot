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

package cn.universal.admin.system.web;

import cn.hutool.core.util.StrUtil;
import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.bo.RulegoChainBO;
import cn.universal.persistence.entity.vo.RulegoChainVO;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.rule.rulego.service.RulegoChainService;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
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
 * rulego规则链管理控制器
 *
 * @author gitee.com/NexIoT
 * @since 2025/01/15
 */
@RestController
@Tag(name = "rulego规则链管理", description = "rulego规则链管理")
@RequestMapping("/admin/v1/rulego")
public class RulegoChainController extends BaseController {

  @Resource private RulegoChainService rulegoChainService;

  @Operation(summary = "查询规则链列表", description = "分页查询规则链列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取规则链列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/chain/list")
  public TableDataInfo<RulegoChainVO> pageList(RulegoChainBO bo) {
    IoTUser tenantOwner = currentTenantOwner();
    bo.setCreatorId(tenantOwner.getUnionId());
    bo.setTenantId(currentTenantId(tenantOwner));
    startPage();
    return getDataTable(rulegoChainService.queryRulegoChainList(bo));
  }

  @Operation(summary = "查询规则链详情", description = "根据规则链ID查询规则链详情")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取规则链详情",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping("/chain/{id}")
  public AjaxResult<RulegoChainVO> detail(@PathVariable @Parameter(description = "规则链ID") Long id) {
    IoTUser tenantOwner = currentTenantOwner();
    RulegoChainVO rulegoChain =
        rulegoChainService.queryRulegoChainById(
            id, tenantOwner.getUnionId(), currentTenantId(tenantOwner));
    return AjaxResult.success(rulegoChain);
  }

  @Operation(summary = "根据rulegoId查询规则链详情", description = "根据rulego规则链ID查询规则链详情")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取规则链详情",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping("/chain/detail")
  public AjaxResult<RulegoChainVO> detailByRulegoId(
      @RequestParam @Parameter(description = "rulego规则链ID") String rulegoId) {
    IoTUser tenantOwner = currentTenantOwner();
    RulegoChainVO rulegoChain =
        rulegoChainService.queryRulegoChainByRulegoId(
            rulegoId, tenantOwner.getUnionId(), currentTenantId(tenantOwner));
    return AjaxResult.success(rulegoChain);
  }

  @Operation(summary = "新增规则链", description = "新增rulego规则链信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功新增规则链",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping("/chain")
  @Log(title = "新增rulego规则链", businessType = BusinessType.INSERT)
  public AjaxResult<Long> addRulegoChain(@RequestBody RulegoChainBO bo) {
    IoTUser tenantOwner = currentTenantOwner();
    bo.setCreatorId(tenantOwner.getUnionId());
    bo.setTenantId(currentTenantId(tenantOwner));
    bo.setCreatorName(tenantOwner.getUsername());
    Long chainId = rulegoChainService.saveRulegoChain(bo);
    return AjaxResult.success(chainId);
  }

  @Operation(summary = "更新规则链", description = "更新rulego规则链信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功更新规则链",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping("/chain")
  @Log(title = "更新rulego规则链", businessType = BusinessType.UPDATE)
  public AjaxResult<Void> updateRulegoChain(@RequestBody RulegoChainBO bo) {
    IoTUser tenantOwner = currentTenantOwner();
    bo.setCreatorId(tenantOwner.getUnionId());
    bo.setTenantId(currentTenantId(tenantOwner));
    bo.setCreatorName(tenantOwner.getUsername());
    boolean result = rulegoChainService.updateRulegoChain(bo);
    return result ? AjaxResult.success() : AjaxResult.error("更新失败");
  }

  @Operation(summary = "删除规则链", description = "根据规则链ID删除规则链")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除规则链",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @DeleteMapping("/chain/{id}")
  @Log(title = "删除rulego规则链", businessType = BusinessType.DELETE)
  public AjaxResult<Void> deleteRulegoChain(
      @PathVariable @Parameter(description = "规则链ID") Long id) {
    IoTUser tenantOwner = currentTenantOwner();
    boolean result =
        rulegoChainService.deleteRulegoChain(
            id, tenantOwner.getUnionId(), currentTenantId(tenantOwner));
    return result ? AjaxResult.success() : AjaxResult.error("删除失败");
  }

  @Operation(summary = "部署规则链", description = "部署rulego规则链")
  @ApiResponse(
      responseCode = "200",
      description = "成功部署规则链",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping("/chain/{id}/deploy")
  @Log(title = "部署rulego规则链", businessType = BusinessType.UPDATE)
  public AjaxResult<Void> deployRulegoChain(
      @PathVariable @Parameter(description = "规则链ID") Long id) {
    IoTUser tenantOwner = currentTenantOwner();
    boolean result =
        rulegoChainService.deployRulegoChain(
            id, tenantOwner.getUnionId(), currentTenantId(tenantOwner));
    return result ? AjaxResult.success() : AjaxResult.error("部署失败");
  }

  @Operation(summary = "停止规则链", description = "停止rulego规则链")
  @ApiResponse(
      responseCode = "200",
      description = "成功停止规则链",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping("/chain/{id}/stop")
  @Log(title = "停止rulego规则链", businessType = BusinessType.UPDATE)
  public AjaxResult<Void> stopRulegoChain(@PathVariable @Parameter(description = "规则链ID") Long id) {
    IoTUser tenantOwner = currentTenantOwner();
    boolean result =
        rulegoChainService.stopRulegoChain(
            id, tenantOwner.getUnionId(), currentTenantId(tenantOwner));
    return result ? AjaxResult.success() : AjaxResult.error("停止失败");
  }

  @Operation(summary = "同步规则链DSL", description = "同步rulego规则链DSL配置")
  @ApiResponse(
      responseCode = "200",
      description = "成功同步规则链DSL",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping("/chain/{id}/sync")
  @Log(title = "同步rulego规则链DSL", businessType = BusinessType.UPDATE)
  public AjaxResult<Void> syncRulegoChainDsl(
      @PathVariable @Parameter(description = "规则链ID") Long id) {
    IoTUser tenantOwner = currentTenantOwner();
    RulegoChainVO rulegoChain =
        rulegoChainService.queryRulegoChainById(
            id, tenantOwner.getUnionId(), currentTenantId(tenantOwner));
    boolean result = rulegoChainService.syncRulegoChainDsl(rulegoChain.getRulegoId());
    return result ? AjaxResult.success() : AjaxResult.error("同步失败");
  }

  @Operation(summary = "获取设计器URL", description = "根据规则链ID获取设计器URL")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设计器URL",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping("/chain/{id}/designer")
  public AjaxResult<String> getDesignerUrl(
      @PathVariable @Parameter(description = "规则链ID") Long id) {
    IoTUser tenantOwner = currentTenantOwner();
    RulegoChainVO rulegoChain =
        rulegoChainService.queryRulegoChainById(
            id, tenantOwner.getUnionId(), currentTenantId(tenantOwner));
    String designerUrl = rulegoChainService.getDesignerUrl(rulegoChain.getRulegoId());
    AjaxResult<String> result = new AjaxResult<>();
    result.setCode(0);
    result.setMsg("success");
    result.setData(designerUrl);
    return result;
  }

  private IoTUser currentTenantOwner() {
    return loginIoTUnionUser(SecurityUtils.getUnionId());
  }

  private String currentTenantId(IoTUser tenantOwner) {
    return StrUtil.blankToDefault(SecurityUtils.getTenantIdQuietly(), tenantOwner.getUnionId());
  }
}
