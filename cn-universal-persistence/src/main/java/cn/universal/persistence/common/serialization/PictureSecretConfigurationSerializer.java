package cn.universal.persistence.common.serialization;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

/** Device configuration remains usable in the server, while HTTP serialization masks picture keys. */
public class PictureSecretConfigurationSerializer extends JsonSerializer<String> {
  @Override public void serialize(String value, JsonGenerator generator, SerializerProvider provider) throws IOException {
    try {
      var config = JSONUtil.parseObj(value);
      if (config.containsKey("pictureSecret")) config.set("pictureSecret", "***");
      generator.writeString(config.toString());
    } catch (RuntimeException ignored) { generator.writeString(value); }
  }
}
