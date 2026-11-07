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

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.universal.admin.common.utils.ExcelUtil;
import cn.universal.admin.platform.service.IIoTDeviceService;
import cn.universal.admin.platform.service.taskcenter.IoTDeviceImportTaskService;
import cn.universal.admin.system.service.IIoTUserApplicationService;
import cn.universal.admin.system.service.IoTDeviceProtocolService;
import cn.universal.common.annotation.Log;
import cn.universal.common.domain.R;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.enums.OperatorType;
import cn.universal.common.exception.IoTException;
import cn.universal.common.poi.ExcelTemplate;
import cn.universal.core.service.IUP;
import cn.universal.core.service.IoTDownlFactory;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.persistence.dto.IoTDeviceDeletePreview;
import cn.universal.persistence.dto.IoTDeviceExportDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTDeviceShadow;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.IoTUserApplication;
import cn.universal.persistence.entity.bo.IoTDeviceBO;
import cn.universal.persistence.entity.bo.IoTDeviceOwnerBO;
import cn.universal.persistence.entity.bo.IoTGwDeviceBO;
import cn.universal.persistence.entity.bo.IoTSubDeviceGatewayBindBO;
import cn.universal.persistence.entity.vo.GwIoTDeviceVo;
import cn.universal.persistence.entity.vo.IoTDeviceCompanyVO;
import cn.universal.persistence.entity.vo.IoTDeviceModelVO;
import cn.universal.persistence.entity.vo.IoTTaskSubmitResultVO;
import cn.universal.persistence.mapper.IoTProductMapper;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 设备Controller @Author ruoyi
 *
 * @since 2025-12-24
 */
@RestController
@RequestMapping("/admin/v1/device")
@Tag(name = "IoT设备管理", description = "IoT设备管理接口")
@Slf4j
public class IoTDeviceController extends BaseController {

  @Autowired private IIoTDeviceService iIotDeviceService;

  @Resource private IoTProductDeviceService iotProductDeviceService;

  @Resource private IoTDeviceProtocolService ioTDeviceProtocolService;

  @Resource private IIoTUserApplicationService iIotUserApplicationService;

  @Resource private IoTProductMapper ioTProductMapper;
  @Resource private IoTDeviceImportTaskService ioTDeviceImportTaskService;

