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
import cn.hutool.json.JSONObject;
import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.bo.RuleModelBO;
import cn.universal.persistence.entity.vo.RuleModelVO;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.rule.model.bo.RuleBo;
import cn.universal.rule.model.bo.RuleTargetTestBO;
import cn.universal.rule.service.RuleService;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
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
 * 规则管理控制器
 *
 * @since 2023/1/14 9:10
 */
@RestController
@Tag(name = "规则引擎", description = "规则引擎")
@RequestMapping("/admin/v1/")
public class RuleController extends BaseController {

  @Resource private RuleService ruleService;

  @Operation(summary = "查询规则列表", description = "分页查询规则列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取规则列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("rule/list")
  public TableDataInfo<RuleModelVO> pageList(RuleModelBO ruleModelBo) {
    IoTUser tenantOwner = currentTenantOwner();
    ruleModelBo.setCreatorId(tenantOwner.getUnionId());
    ruleModelBo.setTenantId(currentTenantId(tenantOwner));
    startPage();
    List<RuleModelVO> userList = ruleService.queryRuleListByBo(ruleModelBo);
    return getDataTable(userList);
  }

  @Operation(summary = "查询规则", description = "根据规则ID查询规则详情")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取规则详情",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping("rule")
  public AjaxResult<RuleModelVO> detail(@RequestParam @Parameter(description = "规则id") Long id) {
    IoTUser tenantOwner = currentTenantOwner();
    RuleModelVO ruleModel =
        ruleService.queryVoByIdAndCreator(id, tenantOwner.getUnionId(), currentTenantId(tenantOwner));
    return AjaxResult.success(ruleModel);
  }

  @Operation(summary = "新增规则", description = "新增规则信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功新增规则",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping("/rule")
  @Log(title = "新增规则", businessType = BusinessType.INSERT)
  public AjaxResult<Long> addRule(@RequestBody RuleBo ruleModelBo) {
    IoTUser tenantOwner = currentTenantOwner();
    ruleModelBo.setCreatorId(tenantOwner.getUnionId());
    ruleModelBo.setTenantId(currentTenantId(tenantOwner));
    Long ruleId = ruleService.addRule(ruleModelBo);
    return AjaxResult.success(ruleId);
  }

  @Operation(summary = "删除规则", description = "根据规则ID删除规则")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除规则",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @DeleteMapping("/rule/{id}")
  @Log(title = "删除规则", businessType = BusinessType.DELETE)
  public AjaxResult<Void> deleteRule(@PathVariable @Parameter(description = "规则id") Long id) {
    IoTUser tenantOwner = currentTenantOwner();
    ruleService.deletedRule(id, tenantOwner.getUnionId(), currentTenantId(tenantOwner));
    return AjaxResult.success();
  }

  @Operation(summary = "更新规则", description = "更新规则信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功更新规则",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping("/rule")
  @Log(title = "更新规则", businessType = BusinessType.UPDATE)
  public AjaxResult<Void> updateRule(@RequestBody RuleBo ruleModelBo) {
    IoTUser tenantOwner = currentTenantOwner();
    ruleModelBo.setCreatorId(tenantOwner.getUnionId());
    ruleModelBo.setTenantId(currentTenantId(tenantOwner));
    ruleService.updateRule(ruleModelBo);
    return AjaxResult.success();
  }

  @Operation(summary = "更新规则调用目标", description = "更新规则的调用目标配置")
  @ApiResponse(
      responseCode = "200",
      description = "成功更新规则调用目标",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping("/rule/targets")
  @Log(title = "更新规则调用目标", businessType = BusinessType.UPDATE)
  public AjaxResult<Void> updateRuleTargets(@RequestBody RuleBo ruleModelBo) {
    IoTUser tenantOwner = currentTenantOwner();
    ruleModelBo.setCreatorId(tenantOwner.getUnionId());
    ruleModelBo.setTenantId(currentTenantId(tenantOwner));
    ruleService.updateRuleTargets(ruleModelBo);
    return AjaxResult.success();
  }

  @Operation(summary = "更新规则状态", description = "更新规则的启用/禁用状态")
  @ApiResponse(
      responseCode = "200",
      description = "成功更新规则状态",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping("rule/status")
  @Log(title = "更新规则状态", businessType = BusinessType.UPDATE)
  public AjaxResult<Void> changeStatus(@RequestBody RuleModelBO ruleModelBo) {
    IoTUser tenantOwner = currentTenantOwner();
    ruleModelBo.setCreatorId(tenantOwner.getUnionId());
    ruleModelBo.setTenantId(currentTenantId(tenantOwner));
    ruleService.changeStatus(ruleModelBo);
    return AjaxResult.success();
  }

  @Operation(summary = "sql测试", description = "测试规则SQL语句执行")
  @ApiResponse(
      responseCode = "200",
      description = "成功执行SQL测试",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping("/rule/sql/test")
  @Log(title = "sql测试", businessType = BusinessType.OTHER)
  public AjaxResult<JSONObject> sqlTest(@RequestBody RuleBo sqlBo) {
    return AjaxResult.success(ruleService.testExecuteRule(sqlBo));
  }

  @Operation(summary = "调用目标测试", description = "测试规则调用目标执行")
  @ApiResponse(
      responseCode = "200",
      description = "成功执行调用目标测试",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping("/rule/target/test")
  @Log(title = "调用目标测试", businessType = BusinessType.OTHER)
  public AjaxResult<Object> targetTest(@RequestBody RuleTargetTestBO testBo) {
    Object result = ruleService.testRuleTarget(testBo);
    return AjaxResult.success(result);
  }

  private IoTUser currentTenantOwner() {
    return loginIoTUnionUser(SecurityUtils.getUnionId());
  }

  private String currentTenantId(IoTUser tenantOwner) {
    return StrUtil.blankToDefault(SecurityUtils.getTenantIdQuietly(), tenantOwner.getUnionId());
  }
}
