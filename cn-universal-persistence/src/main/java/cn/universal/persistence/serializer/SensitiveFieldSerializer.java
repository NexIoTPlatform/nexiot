package cn.universal.persistence.serializer;

import cn.universal.persistence.annotation.SensitiveField;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import java.io.IOException;

/** 敏感字段序列化器 自动处理敏感字段的显示 */
public class SensitiveFieldSerializer extends JsonSerializer<String>
    implements ContextualSerializer {

  private SensitiveField sensitiveField;

  public SensitiveFieldSerializer() {}

  public SensitiveFieldSerializer(SensitiveField sensitiveField) {
    this.sensitiveField = sensitiveField;
  }

  @Override
  public void serialize(String value, JsonGenerator gen, SerializerProvider serializers)
      throws IOException {
    if (value == null) {
      gen.writeNull();
      return;
    }

    String maskedValue = maskValue(value);
    gen.writeString(maskedValue);
  }

  @Override
  public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
      throws JsonMappingException {
    if (property != null) {
      SensitiveField annotation = property.getAnnotation(SensitiveField.class);
      if (annotation != null) {
        return new SensitiveFieldSerializer(annotation);
      }
    }
    return this;
  }

  /** 脱敏处理 */
  private String maskValue(String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }

    if (sensitiveField.hide()) {
      return "***";
    }

    int prefixLen = sensitiveField.showPrefix();
    int suffixLen = sensitiveField.showSuffix();

    if (prefixLen == 0 && suffixLen == 0) {
      // 默认显示前3位和后3位
      if (value.length() <= 6) {
        return "***";
      }
      return value.substring(0, 3) + "***" + value.substring(value.length() - 3);
    }

    if (value.length() <= prefixLen + suffixLen) {
      return "***";
    }

    StringBuilder masked = new StringBuilder();
    if (prefixLen > 0) {
      masked.append(value.substring(0, prefixLen));
    }
    masked.append("***");
    if (suffixLen > 0) {
      masked.append(value.substring(value.length() - suffixLen));
    }

    return masked.toString();
  }
}
