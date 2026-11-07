package cn.universal.dm.downlink;

import static org.junit.jupiter.api.Assertions.*;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.common.constant.IoTConstant.DownCmd;
import cn.universal.core.downlink.DownlinkContext;
import cn.universal.core.message.UnifiedDownlinkCommand;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 测试验证 UnifiedDownlinkCommand 中 function 对象的引用行为
 *
 * <p>主要验证： 1. getFunction() 每次返回的是否是同一个对象引用 2. 修改 data 后是否需要显式回写 3. JSONObject 作为 Map 实现的特殊行为 4.
 * fromJson() 构建的 command 的行为（真实场景）
 *
 * <p>测试结论： - HashMap: 引用一致，修改自动生效 - JSONObject: 需要验证是否每次返回同一引用 - fromJson构建: 完全模拟真实消息处理流程
 */
@DisplayName("下行功能参数引用行为测试")
class DownlinkFunctionReferenceTest {

  @Test
  @DisplayName("测试1: HashMap作为function - 验证引用一致性")
  void testHashMapFunctionReference() {
    // 1. 创建 command，使用普通 HashMap
    UnifiedDownlinkCommand command =
        UnifiedDownlinkCommand.builder()
            .productKey("testProduct")
            .deviceId("testDevice")
            .cmd(DownCmd.DEV_FUNCTION)
            .function(new HashMap<>())
            .build();

    // 初始化 function 数据
    command.getFunction().put("function", "testFunc");
    command.getFunction().put("data", new HashMap<String, Object>());

    System.out.println("\n========== 测试1: HashMap 作为 function ==========");

    // 2. 第一次获取引用
    Map<String, Object> function1 = command.getFunction();
    System.out.println("第一次获取 function 引用: " + System.identityHashCode(function1));
    System.out.println("function1 内容: " + function1);

    // 3. 获取 data 并修改
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) function1.get("data");
    System.out.println("获取 data 引用: " + System.identityHashCode(data));

    data.put("param1", "value1");
    System.out.println("修改后 data 内容: " + data);

    // 4. 第二次获取引用
    Map<String, Object> function2 = command.getFunction();
    System.out.println("第二次获取 function 引用: " + System.identityHashCode(function2));
    System.out.println("function2 内容: " + function2);

    // 5. 验证
    System.out.println("\n--- 验证结果 ---");
    System.out.println("是否是同一个对象: " + (function1 == function2));
    System.out.println("function1.get(\"data\"): " + function1.get("data"));
    System.out.println("function2.get(\"data\"): " + function2.get("data"));

