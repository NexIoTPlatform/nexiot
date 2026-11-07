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

package cn.universal.core.metadata.unit;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class JsonValueUnit implements ValueUnit {

  private final String symbol;

  private final String name;

  public static JsonValueUnit of(String jsonStr) {

    JSONObject json = JSONUtil.parseObj(jsonStr);

    String symbol = json.getStr("symbol");
    if (null == symbol) {
      return null;
    }

    return new JsonValueUnit(symbol, (String) json.getOrDefault("name", symbol));
  }

  @Override
  public String getSymbol() {
    return symbol;
  }

  @Override
  public Object format(Object value) {
    if (value == null) {
      return null;
    }
    return value + "" + symbol;
  }

  @Override
  public String getId() {
    return "custom_" + symbol;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return symbol;
  }
}
