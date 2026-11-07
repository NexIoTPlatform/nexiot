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

package cn.universal.dm.device.service.task;

import cn.universal.common.constant.IoTConstant;
import cn.universal.common.utils.DingTalkUtil;
import jakarta.annotation.Resource;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TcpErrorNoticeTask {

  @Resource private StringRedisTemplate stringRedisTemplate;
  private final String KEY = "tcpErrorNoticeTask";

  @Scheduled(cron = "0 55 8 * *  ? ")
  public void doTcpErrorNotice() {
    // 分布式锁,只执行1次，必须设置失效时间
    boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(KEY, "0", 5, TimeUnit.MINUTES);
    if (!flag) {
      return;
    }
    log.info("开始执行tcp连接错误次数通知");
    Map<Object, Object> entries =
        stringRedisTemplate.opsForHash().entries(IoTConstant.TCP_ERROR_MONITOR);
    String msg = "tcp异常连接数统计:\n";
    Iterator<Entry<Object, Object>> iterable = entries.entrySet().iterator();
    if (!iterable.hasNext()) {
      log.info("结束tcp连接错误次数通知");
      return;
    }
    while (iterable.hasNext()) {
      Map.Entry<Object, Object> entry = iterable.next();
      String host = entry.getKey().toString();
      Integer num = Integer.parseInt(entry.getValue().toString());
      msg = msg + String.format("ip=[ %s ]进入黑名单后重复连接[ %d ]次", host, num) + "\n";
    }
    DingTalkUtil.send(msg);
    stringRedisTemplate.delete(IoTConstant.TCP_ERROR_MONITOR);
    log.info("结束tcp连接错误次数通知");
  }
}
