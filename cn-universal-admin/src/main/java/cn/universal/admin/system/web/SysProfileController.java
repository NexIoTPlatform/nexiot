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

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.PhoneUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.system.service.ISysRoleService;
import cn.universal.admin.system.service.UserWebSocketPushSettingService;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.constant.IoTUserConstants;
import cn.universal.common.exception.IoTException;
import cn.universal.common.utils.FileUploadUtils;
import cn.universal.common.utils.MimeTypeUtils;
import cn.universal.ossm.entity.SysOss;
import cn.universal.ossm.service.ISysOssService;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.bo.IoTUserBO;
import cn.universal.persistence.entity.bo.PasswordBO;
import cn.universal.persistence.entity.bo.WebSocketPushProfileBO;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import cn.universal.security.service.IoTUserService;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import javax.imageio.ImageIO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 个人信息 */
@RestController
@RequestMapping("/admin/system/user/profile")
@Tag(name = "用户个人信息管理", description = "用户个人信息管理接口")
public class SysProfileController extends BaseController {

  @Resource private IoTUserService ioTUserService;
  @Resource private ISysRoleService iSysRoleService;
  @Resource private UserWebSocketPushSettingService userWebSocketPushSettingService;
  @Resource private ISysOssService sysOssService;

  /** 查询个人信息 */
  @Operation(summary = "查询个人信息", description = "获取当前用户的个人信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取个人信息",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping
  public AjaxResult<JSONObject> profile() {
    JSONObject jsonObject = new JSONObject();
    String unionId = SecurityUtils.getUnionId();

    IoTUser iotUser = ioTUserService.selectUserByUnionId(unionId);
    // 手机号脱敏
    if (StrUtil.isNotEmpty(iotUser.getMobile())) {
      iotUser.setMobile(PhoneUtil.hideBetween(iotUser.getMobile()).toString());
    }
    jsonObject.set("user", iotUser);
    jsonObject.set("roleGroup", iSysRoleService.selectRolesByUnionId(unionId));

    return AjaxResult.success(jsonObject);
  }

  /** 个人信息 */
  @Operation(summary = "修改个人信息", description = "修改当前用户的个人信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改个人信息",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping
  public AjaxResult<Void> profile(@RequestBody IoTUserBO iotUserBo) {
    String unionId = SecurityUtils.getUnionId();
    IoTUser iotUser = ioTUserService.selectUserByUnionId(unionId);
    if (iotUser == null) {
      throw new IoTException("当前用户不存在");
    }
    String mobile = iotUserBo.getMobile();
    if (StrUtil.contains(mobile, '*')) {
      String maskedMobile =
          StrUtil.isBlank(iotUser.getMobile())
              ? StrUtil.EMPTY
              : PhoneUtil.hideBetween(iotUser.getMobile()).toString();
      if (!StrUtil.equals(maskedMobile, mobile)) {
        return error("修改个人信息失败，脱敏手机号码不匹配");
      }
      mobile = iotUser.getMobile();
    }
    if (Validator.isEmpty(mobile)) {
      return error("修改个人信息失败，手机号码不能为空");
    }
    if (!mobile.matches("^1[3-9]\\d{9}$")) {
      return error("修改个人信息失败，手机号码格式不正确");
    }
    IoTUser user = BeanUtil.toBean(iotUserBo, IoTUser.class);
    user.setId(iotUser.getId());
    user.setMobile(mobile);
    if (IoTUserConstants.NOT_UNIQUE.equals(ioTUserService.checkPhoneUnique(user))) {
      return error("修改个人信息失败，手机号码已存在");
    }
    iotUser.setAlias(iotUserBo.getAlias());
    iotUser.setEmail(iotUserBo.getEmail());
    iotUser.setMobile(mobile);
    iotUser.setUpdateBy(unionId);
    iotUser.setUpdateDate(new Date());
    if (JSONUtil.isTypeJSON(iotUserBo.getCfg())) {
      JSONObject merged =
          userWebSocketPushSettingService.mergeProfileCfg(iotUser.getCfg(), iotUserBo.getCfg());
      iotUser.setCfg(JSONUtil.toJsonStr(merged));
    }
    ioTUserService.updateUserById(iotUser);
    return AjaxResult.success();
  }

  /** 上传并更新当前用户头像。 */
  @PostMapping("/avatar")
  public AjaxResult<Map<String, String>> avatar(
      @RequestPart("avatarfile") MultipartFile file) throws Exception {
    if (file == null || file.isEmpty()) {
      return AjaxResult.error("头像文件不能为空", Map.of());
    }
    if (file.getSize() > 5 * 1024 * 1024) {
      return AjaxResult.error("头像文件不能超过5MB", Map.of());
    }
    FileUploadUtils.assertAllowed(file, MimeTypeUtils.IMAGE_EXTENSION);
    try (InputStream inputStream = file.getInputStream()) {
      if (ImageIO.read(inputStream) == null) {
        return AjaxResult.error("头像文件内容不是有效图片", Map.of());
      }
    }
    String unionId = SecurityUtils.getUnionId();
    IoTUser iotUser = ioTUserService.selectUserByUnionId(unionId);
    if (iotUser == null) {
      throw new IoTException("当前用户不存在");
    }
    SysOss oss = sysOssService.upload(file, unionId, iotUser.getTenantId());
    iotUser.setAvatar(oss.getUrl());
    iotUser.setUpdateBy(unionId);
    iotUser.setUpdateDate(new Date());
    ioTUserService.updateUserById(iotUser);
    return AjaxResult.success(Map.of("imgUrl", oss.getUrl()));
  }

  @GetMapping("/websocketPush")
  public AjaxResult<WebSocketPushProfileBO> getWebSocketPushProfile() {
    return AjaxResult.success(
        userWebSocketPushSettingService.getProfile(SecurityUtils.getUnionId()));
  }

  @PutMapping("/websocketPush")
  public AjaxResult<WebSocketPushProfileBO> saveWebSocketPushProfile(
      @RequestBody WebSocketPushProfileBO profile) {
    return AjaxResult.success(
        userWebSocketPushSettingService.saveProfile(SecurityUtils.getUnionId(), profile));
  }

  /** 重置密码（修改密码） */
  @Operation(summary = "重置密码", description = "修改用户密码")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改密码",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "bo", description = "密码信息", required = true)
  @PutMapping("/updatePwd")
  public AjaxResult<Void> updatePwd(@RequestBody PasswordBO bo) {
    if (bo == null || StrUtil.isBlank(bo.getOldPassword()) || StrUtil.isBlank(bo.getNewPassword())) {
      return error("旧密码和新密码不能为空");
    }
    String unionId = SecurityUtils.getUnionId();
    IoTUser currentUser = ioTUserService.selectUserByUnionId(unionId);
    if (currentUser == null) {
      throw new IoTException("当前用户不存在");
    }
    IoTUser iotUser = ioTUserService.selectUserById(currentUser.getId());
    if (iotUser == null) {
      throw new IoTException("当前用户不存在");
    }
    Matcher matcher = IoTConstant.pattern.matcher(bo.getNewPassword());
    if (!matcher.matches()) {
      throw new IoTException("密码中必须包含字母、数字、特殊字符，至少8个字符，最多30个字符");
    }
    if (!SecurityUtils.matchesPassword(bo.getOldPassword(), iotUser.getPassword())) {
      throw new IoTException("旧密码错误");
    }
    if (SecurityUtils.matchesPassword(bo.getNewPassword(), iotUser.getPassword())) {
      throw new IoTException("新密码不能与旧密码相同");
    }
    iotUser.setPassword(SecurityUtils.encryptPassword(bo.getNewPassword()));
    iotUser.setUpdateBy(unionId);
    iotUser.setUpdateDate(new Date());
    ioTUserService.updateUserById(iotUser);
    return AjaxResult.success();
  }

  /** 修改手机号 */
  @Operation(summary = "修改手机号", description = "修改用户手机号码")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改手机号",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "mobile", description = "新手机号码", required = true)
  @RequestMapping("/updateMobile/{mobile}")
  public AjaxResult<Void> updatePwd(@PathVariable("mobile") String mobile) {
    String unionId = SecurityUtils.getUnionId();
    IoTUser iotUser = ioTUserService.selectUserByUnionId(unionId);
    IoTUser user = ioTUserService.selectUserByMobile(mobile);
    if (Objects.nonNull(user)) {
      throw new IoTException("手机号码已存在,请勿重复添加");
    } else {
      iotUser.setMobile(mobile);
      ioTUserService.updateUserById(iotUser);
      return AjaxResult.success();
    }
  }
}
