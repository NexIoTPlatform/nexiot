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

package cn.universal.admin.network.web;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.network.service.INetworkService;
import cn.universal.common.annotation.Log;
import cn.universal.common.domain.R;
import cn.universal.common.enums.BusinessType;
import cn.universal.common.enums.OperatorType;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.Network;
import cn.universal.persistence.entity.bo.IoTDeviceBO;
import cn.universal.persistence.entity.bo.NetworkBO;
import cn.universal.persistence.entity.vo.NetworkVO;
import cn.universal.persistence.page.TableDataInfo;
import cn.universal.persistence.query.AjaxResult;
import cn.universal.persistence.query.NetworkQuery;
import cn.universal.security.BaseController;
import cn.universal.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/20
 */
@Slf4j
@RestController
@Tag(name = "网络管理", description = "网络配置与管理接口")
@RequestMapping("/admin/network")
public class NetworkController extends BaseController {

  @Autowired private INetworkService networkService;

  @GetMapping("/tcp/port-policy")
  public AjaxResult tcpPortPolicy() {
    return AjaxResult.success(networkService.getTcpPortPolicy());
  }

  /**
   * 查询网络配置列表
   *
   * @param query 网络查询条件
   * @return 网络配置列表
   */
  @Operation(summary = "查询网络配置列表", description = "根据查询条件获取网络配置列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取网络配置列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/list")
  public TableDataInfo list(@Parameter(description = "网络查询条件") NetworkQuery query) {
    try {
      List<NetworkBO> list = networkService.selectNetworkList(query);
      return getDataTable(list);
    } catch (Exception e) {
      log.error("查询列表失败", e);
      throw new IoTException("查询列表失败: " + e.getMessage());
    }
  }

