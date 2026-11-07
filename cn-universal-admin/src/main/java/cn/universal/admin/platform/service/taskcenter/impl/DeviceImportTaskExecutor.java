package cn.universal.admin.platform.service.taskcenter.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.platform.service.taskcenter.IIoTTaskCenterService;
import cn.universal.admin.platform.service.taskcenter.IoTTaskCenterExecutor;
import cn.universal.admin.platform.service.taskcenter.support.IoTTaskCenterConstants;
import cn.universal.common.exception.BaseException;
import cn.universal.common.utils.StringUtils;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTTaskCenter;
import cn.universal.persistence.entity.IoTTaskItem;
import cn.universal.persistence.entity.bo.IoTDeviceBO;
import cn.universal.persistence.entity.vo.IoTTaskCenterStatisticsVO;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeviceImportTaskExecutor implements IoTTaskCenterExecutor {

  private final IIoTTaskCenterService taskCenterService;
  private final IoTProductMapper ioTProductMapper;
  private final IoTDeviceMapper ioTDeviceMapper;
  private final DeviceImportRowTransactionService rowTransactionService;

  @Override
  public String taskType() {
    return IoTTaskCenterConstants.TaskType.DEVICE_IMPORT;
  }

  @Override
  public void execute(IoTTaskCenter taskCenter) {
    JSONObject request = JSONUtil.parseObj(taskCenter.getRequestSnapshot());
    String productKey = request.getStr("productKey");
    String gwDeviceId = request.getStr("gwDeviceId");
    String gwProductKey = request.getStr("gwProductKey");
    if (request.getLong("industrialTemplateId") != null
        || request.getInt("industrialTemplateVersion") != null) {
      throw new BaseException("开源版本不支持工业模板导入");
    }

    String tenantId = requireTaskTenantId(taskCenter);
    IoTProduct product = ioTProductMapper.getProductByProductKey(productKey);
    if (product == null || !Objects.equals(tenantId, product.getTenantId())) {
      throw new BaseException("导入产品不存在或不属于任务租户: " + productKey);
    }
    gwProductKey = resolveGatewayProductKey(gwDeviceId, gwProductKey, tenantId);

    List<IoTTaskItem> items = taskCenterService.selectPendingItems(taskCenter.getId());
    for (IoTTaskItem item : items) {
      processItem(taskCenter, item, product, gwDeviceId, gwProductKey);
    }

    IoTTaskCenterStatisticsVO statistics = taskCenterService.refreshTaskStatistics(taskCenter.getId());
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("totalCount", statistics.getTotalCount());
    result.put("successCount", statistics.getSuccessCount());
    result.put("failCount", statistics.getFailCount());
    result.put("pendingCount", statistics.getPendingCount());
    taskCenterService.appendLog(
        taskCenter.getId(),
        IoTTaskCenterConstants.LogLevel.INFO,
        IoTTaskCenterConstants.LogType.FINISH,
        String.format(
            "任务执行完成，成功 %s 条，失败 %s 条",
            defaultZero(statistics.getSuccessCount()), defaultZero(statistics.getFailCount())),
        JSONUtil.toJsonStr(result));
    taskCenterService.markTaskFinished(taskCenter.getId(), null, JSONUtil.toJsonStr(result), null);
  }

  private String requireTaskTenantId(IoTTaskCenter taskCenter) {
    String tenantId = taskCenter == null ? null : StrUtil.trim(taskCenter.getTenantId());
    if (StrUtil.isBlank(tenantId)) {
      throw new BaseException("设备导入任务缺少租户信息");
    }
    return tenantId;
  }

  private String resolveGatewayProductKey(
      String gwDeviceId, String gwProductKey, String tenantId) {
    String deviceId = StrUtil.trim(gwDeviceId);
    String productKey = StrUtil.trim(gwProductKey);
    if (StrUtil.isBlank(deviceId)) {
      if (StrUtil.isNotBlank(productKey)) {
        throw new BaseException("设备导入任务的网关身份不完整");
      }
      return null;
    }

    IoTDevice gateway;
    if (StrUtil.isNotBlank(productKey)) {
      gateway =
          ioTDeviceMapper.selectOne(
              IoTDevice.builder()
                  .tenantId(tenantId)
                  .productKey(productKey)
                  .deviceId(deviceId)
                  .build());
      if (gateway == null) {
        throw new BaseException("任务所属网关不存在或不属于任务租户");
      }
    } else {
      List<IoTDevice> gateways =
          ioTDeviceMapper.select(
              IoTDevice.builder().tenantId(tenantId).deviceId(deviceId).build());
      if (gateways == null || gateways.isEmpty()) {
        throw new BaseException("任务所属网关不存在或不属于任务租户");
      }
      if (gateways.size() != 1) {
        throw new BaseException("任务租户存在同名网关，无法恢复唯一网关身份");
      }
      gateway = gateways.get(0);
      productKey = gateway.getProductKey();
    }

    IoTProduct gatewayProduct = ioTProductMapper.getProductByProductKey(productKey);
    if (gatewayProduct == null || !Objects.equals(tenantId, gatewayProduct.getTenantId())) {
      throw new BaseException("任务所属网关产品不存在或不属于任务租户");
    }
    return gateway.getProductKey();
  }

  private void processItem(
      IoTTaskCenter taskCenter,
      IoTTaskItem item,
      IoTProduct product,
      String gwDeviceId,
      String gwProductKey) {
    taskCenterService.markItemRunning(item.getId());
    IoTDevice source = parseImportDeviceRawData(item.getRawData());
    try {
      validImportDevice(source, product, gwDeviceId);
      IoTDeviceBO deviceBO = buildDeviceBO(source, product, gwDeviceId, gwProductKey);
      deviceBO.setTenantId(taskCenter.getTenantId());
      deviceBO.setCreatorId(taskCenter.getCreatorId());
      deviceBO.setCreatorName(taskCenter.getCreatorName());
      rowTransactionService.create(deviceBO);
      Map<String, Object> successData = new LinkedHashMap<>();
      successData.put("deviceId", deviceBO.getDeviceId());
      successData.put("deviceName", deviceBO.getDeviceName());
      successData.put("message", "导入成功");
      taskCenterService.markItemFinished(
          item.getId(),
          IoTTaskCenterConstants.ItemStatus.SUCCESS,
          "导入成功",
          JSONUtil.toJsonStr(successData));
    } catch (Exception e) {
      taskCenterService.appendLog(
          taskCenter.getId(),
          IoTTaskCenterConstants.LogLevel.WARN,
          IoTTaskCenterConstants.LogType.ITEM,
          String.format("第 %s 行导入失败: %s", item.getRowNo(), e.getMessage()),
          item.getRawData());
      taskCenterService.markItemFinished(
          item.getId(),
          IoTTaskCenterConstants.ItemStatus.FAILED,
          e.getMessage(),
          null);
    } finally {
      taskCenterService.refreshTaskStatistics(taskCenter.getId());
    }
  }

  private IoTDeviceBO buildDeviceBO(
      IoTDevice instance, IoTProduct product, String gwDeviceId, String gwProductKey) {
    IoTDeviceBO deviceBO = new IoTDeviceBO();
    BeanUtil.copyProperties(instance, deviceBO);
    deviceBO.setProductKey(product.getProductKey());

    if (deviceBO.getOtherParams() == null) {
      deviceBO.setOtherParams(new LinkedHashMap<>());
    }
    if (deviceBO.getOtherParams().containsKey("deviceKey")) {
      deviceBO.setSecretKey(String.valueOf(deviceBO.getOtherParams().get("deviceKey")));
    }

    if (StringUtils.isEmpty(deviceBO.getLatitude())) {
      deviceBO.setLatitude("44.61081");
    }
    if (StringUtils.isEmpty(deviceBO.getLongitude())) {
      deviceBO.setLongitude("80.990372");
    }

    JSONObject config = JSONUtil.parseObj(product.getThirdConfiguration());
    JSONArray customFields = config.getJSONArray("customField");
    if (CollectionUtil.isNotEmpty(customFields) && StrUtil.isBlank(deviceBO.getDeviceId())) {
      for (int i = 0; i < customFields.size(); i++) {
        JSONObject field = customFields.getJSONObject(i);
        if (field.getBool("autoGenerate", false)) {
          String fieldId = field.getStr("id");
          Object fieldValue =
              deviceBO.getOtherParams() == null ? null : deviceBO.getOtherParams().get(fieldId);
          if (ObjectUtil.isNotEmpty(fieldValue) && StrUtil.isNotBlank(gwDeviceId)) {
            deviceBO.setDeviceId(gwDeviceId + "-" + String.valueOf(fieldValue).trim());
          }
          break;
        }
      }
    }

    if (StrUtil.isNotBlank(gwDeviceId)) {
      deviceBO.setGwDeviceId(gwDeviceId);
      deviceBO.setExtDeviceId(gwDeviceId);
      deviceBO.setGwProductKey(gwProductKey);
    }
    deviceBO.setCoordinate(deviceBO.getLongitude() + "," + deviceBO.getLatitude());
    return deviceBO;
  }

  private IoTDevice parseImportDeviceRawData(String rawData) {
    JSONObject rawJson = JSONUtil.parseObj(StrUtil.blankToDefault(rawData, "{}"));
    IoTDevice device = JSONUtil.toBean(rawJson, IoTDevice.class);
    device.setLongitude(rawJson.getStr("longitude"));
    device.setLatitude(rawJson.getStr("latitude"));
    return device;
  }

  private void validImportDevice(IoTDevice device, IoTProduct product, String gwDeviceId) {
    JSONObject config =
        StrUtil.isBlank(product.getThirdConfiguration())
            ? new JSONObject()
            : JSONUtil.parseObj(product.getThirdConfiguration());

    if (StringUtils.isEmpty(device.getDeviceName())) {
      throw new BaseException("设备名称不能为空");
    }

    JSONArray customFields = config.getJSONArray("customField");
    boolean autoGenerate = false;
    if (CollectionUtil.isNotEmpty(customFields)) {
      for (int i = 0; i < customFields.size(); i++) {
        JSONObject field = customFields.getJSONObject(i);
        String fieldId = field.getStr("id");
        String fieldName = field.getStr("name", fieldId);
        boolean required = field.getBool("required", false);
        if (field.getBool("autoGenerate", false)) {
          autoGenerate = true;
          required = true;
        }
        Object fieldValue =
            device.getOtherParams() == null ? null : device.getOtherParams().get(fieldId);
        if (required && (fieldValue == null || StrUtil.isBlank(String.valueOf(fieldValue)))) {
          throw new BaseException(fieldName + "不能为空");
        }
      }
    }

    if (!autoGenerate && StringUtils.isEmpty(device.getDeviceId())) {
      throw new BaseException("设备序列号不能为空");
    }
    if (autoGenerate && StrUtil.isBlank(gwDeviceId) && StringUtils.isEmpty(device.getDeviceId())) {
      throw new BaseException("自动生成设备ID需要先选择所属网关设备");
    }
  }

  private int defaultZero(Integer value) {
    return value == null ? 0 : value;
  }
}
