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

package cn.universal.rule.rulego;

import static org.junit.jupiter.api.Assertions.*;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.databridge.util.ConnectionTester;
import cn.universal.rule.model.RuleTarget;
import cn.universal.rule.model.bo.RuleBo;
import cn.universal.rule.transmit.RuleTransmitTemplate;
import cn.universal.rule.transmit.impl.RuleHttpTransmitStrategy;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 连接测试器测试类
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
public class RuleTest {

  private final ConnectionTester connectionTester = new ConnectionTester();

  @Test
  public void testMySQLConnection() {
    String parem =
        """
    {"id":"1978752281910370304","ruleName":"测试","description":null,"dataLevel":"device","productKey":"RpZ3VQIa6YE2","status":"stop","payload":"{\\n  \\"time\\": 1644303132765,\\n  \\"iotId\\": \\"ae0e2153ef644d5ea2f61668c7ac38d6\\",\\n  \\"deviceId\\": \\"1234567890\\",\\n  \\"deviceName\\": \\"设备名称\\",\\n  \\"deviceNode\\": \\"DEVICE\\",\\n  \\"productKey\\": \\"产品编号\\",\\n  \\"properties\\": {\\n    \\"battery\\": \\"70\\",\\n    \\"temperature\\": 100\\n  },\\n  \\"messageType\\": \\"PROPERTIES\\"\\n}","groupId":null,"relationIds":[null],"config":{"appId":"*","fields":"*","targets":[],"condition":"properties.temperature &gt; 120"},"creatorId":"test","createTime":"2025-10-16 17:18:02"}
    """;
    RuleBo bean = BeanUtil.toBean(parem, RuleBo.class);
    JSONObject payload = JSONUtil.parseObj(bean.getPayload());
    String str =
        """
        {
          "time": 1644303132765,
          "iotId": "ae0e2153ef644d5ea2f61668c7ac38d6",
          "deviceId": "1234567890",
          "deviceName": "设备名称",
          "deviceNode": "DEVICE",
          "productKey": "产品编号",
          "properties": {
            "battery": "70",
            "temperature": 100
          },
          "messageType": "PROPERTIES"
        }

        """;
    JSONObject jsonObject = JSONUtil.parseObj(parem);
    RuleHttpTransmitStrategy ruleHttpTransmitStrategy = new RuleHttpTransmitStrategy();
    RuleTransmitTemplate template =
        new RuleTransmitTemplate(
            Stream.of(ruleHttpTransmitStrategy).collect(Collectors.toUnmodifiableList()));
    RuleTarget target = new RuleTarget();
    target.setId("1");
    target.setType("http");
    target.setUrl("sss");
    template.testTransmit(jsonObject, target);
  }
}
