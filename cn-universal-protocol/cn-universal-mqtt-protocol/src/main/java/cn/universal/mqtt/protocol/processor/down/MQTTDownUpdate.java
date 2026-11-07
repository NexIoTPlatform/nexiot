package cn.universal.mqtt.protocol.processor.down;

import cn.hutool.core.util.StrUtil;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.constant.IoTConstant.ERROR_CODE;
import cn.universal.common.domain.R;
import cn.universal.dm.device.service.AbstratIoTService;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.mqtt.protocol.entity.MQTTDownRequest;
import cn.universal.mqtt.protocol.processor.MQTTDownMessageProcessor;
import cn.universal.persistence.base.IoTDeviceLifeCycle;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.mapper.IoTDeviceMapper;
import cn.universal.persistence.service.IoTDeviceAreaService;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * tcp 设备增加
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/7/10 10:40
 */
@Component
@Slf4j(topic = "mqtt")
public class MQTTDownUpdate extends AbstratIoTService implements MQTTDownMessageProcessor {

  private String CUSTOM_FIELD = "customField";

  @Resource(name = "ioTDeviceActionAfterService")
  private IoTDeviceLifeCycle ioTDeviceLifeCycle;

  @Resource private IoTDeviceAreaService ioTDeviceAreaService;
  @Resource private StringRedisTemplate stringRedisTemplate;

  @Resource private IoTDeviceService iotDeviceService;
  @Resource private IoTDeviceMapper ioTDeviceMapper;

  @Override
  public R<?> process(MQTTDownRequest downRequest) {
    IoTDevice ioTDevice =
        IoTDevice.builder()
            .productKey(downRequest.getProductKey())
            .deviceId(downRequest.getDeviceId())
            .build();
    R preR =
        beforeDownAction(downRequest.getIoTProduct(), downRequest.getDownCommonData(), downRequest);
    if (Objects.nonNull(preR)) {
      return preR;
    }
    IoTDevice ioTDeviceOne = ioTDeviceMapper.selectOne(ioTDevice);
    int size = ioTDeviceMapper.selectCount(ioTDevice);
    if (size == 0) {
      // 设备不存在
      return R.error(
          ERROR_CODE.DEV_UPDATE_DEVICE_NO_ID_EXIST.getCode(),
          ERROR_CODE.DEV_UPDATE_DEVICE_NO_ID_EXIST.getName());
    }
    ioTDeviceOne.setDeviceName(downRequest.getDownCommonData().getDeviceName());
    return updateDevInstance(ioTDeviceOne, downRequest);
  }

  /**
   * 修改本地数据库设备 详见文档 https://apiportalweb.ctwing.cn/index.html#/apiDetail/10255/218/1001
   * gitee.com/NexIoT
   */
  private R<?> updateDevInstance(IoTDevice ioTDevice, MQTTDownRequest downRequest) {
    if (StrUtil.isNotBlank(downRequest.getDownCommonData().getLatitude())
        && StrUtil.isNotBlank(downRequest.getDownCommonData().getLongitude())) {
      ioTDeviceAreaService.fillDeviceArea(
          ioTDevice,
          downRequest.getDownCommonData().getLongitude(),
          downRequest.getDownCommonData().getLatitude());
    }
    // 组件返回字段
    Map<String, Object> result = new HashMap<>();
    result.put("deviceId", ioTDevice.getDeviceId());
    result.put("areasId", ioTDevice.getAreasId() == null ? "" : ioTDevice.getAreasId());
    if(StrUtil.isNotBlank(downRequest.getDeviceAddress())) {
      ioTDevice.setDeviceAddress(downRequest.getDeviceAddress());
    }
    if(StrUtil.isNotBlank(downRequest.getDetail())) {
      ioTDevice.setDetail(downRequest.getDetail());
    }
    finalDown_Update(ioTDevice, downRequest);
    int count = ioTDeviceMapper.updateByPrimaryKey(ioTDevice);
    if (count > 0) {
      // 设备生命周期-修改
      ioTDeviceLifeCycle.update(ioTDevice.getProductKey(), ioTDevice.getDeviceId(), downRequest);
      result.put("success", true);
      return R.ok(result);
    }
    return R.error("删除失败");
  }

  @Override
  public boolean supports(MQTTDownRequest request) {
    if (!DownCmd.DEV_UPDATE.equals(request.getCmd())) {
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
