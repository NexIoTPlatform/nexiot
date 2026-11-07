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

package cn.universal.web;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.CnUniversalIoTApplication;
import cn.universal.core.metadata.DeviceMetadata;
import cn.universal.dm.device.service.impl.IoTDeviceService;
import cn.universal.dm.device.service.impl.IoTProductDeviceService;
import cn.universal.dm.device.service.task.ShadowFlushScheduler;
import cn.universal.persistence.dto.IoTDeviceDTO;
import cn.universal.persistence.entity.SceneLinkage;
import cn.universal.persistence.mapper.SceneLinkageMapper;
import cn.universal.rule.engine.RuleEngine;
import cn.universal.rule.model.bo.RuleBo;
import cn.universal.rule.service.RuleService;
import jakarta.annotation.Resource;
import java.security.SecureRandom;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CnUniversalIoTApplication.class)
@ActiveProfiles("dev")
public class CnUniversalTest {

  @Resource private LoggingSystem loggingSystem;

  @Resource private ShadowFlushScheduler shadowFlushScheduler;

  @Resource private SceneLinkageMapper sceneLinkageMapper;
  @Resource private RuleService ruleService;
  @Resource private RuleEngine ruleEngine;

  @Before
  public void before() {
    loggingSystem.setLogLevel("com.tk.mybatis", LogLevel.DEBUG);
    loggingSystem.setLogLevel("cn.universal", LogLevel.DEBUG);
    loggingSystem.setLogLevel("cn.hutool", LogLevel.DEBUG);
  }



  @Test
  public void ruleTesttarget() {
    String params =
        """
            {"id":"1978752281910370304","ruleName":"测试","description":null,"dataLevel":"device","productKey":"RpZ3VQIa6YE2","status":"stop","payload":"{\\n  \\"time\\": 1644303132765,\\n  \\"iotId\\": \\"ae0e2153ef644d5ea2f61668c7ac38d6\\",\\n  \\"deviceId\\": \\"1234567890\\",\\n  \\"deviceName\\": \\"设备名称\\",\\n  \\"deviceNode\\": \\"DEVICE\\",\\n  \\"productKey\\": \\"产品编号\\",\\n  \\"properties\\": {\\n    \\"battery\\": \\"70\\",\\n    \\"temperature\\": 100\\n  },\\n  \\"messageType\\": \\"PROPERTIES\\"\\n}","groupId":null,"relationIds":[null],"config":{"appId":"*","fields":"*","targets":[],"condition":"properties.temperature &gt; 120"},"creatorId":"test","createTime":"2025-10-16 17:18:02"}
            """;
    // &lt;
    RuleBo bean = BeanUtil.toBean(JSONUtil.parseObj(params), RuleBo.class);
    JSONObject jsonObject =
        ruleEngine.executeRule(
            JSONUtil.parseObj(bean.getPayload()),
            "select * from * where  properties.temperature>33 && properties.battery==40",
            "12333");
    System.out.println(JSONUtil.toJsonStr(jsonObject));
    //    String str =
    //        """
    //                {
    //                  "time": 1644303132765,
    //                  "iotId": "ae0e2153ef644d5ea2f61668c7ac38d6",
    //                  "deviceId": "1234567890",
    //                  "deviceName": "设备名称",
    //                  "deviceNode": "DEVICE",
    //                  "productKey": "产品编号",
    //                  "properties": {
    //                    "battery": "70",
    //                    "temperature": 100
    //                  },
    //                  "messageType": "PROPERTIES"
    //                }
    //
    //                """;
    //    JSONObject jsonObject = JSONUtil.parseObj(parem);
    //    RuleHttpTransmitStrategy ruleHttpTransmitStrategy = new RuleHttpTransmitStrategy();
    //    RuleTransmitTemplate template =
    //        new RuleTransmitTemplate(
    //            Stream.of(ruleHttpTransmitStrategy).collect(Collectors.toUnmodifiableList()));
    //    RuleTarget target = new RuleTarget();
    //    target.setId("1");
    //    target.setType("http");
    //    target.setUrl("sss");
    //    template.testTransmit(jsonObject, target);
  }

  @Test
  public void sceneLinkage() throws Exception {
    List<SceneLinkage> p0B2kKqdJD8M =
        sceneLinkageMapper.selectSceneLinkageListByProductKeyAndDeviceId(
            "p0B2kKqdJD8M", "860048070262660-2");
    System.out.println(JSONUtil.toJsonStr(p0B2kKqdJD8M));
  }

  @Test
  public void testCount() throws Exception {
    Assert.assertTrue(true);
  }

  @Test
  public void zxcvb() throws Exception {
    shadowFlushScheduler.flushDueShadows();
  }

  @Autowired private IoTProductDeviceService ioTProductDeviceService;

  @Test
  public void properties() throws Exception {
    DeviceMetadata zxc = ioTProductDeviceService.getDeviceMetadata("Ru871cfJjhoM");
    System.out.println(zxc.getProperties());
  }

  @Resource private IoTDeviceService ioTDeviceService;

  @Test
  public void ioTDeviceDTO() throws Exception {
    IoTDeviceDTO ioTDeviceDTO =
        ioTDeviceService.selectDevInstanceBO("FzpVZ9unsUvU", "860048070262660-9");
    System.out.println(JSONUtil.toJsonStr(ioTDeviceDTO));
  }
}
