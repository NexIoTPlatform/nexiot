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

package cn.universal.core.metadata.types.codec;

import static java.util.Optional.ofNullable;

import cn.hutool.json.JSONObject;
import cn.universal.core.metadata.AbstractPropertyMetadata;
import cn.universal.core.metadata.PropertyMetadata;
import cn.universal.core.metadata.types.ObjectType;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ObjectCodec extends AbstractValueTypeCodec<ObjectType> {

  @Override
  public String getTypeId() {
    return ObjectType.ID;
  }

  @Override
  public ObjectType decode(ObjectType type, Map<String, Object> config) {
    super.decode(type, config);
    JSONObject jsonObject = new JSONObject(config);

    ofNullable(jsonObject.getJSONArray("properties"))
        .map(
            list ->
                list.stream()
                    .map(JSONObject.class::cast)
                    .<AbstractPropertyMetadata>map(PropertyMetadata::new)
                    .collect(Collectors.toList()))
        .ifPresent(type::setProperties);

    return type;
  }

  @Override
  protected void doEncode(Map<String, Object> encoded, ObjectType type) {
    super.doEncode(encoded, type);
    if (type.getProperties() != null) {
      encoded.put(
          "properties",
          type.getProperties().stream()
              .map(PropertyMetadata::new)
              .map(AbstractPropertyMetadata::toJson)
              .collect(Collectors.toList()));
    }
  }
}
