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
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.common.utils.ExcelUtil;
import cn.universal.admin.platform.service.impl.MagicFunctionCatalogService;
import cn.universal.admin.system.service.IoTDeviceProtocolService;
import cn.universal.admin.system.service.impl.IoTDeviceMessageCodecService;
import cn.universal.common.annotation.Log;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.exception.CodecException;
import cn.universal.common.exception.IoTException;
import cn.universal.core.protocol.jscrtipt.JsMethodExtractor;
import cn.universal.core.protocol.magic.MagicScriptMethodExtractor;
import cn.universal.core.protocol.support.ProtocolCodecSupport.CodecMethod;
import cn.universal.ossm.service.ISysOssService;
import cn.universal.persistence.entity.IoTDeviceProtocol;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.bo.IoTDeviceProtocolBO;
import cn.universal.persistence.entity.vo.CodecBeanInfo;
import cn.universal.persistence.entity.vo.IoTProtocolVO;
import cn.universal.persistence.entity.vo.JarValidationResult;
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
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 设备协议Controller @Author ruoyi
 *
 * @since 2023-01-06
 */
@RestController
@RequestMapping("/admin/v1/protocol")
@Tag(name = "设备协议管理", description = "设备协议管理接口")
public class IoTDeviceProtocolController extends BaseController {

  @Autowired private IoTDeviceProtocolService devProtocolService;
  @Resource private IoTDeviceMessageCodecService ioTDeviceMessageCodecService;
  @Resource private ISysOssService ossService;
  @Resource private MagicFunctionCatalogService magicFunctionCatalogService;

