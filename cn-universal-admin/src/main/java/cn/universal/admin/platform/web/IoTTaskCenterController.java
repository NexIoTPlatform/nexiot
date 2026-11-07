package cn.universal.admin.platform.web;

import cn.hutool.core.util.StrUtil;
import cn.universal.admin.platform.service.IIoTDeviceService;
import cn.universal.admin.platform.service.taskcenter.IIoTTaskCenterService;
import cn.universal.admin.platform.service.taskcenter.IIoTTaskDefinitionService;
import cn.universal.admin.platform.service.taskcenter.IoTDeviceCredentialTaskService;
import cn.universal.admin.platform.service.taskcenter.IoTDeviceJobTaskService;
import cn.universal.admin.platform.service.taskcenter.support.IoTTaskCenterConstants;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTTaskCenter;
import cn.universal.persistence.entity.IoTTaskDefinition;
import cn.universal.persistence.entity.IoTTaskItem;
import cn.universal.persistence.entity.IoTTaskLog;
import cn.universal.persistence.entity.bo.IoTDeviceBO;
import cn.universal.persistence.entity.bo.IoTDeviceCredentialGenerateTaskBO;
import cn.universal.persistence.entity.bo.IoTDeviceJobTaskSubmitBO;
import cn.universal.persistence.entity.vo.IoTTaskSubmitResultVO;
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
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/task-center")
@Tag(name = "通用任务中心", description = "统一管理设备作业、设备导入等异步任务的定义、执行和明细")
public class IoTTaskCenterController extends BaseController {

  private final IIoTDeviceService ioTDeviceService;
  private final IIoTTaskCenterService taskCenterService;
  private final IIoTTaskDefinitionService taskDefinitionService;
  private final IoTDeviceJobTaskService deviceJobTaskService;
  private final IoTDeviceCredentialTaskService deviceCredentialTaskService;

