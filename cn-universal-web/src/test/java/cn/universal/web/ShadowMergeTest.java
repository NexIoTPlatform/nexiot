/*
 *
 * Copyright (c) 2025, NexIoT. All Rights Reserved.
 *
 * @Description: 影子数据合并测试
 * @Author: gitee.com/NexIoT
 * @Email: wo8335224@gmail.com
 * @Wechat: outlookFil
 *
 */

package cn.universal.web;

import static org.junit.jupiter.api.Assertions.*;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;

/**
 * 影子数据合并测试 用于测试 mergeShadowJson 方法的正确性
 *
 * @author gitee.com/NexIoT
 * @version 1.0
 * @since 2025/1/15
 */
public class ShadowMergeTest {

  /** 模拟 mergeShadowJson 方法的实现 用于测试验证 */
  private String mergeShadowJson(String existingJson, String incomingJson) {
    if (incomingJson == null || incomingJson.trim().isEmpty()) {
      return existingJson == null || existingJson.trim().isEmpty() ? "{}" : existingJson;
    }
    if (existingJson == null || existingJson.trim().isEmpty()) {
      return incomingJson;
    }

    JSONObject existing;
    JSONObject incoming;
    try {
      existing = JSONUtil.parseObj(existingJson);
    } catch (Exception e) {
      existing = new JSONObject();
    }
    try {
      incoming = JSONUtil.parseObj(incomingJson);
    } catch (Exception e) {
      return existingJson;
    }

    JSONObject merged = deepMergeObject(existing, incoming);

    // 顶层特殊字段处理
    try {
      Long tsExisting = merged.getLong("timestamp");
      Long tsIncoming = incoming.getLong("timestamp");
      if (tsExisting != null || tsIncoming != null) {
        long maxTs =
            Math.max(tsExisting == null ? 0L : tsExisting, tsIncoming == null ? 0L : tsIncoming);
        merged.set("timestamp", maxTs);
      }
    } catch (Exception ignore) {
      // ignore
    }

    try {
      Long verExisting = merged.getLong("version");
      Long verIncoming = incoming.getLong("version");
      if (verExisting != null || verIncoming != null) {
        long maxVer =
            Math.max(
                verExisting == null ? 0L : verExisting, verIncoming == null ? 0L : verIncoming);
        merged.set("version", maxVer);
      }
    } catch (Exception ignore) {
      // ignore
    }

    return JSONUtil.toJsonStr(merged);
  }

  /** 对 JSONObject 进行递归深度合并 */
  private JSONObject deepMergeObject(JSONObject base, JSONObject incoming) {
    if (base == null) {
      return incoming == null ? new JSONObject() : incoming;
    }
    if (incoming == null) {
      return base;
    }
    JSONObject result = JSONUtil.parseObj(base.toString());
    for (String key : incoming.keySet()) {
      Object inVal = incoming.get(key);
      Object baseVal = result.get(key);

      if (inVal instanceof JSONObject && baseVal instanceof JSONObject) {
        result.set(key, deepMergeObject((JSONObject) baseVal, (JSONObject) inVal));
      } else {
        // 数组或标量：直接覆盖
        result.set(key, inVal);
      }
    }
    return result;
  }

  @Test
  void testMergeShadowJson_EmptyIncoming() {
    String existing = "{\"device\":{\"temperature\":25.5,\"humidity\":60}}";
    String incoming = "";

    String result = mergeShadowJson(existing, incoming);

    assertEquals(existing, result);
  }

  @Test
  void testMergeShadowJson_EmptyExisting() {
    String existing = "";
    String incoming = "{\"device\":{\"temperature\":30.0,\"humidity\":70}}";

    String result = mergeShadowJson(existing, incoming);

    assertEquals(incoming, result);
  }

  @Test
  void testMergeShadowJson_BothEmpty() {
    String existing = "";
    String incoming = "";

    String result = mergeShadowJson(existing, incoming);

    assertEquals("{}", result);
  }

  @Test
  void testMergeShadowJson_SimpleMerge() {
    String existing = "{\"device\":{\"temperature\":25.5,\"humidity\":60}}";
    String incoming = "{\"device\":{\"temperature\":30.0,\"pressure\":1013}}";

    String result = mergeShadowJson(existing, incoming);

    JSONObject resultObj = JSONUtil.parseObj(result);
    JSONObject device = resultObj.getJSONObject("device");

    assertEquals(30.0, device.getDouble("temperature"));
    assertEquals(60, device.getInt("humidity"));
    assertEquals(1013, device.getInt("pressure"));
  }

  @Test
  void testMergeShadowJson_TimestampMerge() {
    String existing = "{\"timestamp\":1000,\"device\":{\"temperature\":25.5}}";
    String incoming = "{\"timestamp\":2000,\"device\":{\"humidity\":70}}";

    String result = mergeShadowJson(existing, incoming);

    JSONObject resultObj = JSONUtil.parseObj(result);
    assertEquals(2000L, resultObj.getLong("timestamp"));
    assertEquals(25.5, resultObj.getJSONObject("device").getDouble("temperature"));
    assertEquals(70, resultObj.getJSONObject("device").getInt("humidity"));
  }

