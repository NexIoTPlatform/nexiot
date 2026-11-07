/*
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 */
package cn.universal.miniapp.controller.api;

import cn.universal.miniapp.dto.MpPasswordLoginDTO;
import cn.universal.miniapp.dto.MpWxLoginDTO;
import cn.universal.miniapp.entity.MpUser;
import cn.universal.miniapp.security.MpTokenService;
import cn.universal.miniapp.service.MpBindQrcodeService;
import cn.universal.miniapp.service.MpUserService;
import cn.universal.miniapp.vo.MpUserInfoVO;
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
 * 小程序API-认证接口
 *
 * @author NexIoT
 * @since 2025/12/15
 */
@Slf4j
@RestController
@RequestMapping("/mp/api/auth")
@Tag(name = "小程序-认证接口", description = "小程序API-认证接口")
public class MpAuthApiController {

  @Autowired private MpBindQrcodeService mpBindQrcodeService;

  @Autowired private MpUserService mpUserService;

  @Autowired private MpTokenService mpTokenService;

  /** 账号密码登录 */
  @PostMapping("/login")
  @Operation(summary = "账号密码登录", description = "使用账号密码进行登录")
  @ApiResponse(
      responseCode = "200",
      description = "成功登录",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  public AjaxResult<Map<String, Object>> loginByPassword(@RequestBody MpPasswordLoginDTO dto) {
    MpUserInfoVO userInfo =
        mpUserService.loginByPassword(dto.getAppId(), dto.getUsername(), dto.getPassword());

    // 查询用户完整信息生成Token
    MpUser user = mpUserService.getUserById(userInfo.getUserId());
    String token = mpTokenService.generateToken(user);

    Map<String, Object> data = new HashMap<>();
    data.put("token", token);
    data.put("userInfo", userInfo);

    return AjaxResult.success(data);
  }

  /** 微信登录（扫码绑定） */
  @PostMapping("/wxLogin")
  @Operation(summary = "微信登录", description = "使用微信进行登录（扫码绑定）")
  @ApiResponse(
      responseCode = "200",
      description = "成功登录",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  public Map<String, Object> wxLogin(@RequestBody MpWxLoginDTO dto) {
    // 这里需要调用微信接口获取openId
    // 简化示例，实际需要使用code换取openId
    String openId = dto.getOpenId();
    String unionId = dto.getUnionId();
    String code = dto.getBindCode();

    MpUserInfoVO userInfo = mpBindQrcodeService.scanAndBind(code, openId, unionId);

    Map<String, Object> result = new HashMap<>();
    result.put("code", 200);
    result.put("data", userInfo);
    return result;
  }

  /** 生成绑定二维码 */
  @PostMapping("/qrcode/generate")
  @Operation(summary = "生成绑定二维码", description = "生成用于绑定的二维码")
  @ApiResponse(
      responseCode = "200",
      description = "成功生成二维码",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
  @Parameter(name = "appId", description = "应用ID", required = true)
  @Parameter(name = "targetUserId", description = "目标用户ID", required = false)
  public Map<String, Object> generateQrcode(
      @RequestParam String appId, @RequestParam(required = false) Long targetUserId) {
    String qrcodeCode = mpBindQrcodeService.generateQrcode(appId, targetUserId);

    Map<String, Object> result = new HashMap<>();
    result.put("code", 200);
    result.put("data", Map.of("qrcodeCode", qrcodeCode, "expireMinutes", 5));
    return result;
  }
}