  /**
   * 查询设备协议列表
   *
   * @param ioTDeviceProtocol 设备协议查询条件
   * @return 设备协议列表
   */
  @Operation(summary = "查询设备协议列表", description = "根据查询条件获取设备协议列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备协议列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/list")
  public TableDataInfo list(IoTDeviceProtocol ioTDeviceProtocol) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    startPage();
    List<IoTDeviceProtocol> list =
        devProtocolService.selectDevProtocolList(ioTDeviceProtocol, iotUser);
    return getDataTable(list);
  }

  /**
   * 根据ID列表查询设备协议
   *
   * @param ioTDeviceProtocol 设备协议查询条件
   * @return 设备协议ID列表
   */
  @Operation(summary = "根据ID列表查询设备协议", description = "根据查询条件获取设备协议ID列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备协议ID列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping("/list/ids")
  public AjaxResult<List<String>> idList(IoTDeviceProtocol ioTDeviceProtocol) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    List<IoTDeviceProtocol> list =
        devProtocolService.selectDevProtocolList(ioTDeviceProtocol, iotUser);
    List<String> list1 = list.stream().map(IoTDeviceProtocol::getId).collect(Collectors.toList());
    return AjaxResult.success(list1);
  }

  /**
   * 导出设备协议列表
   *
   * @param response HTTP响应
   * @param ioTDeviceProtocol 设备协议查询条件
   */
  @Operation(summary = "导出设备协议列表", description = "导出设备协议列表到Excel文件")
  @ApiResponse(responseCode = "200", description = "成功导出设备协议列表")
  @PostMapping("/export")
  @Log(title = "导出设备协议列表", businessType = BusinessType.EXPORT)
  public void export(HttpServletResponse response, IoTDeviceProtocol ioTDeviceProtocol) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    List<IoTDeviceProtocol> list =
        devProtocolService.selectDevProtocolList(ioTDeviceProtocol, iotUser);
    ExcelUtil<IoTDeviceProtocol> util = new ExcelUtil<IoTDeviceProtocol>(IoTDeviceProtocol.class);
    util.exportExcel(response, list, "设备协议数据");
  }

  /**
   * 获取设备协议详细信息
   *
   * @param id 协议ID
   * @return 设备协议详细信息
   */
  @Operation(summary = "获取设备协议详细信息", description = "根据协议ID获取设备协议详细信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备协议详细信息",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "协议ID", required = true)
  @GetMapping(value = "/{id}")
  public AjaxResult getInfo(@PathVariable("id") String id) {
    if (StrUtil.isBlank(id)) {
      return AjaxResult.error("id is null");
    }
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    IoTDeviceProtocol ioTDeviceProtocol =
        devProtocolService.selectDevProtocolById(
            id, iotUser.isAdmin() ? null : iotUser.getUnionId());
    if (ioTDeviceProtocol == null) {
      throw new IoTException("该协议没有操作权限！");
    }
    IoTProtocolVO devProductVO = BeanUtil.toBean(ioTDeviceProtocol, IoTProtocolVO.class);
    JSONObject config = JSONUtil.parseObj(ioTDeviceProtocol.getConfiguration());
    if ("jar".equals(ioTDeviceProtocol.getType())) {
      devProductVO.setFileName(config.getStr("provider"));
      devProductVO.setUrl(config.getStr("location"));
    } else {
      devProductVO.setJscript(config.getStr("location"));
      devProductVO.setNeedBs4Decode(config.getStr("needBs4Decode"));
    }

    if ("magic".equals(ioTDeviceProtocol.getType())) {
      devProductVO.setExpectedSourceHash(
          cn.universal.admin.platform.service.debug.DriverRevisionService.hash(devProductVO.getJscript()));
    }
    return AjaxResult.success(devProductVO);
  }

  /**
   * 新增设备协议
   *
   * @param ioTDeviceProtocolBO 设备协议信息
   * @return 新增结果
   */
  @Operation(summary = "新增设备协议", description = "新增设备协议信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功新增设备协议",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping
  @Log(title = "新增设备协议", businessType = BusinessType.INSERT)
  public AjaxResult add(@RequestBody IoTDeviceProtocolBO ioTDeviceProtocolBO) {
    if (devProtocolService.selectDevProtocolById(ioTDeviceProtocolBO.getId(), null) != null) {
      throw new IoTException("该产品已存在对应协议！");
    }
    if (devProtocolService.countByProvider(ioTDeviceProtocolBO.getFileName()) > 0) {
      throw new IoTException("该包名已存在，请修改后重试！");
    }
    IoTDeviceProtocol ioTDeviceProtocol =
        BeanUtil.toBean(ioTDeviceProtocolBO, IoTDeviceProtocol.class);
    JSONObject object = parseProtocolConfiguration(ioTDeviceProtocolBO.getConfiguration());

    // 根据类型设置configuration
    switch (ioTDeviceProtocolBO.getType()) {
      case "magic":
      case "jscript":
        // 脚本类型
        object.set(
            "needBs4Decode", ObjectUtil.defaultIfNull(ioTDeviceProtocolBO.getNeedBs4Decode(), false));
        object.set("location", ioTDeviceProtocolBO.getJscript());
        object.set("provider", ioTDeviceProtocolBO.getId());
        Set<String> supportMethods =
            detectScriptSupportMethods(
                ioTDeviceProtocolBO.getType(), ioTDeviceProtocolBO.getJscript());
        if (CollUtil.isEmpty(supportMethods)) {
          throw new IoTException("预编译失败,没有包含任何一个有效方法");
        }
        object.set("supportMethods", supportMethods);
        break;

      case "spring":
        // Spring Bean类型
        object.set("location", ioTDeviceProtocolBO.getBeanName());
        object.set("provider", ioTDeviceProtocolBO.getBeanName());
        // 校验Bean并获取supportMethods
        JarValidationResult validation =
            devProtocolService.validateJarMethods(ioTDeviceProtocolBO.getBeanName(), "spring");
        if (!validation.getSuccess()) {
          throw new IoTException("校验Bean失败: " + validation.getErrorMessage());
        }
        object.set("supportMethods", validation.getSupportMethods());
        ioTDeviceProtocol.setState((byte) 1); // Spring Bean默认已发布
        break;

      case "jar":
        // Jar包类型
        object.set("provider", ioTDeviceProtocolBO.getFileName());
        object.set("location", ioTDeviceProtocolBO.getUrl());
        // 校验Jar包并获取supportMethods，传入provider作为类名
        JarValidationResult jarValidation =
            devProtocolService.validateJarMethods(
                ioTDeviceProtocolBO.getUrl(), 
                "jar",
                ioTDeviceProtocolBO.getFileName()
            );
        if (!jarValidation.getSuccess()) {
          throw new IoTException("校验Jar包失败: " + jarValidation.getErrorMessage());
        }
        object.set("supportMethods", jarValidation.getSupportMethods());
        ioTDeviceProtocol.setState((byte) 0); // Jar需要审核
        break;

      default:
        throw new IoTException("不支持的协议类型: " + ioTDeviceProtocolBO.getType());
    }

    ioTDeviceProtocol.setConfiguration(object.toString());
    // 设置创建时间
    ioTDeviceProtocol.setCreateTime(new Date());
    return toAjax(devProtocolService.insertDevProtocol(ioTDeviceProtocol));
  }

  /**
   * 修改设备协议
   *
   * @param ioTDeviceProtocolBO 设备协议信息
   * @return 修改结果
   */
  @Operation(summary = "修改设备协议", description = "修改设备协议信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改设备协议",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping
  @Log(title = "修改设备协议", businessType = BusinessType.UPDATE)
  public AjaxResult edit(@RequestBody IoTDeviceProtocolBO ioTDeviceProtocolBO) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    return toAjax(devProtocolService.updateDevProtocol(ioTDeviceProtocolBO, iotUser));
  }

  /**
   * 删除设备协议
   *
   * @param ids 协议ID数组
   * @return 删除结果
   */
  @Operation(summary = "删除设备协议", description = "根据协议ID数组删除设备协议")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除设备协议",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "ids", description = "协议ID数组", required = true)
  @DeleteMapping("/{ids}")
  @Log(title = "删除设备协议", businessType = BusinessType.DELETE)
  public AjaxResult remove(@PathVariable String[] ids) {
    return toAjax(devProtocolService.deleteDevProtocolByIds(ids));
  }

  /**
   * 编解码测试
   *
   * @param ioTDeviceProtocolBO 设备协议信息
   * @return 测试结果
   */
  @Operation(summary = "编解码测试", description = "对设备协议进行编解码测试")
  @ApiResponse(
      responseCode = "200",
      description = "成功执行编解码测试",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping("/codec")
  public AjaxResult codec(@RequestBody IoTDeviceProtocolBO ioTDeviceProtocolBO) {

    try {
      if (ioTDeviceProtocolBO.getDecode()) {
        return AjaxResult.success(
            ioTDeviceMessageCodecService.decode(
                ioTDeviceProtocolBO.getId(), ioTDeviceProtocolBO.getPayload()));
      } else {
        return AjaxResult.success(
            ioTDeviceMessageCodecService.encode(
                ioTDeviceProtocolBO.getId(), ioTDeviceProtocolBO.getPayload()));
      }
    } catch (CodecException e) {
      return AjaxResult.success(e.getMsg());
    }
  }

  @Operation(summary = "临时脚本预览调试", description = "对未保存的magic/jscript脚本进行临时编解码测试")
  @ApiResponse(
      responseCode = "200",
      description = "成功执行临时预览",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping("/codec/preview")
  public AjaxResult codecPreview(@RequestBody IoTDeviceProtocolBO ioTDeviceProtocolBO) {
    try {
      return AjaxResult.success(ioTDeviceMessageCodecService.preview(ioTDeviceProtocolBO));
    } catch (CodecException e) {
      return AjaxResult.error(e.getMsg());
    }
  }

  @Operation(summary = "查询Magic内置函数目录", description = "返回 Magic 引擎实际注册的内置与扩展函数清单")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取函数目录",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping("/magic/functions")
  public AjaxResult<List<JSONObject>> getMagicFunctions() {
    return AjaxResult.success(magicFunctionCatalogService.listFunctions());
  }

  /**
   * 查询未创建协议的产品列表（归属人为当前用户）
   *
   * @param searchKey 搜索关键词（产品名称或productKey，可选）
   * @param pageNum 页码
   * @param pageSize 每页数量
   * @return 产品分页列表
   */
  @Operation(summary = "查询未创建协议的产品列表", description = "查询未创建协议的产品列表（归属人为当前用户）")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取产品列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @Parameter(name = "searchKey", description = "搜索关键词（产品名称或productKey）", required = false)
  @GetMapping("/products/without-protocol")
  public TableDataInfo<IoTProduct> getProductsWithoutProtocol(
      @RequestParam(value = "searchKey", required = false) String searchKey,
      @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
      @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
    IoTUser iotUser = loginIoTUnionUser(SecurityUtils.getUnionId());
    String unionId = iotUser.isAdmin() ? null : SecurityUtils.getUnionId();
    int resolvedPageNum = pageNum == null ? 1 : Math.max(pageNum, 1);
    int resolvedPageSize = pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 100);
    List<IoTProduct> list =
        devProtocolService.selectProductsWithoutProtocol(
            unionId, StrUtil.trim(searchKey), resolvedPageNum, resolvedPageSize);
    return getDataTable(list);
  }

  /**
   * 获取可用的Codec Bean列表
   *
   * @return Bean列表
   */
  @Operation(
      summary = "获取可用的Codec Bean列表",
      description = "获取系统中所有实现JarDriverCodecService接口的Spring Bean")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取Bean列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping("/beans/available")
  public AjaxResult<List<CodecBeanInfo>> getAvailableCodecBeans() {
    List<CodecBeanInfo> beans = devProtocolService.getAvailableCodecBeans();
    return AjaxResult.success(beans);
  }

  /**
   * 校验Jar包或Bean的方法有效性
   *
   * @param request 校验请求
   * @return 校验结果
   */
  @Operation(summary = "校验Jar包或Bean方法", description = "校验Jar包或Spring Bean是否实现有效的编解码方法")
  @ApiResponse(
      responseCode = "200",
      description = "成功校验",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping("/jar/validate")
  public AjaxResult<JarValidationResult> validateJarMethods(
      @RequestBody Map<String, String> request) {
    String location = request.get("location");
    String type = request.get("type");

    if (StrUtil.isBlank(location) || StrUtil.isBlank(type)) {
      return AjaxResult.error("参数不能为空", new JarValidationResult());
    }

    JarValidationResult result = devProtocolService.validateJarMethods(location, type);
    return AjaxResult.success(result);
  }

  private JSONObject parseProtocolConfiguration(String configuration) {
    if (StrUtil.isBlank(configuration)) {
      return new JSONObject();
    }
    try {
      return JSONUtil.parseObj(configuration);
    } catch (Exception e) {
      return new JSONObject();
    }
  }

  private Set<String> detectScriptSupportMethods(String type, String script) {
    if (StrUtil.isBlank(script)) {
      return new HashSet<>();
    }
    Set<String> targetMethods =
        Arrays.stream(CodecMethod.values()).map(Enum::name).collect(Collectors.toSet());
    try {
      if ("magic".equals(type)) {
        return MagicScriptMethodExtractor.extractSupportMethods(script, targetMethods);
      }
      Set<String> supportMethods = new HashSet<>();
      for (String methodName : JsMethodExtractor.extractMethodNames(script)) {
        if (targetMethods.contains(methodName)) {
          supportMethods.add(methodName);
        }
      }
      return supportMethods;
    } catch (RuntimeException e) {
      String message = StrUtil.blankToDefault(e.getMessage(), ExceptionUtil.getRootCauseMessage(e));
      throw new IoTException(message);
    }
  }
}