  @Test
  void testMergeShadowJson_VersionMerge() {
    String existing = "{\"version\":5,\"device\":{\"temperature\":25.5}}";
    String incoming = "{\"version\":3,\"device\":{\"humidity\":70}}";

    String result = mergeShadowJson(existing, incoming);

    JSONObject resultObj = JSONUtil.parseObj(result);
    assertEquals(5L, resultObj.getLong("version"));
    assertEquals(25.5, resultObj.getJSONObject("device").getDouble("temperature"));
    assertEquals(70, resultObj.getJSONObject("device").getInt("humidity"));
  }

  @Test
  void testMergeShadowJson_ComplexNestedMerge() {
    String existing =
        """
            {
                "device": {
                    "sensors": {
                        "temperature": 25.5,
                        "humidity": 60
                    },
                    "status": "online"
                },
                "metadata": {
                    "location": "room1"
                }
            }
            """;

    String incoming =
        """
            {
                "device": {
                    "sensors": {
                        "temperature": 30.0,
                        "pressure": 1013
                    },
                    "battery": 85
                },
                "metadata": {
                    "location": "room2",
                    "floor": 1
                }
            }
            """;

    String result = mergeShadowJson(existing, incoming);

    JSONObject resultObj = JSONUtil.parseObj(result);
    JSONObject device = resultObj.getJSONObject("device");
    JSONObject sensors = device.getJSONObject("sensors");
    JSONObject metadata = resultObj.getJSONObject("metadata");

    // 验证合并结果
    assertEquals(30.0, sensors.getDouble("temperature")); // 被覆盖
    assertEquals(60, sensors.getInt("humidity")); // 保留
    assertEquals(1013, sensors.getInt("pressure")); // 新增
    assertEquals("online", device.getStr("status")); // 保留
    assertEquals(85, device.getInt("battery")); // 新增
    assertEquals("room2", metadata.getStr("location")); // 被覆盖
    assertEquals(1, metadata.getInt("floor")); // 新增
  }

  @Test
  void testMergeShadowJson_ArrayOverwrite() {
    String existing = "{\"tags\":[\"tag1\",\"tag2\"],\"device\":{\"temperature\":25.5}}";
    String incoming = "{\"tags\":[\"tag3\",\"tag4\",\"tag5\"],\"device\":{\"humidity\":70}}";

    String result = mergeShadowJson(existing, incoming);

    JSONObject resultObj = JSONUtil.parseObj(result);
    assertTrue(resultObj.getJSONArray("tags").contains("tag3"));
    assertTrue(resultObj.getJSONArray("tags").contains("tag4"));
    assertTrue(resultObj.getJSONArray("tags").contains("tag5"));
    assertFalse(resultObj.getJSONArray("tags").contains("tag1"));
    assertFalse(resultObj.getJSONArray("tags").contains("tag2"));
  }

  @Test
  void testMergeShadowJson_InvalidJson() {
    String existing = "{\"device\":{\"temperature\":25.5}}";
    String incoming = "invalid json";

    String result = mergeShadowJson(existing, incoming);

    assertEquals(existing, result);
  }

  @Test
  void testMergeShadowJson_RealWorldScenario() {
    // 真实场景测试：设备影子数据合并
    String existing =
        """
            {
                "timestamp": 1640995200000,
                "version": 1,
                "state": {
                    "reported": {
                        "temperature": 25.5,
                        "humidity": 60,
                        "status": "online"
                    },
                    "desired": {
                        "target_temp": 22.0
                    }
                },
                "metadata": {
                    "device_type": "sensor",
                    "location": "room1"
                }
            }
            """;

    String incoming =
        """
            {
                "timestamp": 1640995300000,
                "version": 2,
                "state": {
                    "reported": {
                        "temperature": 26.0,
                        "pressure": 1013,
                        "battery": 85
                    },
                    "desired": {
                        "target_temp": 23.0,
                        "fan_speed": "high"
                    }
                },
                "metadata": {
                    "device_type": "sensor",
                    "location": "room2",
                    "floor": 1
                }
            }
            """;

    String result = mergeShadowJson(existing, incoming);

    JSONObject resultObj = JSONUtil.parseObj(result);

    // 验证时间戳和版本号（取较大值）
    assertEquals(1640995300000L, resultObj.getLong("timestamp"));
    assertEquals(2L, resultObj.getLong("version"));

    // 验证reported状态合并
    JSONObject reported = resultObj.getJSONObject("state").getJSONObject("reported");
    assertEquals(26.0, reported.getDouble("temperature")); // 被覆盖
    assertEquals(60, reported.getInt("humidity")); // 保留
    assertEquals("online", reported.getStr("status")); // 保留
    assertEquals(1013, reported.getInt("pressure")); // 新增
    assertEquals(85, reported.getInt("battery")); // 新增

    // 验证desired状态合并
    JSONObject desired = resultObj.getJSONObject("state").getJSONObject("desired");
    assertEquals(23.0, desired.getDouble("target_temp")); // 被覆盖
    assertEquals("high", desired.getStr("fan_speed")); // 新增

    // 验证metadata合并
    JSONObject metadata = resultObj.getJSONObject("metadata");
    assertEquals("sensor", metadata.getStr("device_type")); // 保留
    assertEquals("room2", metadata.getStr("location")); // 被覆盖
    assertEquals(1, metadata.getInt("floor")); // 新增

    // 打印结果用于调试
    System.out.println("合并结果:");
    System.out.println(JSONUtil.toJsonPrettyStr(resultObj));
  }
}
