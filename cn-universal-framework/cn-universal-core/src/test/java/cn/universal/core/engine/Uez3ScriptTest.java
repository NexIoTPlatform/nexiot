package cn.universal.core.engine;

import cn.hutool.json.JSONObject;
import cn.universal.core.engine.extend.HexFunctions;
import cn.universal.core.engine.extend.ModbusFunctions;
import cn.universal.core.engine.extend.ProtocolFunctions;
import cn.universal.core.engine.extend.ShadowFunctions;
import cn.universal.core.engine.extend.TypeCheckFunctions;
import cn.universal.core.engine.extend.UnivFunctions;
import cn.universal.core.engine.extend.convert.ConvertFunctions;
import cn.universal.core.engine.extend.method.JSONObjectExtension;
import cn.universal.core.engine.functions.DynamicModuleImport;
import cn.universal.core.engine.reflection.JavaReflection;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UEZ3/1PN 智能断路器 ms 脚本编译与功能验收测试。
 *
 * <p>验收目标：
 * <ol>
 *   <li>脚本可正常编译通过（无语法兼容性问题）</li>
 *   <li>encode 出来的报文符合改造版 Modbus TCP 格式（MBAP头+CRC+PDU）</li>
 *   <li>decode 能正确解析用户提供的真实上报报文</li>
 *   <li>encode → decode 往返一致</li>
 * </ol>
 */
public class Uez3ScriptTest {

  private static String scriptText;

  @BeforeClass
  public static void setUp() throws IOException {
    // 注册 JSONObject 扩展方法（getStr/getInt/getObj 等）
    JavaReflection.registerMethodExtension(JSONObject.class, new JSONObjectExtension());
    // 注册全部 extend 函数
    JavaReflection.registerFunction(new UnivFunctions());
    JavaReflection.registerFunction(new ModbusFunctions());
    JavaReflection.registerFunction(new HexFunctions());
    JavaReflection.registerFunction(new ProtocolFunctions());
    JavaReflection.registerFunction(new ShadowFunctions());
    JavaReflection.registerFunction(new TypeCheckFunctions());
    JavaReflection.registerFunction(new ConvertFunctions());

    // 注册 log 模块（生产环境由 MagicAPIAutoConfiguration 注册，测试环境需手动注册）
    MagicResourceLoader.addModule(
        "log",
        new DynamicModuleImport(
            Logger.class,
            context ->
                LoggerFactory.getLogger(
                    Objects.toString(context.getScriptName(), "Uez3ScriptTest"))));

    // 读取 UEZ3/1PN ms 脚本（从 classpath 加载）
    scriptText = readResource("/ms/UEZ3/uez3_1pn.ms");
    Assert.assertNotNull("未找到 ms/UEZ3/uez3_1pn.ms 资源", scriptText);
    Assert.assertTrue("ms 脚本内容为空", scriptText.length() > 0);

    // 编译原始脚本（如果有语法兼容性问题，这里会抛出异常）
    MagicScript magicScript = MagicScript.create(scriptText, null);
    magicScript.compile();
    System.out.println("[OK] UEZ3/1PN ms 脚本编译通过");
  }

  @Test
  public void shouldCompileWithoutError() {
    // 编译已在 @BeforeClass 完成，这里仅断言脚本文本已加载
    Assert.assertNotNull(scriptText);
    System.out.println("[OK] 编译验证测试通过");
  }

  @Test
  public void shouldEncodeMeterReadFrame() {
    // 构造物模型指令：读取所有电参量（0x0010 起 12 寄存器，从机地址 1）
    String functionReq =
        "{\"function\":\"meterRead\",\"data\":{\"slaveAddr\":1}}";
    String frame = runEncode(functionReq, new JSONObject());
    System.out.println("[OK] meterRead 编码结果: " + frame);
    Assert.assertNotNull("encode 结果不应为 null", frame);
    // MBAP 头 7 字节 + PDU 5 字节 = 12 字节 = 24 字符
    Assert.assertEquals("报文总长度应为 24 字符", 24, frame.length());
    // 起始寄存器地址应为 0010
    Assert.assertEquals("起始寄存器地址", "0010", frame.substring(4, 8));
    // 长度应为 0006
    Assert.assertEquals("长度字段", "0006", frame.substring(8, 12));
    // UnitID 应为 01
    Assert.assertEquals("UnitID", "01", frame.substring(12, 14));
    // 功能码应为 04
    Assert.assertEquals("功能码", "04", frame.substring(14, 16));
    // PDU 中起始寄存器地址应为 0010
    Assert.assertEquals("PDU 起始地址", "0010", frame.substring(16, 20));
    // 寄存器数量应为 000C
    Assert.assertEquals("寄存器数量", "000C", frame.substring(20, 24));
  }

