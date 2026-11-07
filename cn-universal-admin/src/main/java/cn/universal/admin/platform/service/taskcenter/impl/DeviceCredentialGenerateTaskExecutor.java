package cn.universal.admin.platform.service.taskcenter.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.platform.service.taskcenter.IIoTTaskCenterService;
import cn.universal.admin.platform.service.taskcenter.IoTTaskCenterExecutor;
import cn.universal.admin.platform.service.taskcenter.support.IoTTaskCenterConstants;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.exception.BaseException;
import cn.universal.persistence.base.IoTDeviceLifeCycle;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTTaskCenter;
import cn.universal.persistence.entity.IoTTaskItem;
import cn.universal.persistence.entity.vo.IoTTaskCenterStatisticsVO;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeviceCredentialGenerateTaskExecutor implements IoTTaskCenterExecutor {

  private final IIoTTaskCenterService taskCenterService;
  private final IoTDeviceMapper ioTDeviceMapper;
  private final IoTProductMapper ioTProductMapper;
  private final IoTDeviceLifeCycle ioTDeviceLifeCycle;

  public DeviceCredentialGenerateTaskExecutor(
      IIoTTaskCenterService taskCenterService,
      IoTDeviceMapper ioTDeviceMapper,
      IoTProductMapper ioTProductMapper,
      @Qualifier("ioTDeviceActionAfterService") IoTDeviceLifeCycle ioTDeviceLifeCycle) {
    this.taskCenterService = taskCenterService;
    this.ioTDeviceMapper = ioTDeviceMapper;
    this.ioTProductMapper = ioTProductMapper;
    this.ioTDeviceLifeCycle = ioTDeviceLifeCycle;
  }

  @Override
  public String taskType() {
    return IoTTaskCenterConstants.TaskType.DEVICE_CREDENTIAL_GENERATE;
  }

  @Override
  public void execute(IoTTaskCenter taskCenter) {
    IoTProduct product = ioTProductMapper.getProductByProductKey(taskCenter.getProductKey());
    if (product == null) {
      throw new BaseException("三元组所属产品不存在: " + taskCenter.getProductKey());
    }

    List<IoTTaskItem> items = taskCenterService.selectPendingItems(taskCenter.getId());
    for (IoTTaskItem item : items) {
      processItem(taskCenter, item, product);
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
            "三元组生成完成，成功 %s 条，失败 %s 条",
            defaultZero(statistics.getSuccessCount()), defaultZero(statistics.getFailCount())),
        JSONUtil.toJsonStr(result));
    taskCenterService.markTaskFinished(taskCenter.getId(), null, JSONUtil.toJsonStr(result), null);
  }

  private void processItem(IoTTaskCenter taskCenter, IoTTaskItem item, IoTProduct product) {
    taskCenterService.markItemRunning(item.getId());
    JSONObject rawData = parseJson(item.getRawData());
    String productKey = StrUtil.blankToDefault(rawData.getStr("productKey"), product.getProductKey());
    String deviceId = StrUtil.blankToDefault(rawData.getStr("deviceId"), item.getItemKey());
    try {
      if (StrUtil.isBlank(deviceId)) {
        throw new BaseException("设备ID不能为空");
      }
      IoTDevice existing = ioTDeviceMapper.selectIoTDevice(productKey, deviceId);
      if (existing != null) {
        throw new BaseException("设备已存在");
      }

      String deviceSecret = buildDeviceSecret(productKey, deviceId);
      IoTDevice device = buildDevice(taskCenter, product, rawData, deviceId, deviceSecret);
      int rows = ioTDeviceMapper.insertUseGeneratedKeys(device);
      if (rows <= 0) {
        throw new BaseException("设备入库失败");
      }
      publishCreateLifecycle(productKey, deviceId);

      Map<String, Object> successData = buildSuccessData(product, device, deviceSecret);
      taskCenterService.markItemFinished(
          item.getId(),
          IoTTaskCenterConstants.ItemStatus.SUCCESS,
          "三元组生成成功",
          JSONUtil.toJsonStr(successData));
    } catch (DuplicateKeyException e) {
      markFailed(taskCenter, item, "设备已存在或唯一键冲突");
    } catch (Exception e) {
      markFailed(taskCenter, item, e.getMessage());
    } finally {
      taskCenterService.refreshTaskStatistics(taskCenter.getId());
    }
  }

  private IoTDevice buildDevice(
      IoTTaskCenter taskCenter,
      IoTProduct product,
      JSONObject rawData,
      String deviceId,
      String deviceSecret) {
    long nowSeconds = System.currentTimeMillis() / 1000;
    String productKey = product.getProductKey();
    String deviceName = StrUtil.blankToDefault(rawData.getStr("deviceName"), deviceId);
    return IoTDevice.builder()
        .productKey(productKey)
        .deviceId(deviceId)
        .iotId(productKey + deviceId)
        .deviceName(deviceName)
        .deviceSecret(deviceSecret)
        .productName(product.getName())
        .creatorId(taskCenter.getCreatorId())
        .tenantId(taskCenter.getTenantId())
        .deptId(rawData.getLong("deptId"))
        .application(rawData.getStr("application"))
        .detail(rawData.getStr("detail"))
        .state(false)
        .createTime(nowSeconds)
        .configuration("{}")
        .ext3(IoTTaskCenterConstants.TaskType.DEVICE_CREDENTIAL_GENERATE)
        .ext4(taskCenter.getTaskNo())
        .build();
  }

  private Map<String, Object> buildSuccessData(
      IoTProduct product, IoTDevice device, String deviceSecret) {
    String productKey = product.getProductKey();
    String deviceId = device.getDeviceId();
    Map<String, Object> successData = new LinkedHashMap<>();
    successData.put("productName", product.getName());
    successData.put("productKey", productKey);
    successData.put("deviceId", deviceId);
    successData.put("deviceName", device.getDeviceName());
    successData.put("iotId", productKey + deviceId);
    successData.put("clientId", productKey + "." + deviceId);
    successData.put("username", deviceId + "&" + productKey);
    successData.put("password", deviceSecret);
    successData.put("deviceSecret", deviceSecret);
    successData.put("createTime", DateUtil.formatDateTime(new Date()));
    successData.put("message", "三元组生成成功");
    return successData;
  }

  private void markFailed(IoTTaskCenter taskCenter, IoTTaskItem item, String message) {
    String failMessage = StrUtil.blankToDefault(message, "三元组生成失败");
    taskCenterService.appendLog(
        taskCenter.getId(),
        IoTTaskCenterConstants.LogLevel.WARN,
        IoTTaskCenterConstants.LogType.ITEM,
        String.format("设备 %s 三元组生成失败: %s", item.getItemKey(), failMessage),
        item.getRawData());
    taskCenterService.markItemFinished(
        item.getId(), IoTTaskCenterConstants.ItemStatus.FAILED, failMessage, null);
  }

  private void publishCreateLifecycle(String productKey, String deviceId) {
    try {
      ioTDeviceLifeCycle.create(productKey, deviceId, null);
    } catch (Exception e) {
      log.warn(
          "三元组设备已入库但创建生命周期处理失败: productKey={}, deviceId={}, error={}",
          productKey,
          deviceId,
          e.getMessage(),
          e);
    }
  }

  private String buildDeviceSecret(String productKey, String deviceId) {
    return MD5.create().digestHex(productKey + deviceId + IoTConstant.SALT);
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

  private int defaultZero(Integer value) {
    return value == null ? 0 : value;
  }
}
