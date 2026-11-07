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
import cn.universal.core.metadata.types.EnumType;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnumCodec extends AbstractValueTypeCodec<EnumType> {

  @Override
  public String getTypeId() {
    return EnumType.ID;
  }

  @Override
  public EnumType decode(EnumType type, Map<String, Object> config) {
    super.decode(type, config);
    JSONObject jsonObject = new JSONObject(config);

    ofNullable(jsonObject.getJSONArray("elements"))
        .map(
            list ->
                list.stream()
                    .map(JSONObject.class::cast)
                    .map(
                        e ->
                            EnumType.Element.of(
                                e.getStr("value"), e.getStr("text"), e.getStr("description")))
                    .collect(Collectors.toList()))
        .ifPresent(type::setElements);

    return type;
  }

  @Override
  protected void doEncode(Map<String, Object> encoded, EnumType type) {
    super.doEncode(encoded, type);
    if (type.getElements() == null) {
      return;
    }
    encoded.put(
        "elements",
        type.getElements().stream().map(EnumType.Element::toMap).collect(Collectors.toList()));
  }
}
