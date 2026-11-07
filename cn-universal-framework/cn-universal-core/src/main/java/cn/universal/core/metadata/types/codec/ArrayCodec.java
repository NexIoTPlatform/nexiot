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
import cn.universal.core.metadata.ValueType;
import cn.universal.core.metadata.ValueTypeCodecs;
import cn.universal.core.metadata.types.ArrayType;
import cn.universal.core.metadata.types.ValueTypes;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArrayCodec extends AbstractValueTypeCodec<ArrayType> {

  @Override
  public String getTypeId() {
    return ArrayType.ID;
  }

  @Override
  public ArrayType decode(ArrayType type, Map<String, Object> config) {
    super.decode(type, config);
    JSONObject jsonObject = new JSONObject(config);
    ofNullable(jsonObject.get("elementType"))
        .map(
            v -> {
              if (v instanceof JSONObject) {
                return ((JSONObject) v);
              }
              JSONObject eleType = new JSONObject();
              eleType.put("type", v);
              return eleType;
            })
        .map(
            eleType -> {
              ValueType ValueType = ValueTypes.lookup(eleType.getStr("type")).get();

              ValueTypeCodecs.getCodec(ValueType.getId())
                  .ifPresent(codec -> codec.decode(ValueType, eleType));

              return ValueType;
            })
        .ifPresent(type::setElementType);

    return type;
  }

  @Override
  protected void doEncode(Map<String, Object> encoded, ArrayType type) {
    super.doEncode(encoded, type);
    ValueTypeCodecs.getCodec(type.getElementType().getId())
        .ifPresent(codec -> encoded.put("elementType", codec.encode(type.getElementType())));
  }
}
