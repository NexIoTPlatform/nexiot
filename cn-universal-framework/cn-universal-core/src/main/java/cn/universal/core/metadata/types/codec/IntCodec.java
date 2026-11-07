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
import cn.universal.core.metadata.types.IntType;
import cn.universal.core.metadata.unit.ValueUnits;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IntCodec extends AbstractValueTypeCodec<IntType> {

  @Override
  public String getTypeId() {
    return IntType.ID;
  }

  @Override
  public IntType decode(IntType type, Map<String, Object> config) {
    super.decode(type, config);
    JSONObject jsonObject = new JSONObject(config);
    ofNullable(jsonObject.getInt("max")).ifPresent(type::setMax);
    ofNullable(jsonObject.getInt("min")).ifPresent(type::setMin);
    ofNullable(jsonObject.getStr("unit")).flatMap(ValueUnits::lookup).ifPresent(type::setUnit);

    return type;
  }

  @Override
  protected void doEncode(Map<String, Object> encoded, IntType type) {
    encoded.put("max", type.getMax());
    encoded.put("min", type.getMin());

    if (type.getUnit() != null) {
      encoded.put("unit", type.getUnit().getId());
    }
  }
}
