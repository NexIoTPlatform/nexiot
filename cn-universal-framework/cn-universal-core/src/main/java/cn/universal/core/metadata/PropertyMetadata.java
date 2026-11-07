/*
 * Copyright 2019-2024 JetLinks Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.universal.core.metadata;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.universal.core.metadata.types.UnknownType;
import cn.universal.core.metadata.types.ValueTypes;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.Setter;

/**
 * @since 1.0.0
 */
public class PropertyMetadata implements AbstractPropertyMetadata {

  @JsonIgnore private JSONObject json;
  //
  @Setter private transient ValueType valueType;

  //  private ValueType valueType;

  @Getter @Setter private String id;

  @Getter @Setter private String name;

  @Getter @Setter private String description;

  @Setter private String mode;

  @Setter private String source;

  @Getter @Setter private Map<String, Object> expands;

  public PropertyMetadata() {}

  public PropertyMetadata(JSONObject json) {
    fromJson(json);
  }

  public PropertyMetadata(AbstractPropertyMetadata another) {
    this.id = another.getId();
    this.name = another.getName();
    this.description = another.getDescription();
    this.valueType = another.getValueType();
    this.mode = another.getMode();
    this.source = another.getSource();
    this.expands = another.getExpands() == null ? null : new HashMap<>(another.getExpands());
  }

  protected Optional<ValueTypeCodec<ValueType>> getDataTypeCodec(ValueType dataType) {

    return ValueTypeCodecs.getCodec(dataType.getId());
  }

  protected ValueType parseDataType() {
    //    System.out.println(111111);
    JSONObject dataTypeJson = json.getJSONObject("valueType");
    if (dataTypeJson == null) {
      // 如果valueType为空，返回UnknownType而不是抛出异常
      return new UnknownType();
    }
    ValueType dataType =
        Optional.ofNullable(dataTypeJson.getStr("type"))
            .map(ValueTypes::lookup)
            .map(Supplier::get)
            .orElseGet(UnknownType::new);
    //    System.out.println("dataType-"+dataType.getId()+dataType.getType());
    getDataTypeCodec(dataType).ifPresent(codec -> codec.decode(dataType, dataTypeJson));
    //    System.out.println("dataType--"+dataType);
    return dataType;
  }

  @Override
  public ValueType getValueType() {
    if (valueType == null) {
      if (json != null) {
        valueType = parseDataType();
      } else {
        // 如果json为空，返回UnknownType
        valueType = new UnknownType();
      }
    }
    return valueType;
  }

  public String getMode() {
    if (StrUtil.isBlank(mode)) {
      mode = PropertyMode.r.name();
    }
    return mode;
  }

  @Override
  public String getSource() {
    if (StrUtil.isBlank(source)) {
      source = PropertySource.extended.name();
    }
    return source;
  }

  @Override
  public JSONObject toJson() {
    JSONObject json = new JSONObject();
    json.set("id", id);
    json.set("name", name);
    json.set("description", description);
    json.set("valueType", ValueTypeCodecs.encode(getValueType()).orElse(null));
    json.set("mode", getMode());
    json.set("source", getSource());
    json.set("expands", expands);
    return json;
  }

  @Override
  public void fromJson(JSONObject jsonObject) {
    if (jsonObject != null) {
      this.json = jsonObject;
      this.id = json.getStr("id");
      this.name = json.getStr("name");
      this.description = json.getStr("description");
      this.mode = json.getStr("mode");
      this.source = json.getStr("source");
      this.valueType = null;
      this.expands = json.getJSONObject("expands");
    }
  }

  @Override
  public String toString() {
    //  /* 测试 */ int name,
    return String.join("", getValueType().getId(), " ", getId(), " /* ", getName(), " */ ");
  }
}
