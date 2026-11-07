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

import cn.universal.core.metadata.Converter;
import cn.universal.core.metadata.UnitSupported;
import cn.universal.core.metadata.ValidateResult;
import cn.universal.core.metadata.ValueType;
import cn.universal.core.metadata.unit.ValueUnit;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StringType extends AbstractType<StringType>
    implements ValueType, Converter<String>, UnitSupported {

  public static final String ID = "string";
  public static final StringType GLOBAL = new StringType();

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return "字符串";
  }

  // 支持单位
  private ValueUnit unit;

  @Override
  public ValidateResult validate(Object value) {
    return ValidateResult.success(String.valueOf(value));
  }

  @Override
  public String format(Object value) {
    ValueUnit unit = getUnit();
    if (unit == null) {
      return String.valueOf(value);
    }
    return (String) unit.format(value);
  }

  @Override
  public String convert(Object value) {
    return value == null ? null : String.valueOf(value);
  }
}
