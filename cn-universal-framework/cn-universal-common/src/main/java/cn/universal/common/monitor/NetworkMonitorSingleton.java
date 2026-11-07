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

package cn.universal.common.monitor;

import cn.hutool.core.util.StrUtil;
import cn.universal.common.utils.DingTalkUtil;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/** 网络监测单例 */
@Slf4j
public class NetworkMonitorSingleton {

  private NetworkMonitorSingleton() {}

  private static class SingleTonHolder {

    private static NetworkMonitorSingleton INSTANCE = new NetworkMonitorSingleton();
  }

  public static NetworkMonitorSingleton getInstance() {
    return SingleTonHolder.INSTANCE;
  }

  private static volatile Map<String, Boolean> networkMap = new HashMap<>();

  public static void freshConnectionStatus(String serverURI, Boolean status) {
    if (StrUtil.isBlank(serverURI)) {
      return;
    }
    boolean lastStatusNormal =
        !networkMap.containsKey(serverURI) || networkMap.get(serverURI).equals(Boolean.TRUE);
    if (lastStatusNormal && Boolean.FALSE.equals(status)) {
      DingTalkUtil.send("IoT Universal，[" + serverURI + "] 断开异常!");
      log.warn("[网络监控] 服务断开: serverURI={}, status={},请注意！", serverURI, status);
    }
    boolean lastStatusAbnormal =
        !networkMap.containsKey(serverURI) || networkMap.get(serverURI).equals(Boolean.FALSE);
    if (lastStatusAbnormal && Boolean.TRUE.equals(status)) {
      DingTalkUtil.send("IoT Universal，指标[" + serverURI + "] 状态正常！！!");
    }
    if (!networkMap.containsKey(serverURI)) {
      networkMap.put(serverURI, status);
    }
    if (!networkMap.get(serverURI).equals(status)) {
      networkMap.put(serverURI, status);
    }
  }

  public static void removeMosquittoServer(String serverId) {
    NetworkMonitorSingleton.networkMap.remove(serverId);
  }

  public static Map<String, Boolean> getConnectionStatus() {
    return networkMap;
  }
}
