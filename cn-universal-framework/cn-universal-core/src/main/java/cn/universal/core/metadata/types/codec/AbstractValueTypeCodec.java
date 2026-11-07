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

import cn.universal.core.metadata.ValueType;
import cn.universal.core.metadata.ValueTypeCodec;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractValueTypeCodec<T extends ValueType> implements ValueTypeCodec<T> {

  @Override
  public T decode(T type, Map<String, Object> config) {
    ofNullable(config.get("description")).map(String::valueOf).ifPresent(type::setDescription);

    ofNullable(config.get("expands"))
        .filter(Map.class::isInstance)
        .map(Map.class::cast)
        .ifPresent(type::setExpands);
    return type;
  }

  @Override
  public Map<String, Object> encode(T type) {
    Map<String, Object> encoded = new HashMap<>();
    encoded.put("type", getTypeId());
    encoded.put("description", type.getDescription());
    encoded.put("expands", type.getExpands());
    doEncode(encoded, type);
    return encoded;
  }

  protected void doEncode(Map<String, Object> encoded, T type) {}
}
