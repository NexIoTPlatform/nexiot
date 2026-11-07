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

package cn.universal.persistence.base;

import cn.universal.core.message.DownRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class IoTDeviceExtendService {

  private final Map<String, IoTDeviceExtendDTO> deviceExtMap;

  public IoTDeviceExtendService(List<IoTDeviceExtendDTO> deviceExtList) {
    deviceExtMap =
        deviceExtList.stream()
            .collect(Collectors.toMap(IoTDeviceExtendDTO::productKey, Function.identity()));
  }

  public void downExt(DownRequest downRequest) {
    IoTDeviceExtendDTO ioTDeviceExtendDTO = deviceExtMap.get(downRequest.getProductKey());
    if (Objects.nonNull(ioTDeviceExtendDTO)) {
      ioTDeviceExtendDTO.downExt(downRequest);
    }
  }

  public void upExt(BaseUPRequest upRequest) {
    IoTDeviceExtendDTO ioTDeviceExtendDTO = deviceExtMap.get(upRequest.getProductKey());
    if (Objects.nonNull(ioTDeviceExtendDTO)) {
      ioTDeviceExtendDTO.upExt(upRequest);
    }
  }
}
