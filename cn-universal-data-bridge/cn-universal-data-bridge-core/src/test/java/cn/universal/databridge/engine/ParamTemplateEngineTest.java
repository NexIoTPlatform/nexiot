package cn.universal.databridge.engine;

import static org.junit.jupiter.api.Assertions.*;

import cn.universal.databridge.engine.dialect.MySqlDialectAdapter;
import cn.universal.databridge.engine.dialect.OracleDialectAdapter;
import cn.universal.databridge.engine.dialect.PostgresDialectAdapter;
import cn.universal.databridge.engine.dialect.SqlServerDialectAdapter;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** ParamTemplateEngine 单元测试 */
class ParamTemplateEngineTest {

    private ParamTemplateEngine engine;
    private Map<String, Object> variables;

    @BeforeEach
    void setUp() {
        engine = new ParamTemplateEngine();

        // 准备测试数据
        variables = new HashMap<>();
        variables.put("deviceId", "dev123");
        variables.put("productKey", "pk001");
        variables.put("deviceName", "sensor01");
        variables.put("iotId", "iot456");
        variables.put("deviceNode", "node1");
        variables.put("messageType", "property");

        // 嵌套属性
        Map<String, Object> properties = new HashMap<>();
        properties.put("temperature", 25.5);
        properties.put("humidity", 60);
        properties.put("illuminationDesc", "明亮");
        variables.put("properties", properties);
    }

    @Test
    void testMysqlTemplate() {
    SqlDialectAdapter adapter = new MySqlDialectAdapter();

    String template =
        "INSERT INTO device_data(\n"
            + "    device_id, product_key, device_name, iot_id,\n"
            + "    device_node, message_type, properties, raw_data\n"
            + ") VALUES(\n"
            + "    #{deviceId}, #{productKey}, #{deviceName}, #{iotId},\n"
            + "    #{deviceNode}, #{messageType}, #{properties}, \n"
            + "    ${json('csq', properties.temperature, 'tips', properties.illuminationDesc)}\n"
            + ");";

    ParamSql result = engine.process(template, variables, adapter);

    assertNotNull(result);
    assertTrue(result.getSql().contains("JSON_OBJECT"));
    assertTrue(result.getSql().contains("?"));
    assertEquals(9, result.getParams().size());
    assertEquals("dev123", result.getParams().get(0));
    assertEquals("pk001", result.getParams().get(1));
    assertEquals(25.5, result.getParams().get(7)); // csq value
    assertEquals("明亮", result.getParams().get(8)); // tips value
  }

  @Test
  void testPostgreSqlTemplate() {
    SqlDialectAdapter adapter = new PostgresDialectAdapter();

    String template =
        "INSERT INTO device_data(\n"
            + "    device_id, product_key, device_name, iot_id,\n"
            + "    device_node, message_type, properties, raw_data\n"
            + ") VALUES(\n"
            + "    #{deviceId}, #{productKey}, #{deviceName}, #{iotId},\n"
            + "    #{deviceNode}, #{messageType}, #{properties}, \n"
            + "    ${json_text('csq', properties.temperature, 'tips', properties.illuminationDesc)}\n"
            + ");";

    ParamSql result = engine.process(template, variables, adapter);

    assertNotNull(result);
    assertTrue(result.getSql().contains("json_build_object"));
    assertTrue(result.getSql().contains("::TEXT"));
    assertTrue(result.getSql().contains("?"));
    assertEquals(9, result.getParams().size());
    assertEquals("dev123", result.getParams().get(0));
    assertEquals("明亮", result.getParams().get(8));
  }

  @Test
  void testOracleTemplate() {
    SqlDialectAdapter adapter = new OracleDialectAdapter();

    String template =
        "INSERT INTO device_data(\n"
            + "    device_id, product_key, device_name, iot_id,\n"
            + "    device_node, message_type, properties, raw_data\n"
            + ") VALUES(\n"
            + "    #{deviceId}, #{productKey}, #{deviceName}, #{iotId},\n"
            + "    #{deviceNode}, #{messageType}, #{properties}, \n"
            + "    ${json('csq', properties.temperature, 'tips', properties.illuminationDesc)}\n"
            + ");";

    ParamSql result = engine.process(template, variables, adapter);

    assertNotNull(result);
    assertTrue(result.getSql().contains("JSON_OBJECT"));
    assertTrue(result.getSql().contains("VALUE"));
    assertTrue(result.getSql().contains("?"));
    assertEquals(9, result.getParams().size());
  }

