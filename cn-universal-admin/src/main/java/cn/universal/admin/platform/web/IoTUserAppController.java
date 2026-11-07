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

package cn.universal.admin.platform.web;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import cn.universal.admin.common.utils.ExcelUtil;
import cn.universal.admin.platform.service.IIoTDeviceService;
import cn.universal.admin.platform.service.push.ApplicationPushConnectionTestService;
import cn.universal.admin.system.service.IIoTUserApplicationService;
import cn.universal.admin.system.service.IOAuthClientDetailsService;
import cn.universal.common.annotation.Log;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.entity.OAuth2ClientDetails;
import cn.universal.persistence.entity.vo.IoTDeviceCountVO;
import cn.universal.persistence.entity.vo.IoTUserApplicationVO;
import cn.universal.persistence.mapper.IoTUserApplicationMapper;
import cn.universal.persistence.page.TableDataInfo;
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
import jakarta.servlet.http.HttpServletResponse;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户应用信息管理控制器
 *
 * @since 2025-12-30
 */
@RestController
@Tag(name = "用户应用信息管理", description = "用户应用信息管理接口，包括应用的增删改查、推送配置等")
@RequestMapping("/admin/v1/apply")
@Slf4j
public class IoTUserAppController extends BaseController {

  @Autowired private IIoTUserApplicationService iotUserApplicationService;
  @Resource private ApplicationPushConnectionTestService pushConnectionTestService;
  @Resource private IoTUserApplicationMapper iotUserApplicationMapper;
  @Resource private IOAuthClientDetailsService oauthClientDetailsService;
  @Resource private IIoTDeviceService iIotDeviceService;
  @Resource private IoTUserService ioTUserService;

