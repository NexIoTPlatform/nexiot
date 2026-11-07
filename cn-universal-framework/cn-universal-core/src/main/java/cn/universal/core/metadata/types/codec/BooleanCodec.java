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
import cn.universal.core.metadata.types.BooleanType;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BooleanCodec extends AbstractValueTypeCodec<BooleanType> {

  @Override
  public String getTypeId() {
    return BooleanType.ID;
  }

  @Override
  public BooleanType decode(BooleanType type, Map<String, Object> config) {
    super.decode(type, config);
    JSONObject jsonObject = new JSONObject(config);

    ofNullable(jsonObject.getStr("trueText")).ifPresent(type::setTrueText);
    ofNullable(jsonObject.getStr("falseText")).ifPresent(type::setFalseText);
    ofNullable(jsonObject.getStr("trueValue")).ifPresent(type::setTrueValue);
    ofNullable(jsonObject.getStr("falseValue")).ifPresent(type::setFalseValue);
    ofNullable(jsonObject.getStr("description")).ifPresent(type::setDescription);

    return type;
  }

  @Override
  protected void doEncode(Map<String, Object> encoded, BooleanType type) {
    super.doEncode(encoded, type);
    encoded.put("trueText", type.getTrueText());
    encoded.put("falseText", type.getFalseText());
    encoded.put("trueValue", type.getTrueValue());
    encoded.put("falseValue", type.getFalseValue());
  }
}