  /**
   * 根据ID查询网络配置详情
   *
   * @param id 网络配置ID
   * @return 网络配置详情
   */
  @Operation(summary = "获取网络配置详情", description = "根据网络配置ID获取详细信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取网络配置详情",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping("/{id}")
  public AjaxResult getInfo(
      @Parameter(description = "网络配置ID", example = "1") @PathVariable Long id) {
    try {
      NetworkVO network = networkService.selectNetworkById(id);
      if (network == null) {
        return AjaxResult.error("不存在");
      }
      return AjaxResult.success(network);
    } catch (Exception e) {
      log.error("查询失败", e);
      return AjaxResult.error("查询失败: " + e.getMessage());
    }
  }

  /**
   * 新增网络配置
   *
   * @param network 网络配置信息
   * @return 操作结果
   */
  @Operation(summary = "新增网络配置", description = "创建新的网络配置")
  @ApiResponse(
      responseCode = "200",
      description = "成功新增网络配置",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping
  @Log(
      title = "新增网络",
      businessType = BusinessType.INSERT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult add(@Parameter(description = "网络配置信息") @RequestBody Network network) {
    try {
      // 设置创建用户
      network.setCreateUser(SecurityUtils.getUnionId());
      int result = networkService.insertNetwork(network);
      if (result > 0) {
        return AjaxResult.success("新增成功");
      } else {
        return AjaxResult.error("新增失败");
      }
    } catch (Exception e) {
      log.error("新增失败", e);
      return AjaxResult.error("新增失败: " + e.getMessage());
    }
  }

  /**
   * 修改网络配置
   *
   * @param network 网络配置信息
   * @return 操作结果
   */
  @Operation(summary = "修改网络配置", description = "更新网络配置信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功修改网络配置",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PutMapping
  @Log(
      title = "修改网络",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult edit(@Parameter(description = "网络配置信息") @RequestBody Network network) {
    try {
      int result = networkService.updateNetwork(network);
      if (result > 0) {
        return AjaxResult.success("修改成功");
      } else {
        return AjaxResult.error("修改失败");
      }
    } catch (Exception e) {
      log.error("修改失败", e);
      return AjaxResult.error("操作失败" + e.getMessage());
    }
  }

  /**
   * 删除网络配置
   *
   * @param id 网络配置ID
   * @return 操作结果
   */
  @Operation(summary = "删除网络配置", description = "根据ID删除网络配置")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除网络配置",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @DeleteMapping("/{id}")
  @Log(
      title = "删除网络",
      businessType = BusinessType.DELETE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult remove(
      @Parameter(description = "网络配置ID", example = "1") @PathVariable Long id) {
    try {
      int result = networkService.deleteNetworkById(id);
      if (result > 0) {
        return AjaxResult.success("删除成功");
      } else {
        return AjaxResult.error("删除失败");
      }
    } catch (Exception e) {
      log.error("删除失败", e);
      return AjaxResult.error("删除失败: " + e.getMessage());
    }
  }

  /**
   * 批量删除网络配置
   *
   * @param ids 网络配置ID数组
   * @return 操作结果
   */
  @Operation(summary = "批量删除网络配置", description = "批量删除多个网络配置")
  @ApiResponse(
      responseCode = "200",
      description = "成功批量删除网络配置",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @DeleteMapping("/batch/{ids}")
  @Log(
      title = "批量删除网络",
      businessType = BusinessType.DELETE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult removeBatch(
      @Parameter(description = "网络配置ID数组", example = "[1,2,3]") @PathVariable Long[] ids) {
    try {
      int result = networkService.deleteNetworkByIds(ids);
      if (result > 0) {
        return AjaxResult.success("批量删除成功");
      } else {
        return AjaxResult.error("批量删除失败");
      }
    } catch (Exception e) {
      log.error("批量删除失败", e);
      return AjaxResult.error("批量删除失败: " + e.getMessage());
    }
  }

  /**
   * 启动网络配置
   *
   * @param id 网络配置ID
   * @return 操作结果
   */
  @Operation(summary = "启动网络配置", description = "启动指定的网络配置")
  @ApiResponse(
      responseCode = "200",
      description = "成功启动网络配置",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping("/start/{id}")
  @Log(
      title = "启动网络",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult start(
      @Parameter(description = "网络配置ID", example = "1") @PathVariable Long id) {
    try {
      int result = networkService.startNetwork(id);
      if (result > 0) {
        return AjaxResult.success("启动成功");
      } else {
        return AjaxResult.error("启动失败");
      }
    } catch (Exception e) {
      log.error("启动失败", e);
      return AjaxResult.error("启动失败: " + e.getMessage());
    }
  }

  /**
   * 停止网络配置
   *
   * @param id 网络配置ID
   * @return 操作结果
   */
  @Operation(summary = "停止网络配置", description = "停止指定的网络配置")
  @ApiResponse(
      responseCode = "200",
      description = "成功停止网络配置",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping("/stop/{id}")
  @Log(
      title = "停止网络",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult stop(
      @Parameter(description = "网络配置ID", example = "1") @PathVariable Long id) {
    try {
      int result = networkService.stopNetwork(id);
      if (result > 0) {
        return AjaxResult.success("操作成功");
      } else {
        return AjaxResult.error("操作失败");
      }
    } catch (Exception e) {
      log.error("操作失败", e);
      return AjaxResult.error("操作失败: " + e.getMessage());
    }
  }

  /**
   * 重启网络配置
   *
   * @param id 网络配置ID
   * @return 操作结果
   */
  @Operation(summary = "重启网络配置", description = "重启指定的网络配置")
  @ApiResponse(
      responseCode = "200",
      description = "成功重启网络配置",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping("/restart/{id}")
  @Log(
      title = "重启网络",
      businessType = BusinessType.UPDATE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public AjaxResult restart(
      @Parameter(description = "网络配置ID", example = "1") @PathVariable Long id) {
    try {
      int result = networkService.restartNetwork(id);
      if (result > 0) {
        return AjaxResult.success("重启成功");
      } else {
        return AjaxResult.error("重启失败");
      }
    } catch (Exception e) {
      log.error("重启失败", e);
      return AjaxResult.error("重启失败: " + e.getMessage());
    }
  }

  /**
   * 获取网络类型列表
   *
   * @return 网络类型列表
   */
  @Operation(summary = "获取网络类型列表", description = "获取所有支持的网络类型")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取网络类型列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping("/types")
  public AjaxResult getNetworkTypes() {
    try {
      List<String> types = networkService.getNetworkTypes();
      return AjaxResult.success(types);
    } catch (Exception e) {
      log.error("获取网络类型列表失败", e);
      return AjaxResult.error("获取网络类型列表失败: " + e.getMessage());
    }
  }

  /**
   * 验证网络配置
   *
   * @param network 网络配置信息
   * @return 验证结果
   */
  @Operation(summary = "验证网络配置", description = "验证网络配置的有效性")
  @ApiResponse(
      responseCode = "200",
      description = "成功验证网络配置",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PostMapping("/validate")
  public AjaxResult validateConfig(
      @Parameter(description = "网络配置信息") @RequestBody Network network) {
    try {
      // 设置创建用户
      network.setCreateUser(SecurityUtils.getUnionId());
      boolean isValid = networkService.validateNetworkConfig(network);
      if (isValid) {
        return AjaxResult.success("配置验证通过");
      } else {
        return AjaxResult.error("配置验证失败");
      }
    } catch (Exception e) {
      log.error("验证配置失败", e);
      return AjaxResult.error("验证配置失败: " + e.getMessage());
    }
  }

  @Resource private INetworkService iNetworkService;

  /**
   * 查询网络列表(V1版本)
   *
   * @param bo 网络查询条件
   * @return 网络列表
   */
  @Operation(summary = "查询网络列表(V1)", description = "根据查询条件获取网络列表(V1版本)")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取网络列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/v1/list")
  public TableDataInfo list(@Parameter(description = "网络查询条件") NetworkBO bo) {
    startPage();
    List<NetworkVO> list = iNetworkService.selectNetworkList(bo);
    return getDataTable(list);
  }

  /**
   * 查询绑定指定星纵网关的设备
   *
   * @param bo 网络查询条件
   * @return 设备列表
   */
  @Operation(summary = "查询星纵网关设备", description = "查询绑定指定星纵网关的设备列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取设备列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @GetMapping("/xz/gateway/list")
  public TableDataInfo queryMileSightList(@Parameter(description = "网络查询条件") NetworkBO bo) {
    startPage();
    List<IoTDevice> list = iNetworkService.queryMileSightList(bo);
    return getDataTable(list);
  }

  /**
   * 新增星纵网关设备
   *
   * @param devInstancebo 设备信息
   * @return 操作结果
   */
  @Operation(summary = "新增星纵网关设备", description = "新增绑定到星纵网关的设备")
  @ApiResponse(
      responseCode = "200",
      description = "成功新增设备",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @PostMapping("/xz/gateway/add")
  @Log(
      title = "新增设备",
      businessType = BusinessType.INSERT,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public R add(@Parameter(description = "设备信息") @RequestBody IoTDeviceBO devInstancebo) {
    rejectSuperAdminCreate("设备");
    // 设置创建用户
    devInstancebo.setCreatorId(SecurityUtils.getUnionId());
    R result = iNetworkService.insertDevInstance(devInstancebo);
    JSONObject jsonObject = JSONUtil.parseObj(result);
    String code = jsonObject.getStr("code");
    String msg = jsonObject.getStr("msg");
    if ("0".equals(code)) {
      return R.ok();
    } else {
      return R.error(msg);
    }
  }

  /**
   * 删除星纵网关设备
   *
   * @param ids 设备ID数组
   * @return 操作结果
   */
  @Operation(summary = "删除星纵网关设备", description = "删除指定的星纵网关设备")
  @ApiResponse(
      responseCode = "200",
      description = "成功删除设备",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @DeleteMapping("/xz/gateway/{ids}")
  @Log(
      title = "删除设备",
      businessType = BusinessType.DELETE,
      operatorType = OperatorType.MANAGE,
      isSaveRequestData = true)
  public R remove(
      @Parameter(description = "设备ID数组", example = "[\"device1\", \"device2\"]") @PathVariable
          String[] ids) {
    for (String id : ids) {
      IoTDevice device = iNetworkService.getDeviceById(id);
      if (device == null) {
        return R.error("设备不存在: " + id);
      }
    }
    R result = iNetworkService.deleteDevInstanceByIds(ids);
    JSONObject jsonObject = JSONUtil.parseObj(result);
    String code = jsonObject.getStr("code");
    String msg = jsonObject.getStr("msg");
    if ("0".equals(code)) {
      return R.ok();
    } else {
      return R.error(msg);
    }
  }

  /**
   * 重新加载TCP客户端
   *
   * @param applicationId 应用ID
   * @return 操作结果
   */
  @Operation(summary = "重新加载TCP客户端", description = "重新加载指定应用的TCP客户端配置")
  @ApiResponse(
      responseCode = "200",
      description = "成功重新加载TCP客户端",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
  @GetMapping("/reload/tcpclient/{applicationId}")
  public R reloadTcpClient(
      @Parameter(description = "应用ID", example = "app123") @PathVariable("applicationId")
          String applicationId) {
    // 校验用户权限 - 需要根据applicationId获取相关的信息进行权限校验
    // 这里可能需要额外的业务逻辑来实现权限校验
    return R.ok();
  }

  /**
   * 获取网络配置状态
   *
   * @param id 网络配置ID
   * @return 网络配置状态
   */
  @Operation(summary = "获取网络配置状态", description = "获取指定网络配置的当前状态")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取网络配置状态",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping("/status/{id}")
  public AjaxResult getStatus(
      @Parameter(description = "网络配置ID", example = "1") @PathVariable Long id) {
    try {
      NetworkVO network = networkService.selectNetworkById(id);
      if (network == null) {
        return AjaxResult.error("不存在");
      }

      // 构建状态信息
      Map<String, Object> status = new HashMap<>();
      status.put("id", network.getId());
      status.put("name", network.getName());
      status.put("type", network.getType());
      status.put("typeName", network.getTypeName());
      status.put("state", network.getState());
      status.put("stateName", network.getStateName());
      status.put("productKey", network.getProductKey());
      status.put("unionId", network.getUnionId());
      status.put("createDate", network.getCreateDate());
      status.put("createUser", network.getCreateUser());

      return AjaxResult.success(status);
    } catch (Exception e) {
      log.error("获取状态失败", e);
      return AjaxResult.error("获取状态失败: " + e.getMessage());
    }
  }

  /**
   * 获取所有网络配置状态
   *
   * @return 所有网络配置状态
   */
  @Operation(summary = "获取所有网络配置状态", description = "获取所有网络配置的当前状态")
  @ApiResponse(
      responseCode = "200",
      description = "成功获取所有网络配置状态",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @GetMapping("/status")
  public AjaxResult getAllStatus() {
    try {
      NetworkQuery query = new NetworkQuery();
      query.setPageNum(1);
      query.setPageSize(1000); // 获取所有

      List<NetworkBO> networks = networkService.selectNetworkList(query);

      List<Map<String, Object>> statusList = new ArrayList<>();
      for (NetworkBO network : networks) {
        Map<String, Object> status = new HashMap<>();
        status.put("id", network.getId());
        status.put("name", network.getName());
        status.put("type", network.getType());
        status.put("state", network.getState());
        status.put("productKey", network.getProductKey());
        status.put("unionId", network.getUnionId());
        statusList.add(status);
      }

      Map<String, Object> result = new HashMap<>();
      result.put("total", statusList.size());
      result.put("networks", statusList);

      return AjaxResult.success(result);
    } catch (Exception e) {
      log.error("获取所有状态失败", e);
      return AjaxResult.error("获取所有状态失败: " + e.getMessage());
    }
  }
}
