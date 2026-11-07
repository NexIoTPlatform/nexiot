package cn.universal.core.metadata;

import cn.hutool.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class PropertyMetadataTest {

  @Test
  public void shouldReadAndWriteExplicitSource() {
    JSONObject json = new JSONObject();
    json.set("id", "totalEnergy");
    json.set("name", "总用电量");
    json.set("mode", "r");
    json.set("source", "device");

    PropertyMetadata metadata = new PropertyMetadata(json);

    Assert.assertEquals("device", metadata.getSource());
    Assert.assertEquals("device", metadata.toJson().getStr("source"));
  }

  @Test
  public void shouldDefaultSourceToExtended() {
    PropertyMetadata metadata = new PropertyMetadata();
    metadata.setId("totalEnergy");

    Assert.assertEquals("extended", metadata.getSource());
    Assert.assertEquals("extended", metadata.toJson().getStr("source"));
  }
}
