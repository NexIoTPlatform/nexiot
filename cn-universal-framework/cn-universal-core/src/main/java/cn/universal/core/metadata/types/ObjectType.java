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

package cn.universal.core.metadata.types;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.universal.core.metadata.AbstractPropertyMetadata;
import cn.universal.core.metadata.Converter;
import cn.universal.core.metadata.PropertyMetadata;
import cn.universal.core.metadata.ValidateResult;
import cn.universal.core.metadata.ValueType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Getter
@Setter
@Slf4j
public class ObjectType extends AbstractType<ObjectType>
    implements ValueType, Converter<Map<String, Object>> {

  public static final String ID = "object";

  private List<AbstractPropertyMetadata> properties;

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "对象类型";
  }

  public ObjectType addPropertyMetadata(AbstractPropertyMetadata property) {

    if (this.properties == null) {
      this.properties = new ArrayList<>();
    }

    this.properties.add(property);

    return this;
  }

  public List<AbstractPropertyMetadata> getProperties() {
    if (properties == null) {
      return Collections.emptyList();
    }
    return properties;
  }

  public ObjectType addProperty(String property, ValueType type) {
    return this.addProperty(property, property, type);
  }

  public ObjectType addProperty(String property, String name, ValueType type) {
    PropertyMetadata metadata = new PropertyMetadata();
    metadata.setId(property);
    metadata.setName(name);
    metadata.setValueType(type);
    return addPropertyMetadata(metadata);
  }

  @Override
  public ValidateResult validate(Object value) {

    if (properties == null || properties.isEmpty()) {
      return ValidateResult.success(value);
    }
    Map<String, Object> mapValue = convert(value);

    for (AbstractPropertyMetadata property : properties) {
      Object data = mapValue.get(property.getId());
      if (data == null) {
        continue;
      }
      ValidateResult result = property.getValueType().validate(data);
      if (!result.isSuccess()) {
        return result;
      }
    }
    return ValidateResult.success(mapValue);
  }

  @Override
  public JSONObject format(Object value) {
    return new JSONObject(handle(value, ValueType::format));
  }

  @SuppressWarnings("all")
  public Map<String, Object> handle(Object value, BiFunction<ValueType, Object, Object> mapping) {
    if (value == null) {
      return null;
    }
    if (value instanceof String && ((String) value).startsWith("{")) {
      value = JSONUtil.parseObj(String.valueOf(value));
    }
    if (!(value instanceof Map)) {
      value = (Map) value;
    }
    if (value instanceof Map) {
      Map<String, Object> mapValue = new HashMap<>(((Map) value));
      if (properties != null) {
        for (AbstractPropertyMetadata property : properties) {
          Object data = mapValue.get(property.getId());
          ValueType type = property.getValueType();
          if (data != null) {
            mapValue.put(property.getId(), mapping.apply(type, data));
          }
        }
      }
      return mapValue;
    }
    return null;
  }

  @Override
  public Map<String, Object> convert(Object value) {
    return handle(
        value,
        (type, data) -> {
          if (type instanceof Converter) {
            return ((Converter<?>) type).convert(data);
          }
          return data;
        });
  }

  public Optional<AbstractPropertyMetadata> getProperty(String key) {
    if (CollectionUtils.isEmpty(properties)) {
      return Optional.empty();
    }
    return properties.stream().filter(prop -> prop.getId().equals(key)).findAny();
  }
}