  @Operation(summary = "分页查询任务定义", description = "查询可重复执行的任务定义列表，当前主要用于设备作业定义")
  @ApiResponse(
      responseCode = "200",
      description = "成功返回任务定义分页数据",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @PreAuthorize("@ss.hasPermi('system:task:center:definition:list')")
  @GetMapping("/definitions/list")
  public TableDataInfo<IoTTaskDefinition> listDefinitions(IoTTaskDefinition query) {
    startPage();
    List<IoTTaskDefinition> list =
        taskDefinitionService.selectTaskDefinitionList(
            query, loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId(), isAdmin());
    return getDataTable(list);
  }

  @Operation(summary = "获取任务定义详情", description = "根据任务定义ID获取设备作业定义详情")
  @ApiResponse(
      responseCode = "200",
      description = "成功返回任务定义详情",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "任务定义ID", required = true)
  @PreAuthorize("@ss.hasPermi('system:task:center:definition:detail')")
  @GetMapping("/definitions/{id}")
  public AjaxResult<IoTTaskDefinition> getDefinitionInfo(@PathVariable("id") Long id) {
    IoTTaskDefinition definition =
        taskDefinitionService.selectTaskDefinitionById(
            id, loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId(), isAdmin());
    if (definition == null) {
      return AjaxResult.error("任务定义不存在或无权访问", null);
    }
    return AjaxResult.success(definition);
  }

  @Operation(summary = "创建设备作业定义", description = "创建可手动执行或按Cron调度执行的设备作业定义")
  @ApiResponse(
      responseCode = "200",
      description = "成功创建设备作业定义",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PreAuthorize("@ss.hasPermi('system:task:center:definition:add')")
  @PostMapping("/definitions/device-job")
  public AjaxResult<IoTTaskDefinition> createDeviceJobDefinition(
      @RequestBody IoTDeviceJobTaskSubmitBO submitBO) {
    IoTTaskDefinition definition =
        deviceJobTaskService.createFunctionJobDefinition(
            submitBO, loginIoTUnionUser(SecurityUtils.getUnionId()));
    return AjaxResult.success("任务定义已创建", definition);
  }

  @Operation(summary = "更新设备作业定义", description = "根据任务定义ID更新设备作业的范围、功能参数和调度配置")
  @ApiResponse(
      responseCode = "200",
      description = "成功更新设备作业定义",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "任务定义ID", required = true)
  @PreAuthorize("@ss.hasPermi('system:task:center:definition:edit')")
  @PutMapping("/definitions/device-job/{id}")
  public AjaxResult<IoTTaskDefinition> updateDeviceJobDefinition(
      @PathVariable("id") Long id, @RequestBody IoTDeviceJobTaskSubmitBO submitBO) {
    IoTTaskDefinition definition =
        deviceJobTaskService.updateFunctionJobDefinition(
            id, submitBO, loginIoTUnionUser(SecurityUtils.getUnionId()));
    return AjaxResult.success("任务定义已更新", definition);
  }

  @Operation(summary = "立即执行任务定义", description = "手动触发指定任务定义，并在执行记录中生成新的任务")
  @ApiResponse(
      responseCode = "200",
      description = "成功触发任务定义执行",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "任务定义ID", required = true)
  @PreAuthorize("@ss.hasPermi('system:task:center:definition:run')")
  @PutMapping("/definitions/run/{id}")
  public AjaxResult<IoTTaskSubmitResultVO> runDefinition(@PathVariable("id") Long id) {
    IoTTaskSubmitResultVO result =
        deviceJobTaskService.runFunctionJobDefinition(
            id, loginIoTUnionUser(SecurityUtils.getUnionId()));
    return AjaxResult.success("任务已触发执行", result);
  }

  @Operation(summary = "修改任务定义调度状态", description = "启用或暂停指定任务定义的Cron调度")
  @ApiResponse(responseCode = "200", description = "成功更新任务定义调度状态")
  @Parameter(name = "id", description = "任务定义ID", required = true)
  @Parameter(name = "scheduleStatus", description = "调度状态，支持 ENABLED 或 PAUSED", required = true)
  @PreAuthorize("@ss.hasPermi('system:task:center:definition:edit')")
  @PutMapping("/definitions/schedule-status/{id}/{scheduleStatus}")
  public AjaxResult<Void> changeDefinitionScheduleStatus(
      @PathVariable("id") Long id, @PathVariable("scheduleStatus") String scheduleStatus) {
    deviceJobTaskService.changeDefinitionScheduleStatus(
        id, scheduleStatus, loginIoTUnionUser(SecurityUtils.getUnionId()));
    return AjaxResult.success();
  }

  @Operation(summary = "删除任务定义", description = "批量删除任务定义，同时清理关联的调度配置")
  @ApiResponse(responseCode = "200", description = "成功删除任务定义")
  @Parameter(name = "definitionIds", description = "任务定义ID数组", required = true)
  @PreAuthorize("@ss.hasPermi('system:task:center:definition:remove')")
  @DeleteMapping("/definitions/{definitionIds}")
  public AjaxResult<Void> removeDefinitions(@PathVariable Long[] definitionIds) {
    deviceJobTaskService.deleteFunctionJobDefinitions(
        definitionIds, loginIoTUnionUser(SecurityUtils.getUnionId()));
    return AjaxResult.success();
  }

  @Operation(summary = "分页查询执行记录", description = "查询任务中心中的执行记录，包含设备导入和设备作业任务")
  @ApiResponse(
      responseCode = "200",
      description = "成功返回执行记录分页数据",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @PreAuthorize("@ss.hasPermi('system:task:center:list')")
  @GetMapping("/list")
  public TableDataInfo<IoTTaskCenter> list(IoTTaskCenter query) {
    startPage();
    List<IoTTaskCenter> list =
        taskCenterService.selectTaskCenterList(
            query, loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId(), isAdmin());
    return getDataTable(list);
  }

  @Operation(summary = "获取执行记录详情", description = "根据任务ID获取任务中心执行记录的详细信息")
  @ApiResponse(
      responseCode = "200",
      description = "成功返回执行记录详情",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "任务ID", required = true)
  @PreAuthorize("@ss.hasPermi('system:task:center:detail')")
  @GetMapping("/{id}")
  public AjaxResult<IoTTaskCenter> getInfo(@PathVariable("id") Long id) {
    IoTTaskCenter taskCenter =
        taskCenterService.selectTaskCenterById(
            id, loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId(), isAdmin());
    if (taskCenter == null) {
      return AjaxResult.error("任务不存在或无权访问", null);
    }
    return AjaxResult.success(taskCenter);
  }

  @Operation(summary = "分页查询任务明细", description = "根据任务ID查询任务明细列表，例如设备导入行明细或设备作业设备明细")
  @ApiResponse(
      responseCode = "200",
      description = "成功返回任务明细分页数据",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @Parameter(name = "taskId", description = "任务ID", required = true)
  @PreAuthorize("@ss.hasPermi('system:task:center:detail')")
  @GetMapping("/items/{taskId}")
  public TableDataInfo<IoTTaskItem> items(@PathVariable("taskId") Long taskId, IoTTaskItem query) {
    query.setTaskId(taskId);
    startPage();
    List<IoTTaskItem> list =
        taskCenterService.selectTaskItemList(
            query, loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId(), isAdmin());
    return getDataTable(list);
  }

  @Operation(summary = "查询任务日志", description = "根据任务ID查询任务执行过程中的日志记录")
  @ApiResponse(
      responseCode = "200",
      description = "成功返回任务日志列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "taskId", description = "任务ID", required = true)
  @PreAuthorize("@ss.hasPermi('system:task:center:detail')")
  @GetMapping("/logs/{taskId}")
  public AjaxResult<List<IoTTaskLog>> logs(@PathVariable("taskId") Long taskId) {
    return AjaxResult.success(
        taskCenterService.selectTaskLogList(
            taskId, loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId(), isAdmin()));
  }

  @Operation(summary = "查询设备作业目标设备", description = "根据产品、应用、分组和关键字筛选设备作业的目标设备列表")
  @ApiResponse(
      responseCode = "200",
      description = "成功返回设备作业目标设备分页数据",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @Parameter(name = "keyword", description = "设备关键字，可匹配设备ID或设备名称", required = false)
  @PreAuthorize("@ss.hasPermi('system:task:center:deviceJob:add')")
  @GetMapping("/device-job/devices")
  public TableDataInfo<IoTDevice> listDeviceJobDevices(IoTDeviceBO query, String keyword) {
    if (StrUtil.isBlank(query.getProductKey())) {
      throw new IoTException("请选择产品");
    }
    if (StrUtil.isNotBlank(keyword)) {
      query.getParams().put("keyword", keyword);
    }
    startPage();
    return getDataTable(
        ioTDeviceService.selectDevInstanceListWithTags(
            query, loginIoTUnionUser(SecurityUtils.getUnionId())));
  }

  @Operation(summary = "创建设备作业任务", description = "直接提交一次性设备作业任务，并在执行记录中生成任务")
  @ApiResponse(
      responseCode = "200",
      description = "成功创建设备作业任务",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PreAuthorize("@ss.hasPermi('system:task:center:deviceJob:add')")
  @PostMapping("/device-job")
  public AjaxResult<IoTTaskSubmitResultVO> createDeviceJobTask(
      @RequestBody IoTDeviceJobTaskSubmitBO submitBO) {
    IoTTaskSubmitResultVO result =
        deviceJobTaskService.submitFunctionJobTask(
            submitBO, loginIoTUnionUser(SecurityUtils.getUnionId()));
    return AjaxResult.success("设备作业已创建", result);
  }

  @Operation(summary = "创建设备三元组生成任务", description = "批量预注册设备并生成平台直连三元组")
  @ApiResponse(
      responseCode = "200",
      description = "成功创建设备三元组生成任务",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @PreAuthorize("@ss.hasPermi('system:task:center:credential:add')")
  @PostMapping("/device-credential")
  public AjaxResult<IoTTaskSubmitResultVO> createDeviceCredentialTask(
      @RequestBody IoTDeviceCredentialGenerateTaskBO submitBO) {
    IoTTaskSubmitResultVO result =
        deviceCredentialTaskService.submitGenerateTask(
            submitBO, loginIoTUnionUser(SecurityUtils.getUnionId()));
    return AjaxResult.success("三元组生成任务已创建", result);
  }

  @Operation(summary = "分页查询设备三元组生成任务", description = "只返回设备三元组生成任务，供独立菜单使用")
  @ApiResponse(
      responseCode = "200",
      description = "成功返回三元组生成任务分页数据",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @PreAuthorize("@ss.hasPermi('system:task:center:credential:list')")
  @GetMapping("/device-credential/list")
  public TableDataInfo<IoTTaskCenter> listDeviceCredentialTasks(IoTTaskCenter query) {
    query.setTaskType(IoTTaskCenterConstants.TaskType.DEVICE_CREDENTIAL_GENERATE);
    startPage();
    List<IoTTaskCenter> list =
        taskCenterService.selectTaskCenterList(
            query, loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId(), isAdmin());
    return getDataTable(list);
  }

  @Operation(summary = "获取设备三元组生成任务详情", description = "根据任务ID获取三元组生成任务详情")
  @ApiResponse(
      responseCode = "200",
      description = "成功返回三元组生成任务详情",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "id", description = "任务ID", required = true)
  @PreAuthorize("@ss.hasPermi('system:task:center:credential:detail')")
  @GetMapping("/device-credential/{id}")
  public AjaxResult<IoTTaskCenter> getDeviceCredentialTask(@PathVariable("id") Long id) {
    IoTTaskCenter taskCenter =
        selectDeviceCredentialTaskOrThrow(
            id, loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId());
    return AjaxResult.success(taskCenter);
  }

  @Operation(summary = "分页查询设备三元组生成任务明细", description = "根据任务ID查询三元组生成任务明细")
  @ApiResponse(
      responseCode = "200",
      description = "成功返回三元组生成任务明细分页数据",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TableDataInfo.class)))
  @Parameter(name = "taskId", description = "任务ID", required = true)
  @PreAuthorize("@ss.hasPermi('system:task:center:credential:detail')")
  @GetMapping("/device-credential/items/{taskId}")
  public TableDataInfo<IoTTaskItem> deviceCredentialItems(
      @PathVariable("taskId") Long taskId, IoTTaskItem query) {
    String unionId = loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId();
    selectDeviceCredentialTaskOrThrow(taskId, unionId);
    query.setTaskId(taskId);
    startPage();
    List<IoTTaskItem> list = taskCenterService.selectTaskItemList(query, unionId, isAdmin());
    return getDataTable(list);
  }

  @Operation(summary = "查询设备三元组生成任务日志", description = "根据任务ID查询三元组生成任务日志")
  @ApiResponse(
      responseCode = "200",
      description = "成功返回三元组生成任务日志列表",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AjaxResult.class)))
  @Parameter(name = "taskId", description = "任务ID", required = true)
  @PreAuthorize("@ss.hasPermi('system:task:center:credential:detail')")
  @GetMapping("/device-credential/logs/{taskId}")
  public AjaxResult<List<IoTTaskLog>> deviceCredentialLogs(@PathVariable("taskId") Long taskId) {
    String unionId = loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId();
    selectDeviceCredentialTaskOrThrow(taskId, unionId);
    return AjaxResult.success(taskCenterService.selectTaskLogList(taskId, unionId, isAdmin()));
  }

  @Operation(summary = "导出失败明细", description = "导出设备导入任务中的失败设备明细Excel")
  @ApiResponse(responseCode = "200", description = "成功导出失败明细文件")
  @Parameter(name = "taskId", description = "任务ID", required = true)
  @PreAuthorize("@ss.hasPermi('system:task:center:detail')")
  @PostMapping("/export-failed/{taskId}")
  public void exportFailed(@PathVariable("taskId") Long taskId, HttpServletResponse response)
      throws Exception {
    String unionId = loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId();
    IoTTaskCenter taskCenter = taskCenterService.selectTaskCenterById(taskId, unionId, isAdmin());
    if (taskCenter == null) {
      throw new IoTException("任务不存在或无权访问");
    }
    if (!IoTTaskCenterConstants.TaskType.DEVICE_IMPORT.equals(taskCenter.getTaskType())) {
      throw new IoTException("当前任务暂不支持失败明细下载");
    }
    IoTTaskItem query = new IoTTaskItem();
    query.setTaskId(taskId);
    query.setStatus(IoTTaskCenterConstants.ItemStatus.FAILED);
    List<IoTTaskItem> failedItems = taskCenterService.selectTaskItemList(query, unionId, isAdmin());
    if (failedItems.isEmpty()) {
      throw new IoTException("当前任务没有失败记录");
    }
    ioTDeviceService.exportFailedImportTemplate(
        taskCenter.getProductKey(), taskCenter.getTaskName(), failedItems, response);
  }

  @Operation(summary = "导出三元组", description = "导出三元组生成任务中的成功设备凭证Excel")
  @ApiResponse(responseCode = "200", description = "成功导出三元组文件")
  @Parameter(name = "taskId", description = "任务ID", required = true)
  @PreAuthorize("@ss.hasPermi('system:task:center:credential:export')")
  @PostMapping("/export-credentials/{taskId}")
  public void exportCredentials(@PathVariable("taskId") Long taskId, HttpServletResponse response)
      throws Exception {
    String unionId = loginIoTUnionUser(SecurityUtils.getUnionId()).getUnionId();
    deviceCredentialTaskService.exportCredentials(taskId, unionId, isAdmin(), response);
  }

  private IoTTaskCenter selectDeviceCredentialTaskOrThrow(Long taskId, String unionId) {
    IoTTaskCenter taskCenter = taskCenterService.selectTaskCenterById(taskId, unionId, isAdmin());
    if (taskCenter == null) {
      throw new IoTException("任务不存在或无权访问");
    }
    if (!IoTTaskCenterConstants.TaskType.DEVICE_CREDENTIAL_GENERATE.equals(
        taskCenter.getTaskType())) {
      throw new IoTException("当前任务不是三元组生成任务");
    }
    return taskCenter;
  }
}
