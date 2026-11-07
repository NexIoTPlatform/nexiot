package cn.universal.admin.platform.service.taskcenter.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.common.service.BaseServiceImpl;
import cn.universal.admin.common.utils.ExcelUtil;
import cn.universal.admin.platform.service.taskcenter.IIoTTaskCenterService;
import cn.universal.admin.platform.service.taskcenter.IoTDeviceCredentialTaskService;
import cn.universal.admin.platform.service.taskcenter.IoTTaskCenterDispatchService;
import cn.universal.admin.platform.service.taskcenter.support.IoTTaskCenterConstants;
import cn.universal.common.exception.BaseException;
import cn.universal.common.exception.IoTException;
import cn.universal.persistence.dto.IoTDeviceCredentialExportDTO;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTTaskCenter;
import cn.universal.persistence.entity.IoTTaskItem;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.bo.IoTDeviceCredentialGenerateTaskBO;
import cn.universal.persistence.entity.vo.IoTTaskSubmitResultVO;
import cn.universal.persistence.mapper.IoTProductMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IoTDeviceCredentialTaskServiceImpl extends BaseServiceImpl
    implements IoTDeviceCredentialTaskService {

  private static final int MAX_GENERATE_COUNT = 5000;
  private static final int DEFAULT_ID_LENGTH = 6;
  private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_.:-]{1,64}$");
  private static final Pattern DEVICE_ID_SPLITTER = Pattern.compile("[,，;；\\s]+");

  private final IIoTTaskCenterService taskCenterService;
  private final IoTTaskCenterDispatchService dispatchService;
  private final IoTProductMapper ioTProductMapper;

  @Override
  public IoTTaskSubmitResultVO submitGenerateTask(
      IoTDeviceCredentialGenerateTaskBO submitBO, IoTUser creator) {
    if (submitBO == null) {
      throw new BaseException("提交参数不能为空");
    }
    if (creator == null || StrUtil.isBlank(creator.getUnionId())) {
      throw new BaseException("无法获取当前登录用户");
    }
    if (StrUtil.isBlank(submitBO.getProductKey())) {
      throw new BaseException("请选择产品");
    }

    IoTProduct product = ioTProductMapper.getProductByProductKey(submitBO.getProductKey());
    if (product == null || Integer.valueOf(1).equals(product.getIsDeleted())) {
      throw new BaseException("产品不存在或已停用");
    }
    if (!"mqtt".equalsIgnoreCase(product.getThirdPlatform())) {
      throw new BaseException("三元组生成仅支持 MQTT 接入产品");
    }
    assertTenantResourceAccess(product.getTenantId(), product.getCreatorId(), "无权操作该产品");

    List<String> deviceIds = resolveDeviceIds(submitBO);
    Date now = new Date();
    String taskNo = "CRED-" + now.getTime() + "-" + IdUtil.fastSimpleUUID().substring(0, 8);
    String taskName =
        StrUtil.blankToDefault(
            submitBO.getTaskName(),
            String.format("三元组生成-%s-%s", product.getName(), DateUtil.formatDateTime(now)));
    String creatorName =
        StrUtil.blankToDefault(
            StrUtil.blankToDefault(creator.getAlias(), creator.getUsername()), creator.getUnionId());
    String tenantId = resolveTenantId(creator);
    Long targetDeptId = submitBO.getDeptId() == null ? creator.getDeptId() : submitBO.getDeptId();

    Map<String, Object> requestSnapshot = new LinkedHashMap<>();
    requestSnapshot.put("productKey", product.getProductKey());
    requestSnapshot.put("productName", product.getName());
    requestSnapshot.put("tenantId", tenantId);
    requestSnapshot.put("productTenantId", product.getTenantId());
    requestSnapshot.put("creatorId", creator.getUnionId());
    requestSnapshot.put("generateMode", resolveGenerateMode(submitBO));
    requestSnapshot.put("deviceCount", deviceIds.size());
    requestSnapshot.put("deviceIdPrefix", submitBO.getDeviceIdPrefix());
    requestSnapshot.put("deviceIdStartNo", submitBO.getDeviceIdStartNo());
    requestSnapshot.put("deviceIdLength", submitBO.getDeviceIdLength());
    requestSnapshot.put("deviceNamePrefix", submitBO.getDeviceNamePrefix());
    requestSnapshot.put("application", submitBO.getApplication());
    requestSnapshot.put("deptId", targetDeptId);
    requestSnapshot.put("credentialRule", "ClientID=ProductKey.DeviceId, Username=DeviceId&ProductKey");

    IoTTaskCenter taskCenter =
        IoTTaskCenter.builder()
            .taskNo(taskNo)
            .taskType(IoTTaskCenterConstants.TaskType.DEVICE_CREDENTIAL_GENERATE)
            .taskName(taskName)
            .bizModule("device")
            .bizKey(product.getProductKey())
            .productKey(product.getProductKey())
            .productName(product.getName())
            .triggerType(IoTTaskCenterConstants.TriggerType.DIRECT)
            .status(IoTTaskCenterConstants.TaskStatus.PENDING)
            .totalCount(deviceIds.size())
            .successCount(0)
            .failCount(0)
            .creatorId(creator.getUnionId())
            .creatorName(creatorName)
            .tenantId(tenantId)
            .requestSnapshot(JSONUtil.toJsonStr(requestSnapshot))
            .build();

    List<IoTTaskItem> items = new ArrayList<>();
    for (int i = 0; i < deviceIds.size(); i++) {
      String deviceId = deviceIds.get(i);
      String deviceName = buildDeviceName(submitBO, deviceId, i);
      Map<String, Object> rawData = new LinkedHashMap<>();
      rawData.put("productKey", product.getProductKey());
      rawData.put("productName", product.getName());
      rawData.put("deviceId", deviceId);
      rawData.put("deviceName", deviceName);
      rawData.put("application", submitBO.getApplication());
      rawData.put("deptId", targetDeptId);
      rawData.put("detail", submitBO.getDetail());
      items.add(
          IoTTaskItem.builder()
              .itemNo(String.format("CRED-%04d", i + 1))
              .itemKey(deviceId)
              .itemName(deviceName)
              .rowNo(i + 1)
              .status(IoTTaskCenterConstants.ItemStatus.PENDING)
              .rawData(JSONUtil.toJsonStr(rawData))
              .build());
    }

    Long taskId = taskCenterService.createTask(taskCenter, items);
    taskCenterService.appendLog(
        taskId,
        IoTTaskCenterConstants.LogLevel.INFO,
        IoTTaskCenterConstants.LogType.SUBMIT,
        String.format("三元组生成任务已创建，共 %s 台设备", deviceIds.size()),
        JSONUtil.toJsonStr(requestSnapshot));
    dispatchService.dispatch(taskId);

    return IoTTaskSubmitResultVO.builder()
        .taskId(taskId)
        .taskNo(taskNo)
        .taskType(IoTTaskCenterConstants.TaskType.DEVICE_CREDENTIAL_GENERATE)
        .taskName(taskName)
        .status(IoTTaskCenterConstants.TaskStatus.PENDING)
        .build();
  }

  @Override
  public void exportCredentials(
      Long taskId, String unionId, boolean admin, HttpServletResponse response) throws Exception {
    IoTTaskCenter taskCenter = taskCenterService.selectTaskCenterById(taskId, unionId, admin);
    if (taskCenter == null) {
      throw new IoTException("任务不存在或无权访问");
    }
    if (!IoTTaskCenterConstants.TaskType.DEVICE_CREDENTIAL_GENERATE.equals(
        taskCenter.getTaskType())) {
      throw new IoTException("当前任务不支持三元组导出");
    }

    IoTTaskItem query = new IoTTaskItem();
    query.setTaskId(taskId);
    query.setStatus(IoTTaskCenterConstants.ItemStatus.SUCCESS);
    List<IoTTaskItem> successItems = taskCenterService.selectTaskItemList(query, unionId, admin);
    if (CollectionUtil.isEmpty(successItems)) {
      throw new IoTException("当前任务没有可导出的成功记录");
    }

    List<IoTDeviceCredentialExportDTO> rows =
        successItems.stream()
            .map(item -> buildExportRow(taskCenter, item))
            .collect(Collectors.toList());
    ExcelUtil<IoTDeviceCredentialExportDTO> util =
        new ExcelUtil<>(IoTDeviceCredentialExportDTO.class);
    util.exportExcel(response, rows, "设备三元组");
  }

  private List<String> resolveDeviceIds(IoTDeviceCredentialGenerateTaskBO submitBO) {
    List<String> explicitIds = resolveExplicitDeviceIds(submitBO);
    if ("LIST".equalsIgnoreCase(submitBO.getGenerateMode())
        && CollectionUtil.isEmpty(explicitIds)) {
      throw new BaseException("请输入设备ID列表");
    }
    if (CollectionUtil.isNotEmpty(explicitIds)) {
      validateDeviceIds(explicitIds);
      return explicitIds;
    }

    Integer count = submitBO.getCount();
    if (count == null || count <= 0) {
      throw new BaseException("请输入生成数量");
    }
    if (count > MAX_GENERATE_COUNT) {
      throw new BaseException("超过一次最大生成数量" + MAX_GENERATE_COUNT);
    }
    long startNo = submitBO.getDeviceIdStartNo() == null ? 1L : submitBO.getDeviceIdStartNo();
    if (startNo < 0) {
      throw new BaseException("起始序号不能小于0");
    }
    int length = submitBO.getDeviceIdLength() == null ? DEFAULT_ID_LENGTH : submitBO.getDeviceIdLength();
    if (length < 1 || length > 18) {
      throw new BaseException("序号长度需在1到18之间");
    }
    String prefix = StrUtil.blankToDefault(submitBO.getDeviceIdPrefix(), "DEV").trim();
    List<String> deviceIds = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      deviceIds.add(prefix + padNumber(startNo + i, length));
    }
    validateDeviceIds(deviceIds);
    return deviceIds;
  }

  private List<String> resolveExplicitDeviceIds(IoTDeviceCredentialGenerateTaskBO submitBO) {
    Set<String> ids = new LinkedHashSet<>();
    if (submitBO.getDeviceIds() != null) {
      for (String item : submitBO.getDeviceIds()) {
        addExplicitDeviceId(ids, item);
      }
    }
    if (StrUtil.isNotBlank(submitBO.getDeviceIdsText())) {
      String[] parts = DEVICE_ID_SPLITTER.split(submitBO.getDeviceIdsText());
      for (String part : parts) {
        addExplicitDeviceId(ids, part);
      }
    }
    if (ids.size() > MAX_GENERATE_COUNT) {
      throw new BaseException("超过一次最大生成数量" + MAX_GENERATE_COUNT);
    }
    return new ArrayList<>(ids);
  }

  private void addExplicitDeviceId(Set<String> ids, String value) {
    String deviceId = StrUtil.trim(value);
    if (StrUtil.isBlank(deviceId)) {
      return;
    }
    if (ids.contains(deviceId)) {
      throw new BaseException("设备ID重复: " + deviceId);
    }
    ids.add(deviceId);
  }

  private void validateDeviceIds(List<String> deviceIds) {
    if (CollectionUtil.isEmpty(deviceIds)) {
      throw new BaseException("设备ID不能为空");
    }
    for (String deviceId : deviceIds) {
      if (!DEVICE_ID_PATTERN.matcher(deviceId).matches()) {
        throw new BaseException("设备ID格式不合法: " + deviceId);
      }
    }
  }

  private String buildDeviceName(
      IoTDeviceCredentialGenerateTaskBO submitBO, String deviceId, int index) {
    if (StrUtil.isBlank(submitBO.getDeviceNamePrefix())) {
      return deviceId;
    }
    return submitBO.getDeviceNamePrefix().trim() + (index + 1);
  }

  private String resolveGenerateMode(IoTDeviceCredentialGenerateTaskBO submitBO) {
    if (CollectionUtil.isNotEmpty(submitBO.getDeviceIds())
        || StrUtil.isNotBlank(submitBO.getDeviceIdsText())) {
      return "LIST";
    }
    return StrUtil.blankToDefault(submitBO.getGenerateMode(), "AUTO");
  }

  private String padNumber(long number, int length) {
    return String.format("%0" + length + "d", number);
  }

  private IoTDeviceCredentialExportDTO buildExportRow(
      IoTTaskCenter taskCenter, IoTTaskItem item) {
    JSONObject result = parseJson(item.getResultData());
    String productKey = result.getStr("productKey", taskCenter.getProductKey());
    String deviceId = result.getStr("deviceId", item.getItemKey());
    String deviceSecret = result.getStr("deviceSecret", result.getStr("password"));
    String endTime = item.getEndTime() == null ? "" : DateUtil.formatDateTime(item.getEndTime());
    return IoTDeviceCredentialExportDTO.builder()
        .taskNo(taskCenter.getTaskNo())
        .productName(result.getStr("productName", taskCenter.getProductName()))
        .productKey(productKey)
        .deviceId(deviceId)
        .deviceName(result.getStr("deviceName", item.getItemName()))
        .iotId(result.getStr("iotId", productKey + deviceId))
        .clientId(result.getStr("clientId", productKey + "." + deviceId))
        .username(result.getStr("username", deviceId + "&" + productKey))
        .password(result.getStr("password", deviceSecret))
        .deviceSecret(deviceSecret)
        .createTime(result.getStr("createTime", endTime))
        .build();
  }

  private JSONObject parseJson(String value) {
    if (StrUtil.isBlank(value)) {
      return new JSONObject();
    }
    try {
      return JSONUtil.parseObj(value);
    } catch (Exception e) {
      return new JSONObject();
    }
  }
}
