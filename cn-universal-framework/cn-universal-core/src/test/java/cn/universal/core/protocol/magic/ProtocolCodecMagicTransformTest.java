package cn.universal.core.protocol.magic;

import static org.junit.Assert.assertEquals;

import cn.universal.core.protocol.request.ProtocolDecodeRequest;
import cn.universal.core.protocol.request.ProtocolEncodeRequest;
import cn.universal.core.protocol.support.ProtocolCodecSupport.CodecMethod;
import cn.universal.core.protocol.support.ProtocolSupportDefinition;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Test;

public class ProtocolCodecMagicTransformTest {

  private static final String PROVIDER = "magic-http-transform-test";
  private final ProtocolCodecMagic codec = ProtocolCodecMagic.getInstance();

  @After
  public void cleanUp() {
    codec.remove(PROVIDER);
  }

  @Test
  public void keepsTransformScriptsIndependentAndPassesContext() throws Exception {
    ProtocolSupportDefinition definition = definition();
    Map<String, Object> context = Map.of("marker", "CTX");

    assertEquals(
        "IOT:CTX:payload",
        codec.iotToYour(new ProtocolEncodeRequest(definition, "payload", context)));
    assertEquals(
        "ENC:CTX:payload",
        codec.encode(new ProtocolEncodeRequest(definition, "payload", context)));
    assertEquals(
        "YOUR:CTX:payload",
        codec.yourToIot(new ProtocolDecodeRequest(definition, "payload", context)));
    assertEquals(
        "DEC:CTX:payload",
        codec.decode(new ProtocolDecodeRequest(definition, "payload", context)));
  }

  private ProtocolSupportDefinition definition() {
    String script =
        """
        var encode = (payload, context) => { return 'ENC:' + context.marker + ':' + payload; };
        var decode = (payload, context) => { return 'DEC:' + context.marker + ':' + payload; };
        var iotToYour = (payload, context) => { return 'IOT:' + context.marker + ':' + payload; };
        var yourToIot = (payload, context) => { return 'YOUR:' + context.marker + ':' + payload; };
        """;
    Map<String, Object> configuration = new HashMap<>();
    configuration.put("provider", PROVIDER);
    configuration.put("location", script);
    ProtocolSupportDefinition definition = new ProtocolSupportDefinition();
    definition.setId(PROVIDER);
    definition.setProvider(PROVIDER);
    definition.setType("magic");
    definition.setConfiguration(configuration);
    definition.setSupportMethods(
        Set.of(
            CodecMethod.encode.name(),
            CodecMethod.decode.name(),
            CodecMethod.iotToYour.name(),
            CodecMethod.yourToIot.name()));
    return definition;
  }
}