    // 断言：HashMap 情况下，引用应该一致，修改自动生效
    assertTrue(function1 == function2, "HashMap: 两次获取应该是同一个对象");
    assertEquals("value1", ((Map<?, ?>) function2.get("data")).get("param1"), "HashMap: 修改应该自动生效");
  }

  @Test
  @DisplayName("测试2: JSONObject作为function - 验证引用行为")
  void testJSONObjectFunctionReference() {
    // 1. 创建 command，使用 JSONObject（模拟实际场景）
    JSONObject functionJson = new JSONObject();
    functionJson.set("function", "testFunc");
    functionJson.set("data", new JSONObject());

    UnifiedDownlinkCommand command =
        UnifiedDownlinkCommand.builder()
            .productKey("testProduct")
            .deviceId("testDevice")
            .cmd(DownCmd.DEV_FUNCTION)
            .function(functionJson)
            .build();

    System.out.println("\n========== 测试2: JSONObject 作为 function ==========");

    // 2. 第一次获取引用
    Map<String, Object> function1 = command.getFunction();
    System.out.println("第一次获取 function 引用: " + System.identityHashCode(function1));
    System.out.println("function1 类型: " + function1.getClass().getName());
    System.out.println("function1 内容: " + function1);

    // 3. 获取 data 并修改
    Object dataObj = function1.get("data");
    System.out.println("data 类型: " + (dataObj != null ? dataObj.getClass().getName() : "null"));

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) dataObj;
    System.out.println("获取 data 引用: " + System.identityHashCode(data));

    data.put("param1", "value1");
    System.out.println("修改后 data 内容: " + data);

    // 4. 第二次获取引用
    Map<String, Object> function2 = command.getFunction();
    System.out.println("第二次获取 function 引用: " + System.identityHashCode(function2));
    System.out.println("function2 内容: " + function2);

    // 5. 验证
    System.out.println("\n--- 验证结果 ---");
    System.out.println("是否是同一个对象: " + (function1 == function2));
    System.out.println("function1.get(\"data\"): " + function1.get("data"));
    System.out.println("function2.get(\"data\"): " + function2.get("data"));

    @SuppressWarnings("unchecked")
    Map<String, Object> dataFromFunction2 = (Map<String, Object>) function2.get("data");
    System.out.println("function2 中的 data.get(\"param1\"): " + dataFromFunction2.get("param1"));

    // 断言：JSONObject 情况下，引用应该一致，修改应该生效
    assertTrue(function1 == function2, "JSONObject: 两次获取应该是同一个对象");
    assertEquals("value1", dataFromFunction2.get("param1"), "JSONObject: 修改应该自动生效");
  }

  @Test
  @DisplayName("测试3: 模拟拦截器场景 - 不回写的情况")
  void testInterceptorScenarioWithoutPutBack() {
    // 模拟从 JSON 构建 command（真实场景）
    String jsonStr =
        """
            {
                "productKey": "HnDdSnj1kiQg",
                "deviceId": "241031d76ca4",
                "cmd": "DEV_FUNCTION",
                "function": {
                    "function": "fillParames",
                    "data": {}
                }
            }
            """;

    UnifiedDownlinkCommand command = UnifiedDownlinkCommand.fromString(jsonStr);

    System.out.println("\n========== 测试3: 拦截器场景 - 不回写 ==========");
    System.out.println("初始 command.function: " + command.getFunction());

    // 模拟拦截器逻辑
    Map<String, Object> function = command.getFunction();
    System.out.println("获取 function 引用: " + System.identityHashCode(function));

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) function.get("data");
    if (data == null) {
      data = new HashMap<>();
      function.put("data", data);
    }
    System.out.println("获取/创建 data 引用: " + System.identityHashCode(data));

    // 填充参数
    data.put("meterNo", "250707ea8dda");
    System.out.println("填充后 data: " + data);

    // 不回写，直接检查
    Map<String, Object> functionAfter = command.getFunction();
    System.out.println("填充后 command.function: " + functionAfter);

    @SuppressWarnings("unchecked")
    Map<String, Object> dataAfter = (Map<String, Object>) functionAfter.get("data");
    System.out.println("dataAfter: " + dataAfter);
    System.out.println("dataAfter.size(): " + (dataAfter != null ? dataAfter.size() : 0));

    // 验证
    assertNotNull(dataAfter, "data 不应该为 null");
    assertTrue(dataAfter.containsKey("meterNo"), "data 应该包含 meterNo");
    assertEquals("250707ea8dda", dataAfter.get("meterNo"), "meterNo 值应该正确");
  }

  @Test
  @DisplayName("测试4: 模拟拦截器场景 - 显式回写")
  void testInterceptorScenarioWithPutBack() {
    // 模拟从 JSON 构建 command
    String jsonStr =
        """
            {
                "productKey": "HnDdSnj1kiQg",
                "deviceId": "241031d76ca4",
                "cmd": "DEV_FUNCTION",
                "function": {
                    "function": "fillParames",
                    "data": {}
                }
            }
            """;

    UnifiedDownlinkCommand command = UnifiedDownlinkCommand.fromString(jsonStr);

    System.out.println("\n========== 测试4: 拦截器场景 - 显式回写 ==========");
    System.out.println("初始 command.function: " + command.getFunction());

    // 模拟拦截器逻辑（带回写）
    Map<String, Object> function = command.getFunction();

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) function.get("data");
    if (data == null) {
      data = new HashMap<>();
      function.put("data", data);
    }

    // 填充参数
    data.put("meterNo", "250707ea8dda");
    System.out.println("填充后 data: " + data);

    // 显式回写
    function.put("data", data);
    System.out.println("回写后 function: " + function);

    // 检查
    Map<String, Object> functionAfter = command.getFunction();
    @SuppressWarnings("unchecked")
    Map<String, Object> dataAfter = (Map<String, Object>) functionAfter.get("data");
    System.out.println("最终 dataAfter: " + dataAfter);

    // 验证
    assertNotNull(dataAfter, "data 不应该为 null");
    assertEquals(1, dataAfter.size(), "data 应该包含 1 个元素");
    assertEquals("250707ea8dda", dataAfter.get("meterNo"), "meterNo 值应该正确");
  }

  @Test
  @DisplayName("测试5: 使用 DownlinkContext 的完整场景")
  void testWithDownlinkContext() {
    // 创建完整的上下文
    UnifiedDownlinkCommand command =
        UnifiedDownlinkCommand.builder()
            .productKey("HnDdSnj1kiQg")
            .deviceId("241031d76ca4")
            .cmd(DownCmd.DEV_FUNCTION)
            .build();

    JSONObject functionJson = new JSONObject();
    functionJson.set("function", "fillParames");
    functionJson.set("data", new JSONObject());
    command.setFunction(functionJson);

    DownlinkContext<?> context = new DownlinkContext<>();
    context.setCommand(command);

    System.out.println("\n========== 测试5: 使用 DownlinkContext ==========");

    // 模拟拦截器中的代码
    var cmd = context.getCommand();
    Map<String, Object> function1 = cmd.getFunction();
    System.out.println("第一次获取 function 引用: " + System.identityHashCode(function1));

    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) function1.get("data");
    data.put("param1", "value1");

    Map<String, Object> function2 = cmd.getFunction();
    System.out.println("第二次获取 function 引用: " + System.identityHashCode(function2));
    System.out.println("是否是同一个对象: " + (function1 == function2));

    // 验证修改是否生效
    @SuppressWarnings("unchecked")
    Map<String, Object> dataCheck = (Map<String, Object>) function2.get("data");
    System.out.println("验证 dataCheck: " + dataCheck);

    assertEquals("value1", dataCheck.get("param1"), "修改应该生效");
  }

  @Test
  @DisplayName("测试6: 完全模拟真实场景 - fromJson构建")
  void testRealScenarioWithFromJson() {
    // 完全模拟真实的下行消息处理流程
    String jsonMessage =
        """
            {
                "productKey": "HnDdSnj1kiQg",
                "deviceId": "241031d76ca4",
                "iotId": "HnDdSnj1kiQg241031d76ca4",
                "cmd": "DEV_FUNCTION",
                "function": {
                    "function": "fillParames",
                    "data": {}
                }
            }
            """;

    System.out.println("\n========== 测试6: 完全模拟真实场景 ==========");
    System.out.println("原始 JSON: " + jsonMessage);

    // 1. 模拟框架层：从 JSON 构建 command
    JSONObject jsonObject = JSONUtil.parseObj(jsonMessage);
    UnifiedDownlinkCommand command = UnifiedDownlinkCommand.fromJson(jsonObject);

    System.out.println("\n构建后 command.function 类型: " + command.getFunction().getClass().getName());
    System.out.println("构建后 command.function: " + command.getFunction());

    // 2. 模拟框架层：创建 Context
    DownlinkContext<?> context = new DownlinkContext<>();
    context.setCommand(command);

    // 3. 模拟拦截器代码（完全按照真实代码）
    System.out.println("\n========== 开始模拟拦截器逻辑 ==========");

    var cmd = context.getCommand();
    System.out.println("第1次 getCommand 引用: " + System.identityHashCode(cmd));

    Map<String, Object> function = cmd.getFunction();
    System.out.println("第1次 getFunction 引用: " + System.identityHashCode(function));
    System.out.println("function 类型: " + function.getClass().getName());

    // 获取 data
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) function.get("data");
    System.out.println("第1次 get(\"data\") 引用: " + System.identityHashCode(data));
    System.out.println("data 类型: " + (data != null ? data.getClass().getName() : "null"));
    System.out.println("data 初始内容: " + data);

    if (data == null) {
      data = new HashMap<>();
      function.put("data", data);
      System.out.println("创建新 data 并 put 回去");
    }

    // 填充参数
    data.put("meterNo", "250707ea8dda");
    System.out.println("\n填充参数后 data: " + data);
    System.out.println("data 引用: " + System.identityHashCode(data));

    // 关键测试：不回写，看看能否获取到
    System.out.println("\n========== 不回写，检查是否生效 ==========");
    var cmd2 = context.getCommand();
    System.out.println("第2次 getCommand 引用: " + System.identityHashCode(cmd2));
    System.out.println("cmd == cmd2: " + (cmd == cmd2));

    Map<String, Object> function2 = cmd2.getFunction();
    System.out.println("第2次 getFunction 引用: " + System.identityHashCode(function2));
    System.out.println("function == function2: " + (function == function2));

    @SuppressWarnings("unchecked")
    Map<String, Object> data2 = (Map<String, Object>) function2.get("data");
    System.out.println("第2次 get(\"data\") 引用: " + System.identityHashCode(data2));
    System.out.println("data == data2: " + (data == data2));
    System.out.println("data2 内容: " + data2);
    System.out.println("data2.size(): " + (data2 != null ? data2.size() : 0));

    if (data2 != null && data2.containsKey("meterNo")) {
      System.out.println("✅ 不回写情况下，修改生效了！");
    } else {
      System.out.println("❌ 不回写情况下，修改未生效！");
    }

    // 显式回写
    System.out.println("\n========== 显式回写，再次检查 ==========");
    function.put("data", data);
    System.out.println("执行 function.put(\"data\", data)");

    Map<String, Object> function3 = cmd.getFunction();
    @SuppressWarnings("unchecked")
    Map<String, Object> data3 = (Map<String, Object>) function3.get("data");
    System.out.println("回写后 data3: " + data3);
    System.out.println("data3.size(): " + (data3 != null ? data3.size() : 0));

    // 断言验证
    assertNotNull(data3, "data3 不应该为 null");
    assertTrue(data3.containsKey("meterNo"), "应该包含 meterNo");
    assertEquals("250707ea8dda", data3.get("meterNo"), "meterNo 值应该正确");
  }
}
