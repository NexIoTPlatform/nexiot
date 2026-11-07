package cn.universal.mqtt.protocol.processor.down;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant;
import cn.universal.common.constant.IoTConstant.DeviceStatus;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.constant.IoTConstant.ERROR_CODE;
import cn.universal.common.domain.R;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.mqtt.protocol.entity.MQTTDownRequest;
import cn.universal.mqtt.protocol.processor.MQTTDownMessageProcessor;
import cn.universal.persistence.base.DownRequestDataHelper;
import cn.universal.persistence.base.IoTDeviceLifeCycle;
import cn.universal.persistence.base.IoTDownAdapter;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.service.IoTDeviceAreaService;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * mqtt 设备增加
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/7/10 10:40
 */
@Component
@Slf4j(topic = "mqtt")
public class MQTTDownAdd extends IoTDownAdapter<MQTTDownRequest>
    implements MQTTDownMessageProcessor {

  private String CUSTOM_FIELD = "customField";

  @Resource(name = "ioTDeviceActionAfterService")
  private IoTDeviceLifeCycle ioTDeviceLifeCycle;

  @Resource private IoTDeviceAreaService ioTDeviceAreaService;
  @Resource private IoTDeviceMapper ioTDeviceMapper;
  @Autowired private IoTProductDeviceService iotProductDeviceService;

  @Override
  public R<?> process(MQTTDownRequest downRequest) {
    /** 当编解码请求结果为空 或者 编解码请求结果不为空且状态码等于0时，添加设备到本地 */
    log.info(
        "添加mqtt设备,deviceId={},productKey={}",
        downRequest.getDeviceId(),
        downRequest.getProductKey());
    R preR =
        beforeDownAction(downRequest.getIoTProduct(), downRequest.getDownCommonData(), downRequest);
    if (Objects.nonNull(preR)) {
      return preR;
    }
    IoTDevice ioTDevice =
        IoTDevice.builder()
            .productKey(downRequest.getProductKey())
            .deviceId(downRequest.getDeviceId())
            .build();
    int size = ioTDeviceMapper.selectCount(ioTDevice);
    if (size > 0) {
      // 设备已经存在
      return R.error(
          ERROR_CODE.DEV_ADD_DEVICE_ID_EXIST.getCode(),
          ERROR_CODE.DEV_ADD_DEVICE_ID_EXIST.getName());
    }
    // 操作数据库
    R<?> rs = saveIoTDevice(downRequest);
    return rs;
  }

  private R<?> saveIoTDevice(MQTTDownRequest downRequest) {
    IoTDevice ioTDevice =
        IoTDevice.builder()
            .deviceId(downRequest.getDeviceId())
            .createTime(System.currentTimeMillis() / 1000)
            .deviceSecret(
                MD5.create()
                    .digestHex(
                        downRequest.getProductKey() + downRequest.getDeviceId() + IoTConstant.SALT))
            .deviceName(downRequest.getDownCommonData().getDeviceName())
            .state(DeviceStatus.offline.getCode())
            .iotId(downRequest.getProductKey() + downRequest.getDeviceId())
            .application(downRequest.getApplicationId())
            .deviceAddress(downRequest.getDeviceAddress())
            .creatorId(downRequest.getAppUnionId())
            .productName(downRequest.getIoTProduct().getName())
            .detail(downRequest.getDetail())
            .gwProductKey(downRequest.getGwProductKey())
            .extDeviceId(downRequest.getGwDeviceId())
            .productKey(downRequest.getProductKey())
            .build();

    Object extDeviceId =
        DownRequestDataHelper.getFieldValue(downRequest, downRequest.getData(), "extDeviceId");
    if (extDeviceId != null && StrUtil.isNotBlank(String.valueOf(extDeviceId))) {
      ioTDevice.setExtDeviceId(String.valueOf(extDeviceId));
    }

    if (StrUtil.isNotBlank(downRequest.getDownCommonData().getLatitude())
        && StrUtil.isNotBlank(downRequest.getDownCommonData().getLongitude())) {
      ioTDeviceAreaService.fillDeviceArea(
          ioTDevice,
          downRequest.getDownCommonData().getLongitude(),
          downRequest.getDownCommonData().getLatitude());
    }
    Map<String, Object> config = new HashMap<>();
    finalDown_Add(config, downRequest.getIoTProduct(), downRequest, downRequest.getData());
    ioTDevice.setConfiguration(JSONUtil.toJsonStr(config));
    IoTDownAdapter.finalDown_Add(ioTDevice, downRequest.getIoTProduct(), downRequest);
    int success = ioTDeviceMapper.insertUseGeneratedKeys(ioTDevice);
    if (success > 0) {
      // 推送设备创建消息
      ioTDeviceLifeCycle.create(
          downRequest.getProductKey(), downRequest.getDeviceId(), downRequest);
      // 组件返回字段
      Map<String, Object> result = new HashMap<>();
      result.put("iotId", ioTDevice.getIotId());
      result.put("areasId", ioTDevice.getAreasId() == null ? "" : ioTDevice.getAreasId());
      result.put("deviceSecret",ioTDevice.getDeviceSecret());
      result.put("productKey", downRequest.getProductKey());
      result.put("productName",downRequest.getIoTProduct().getName());
      result.put("deviceNode", downRequest.getIoTProduct().getDeviceNode());
      result.put("configuration",ioTDevice.getConfiguration());
      return R.ok(result);
    } else {
      return R.error("设备增加失败");
    }
  }

  @Override
  public boolean supports(MQTTDownRequest request) {
    if (!DownCmd.DEV_ADD.equals(request.getCmd())) {
      return false;
    }
    return true;
  }

  @Override
  public String getName() {
    return "";
  }

  @Override
  public int getOrder() {
    return 0;
  }
}
