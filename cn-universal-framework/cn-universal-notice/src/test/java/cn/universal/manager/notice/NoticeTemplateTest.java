package cn.universal.manager.notice;

import cn.hutool.json.JSONObject;
import cn.universal.manager.notice.util.NoticeTemplateUtil;
import java.util.Map;
import org.junit.Test;

/**
 * @version 1.0
 * @since 2025/10/21 10:11
 */
public class NoticeTemplateTest {
  @Test
  public void maixn() {
    String str = "*";
    JSONObject cc = new JSONObject();
    cc.set("czx", "*****");
    System.out.println(cc.toString().contains(str));
  }

  @Test
  public void template() {
    String template =
        "#{event.sax} | #{event},天黑了，改关灯睡觉了~~#{status}-#{deviceId}-#{productKey},#{properties.meterNo},#{properties.platform.key}";
    String param =
        """
        {
           "deviceId": "30140250803096",
           "deviceName": "林家新区5-1-1206",
           "deviceNode": "DEVICE",
           "iotId": "cd15f57afc6c4c4abc6bf55778608d00",
           "messageType": "PROPERTIES",
           "productKey": "66ececdf6b00748064184fcf",
           "properties": {
              "communityName": "--",
              "houseName": "--",
              "meterData": "0.19",
              "meterNo": "30140250803096",
              "meterPrice": "--",
              "onlineState": "在线",
              "platform": {
                 "key": "ctaiot"
              },
              "reportTime": "2025-05-29 09:50:23",
              "waterSurplus": "-0.01"
           },
           "time": 1748484602422,
           "userUnionId": "tonglu_192"
        }
    """;
    Map<String, Object> paremMap = NoticeTemplateUtil.parseJson(param);
    String context = NoticeTemplateUtil.replaceNestedParams(template, paremMap);
    System.out.println(context);
  }
}
