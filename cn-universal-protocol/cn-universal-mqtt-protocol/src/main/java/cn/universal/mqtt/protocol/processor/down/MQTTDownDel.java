package cn.universal.mqtt.protocol.processor.down;

import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.common.constant.IoTConstant.ERROR_CODE;
import cn.universal.common.domain.R;
import cn.universal.core.message.DownRequest;
import cn.universal.dm.device.service.AbstratIoTService;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.mqtt.protocol.entity.MQTTDownRequest;
import cn.universal.mqtt.protocol.processor.MQTTDownMessageProcessor;
import cn.universal.persistence.base.IoTDeviceLifeCycle;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.IoTDevice;
import cn.universal.persistence.mapper.IoTDeviceMapper;
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
public class MQTTDownDel extends AbstratIoTService implements MQTTDownMessageProcessor {

  private String CUSTOM_FIELD = "customField";

  @Resource(name = "ioTDeviceActionAfterService")
  private IoTDeviceLifeCycle ioTDeviceLifeCycle;

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
    IoTDevice instance = ioTDeviceMapper.selectOne(ioTDevice);
    if (instance == null) {
      // 设备不存在
      return R.error(
          ERROR_CODE.DEV_DEL_DEVICE_NO_ID_EXIST.getCode(),
          ERROR_CODE.DEV_DEL_DEVICE_NO_ID_EXIST.getName());
    }
    R preR =
        beforeDownAction(downRequest.getIoTProduct(), downRequest.getDownCommonData(), downRequest);
    if (Objects.nonNull(preR)) {
      return preR;
    }
    // 操作数据库
    boolean isSuccess = deleteDevInstance(instance, downRequest);
    // TODO 设置集群标记、清除缓存消息标记、清理mqtt、连接
    if (isSuccess) {
      return R.ok("删除成功");
    }
    return R.error("删除失败");
  }

  /** */
  private boolean deleteDevInstance(IoTDevice ioTDevice, DownRequest downRequest) {
    Map<String, Object> objectMap = new HashMap<>();
    objectMap.put("iotId", ioTDevice.getIotId());
    IoTDeviceDTO ioTDeviceDTO = iotDeviceService.selectDevInstanceBO(objectMap);
    int success = iotDeviceService.delDevInstance(ioTDevice.getIotId());
    if (success > 0) {
      // 设备生命周期-删除
      ioTDeviceLifeCycle.delete(ioTDeviceDTO, downRequest);
      return true;
    }
    return false;
  }

  @Override
  public boolean supports(MQTTDownRequest request) {
    if (!DownCmd.DEV_DEL.equals(request.getCmd())) {
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
