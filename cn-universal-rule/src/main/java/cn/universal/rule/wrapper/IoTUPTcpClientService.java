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

package cn.universal.rule.wrapper;

import cn.hutool.core.collection.CollectionUtil;
import cn.universal.persistence.base.BaseUPRequest;
import cn.universal.persistence.base.IoTUPWrapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2023/1/20
 */
@Service("iotUPTcpClientService")
@ConditionalOnProperty(prefix = "nexiot.push.tcp-client", name = "enabled", havingValue = "true")
@Slf4j
public class IoTUPTcpClientService implements IoTUPWrapper<BaseUPRequest> {

  // 809协议转发 广播
  @Override
  public void beforePush(List<BaseUPRequest> baseUPRequests) {
    if (CollectionUtil.isEmpty(baseUPRequests)) {
      return;
    }
    //    baseUPRequests.stream().forEach(baseUPRequest -> {
    //      IoTDeviceDTO instanceBO = baseUPRequest.getIoTDeviceDTO();
    //      //tcp 809协议推送第三方
    //      log.info("809协议推送第三方={}", instanceBO);
    //    });
  }
}
