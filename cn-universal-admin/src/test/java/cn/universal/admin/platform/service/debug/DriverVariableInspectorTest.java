package cn.universal.admin.platform.service.debug;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class DriverVariableInspectorTest {
  static class Base {
    private int inherited = 7;
  }

  static class Device extends Base {
    private String name = "温度计";
    private int[] samples = {18, 21};

    public String getName() {
      throw new AssertionError("不能调用 getter");
    }

    @Override
    public String toString() {
      throw new AssertionError("不能调用 toString");
    }
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> children(Map<String, Object> node) {
    return (List<Map<String, Object>>) node.getOrDefault("children", List.of());
  }

  private Map<String, Object> find(List<Map<String, Object>> nodes, String name) {
    return nodes.stream().filter(node -> name.equals(node.get("name"))).findFirst().orElseThrow();
  }

  @Test
  void showsNestedCollectionsBytesClassesAndFieldsWithoutCallingUserMethods() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("device", new Device());
    values.put("rs", Map.of("properties", Map.of("temperatures", List.of(18, 21))));
    values.put("bytes", new byte[] {1, 2, -1});
    values.put("AES", cn.hutool.crypto.symmetric.AES.class);
    values.put("context", null);
    var nodes = DriverVariableInspector.inspect(values);
    var device = find(nodes, "device");
    assertEquals(Device.class.getName(), device.get("type"));
    assertEquals("温度计", find(children(device), "name").get("value"));
    assertEquals("7", find(children(device), "inherited").get("value"));
    assertEquals("int[]", find(children(device), "samples").get("type"));
    var temperatures =
        find(children(find(children(find(nodes, "rs")), "properties")), "temperatures");
    assertEquals("21", find(children(temperatures), "[1]").get("value"));
    assertEquals("Base64: AQL/", find(nodes, "bytes").get("value"));
    assertEquals("byte[]", find(nodes, "bytes").get("type"));
    assertEquals("-1", find(children(find(nodes, "bytes")), "[2]").get("value"));
    assertEquals("java.lang.Class", find(nodes, "AES").get("type"));
    assertEquals("cn.hutool.crypto.symmetric.AES", find(nodes, "AES").get("value"));
    assertEquals("null", find(nodes, "context").get("value"));
  }

  @Test
  void largeLocalValuesDoNotHideOtherRootVariables() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("payload", "010203");
    values.put("context", null);
    values.put("large", Collections.nCopies(50, Collections.nCopies(50, "item")));
    var snapshot = DriverVariableInspector.inspect(values);
    assertEquals("large", snapshot.getFirst().get("name"));
    assertEquals("010203", find(snapshot, "payload").get("value"));
    assertEquals("null", find(snapshot, "context").get("value"));
  }

  @Test
  void boundsCyclesLargeArraysAndDeepObjectGraphsWithExplicitNotices() {
    Map<String, Object> cyclic = new LinkedHashMap<>();
    cyclic.put("self", cyclic);
    var node = DriverVariableInspector.inspect(Map.of("cyclic", cyclic)).getFirst();
    assertEquals("循环引用", find(children(node), "self").get("notice"));
    node = DriverVariableInspector.inspect(Map.of("large", new int[100000])).getFirst();
    assertEquals(100000, node.get("members"));
    assertEquals(50, children(node).size());
    assertTrue(node.get("notice").toString().contains("截断"));
    Object nested = "value";
    for (int i = 0; i < 100; i++) nested = List.of(nested);
    node = DriverVariableInspector.inspect(Map.of("deep", nested)).getFirst();
    for (int i = 0; i < 4; i++) node = children(node).getFirst();
    assertEquals("达到展开深度上限", node.get("notice"));
  }
}
