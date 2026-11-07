package cn.universal.admin.platform.support;

import static org.junit.jupiter.api.Assertions.*;

import cn.hutool.json.JSONUtil;
import cn.universal.persistence.entity.IoTProduct;
import org.junit.jupiter.api.Test;

class SipProductConfigSupportTest {
  @Test
  void editingMaskedOrEmptyCredentialPreservesOriginalAndOtherFields() {
    IoTProduct old =
        IoTProduct.builder()
            .thirdPlatform("sip")
            .configuration("{\"sipEslPassword\":\"secret\",\"other\":42}")
            .build();
    for (String input :
        new String[] {"{}", "{\"sipEslPassword\":\"***\"}", "{\"sipEslPassword\":\"\"}", null}) {
      IoTProduct edited = IoTProduct.builder().configuration(input).build();
      SipProductConfigSupport.preserveCredential(edited, old);
      assertEquals("secret", JSONUtil.parseObj(edited.getConfiguration()).getStr("sipEslPassword"));
      assertEquals(42, JSONUtil.parseObj(edited.getConfiguration()).getInt("other"));
    }
    IoTProduct rotated =
        IoTProduct.builder().configuration("{\"sipEslPassword\":\"new-secret\"}").build();
    SipProductConfigSupport.preserveCredential(rotated, old);
    assertEquals(
        "new-secret", JSONUtil.parseObj(rotated.getConfiguration()).getStr("sipEslPassword"));
    assertTrue(old.getConfiguration().contains("\"secret\""));
  }

  @Test
  void leavesOtherProtocolsUnchanged() {
    IoTProduct old =
        IoTProduct.builder().thirdPlatform("tcp").configuration("{\"port\":5000}").build();
    IoTProduct edited = IoTProduct.builder().configuration("{\"port\":6000}").build();
    SipProductConfigSupport.preserveCredential(edited, old);
    assertEquals("{\"port\":6000}", edited.getConfiguration());
  }
}
