/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.controller.api;

import cn.universal.miniapp.entity.MpApp;
import cn.universal.miniapp.service.MpAppService;
import cn.universal.persistence.query.AjaxResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序API-应用配置接口
 *
 * @author NexIoT
 * @since 2025/12/17
 */
@Slf4j
@RestController
@RequestMapping("/mp/api/app")
@Tag(name = "小程序-应用配置接口", description = "小程序API-应用配置接口")
public class MpAppApiController {

  @Autowired private MpAppService mpAppService;

  /**
   * 获取小程序配置
   *
   * @param appId 应用ID
   * @return 小程序配置信息
   */
  @GetMapping("/config")
  @Operation(summary = "获取小程序配置", description = "根据应用ID获取小程序配置信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取小程序配置",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "appId", description = "应用ID", required = true)
  public AjaxResult<Object> getAppConfig(@RequestParam String appId) {
    log.info("[小程序配置] 获取配置 appId={}", appId);

    MpApp mpApp = mpAppService.getAppByAppId(appId);
    if (mpApp == null) {
      log.warn("[小程序配置] appId不存在: {}", appId);
      return AjaxResult.error("小程序不存在", null);
    }

    Map<String, Object> config = new HashMap<>();
    config.put("name", mpApp.getName());
    config.put("logo", mpApp.getLogo());
    config.put("description", mpApp.getDescription());
    config.put("primaryColor", mpApp.getPrimaryColor());
    config.put("welcomeText", mpApp.getWelcomeText());
    config.put("appId", mpApp.getAppId());

    log.info("[小程序配置] 返回配置: name={}, primaryColor={}", mpApp.getName(), mpApp.getPrimaryColor());

    return AjaxResult.success(config);
  }
}