  @Test
  void testSqlServerTemplate() {
    SqlDialectAdapter adapter = new SqlServerDialectAdapter();

    String template =
        "INSERT INTO device_data(\n"
            + "    device_id, product_key, device_name, iot_id,\n"
            + "    device_node, message_type, properties, raw_data\n"
            + ") VALUES(\n"
            + "    #{deviceId}, #{productKey}, #{deviceName}, #{iotId},\n"
            + "    #{deviceNode}, #{messageType}, #{properties}, \n"
            + "    ${json('csq', properties.temperature, 'tips', properties.illuminationDesc)}\n"
            + ");";

    ParamSql result = engine.process(template, variables, adapter);

    assertNotNull(result);
    assertTrue(result.getSql().contains("CONCAT"));
    assertTrue(result.getSql().contains("?"));
    assertEquals(9, result.getParams().size());
    }

    @Test
    void testNestedPropertyAccess() {
        SqlDialectAdapter adapter = new MySqlDialectAdapter();

        String template = "SELECT #{properties.temperature}, #{properties.humidity}";

        ParamSql result = engine.process(template, variables, adapter);

        assertEquals("SELECT ?, ?", result.getSql());
        assertEquals(2, result.getParams().size());
        assertEquals(25.5, result.getParams().get(0));
        assertEquals(60, result.getParams().get(1));
    }

    @Test
    void testDirectPropertiesAccess() {
        SqlDialectAdapter adapter = new MySqlDialectAdapter();

        String template = "INSERT INTO test(data) VALUES(#{properties})";

        ParamSql result = engine.process(template, variables, adapter);

        assertEquals("INSERT INTO test(data) VALUES(?)", result.getSql());
        assertEquals(1, result.getParams().size());
        // properties 应该被序列化为 JSON 字符串
        assertTrue(result.getParams().get(0) instanceof String);
        assertTrue(((String) result.getParams().get(0)).contains("temperature"));
    }

    @Test
    void testMultipleJsonMacros() {
        SqlDialectAdapter adapter = new MySqlDialectAdapter();

        String template = "INSERT INTO test(col1, col2) VALUES(" +
                "${json('a', deviceId)}, " +
                "${json('b', productKey)}" +
                ")";

        ParamSql result = engine.process(template, variables, adapter);

        assertTrue(result.getSql().contains("JSON_OBJECT"));
        assertEquals(2, result.getParams().size());
        assertEquals("dev123", result.getParams().get(0));
        assertEquals("pk001", result.getParams().get(1));
    }

    @Test
    void testEmptyTemplate() {
        SqlDialectAdapter adapter = new MySqlDialectAdapter();

        ParamSql result = engine.process("", variables, adapter);

        assertEquals("", result.getSql());
        assertTrue(result.getParams().isEmpty());
    }

    @Test
    void testNullTemplate() {
        SqlDialectAdapter adapter = new MySqlDialectAdapter();

        ParamSql result = engine.process(null, variables, adapter);

        assertEquals("", result.getSql());
        assertTrue(result.getParams().isEmpty());
    }

    @Test
    void testNoVariablesTemplate() {
        SqlDialectAdapter adapter = new MySqlDialectAdapter();

        String template = "SELECT * FROM device_data WHERE id = 1";

        ParamSql result = engine.process(template, variables, adapter);

        assertEquals(template, result.getSql());
        assertTrue(result.getParams().isEmpty());
    }
  @Test
  void testMixedJsonAndJsonText() {
    SqlDialectAdapter adapter = new PostgresDialectAdapter();

    String template =
        "INSERT INTO test(col1, col2) VALUES("
            + "${json('temp', properties.temperature)}, "
            + "${json_text('desc', properties.illuminationDesc)}"
            + ")";

    ParamSql result = engine.process(template, variables, adapter);

    assertTrue(result.getSql().contains("json_build_object"));
    assertTrue(result.getSql().contains("::TEXT"));
    assertEquals(2, result.getParams().size());
    assertEquals(25.5, result.getParams().get(0));
    assertEquals("明亮", result.getParams().get(1));
  }

  @Test
  void testChineseCharacters() {
    SqlDialectAdapter adapter = new MySqlDialectAdapter();

    variables.put("chineseValue", "测试中文");
    String template = "INSERT INTO test(name) VALUES(#{chineseValue})";

    ParamSql result = engine.process(template, variables, adapter);

    assertEquals("INSERT INTO test(name) VALUES(?)", result.getSql());
    assertEquals("测试中文", result.getParams().get(0));
  }

  @Test
  void testSpecialCharactersInJson() {
    SqlDialectAdapter adapter = new PostgresDialectAdapter();

    Map<String, Object> props = (Map<String, Object>) variables.get("properties");
    props.put("specialDesc", "包含\"引号\"和'单引号'");

    String template = "SELECT ${json_text('data', properties.specialDesc)}";

    ParamSql result = engine.process(template, variables, adapter);

    assertNotNull(result);
    assertTrue(result.getParams().get(0).toString().contains("引号"));
  }
}
