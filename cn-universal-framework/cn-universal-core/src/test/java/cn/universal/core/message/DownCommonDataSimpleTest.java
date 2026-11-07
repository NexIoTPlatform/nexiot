//package cn.universal.core.message;
//
//import cn.hutool.json.JSONObject;
//import cn.hutool.json.JSONUtil;
//import com.fasterxml.jackson.databind.ObjectMapper;
//
///**
// * DownCommonData 简单测试（可直接运行的 main 方法）
// */
//public class DownCommonDataSimpleTest {
//
//    public static void main(String[] args) throws Exception {
//        System.out.println("========== DownCommonData 测试开始 ==========\n");
//
//        int passedTests = 0;
//        int failedTests = 0;
//
//        // 测试1: 固定字段读写
//        try {
//            System.out.println("【测试1】固定字段读写");
//            DownCommonData data = new DownCommonData();
//            data.setDeviceName("测试设备");
//            data.setImei("123456789012345");
//
//            assert "测试设备".equals(data.getDeviceName()) : "固定字段 getter 失败";
//            assert "测试设备".equals(data.get("deviceName")) : "固定字段 get() 失败";
//            assert "123456789012345".equals(data.get("imei")) : "固定字段 get() 失败";
//
//            System.out.println("✅ 通过\n");
//            passedTests++;
//        } catch (AssertionError e) {
//            System.out.println("❌ 失败: " + e.getMessage() + "\n");
//            failedTests++;
//        }
//
//        // 测试2: 动态字段读写
//        try {
//            System.out.println("【测试2】动态字段读写");
//            DownCommonData data = new DownCommonData();
//            data.put("customField1", "自定义值1");
//            data.put("customField2", 12345);
//
//            assert "自定义值1".equals(data.get("customField1")) : "动态字段获取失败";
//            assert Integer.valueOf(12345).equals(data.get("customField2")) : "动态字段获取失败";
//
//            System.out.println("✅ 通过\n");
//            passedTests++;
//        } catch (AssertionError e) {
//            System.out.println("❌ 失败: " + e.getMessage() + "\n");
//            failedTests++;
//        }
//
//        // 测试3: 混合字段
//        try {
//            System.out.println("【测试3】混合使用固定字段和动态字段");
//            DownCommonData data = new DownCommonData();
//            data.setDeviceName("混合测试");
//            data.put("customField", "动态值");
//
//            assert "混合测试".equals(data.get("deviceName")) : "固定字段获取失败";
//            assert "动态值".equals(data.get("customField")) : "动态字段获取失败";
//            assert data.containsKey("deviceName") : "containsKey 固定字段失败";
//            assert data.containsKey("customField") : "containsKey 动态字段失败";
//            assert !data.containsKey("notExist") : "containsKey 不存在字段失败";
//
//            System.out.println("✅ 通过\n");
//            passedTests++;
//        } catch (AssertionError e) {
//            System.out.println("❌ 失败: " + e.getMessage() + "\n");
//            failedTests++;
//        }
//
//        // 测试4: Jackson JSON 序列化
//        try {
//            System.out.println("【测试4】Jackson JSON 序列化");
//            DownCommonData data = new DownCommonData();
//            data.setDeviceName("JSON测试");
//            data.setImei("888888888888888");
//            data.put("customField", "自定义");
//
//            ObjectMapper mapper = new ObjectMapper();
//            String jsonStr = mapper.writeValueAsString(data);
//            System.out.println("序列化结果: " + jsonStr);
//
//            assert jsonStr.contains("deviceName") : "缺少固定字段";
//            assert jsonStr.contains("JSON测试") : "缺少固定字段值";
//            assert jsonStr.contains("customField") : "缺少动态字段";
//            assert jsonStr.contains("自定义") : "缺少动态字段值";
//            assert !jsonStr.contains("\"additionalProperties\"") : "不应包含 additionalProperties 字段";
//
//            System.out.println("✅ 通过\n");
//            passedTests++;
//        } catch (Exception e) {
//            System.out.println("❌ 失败: " + e.getMessage() + "\n");
//            e.printStackTrace();
//            failedTests++;
//        }
//
//        // 测试5: Jackson JSON 反序列化
//        try {
//            System.out.println("【测试5】Jackson JSON 反序列化");
//            String jsonStr = "{\"deviceName\":\"反序列化设备\",\"imei\":\"777777777777777\",\"customField\":\"动态字段值\"}";
//
//            ObjectMapper mapper = new ObjectMapper();
//            DownCommonData data = mapper.readValue(jsonStr, DownCommonData.class);
//
//            assert "反序列化设备".equals(data.getDeviceName()) : "固定字段反序列化失败";
//            assert "777777777777777".equals(data.getImei()) : "固定字段反序列化失败";
//            assert "动态字段值".equals(data.get("customField")) : "动态字段反序列化失败";
//
//            System.out.println("✅ 通过\n");
//            passedTests++;
//        } catch (Exception e) {
//            System.out.println("❌ 失败: " + e.getMessage() + "\n");
//            e.printStackTrace();
//            failedTests++;
//        }
//
//        // 测试6: Hutool JSON 序列化
//        try {
//            System.out.println("【测试6】Hutool JSON 序列化");
//            DownCommonData data = new DownCommonData();
//            data.setDeviceName("Hutool测试");
//            data.put("customField", "自定义");
//
//            String jsonStr = JSONUtil.toJsonStr(data);
//            System.out.println("Hutool序列化: " + jsonStr);
//
//            assert jsonStr.contains("deviceName") : "缺少固定字段";
//            assert jsonStr.contains("Hutool测试") : "缺少固定字段值";
//            assert jsonStr.contains("customField") : "缺少动态字段";
//            assert jsonStr.contains("自定义") : "缺少动态字段值";
//            assert !jsonStr.contains("\"additionalProperties\"") : "不应包含 additionalProperties 字段";
//
//            System.out.println("✅ 通过\n");
//            passedTests++;
//        } catch (AssertionError e) {
//            System.out.println("❌ 失败: " + e.getMessage() + "\n");
//            failedTests++;
//        }
//
//        // 测试7: Hutool JSON 反序列化
//        try {
//            System.out.println("【测试7】Hutool JSON 反序列化");
//            String jsonStr = "{\"deviceName\":\"Hutool反序列化\",\"imei\":\"666666666666666\",\"customField\":\"动态值\"}";
//
//            DownCommonData data = JSONUtil.toBean(jsonStr, DownCommonData.class);
//
//            assert "Hutool反序列化".equals(data.getDeviceName()) : "固定字段反序列化失败";
//            assert "666666666666666".equals(data.getImei()) : "固定字段反序列化失败";
//            assert "动态值".equals(data.get("customField")) : "动态字段反序列化失败";
//
//            System.out.println("✅ 通过\n");
//            passedTests++;
//        } catch (AssertionError e) {
//            System.out.println("❌ 失败: " + e.getMessage() + "\n");
//            failedTests++;
//        }
//
//        // 测试8: toJSONObject 方法
//        try {
//            System.out.println("【测试8】toJSONObject 方法");
//            DownCommonData data = new DownCommonData();
//            data.setDeviceName("JSONObject测试");
//            data.put("customField", "自定义");
//
//            JSONObject json = data.toJSONObject();
//            System.out.println("toJSONObject: " + json.toString());
//
//            assert "JSONObject测试".equals(json.getStr("deviceName")) : "固定字段缺失";
//            assert "自定义".equals(json.getStr("customField")) : "动态字段缺失";
//            assert !json.containsKey("additionalProperties") : "不应包含 additionalProperties";
//
//            System.out.println("✅ 通过\n");
//            passedTests++;
//        } catch (AssertionError e) {
//            System.out.println("❌ 失败: " + e.getMessage() + "\n");
//            failedTests++;
//        }
//
//        // 测试9: toMap 方法
//        try {
//            System.out.println("【测试9】toMap 方法");
//            DownCommonData data = new DownCommonData();
//            data.setDeviceName("Map测试");
//            data.put("customField", "动态");
//
//            var map = data.toMap();
//            System.out.println("toMap: " + map);
//
//            assert "Map测试".equals(map.get("deviceName")) : "固定字段缺失";
//            assert "动态".equals(map.get("customField")) : "动态字段缺失";
//            assert !map.containsKey("additionalProperties") : "不应包含 additionalProperties";
//
//            System.out.println("✅ 通过\n");
//            passedTests++;
//        } catch (AssertionError e) {
//            System.out.println("❌ 失败: " + e.getMessage() + "\n");
//            failedTests++;
//        }
//
//        // 测试10: 类型安全的 get 方法
//        try {
//            System.out.println("【测试10】类型安全的 get 方法");
//            DownCommonData data = new DownCommonData();
//            data.put("stringField", "字符串");
//            data.put("intField", 100);
//
//            String strValue = data.get("stringField", String.class);
//            Integer intValue = data.get("intField", Integer.class);
//
//            assert "字符串".equals(strValue) : "字符串类型获取失败";
//            assert Integer.valueOf(100).equals(intValue) : "整数类型获取失败";
//
//            System.out.println("✅ 通过\n");
//            passedTests++;
//        } catch (AssertionError e) {
//            System.out.println("❌ 失败: " + e.getMessage() + "\n");
//            failedTests++;
//        }
//
//        // 总结
//        System.out.println("========== 测试总结 ==========");
//        System.out.println("通过: " + passedTests + " 个");
//        System.out.println("失败: " + failedTests + " 个");
//        System.out.println("总计: " + (passedTests + failedTests) + " 个");
//
//        if (failedTests == 0) {
//            System.out.println("\n🎉 所有测试通过！");
//        } else {
//            System.out.println("\n⚠️ 有测试失败，请检查！");
//            System.exit(1);
//        }
//    }
//}
//
