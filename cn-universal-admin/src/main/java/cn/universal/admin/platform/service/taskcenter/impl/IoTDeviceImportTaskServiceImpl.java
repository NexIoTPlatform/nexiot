package cn.universal.admin.platform.service.taskcenter.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.admin.platform.service.taskcenter.IIoTTaskCenterService;
import cn.universal.admin.platform.service.taskcenter.IoTDeviceImportTaskService;
import cn.universal.admin.platform.service.taskcenter.IoTTaskCenterDispatchService;
import cn.universal.admin.platform.service.taskcenter.support.IoTTaskCenterConstants;
import cn.universal.common.exception.BaseException;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.entity.IoTProduct;
import cn.universal.persistence.entity.IoTTaskCenter;
import cn.universal.persistence.entity.IoTTaskItem;
import cn.universal.persistence.entity.IoTUser;
import cn.universal.persistence.entity.vo.IoTTaskSubmitResultVO;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.mapper.IoTProductMapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IoTDeviceImportTaskServiceImpl implements IoTDeviceImportTaskService {

  private final IIoTTaskCenterService taskCenterService;
  private final IoTTaskCenterDispatchService dispatchService;
  private final IoTProductMapper productMapper;
  private final IoTDeviceMapper deviceMapper;

  @Override
  public IoTTaskSubmitResultVO submitImportTask(
      List<IoTDevice> devicesImport,
      IoTProduct product,
      String fileName,
      String gwDeviceId,
      String gwProductKey,
      IoTUser creator,
      String tenantId,
      Long industrialTemplateId,
      Integer industrialTemplateVersion) {
    validateImportBatch(devicesImport);
    String scopedTenantId = requireTenantId(tenantId, creator);
    IoTProduct scopedProduct = requireProduct(product, scopedTenantId);
    String resolvedGwProductKey =
        resolveGatewayProductKey(gwDeviceId, gwProductKey, scopedTenantId);
    if (industrialTemplateId != null || industrialTemplateVersion != null) {
      throw new BaseException("开源版本不支持工业模板导入");
    }
    Date now = new Date();
    String taskNo = "TASK-" + now.getTime() + "-" + IdUtil.fastSimpleUUID().substring(0, 8);
    String creatorName =
        StrUtil.blankToDefault(StrUtil.blankToDefault(creator.getAlias(), creator.getUsername()), creator.getUnionId());
    String taskName =
        String.format(
            "设备导入-%s-%s",
            scopedProduct.getName(), cn.hutool.core.date.DateUtil.formatDateTime(now));

    Map<String, Object> requestSnapshot = new LinkedHashMap<>();
    requestSnapshot.put("fileName", fileName);
    requestSnapshot.put("productKey", scopedProduct.getProductKey());
    requestSnapshot.put("productName", scopedProduct.getName());
    requestSnapshot.put("gwDeviceId", gwDeviceId);
    requestSnapshot.put("gwProductKey", resolvedGwProductKey);
    requestSnapshot.put("deviceCount", devicesImport.size());

    IoTTaskCenter taskCenter =
        IoTTaskCenter.builder()
            .taskNo(taskNo)
            .taskType(IoTTaskCenterConstants.TaskType.DEVICE_IMPORT)
            .taskName(taskName)
            .bizModule("device")
            .bizKey(scopedProduct.getProductKey())
            .productKey(scopedProduct.getProductKey())
            .productName(scopedProduct.getName())
            .status(IoTTaskCenterConstants.TaskStatus.PENDING)
            .totalCount(devicesImport.size())
            .successCount(0)
            .failCount(0)
            .creatorId(creator.getUnionId())
            .tenantId(scopedTenantId)
            .creatorName(creatorName)
            .requestSnapshot(JSONUtil.toJsonStr(requestSnapshot))
            .build();

    List<IoTTaskItem> items = new ArrayList<>();
    for (int i = 0; i < devicesImport.size(); i++) {
      IoTDevice device = devicesImport.get(i);
      items.add(
          IoTTaskItem.builder()
              .itemNo(String.format("ROW-%03d", i + 1))
              .itemKey(device.getDeviceId())
              .itemName(device.getDeviceName())
              .rowNo(resolveImportRowNo(device, i + 3))
              .status(IoTTaskCenterConstants.ItemStatus.PENDING)
              .rawData(buildDeviceRawData(device))
              .build());
    }

    Long taskId = taskCenterService.createTask(taskCenter, items);
    taskCenterService.appendLog(
        taskId,
        IoTTaskCenterConstants.LogLevel.INFO,
        IoTTaskCenterConstants.LogType.SUBMIT,
        String.format("任务已创建，共 %s 条待导入数据", devicesImport.size()),
        JSONUtil.toJsonStr(requestSnapshot));
    dispatchService.dispatch(taskId);
    return IoTTaskSubmitResultVO.builder()
        .taskId(taskId)
        .taskNo(taskNo)
        .taskType(IoTTaskCenterConstants.TaskType.DEVICE_IMPORT)
        .taskName(taskName)
        .status(IoTTaskCenterConstants.TaskStatus.PENDING)
        .build();
  }

  private String requireTenantId(String tenantId, IoTUser creator) {
    if (creator == null) {
      throw new BaseException("无法识别设备导入操作用户");
    }
    String scopedTenantId =
        StrUtil.trim(StrUtil.blankToDefault(tenantId, creator.getTenantId()));
    if (StrUtil.isBlank(scopedTenantId)) {
      throw new BaseException("无法识别设备导入租户");
    }
    return scopedTenantId;
  }

  private IoTProduct requireProduct(IoTProduct requestedProduct, String tenantId) {
    String productKey = requestedProduct == null ? null : StrUtil.trim(requestedProduct.getProductKey());
    if (StrUtil.isBlank(productKey)) {
      throw new BaseException("导入产品不存在");
    }
    IoTProduct product = productMapper.getProductByProductKey(productKey);
    if (product == null || !Objects.equals(tenantId, product.getTenantId())) {
      throw new BaseException("导入产品不存在或不属于当前租户: " + productKey);
    }
    return product;
  }

  private String resolveGatewayProductKey(
      String gwDeviceId, String gwProductKey, String tenantId) {
    String deviceId = StrUtil.trim(gwDeviceId);
    String productKey = StrUtil.trim(gwProductKey);
    if (StrUtil.isBlank(deviceId)) {
      if (StrUtil.isNotBlank(productKey)) {
        throw new BaseException("网关产品编号必须与网关设备同时提供");
      }
      return null;
    }

    IoTDevice gateway;
    if (StrUtil.isNotBlank(productKey)) {
      gateway =
          deviceMapper.selectOne(
              IoTDevice.builder()
                  .tenantId(tenantId)
                  .productKey(productKey)
                  .deviceId(deviceId)
                  .build());
      if (gateway == null) {
        throw new BaseException("所属网关不存在或不属于当前租户");
      }
    } else {
      List<IoTDevice> gateways =
          deviceMapper.select(
              IoTDevice.builder().tenantId(tenantId).deviceId(deviceId).build());
      if (gateways == null || gateways.isEmpty()) {
        throw new BaseException("所属网关不存在或不属于当前租户");
      }
      if (gateways.size() != 1) {
        throw new BaseException("当前租户存在同名网关，请同时提供网关产品编号");
      }
      gateway = gateways.get(0);
      productKey = gateway.getProductKey();
    }

    IoTProduct gatewayProduct = productMapper.getProductByProductKey(productKey);
    if (gatewayProduct == null || !Objects.equals(tenantId, gatewayProduct.getTenantId())) {
      throw new BaseException("所属网关产品不存在或不属于当前租户");
    }
    return gateway.getProductKey();
  }

  private String buildDeviceRawData(IoTDevice device) {
    JSONObject rawData = JSONUtil.parseObj(device);
    rawData.set("longitude", device.getLongitude());
    rawData.set("latitude", device.getLatitude());
    return JSONUtil.toJsonStr(rawData);
  }

  private void validateImportBatch(List<IoTDevice> devicesImport) {
    if (CollectionUtil.isEmpty(devicesImport)) {
      throw new BaseException("表格数据为空");
    }
    if (devicesImport.size() > 500) {
      throw new BaseException("超过一次最大导入数量500");
    }
  }

  private Integer resolveImportRowNo(IoTDevice device, int defaultRowNo) {
    if (device == null || device.getParams() == null) {
      return defaultRowNo;
    }
    Object rowNo = device.getParams().get("importRowNo");
    if (rowNo instanceof Number) {
      return ((Number) rowNo).intValue();
    }
    if (rowNo != null && StrUtil.isNumeric(String.valueOf(rowNo))) {
      return Integer.parseInt(String.valueOf(rowNo));
    }
    return defaultRowNo;
  }
}
