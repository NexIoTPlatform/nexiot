package cn.universal.core.protocol.support;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class ProtocolCodecSupportWrapperTest {

  private final TestProtocolCodecSupportWrapper wrapper = new TestProtocolCodecSupportWrapper();

  @Test
  public void shouldReturnEmptyStringForEmptyMap() {
    Assert.assertEquals("", wrapper.strValue(new LinkedHashMap<>()));
  }

  @Test
  public void shouldSerializeNonEmptyMap() {
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("cmd", "AA55");

    Assert.assertEquals("{\"cmd\":\"AA55\"}", wrapper.strValue(value));
  }

  private static class TestProtocolCodecSupportWrapper extends ProtocolCodecSupportWrapper {

    private String strValue(Object value) {
      return str(value);
    }
  }
}