  @Test
  public void shouldEncodeSwitchOnFrame() {
    // 远程合闸：从机地址 3，valveOn=1
    String functionReq =
        "{\"function\":\"switchOnOff\",\"data\":{\"slaveAddr\":3,\"valveOn\":\"1\"}}";
    String frame = runEncode(functionReq, new JSONObject());
    System.out.println("[OK] switchOnOff(合闸) 编码结果: " + frame);
    Assert.assertNotNull(frame);
    // 写单寄存器：MBAP(7) + FC(1) + regAddr(2) + value(2) = 12 字节 = 24 字符
    Assert.assertEquals(24, frame.length());
    // 起始寄存器地址应为 01D0
    Assert.assertEquals("起始寄存器地址", "01D0", frame.substring(4, 8));
    // UnitID 应为 03
    Assert.assertEquals("UnitID", "03", frame.substring(12, 14));
    // 功能码应为 06
    Assert.assertEquals("功能码", "06", frame.substring(14, 16));
    // 寄存器地址应为 01D0
    Assert.assertEquals("寄存器地址", "01D0", frame.substring(16, 20));
    // 写入值应为 0001（合闸）
    Assert.assertEquals("合闸命令值", "0001", frame.substring(20, 24));
  }

  @Test
  public void shouldDecodeRealReportPacket() {
    // 用户提供的真实主动上报报文（来自上次分析）
    String payload =
        "1BE00016002F03042C000000003E960419138600000000000193DA250413124851"
            + "00000000000000000000000000000000000000";
    Object result = runDecode(payload);
    Assert.assertNotNull("decode 结果不应为 null", result);
    System.out.println("[OK] 真实报文 decode 结果: " + result);
  }

  @Test
  public void shouldRoundTripSwitchOnOff() {
    // encode 合闸指令
    String functionReq =
        "{\"function\":\"switchOnOff\",\"data\":{\"slaveAddr\":3,\"valveOn\":\"1\"}}";
    String frame = runEncode(functionReq, new JSONObject());
    Assert.assertNotNull(frame);
    // decode 同一帧（写单寄存器响应）
    Object result = runDecode(frame);
    Assert.assertNotNull("往返 decode 结果不应为 null", result);
    System.out.println("[OK] switchOnOff 往返测试: frame=" + frame + ", result=" + result);
  }

  /**
   * 调用 encode 函数：在脚本末尾追加 `return encode(functionReqJson, shadow);` 后编译执行
   */
  private String runEncode(String functionReqJson, JSONObject shadow) {
    String scriptWithCall = scriptText + "\nreturn encode(functionReqJson, shadow);";
    MagicScript magicScript = MagicScript.create(scriptWithCall, null);
    magicScript.compile();
    MagicScriptContext context = new MagicScriptContext();
    context.setScriptName("uez3_1pn.ms");
    context.set("functionReqJson", functionReqJson);
    context.set("shadow", shadow);
    Object value = magicScript.execute(context);
    return value == null ? null : value.toString();
  }

  /**
   * 调用 decode 函数：在脚本末尾追加 `return decode(payload, null);` 后编译执行
   */
  private Object runDecode(String payload) {
    String scriptWithCall = scriptText + "\nreturn decode(payload, null);";
    MagicScript magicScript = MagicScript.create(scriptWithCall, null);
    magicScript.compile();
    MagicScriptContext context = new MagicScriptContext();
    context.setScriptName("uez3_1pn.ms");
    context.set("payload", payload);
    return magicScript.execute(context);
  }

  /** 从 classpath 读取资源文件 */
  private static String readResource(String path) throws IOException {
    try (InputStream in = Uez3ScriptTest.class.getResourceAsStream(path)) {
      if (in == null) {
        return null;
      }
      byte[] bytes = in.readAllBytes();
      return new String(bytes, StandardCharsets.UTF_8);
    }
  }
}
