package cn.universal.http.access.down;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.constant.IoTConstant.DeviceStatus;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.constant.IoTConstant.ERROR_CODE;
import cn.universal.common.domain.R;
import cn.universal.core.message.DownCommonData;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.persistence.base.BaseDownRequest;
import cn.universal.persistence.base.IoTDeviceLifeCycle;
import cn.universal.persistence.base.IoTDownAdapter;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.service.IoTDeviceAreaService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/** Local HTTP device management follows the shared wrappers and lifecycle callbacks. */
@Service
public class HttpDeviceLifecycleService extends IoTDownAdapter<BaseDownRequest> {
  private final IoTDeviceMapper devices;
  private final IoTDeviceService deviceService;
  private final IoTDeviceAreaService areas;
  private final IoTDeviceLifeCycle lifecycle;

  public HttpDeviceLifecycleService(
      IoTDeviceMapper devices,
      IoTDeviceService deviceService,
      IoTDeviceAreaService areas,
      @Qualifier("ioTDeviceActionAfterService") IoTDeviceLifeCycle lifecycle) {
    this.devices = devices;
    this.deviceService = deviceService;
    this.areas = areas;
    this.lifecycle = lifecycle;
  }

  public R<?> process(HttpDownRequest request) {
    // Shared wrappers recognize the original DEV_* management commands.
    request.setCmd(canonical(request.getCmd()));
    if (DownCmd.DEV_DEL.equals(request.getCmd())) return delete(request);
    R<?> intercepted = beforeDownAction(request.getProduct(), request.getData(), request);
    if (intercepted != null) return intercepted;
    return DownCmd.DEV_ADD.equals(request.getCmd()) ? add(request) : update(request);
  }

  private R<?> add(HttpDownRequest request) {
    if (devices.selectCount(identity(request)) > 0) {
      return R.error(ERROR_CODE.DEV_ADD_DEVICE_ID_EXIST.getCode(),
          ERROR_CODE.DEV_ADD_DEVICE_ID_EXIST.getName());
    }
    DownCommonData data = DownCommonData.from(request.getData());
    IoTDevice device = IoTDevice.builder()
        .productKey(request.getProductKey())
        .deviceId(request.getDeviceId())
        .iotId(request.getProductKey() + request.getDeviceId())
        .deviceName(StrUtil.blankToDefault(data.getDeviceName(), request.getDeviceId()))
        .deviceSecret(MD5.create().digestHex(
            request.getProductKey() + request.getDeviceId() + IoTConstant.SALT))
        .createTime(System.currentTimeMillis() / 1000)
        .state(DeviceStatus.offline.getCode())
        .application(request.getApplicationId())
        .creatorId(request.getAppUnionId())
        .productName(request.getProduct().getName())
        .detail(request.getDetail())
        .deviceAddress(request.getDeviceAddress())
        .gwProductKey(request.getGwProductKey())
        .extDeviceId(StrUtil.blankToDefault(data.getExtDeviceId(),
            StrUtil.blankToDefault(request.getExtDeviceId(), request.getGwDeviceId())))
        .build();
    fillArea(device, data);
    Map<String, Object> configuration = data.getConfiguration() == null
        ? new LinkedHashMap<>() : new LinkedHashMap<>(data.getConfiguration());
    finalDown_Add(configuration, request.getProduct(), request, request.getData());
    device.setConfiguration(JSONUtil.toJsonStr(configuration));
    IoTDownAdapter.finalDown_Add(device, request.getProduct(), request);
    if (devices.insertUseGeneratedKeys(device) <= 0) return R.error("设备增加失败");
    lifecycle.create(request.getProductKey(), request.getDeviceId(), request);
    Map<String, Object> result = result(device);
    result.put("deviceSecret", device.getDeviceSecret());
    result.put("productName", request.getProduct().getName());
    result.put("deviceNode", request.getProduct().getDeviceNode());
    result.put("configuration", device.getConfiguration());
    return R.ok(result);
  }

  private R<?> update(HttpDownRequest request) {
    IoTDevice device = devices.selectOne(identity(request));
    if (device == null) {
      return R.error(ERROR_CODE.DEV_UPDATE_DEVICE_NO_ID_EXIST.getCode(),
          ERROR_CODE.DEV_UPDATE_DEVICE_NO_ID_EXIST.getName());
    }
    DownCommonData data = DownCommonData.from(request.getData());
    device.setDeviceName(data.getDeviceName());
    fillArea(device, data);
    if (StrUtil.isNotBlank(request.getDeviceAddress())) device.setDeviceAddress(request.getDeviceAddress());
    if (StrUtil.isNotBlank(request.getDetail())) device.setDetail(request.getDetail());
    if (data.getConfiguration() != null) {
      JSONObject configuration = JSONUtil.parseObj(device.getConfiguration());
      configuration.putAll(data.getConfiguration());
      device.setConfiguration(configuration.toString());
    }
    finalDown_Update(device, request);
    if (devices.updateByPrimaryKey(device) <= 0) return R.error("设备修改失败");
    lifecycle.update(request.getProductKey(), request.getDeviceId(), request);
    Map<String, Object> result = result(device);
    result.put("success", true);
    return R.ok(result);
  }

  private R<?> delete(HttpDownRequest request) {
    IoTDevice device = devices.selectOne(identity(request));
    if (device == null) {
      return R.error(ERROR_CODE.DEV_DEL_DEVICE_NO_ID_EXIST.getCode(),
          ERROR_CODE.DEV_DEL_DEVICE_NO_ID_EXIST.getName());
    }
    R<?> intercepted = beforeDownAction(request.getProduct(), request.getData(), request);
    if (intercepted != null) return intercepted;
    IoTDeviceDTO snapshot = deviceService.selectDevInstanceBO(
        request.getProductKey(), request.getDeviceId());
    if (deviceService.delDevInstance(device.getIotId()) <= 0) return R.error("设备删除失败");
    lifecycle.delete(snapshot, request);
    return R.ok("删除成功");
  }

  private void fillArea(IoTDevice device, DownCommonData data) {
    if (StrUtil.isNotBlank(data.getLongitude()) && StrUtil.isNotBlank(data.getLatitude())) {
      areas.fillDeviceArea(device, data.getLongitude(), data.getLatitude());
    }
  }

  private IoTDevice identity(HttpDownRequest request) {
    return IoTDevice.builder().productKey(request.getProductKey()).deviceId(request.getDeviceId()).build();
  }

  private Map<String, Object> result(IoTDevice device) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("iotId", device.getIotId());
    result.put("deviceId", device.getDeviceId());
    result.put("productKey", device.getProductKey());
    result.put("areasId", device.getAreasId() == null ? "" : device.getAreasId());
    return result;
  }

  private DownCmd canonical(DownCmd cmd) {
    return switch (cmd) {
      case DEV_ADD, DEV_ADDS, DEVICE_ADD -> DownCmd.DEV_ADD;
      case DEV_DEL, DEVICE_DELETE -> DownCmd.DEV_DEL;
      case DEV_UPDATE, DEVICE_UPDATE -> DownCmd.DEV_UPDATE;
      default -> throw new IllegalArgumentException("Unsupported HTTP lifecycle command: " + cmd);
    };
  }
}