  /**
   * 查询设备列表
   *
   * @param ioTDevice 设备查询条件
   * @return 设备列表
   */
  @Operation(summary = "查询设备列表", description = "根据查询条件获取设备列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/list")
  public TableDataInfo list(IoTDevice ioTDevice) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    startPage();
    List<IoTDevice> list = iIotDeviceService.selectDevInstanceList(ioTDevice, iotUser);
    return getDataTable(list);
  }

  /**
   * 获取设备详细信息
   *
   * @param id 设备ID
   * @return 设备详细信息
   */
  @Operation(summary = "获取设备详细信息", description = "根据设备ID获取设备详细信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备详细信息",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "设备ID", required = true)
  @GetMapping(value = "/{id}")
  public AjaxResult getIotDeviceInfo(@PathVariable("id") String id) {
    IoTDevice ioTDevice = iIotDeviceService.selectDevInstanceById(id);
    int count = ioTDeviceProtocolService.countProtocol(ioTDevice.getProductKey());
    IoTDeviceBO ioTDeviceBO = new IoTDeviceBO();
    BeanUtil.copyProperties(ioTDevice, ioTDeviceBO);
    // 查询网关产品信息
    if (StrUtil.isNotBlank(ioTDeviceBO.getGwProductKey())) {
      IoTDevice parentDevice =
          iIotDeviceService.selectIoTDevice(
              ioTDeviceBO.getGwProductKey(), ioTDevice.getExtDeviceId());
      if (ObjectUtil.isNotNull(parentDevice)) {
        ioTDeviceBO.setGwDeviceInfo(
            parentDevice.getDeviceId() + "(" + parentDevice.getDeviceName() + ")");
      }
    }
    if (StrUtil.isNotBlank(ioTDevice.getApplication())) {
      ioTDeviceBO.setApplication(
          iIotUserApplicationService.selectApplicationName(ioTDevice.getApplication()));
    }
    ioTDeviceBO.setDevGroupName(iIotDeviceService.selectDevInstanceGroupName(ioTDevice.getIotId()));
    ioTDeviceBO.setHasProtocol(count > 0);
    return AjaxResult.success(ioTDeviceBO);
  }

  /**
   * 测试设备 Modbus 端点连通性。
   *
   * @param id 设备ID
   * @return 测试结果
   */
  @Operation(summary = "测试Modbus端点连通性", description = "仅测试设备配置中的Modbus端点是否可连接，不执行寄存器读写")
  @ApiResponse(
      responseCode = "200",
      description = "成功返回测试结果",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "id", description = "设备ID", required = true)
  @GetMapping(value = "/modbus/probe/{id}")
  public R probeModbusEndpoint(@PathVariable("id") String id) {
    return iIotDeviceService.probeModbusEndpoint(id);
  }

  /**
   * 新：按第三方平台过滤的设备列表（用于萤石/乐橙）
   *
   * @param ioTDevice 设备查询条件
   * @param thirdPlatform 第三方平台
   * @return 设备列表
   */
  @Operation(summary = "按第三方平台过滤的设备列表", description = "根据第三方平台过滤设备列表（用于萤石/乐橙）")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @Parameter(name = "thirdPlatform", description = "第三方平台", required = false)
  @GetMapping("/listByPlatform")
  public TableDataInfo listByPlatform(
      IoTDevice ioTDevice,
      @RequestParam(value = "thirdPlatform", required = false) String thirdPlatform) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    startPage();
    List<IoTDevice> list =
        iIotDeviceService.selectDevInstanceListByPlatform(ioTDevice, iotUser, thirdPlatform);
    return getDataTable(list);
  }

  /**
   * 查询视频设备列表（强制过滤 VIDEO_DEVICE，用于视频墙）
   *
   * @param keyword 关键词（设备名称/序列号模糊搜索）
   * @return 视频设备列表
   */
  @Operation(summary = "查询视频设备列表", description = "查询VIDEO_DEVICE类型的监控设备，支持设备名称和序列号模糊搜索")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取视频设备列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @Parameter(name = "keyword", description = "设备名称/序列号关键词", required = false)
  @GetMapping("/video/list")
  public TableDataInfo listVideoDevices(
      @RequestParam(value = "keyword", required = false) String keyword) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    startPage();
    List<IoTDevice> list = iIotDeviceService.selectVideoDeviceList(keyword, iotUser);
    return getDataTable(list);
  }

  /**
   * 查询分组未绑定设备列表
   *
   * @param groupId 分组ID
   * @param ioTDevice 设备查询条件
   * @return 设备列表
   */
  @Operation(summary = "查询分组未绑定设备列表", description = "根据分组ID查询未绑定的设备列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @Parameter(name = "groupId", description = "分组ID", required = true)
  @GetMapping("/list/{groupId}")
  public TableDataInfo list(@PathVariable("groupId") String groupId, IoTDevice ioTDevice) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    startPage();
    List<IoTDevice> list =
        iIotDeviceService.selectNotBindDevInstanceList(groupId, ioTDevice, iotUser);
    return getDataTable(list);
  }

  /**
   * 查询未绑定的设备列表
   *
   * @param ioTDevice 设备查询条件
   * @return 未绑定的设备列表
   */
  @Operation(summary = "查询未绑定的设备列表", description = "查询未绑定的设备列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取未绑定设备列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/unlist")
  public TableDataInfo unlist(IoTDevice ioTDevice) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    startPage();
    List<IoTDevice> list = iIotDeviceService.selectDevInstanceUnList(ioTDevice, iotUser);
    return getDataTable(list);
  }

  /**
   * 导出设备列表
   *
   * @param response HTTP响应
   * @param ioTDevice 设备查询条件
   */
  @Operation(summary = "导出设备列表", description = "导出设备列表到Excel文件(包含产品信息)")
  @ApiResponse(responseCode = "200", description = "成功导出设备列表")
  @PostMapping("/export")
  @Log(
      title = "导出设备列表",
      businessType = BusinessType.EXPORT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public void export(HttpServletResponse response, IoTDevice ioTDevice) {
    log.info(
        "[设备导出] 开始导出设备数据: deviceId={}, productKey={}, deviceName={}, 操作人={}",
        ioTDevice.getDeviceId(),
        ioTDevice.getProductKey(),
        ioTDevice.getDeviceName(),
        SecurityUtils.getUnionId());

    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    List<IoTDeviceExportDTO> list = iIotDeviceService.selectDeviceForExport(ioTDevice, iotUser);

    log.info("[设备导出] 导出数据准备完成,共{}条记录", list != null ? list.size() : 0);

    ExcelUtil<IoTDeviceExportDTO> util =
        new ExcelUtil<IoTDeviceExportDTO>(IoTDeviceExportDTO.class);
    util.exportExcel(response, list, "设备数据");

    log.info("[设备导出] 导出完成");
  }

  /**
   * 设备模板下载
   *
   * @param productKey 产品编号
   * @param response HTTP响应
   */
  @Operation(summary = "设备模板下载", description = "下载设备导入模板Excel文件")
  @ApiResponse(responseCode = "200", description = "成功下载设备模板")
  @Parameter(name = "productKey", description = "产品编号", required = true)
  @PostMapping("/import/template")
  public void importTemplate(
      @RequestParam() String productKey,
      @RequestParam(required = false) Long industrialTemplateId,
      @RequestParam(required = false) Integer industrialTemplateVersion,
      HttpServletResponse response) {
    requireCurrentTenantProduct(productKey);
    ExcelTemplate excelTemplate =
        iIotDeviceService.importTemplateExcel(productKey, industrialTemplateId != null);
    excelTemplate.exportExcel(response);
  }

  /**
   * 导入设备列表
   *
   * @param file Excel文件
   * @param productKey 产品编号
   * @param deviceId 父设备deviceId
   * @return 导入结果
   */
  @Operation(summary = "导入设备列表", description = "从Excel文件导入设备列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功导入设备列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "productKey", description = "产品编号", required = true)
  @Parameter(name = "deviceId", description = "父设备deviceId", required = false)
  @Parameter(name = "gwDeviceId", description = "网关设备deviceId，优先级高于 deviceId", required = false)
  @Parameter(name = "gwProductKey", description = "网关产品编号", required = false)
  @PostMapping("/import")
  @Log(
      title = "设备批量导入",
      businessType = BusinessType.IMPORT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult<IoTTaskSubmitResultVO> importDevice(
      MultipartFile file,
      @RequestParam() String productKey,
      @RequestParam(required = false) String deviceId,
      @RequestParam(required = false) String gwDeviceId,
      @RequestParam(required = false) String gwProductKey,
      @RequestParam(required = false) Long industrialTemplateId,
      @RequestParam(required = false) Integer industrialTemplateVersion)
      throws Exception {

    rejectSuperAdminCreate("设备");
    IoTProduct ioTProduct = requireCurrentTenantProduct(productKey);
    if (industrialTemplateId == null && industrialTemplateVersion != null) {
      return AjaxResult.error("选择工业模板版本前必须先选择工业模板", null);
    }
    if (industrialTemplateVersion != null && industrialTemplateVersion < 1) {
      return AjaxResult.error("工业模板版本必须大于0", null);
    }
    List<IoTDevice> devicesImport =
        iIotDeviceService.parseImportExcel(ioTProduct, file.getInputStream());
    // 兼容历史调用方仍传 deviceId 的场景，优先使用新网关参数。
    String resolvedGwDeviceId = StrUtil.isNotBlank(gwDeviceId) ? gwDeviceId : deviceId;
    IoTTaskSubmitResultVO result =
        ioTDeviceImportTaskService.submitImportTask(
            devicesImport,
            ioTProduct,
            file.getOriginalFilename(),
            resolvedGwDeviceId,
            gwProductKey,
            loginIoTUnionUser(SecurityUtils.getUnionId()),
            currentTenantId(),
            industrialTemplateId,
            industrialTemplateVersion);
    return AjaxResult.success("导入任务已创建", result);
  }

  private IoTProduct requireCurrentTenantProduct(String productKey) {
    String tenantId = currentTenantId();
    IoTProduct product = ioTProductMapper.getProductByProductKey(StrUtil.trim(productKey));
    if (product == null
        || StrUtil.isBlank(tenantId)
        || !Objects.equals(tenantId, product.getTenantId())) {
      throw new IoTException("没有找到产品", 400);
    }
    return product;
  }

  private String currentTenantId() {
    return StrUtil.blankToDefault(
        SecurityUtils.getTenantIdQuietly(), resolveTenantId(currentTenantUser()));
  }

  /**
   * 新增设备
   *
   * @param ioTDeviceBO 设备信息
   * @return 新增结果
   */
  @Operation(summary = "新增设备", description = "新增设备信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功新增设备",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @PostMapping
  @Log(
      title = "新增设备",
      businessType = BusinessType.INSERT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public R add(@RequestBody IoTDeviceBO ioTDeviceBO) {
    rejectSuperAdminCreate("设备");
    return iIotDeviceService.insertDevInstance(ioTDeviceBO);
  }

  /**
   * 修改设备
   *
   * @param devInstancebo 设备信息
   * @return 修改结果
   */
  @Operation(summary = "修改设备", description = "修改设备信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改设备",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @PutMapping
  @Log(
      title = "修改设备",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public R edit(@RequestBody IoTDeviceBO devInstancebo) {
    return iIotDeviceService.updateDevInstance(devInstancebo);
  }

  /**
   * 修改设备归属账号
   *
   * @param id 设备ID
   * @param ownerBO 目标归属
   * @return 修改结果
   */
  @Operation(summary = "修改设备归属账号", description = "将设备分配到指定账号名下")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改设备归属",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "id", description = "设备ID", required = true)
  @PutMapping("/{id}/owner")
  @Log(
      title = "修改设备归属",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public R updateOwner(@PathVariable("id") String id, @RequestBody IoTDeviceOwnerBO ownerBO) {
    return iIotDeviceService.updateDevInstanceOwner(id, ownerBO);
  }

  @Operation(summary = "修改子设备所属网关", description = "将网关子设备换绑到指定网关设备")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改子设备所属网关",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "id", description = "设备ID", required = true)
  @PutMapping("/{id}/gateway")
  @Log(
      title = "修改子设备所属网关",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public R updateSubDeviceGateway(
      @PathVariable("id") String id, @RequestBody IoTSubDeviceGatewayBindBO bindBO) {
    return iIotDeviceService.updateSubDeviceGateway(id, bindBO);
  }

  @Operation(summary = "查询子设备可换绑网关", description = "根据子设备实例查询可换绑的网关设备")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取可换绑网关列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "设备ID", required = true)
  @GetMapping("/{id}/gateway/candidates")
  public AjaxResult<List<GwIoTDeviceVo>> getSubDeviceGatewayCandidates(
      @PathVariable("id") String id) {
    return AjaxResult.success(iIotDeviceService.selectSubDeviceGatewayCandidates(id));
  }

  /**
   * 绑定设备平台
   *
   * @param object 绑定参数
   * @return 绑定结果
   */
  @Operation(summary = "绑定设备平台", description = "绑定设备到指定平台")
  @ApiResponse(
      responseCode = "200",
      description = "成功绑定设备平台",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping(value = "/app")
  @Log(
      title = "绑定设备平台",
      businessType = BusinessType.OTHER,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult bindApp(@RequestBody JSONObject object) {
    String ids = object.getStr("ids");
    String appUniqueId = object.getStr("appUniqueId");
    if (StrUtil.isBlank(appUniqueId)) {
      String id = ids.split(",")[0];
      IoTDevice ioTDevice = iIotDeviceService.selectDevInstanceById(id);
      appUniqueId = ioTDevice.getApplication();
    }
    IoTUserApplication application =
        iIotUserApplicationService.selectIotUserApplicationById(appUniqueId);
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    if (ObjectUtil.isEmpty(application)
        || (!iotUser.isAdmin() && !application.getUnionId().equals(iotUser.getUnionId()))) {
      throw new IoTException("你无权操作");
    }
    return toAjax(iIotDeviceService.bindApp(object.getStr("ids"), object.getStr("appUniqueId")));
  }

  /**
   * 删除设备
   *
   * @param ids 设备ID数组
   * @return 删除结果
   */
  @Operation(summary = "删除设备", description = "根据设备ID数组删除设备")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除设备",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "ids", description = "设备ID数组", required = true)
  @DeleteMapping("/{ids}")
  @PreAuthorize("@ss.hasPermi('system:instance:remove')")
  @Log(
      title = "删除设备",
      businessType = BusinessType.DELETE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public R remove(@PathVariable String[] ids) {
    return iIotDeviceService.deleteDevInstanceByIds(ids);
  }

  @Operation(summary = "设备删除影响预检", description = "返回网关子设备与模块资源影响，不执行删除")
  @GetMapping("/delete-preview/{ids}")
  @PreAuthorize("@ss.hasPermi('system:instance:remove')")
  public R<List<IoTDeviceDeletePreview>> deletePreview(@PathVariable String[] ids) {
    return R.ok(iIotDeviceService.previewDeleteDevInstances(ids));
  }

  /**
   * 获取所有设备型号
   *
   * @param child 只查询子设备
   * @param apps 支持应用
   * @return 设备型号列表
   */
  @Operation(summary = "获取所有设备型号", description = "获取所有设备型号信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备型号列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "child", description = "只查询子设备", required = false)
  @Parameter(name = "apps", description = "支持应用", required = false)
  @GetMapping("/models")
  public AjaxResult<List<IoTDeviceCompanyVO>> getModels(
      @RequestParam(required = false, defaultValue = "false") Boolean child,
      @RequestParam(required = false) String apps) {
    List<IoTDeviceCompanyVO> ioTDeviceCompanyVOS =
        iIotDeviceService.queryAllBySupportChild(child, apps);
    return AjaxResult.success(ioTDeviceCompanyVOS);
  }

  /**
   * 获取设备影子
   *
   * @param id 设备ID
   * @return 设备影子信息
   */
  @Operation(summary = "获取设备影子", description = "根据设备ID获取设备影子信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备影子信息",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "设备ID", required = true)
  @GetMapping("/shadow/{id}")
  public AjaxResult<IoTDeviceShadow> getDeviceShadow(@PathVariable("id") String id) {
    IoTDeviceShadow deviceShadow = iIotDeviceService.getDeviceShadow(id);
    return AjaxResult.success(deviceShadow);
  }

  /**
   * 批量获取设备影子
   *
   * @param ids 设备 IoT ID 列表
   * @return 设备影子列表
   */
  @Operation(summary = "批量获取设备影子", description = "根据设备 IoT ID 列表批量获取设备影子信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备影子列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping("/shadow/batch")
  public AjaxResult<List<IoTDeviceShadow>> getDeviceShadows(
      @RequestBody(required = false) List<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return AjaxResult.success(Collections.emptyList());
    }
    return AjaxResult.success(iIotDeviceService.getDeviceShadows(ids));
  }

  /**
   * 下行加保存日志
   *
   * @param productKey 产品Key
   * @param downRequest 下行请求
   * @return 下行结果
   */
  @Operation(summary = "下行加保存日志", description = "发送下行指令并保存日志")
  @ApiResponse(
      responseCode = "200",
      description = "成功发送下行指令",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @RequestMapping("/down/{productKey}")
  @Log(
      title = "下行加保存日志",
      businessType = BusinessType.OTHER,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public R iotFunctionsDown(
      @PathVariable("productKey") String productKey, @RequestBody String downRequest) {
    return iIotDeviceService.iotFunctionsDown(productKey, downRequest);
  }

  /**
   * 上行
   *
   * @param productKey 产品Key
   * @param upMsg 上行消息
   * @return 上行结果
   */
  @Operation(summary = "上行", description = "处理设备上行消息")
  @ApiResponse(
      responseCode = "200",
      description = "成功处理上行消息",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @RequestMapping("/up/{productKey}")
  @Log(title = "上行", businessType = BusinessType.OTHER)
  public R iotUp(@PathVariable("productKey") String productKey, @RequestBody String upMsg) {
    IoTProduct ioTProduct = iotProductDeviceService.getProduct(productKey);
    IUP doUp = IoTDownlFactory.getIUP(ioTProduct.getThirdPlatform());
    doUp.debugAsyncUP(upMsg);
    return R.ok();
  }

  /**
   * 根据产品key查型号
   *
   * @param productKey 产品Key
   * @return 产品型号信息
   */
  @Operation(summary = "获取产品型号", description = "根据产品Key获取产品型号信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取产品型号信息",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @GetMapping("/model/{productKey}")
  public AjaxResult<IoTDeviceModelVO> model(@PathVariable("productKey") String productKey) {
    IoTDeviceModelVO ioTDeviceModelVO = iIotDeviceService.getModelByProductKey(productKey);
    return AjaxResult.success(ioTDeviceModelVO);
  }

  /**
   * 根据产品key查询匹配的网关设备
   *
   * @param productKey 产品Key
   * @param iotUser IoT用户
   * @return 网关设备列表
   */
  @Operation(summary = "根据产品key查询匹配的网关设备", description = "根据产品Key查询匹配的网关设备列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取网关设备列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @GetMapping("/gateway/{productKey}")
  public AjaxResult<List<GwIoTDeviceVo>> getGatewayDeviceList(
      @PathVariable("productKey") String productKey, IoTUser iotUser) {
    IoTGwDeviceBO bo = new IoTGwDeviceBO();
    bo.setProductKey(productKey);
    if (!iotUser.isAdmin()) {
      bo.setCreatorId(iotUser.getUnionId());
    }
    List<GwIoTDeviceVo> gwIoTDeviceVo = iIotDeviceService.selectGwIotDevices(bo);
    return AjaxResult.success(gwIoTDeviceVo);
  }

  /**
   * 根据网关设备，查询子设备的关系
   *
   * @param productKey 产品Key
   * @param deviceId 设备ID
   * @return 子设备关系列表
   */
  @Operation(summary = "根据网关设备，查询子设备的关系", description = "根据网关设备查询子设备的关系")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取子设备关系列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "productKey", description = "产品Key", required = true)
  @Parameter(name = "deviceId", description = "设备ID", required = true)
  @GetMapping("/gateway/sub/relation/{productKey}/{deviceId}")
  public AjaxResult<List<GwIoTDeviceVo>> getSubDevice(
      @PathVariable("productKey") String productKey, @PathVariable("deviceId") String deviceId) {
    // 查询子设备关系
    List<GwIoTDeviceVo> subDevices =
        iIotDeviceService.selectSubDeviceRelationByGw(productKey, deviceId);
    return AjaxResult.success(subDevices);
  }
}