  /** 查询用户应用信息列表 */
  @Operation(summary = "查询用户应用信息列表", description = "根据查询条件获取用户应用信息列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取用户应用信息列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/list")
  public TableDataInfo list(IoTUserApplication iotUserApplication) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    startPage();
    List<IoTUserApplicationVO> list =
        iotUserApplicationService.selectIotUserApplicationList(iotUserApplication, iotUser);
    List<IoTDeviceCountVO> map = iIotDeviceService.countDevByApplication(null);
    Map<String, String> map1 =
        map.stream()
            .collect(
                Collectors.toMap(IoTDeviceCountVO::getApplication, IoTDeviceCountVO::getDevNum));
    list.forEach(
        applicationVo -> {
          applicationVo.setDevNum(
              map1.get(applicationVo.getAppUniqueId()) != null
                  ? Integer.parseInt(map1.get(applicationVo.getAppUniqueId()))
                  : 0);
        });
    // 按设备数量降序排序
    List<IoTUserApplicationVO> sort =
        CollectionUtil.sort(list, Comparator.comparing(IoTUserApplicationVO::getDevNum).reversed());
    return getDataTable(sort);
  }

  /** 导出用户应用信息列表 */
  @Operation(summary = "导出用户应用信息列表")
  @PostMapping("/export")
  @Log(title = "导出用户应用信息列表", businessType = BusinessType.EXPORT)
  public void export(HttpServletResponse response, IoTUserApplication iotUserApplication) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    iotUserApplication.setUnionId(
        ioTUserService.selectUserByUnionId(SecurityUtils.getUnionId()).isAdmin()
            ? null
            : SecurityUtils.getUnionId());
    List<IoTUserApplicationVO> list =
        iotUserApplicationService.selectIotUserApplicationList(iotUserApplication, iotUser);
    List<IoTDeviceCountVO> map = iIotDeviceService.countDevByApplication(null);
    Map<String, String> map1 =
        map.stream()
            .collect(
                Collectors.toMap(IoTDeviceCountVO::getApplication, IoTDeviceCountVO::getDevNum));
    list.forEach(
        applicationVo -> {
          applicationVo.setDevNum(
              map1.get(applicationVo.getAppUniqueId()) != null
                  ? Integer.parseInt(map1.get(applicationVo.getAppUniqueId()))
                  : 0);
        });
    ExcelUtil<IoTUserApplicationVO> util =
        new ExcelUtil<IoTUserApplicationVO>(IoTUserApplicationVO.class);
    util.exportExcel(response, list, "用户应用信息数据");
  }

  /** 获取用户应用信息详细信息 */
  @Operation(summary = "启用推送")
  @PostMapping(value = "/{appUniqueId}/{pushType}/{enable}")
  public AjaxResult<Void> enableOrDisablePush(
      @PathVariable("appUniqueId") String appUniqueId,
      @PathVariable("pushType") String pushType,
      @PathVariable("enable") boolean enable) {
    iotUserApplicationService.selectIotUserApplicationById(appUniqueId);
    boolean flag = iotUserApplicationService.enableOrDisablePushCfg(appUniqueId, pushType, enable);
    if (flag) {
      return AjaxResult.success();
    }
    return AjaxResult.error("失败");
  }

  @Operation(summary = "测试应用推送目标连接")
  @PostMapping(value = "/{appUniqueId}/push/{pushType}/test")
  public AjaxResult<Void> testPushConnection(
      @PathVariable String appUniqueId,
      @PathVariable String pushType,
      @RequestBody JSONObject config) {
    iotUserApplicationService.selectIotUserApplicationById(appUniqueId);
    pushConnectionTestService.test(pushType, config);
    return AjaxResult.success();
  }

  /** 获取用户应用信息详细信息 */
  @Operation(summary = "获取用户应用信息详细信息", description = "根据应用唯一ID获取用户应用详细信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取用户应用详细信息",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "appUniqueId", description = "应用唯一ID", required = true)
  @GetMapping(value = "/{appUniqueId}")
  public AjaxResult<IoTUserApplication> getInfo(@PathVariable("appUniqueId") String appUniqueId) {
    return AjaxResult.success(iotUserApplicationService.selectIotUserApplicationById(appUniqueId));
  }

  /** 新增用户应用信息 */
  @Operation(summary = "新增用户应用信息", description = "新增用户应用信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功新增用户应用信息",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping
  @Transactional
  @Log(title = "新增用户应用信息", businessType = BusinessType.INSERT)
  public AjaxResult<Void> add(@RequestBody IoTUserApplication iotUserApplication) {
    rejectSuperAdminCreate("应用");
    String appId = RandomUtil.randomString(16);
    // 检查appid是否重复
    while (!CollectionUtils.isEmpty(
        iotUserApplicationService.selectIotUserApplicationList(
            IoTUserApplication.builder().appId(appId).build(),
            IoTUser.builder().identity(0).build()))) {
      appId = RandomUtil.randomString(16);
    }
    iotUserApplication.setAppUniqueId(String.valueOf(IdUtil.getSnowflake().nextId()));
    iotUserApplication.setAppId(appId);
    iotUserApplication.setAppSecret(RandomUtil.randomString(32));
    iotUserApplication.setCreateDate(new Date());
    iotUserApplication.setUnionId(SecurityUtils.getUnionId());
    if (iotUserApplicationService.insertIotAppUser(iotUserApplication) == 0) {
      throw new IoTException("新增应用失败！");
    }
    if (oauthClientDetailsService.insert(
            OAuth2ClientDetails.builder()
                .clientId(iotUserApplication.getAppId())
                .clientSecret(new BCryptPasswordEncoder().encode(iotUserApplication.getAppSecret()))
                .scope("all")
                .AuthorizedGrantTypes("client_credentials")
                .webServerRedirectUri("http://localhost:9091/client-authcode/login")
                .accessTokenValidity(7200)
                .refreshTokenValidity(36000)
                .autoapprove("1")
                .iotUnionId(iotUserApplication.getUnionId())
                .tenantId(iotUserApplication.getTenantId())
                .build())
        == 0) {
      throw new IoTException("认证信息新增失败！");
    }
    return AjaxResult.success();
  }

  /** 重置密钥 */
  @Operation(summary = "重置密钥")
  @PutMapping(value = "/reset")
  @Transactional
  @Log(title = "重置密钥", businessType = BusinessType.UPDATE)
  public AjaxResult<Void> resetSecret(@RequestBody IoTUserApplication iotUserApplication) {
    IoTUser user = loginIoTUnionUser(SecurityUtils.getUnionId());
    iotUserApplicationService.selectIotUserApplicationById(iotUserApplication.getAppUniqueId());
    // 调用service层方法完成密钥重置和MQTT配置同步
    if (!iotUserApplicationService.resetAppSecretAndSyncMqtt(
        iotUserApplication.getAppUniqueId(), user.getUnionId())) {
      throw new IoTException("应用密钥重置失败");
    }
    return AjaxResult.success();
  }

  /** 修改用户应用信息 */
  @Operation(summary = "修改用户应用信息", description = "修改用户应用信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改用户应用信息",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping
  @Transactional
  @Log(title = "修改用户应用信息", businessType = BusinessType.UPDATE)
  public AjaxResult<Map<String, String>> edit(@RequestBody IoTUserApplication iotUserApplication) {
    log.info(
        "修改用户应用信息,操作人={},应用={}",
        SecurityUtils.getUnionId(),
        iotUserApplication.getAppUniqueId());
    IoTUserApplication app =
        iotUserApplicationService.selectIotUserApplicationById(iotUserApplication.getAppUniqueId());
    Map<String, String> map = new HashMap<>();
    map.put("message", "success");
    iotUserApplication.setUnionId(app.getUnionId());
    iotUserApplication.setTenantId(app.getTenantId());
    iotUserApplicationService.updateIotUserApplication(iotUserApplication);
    return AjaxResult.success(map);
  }

  /** 删除用户应用信息 */
  @Operation(summary = "删除用户应用信息", description = "根据应用唯一ID数组删除用户应用信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除用户应用信息",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "appUniqueId", description = "应用唯一ID数组", required = true)
  @DeleteMapping("/{appUniqueId}")
  @Transactional
  @Log(title = "删除用户应用信息", businessType = BusinessType.DELETE)
  public AjaxResult<Void> remove(@PathVariable String[] appUniqueId) {
    log.info("删除用户应用信息,操作人={},参数={}", SecurityUtils.getUnionId(), JSONUtil.toJsonStr(appUniqueId));

    if (iIotDeviceService.selectDevByAppUniqueId(appUniqueId) > 0) {
      throw new IoTException("该应用下有绑定设备，禁止删除！");
    }
    for (String id : appUniqueId) {
      iotUserApplicationService.selectIotUserApplicationById(id);
    }
    List<IoTUserApplication> applications =
        iotUserApplicationService.selectIotUserApplicationByIds(appUniqueId);
    if (oauthClientDetailsService.deleteByClientIds(
            applications.stream().map(IoTUserApplication::getAppId).toArray(String[]::new))
        == 0) {
      throw new IoTException("应用对应授权信息删除失败！");
    }
    if (iotUserApplicationService.deleteIotUserApplicationByIds(appUniqueId) == 0) {
      throw new IoTException("应用删除失败！");
    }
    return AjaxResult.success();
  }

  @Operation(summary = "推送地址测试", description = "测试推送地址是否可用")
  @ApiResponse(
      responseCode = "200",
      description = "成功测试推送地址",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping(value = "/check")
  public AjaxResult<Void> checkUrl(IoTUserApplication iotUserApplication) {
    String url = iotUserApplication.getNotifyUrl();
    BaseUPRequest downRequest = new BaseUPRequest();
    downRequest.setIotId("1111111111");
    downRequest.setDeviceName("测试");
    downRequest.setDeviceId("11111111");
    downRequest.setDebug(true);
    downRequest.setTime(new Date().getTime());
    try {
      HttpRequest request = HttpUtil.createPost(url);
      request.timeout(3000);
      String timestamp = String.valueOf(System.currentTimeMillis());
      String signature = String.valueOf((timestamp + url).hashCode());
      request.header("X-Timestamp", timestamp);
      request.header("X-Signature", signature);
      request.header(Header.CONTENT_TYPE, "application/json");
      request.body(JSONUtil.toJsonStr(downRequest));
      HttpResponse postBody = request.execute();
      if (postBody == null) {
        throw new IoTException("该地址无法正常访问!");
      }
      if (postBody.getStatus() != 200) {
        throw new IoTException("该地址无法正常访问! 错误码：" + postBody.getStatus());
      }
      return AjaxResult.success("200");
    } catch (Exception e) {
      return AjaxResult.success(e.getMessage());
    }
  }

  /** HTTP推送启/停用 */
  @Operation(summary = "HTTP推送启/停用", description = "启用或停用HTTP推送功能")
  @ApiResponse(
      responseCode = "200",
      description = "成功设置HTTP推送状态",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping(value = "/enable")
  public AjaxResult<Void> HttpEnable(@RequestBody IoTUserApplication iotUserApplication) {
    iotUserApplicationService.selectIotUserApplicationById(iotUserApplication.getAppUniqueId());
    boolean enabled = Integer.valueOf(IoTConstant.ONE_TRUE).equals(iotUserApplication.getHttpEnable());
    if (!iotUserApplicationService.enableOrDisablePushCfg(
        iotUserApplication.getAppUniqueId(), "http", enabled)) {
      return AjaxResult.error("HTTP推送状态更新失败");
    }
    return AjaxResult.success();
  }

  @Resource
  private cn.universal.admin.platform.service.push.ProductChangePushService productChangePushService;

  /** 获取产品订阅配置 */
  @Operation(summary = "获取产品订阅配置", description = "获取应用的产品变更订阅配置信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取产品订阅配置",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping(value = "/subscription/{appUniqueId}")
  public AjaxResult<Object> getProductSubscription(
      @PathVariable @Parameter(description = "应用唯一ID") String appUniqueId) {
    iotUserApplicationService.selectIotUserApplicationById(appUniqueId);
    return AjaxResult.success(productChangePushService.getProductSubscription(appUniqueId));
  }

  /** 更新产品订阅配置 */
  @Operation(summary = "更新产品订阅配置", description = "更新应用的产品变更订阅配置（复用HTTP推送地址）")
  @ApiResponse(
      responseCode = "200",
      description = "成功更新产品订阅配置",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping(value = "/subscription/{appUniqueId}")
  public AjaxResult<Boolean> updateProductSubscription(
      @PathVariable @Parameter(description = "应用唯一ID") String appUniqueId,
      @RequestBody @Parameter(description = "订阅配置") Map<String, Object> config) {
    iotUserApplicationService.selectIotUserApplicationById(appUniqueId);

    boolean enabled = Boolean.TRUE.equals(config.get("enabled"));

    boolean result = productChangePushService.updateProductSubscription(appUniqueId, enabled);
    return AjaxResult.success(result);
  }
}
